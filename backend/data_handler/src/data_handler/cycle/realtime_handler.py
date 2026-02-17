"""Handler for real-time station snapshot ingestion."""

from __future__ import annotations

import logging
from datetime import UTC, datetime

from sqlalchemy.dialects.postgresql import insert as pg_insert

from data_handler.cycle.api_client import get_jcdecaux_client
from data_handler.cycle.gbfs_parsing_utils import parse_iso_timestamp
from data_handler.cycle.models import DublinBikesStationHistory, DublinBikesStationSnapshot
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
    3. Archives to station_history table (with ON CONFLICT DO NOTHING)
    """
    logger.info("Fetching real-time station snapshots...")

    client = get_jcdecaux_client()
    stations = client.fetch_station_status()

    if not stations:
        logger.warning("No station data received from API")
        return

    fetch_timestamp = datetime.now(UTC)
    records = _transform_station_records(stations, fetch_timestamp)

    # History records exclude disabled_bikes and disabled_docks
    history_records = [
        {k: v for k, v in r.items() if k not in ("disabled_bikes", "disabled_docks")}
        for r in records
    ]

    try:
        with SessionLocal() as session:
            session.execute(
                pg_insert(DublinBikesStationSnapshot).values(records)
            )

            stmt = pg_insert(DublinBikesStationHistory).values(history_records)
            stmt = stmt.on_conflict_do_nothing(
                constraint="uq_station_timestamp",
            )
            session.execute(stmt)

            session.commit()

        logger.info(
            "Successfully stored %d station snapshots and archived to history",
            len(records),
        )

    except Exception:
        logger.exception("Error storing station snapshots")
        raise
