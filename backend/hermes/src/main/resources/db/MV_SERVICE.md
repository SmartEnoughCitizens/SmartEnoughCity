# Materialized View Management Service

## Why This Exists

The original `BusMetricsComputeService` loaded the entire `bus_trips`, `bus_ridership`, and
`bus_live_trip_updates_stop_time_updates` tables into Java heap every 5 minutes to compute
per-route metrics. With `bus_live_trip_updates_stop_time_updates` alone exceeding 1.8 million
rows, this caused repeated `OutOfMemoryError: Java heap space` in production.

The fix moves all computation into PostgreSQL as a **Materialized View (MV)**. Java no longer
touches those tables at compute time — it only issues a single
`REFRESH MATERIALIZED VIEW CONCURRENTLY` statement every 5 minutes, which Postgres executes
entirely server-side.

A generic MV management service was built alongside the fix so that future MVs can be registered,
scheduled, and monitored through a single internal API without touching Java code.

---

## How It Works

### Materialized Views

A materialized view is a snapshot of a query result stored as a physical table in Postgres.
Unlike a regular view (which re-runs the query on every read), an MV is pre-computed and
reads are instantaneous.

`REFRESH MATERIALIZED VIEW CONCURRENTLY` re-runs the defining query in the background and
swaps in the new data atomically. Reads continue working during the refresh — there is no
downtime or locking of the view.

**Requirement:** `CONCURRENTLY` requires at least one `UNIQUE INDEX` on the MV. The service
creates these automatically from the `uniqueKeyColumns` field.

### Scheduling Flow

```
App startup
    │
    ├── Hibernate creates backend.mv_registry and backend.mv_refresh_log tables
    │
    └── MvSchedulerService.configureTasks() called ONCE by Spring
            │
            └── Reads all enabled MVs from mv_registry
                Registers a CronTrigger per MV into ThreadPoolTaskScheduler (in-memory)
                No further DB polling — JVM clock drives the schedule
                          │
                          ▼
                Every N minutes: trigger fires
                    → MaterializedViewService.refresh(name, "SCHEDULER")
                    → REFRESH MATERIALIZED VIEW CONCURRENTLY backend.<name>
                    → Updates mv_registry (lastRefreshedAt, status, duration)
                    → Inserts row into mv_refresh_log
                    → Prunes mv_refresh_log to last 10 rows for this MV

POST /api/v1/mv (upsert)
    └── Creates/recreates MV in Postgres
        Saves row in mv_registry
        Calls reschedule() → cancels old in-memory CronTrigger, registers new one
        (cron changes are live immediately — no restart needed)
```

### Concurrent Refreshes

If two refresh calls arrive for the same MV simultaneously (e.g. scheduler fires while a
manual refresh is in progress), Postgres serializes them via an exclusive lock on the MV.
The second call waits until the first finishes. No data corruption occurs. No application-level
queuing is needed.

---

## Database Tables

### `backend.mv_registry`

Stores the definition and current status of every registered MV. Created automatically by
Hibernate on startup.

| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGSERIAL` | Primary key |
| `name` | `VARCHAR(100)` | Unique logical identifier. Used as the Postgres object name. |
| `description` | `TEXT` | Human-readable purpose |
| `view_schema` | `VARCHAR(50)` | Schema the MV lives in (default: `backend`) |
| `query_sql` | `TEXT` | The SELECT body stored as-is — single source of truth for the MV definition |
| `unique_key_columns` | `TEXT` | Comma-separated columns with UNIQUE indexes (required for REFRESH CONCURRENTLY) |
| `refresh_cron` | `VARCHAR(100)` | Spring 6-field cron expression. Null = no scheduled refresh |
| `last_refreshed_at` | `TIMESTAMPTZ` | Timestamp of last completed refresh |
| `last_refresh_duration_ms` | `BIGINT` | Wall-clock ms of last refresh |
| `last_refresh_status` | `VARCHAR(20)` | `SUCCESS` or `FAILED` |
| `last_refresh_error` | `TEXT` | Postgres exception message if last status was FAILED |
| `version` | `INT` | Incremented on every upsert — tracks how many times the definition changed |
| `enabled` | `BOOLEAN` | If false, skipped by scheduler and refresh-all |
| `created_at` | `TIMESTAMPTZ` | Set once on first registration |
| `updated_at` | `TIMESTAMPTZ` | Updated on every upsert |

### `backend.mv_refresh_log`

Audit log for every refresh attempt. Capped at the last 10 rows per MV (older rows are
automatically deleted after each refresh).

| Column | Type | Description |
|--------|------|-------------|
| `id` | `BIGSERIAL` | Primary key |
| `mv_name` | `VARCHAR(100)` | References the MV by name |
| `status` | `VARCHAR(20)` | `SUCCESS` or `FAILED` |
| `duration_ms` | `BIGINT` | Wall-clock ms of the refresh |
| `refreshed_at` | `TIMESTAMPTZ` | When the refresh ran |
| `error_message` | `TEXT` | Exception message if FAILED |
| `triggered_by` | `VARCHAR(50)` | `SCHEDULER`, `API`, or `REFRESH_ALL` |

---

## Package Structure

```
com.trinity.hermes.mv
├── config
│   └── MvSchedulerConfig.java        ThreadPoolTaskScheduler bean (pool=2, prefix mv-scheduler-)
├── controller
│   └── MaterializedViewController.java  REST controller at /api/v1/mv
├── dto
│   ├── MvRefreshResult.java          Result of a single refresh (status, duration, error)
│   ├── MvRegistryDTO.java            Full read model including stored query + refresh history
│   └── UpsertMvRequest.java          Input DTO for POST /api/v1/mv
├── entity
│   ├── MvRefreshLog.java             JPA entity → backend.mv_refresh_log
│   └── MvRegistry.java               JPA entity → backend.mv_registry
├── facade
│   └── MaterializedViewFacade.java   Thin delegation facade (follows existing project pattern)
├── repository
│   ├── MvRefreshLogRepository.java   findByMvName, pruneOldLogs, deleteByMvName
│   └── MvRegistryRepository.java     findByName, findAllByEnabledTrue, existsByName
└── service
    ├── MaterializedViewService.java  Core: upsert, refresh, drop, toggle, list
    └── MvSchedulerService.java       Dynamic cron scheduling via SchedulingConfigurer
