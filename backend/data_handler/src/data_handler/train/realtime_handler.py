"""
Irish Rail Realtime API Handler.

Fetches live data from api.irishrail.ie:
- All stations (getAllStationsXML)
- Current running trains (getCurrentTrainsXML)
- Station arrival/departure data (getStationDataByCodeXML)
- Train movements/journey details (getTrainMovementsXML)

API Documentation: http://api.irishrail.ie/realtime/
"""

import logging
from datetime import datetime

import requests
import xmltodict
from sqlalchemy import delete, text

from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings
from data_handler.train.models import (
    IrishRailCurrentTrain,
    IrishRailStation,
    IrishRailStationData,
    IrishRailTrainMovement,
)

logger = logging.getLogger(__name__)

BASE_URL = "http://api.irishrail.ie/realtime/realtime.asmx"

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


def _ensure_list(data: object) -> list:
    """Ensure data is always a list (XML parsing returns dict for single item)."""
    if data is None:
        return []
    if isinstance(data, dict):
        return [data]
    return list(data)


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
    url = f"{BASE_URL}/getAllStationsXML_WithStationType?StationType={station_type}"
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
        stations = []
        for s in stations_data:
            station = IrishRailStation(
                station_id=int(s["StationId"]),
                station_code=s["StationCode"].strip(),
                station_desc=s["StationDesc"].strip(),
                station_alias=(s.get("StationAlias") or "").strip() or None,
                station_type=None,  # Will be set by type-specific fetch
                lat=float(s["StationLatitude"]),
                lon=float(s["StationLongitude"]),
            )
            stations.append(station)

        # Upsert using raw SQL for ON CONFLICT
        schema = get_db_settings().postgres_schema
        insert_sql = f"""
        INSERT INTO {schema}.irish_rail_stations
            (station_id, station_code, station_desc, station_alias, station_type, lat, lon)
        VALUES
            (:station_id, :station_code, :station_desc, :station_alias, :station_type, :lat, :lon)
        ON CONFLICT (station_id)
        DO UPDATE SET
            station_code = EXCLUDED.station_code,
            station_desc = EXCLUDED.station_desc,
            station_alias = EXCLUDED.station_alias,
            lat = EXCLUDED.lat,
            lon = EXCLUDED.lon;
        """

        records = [
            {
                "station_id": s.station_id,
                "station_code": s.station_code,
                "station_desc": s.station_desc,
                "station_alias": s.station_alias,
                "station_type": s.station_type,
                "lat": s.lat,
                "lon": s.lon,
            }
            for s in stations
        ]

        session.execute(text(insert_sql), records)
        session.commit()
        logger.info("Inserted/updated %d Irish Rail stations.", len(records))

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
    schema = get_db_settings().postgres_schema

    try:
        for type_code in ["M", "S", "D"]:
            stations = fetch_all_stations(type_code)
            if not stations:
                continue

            station_codes = [s["StationCode"].strip() for s in stations]
            if station_codes:
                # Update type for these stations
                update_sql = f"""
                UPDATE {schema}.irish_rail_stations
                SET station_type = :station_type
                WHERE station_code = ANY(:codes);
                """
                session.execute(
                    text(update_sql),
                    {"station_type": type_code, "codes": station_codes}
                )
                logger.info("Updated %d stations with type %s.", len(station_codes), type_code)

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
    url = f"{BASE_URL}/getCurrentTrainsXML_WithTrainType?TrainType={train_type}"
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

        trains = []
        for t in trains_data:
            train = IrishRailCurrentTrain(
                train_code=(t.get("TrainCode") or "").strip(),
                train_date=(t.get("TrainDate") or "").strip(),
                train_status=(t.get("TrainStatus") or "").strip(),
                train_type=(t.get("TrainType") or "").strip() or None,
                direction=(t.get("Direction") or "").strip() or None,
                lat=_safe_float(t.get("TrainLatitude")),
                lon=_safe_float(t.get("TrainLongitude")),
                public_message=(t.get("PublicMessage") or "").strip() or None,
                fetched_at=fetched_at,
            )
            trains.append(train)

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
    url = f"{BASE_URL}/getStationDataByCodeXML_WithNumMins?StationCode={station_code}&NumMins={num_mins}"
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


