"""Handler for real-time station snapshot ingestion."""

import logging
from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert

from data_handler.cycle.api_client import get_dublin_bikes_client
from data_handler.cycle.gbfs_parsing_utils import parse_iso_timestamp
from data_handler.cycle.models import DublinBikesStation, DublinBikesStationSnapshot
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


def _parse_last_reported(value: int | str) -> datetime:
    """Parse last_reported which may be a Unix timestamp (int) or ISO string."""
    if isinstance(value, (int, float)):
        if value < 0:
            return datetime(1, 1, 1, tzinfo=UTC)
        return datetime.fromtimestamp(value, tz=UTC)
    return parse_iso_timestamp(value)


def _transform_station_records(stations: list[dict], fetch_ts: datetime) -> list[dict]:
    """Transform API station status records to database-ready dicts.

    Args:
        stations: Raw station status records from the GBFS API.
        fetch_ts: The timestamp when the data was fetched.

    Returns:
        List of dicts ready for database insertion.
    """
    return [
        {
            "station_id": int(station["station_id"]),
            "timestamp": fetch_ts,
            "last_reported": _parse_last_reported(station["last_reported"]),
            "available_bikes": station["num_bikes_available"],
            "available_docks": station["num_docks_available"],
            "disabled_bikes": station.get("num_bikes_disabled", 0),
            "disabled_docks": station.get("num_docks_disabled", 0),
            "is_installed": station["is_installed"],
            "is_renting": station["is_renting"],
            "is_returning": station["is_returning"],
        }
        for station in stations
    ]


def fetch_and_store_station_snapshots() -> None:
    """Fetch and store real-time station status.

    This function:
    1. Fetches current status from Dublin Bikes GeoJSON API
    2. Filters out stations not present in the stations table
    3. Stores in station_snapshots table
    """
    logger.info("Fetching real-time station snapshots...")

    client = get_dublin_bikes_client()
    stations = client.fetch_station_status()

    if not stations:
        logger.warning("No station data received from API")
        return

    fetch_timestamp = datetime.now(UTC)
    records = _transform_station_records(stations, fetch_timestamp)

    try:
        with SessionLocal() as session:
            # Get all existing station IDs to filter records
            existing_station_ids = {
                station_id
                for (station_id,) in session.execute(
                    select(DublinBikesStation.station_id)
                ).all()
            }

            # Filter records to only include stations that exist in the database
            valid_records = [
                record
                for record in records
                if record["station_id"] in existing_station_ids
            ]

            if len(valid_records) < len(records):
                missing_count = len(records) - len(valid_records)
                missing_ids = {
                    record["station_id"]
                    for record in records
                    if record["station_id"] not in existing_station_ids
                }
                logger.warning(
                    "Skipping %d snapshots for stations not in database: %s",
                    missing_count,
                    sorted(missing_ids),
                )

            if not valid_records:
                logger.warning(
                    "No valid station snapshots to store (all stations missing from database)"
                )
                return

            session.execute(pg_insert(DublinBikesStationSnapshot).values(valid_records))
            session.commit()

        logger.info("Successfully stored %d station snapshots", len(valid_records))

    except Exception:
        logger.exception("Error storing station snapshots")
        raise
