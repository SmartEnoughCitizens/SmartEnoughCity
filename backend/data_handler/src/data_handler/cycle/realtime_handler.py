"""Handler for real-time station snapshot ingestion."""

from __future__ import annotations

import logging
from datetime import datetime, timezone

from sqlalchemy import text

from data_handler.cycle.api_client import get_jcdecaux_client
from data_handler.cycle.gbfs_parsing_utils import parse_iso_timestamp
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings

logger = logging.getLogger(__name__)


def _parse_last_reported(value: int | str) -> datetime:
    """Parse last_reported which may be a Unix timestamp (int) or ISO string."""
    if isinstance(value, (int, float)):
        if value < 0:
            return datetime(1, 1, 1, tzinfo=timezone.utc)
        return datetime.fromtimestamp(value, tz=timezone.utc)
    return parse_iso_timestamp(value)


def _transform_station_records(
    stations: list[dict], fetch_ts: datetime
) -> list[dict]:
    """Transform API station status records to database-ready dicts.

    Args:
        stations: Raw station status records from the GBFS API.
        fetch_ts: The timestamp when the data was fetched.

    Returns:
        List of dicts ready for database insertion.
    """
    records = []
    for station in stations:
        records.append(
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
        )
    return records


def fetch_and_store_station_snapshots() -> None:
    """Fetch and store real-time station status.

    This function:
    1. Fetches current status from JCDecaux API
    2. Stores in station_snapshots table
    3. Archives to station_history table
    """
    logger.info("Fetching real-time station snapshots...")

    client = get_jcdecaux_client()
    stations = client.fetch_station_status()

    if not stations:
        logger.warning("No station data received from API")
        return

    fetch_timestamp = datetime.now(timezone.utc)
    records = _transform_station_records(stations, fetch_timestamp)

    schema = get_db_settings().postgres_schema
    table_prefix = f"{schema}." if schema else ""

    insert_snapshot_sql = f"""
    INSERT INTO {table_prefix}dublin_bikes_station_snapshots
        (station_id, timestamp, last_reported, available_bikes, available_docks,
         disabled_bikes, disabled_docks, is_installed, is_renting, is_returning)
    VALUES
        (:station_id, :timestamp, :last_reported, :available_bikes, :available_docks,
         :disabled_bikes, :disabled_docks, :is_installed, :is_renting, :is_returning)
    """

    insert_history_sql = f"""
    INSERT INTO {table_prefix}dublin_bikes_station_history
        (station_id, timestamp, last_reported, available_bikes, available_docks,
         is_installed, is_renting, is_returning)
    VALUES
        (:station_id, :timestamp, :last_reported, :available_bikes, :available_docks,
         :is_installed, :is_renting, :is_returning)
    ON CONFLICT (station_id, timestamp) DO NOTHING;
    """

    try:
        with SessionLocal() as session:
            session.execute(text(insert_snapshot_sql), records)
            session.execute(text(insert_history_sql), records)
            session.commit()

        logger.info(
            "Successfully stored %d station snapshots and archived to history",
            len(records),
        )

    except Exception:
        logger.exception("Error storing station snapshots")
        raise
