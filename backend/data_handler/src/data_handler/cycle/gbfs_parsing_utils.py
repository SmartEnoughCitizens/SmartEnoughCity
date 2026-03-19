"""Parsing and validation utilities for GBFS (General Bikeshare Feed Specification) data."""

import logging
from datetime import datetime

logger = logging.getLogger(__name__)


def parse_iso_timestamp(timestamp_str: str) -> datetime:
    """
    Parse RFC3339/ISO timestamp from GBFS API.

    Args:
        timestamp_str: ISO format timestamp (e.g., "2026-01-22T17:30:00+00:00" or "2026-01-22T17:30:00Z")

    Returns:
        Timezone-aware datetime object

    Raises:
        ValueError: If timestamp format is invalid
    """
    try:
        # Handle both formats: 2026-01-22T17:30:00+00:00 and 2026-01-22T17:30:00Z
        if timestamp_str.endswith("Z"):
            timestamp_str = timestamp_str[:-1] + "+00:00"
        return datetime.fromisoformat(timestamp_str)
    except ValueError as e:
        msg = f"Invalid ISO timestamp format: {timestamp_str}"
        raise ValueError(msg) from e


def validate_station_status_record(record: dict) -> None:
    """
    Validate API station status record.

    Args:
        record: Dictionary containing station status data from GBFS API

    Raises:
        ValueError: If record is missing required fields or has invalid data
    """
    required_fields = [
        "station_id",
        "num_bikes_available",
        "num_docks_available",
        "is_installed",
        "is_renting",
        "is_returning",
        "last_reported",
    ]

    # Check for missing fields
    for field in required_fields:
        if field not in record:
            msg = f"Missing required field: {field}"
            raise ValueError(msg)

    # Validate data types and ranges
    station_id = record["station_id"]
    if not isinstance(station_id, (int, str)) or int(station_id) <= 0:
        msg = f"Invalid station_id: {station_id}"
        raise ValueError(msg)

    bikes = record["num_bikes_available"]
    if not isinstance(bikes, int) or bikes < 0:
        msg = f"Invalid num_bikes_available: {bikes}"
        raise ValueError(msg)

    docks = record["num_docks_available"]
    if not isinstance(docks, int) or docks < 0:
        msg = f"Invalid num_docks_available: {docks}"
        raise ValueError(msg)

    # Validate booleans
    for bool_field in ["is_installed", "is_renting", "is_returning"]:
        if not isinstance(record[bool_field], bool):
            msg = f"{bool_field} must be boolean, got: {type(record[bool_field])}"
            raise TypeError(msg)
