"""
Train station demand scoring pipeline.

Demand score = weighted sum of four independently-normalised signals:
  40% ridership volume   — annual boardings / max across Dublin stops
  25% local uptake       — daily riders / catchment population (CSO 800 m radius)
  25% capacity pressure  — daily riders / (trips/day × train-type seat capacity)
  10% recent footfall    — pedestrian count near stop (last 24 h) / max footfall

Fallback for stops with no ridership data:
  GTFS trip-frequency score × 0.5  (supply-only; always ranks below ridership-backed stops)

Simulation contract:
  Adding trains to a corridor changes the capacity-pressure signal only.
  Ridership, uptake, and footfall are unchanged (riders don't instantly shift routes).
  Stored max_pressure is used for re-normalisation so base and simulated scores
  live on the same 0-1 scale and can be compared directly.
"""

from __future__ import annotations

import logging

import pandas as pd
from sqlalchemy import text

from inference_engine.db import engine

logger = logging.getLogger(__name__)

# ── Dublin bounding box ──────────────────────────────────────────────
DUBLIN_LAT_MIN = 53.05
DUBLIN_LAT_MAX = 53.75
DUBLIN_LON_MIN = -6.65
DUBLIN_LON_MAX = -5.90

# ── Train type seat capacities ───────────────────────────────────────
# D = DART  (CAF 8500 EMU 2-unit set)
# S = Suburban / Commuter (IÉ 29000 / 22000 DMU)
# M = Mainline / Intercity (CAF ICR)
TRAIN_TYPE_CAPACITY: dict[str, int] = {
    "D": 350, "S": 300, "M": 450,               # single-char codes (legacy)
    "DART": 350, "SUBURBAN": 300, "MAINLINE": 450,  # full enum names from DB
    "COMMUTER": 300,
}
DEFAULT_CAPACITY = 350

# ── Demand-score weights (must sum to 1.0) ───────────────────────────
W_RIDERSHIP = 0.40
W_UPTAKE    = 0.25
W_PRESSURE  = 0.25
W_FOOTFALL  = 0.10

_DUBLIN_PARAMS = {
    "lat_min": DUBLIN_LAT_MIN,
    "lat_max": DUBLIN_LAT_MAX,
    "lon_min": DUBLIN_LON_MIN,
    "lon_max": DUBLIN_LON_MAX,
}


# ── Table bootstrap ──────────────────────────────────────────────────

def ensure_demand_scores_table() -> None:
    """Create backend.station_demand_scores if it doesn't exist yet."""
    with engine.begin() as conn:
        conn.execute(text("""
            CREATE TABLE IF NOT EXISTS backend.station_demand_scores (
                stop_id              TEXT PRIMARY KEY,
                name                 TEXT    NOT NULL,
                lat                  DOUBLE PRECISION NOT NULL,
                lon                  DOUBLE PRECISION NOT NULL,
                trip_count           INTEGER NOT NULL DEFAULT 0,
                ridership_count      INTEGER NOT NULL DEFAULT 0,
                catchment_population INTEGER NOT NULL DEFAULT 0,
                station_type         TEXT,
                footfall_count       INTEGER NOT NULL DEFAULT 0,
                norm_ridership       DOUBLE PRECISION NOT NULL DEFAULT 0,
                norm_uptake          DOUBLE PRECISION NOT NULL DEFAULT 0,
                norm_pressure        DOUBLE PRECISION NOT NULL DEFAULT 0,
                norm_footfall        DOUBLE PRECISION NOT NULL DEFAULT 0,
                raw_pressure         DOUBLE PRECISION NOT NULL DEFAULT 0,
                max_pressure         DOUBLE PRECISION NOT NULL DEFAULT 1,
                demand_score         DOUBLE PRECISION NOT NULL DEFAULT 0,
                computed_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
        """))
    logger.info("backend.station_demand_scores table ensured.")


# ── Data loaders ─────────────────────────────────────────────────────

