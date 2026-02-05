"""Parsing utilities for TII traffic data."""

from dataclasses import dataclass
from typing import Any

from data_handler.congestion_and_construction.models import TrafficEventType


@dataclass
class ParsedTrafficEvent:
    """Parsed traffic event data before database insertion."""

    event_type: TrafficEventType
    title: str
    description: str | None
    lat: float
    lon: float
    color: str
    source_id: str | None = None


# Keywords for categorizing traffic events
CONGESTION_KEYWORDS = frozenset(["congestion", "queue", "delay", "slow"])
CLOSURE_KEYWORDS = frozenset([
    "closed", "blocked", "closure", "impassable", "collision", "crash"
])
ROADWORKS_KEYWORDS = frozenset(["roadworks", "works", "maintenance"])


def determine_event_type(title: str, description: str | None) -> tuple[TrafficEventType, str]:
    """
    Categorize a traffic event based on keywords in title and description.

    Args:
        title: Event title
        description: Event description (may be None)

    Returns:
        Tuple of (TrafficEventType, color string for map display)
    """
    text = f"{title} {description or ''}".lower()

    if any(keyword in text for keyword in CONGESTION_KEYWORDS):
        return TrafficEventType.CONGESTION, "orange"

    if any(keyword in text for keyword in CLOSURE_KEYWORDS):
        return TrafficEventType.CLOSURE_INCIDENT, "red"

    if any(keyword in text for keyword in ROADWORKS_KEYWORDS):
        return TrafficEventType.ROADWORKS, "black"

    return TrafficEventType.WARNING, "blue"


def extract_coordinates(geometry: dict[str, Any]) -> tuple[float, float] | None:
    """
    Extract latitude and longitude from GeoJSON geometry.

    Handles both Point and LineString geometry types.

    Args:
        geometry: GeoJSON geometry object

    Returns:
        Tuple of (longitude, latitude) or None if coordinates not found
    """
    coords = geometry.get("coordinates")
    if not coords:
        return None

    # Handle LineString (list of coordinate pairs) vs Point (single pair)
    if isinstance(coords[0], list):
        # LineString - use first point
        return coords[0][0], coords[0][1]
    else:
        # Point
        return coords[0], coords[1]


def parse_traffic_event(item: dict[str, Any]) -> ParsedTrafficEvent | None:
    """
    Parse a single traffic event from the TII API response.

    Args:
        item: Raw event item from API response

    Returns:
        ParsedTrafficEvent or None if parsing fails
    """
    title = item.get("title") or item.get("tooltip") or "Unknown Event"

    features = item.get("features", [])
    if not features:
        return None

    feature = features[0]
    props = feature.get("properties", {})
    geometry = feature.get("geometry", {})

    coords = extract_coordinates(geometry)
    if coords is None:
        return None

    lon, lat = coords
    description = props.get("description") or props.get("encodedDescription")
    
    if description:
        # Clean up description by replacing newlines
        description = description.replace("\n", " ").strip()

    event_type, color = determine_event_type(title, description)

    # Try to extract a unique source ID if available
    source_id = props.get("id") or props.get("uri")

    return ParsedTrafficEvent(
        event_type=event_type,
        title=title,
        description=description,
        lat=lat,
        lon=lon,
        color=color,
        source_id=str(source_id) if source_id else None,
    )


def parse_api_response(raw_data: list[dict[str, Any]]) -> list[ParsedTrafficEvent]:
    """
    Parse the complete TII API response into a list of traffic events.

    Args:
        raw_data: Raw JSON response from the TII API

    Returns:
        List of ParsedTrafficEvent objects
    """
    events: list[ParsedTrafficEvent] = []

    try:
        data_block = raw_data[0]["data"]["mapFeaturesQuery"]["mapFeatures"]
    except (KeyError, IndexError, TypeError):
        return events

    for item in data_block:
        event = parse_traffic_event(item)
        if event is not None:
            events.append(event)

    return events
