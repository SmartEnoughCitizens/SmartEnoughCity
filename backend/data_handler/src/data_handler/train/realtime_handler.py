"""
Irish Rail Realtime API Handler.

Fetches live data from api.irishrail.ie:
- All stations (getAllStationsXML)
- Current running trains (getCurrentTrainsXML)
- Station arrival/departure data (getStationDataByCodeXML)
- Train movements/journey details (getTrainMovementsXML)

API Documentation: http://api.irishrail.ie/realtime/
"""

import enum
import logging
from datetime import date, datetime, time

import requests
import xmltodict
from sqlalchemy import delete, select, update
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from data_handler.db import SessionLocal
from data_handler.settings.api_settings import get_api_settings
from data_handler.train.models import (
    IrishRailCurrentTrain,
    IrishRailStation,
    IrishRailStationData,
    IrishRailTrainMovement,
    MovementLocationType,
    StationLocationType,
    StationType,
    StopType,
    TrainStatus,
)

logger = logging.getLogger(__name__)

# Station types: A=All, M=Mainline, S=Suburban, D=DART
STATION_TYPES = {"A": "All", "M": "Mainline", "S": "Suburban", "D": "DART"}


# ── Helper Functions ────────────────────────────────────────────────


def _safe_int(val: str | None) -> int | None:
    """Safely convert string to int, returning None on failure."""
    if val is None or str(val).strip() == "":
        return None
    try:
        return int(val)
    except (ValueError, TypeError):
        return None


def _safe_float(val: str | None) -> float | None:
    """Safely convert string to float, returning None on failure."""
    if val is None or str(val).strip() == "":
        return None
    try:
        return float(val)
    except (ValueError, TypeError):
        return None


def _safe_time(val: str | None) -> time | None:
    """Safely parse a time string like '10:35' or '10:35:00' to a time object."""
    if val is None or val.strip() == "":
        return None
    val = val.strip()
    parts = val.split(":")
    if len(parts) < 2:
        return None
    try:
        return time(
            int(parts[0]) % 24, int(parts[1]), int(parts[2]) if len(parts) > 2 else 0
        )
    except (ValueError, TypeError):
        return None


def _safe_date(val: str | None) -> date | None:
    """Safely parse a date string like '22 Jan 2026' to a date object."""
    if val is None or val.strip() == "":
        return None
    try:
        return datetime.strptime(val.strip(), "%d %b %Y").date()
    except (ValueError, TypeError):
        return None


def _ensure_list(data: object) -> list:
    """Ensure data is always a list (XML parsing returns dict for single item)."""
    if data is None:
        return []
    if isinstance(data, dict):
        return [data]
    return list(data)


def _safe_enum(enum_cls: type[enum.Enum], val: str | None) -> enum.Enum | None:
    """Safely convert a string to an enum member, returning None on failure."""
    if val is None or val.strip() == "":
        return None
    try:
        return enum_cls(val.strip())
    except (ValueError, KeyError):
        return None


# ── Parsing Helpers ─────────────────────────────────────────────────


def _parse_station_dict(s: dict) -> IrishRailStation:
    """Parse a station dict from XML into an IrishRailStation object."""
    return IrishRailStation(
        station_id=int(s["StationId"]),
        station_code=s["StationCode"].strip(),
        station_desc=s["StationDesc"].strip(),
        station_alias=(s.get("StationAlias") or "").strip() or None,
        station_type=None,  # Will be set by type-specific fetch
        lat=float(s["StationLatitude"]),
        lon=float(s["StationLongitude"]),
    )


def _parse_current_train_dict(t: dict, fetched_at: datetime) -> IrishRailCurrentTrain:
    """Parse a train dict from XML into an IrishRailCurrentTrain object."""
    return IrishRailCurrentTrain(
        train_code=(t.get("TrainCode") or "").strip(),
        train_date=_safe_date((t.get("TrainDate") or "").strip()),
        train_status=_safe_enum(TrainStatus, (t.get("TrainStatus") or "").strip()),
        train_type=(t.get("TrainType") or "").strip() or None,
        direction=(t.get("Direction") or "").strip() or None,
        lat=_safe_float(t.get("TrainLatitude")),
        lon=_safe_float(t.get("TrainLongitude")),
        public_message=(t.get("PublicMessage") or "").strip() or None,
        fetched_at=fetched_at,
    )


