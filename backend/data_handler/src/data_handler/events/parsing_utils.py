"""Parsing utilities for events data from Ticketmaster."""

import contextlib
import logging
from dataclasses import dataclass
from datetime import date, datetime
from typing import Any

logger = logging.getLogger(__name__)

HIGH_IMPACT_VENUES: frozenset[str] = frozenset(
    {"Aviva Stadium", "Croke Park", "3Arena", "RDS Arena", "Tallaght Stadium"}
)

VENUE_CAPACITY_THRESHOLD: int = 8000


@dataclass
class ParsedEvent:
    """Parsed event data before database insertion."""

    source: str
    source_id: str
    event_name: str
    event_type: str
    venue_name: str
    latitude: float
    longitude: float
    event_date: date
    start_time: datetime
    end_time: datetime | None
    is_high_impact: bool
    estimated_attendance: int | None


def determine_high_impact(venue_name: str, capacity_or_spectators: int | None) -> bool:
    """
    Determine if an event is high-impact based on venue name or capacity.

    Returns True if the venue is in HIGH_IMPACT_VENUES or if
    the capacity/spectator count meets or exceeds the threshold.
    """
    if venue_name in HIGH_IMPACT_VENUES:
        return True
    return (
        capacity_or_spectators is not None
        and capacity_or_spectators >= VENUE_CAPACITY_THRESHOLD
    )


# ---------------------------------------------------------------------------
# Ticketmaster parsing
# ---------------------------------------------------------------------------


def _extract_ticketmaster_venue(raw: dict[str, Any]) -> tuple[str, float, float] | None:
    """Extract venue name, latitude, longitude from a Ticketmaster event dict."""
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
    """
    venue_info = _extract_ticketmaster_venue(raw)
    if venue_info is None:
        return None

    venue_name, latitude, longitude = venue_info

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
        latitude=latitude,
        longitude=longitude,
        event_date=event_date,
        start_time=start_time,
        end_time=end_time,
        is_high_impact=determine_high_impact(venue_name, capacity_or_spectators=None),
        estimated_attendance=None,
    )


def parse_ticketmaster_response(raw_data: dict[str, Any]) -> list[ParsedEvent]:
    """
    Parse the complete Ticketmaster API response into a list of events.

    Returns an empty list if the response is empty or malformed.
    """
    events: list[ParsedEvent] = []

    embedded = raw_data.get("_embedded")
    if not embedded:
        return events

    raw_events = embedded.get("events", [])
    for raw_event in raw_events:
        parsed = parse_ticketmaster_event(raw_event)
        if parsed is not None:
            events.append(parsed)

    return events
