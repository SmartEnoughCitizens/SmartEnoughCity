# data_handler/car/car_parsing_utils.py

import re
from datetime import datetime


def parse_scats_time(time_str: str) -> datetime:
    """
    Parse SCATS time format to datetime.

    Format: 20250826000000 -> 2025-08-26 00:00:00

    Args:
        time_str: Time string in format YYYYMMDDHHMMSS

    Returns:
        datetime object

    Example:
        >>> parse_scats_time("20250826000000")
        datetime(2025, 8, 26, 0, 0, 0)
    """
    if not time_str or len(time_str) != 14:
        msg = "Invalid SCATS time format"
        raise ValueError(msg)

    year = int(time_str[0:4])
    month = int(time_str[4:6])
    day = int(time_str[6:8])
    hour = int(time_str[8:10])
    minute = int(time_str[10:12])
    second = int(time_str[12:14])

    return datetime(year, month, day, hour, minute, second)


def parse_month_year(month_str: str) -> datetime:
    """
    Parse month-year format to datetime (first day of month).

    Formats supported:
    - "1996 June" -> 1996-06-01
    - "June 1996" -> 1996-06-01

    Args:
        month_str: Month string in various formats

    Returns:
        datetime object set to first day of month
    """
    month_str = month_str.strip()

    # Try "YYYY Month" format
    match = re.match(r"(\d{4})\s+(\w+)", month_str)
    if match:
        year = int(match.group(1))
        month_name = match.group(2)
        return datetime.strptime(f"{year} {month_name}", "%Y %B")

    # Try "Month YYYY" format
    match = re.match(r"(\w+)\s+(\d{4})", month_str)
    if match:
        month_name = match.group(1)
        year = int(match.group(2))
        return datetime.strptime(f"{year} {month_name}", "%Y %B")
    msg = "Unable to parse year:" + month_str
    raise ValueError(msg)


def parse_year(year_str: str) -> int:
    """
    Parse year string to integer.

    Args:
        year_str: Year as string

    Returns:
        Year as integer
    """
    year_str = year_str.strip()

    # Handle just year
    if year_str.isdigit() and len(year_str) == 4:
        return int(year_str)

    # Handle "Year YYYY" format
    match = re.search(r"\d{4}", year_str)
    if match:
        return int(match.group(0))
    msg = "Unable to parse year:" + year_str
    raise ValueError(msg)

def parse_kw_value(value: str) -> float | None:
    """
    Parse kilowatt (kW) power value from string to float.

    Handles formats:
    - "50" -> 50.0
    - "50 kW" -> 50.0
    - "50kW" -> 50.0
    - "50-150" -> 150.0 (takes max value)
    - "3.7, 7, 22" -> 22.0 (takes max value)
    - "" -> None
    - "Not available" -> None

    Args:
        value: Power value string from CSV

    Returns:
        Float value in kW, or None if not parseable
    """
    if not value or not value.strip():
        return None

    # Clean the value
    value = value.strip().replace("kW", "").replace("kw", "").strip()

    # Handle empty after cleaning
    if not value or value.lower() in ["not available", "n/a", "na", "none"]:
        return None

    try:
        # Handle ranges: "50-150" -> take max (150)
        if "-" in value:
            parts = value.split("-")
            values = [float(p.strip()) for p in parts if p.strip()]
            result = max(values) if values else None
        # Handle comma-separated: "3.7, 7, 22" -> take max (22)
        elif "," in value:
            parts = value.split(",")
            values = [float(p.strip()) for p in parts if p.strip()]
            result = max(values) if values else None
        # Handle "2x50" format -> 50
        elif "x" in value.lower():
            parts = value.lower().split("x")
            result = float(parts[1].strip()) if len(parts) == 2 else None
        # Single value: "50" or "3.7"
        else:
            result = float(value)
    except (ValueError, TypeError):
        return None
    else:
        return result

def parse_open_hours(value: str) -> tuple[bool, str | None]:
    """
    Parse operating hours string into (is_24_7, description) tuple.

    Args:
        value: Operating hours string from CSV

    Returns:
        Tuple of (is_24_7: bool, description: str | None)

    Examples:
        "24/7" -> (True, "24/7")
        "24 hours" -> (True, "24 hours")
        "Always open" -> (True, "Always open")
        "Mon-Fri 9am-5pm" -> (False, "Mon-Fri 9am-5pm")
        "" -> (False, None)
    """
    if not value or not value.strip():
        return (False, None)

    value_clean = value.strip()
    value_lower = value_clean.lower()

    # Check for 24/7 indicators
    is_24_7 = any([
        "24/7" in value_lower,
        "24 hours" in value_lower,
        "24hrs" in value_lower,
        "always" in value_lower,
        value_lower == "24",
    ])

    return (is_24_7, value_clean)