def irish_rail_station_data_to_db() -> None:
    """Fetch station data for all stations and store to database."""
    logger.info("Loading Irish Rail station data to database...")

    session = SessionLocal()
    fetched_at = datetime.now()

    try:
        # Get all station codes from database
        schema = get_db_settings().postgres_schema
        result = session.execute(
            text(f"SELECT station_code FROM {schema}.irish_rail_stations")
        )
        station_codes = [row[0] for row in result.fetchall()]

        if not station_codes:
            logger.warning("No stations in database. Run irish_rail_stations_to_db() first.")
            return

        # Clear old station data
        session.execute(delete(IrishRailStationData))

        all_data = []
        for station_code in station_codes:
            arrivals = fetch_station_data(station_code, num_mins=90)
            for a in arrivals:
                data = IrishRailStationData(
                    station_code=station_code,
                    train_code=(a.get("Traincode") or "").strip(),
                    train_date=(a.get("Traindate") or "").strip(),
                    train_type=(a.get("Traintype") or "").strip() or None,
                    origin=(a.get("Origin") or "").strip(),
                    destination=(a.get("Destination") or "").strip(),
                    origin_time=(a.get("Origintime") or "").strip() or None,
                    destination_time=(a.get("Destinationtime") or "").strip() or None,
                    status=(a.get("Status") or "").strip() or None,
                    last_location=(a.get("Lastlocation") or "").strip() or None,
                    due_in=_safe_int(a.get("Duein")),
                    late=_safe_int(a.get("Late")),
                    exp_arrival=(a.get("Exparrival") or "").strip() or None,
                    exp_depart=(a.get("Expdepart") or "").strip() or None,
                    sch_arrival=(a.get("Scharrival") or "").strip() or None,
                    sch_depart=(a.get("Schdepart") or "").strip() or None,
                    direction=(a.get("Direction") or "").strip() or None,
                    location_type=(a.get("Locationtype") or "").strip() or None,
                    fetched_at=fetched_at,
                )
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
    url = f"{BASE_URL}/getTrainMovementsXML?TrainId={train_code}&TrainDate={train_date}"
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


def irish_rail_train_movements_to_db() -> None:
    """Fetch train movements for all current trains and store to database."""
    logger.info("Loading Irish Rail train movements to database...")

    session = SessionLocal()
    fetched_at = datetime.now()

    try:
        # Get current trains from database
        schema = get_db_settings().postgres_schema
        result = session.execute(
            text(f"SELECT train_code, train_date FROM {schema}.irish_rail_current_trains")
        )
        trains = [(row[0], row[1]) for row in result.fetchall()]

        if not trains:
            logger.warning("No current trains in database. Run irish_rail_current_trains_to_db() first.")
            return

        # Clear old movement data
        session.execute(delete(IrishRailTrainMovement))

        all_movements = []
        for train_code, train_date in trains:
            movements = fetch_train_movements(train_code, train_date)
            for m in movements:
                movement = IrishRailTrainMovement(
                    train_code=(m.get("TrainCode") or "").strip(),
                    train_date=(m.get("TrainDate") or "").strip(),
                    location_code=(m.get("LocationCode") or "").strip(),
                    location_full_name=(m.get("LocationFullName") or "").strip(),
                    location_order=int(m.get("LocationOrder", 0)),
                    location_type=(m.get("LocationType") or "").strip(),
                    train_origin=(m.get("TrainOrigin") or "").strip(),
                    train_destination=(m.get("TrainDestination") or "").strip(),
                    scheduled_arrival=(m.get("ScheduledArrival") or "").strip() or None,
                    scheduled_departure=(m.get("ScheduledDeparture") or "").strip() or None,
                    actual_arrival=(m.get("Arrival") or "").strip() or None,
                    actual_departure=(m.get("Departure") or "").strip() or None,
                    auto_arrival=m.get("AutoArrival") == "1" if m.get("AutoArrival") else None,
                    auto_depart=m.get("AutoDepart") == "1" if m.get("AutoDepart") else None,
                    stop_type=(m.get("StopType") or "").strip() or None,
                    fetched_at=fetched_at,
                )
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