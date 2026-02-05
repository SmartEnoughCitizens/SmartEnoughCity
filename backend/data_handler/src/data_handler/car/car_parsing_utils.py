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