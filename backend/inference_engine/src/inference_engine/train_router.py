"""Train utilisation, demand scoring, and simulation API endpoints."""

import json  # noqa: I001
import logging
import math
import time
from typing import Any

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from sqlalchemy import text

from inference_engine.db import engine
from inference_engine.indicators.train.train_demand import (
    W_FOOTFALL,
    W_PRESSURE,
    W_RIDERSHIP,
    W_UPTAKE,
    TRAIN_TYPE_CAPACITY,
    DEFAULT_CAPACITY,
    compute_and_save_demand_scores,
    load_demand_scores_from_db,
)
from inference_engine.indicators.train.train_simulation import (
    fetch_pending_recommendation,
    run_simulation,
    store_simulation,
)
from inference_engine.indicators.train.train_utilisation import (
    _TOTAL_CAPACITY,
    build_utilisation_json,
    compute_utilisation,
    distribute_ridership_weighted,
    load_stop_times_with_stops,
    load_train_station_ridership,
    predict_ridership_2025,
    process_stop_times,
    save_recommendation_to_db,
)

router = APIRouter(prefix="/train", tags=["Train"])
logger = logging.getLogger(__name__)

# ── Utilisation cache (headsign pipeline — expensive, annual data) ────
_UTILISATION_CACHE: dict = {"headsigns": None, "timestamp": 0.0}
_CACHE_TTL_SECONDS = 6 * 60 * 60  # 6 hours

# ── Simulation sensitivity ────────────────────────────────────────────
# Controls how aggressively added trains reduce pressure in simulation.
# relief_ratio = extra / trip_count  (fractional service increase, no cap)
# pressure_factor = exp(-relief_ratio * SENSITIVITY)
# At SENSITIVITY=13: adding 20 trains to a 509-trip corridor → ~10% score improvement.
# Smaller corridors benefit more per added train (appropriate for planning).
_PRESSURE_SENSITIVITY = 13.0

# ── Simulation request/response models ───────────────────────────────


class CorridorInput(BaseModel):
    origin_stop_id: str
    destination_stop_id: str
    train_count: int = 1  # 1-20 extra services to add


class SimulateRequest(BaseModel):
    corridors: list[CorridorInput]  # max 3


class StationDemandOut(BaseModel):
    stop_id: str
    name: str
    lat: float
    lon: float
    trip_count: int
    ridership_count: int
    catchment_population: int
    station_type: str | None
    footfall_count: int
    norm_ridership: float
    norm_uptake: float
    norm_pressure: float
    norm_footfall: float
    raw_pressure: float
    max_pressure: float
    demand_score: float


class SimulateResponse(BaseModel):
    base_demand: list[StationDemandOut]
    simulated_demand: list[StationDemandOut]
    affected_stop_ids: list[str]


def _simulate_stop(s: dict, extra: int) -> dict:
    """
    Recompute demand score for one stop after adding `extra` daily services.

    Pressure decays exponentially with relief_ratio = extra / trip_count.
    Smaller corridors benefit more per added train; adding 20 trains to a
    509-trip corridor gives ~10% score improvement (SENSITIVITY = 13).
    """
    capacity = TRAIN_TYPE_CAPACITY.get(s.get("station_type") or "", DEFAULT_CAPACITY)
    new_trips = s["trip_count"] + extra
    daily_riders = s["ridership_count"] / 365.0
    # relief_ratio = fractional service increase (no cap — avoids all stations
    # hitting the same maximum when daily_riders << extra_capacity)
    relief_ratio = extra / max(s["trip_count"], 1)
    pressure_factor = math.exp(-relief_ratio * _PRESSURE_SENSITIVITY)
    new_norm_pres = s["norm_pressure"] * pressure_factor

    if s["ridership_count"] > 0:
        new_score = (
            W_RIDERSHIP * s["norm_ridership"]
            + W_UPTAKE * s["norm_uptake"]
            + W_PRESSURE * new_norm_pres
            + W_FOOTFALL * s["norm_footfall"]
        )
        new_raw = daily_riders / (new_trips * capacity) if new_trips > 0 else 0.0
    else:
        new_score = s["demand_score"] * pressure_factor
        new_raw = 0.0

    return {
        **s,
        "trip_count": new_trips,
        "norm_pressure": round(new_norm_pres, 6),
        "raw_pressure": round(new_raw, 6),
        "demand_score": round(new_score, 6),
    }


