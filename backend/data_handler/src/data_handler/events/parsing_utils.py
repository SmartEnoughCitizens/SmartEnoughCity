"""Parsing utilities for events data from Ticketmaster."""

import contextlib
import logging
from dataclasses import dataclass
from datetime import date, datetime
from typing import Any

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Venue size classification
# ---------------------------------------------------------------------------

# Thresholds mirror the GENERATED ALWAYS AS logic in the Venue DB model.
# Larger venues have disproportionate impact on public transport and traffic.
VENUE_SIZE_TAG_THRESHOLDS: list[tuple[int, str]] = [
    (50000, "major_stadium"),  # e.g. Croke Park (~82k), Aviva Stadium (~51k)
    (20000, "stadium"),  # e.g. RDS Arena (~22k)
    (8000, "arena"),  # e.g. 3Arena (~13k), Tallaght Stadium (~10k)
    (1000, "theatre"),  # e.g. Olympia Theatre, Vicar Street
    (0, "venue"),  # small clubs and intimate spaces
]


@dataclass
class ParsedEvent:
    """Parsed event data before database insertion."""

    source: str
    source_id: str
    event_name: str
    event_type: str
    venue_name: str
    venue_ticketmaster_id: str
    latitude: float
    longitude: float
    event_date: date
    start_time: datetime
    end_time: datetime | None
    estimated_attendance: int | None


@dataclass
class ParsedVenue:
    """Parsed venue data from Ticketmaster. Capacity is NOT included — set manually in DB."""

    ticketmaster_id: str
    name: str
    address: str | None
    city: str | None
    latitude: float
    longitude: float


# ---------------------------------------------------------------------------
# Ticketmaster event parsing
# ---------------------------------------------------------------------------


def _extract_ticketmaster_venue(
    raw: dict[str, Any],
) -> tuple[str, str, float, float] | None:
    """Extract (tm_venue_id, venue_name, latitude, longitude) from a Ticketmaster event dict."""
    embedded = raw.get("_embedded")
    if not embedded:
        return None

    venues = embedded.get("venues", [])
    if not venues:
        return None

    venue = venues[0]
    location = venue.get("location")
    if not location:
        return None

    try:
        return (
            venue.get("id", ""),
            venue.get("name", ""),
            float(location["latitude"]),
            float(location["longitude"]),
        )
    except (KeyError, TypeError, ValueError):
        return None


def parse_ticketmaster_event(raw: dict[str, Any]) -> ParsedEvent | None:
    """
    Parse a single Ticketmaster Discovery API v2 event into a ParsedEvent.

    Returns None if the event cannot be parsed (missing venue, coordinates, etc.).
    venue_size_tag is not set at parse time — it is resolved from the venues
    table in _upsert_events once the venue FK is known.
    """
    venue_info = _extract_ticketmaster_venue(raw)
    if venue_info is None:
        return None

    venue_tm_id, venue_name, latitude, longitude = venue_info

    classifications = raw.get("classifications", [])
    event_type = (
        classifications[0].get("segment", {}).get("name", "Other")
        if classifications
        else "Other"
    )

    dates = raw.get("dates", {})
    start = dates.get("start", {})
    local_date_str = start.get("localDate")
    start_datetime_str = start.get("dateTime")

    if not local_date_str or not start_datetime_str:
        return None

    try:
        event_date = date.fromisoformat(local_date_str)
        start_time = datetime.fromisoformat(start_datetime_str)
    except (ValueError, TypeError):
        return None

    end_time = None
    end_data = dates.get("end")
    if end_data:
        end_datetime_str = end_data.get("dateTime")
        if end_datetime_str:
            with contextlib.suppress(ValueError, TypeError):
                end_time = datetime.fromisoformat(end_datetime_str)

    return ParsedEvent(
        source="ticketmaster",
        source_id=raw.get("id", ""),
        event_name=raw.get("name", ""),
        event_type=event_type,
        venue_name=venue_name,
        venue_ticketmaster_id=venue_tm_id,
        latitude=latitude,
        longitude=longitude,
        event_date=event_date,
        start_time=start_time,
        end_time=end_time,
        estimated_attendance=None,
    )


def parse_ticketmaster_response(raw_data: dict[str, Any]) -> list[ParsedEvent]:
    """
    Parse the complete Ticketmaster API response into a list of events.

    Returns an empty list if the response is empty or malformed.
    """
    embedded = raw_data.get("_embedded")
    if not embedded:
        return []

    return [
        parsed
        for raw_event in embedded.get("events", [])
        if (parsed := parse_ticketmaster_event(raw_event)) is not None
    ]


# ---------------------------------------------------------------------------
# Ticketmaster venue parsing
# ---------------------------------------------------------------------------


def parse_ticketmaster_venue(raw: dict[str, Any]) -> ParsedVenue | None:
    """
    Parse a single Ticketmaster venue dict into a ParsedVenue.

    Returns None if required fields (id, name, latitude, longitude) are missing.
    """
    venue_id = raw.get("id")
    name = raw.get("name")
    if not venue_id or not name:
        return None

    location = raw.get("location", {})
    try:
        latitude = float(location["latitude"])
        longitude = float(location["longitude"])
    except (KeyError, TypeError, ValueError):
        return None

    address_block = raw.get("address", {})
    address = address_block.get("line1") if address_block else None

    city_block = raw.get("city", {})
    city = city_block.get("name") if city_block else None

    return ParsedVenue(
        ticketmaster_id=venue_id,
        name=name,
        address=address,
        city=city,
        latitude=latitude,
        longitude=longitude,
    )


def parse_ticketmaster_venues_response(raw_data: dict[str, Any]) -> list[ParsedVenue]:
    """
    Parse the complete Ticketmaster /venues.json API response.

    Returns an empty list if the response is empty or malformed.
    """
    embedded = raw_data.get("_embedded")
    if not embedded:
        return []

    return [
        parsed
        for raw_venue in embedded.get("venues", [])
        if (parsed := parse_ticketmaster_venue(raw_venue)) is not None
    ]