def _parse_station_data_dict(
    a: dict, station_code: str, fetched_at: datetime
) -> IrishRailStationData:
    """Parse a station data dict from XML into an IrishRailStationData object."""
    return IrishRailStationData(
        station_code=station_code,
        train_code=(a.get("Traincode") or "").strip(),
        train_date=_safe_date((a.get("Traindate") or "").strip()),
        train_type=(a.get("Traintype") or "").strip() or None,
        origin=(a.get("Origin") or "").strip(),
        destination=(a.get("Destination") or "").strip(),
        origin_time=_safe_time((a.get("Origintime") or "").strip()),
        destination_time=_safe_time((a.get("Destinationtime") or "").strip()),
        status=(a.get("Status") or "").strip() or None,
        last_location=(a.get("Lastlocation") or "").strip() or None,
        due_in_minutes=_safe_int(a.get("Duein")),
        late_minutes=_safe_int(a.get("Late")),
        exp_arrival=_safe_time((a.get("Exparrival") or "").strip()),
        exp_depart=_safe_time((a.get("Expdepart") or "").strip()),
        sch_arrival=_safe_time((a.get("Scharrival") or "").strip()),
        sch_depart=_safe_time((a.get("Schdepart") or "").strip()),
        direction=(a.get("Direction") or "").strip() or None,
        location_type=_safe_enum(
            StationLocationType, (a.get("Locationtype") or "").strip()
        ),
        fetched_at=fetched_at,
    )


def _parse_train_movement_dict(m: dict, fetched_at: datetime) -> IrishRailTrainMovement:
    """Parse a movement dict from XML into an IrishRailTrainMovement object."""
    return IrishRailTrainMovement(
        train_code=(m.get("TrainCode") or "").strip(),
        train_date=_safe_date((m.get("TrainDate") or "").strip()),
        location_code=(m.get("LocationCode") or "").strip(),
        location_full_name=(m.get("LocationFullName") or "").strip(),
        location_order=int(m.get("LocationOrder", 0)),
        location_type=_safe_enum(
            MovementLocationType, (m.get("LocationType") or "").strip()
        ),
        train_origin=(m.get("TrainOrigin") or "").strip(),
        train_destination=(m.get("TrainDestination") or "").strip(),
        scheduled_arrival=_safe_time((m.get("ScheduledArrival") or "").strip()),
        scheduled_departure=_safe_time((m.get("ScheduledDeparture") or "").strip()),
        actual_arrival=_safe_time((m.get("Arrival") or "").strip()),
        actual_departure=_safe_time((m.get("Departure") or "").strip()),
        auto_arrival=m.get("AutoArrival") == "1" if m.get("AutoArrival") else None,
        auto_depart=m.get("AutoDepart") == "1" if m.get("AutoDepart") else None,
        stop_type=_safe_enum(StopType, (m.get("StopType") or "").strip()),
        fetched_at=fetched_at,
    )


# ── Station Functions ───────────────────────────────────────────────


def fetch_all_stations(station_type: str = "A") -> list[dict]:
    """
    Fetch all stations from Irish Rail API.

    Args:
        station_type: A=All, M=Mainline, S=Suburban, D=DART

    Returns:
        List of station dicts with keys:
        StationDesc, StationCode, StationId, StationAlias, StationLatitude, StationLongitude
    """
    url = f"{get_api_settings().irish_rail_base_url}/getAllStationsXML_WithStationType?StationType={station_type}"
    logger.info("Fetching stations from %s", url)

    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
    except requests.RequestException:
        logger.exception("Failed to fetch stations")
        return []

    try:
        doc = xmltodict.parse(response.text)
        stations = doc.get("ArrayOfObjStation", {}).get("objStation", [])
        return _ensure_list(stations)
    except Exception:
        logger.exception("Failed to parse stations XML")
        return []


