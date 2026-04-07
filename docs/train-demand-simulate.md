# Train Dashboard — Demand Analysis & Simulation

## Overview

The Train Dashboard was extended with a demand scoring system and a corridor-based simulation tool. This document describes what changed, why, and how the pieces fit together.

---

## What Was There Before

The original Train Dashboard showed:
- Live train positions on a map
- Station list with basic metadata
- Frequent delays tab
- KPI cards (total stations, live trains, on-time %)

There was no demand analysis, no simulation, and no approval workflow.

---

## What Changed

### 1. Demand Scoring (Inference Engine)

**File:** `backend/inference_engine/src/inference_engine/indicators/train/train_demand.py`

A demand score is computed for every train station and stored in the `backend.station_demand_scores` table. Scores are recomputed on inference engine startup.

#### Signal Weighting

| Signal | Weight | Source |
|---|---|---|
| Ridership (annual) | 40% | `train_station_ridership` table, `count_2024` column |
| Local uptake | 25% | `small_areas` census geometry vs station coordinates |
| Capacity pressure | 25% | Trip count / train type capacity |
| Live footfall | 10% | Pedestrian counter sites within 500 m of the stop |

Each signal is normalised 0→1 before weighting. The combined score is stored as `demand_score`.

#### Train Type Capacity

```python
TRAIN_TYPE_CAPACITY = {
    "D": 350, "DART": 350,
    "S": 300, "SUBURBAN": 300, "COMMUTER": 300,
    "M": 450, "MAINLINE": 450,
}
DEFAULT_CAPACITY = 350
```

Both single-character DB codes and full enum names are supported.

#### PostGIS Fix (SRID mislabelling)

`small_areas.geom` is stored in EPSG:2157 (Irish Transverse Mercator, metres) but was labelled as SRID 4326. The catchment query explicitly overrides this:

```sql
ON ST_DWithin(
    ST_Transform(ST_SetSRID(ST_MakePoint(ts2.lon, ts2.lat), 4326), 2157),
    ST_SetSRID(sa.geom, 2157),
    800   -- 800 metre radius
)
```

---

### 2. Demand API

**File:** `backend/inference_engine/src/inference_engine/train_router.py`

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/train/demand` | GET | Returns all station demand scores from the pre-computed DB table |
| `/api/v1/train/demand/simulate` | POST | Simulates adding trains on up to 3 corridors |

---

### 3. Simulation Logic

**Request body:**
```json
{
  "corridors": [
    { "origin_stop_id": "CNLLY", "destination_stop_id": "PERSE", "train_count": 20 }
  ]
}
```

#### Formula — Exponential Pressure Decay

When extra trains are added to a corridor, capacity pressure decays exponentially:

```python
_PRESSURE_SENSITIVITY = 13.0

