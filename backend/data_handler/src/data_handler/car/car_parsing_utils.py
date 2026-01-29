# data_handler/car/car_parsing_utils.py

from datetime import datetime
import re


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
        raise ValueError(f"Invalid SCATS time format: {time_str}")
    
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
    
    raise ValueError(f"Unable to parse month-year: {month_str}")


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
    
    raise ValueError(f"Unable to parse year: {year_str}")


def safe_int(value: str, default: int | None = None) -> int | None:
    """
    Safely convert string to int, returning default if empty or invalid.
    
    Args:
        value: String to convert
        default: Default value if conversion fails
        
    Returns:
        Integer or default value
    """
    if not value or not value.strip():
        return default
    
    try:
        return int(float(value))  # Handle "1.0" -> 1
    except (ValueError, TypeError):
        return default


def safe_float(value: str, default: float | None = None) -> float | None:
    """
    Safely convert string to float, returning default if empty or invalid.
    
    Args:
        value: String to convert
        default: Default value if conversion fails
        
    Returns:
        Float or default value
    """
    if not value or not value.strip():
        return default
    
    try:
        return float(value)
    except (ValueError, TypeError):
        return default