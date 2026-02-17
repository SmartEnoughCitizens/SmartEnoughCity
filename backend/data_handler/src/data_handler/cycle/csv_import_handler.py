"""Handler for importing historical Dublin Bikes CSV archives."""

import logging
from pathlib import Path

from sqlalchemy import text

from data_handler.csv_utils import read_csv_file
from data_handler.cycle.gbfs_parsing_utils import (
    parse_csv_boolean,
    parse_csv_timestamp,
    validate_csv_station_history_row,
)
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings

logger = logging.getLogger(__name__)

REQUIRED_HEADERS = [
    "system_id",
    "last_reported",
    "station_id",
    "num_bikes_available",
    "num_docks_available",
    "is_installed",
    "is_renting",
    "is_returning",
]


def parse_station_history_csv_row(row: dict) -> dict:
    """Transform CSV row to database record.

    Only extracts relevant status fields; ignores redundant station
    metadata (name, address, lat, lon, etc.) present in the CSV.
    """
    validate_csv_station_history_row(row)

    timestamp = parse_csv_timestamp(row["last_reported"])

    return {
        "station_id": int(row["station_id"]),
        "timestamp": timestamp,
        "last_reported": timestamp,
        "available_bikes": int(row["num_bikes_available"]),
        "available_docks": int(row["num_docks_available"]),
        "is_installed": parse_csv_boolean(row["is_installed"]),
        "is_renting": parse_csv_boolean(row["is_renting"]),
        "is_returning": parse_csv_boolean(row["is_returning"]),
    }


def import_station_history_csv(csv_path: Path) -> None:
    """Import historical station data from a single CSV file."""
    logger.info("Importing station history from %s", csv_path)

    if not csv_path.exists():
        msg = f"CSV file not found: {csv_path}"
        raise FileNotFoundError(msg)

    records = [
        parse_station_history_csv_row(row)
        for row in read_csv_file(csv_path, REQUIRED_HEADERS)
    ]

    if not records:
        logger.warning("No records to import")
        return

    schema = get_db_settings().postgres_schema
    table = (
        f"{schema}.dublin_bikes_station_history"
        if schema
        else "dublin_bikes_station_history"
    )

    insert_sql = f"""
    INSERT INTO {table}
        (station_id, timestamp, last_reported, available_bikes, available_docks,
         is_installed, is_renting, is_returning)
    VALUES
        (:station_id, :timestamp, :last_reported, :available_bikes, :available_docks,
         :is_installed, :is_renting, :is_returning)
    ON CONFLICT (station_id, timestamp) DO NOTHING;
    """

    try:
        with SessionLocal() as session:
            session.execute(text(insert_sql), records)
            session.commit()

        logger.info("Successfully imported %d station history records", len(records))

    except Exception:
        logger.exception("Error importing CSV from %s", csv_path)
        raise


def import_all_station_history_csvs(directory: Path) -> None:
    """Import all matching CSV files from a directory."""
    logger.info("Scanning directory for CSV files: %s", directory)

    if not directory.exists() or not directory.is_dir():
        msg = f"Invalid directory: {directory}"
        raise ValueError(msg)

    csv_files = sorted(directory.glob("dublin-bikes_station_status_*.csv"))

    if not csv_files:
        logger.warning("No CSV files found in %s", directory)
        return

    logger.info("Found %d CSV files to import", len(csv_files))

    for csv_file in csv_files:
        try:
            import_station_history_csv(csv_file)
        except Exception:
            logger.exception("Failed to import %s, continuing with next file", csv_file)