def _load_stop_coords() -> dict[str, dict]:
    """Load lat/lon for all train stops keyed by stop name."""
    query = text("SELECT name, lat, lon FROM external_data.train_stops")
    with engine.connect() as conn:
        rows = conn.execute(query).fetchall()
    return {row.name: {"lat": float(row.lat), "lon": float(row.lon)} for row in rows}


def _load_ordered_stop_sequence() -> dict[str, list[str]]:
    """
    Return ordered unique stop names per headsign, preserving route sequence
    by ordering on entry_id and forward-filling the headsign column.
    """
    query = text("""
        SELECT tst.entry_id, tst.headsign, ts.name
        FROM external_data.train_stop_times tst
        INNER JOIN external_data.train_stops ts ON tst.stop_id = ts.id
        ORDER BY tst.entry_id
    """)
    with engine.connect() as conn:
        rows = conn.execute(query).fetchall()

    sequence: dict[str, list[str]] = {}
    current_headsign: str | None = None
    for row in rows:
        if row.headsign:
            current_headsign = row.headsign
        if current_headsign:
            if current_headsign not in sequence:
                sequence[current_headsign] = []
            if row.name not in sequence[current_headsign]:
                sequence[current_headsign].append(row.name)

    return sequence


def _fetch_today_recommendation() -> list | None:
    """
    Return today's utilisation recommendation JSON from the DB, or None if not computed yet.
    Used to skip the heavy pipeline on repeat requests within the same day.
    """
    query = text("""
        SELECT recommendation
        FROM backend.recommendations
        WHERE usecase = 'utilisation_train'
          AND indicator = 'Train'
          AND created_at >= CURRENT_DATE
        ORDER BY created_at DESC
        LIMIT 1
    """)
    with engine.connect() as conn:
        row = conn.execute(query).fetchone()
    if row is None:
        return None
    try:
        rec = row.recommendation
        # recommendation column is jsonb — may come back as dict/list already
        if isinstance(rec, list):
            return rec
        if isinstance(rec, str):
            return json.loads(rec)
        return list(rec)
    except Exception:  # noqa: BLE001
        logger.warning("Could not parse cached recommendation JSON.")
        return None


def _fetch_latest_simulation() -> dict | None:
    """Return the most recently stored simulation JSON from the DB, or None."""
    query = text("""
        SELECT simulation
        FROM backend.recommendations
        WHERE usecase = 'utilisation_train'
          AND indicator = 'Train'
          AND simulation IS NOT NULL
          AND simulation != ''
        ORDER BY created_at DESC
        LIMIT 1
    """)
    with engine.connect() as conn:
        row = conn.execute(query).fetchone()
    if row is None:
        return None
    try:
        return json.loads(row.simulation)
    except Exception:  # noqa: BLE001
        logger.warning("Could not parse stored simulation JSON.")
        return None


def _build_headsigns_response(utilisation_json: list, result_df: Any) -> list:  # noqa: ANN401
    """Build the headsigns list with per-station coords and utilisation ratios."""
    coords = _load_stop_coords()
    stop_sequence = _load_ordered_stop_sequence()

    headsigns = []
    for item in utilisation_json:
        headsign = item["Train Name"]
        attrs = item["Attributes"]

        headsign_df = result_df[result_df["headsign"] == headsign]
        util_by_station = {
            row["name"]: float(row["weighted_distributed_count"]) / _TOTAL_CAPACITY
            for _, row in headsign_df.iterrows()
        }

        ordered_names = stop_sequence.get(headsign, list(util_by_station.keys()))

        stations = []
        for station_name in ordered_names:
            coord = coords.get(station_name)
            util_ratio = util_by_station.get(station_name, 0.0)
            if coord and util_ratio > 0:
                stations.append(
                    {
                        "name": station_name,
                        "lat": coord["lat"],
                        "lon": coord["lon"],
                        "utilisation_ratio": round(min(util_ratio, 2.0), 3),
                    }
                )

        headsigns.append(
            {
                "headsign": headsign,
                "status": attrs["status"],
                "current_count": attrs["Current_count"],
                "predicted_count": attrs["Predicted_count"],
                "recommendation": attrs["Recommendation"],
                "stations": stations,
            }
        )

    return headsigns