def irish_rail_stations_to_db() -> None:
    """Fetch all stations and upsert to database."""
    logger.info("Loading Irish Rail stations to database...")

    stations_data = fetch_all_stations("A")
    if not stations_data:
        logger.warning("No stations fetched from API.")
        return

    session = SessionLocal()
    try:
        # Build station objects
        stations = [_parse_station_dict(s) for s in stations_data]

        # Upsert using SQLAlchemy's on_conflict_do_update
        for station in stations:
            stmt = (
                pg_insert(IrishRailStation)
                .values(
                    station_id=station.station_id,
                    station_code=station.station_code,
                    station_desc=station.station_desc,
                    station_alias=station.station_alias,
                    station_type=station.station_type,
                    lat=station.lat,
                    lon=station.lon,
                )
                .on_conflict_do_update(
                    index_elements=["station_id"],
                    set_={
                        "station_code": station.station_code,
                        "station_desc": station.station_desc,
                        "station_alias": station.station_alias,
                        "lat": station.lat,
                        "lon": station.lon,
                    },
                )
            )
            session.execute(stmt)

        session.commit()
        logger.info("Inserted/updated %d Irish Rail stations.", len(stations))

    except Exception:
        session.rollback()
        logger.exception("Error inserting stations")

    finally:
        session.close()

    # Now update station types
    _update_station_types()


def _update_station_types() -> None:
    """Update station types by fetching each type separately."""
    session = SessionLocal()

    try:
        for type_code in ["M", "S", "D"]:
            stations = fetch_all_stations(type_code)
            if not stations:
                continue

            station_codes = [s["StationCode"].strip() for s in stations]
            if station_codes:
                stmt = (
                    update(IrishRailStation)
                    .where(IrishRailStation.station_code.in_(station_codes))
                    .values(station_type=StationType(type_code))
                )
                session.execute(stmt)
                logger.info(
                    "Updated %d stations with type %s.", len(station_codes), type_code
                )

        session.commit()

    except Exception:
        session.rollback()
        logger.exception("Error updating station types")

    finally:
        session.close()


# ── Current Trains Functions ────────────────────────────────────────


def fetch_current_trains(train_type: str = "A") -> list[dict]:
    """
    Fetch currently running trains from Irish Rail API.

    Args:
        train_type: A=All, M=Mainline, S=Suburban, D=DART

    Returns:
        List of train dicts with keys:
        TrainStatus, TrainLatitude, TrainLongitude, TrainCode, TrainDate,
        PublicMessage, Direction
    """
    url = f"{get_api_settings().irish_rail_base_url}/getCurrentTrainsXML_WithTrainType?TrainType={train_type}"
    logger.info("Fetching current trains from %s", url)

    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
    except requests.RequestException:
        logger.exception("Failed to fetch current trains")
        return []

    try:
        doc = xmltodict.parse(response.text)
        trains = doc.get("ArrayOfObjTrainPositions", {}).get("objTrainPositions", [])
        return _ensure_list(trains)
    except Exception:
        logger.exception("Failed to parse trains XML")
        return []


def irish_rail_current_trains_to_db() -> None:
    """Fetch current trains and store to database (replaces old data)."""
    logger.info("Loading Irish Rail current trains to database...")

    trains_data = fetch_current_trains("A")
    fetched_at = datetime.now()

    session = SessionLocal()
    try:
        # Clear old current train data
        session.execute(delete(IrishRailCurrentTrain))

        if not trains_data:
            logger.info("No current trains running.")
            session.commit()
            return

        trains = [_parse_current_train_dict(t, fetched_at) for t in trains_data]

        session.add_all(trains)
        session.commit()
        logger.info("Inserted %d current trains.", len(trains))

    except Exception:
        session.rollback()
        logger.exception("Error inserting current trains")

    finally:
        session.close()


# ── Station Data Functions ──────────────────────────────────────────


def fetch_station_data(station_code: str, num_mins: int = 90) -> list[dict]:
    """
    Fetch arrival/departure data for a station.

    Args:
        station_code: Station code (e.g., "BYSDE" for Bayside)
        num_mins: Number of minutes to look ahead (5-90)

    Returns:
        List of train arrival dicts
    """
    num_mins = max(5, min(90, num_mins))
    url = f"{get_api_settings().irish_rail_base_url}/getStationDataByCodeXML_WithNumMins?StationCode={station_code}&NumMins={num_mins}"
    logger.debug("Fetching station data from %s", url)

    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
    except requests.RequestException:
        logger.exception("Failed to fetch station data for %s", station_code)
        return []

    try:
        doc = xmltodict.parse(response.text)
        data = doc.get("ArrayOfObjStationData", {}).get("objStationData", [])
        return _ensure_list(data)
    except Exception:
        logger.exception("Failed to parse station data XML")
        return []