def _load_stops_with_signals() -> pd.DataFrame:
    """
    Dublin GTFS stops enriched with:
      - 2024 ridership (train_station_ridership, name match)
      - catchment population  (CSO small_areas within 0.0072° ≈ 800 m)
      - station type D/S/M    (irish_rail_stations, name match)
    """
    query = text("""
        SELECT
            s.id                                AS stop_id,
            s.name,
            s.lat,
            s.lon,
            COALESCE(ir.station_type::TEXT, 'S') AS station_type,
            COALESCE(r.count_2024, 0)           AS ridership_count,
            COALESCE(pop.catchment_population, 0) AS catchment_population
        FROM external_data.train_stops s
        LEFT JOIN external_data.irish_rail_stations ir
               ON LOWER(TRIM(ir.station_desc)) = LOWER(TRIM(s.name))
        LEFT JOIN external_data.train_station_ridership r
               ON LOWER(TRIM(r.station)) = LOWER(TRIM(s.name))
        LEFT JOIN (
            SELECT ts2.id                             AS stop_id,
                   COALESCE(SUM(sa.population), 0)   AS catchment_population
            FROM external_data.train_stops ts2
            LEFT JOIN external_data.small_areas sa
                   ON ST_DWithin(
                          ST_Transform(ST_SetSRID(ST_MakePoint(ts2.lon, ts2.lat), 4326), 2157),
                          ST_SetSRID(sa.geom, 2157),
                          800
                      )
            WHERE ts2.lat BETWEEN :lat_min AND :lat_max
              AND ts2.lon BETWEEN :lon_min AND :lon_max
            GROUP BY ts2.id
        ) pop ON pop.stop_id = s.id
        WHERE s.lat BETWEEN :lat_min AND :lat_max
          AND s.lon BETWEEN :lon_min AND :lon_max
    """)
    with engine.connect() as conn:
        df = pd.read_sql(query, conn, params=_DUBLIN_PARAMS)
    logger.info("Loaded %d Dublin train stops with signals.", len(df))
    return df


def _load_trip_counts() -> pd.DataFrame:
    """
    Weighted-average daily GTFS services per stop across a full 7-day week.
    Aggregates at (stop, service_id) level then weights by days-per-week active.
    """
    query = text("""
        SELECT
            sub.stop_id,
            GREATEST(1, ROUND(
                SUM(sub.trip_count *
                    ((cs.monday::int)    + (cs.tuesday::int)   + (cs.wednesday::int) +
                     (cs.thursday::int)  + (cs.friday::int)    +
                     (cs.saturday::int)  + (cs.sunday::int))
                ) / 7.0
            )::int) AS trip_count
        FROM (
            SELECT st.stop_id, t.service_id,
                   COUNT(DISTINCT st.trip_id) AS trip_count
            FROM external_data.train_stop_times st
            JOIN external_data.train_trips t ON t.id = st.trip_id
            GROUP BY st.stop_id, t.service_id
        ) sub
        JOIN external_data.train_stops s
               ON s.id = sub.stop_id
        JOIN external_data.train_calendar_schedule cs
               ON cs.service_id = sub.service_id
        WHERE s.lat BETWEEN :lat_min AND :lat_max
          AND s.lon BETWEEN :lon_min AND :lon_max
        GROUP BY sub.stop_id
    """)
    with engine.connect() as conn:
        df = pd.read_sql(query, conn, params=_DUBLIN_PARAMS)
    logger.info("Loaded trip counts for %d stops.", len(df))
    return df