relief_ratio   = extra / max(trip_count, 1)          # fractional service increase
pressure_factor = exp(-relief_ratio * _PRESSURE_SENSITIVITY)
new_norm_pres  = base_norm_pres * pressure_factor
```

**Why exponential, not proportional?**

A simple proportional formula (`base_trips / new_trips`) would make a 20-train addition on a 509-trip corridor barely noticeable (<4% improvement), and would cap small corridors at the same maximum improvement as large ones. Exponential decay with `SENSITIVITY=13` calibrates the formula so that adding 20 trains to a 509-trip corridor gives approximately **10% score improvement** — a meaningful, visible change in the UI without being unrealistically large.

Smaller corridors (fewer existing trips) benefit more per added train, which is appropriate for network planning: adding capacity where it is relatively thin matters more.

**Stops with no ridership data** use a simpler fallback:

```python
new_score = base_score * pressure_factor
```

**Response:**
```json
{
  "base_demand": [...],
  "simulated_demand": [...],
  "affected_stop_ids": ["CNLLY", "PERSE", "TARA"]
}
```

---

### 4. Hermes Proxy Layer

**File:** `backend/hermes/src/main/java/com/trinity/hermes/indicators/train/service/TrainDashboardService.java`

Hermes acts as a reverse proxy for the inference engine. The simulation request is manually serialised to snake_case before forwarding:

```java
Map<String, Object> body = Map.of("corridors", corridors);
// corridor entries use snake_case keys: origin_stop_id, destination_stop_id, train_count
```

This is necessary because `@JsonNaming(SnakeCaseStrategy)` on DTOs applies to both serialisation and deserialisation, which would break camelCase output to the frontend. The fix uses `@JsonAlias` per field for deserialising snake_case from the inference engine while serialising camelCase to the frontend.

---

### 5. Train Dashboard UI

**File:** `frontend/src/pages/TrainDashboard.tsx`

#### Demand Tab (Tab 3)

- Each station marker on the map is colour-coded by demand score (green → yellow → red).
- Clicking a station opens a popup showing:
  - Demand score (0–100)
  - Trip count (daily services through this stop)
  - Nearby residents (catchment population within 800 m)
  - Local uptake % (residents / total catchment × demand weight)
  - Capacity pressure %
  - Live footfall score

#### Simulation Panel

Available to `Train_Admin` only. Up to 3 corridors can be configured (origin → destination + train count).

On running the simulation, a summary panel shows:

| Label | Meaning |
|---|---|
| Stations improved | Number of stops whose score decreased |
| Overall score improved by | Average demand score reduction across affected stops (%) |
| Train crowding reduced by | Average pressure relief % across affected stops |
| Biggest improvement | Stop with the largest individual pressure reduction |

Labels are deliberately plain English — the audience is non-technical operations staff, not engineers.

#### Role Guards

```ts
const isTrainAdmin  = roles.includes("Train_Admin") && !roles.includes("City_Manager");
const isCityManager = roles.includes("City_Manager") || roles.includes("Government_Admin");
```

The `!includes("City_Manager")` guard is required because Keycloak composite roles mean a City_Manager JWT also contains Train_Admin as a sub-role.

---

### 6. Approval Workflow

After running a simulation, `Train_Admin` sees a **Send for Approval** button. This submits the simulation corridors and metrics as an approval request.

**Payload shape sent to the approval service:**

```ts
{
  corridors: [{ origin, destination, trainsAdded }],
  impact: {
    stationsAffected: number,
    avgDemandReduction: "4.1%",
    mostImprovedStation: "Tara Street",
  },
  stationBreakdown: [
    {
      station, demandBefore, demandAfter, demandReduction,
      overcrowdingRisk, annualPassengers, nearbyResidents
    }
  ]
}
```

`demandReduction` is `(base - sim) * 100` (always positive — a lower score is better).
`annualPassengers` and `nearbyResidents` are percentile ranks relative to all Dublin stops.

**Notification email body** is built as structured markdown by `ApprovalService.buildDetailedBody()`, including:
- A proposed changes list (origin → destination, +N trains/day)
- An impact summary table
- A per-station breakdown table

The markdown is rendered in the frontend notification detail dialog via `react-markdown` + `remark-gfm`.

**Approvals tab (Tab 4):**

| Role | What they see |
|---|---|
| `Train_Admin` | Their own requests only |
| `City_Manager` / `Government_Admin` | All requests across all indicators |
| `*_Provider` | Only APPROVED requests for their indicator |

City Managers receive an email + in-app notification when a request is submitted. The requester is notified when the decision is made.

Each approval row shows a receipt reference in the format `APR-000042`.

---

### 7. Dead Code Removed

As part of this sprint, orphaned Java code was removed from Hermes:

| Removed | Reason |
|---|---|
| `TrainUtilisationService` | Never injected; methods not called |
| `TrainService` | Duplicate of `TrainDashboardService`; never injected |
| `TrainMovement` + `TrainMovementRepository` | Orphaned entity; never queried |
| `TrainApprovalRequest` | No repository; never accessed |
| `StationTripCountProjection` | Only used by a dead `findStationTripCounts` query |
| 6 DTOs (`TrainUtilisationResponseDTO`, `TrainSimulateResponseDTO`, `TrainSimulationDTO`, `TrainSimResultDTO`, `TrainUtilisationHeadsignDTO`, `TrainUtilisationStationDTO`) | Only used by the dead `TrainUtilisationService` |

The dead `findStationTripCounts` method was also removed from `GtfsStopRepository`.

---

## Deep-Link Navigation

Notifications include an `actionUrl`. Clicking **View in Dashboard** dispatches a Redux `requestNavigation` action:

```ts
dispatch(requestNavigation({ view: "train", tab: "approvals" }))
```

`DashboardLayout` listens for this and switches the active view. `TrainDashboard` listens and switches the tab. No URL changes or page reloads are needed.