def warm_demand_cache() -> None:
    """
    Compute and persist demand scores on startup.
    Subsequent API requests read directly from the pre-computed DB table.
    """
    try:
        logger.info("Computing train demand scores on startup...")
        count = len(compute_and_save_demand_scores())
        logger.info("Train demand scores saved for %d stations.", count)
    except Exception:
        logger.exception("Failed to compute demand scores on startup.")


@router.get("/demand")
def get_demand() -> list[dict]:
    """
    Return per-station demand scores for all Dublin train stops.
    Reads directly from the pre-computed backend.station_demand_scores table.
    """
    try:
        return load_demand_scores_from_db()
    except Exception as e:
        logger.exception("Error in get_demand")
        raise HTTPException(status_code=500, detail=str(e)) from e


@router.post("/demand/simulate")
def simulate_demand(request: SimulateRequest) -> SimulateResponse:
    """
    Simulate adding extra train services on up to 3 corridors.

    Only capacity pressure changes when you add trains — ridership, local uptake,
    and footfall are assumed unchanged (riders don't instantly shift routes).
    Uses the stored max_pressure so simulated scores live on the same 0-1 scale
    as base scores and can be compared directly.

    A lower score on affected stops = better (trains are less full).
    """
    try:
        base: list[dict] = load_demand_scores_from_db()
        if not request.corridors:
            return SimulateResponse(
                base_demand=[StationDemandOut(**s) for s in base],
                simulated_demand=[StationDemandOut(**s) for s in base],
                affected_stop_ids=[],
            )

        # Build route-stop index: routeId → ordered list of stopIds
        route_stop_sql = text("""
            SELECT r.id AS route_id, s.id AS stop_id, st.sequence AS seq
            FROM (
                SELECT DISTINCT ON (route_id) id, route_id
                FROM external_data.train_trips
                WHERE direction_id = 0
                ORDER BY route_id, id
            ) t
            JOIN external_data.train_routes r  ON r.id  = t.route_id
            JOIN external_data.train_stop_times st ON st.trip_id = t.id
            JOIN external_data.train_stops      s  ON s.id  = st.stop_id
            ORDER BY r.id, st.sequence
        """)
        with engine.connect() as conn:
            rows = conn.execute(route_stop_sql).fetchall()

        # route_id → sorted list of stop_ids
        route_stops: dict[str, list[str]] = {}
        for row in rows:
            route_stops.setdefault(row.route_id, []).append(row.stop_id)

        # extra trips per stop accumulated across corridors
        extra_trips: dict[str, int] = {}
        max_corridors = min(3, len(request.corridors))
        for corridor in request.corridors[:max_corridors]:
            origin = corridor.origin_stop_id
            dest = corridor.destination_stop_id
            count = max(1, min(20, corridor.train_count))

            corridor_stops: set[str] = set()
            for stop_list in route_stops.values():
                try:
                    i = stop_list.index(origin)
                    j = stop_list.index(dest)
                    from_idx, to_idx = min(i, j), max(i, j)
                    corridor_stops.update(stop_list[from_idx : to_idx + 1])
                except ValueError:
                    pass
            # fallback: mark at least the two endpoints
            if not corridor_stops:
                corridor_stops = {origin, dest}

            for stop_id in corridor_stops:
                extra_trips[stop_id] = extra_trips.get(stop_id, 0) + count

        # Recompute scores for affected stops only; everything else unchanged
        stop_map = {s["stop_id"]: dict(s) for s in base}
        affected_ids = list(extra_trips.keys())

        for stop_id, extra in extra_trips.items():
            if stop_id not in stop_map:
                continue
            stop_map[stop_id] = _simulate_stop(stop_map[stop_id], extra)

        simulated = list(stop_map.values())

        return SimulateResponse(
            base_demand=[StationDemandOut(**s) for s in base],
            simulated_demand=[StationDemandOut(**s) for s in simulated],
            affected_stop_ids=affected_ids,
        )

    except Exception as e:
        logger.exception("Error in simulate_demand")
        raise HTTPException(status_code=500, detail=str(e)) from e