def _load_live_footfall() -> pd.DataFrame:
    """
    Pedestrian count from the nearest counter site (≤ 500 m) per Dublin stop,
    summed over the last 24 hours.  Returns only stops that have a nearby counter.
    """
    query = text("""
        SELECT
            nearest.stop_id,
            COALESCE(SUM(m.count), 0) AS footfall_count
        FROM (
            SELECT DISTINCT ON (ts.id)
                ts.id  AS stop_id,
                pcs.id AS counter_site_id
            FROM external_data.train_stops ts
            JOIN external_data.pedestrian_counter_sites pcs
                ON ST_DWithin(
                       ST_SetSRID(ST_MakePoint(ts.lon,  ts.lat),  4326),
                       ST_SetSRID(ST_MakePoint(pcs.lon, pcs.lat), 4326),
                       0.005
                   )
               AND pcs.pedestrian_sensor = TRUE
            WHERE ts.lat BETWEEN :lat_min AND :lat_max
              AND ts.lon BETWEEN :lon_min AND :lon_max
            ORDER BY ts.id,
                ST_Distance(
                    ST_SetSRID(ST_MakePoint(ts.lon,  ts.lat),  4326),
                    ST_SetSRID(ST_MakePoint(pcs.lon, pcs.lat), 4326)
                ) ASC
        ) nearest
        JOIN external_data.pedestrian_channels c
               ON c.site_id = nearest.counter_site_id
              AND c.mobility_type = 'PEDESTRIAN'
        JOIN external_data.pedestrian_counter_measures m
               ON m.channel_id = c.channel_id
        WHERE m.start_datetime >= NOW() - INTERVAL '24 hours'
        GROUP BY nearest.stop_id
    """)
    with engine.connect() as conn:
        df = pd.read_sql(query, conn, params=_DUBLIN_PARAMS)
    logger.info("Loaded footfall for %d stops.", len(df))
    return df


# ── Core pipeline ─────────────────────────────────────────────────────

