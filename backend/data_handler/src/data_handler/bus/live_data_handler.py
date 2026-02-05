import json
import logging
import zoneinfo
from datetime import datetime

import requests
from pydantic import BaseModel

from data_handler.bus.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time
from data_handler.bus.models import (
    BusLiveTripStopTimeUpdate,
    BusLiveTripUpdate,
    BusLiveVehicle,
    ScheduleRelationship,
)
from data_handler.db import SessionLocal
from data_handler.settings.api_settings import get_api_settings

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


class TripUpdateFeedHeader(BaseModel):
    gtfs_realtime_version: str
    incrementality: str
    timestamp: str | int


class TripUpdateTrip(BaseModel):
    trip_id: str
    start_time: str
    start_date: str
    schedule_relationship: str
    route_id: str
    direction_id: int


class StopTimeEvent(BaseModel):
    delay: int | None = None
    time: str | int | None = None
    uncertainty: int | None = None


class StopTimeUpdate(BaseModel):
    stop_sequence: int
    stop_id: str
    schedule_relationship: str
    arrival: StopTimeEvent | None = None
    departure: StopTimeEvent | None = None


class TripUpdateVehicle(BaseModel):
    id: str | int


class TripUpdatePayload(BaseModel):
    trip: TripUpdateTrip
    stop_time_update: list[StopTimeUpdate]
    vehicle: TripUpdateVehicle
    timestamp: str | int


class TripUpdateEntity(BaseModel):
    id: str
    trip_update: TripUpdatePayload


class TripUpdateFeed(BaseModel):
    header: TripUpdateFeedHeader
    entity: list[TripUpdateEntity]


def _parse_schedule_relationship(value: str) -> ScheduleRelationship:
    normalized = value.strip().lower()
    try:
        return ScheduleRelationship(normalized)
    except ValueError:
        valid = [e.value for e in ScheduleRelationship]
        msg = f"Invalid schedule_relationship: {value!r}. Expected one of: {valid}."
        raise ValueError(msg) from None


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


def _entity_to_live_trip_update(entity: TripUpdateEntity) -> BusLiveTripUpdate:
    tu = entity.trip_update
    trip = tu.trip

    trip_id = trip.trip_id.strip()
    if not trip_id:
        msg = "trip_id must be a non-empty string."
        raise ValueError(msg)

    start_time = parse_gtfs_time(trip.start_time)
    start_date = parse_gtfs_date(trip.start_date)
    schedule_relationship = _parse_schedule_relationship(trip.schedule_relationship)

    vehicle_id = tu.vehicle.id if isinstance(tu.vehicle.id, int) else int(tu.vehicle.id)

    ts = tu.timestamp
    if isinstance(ts, str):
        ts = int(ts)
    dublin_tz = zoneinfo.ZoneInfo("Europe/Dublin")
    timestamp_dt = datetime.fromtimestamp(ts, tz=dublin_tz)

    trip_update = BusLiveTripUpdate(
        trip_id=trip_id,
        start_time=start_time,
        start_date=start_date,
        schedule_relationship=schedule_relationship,
        direction_id=trip.direction_id,
        vehicle_id=vehicle_id,
        timestamp=timestamp_dt,
    )

    if tu.stop_time_update:
        for stu in tu.stop_time_update:
            arrival_delay = stu.arrival.delay if stu.arrival else None
            departure_delay = stu.departure.delay if stu.departure else None
            if arrival_delay is None and departure_delay is None:
                continue
            stop_schedule_rel = _parse_schedule_relationship(stu.schedule_relationship)
            trip_update.stop_time_updates.append(
                BusLiveTripStopTimeUpdate(
                    stop_id=stu.stop_id.strip(),
                    stop_sequence=stu.stop_sequence,
                    schedule_relationship=stop_schedule_rel,
                    arrival_delay=arrival_delay,
                    departure_delay=departure_delay,
                )
            )

    return trip_update


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


def process_bus_trip_updates_live_data(json_string: str) -> None:
    """
    Parses, validates, and persists bus live trip update data JSON into bus_live_trip_updates.

    Args:
        json_string: Raw JSON string from a bus live trip updates feed.

    Raises:
        ValueError: If JSON is invalid or date/time/field parsing fails.
        ValidationError: If feed structure does not match (Pydantic validation).
    """
    try:
        feed = TripUpdateFeed.model_validate_json(json_string)
    except json.JSONDecodeError as e:
        msg = "Invalid JSON"
        raise ValueError(msg) from e

    rows: list[BusLiveTripUpdate] = []
    for entity in feed.entity:
        rows.append(_entity_to_live_trip_update(entity))

    with SessionLocal() as session:
        try:
            session.add_all(rows)
            session.commit()
            logger.info("Persisted %d bus live trip update record(s).", len(rows))
        except Exception:
            session.rollback()
            logger.exception("Failed to persist bus live trip update data.")
            raise
        finally:
            session.close()


def process_bus_live_data() -> None:
    api_settings = get_api_settings()
    headers = {"x-api-key": api_settings.gtfs_api_key}
    vehicles_url = f"{api_settings.gtfs_api_base_url}/Vehicles?format=json"
    trip_updates_url = f"{api_settings.gtfs_api_base_url}/TripUpdates?format=json"

    logger.info("Fetching bus live vehicles data...")
    vehicles_response = requests.get(vehicles_url, headers=headers)
    vehicles_response.raise_for_status()
    process_bus_vehicles_live_data(vehicles_response.text)

    logger.info("Fetching bus live trip updates data...")
    trip_updates_response = requests.get(trip_updates_url, headers=headers)
    trip_updates_response.raise_for_status()
    process_bus_trip_updates_live_data(trip_updates_response.text)
