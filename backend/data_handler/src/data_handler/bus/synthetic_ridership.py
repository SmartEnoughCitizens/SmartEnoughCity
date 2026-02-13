import logging
import math
import random

from sqlalchemy import text
from sqlalchemy.orm import Session

from data_handler.bus.models import BusRidership
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings

logger = logging.getLogger(__name__)

DEFAULT_VEHICLE_CAPACITY = 80


def _haversine_distance_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Approximate distance in km between two lat/lon points using the haversine formula."""
    r = 6371.0  # Earth radius in km
    d_lat = math.radians(lat2 - lat1)
    d_lon = math.radians(lon2 - lon1)
    a = (
        math.sin(d_lat / 2) ** 2
        + math.cos(math.radians(lat1))
        * math.cos(math.radians(lat2))
        * math.sin(d_lon / 2) ** 2
    )
    return r * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def find_nearest_stop_in_trip(
    session: Session,
    trip_id: str,
    vehicle_lat: float,
    vehicle_lon: float,
) -> tuple[str, int]:
    """
    Find the nearest stop to the vehicle position, constrained to the trip's stop sequence.

    Looks up the trip's stops from bus_stop_times joined with bus_stops (for lat/lon),
    computes haversine distance to each, and returns the closest one.

    Args:
        session: Active SQLAlchemy session.
        trip_id: The trip the vehicle is operating on.
        vehicle_lat: Vehicle latitude.
        vehicle_lon: Vehicle longitude.

    Returns:
        Tuple of (stop_id, stop_sequence) for the nearest stop.

    Raises:
        ValueError: If the trip has no stop times.
    """
    schema = get_db_settings().postgres_schema
    result = session.execute(
        text(
            f"SELECT st.stop_id, st.sequence, s.lat, s.lon "
            f"FROM {schema}.bus_stop_times st "
            f"JOIN {schema}.bus_stops s ON st.stop_id = s.id "
            f"WHERE st.trip_id = :trip_id "
            f"ORDER BY st.sequence"
        ),
        {"trip_id": trip_id},
    )
    stops = result.fetchall()

    if not stops:
        msg = f"No stop times found for trip_id={trip_id!r}"
        raise ValueError(msg)

    best_stop_id = stops[0][0]
    best_sequence = stops[0][1]
    best_distance = float("inf")

    for stop_id, sequence, stop_lat, stop_lon in stops:
        dist = _haversine_distance_km(vehicle_lat, vehicle_lon, stop_lat, stop_lon)
        if dist < best_distance:
            best_distance = dist
            best_stop_id = stop_id
            best_sequence = sequence

    return best_stop_id, best_sequence


def _time_of_day_multiplier(hour: int) -> float:
    """
    Return a load multiplier based on the time of day.

    Peak hours (07-09, 17-19) return higher multipliers.
    Off-peak returns moderate multipliers.
    Night returns low multipliers.
    """
    if 7 <= hour <= 9 or 17 <= hour <= 19:
        return random.uniform(0.55, 0.85)  # noqa: S311
    if 10 <= hour <= 16:
        return random.uniform(0.20, 0.45)  # noqa: S311
    if 20 <= hour <= 23 or 0 <= hour <= 5:
        return random.uniform(0.05, 0.15)  # noqa: S311
    # Shoulder hours (6, 16)
    return random.uniform(0.25, 0.50)  # noqa: S311


def _route_progress_adjustment(stop_sequence: int, total_stops: int) -> float:
    """
    Adjust load based on how far along the route the vehicle is.

    Peaks in the middle of the route (most passengers onboard mid-journey),
    lower at the start and end (boarding at start, alighting at end).
    """
    if total_stops <= 1:
        return 1.0
    progress = stop_sequence / total_stops
    # Bell curve peaking at ~0.5 progress
    return 0.5 + 0.5 * math.sin(progress * math.pi)


def _generate_passenger_counts(
    hour: int,
    stop_sequence: int,
    total_stops: int,
    capacity: int,
) -> tuple[int, int, int]:
    """
    Generate synthetic boarding, alighting, and onboard counts.

    Returns:
        Tuple of (passengers_boarding, passengers_alighting, passengers_onboard).
        All values clamped to valid ranges.
    """
    time_mult = _time_of_day_multiplier(hour)
    progress_mult = _route_progress_adjustment(stop_sequence, total_stops)

    target_load = capacity * time_mult * progress_mult
    target_load = max(0, min(capacity, target_load))

    # Add noise
    noise = random.gauss(0, capacity * 0.05)
    onboard = int(max(0, min(capacity, target_load + noise)))

    # Estimate boarding/alighting based on route progress
    progress = stop_sequence / max(total_stops, 1)
    boarding = int(max(0, onboard * (1 - progress) * random.uniform(0.1, 0.3)))  # noqa: S311
    alighting = int(max(0, onboard * progress * random.uniform(0.1, 0.3)))  # noqa: S311

    return boarding, alighting, onboard


def generate_ridership_for_vehicles(session: Session | None = None) -> None:
    """
    Generate synthetic ridership records for all recent live vehicle positions.

    For each record in bus_live_vehicles, determines the nearest stop in
    the trip's sequence and generates a synthetic passenger count based on
    time of day and route progress.

    Args:
        session: Optional SQLAlchemy session. If None, creates a new one.
    """
    owns_session = session is None
    if owns_session:
        session = SessionLocal()

    schema = get_db_settings().postgres_schema

    try:
        # Fetch latest position per vehicle (most recent timestamp)
        vehicles_result = session.execute(
            text(
                f"SELECT DISTINCT ON (vehicle_id) "
                f"vehicle_id, trip_id, lat, lon, timestamp "
                f"FROM {schema}.bus_live_vehicles "
                f"ORDER BY vehicle_id, timestamp DESC"
            )
        )
        vehicles = vehicles_result.fetchall()

        if not vehicles:
            logger.info("No live vehicles found, skipping ridership generation.")
            return

        rows: list[BusRidership] = []

        for vehicle_id, trip_id, lat, lon, ts in vehicles:
            # Get total stops for this trip
            total_stops_result = session.execute(
                text(
                    f"SELECT COUNT(*) FROM {schema}.bus_stop_times "
                    f"WHERE trip_id = :trip_id"
                ),
                {"trip_id": trip_id},
            )
            total_stops = total_stops_result.scalar() or 0

            if total_stops == 0:
                logger.warning(
                    "Trip %s has no stop_times, skipping ridership for vehicle %d.",
                    trip_id,
                    vehicle_id,
                )
                continue

            nearest_stop_id, stop_seq = find_nearest_stop_in_trip(
                session, trip_id, lat, lon
            )

            hour = ts.hour if hasattr(ts, "hour") else 12
            boarding, alighting, onboard = _generate_passenger_counts(
                hour, stop_seq, total_stops, DEFAULT_VEHICLE_CAPACITY
            )

            rows.append(
                BusRidership(
                    vehicle_id=vehicle_id,
                    trip_id=trip_id,
                    nearest_stop_id=nearest_stop_id,
                    stop_sequence=stop_seq,
                    timestamp=ts,
                    passengers_boarding=boarding,
                    passengers_alighting=alighting,
                    passengers_onboard=onboard,
                    vehicle_capacity=DEFAULT_VEHICLE_CAPACITY,
                )
            )

        if rows:
            session.add_all(rows)
            session.commit()
            logger.info("Persisted %d synthetic ridership record(s).", len(rows))

    except Exception:
        session.rollback()
        logger.exception("Failed to generate synthetic ridership data.")
        raise
    finally:
        if owns_session:
            session.close()