```

### Modified Files

| File | Change |
|------|--------|
| `indicators/bus/entity/BusRouteMetrics.java` | Added `@Immutable`, removed `@GeneratedValue` — entity now maps to the MV |
| `indicators/bus/service/BusMetricsComputeService.java` | Stripped all computation logic. Now delegates to `materializedViewService.refresh("bus_route_metrics")` |
| `usermanagement/config/SecurityConfig.java` | Added `permitAll()` for `/api/v1/mv/**` (internal API, accessed via port-forward only) |

---

## API Reference

**Base URL:** `http://localhost:8080/api/v1/mv`
**Access:** Internal — port-forward to pod. No authentication required.

---

### `POST /api/v1/mv` — Register or update an MV

Same name = upsert. Drops and recreates the MV with the new query, increments `version`.
The MV is **populated immediately** on creation (not lazily).

**Request body:**

```json
{
  "name": "bus_route_metrics",
  "description": "Per-route bus metrics: utilization, delays, occupancy",
  "viewSchema": "backend",
  "querySql": "WITH ... SELECT ...",
  "uniqueKeyColumns": "id,route_id",
  "refreshCron": "0 0/5 * * * *",
  "enabled": true
}
```

| Field | Required | Validation |
|-------|----------|------------|
| `name` | yes | Lowercase alphanumeric + underscores (`^[a-z][a-z0-9_]*$`) |
| `querySql` | yes | No semicolons allowed |
| `uniqueKeyColumns` | yes | Comma-separated lowercase column names |
| `viewSchema` | no | Defaults to `backend` |
| `description` | no | Free text |
| `refreshCron` | no | Valid Spring 6-field cron. Null/blank = no scheduled refresh |
| `enabled` | no | Defaults to `true` |

**Response `200`:** Full MV registry object (see GET /{name} for shape).

**Errors:**
- `400` — validation failure (semicolon in SQL, invalid cron, bad column names)
- `500` — Postgres error (old table still exists, SQL syntax error)

---

### `GET /api/v1/mv` — List all MVs

Returns all registered MVs with full metadata, stored query, and last 10 refresh attempts each.

**Response `200`:** Array of MV registry objects.

---

### `GET /api/v1/mv/{name}` — Get a single MV

**Response `200`:**

```json
{
  "id": 1,
  "name": "bus_route_metrics",
  "description": "Per-route bus metrics: utilization, delays, occupancy",
  "viewSchema": "backend",
  "querySql": "WITH trip_windows AS ...",
  "uniqueKeyColumns": "id,route_id",
  "refreshCron": "0 0/5 * * * *",
  "enabled": true,
  "version": 1,
  "createdAt": "2026-03-27T10:00:00Z",
  "updatedAt": "2026-03-27T10:00:00Z",
  "lastRefreshedAt": "2026-03-27T10:05:00Z",
  "lastRefreshDurationMs": 412,
  "lastRefreshStatus": "SUCCESS",
  "lastRefreshError": null,
  "refreshHistory": [
    {
      "mvName": "bus_route_metrics",
      "status": "SUCCESS",
      "durationMs": 412,
      "refreshedAt": "2026-03-27T10:05:00Z",
      "errorMessage": null
    }
  ]
}
```

**Errors:** `404` if not found.

---

### `POST /api/v1/mv/{name}/refresh` — Manual refresh

Triggers an immediate `REFRESH MATERIALIZED VIEW CONCURRENTLY`. Always returns `200` with
a result object — never throws a 500 even if the refresh fails. Check `status` in the response.

**Response `200`:**

```json
{
  "mvName": "bus_route_metrics",
  "status": "SUCCESS",
  "durationMs": 390,
  "refreshedAt": "2026-03-27T10:10:00Z",
  "errorMessage": null
}
```

`status` values: `SUCCESS`, `FAILED`

**Errors:** `404` if not found.

---

### `POST /api/v1/mv/refresh-all` — Refresh all enabled MVs

Runs sequentially. Returns a result per MV. Never aborts on partial failure — if one MV fails,
the rest still run.

**Response `200`:** Array of refresh result objects.

---

### `GET /api/v1/mv/{name}/history` — Refresh history only

Returns just the `refreshHistory` array without the rest of the MV metadata. Useful for quick
debugging.

**Response `200`:** Array of refresh results (newest first, max 10 entries).

---

### `PATCH /api/v1/mv/{name}/toggle` — Enable / disable scheduled refresh

Disabling cancels the in-memory CronTrigger immediately. Re-enabling reschedules it live.
The MV itself is not dropped — it just stops being automatically refreshed.

**Response `200`:** Full MV registry object with updated `enabled` field.

---

### `DELETE /api/v1/mv/{name}` — Drop MV

Drops the MV from Postgres (`DROP MATERIALIZED VIEW IF EXISTS ... CASCADE`), deletes all
refresh history, cancels the schedule, and removes the registry row.

**Response `204 No Content`**

**Errors:** `404` if not found.

---

## Setup: Registering `bus_route_metrics`

This is a one-time setup step after deploying the new code.

**Step 1 — Drop the old table** (run directly against the database):

```sql
DROP TABLE IF EXISTS backend.bus_route_metrics CASCADE;
```

**Step 2 — Register the MV via the API:**

```bash
curl -X POST http://localhost:8080/api/v1/mv \
  -H "Content-Type: application/json" \
  -d '{
    "name": "bus_route_metrics",
    "description": "Per-route bus metrics computed from live GTFS-RT data. Replaces the old Java-computed table.",
    "viewSchema": "backend",
    "uniqueKeyColumns": "id,route_id",
    "refreshCron": "0 0/5 * * * *",
    "enabled": true,
    "querySql": "<paste the SELECT body here>"
  }'
```

The full `querySql` is in `src/main/resources/db/bus_route_metrics_mv.sql`.

After a successful call:
- `mv_registry` has one row for `bus_route_metrics`
- The MV exists in Postgres, populated with current data
- The scheduler fires every 5 minutes and logs to `mv_refresh_log`
- `POST /api/v1/bus/metrics/refresh` continues to work (delegates to this service)

---

## Adding Future MVs

No Java code changes required. Just POST to the API:

```bash
curl -X POST http://localhost:8080/api/v1/mv \
  -H "Content-Type: application/json" \
  -d '{
    "name": "your_mv_name",
    "description": "...",
    "querySql": "SELECT ...",
    "uniqueKeyColumns": "id",
    "refreshCron": "0 0 * * * *"
  }'
```

To update the query later, POST again with the same name — the MV is dropped and recreated
with the new SQL, `version` is incremented, and the schedule is updated in memory.

---

## Cron Expression Format

Spring 6-field cron: `seconds minutes hours day-of-month month day-of-week`

| Expression | Schedule |
|------------|----------|
| `0 0/5 * * * *` | Every 5 minutes |
| `0 0/15 * * * *` | Every 15 minutes |
| `0 0 * * * *` | Every hour |
| `0 0 2 * * *` | Daily at 02:00 |
| `0 0 */6 * * *` | Every 6 hours |

---

## Debugging

**Check last refresh status:**
```bash
curl http://localhost:8080/api/v1/mv/bus_route_metrics
```

**Check refresh history:**
```bash
curl http://localhost:8080/api/v1/mv/bus_route_metrics/history
```

**Manually trigger a refresh and see result immediately:**
```bash
curl -X POST http://localhost:8080/api/v1/mv/bus_route_metrics/refresh
```

**Check directly in the database:**
```sql
-- Registry
SELECT name, version, last_refresh_status, last_refreshed_at, last_refresh_duration_ms
FROM backend.mv_registry;

-- Last 10 refresh attempts per MV
SELECT * FROM backend.mv_refresh_log ORDER BY refreshed_at DESC;

-- Verify MV exists and has data
SELECT COUNT(*) FROM backend.bus_route_metrics;
SELECT * FROM backend.bus_route_metrics LIMIT 5;
```