def warm_utilisation_cache() -> None:
    """
    Pre-compute and cache the utilisation pipeline result.
    Called at server startup so the first HTTP request is instant.
    """
    try:
        logger.info("Pre-warming train utilisation cache...")
        stop_times_df = load_stop_times_with_stops()
        ridership_df = load_train_station_ridership()
        _, unique_combinations_df = process_stop_times(stop_times_df)
        predicted_df = predict_ridership_2025(ridership_df)
        result_df = distribute_ridership_weighted(
            unique_combinations_df, ridership_df, predicted_df
        )
        utilisation_df = compute_utilisation(result_df)
        utilisation_json = build_utilisation_json(utilisation_df)
        save_recommendation_to_db(utilisation_json)

        headsigns = _build_headsigns_response(utilisation_json, result_df)
        _UTILISATION_CACHE["headsigns"] = headsigns
        _UTILISATION_CACHE["timestamp"] = time.time()
        logger.info("Train utilisation cache warmed with %d headsigns.", len(headsigns))
    except Exception:
        logger.exception(
            "Failed to warm train utilisation cache -- will compute on first request."
        )


@router.get("/utilisation")
def get_train_utilisation() -> dict:
    """
    Return headsign utilisation data with per-station corridor info.
    In-process cache (6 hours) avoids re-running the heavy pipeline on every request.
    """
    try:
        now = time.time()
        if (
            _UTILISATION_CACHE["headsigns"] is not None
            and (now - _UTILISATION_CACHE["timestamp"]) < _CACHE_TTL_SECONDS
        ):
            logger.info("Returning in-process cached utilisation data.")
            simulation = _fetch_latest_simulation()
            return {
                "headsigns": _UTILISATION_CACHE["headsigns"],
                "simulation": simulation,
            }

        logger.info("Cache miss — running utilisation pipeline.")
        stop_times_df = load_stop_times_with_stops()
        ridership_df = load_train_station_ridership()
        _, unique_combinations_df = process_stop_times(stop_times_df)
        predicted_df = predict_ridership_2025(ridership_df)
        result_df = distribute_ridership_weighted(
            unique_combinations_df, ridership_df, predicted_df
        )
        utilisation_df = compute_utilisation(result_df)
        utilisation_json = build_utilisation_json(utilisation_df)
        save_recommendation_to_db(utilisation_json)

        headsigns = _build_headsigns_response(utilisation_json, result_df)
        _UTILISATION_CACHE["headsigns"] = headsigns
        _UTILISATION_CACHE["timestamp"] = now

        simulation = _fetch_latest_simulation()
    except Exception as e:
        logger.exception("Error in get_train_utilisation")
        raise HTTPException(status_code=500, detail=str(e)) from e
    else:
        return {"headsigns": headsigns, "simulation": simulation}


@router.post("/utilisation/simulate")
def run_train_simulation() -> dict:
    """
    Trigger Gemini simulation for the latest pending train utilisation recommendation.
    If no pending recommendation exists, returns the most recently stored result.
    """
    try:
        result = fetch_pending_recommendation()
        if result is None:
            simulation = _fetch_latest_simulation()
            return {
                "simulation": simulation,
                "already_simulated": simulation is not None,
            }

        rec_id, recommendation = result
        simulation_output = run_simulation(recommendation)
        store_simulation(rec_id, simulation_output)

        try:
            parsed = json.loads(simulation_output)
        except Exception:  # noqa: BLE001
            parsed = {"raw": simulation_output}

    except Exception as e:
        logger.exception("Error in run_train_simulation")
        raise HTTPException(status_code=500, detail=str(e)) from e
    else:
        return {"simulation": parsed, "already_simulated": False}
