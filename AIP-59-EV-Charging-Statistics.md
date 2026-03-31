# AIP-59 — EV Charging Statistics Model

[//]: # (Branch: `AIP-59-EV-Charging-Statistics-Model`)

---

## Overview

End-to-end EV charging indicator: live charging station map, per-area demand choropleth, and a statistics panel — all wired from the database through the inference engine, through Hermes, to the React frontend.

This branch also delivered a broader frontend architecture change: all dashboards are always mounted at login, and static/semi-static API data is prefetched into the React Query cache on page load. This means the EV tab (and the car pollution overlay) open instantly with no loading spinner.

---

## Prefetch-on-Login / Map Caching Architecture

### The Problem

Before this branch, each dashboard was lazily mounted when the user navigated to it. This meant every tab switch caused:

- A Leaflet `MapContainer` to be created from scratch (re-downloads tiles, re-renders all markers)
- API calls to fire on demand, causing loading spinners on every navigation

### What Changed

**Always-mounted dashboards (`DashboardLayout.tsx`)**

All dashboards (`BusDashboard`, `CarDashboard`, `TrainDashboard`, etc.) are now rendered inside `DashboardLayout` at all times. Visibility is controlled via CSS (`visibility`/`opacity`/`pointerEvents`) rather than conditional JSX. This means:

- Leaflet map instances are created once and kept alive for the session
- React Query hooks inside each dashboard fire immediately on login
- Navigating between tabs is instant — no re-mount, no tile re-download

**Tile warmer**

`DashboardLayout` renders an off-screen (left: -9999px) `MapContainer` that loads Dublin OSM tiles into the browser cache on login. All subsequent maps reuse these cached tile images, eliminating the grey-tile flash on first open.

**Prefetch calls in `CarDashboard`**

`CarDashboard` unconditionally calls:

```ts
useCarJunctionEmissions()   // was previously conditional on mapMode === "pollution"
useEvChargingStations()
useEvChargingDemand()
useEvAreasGeoJson()
```

Since `CarDashboard` is always mounted, these four calls fire at login. By the time the user clicks the EV tab or switches to pollution mode, the data is already in cache.

**Cache configuration for static data**

EV hooks and the three car data hooks use `staleTime: Infinity` + `gcTime: 24h`:

```ts
// useEv.ts and static car hooks in useDashboard.ts
const EV_QUERY_CONFIG = {
  staleTime: Infinity,   // never considered stale — data doesn't change
  gcTime: 24 * 60 * 60 * 1000,
  refetchOnMount: false,
} as const;
```

Real-time feeds (bus positions, live notifications) retain their own short `staleTime` values (10–60s) and are unaffected.

**Global QueryClient defaults**

The global `QueryClient` defaults were left at what `main` had:

```ts
{ retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 }
```

Per-hook overrides take precedence. The global default only applies to hooks that don't specify their own config.

---

## Changes by Layer

### Inference Engine (Python / FastAPI)

**`src/inference_engine/ev_service.py`** _(new)_

- `get_charging_stations()` — queries `external_data.ev_charging_points`, returns address, coordinates, charger count, opening hours
- `get_charging_demand()` — queries `external_data.ev_charging_demand`, filters to Dublin City / Fingal / South Dublin, returns ranked list with `registered_evs`, `charging_demand`, `home_charge_percentage`, `charge_frequency`; flags areas with `charging_demand >= 20` as high-priority
- `get_areas_geojson()` — joins `ev_electoral_divisions` (PostGIS geometry) with `ev_charging_demand`; normalises accent/case differences between tables via `_normalize()`; returns a GeoJSON `FeatureCollection`
- `_get_db()` — context-manager for psycopg2 connections; reads credentials from `DBSettings`

**`src/inference_engine/settings/api_settings.py`** _(extended)_

- Added `DBSettings` (Pydantic `BaseSettings`) — `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `get_db_settings()` — `@lru_cache`, loads `.env.development` in dev mode, env vars in prod (same pattern as existing `get_api_settings()`)

**`src/inference_engine/ev_router.py`** _(new)_

- FastAPI router prefixed `/ev` with three `GET` endpoints:
  - `/ev/areas-geojson`
  - `/ev/charging-stations`
  - `/ev/charging-demand`
- Each delegates to `ev_service` and wraps exceptions as HTTP 500

**`src/inference_engine/main.py`** _(modified)_

- `app.include_router(ev_router)` — mounts the EV router

---

### Hermes (Java / Spring Boot)

**New DTOs** (`indicators/ev/dto/`)

- `EVStationDTO` — `address`, `county`, `latitude`, `longitude`, `charger_count` (`@JsonProperty("charger_count")`), `open_hours`
- `EVChargingStationsResponseDTO` — `total_stations`, `stations: List<EVStationDTO>`
- `EVAreaDemandDTO` — `area`, `registered_evs`, `charging_demand`, `home_charge_percentage`, `charge_frequency`
- `EVChargingDemandResponseDTO` — `summary: Map<String,Object>`, `high_priority_areas: List<String>`, `areas: List<EVAreaDemandDTO>`

**`indicators/ev/service/EVService.java`** _(new)_

- Proxies the three inference engine endpoints via `RestTemplate`
- `@Value("${inference-engine.base-url:http://localhost:8000}")` — default prevents context failure in tests that don't set the property

**`indicators/ev/controller/EVController.java`** _(new)_

- `GET /api/v1/ev/charging-stations`
- `GET /api/v1/ev/charging-demand`
- `GET /api/v1/ev/areas-geojson`
- Returns HTTP 500 on downstream failure

**`config/RestTemplateConfig.java`** _(new)_

- Registers a `RestTemplate` bean (required by `EVService`)

**`usermanagement/config/SecurityConfig.java`** _(modified)_

- Added `permitAll()` for `/api/v1/ev/**` and `/api/v1/events/**`, `/api/v1/pedestrians/**`

**`application-dev.yaml` / `application-prod.yaml`** _(modified)_

- Added `inference-engine.base-url` property:
  - dev: `http://localhost:8000`
  - prod: `${INFERENCE_ENGINE_BASE_URL}` (read from environment)

**New tests**

- `EVServiceTest` — 5 Mockito unit tests covering happy path, null response, and exception propagation for all three service methods
- `EVControllerTest` — 6 `@WebMvcTest` tests covering `200 OK` body assertions and `500` on service throw for all three endpoints

---

### Frontend (React / TypeScript)

**`src/types/ev.types.ts`** _(new)_

- `EvStation`, `EvChargingStationsResponse`
- `EvAreaDemand`, `EvChargingDemandResponse`
- `EvAreaGeoJsonFeature` — geometry typed as `"Polygon" | "MultiPolygon"` with matching coordinates union; properties use actual API field names (`display_name`, `ED_ENGLISH`, `COUNTY_ENGLISH`, `charging_demand`, `registered_ev`)
- `EvAreasGeoJsonResponse`

**`src/config/api.config.ts`** _(modified)_

- Added `EV_CHARGING_STATIONS`, `EV_CHARGING_DEMAND`, `EV_AREAS_GEOJSON` endpoint constants

**`src/api/ev.api.ts`** _(new)_

- `evApi.getChargingStations()`, `.getChargingDemand()`, `.getAreasGeoJson()`

**`src/hooks/useEv.ts`** _(new)_

- `useEvChargingStations`, `useEvChargingDemand`, `useEvAreasGeoJson`
- All use `staleTime: Infinity` + `gcTime: 24h` + `refetchOnMount: false` — EV data is static; fetched once per session

**`src/pages/EVDashboard.tsx`** _(new)_

- Full-viewport Leaflet map with a floating stats/search panel
- **Station markers** — custom electric-bolt `divIcon`, popup with address / charger count / hours
- **Demand choropleth** (`DemandOverlay`) — GeoJSON polygons coloured green/orange/red by `charging_demand` relative to the area maximum; filterable by demand tier (high/medium/low chips)
- **GeoJSON key** includes the active `demandFilter` so React-Leaflet remounts the layer and re-applies styles whenever the filter changes
- **Stats panel** — total stations, high-priority area count, per-area demand table with search; clicking a row flies the map to that area's centroid (supports both Polygon and MultiPolygon geometries)
- **Map modes** — "Stations" / "Demand Map" tabs; `MapContainer` is never conditionally unmounted
- Types sourced from `@types/leaflet` (`GeoJSONOptions["style"]`, `GeoJSONOptions["onEachFeature"]`) — avoids importing the uninstalled `geojson` package

**`src/pages/CarDashboard.tsx`** _(modified)_

- Added `useCarJunctionEmissions` (previously conditional on `mapMode`, now always called — prefetch on login)
- Added `useEvChargingStations()`, `useEvChargingDemand()`, `useEvAreasGeoJson()` — unconditional prefetch calls so EV data is warm in cache before the user opens the EV tab

**`src/layouts/DashboardLayout.tsx`** _(modified)_

- All dashboards always mounted — visibility toggled via CSS, not conditional JSX
- Role-based access control restored: `canSeeView` map computed from `TRANSPORT_ACCESS` + user roles; nav items filtered; dashboard panels guarded with `{canSeeView.x && ...}`; stale `localStorage` view discarded if user's roles no longer permit it
- Added tile warmer (off-screen `MapContainer`) to pre-load Dublin OSM tiles into browser cache on login

**`src/App.tsx`** _(modified)_

- Removed debug `console.log` statements; global QueryClient defaults left unchanged from `main`

---

### Infrastructure (Helm)

**`infra/helm-charts/charts/hermes/templates/deployment.yaml`** _(fixed)_

- Renamed env var `INFERENCE_ENGINE_URL` → `INFERENCE_ENGINE_BASE_URL` to match the placeholder in `application-prod.yaml`

**`infra/helm-charts/charts/inference-engine/`**

- Deployment and values updated to include the inference engine service in cluster

---

## Bug Fixes Made During This Branch

| Bug | Root Cause | Fix |
|---|---|---|
| EV APIs returning 500 / timeout | Merge-conflict markers left in `ev_service.py` causing `SyntaxError` | Removed conflict markers, kept complete implementation |
| Inference engine not loading DB credentials locally | `APP_ENV=dev` not set → `.env.development` not loaded | Must start as `APP_ENV=dev uv run uvicorn ...` |
| `framer-motion` missing after merge | Merge conflict resolution in `package.json` dropped the package | Restored; also removed unused `idb-keyval` and `@tanstack/react-query-persist-client` that were added by mistake |
| Frontend typecheck: `Cannot find module 'geojson'` | `geojson` package not in `package.json` but imported directly | Replaced with `GeoJSONOptions["style"]` / `["onEachFeature"]` from `@types/leaflet` |
| Demand filter chips had no effect on map | `<GeoJSON key="geojson-${show}">` never changed when filter changed | Key now includes `demandFilter.join(",")` so layer remounts on filter change |
| Hermes `@SpringBootTest` context fail | `EVService` `@Value("${inference-engine.base-url}")` had no default; test environment has no properties file | Added `:http://localhost:8000` default to `@Value` |
| Helm prod env var mismatch | `deployment.yaml` set `INFERENCE_ENGINE_URL`; `application-prod.yaml` read `INFERENCE_ENGINE_BASE_URL` | Renamed env var in helm template |
| RBAC silently removed | Router refactor consolidated all `/dashboard/*` routes into one `<ProtectedRoute>` with no `allowedRoles` | Restored role checks inside `DashboardLayout` via `canSeeView` |

---

## Known Limitations / Future Scope

### Data

- **Static dataset** — `ev_charging_points` and `ev_charging_demand` are loaded once at DB population time. There is no automated refresh. As the EV fleet grows, demand figures will become stale.
- **`charging_demand >= 20` threshold** — the high-priority cutoff in `ev_service.py` is hardcoded. Should be configurable or data-driven.
- **Coverage gap** — `get_areas_geojson()` only joins on the first part of the electoral division name (before the first comma). Areas whose names differ even after normalisation will show no demand colour on the map.

### Features

- **Routing / nearest station** — the map shows station locations but has no "find nearest charger" or routing capability.
- **Real-time availability** — charger occupancy is not tracked; only static charger count is shown.
- **Filtering by county** — the demand endpoint filters to Dublin City / Fingal / South Dublin but the map shows all electoral divisions. A county selector in the UI would improve usability.
- **Accessibility** — map colour coding (red/orange/green) is not accessible to colour-blind users. An additional pattern or label overlay should be added.
- **EV security** — `/api/v1/ev/**` is currently `permitAll()`. If the data is considered sensitive, authentication should be required.
- **Pagination** — `get_charging_demand()` returns all rows in one response. For scalability, server-side pagination should be added.
- **Tests: inference engine** — no unit tests exist for `ev_service.py` or `ev_router.py`. These should be added with a mocked psycopg2 connection.
