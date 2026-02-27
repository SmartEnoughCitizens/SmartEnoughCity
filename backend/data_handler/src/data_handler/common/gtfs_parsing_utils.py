from datetime import date, time


def parse_gtfs_date(date_str: str) -> date:
    """
    Parse a date string from GTFS format (YYYYMMDD) to a date object.

    Args:
        date_str: Date string in YYYYMMDD format (e.g., "20260126")

    Returns:
        date: Parsed date object

    Raises:
        ValueError: If the date string is not in the expected format
    """
    date_str = date_str.strip()
    if len(date_str) != 8:
        msg = f"Invalid date format: {date_str}. Expected YYYYMMDD format."
        raise ValueError(msg)

    return date(int(date_str[:4]), int(date_str[4:6]), int(date_str[6:8]))


def parse_gtfs_time(time_str: str) -> time:
    """
    Parse a time string from GTFS format (HH:MM:SS) to a time object.

    Args:
        time_str: Time string in HH:MM:SS format (e.g., "07:20:00")

    Returns:
        time: Parsed time object

    Raises:
        ValueError: If the time string is not in the expected format
    """
    time_str = time_str.strip()
    parts = time_str.split(":")
    if len(parts) < 2:
        msg = f"Invalid time format: {time_str}. Expected HH:MM:SS format."
        raise ValueError(msg)

    return time(
        # TODO: examine why time entries with hours > 24 are present in the GTFS stop_times data
        # https://aswegroup10.atlassian.net/browse/AIP-35?atlOrigin=eyJpIjoiOGM0YWIwYWU4Mjk5NGZmYWFiZDMxNGU1MTIwMWU4MjEiLCJwIjoiaiJ9
        int(parts[0]) % 24,
        int(parts[1]),
        int(parts[2]) if len(parts) > 2 else 0,
    )
