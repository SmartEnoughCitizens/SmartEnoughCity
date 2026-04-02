"""Handler for real-time station snapshot ingestion."""

import logging
from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.exc import IntegrityError

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


def process_cycle_live_data() -> None:
    """Fetch and store real-time station status.

    This function:
    1. Fetches current status from Dublin Bikes GeoJSON API
    2. Stores in station_snapshots table

    Note: Requires that static station data has been loaded first via
    process_cycle_station_info() to avoid foreign key violations.
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
            valid_ids = set(
                session.scalars(select(DublinBikesStation.station_id)).all()
            )
            filtered = [r for r in records if r["station_id"] in valid_ids]
            skipped = len(records) - len(filtered)
            if skipped:
                logger.warning(
                    "Skipped %d station snapshot record(s) — unknown station_id.",
                    skipped,
                )
            if filtered:
                session.execute(pg_insert(DublinBikesStationSnapshot).values(filtered))
                session.commit()

        logger.info("Inserted %d station snapshot record(s).", len(filtered))

    except IntegrityError as e:
        error_msg = str(e.orig) if hasattr(e, "orig") else str(e)
        if "foreign key" in error_msg.lower():
            logger.exception(
                "Foreign key violation: Station data must be loaded first. "
                "Run process_cycle_station_info() before fetching snapshots."
            )
        raise
    except Exception:
        logger.exception("Error storing station snapshots")
        raise
