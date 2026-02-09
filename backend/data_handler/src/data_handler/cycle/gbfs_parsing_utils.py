"""Parsing and validation utilities for GBFS (General Bikeshare Feed Specification) data."""

from datetime import datetime
import logging

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
        if timestamp_str.endswith('Z'):
            timestamp_str = timestamp_str[:-1] + '+00:00'
        return datetime.fromisoformat(timestamp_str)
    except ValueError as e:
        msg = f"Invalid ISO timestamp format: {timestamp_str}"
        raise ValueError(msg) from e


def parse_csv_boolean(value: str) -> bool:
    """
    Convert CSV string boolean to Python bool.

    Args:
        value: String representation of boolean ("true", "false", "1", "0", "yes", "no")

    Returns:
        Python boolean value

    Raises:
        ValueError: If value cannot be converted to boolean
    """
    value_lower = value.strip().lower()
    if value_lower in ('true', '1', 'yes'):
        return True
    if value_lower in ('false', '0', 'no'):
        return False
    raise ValueError(f"Invalid boolean value: {value}")


def parse_csv_timestamp(timestamp_str: str) -> datetime:
    """
    Parse timestamp from CSV (ISO format).

    Args:
        timestamp_str: ISO format timestamp string

    Returns:
        Timezone-aware datetime object

    Raises:
        ValueError: If timestamp format is invalid
    """
    return parse_iso_timestamp(timestamp_str)


def validate_station_status_record(record: dict) -> None:
    """
    Validate API station status record.

    Args:
        record: Dictionary containing station status data from GBFS API

    Raises:
        ValueError: If record is missing required fields or has invalid data
    """
    required_fields = [
        'station_id', 'num_bikes_available', 'num_docks_available',
        'is_installed', 'is_renting', 'is_returning', 'last_reported'
    ]

    # Check for missing fields
    for field in required_fields:
        if field not in record:
            raise ValueError(f"Missing required field: {field}")

    # Validate data types and ranges
    station_id = record['station_id']
    if not isinstance(station_id, (int, str)) or int(station_id) <= 0:
        raise ValueError(f"Invalid station_id: {station_id}")

    bikes = record['num_bikes_available']
    if not isinstance(bikes, int) or bikes < 0:
        raise ValueError(f"Invalid num_bikes_available: {bikes}")

    docks = record['num_docks_available']
    if not isinstance(docks, int) or docks < 0:
        raise ValueError(f"Invalid num_docks_available: {docks}")

    # Validate booleans
    for bool_field in ['is_installed', 'is_renting', 'is_returning']:
        if not isinstance(record[bool_field], bool):
            raise ValueError(f"{bool_field} must be boolean, got: {type(record[bool_field])}")


def validate_csv_station_history_row(row: dict) -> None:
    """
    Validate CSV row for station history.

    Args:
        row: Dictionary containing one row from CSV file

    Raises:
        ValueError: If row is missing required fields or has invalid data
    """
    required_fields = [
        'station_id', 'last_reported', 'num_bikes_available',
        'num_docks_available', 'is_installed', 'is_renting', 'is_returning'
    ]

    # Check for missing or empty fields
    for field in required_fields:
        if field not in row or not row[field].strip():
            raise ValueError(f"Missing or empty required field: {field}")

    # Validate numeric fields
    try:
        station_id = int(row['station_id'])
        if station_id <= 0:
            raise ValueError(f"Invalid station_id: {station_id}")
    except ValueError as e:
        raise ValueError(f"Invalid station_id: {row['station_id']}") from e

    try:
        bikes = int(row['num_bikes_available'])
        if bikes < 0:
            raise ValueError(f"Negative bikes count: {bikes}")
    except ValueError as e:
        raise ValueError(f"Invalid num_bikes_available: {row['num_bikes_available']}") from e

    try:
        docks = int(row['num_docks_available'])
        if docks < 0:
            raise ValueError(f"Negative docks count: {docks}")
    except ValueError as e:
        raise ValueError(f"Invalid num_docks_available: {row['num_docks_available']}") from e