def _fetch_all_station_codes(session: Session) -> list[str]:
    """Get all station codes from the database."""
    result = session.execute(select(IrishRailStation.station_code))
    return [row[0] for row in result.fetchall()]


def irish_rail_station_data_to_db() -> None:
    """Fetch station data for all stations and store to database."""
    logger.info("Loading Irish Rail station data to database...")

    session = SessionLocal()
    fetched_at = datetime.now()

    try:
        station_codes = _fetch_all_station_codes(session)

        if not station_codes:
            logger.warning(
                "No stations in database. Run irish_rail_stations_to_db() first."
            )
            return

        # Clear old station data
        session.execute(delete(IrishRailStationData))

        all_data = []
        for station_code in station_codes:
            arrivals = fetch_station_data(station_code, num_mins=90)
            for a in arrivals:
                data = _parse_station_data_dict(a, station_code, fetched_at)
                all_data.append(data)

        if all_data:
            session.add_all(all_data)
            session.commit()
            logger.info("Inserted %d station data records.", len(all_data))
        else:
            session.commit()
            logger.info("No station data available.")

    except Exception:
        session.rollback()
        logger.exception("Error inserting station data")

    finally:
        session.close()


# ── Train Movements Functions ───────────────────────────────────────


def fetch_train_movements(train_code: str, train_date: str) -> list[dict]:
    """
    Fetch movement/journey details for a specific train.

    Args:
        train_code: Train code (e.g., "E109")
        train_date: Date string (e.g., "21 dec 2011")

    Returns:
        List of movement dicts with stop information
    """
    url = f"{get_api_settings().irish_rail_base_url}/getTrainMovementsXML?TrainId={train_code}&TrainDate={train_date}"
    logger.debug("Fetching train movements from %s", url)

    try:
        response = requests.get(url, timeout=30)
        response.raise_for_status()
    except requests.RequestException:
        logger.exception("Failed to fetch train movements for %s", train_code)
        return []

    try:
        doc = xmltodict.parse(response.text)
        movements = doc.get("ArrayOfObjTrainMovements", {}).get("objTrainMovements", [])
        return _ensure_list(movements)
    except Exception:
        logger.exception("Failed to parse train movements XML")
        return []


def _fetch_current_trains_from_db(session: Session) -> list[tuple[str, str]]:
    """Get train_code and train_date for all current trains from the database."""
    result = session.execute(
        select(IrishRailCurrentTrain.train_code, IrishRailCurrentTrain.train_date)
    )
    return [
        (
            row[0],
            row[1].strftime("%d %b %Y") if isinstance(row[1], date) else str(row[1]),
        )
        for row in result.fetchall()
    ]


def irish_rail_train_movements_to_db() -> None:
    """Fetch train movements for all current trains and store to database."""
    logger.info("Loading Irish Rail train movements to database...")

    session = SessionLocal()
    fetched_at = datetime.now()

    try:
        trains = _fetch_current_trains_from_db(session)

        if not trains:
            logger.warning(
                "No current trains in database. Run irish_rail_current_trains_to_db() first."
            )
            return

        # Clear old movement data
        session.execute(delete(IrishRailTrainMovement))

        all_movements = []
        for train_code, train_date in trains:
            movements = fetch_train_movements(train_code, train_date)
            for m in movements:
                movement = _parse_train_movement_dict(m, fetched_at)
                all_movements.append(movement)

        if all_movements:
            session.add_all(all_movements)
            session.commit()
            logger.info("Inserted %d train movement records.", len(all_movements))
        else:
            session.commit()
            logger.info("No train movements available.")

    except Exception:
        session.rollback()
        logger.exception("Error inserting train movements")

    finally:
        session.close()


# ── Combined Functions ──────────────────────────────────────────────


def irish_rail_realtime_to_db() -> None:
    """
    Fetch all Irish Rail realtime data and store to database.

    This includes:
    1. All stations
    2. Current running trains
    3. Station arrival/departure data for all stations
    4. Train movements for all current trains
    """
    logger.info("### Loading all Irish Rail realtime data...")
    irish_rail_stations_to_db()
    irish_rail_current_trains_to_db()
    irish_rail_station_data_to_db()
    irish_rail_train_movements_to_db()
    logger.info("### Completed loading Irish Rail realtime data.")