def compute_and_save_demand_scores() -> list[dict]:
    """
    Run the full demand-scoring pipeline and persist results to
    backend.station_demand_scores.  Returns the scored rows as a list
    of dicts (suitable for in-memory caching in the router).

    Signal normalisation:
      Each raw signal is divided by its maximum across all Dublin stops,
      giving a clean 0-1 range before applying weights.  This means the
      station with the highest raw value always scores 1.0 on that signal,
      and all others are proportionally lower.

    Why store norm_* and max_pressure?
      The simulation endpoint only changes trip_count for affected stops.
      Rather than re-running the full pipeline, Hermes can recompute
      norm_pressure for those stops using:
          new_norm_pressure = min(1.0, new_raw_pressure / max_pressure)
      and then recalculate the weighted score in-process.
    """
    ensure_demand_scores_table()

    stops_df    = _load_stops_with_signals()
    trips_df    = _load_trip_counts()
    footfall_df = _load_live_footfall()

    df = stops_df.merge(trips_df,    on="stop_id", how="left")
    df = df.merge(footfall_df, on="stop_id", how="left")
    df["trip_count"]     = df["trip_count"].fillna(0).astype(int)
    df["footfall_count"] = df["footfall_count"].fillna(0).astype(int)

    df["capacity"] = (
        df["station_type"]
        .map(TRAIN_TYPE_CAPACITY)
        .fillna(DEFAULT_CAPACITY)
        .astype(int)
    )

    # ── Raw signals ──────────────────────────────────────────────────
    df["daily_riders"]   = df["ridership_count"] / 365.0
    df["raw_ridership"]  = df["ridership_count"].astype(float)
    df["raw_uptake"]     = df.apply(
        lambda r: r["daily_riders"] / r["catchment_population"]
        if r["catchment_population"] > 0 and r["daily_riders"] > 0 else 0.0,
        axis=1,
    )
    df["raw_pressure"]   = df.apply(
        lambda r: r["daily_riders"] / (r["trip_count"] * r["capacity"])
        if r["daily_riders"] > 0 and r["trip_count"] > 0 else 0.0,
        axis=1,
    )
    df["raw_footfall"]   = df["footfall_count"].astype(float)

    # ── Global maxima (never 0) ──────────────────────────────────────
    max_ridership = float(df["raw_ridership"].max() or 1.0)
    max_uptake    = float(df["raw_uptake"].max()    or 1.0)
    max_pressure  = float(df["raw_pressure"].max()  or 1.0)
    max_footfall  = float(df["raw_footfall"].max()  or 1.0)
    max_trips     = float(df["trip_count"].max()    or 1.0)

    # ── Normalised signals (0-1) ─────────────────────────────────────
    df["norm_ridership"] = df["raw_ridership"] / max_ridership
    df["norm_uptake"]    = df["raw_uptake"]    / max_uptake
    df["norm_pressure"]  = df["raw_pressure"]  / max_pressure
    df["norm_footfall"]  = df["raw_footfall"]  / max_footfall

    # ── Weighted demand score ────────────────────────────────────────
    def _score(row: "pd.Series[float]") -> float:
        if row["ridership_count"] > 0:
            return (
                W_RIDERSHIP * row["norm_ridership"]
                + W_UPTAKE   * row["norm_uptake"]
                + W_PRESSURE * row["norm_pressure"]
                + W_FOOTFALL * row["norm_footfall"]
            )
        # Fallback: GTFS supply only, always < 0.5
        return (row["trip_count"] / max_trips) * 0.5

    df["demand_score"] = df.apply(_score, axis=1)

    # ── Persist ──────────────────────────────────────────────────────
    upsert_sql = text("""
        INSERT INTO backend.station_demand_scores (
            stop_id, name, lat, lon, trip_count, ridership_count, catchment_population,
            station_type, footfall_count,
            norm_ridership, norm_uptake, norm_pressure, norm_footfall,
            raw_pressure, max_pressure, demand_score, computed_at
        ) VALUES (
            :stop_id, :name, :lat, :lon, :trip_count, :ridership_count, :catchment_population,
            :station_type, :footfall_count,
            :norm_ridership, :norm_uptake, :norm_pressure, :norm_footfall,
            :raw_pressure, :max_pressure, :demand_score, NOW()
        )
        ON CONFLICT (stop_id) DO UPDATE SET
            name                 = EXCLUDED.name,
            lat                  = EXCLUDED.lat,
            lon                  = EXCLUDED.lon,
            trip_count           = EXCLUDED.trip_count,
            ridership_count      = EXCLUDED.ridership_count,
            catchment_population = EXCLUDED.catchment_population,
            station_type         = EXCLUDED.station_type,
            footfall_count       = EXCLUDED.footfall_count,
            norm_ridership       = EXCLUDED.norm_ridership,
            norm_uptake          = EXCLUDED.norm_uptake,
            norm_pressure        = EXCLUDED.norm_pressure,
            norm_footfall        = EXCLUDED.norm_footfall,
            raw_pressure         = EXCLUDED.raw_pressure,
            max_pressure         = EXCLUDED.max_pressure,
            demand_score         = EXCLUDED.demand_score,
            computed_at          = NOW()
    """)

    rows: list[dict] = []
    with engine.begin() as conn:
        for _, row in df.iterrows():
            record = {
                "stop_id":               str(row["stop_id"]),
                "name":                  str(row["name"]),
                "lat":                   float(row["lat"]),
                "lon":                   float(row["lon"]),
                "trip_count":            int(row["trip_count"]),
                "ridership_count":       int(row["ridership_count"]),
                "catchment_population":  int(row["catchment_population"]),
                "station_type":          str(row["station_type"]) if row["station_type"] else None,
                "footfall_count":        int(row["footfall_count"]),
                "norm_ridership":        float(row["norm_ridership"]),
                "norm_uptake":           float(row["norm_uptake"]),
                "norm_pressure":         float(row["norm_pressure"]),
                "norm_footfall":         float(row["norm_footfall"]),
                "raw_pressure":          float(row["raw_pressure"]),
                "max_pressure":          max_pressure,
                "demand_score":          float(row["demand_score"]),
            }
            conn.execute(upsert_sql, record)
            rows.append(record)

    logger.info("Upserted %d station demand scores to DB.", len(rows))
    return rows


def load_demand_scores_from_db() -> list[dict]:
    """Read latest demand scores from DB, ordered by score descending."""
    try:
        with engine.connect() as conn:
            df = pd.read_sql(
                text("SELECT * FROM backend.station_demand_scores ORDER BY demand_score DESC"),
                conn,
            )
        return df.to_dict(orient="records")
    except Exception:
        logger.warning("Could not read demand scores from DB — table may not exist yet.")
        return []
