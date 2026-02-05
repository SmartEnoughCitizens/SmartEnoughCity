import json
import logging
import zoneinfo
from datetime import datetime

from pydantic import BaseModel

from data_handler.bus.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time
from data_handler.bus.models import BusLiveVehicle, ScheduleRelationship
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


class VehiclePositionFeedHeader(BaseModel):
    gtfs_realtime_version: str
    incrementality: str
    timestamp: str | int


class VehiclePositionTrip(BaseModel):
    trip_id: str
    start_time: str
    start_date: str
    schedule_relationship: str
    route_id: str
    direction_id: int


class VehiclePositionPosition(BaseModel):
    latitude: float
    longitude: float


class VehiclePositionVehicleId(BaseModel):
    id: str | int


class VehiclePositionPayload(BaseModel):
    trip: VehiclePositionTrip
    position: VehiclePositionPosition
    timestamp: str | int
    vehicle: VehiclePositionVehicleId


class VehiclePositionEntity(BaseModel):
    id: str
    vehicle: VehiclePositionPayload


class VehiclePositionFeed(BaseModel):
    header: VehiclePositionFeedHeader
    entity: list[VehiclePositionEntity]


def _parse_schedule_relationship(value: str) -> ScheduleRelationship:
    normalized = value.strip().lower()
    try:
        return ScheduleRelationship(normalized)
    except ValueError:
        valid = [e.value for e in ScheduleRelationship]
        msg = f"Invalid schedule_relationship: {value!r}. Expected one of: {valid}."
        raise ValueError(
            msg
        ) from None


def _entity_to_live_vehicle(entity: VehiclePositionEntity) -> BusLiveVehicle:
    v = entity.vehicle
    trip = v.trip
    pos = v.position
    nested = v.vehicle

    trip_id = trip.trip_id.strip()
    if not trip_id:
        msg = "trip_id must be a non-empty string."
        raise ValueError(msg)

    start_time = parse_gtfs_time(trip.start_time)
    start_date = parse_gtfs_date(trip.start_date)
    schedule_relationship = _parse_schedule_relationship(trip.schedule_relationship)

    ts = v.timestamp
    if isinstance(ts, str):
        ts = int(ts)
    dublin_tz = zoneinfo.ZoneInfo("Europe/Dublin")
    timestamp_dt = datetime.fromtimestamp(ts, tz=dublin_tz)

    vehicle_id = nested.id if isinstance(nested.id, int) else int(nested.id)

    return BusLiveVehicle(
        vehicle_id=vehicle_id,
        trip_id=trip_id,
        start_time=start_time,
        start_date=start_date,
        schedule_relationship=schedule_relationship,
        direction_id=trip.direction_id,
        lat=pos.latitude,
        lon=pos.longitude,
        timestamp=timestamp_dt,
    )


def process_bus_vehicles_live_data(json_string: str) -> None:
    """
    Parses, validates, and persists bus live vehicle data JSON into bus_live_vehicles.

    Args:
        json_string: Raw JSON string from a bus live vehicle data feed.

    Raises:
        ValueError: If JSON is invalid or date/time/field parsing fails.
        ValidationError: If feed structure does not match (Pydantic validation).
    """
    try:
        feed = VehiclePositionFeed.model_validate_json(json_string)
    except json.JSONDecodeError as e:
        msg = "Invalid JSON"
        raise ValueError(msg) from e

    rows: list[BusLiveVehicle] = []
    for entity in feed.entity:
        rows.append(_entity_to_live_vehicle(entity))

    with SessionLocal() as session:
        try:
            session.add_all(rows)
            session.commit()
            logger.info("Persisted %d bus live vehicle record(s).", len(rows))
        except Exception:
            session.rollback()
            logger.exception("Failed to persist bus live vehicle data.")
            raise
        finally:
            session.close()


def process_bus_live_data(json_string: str) -> None:
    logger.info("Processing bus live data...")
    process_bus_vehicles_live_data(json_string)
