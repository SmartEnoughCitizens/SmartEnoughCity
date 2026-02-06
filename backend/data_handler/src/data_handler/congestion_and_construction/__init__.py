"""
Traffic and Construction Data Handler Package.

This package provides functionality for fetching, parsing, storing, and
exporting traffic and construction data from Transport Infrastructure Ireland (TII).

Example usage:
    from data_handler.congestion_and_construction import (
        fetch_and_store_traffic_data,
        get_current_traffic_events,
        export_to_html_map,
        DUBLIN_BOUNDING_BOX,
    )

    # Fetch latest data
    count = fetch_and_store_traffic_data()
    print(f"Stored {count} events")

    # Get current events
    events = get_current_traffic_events()

    # Export to map
    export_to_html_map(events, "traffic_map.html", open_in_browser=True)
"""

from data_handler.congestion_and_construction.data_handler import (
    clear_all_traffic_events,
    fetch_and_store_traffic_data,
    get_current_traffic_events,
)
from data_handler.congestion_and_construction.export_utils import (
    events_to_geojson,
    export_to_csv,
    export_to_geojson,
    export_to_html_map,
)
from data_handler.congestion_and_construction.models import (
    TrafficDataFetchLog,
    TrafficEvent,
    TrafficEventType,
)
from data_handler.congestion_and_construction.parsing_utils import (
    ParsedTrafficEvent,
    determine_event_type,
    parse_api_response,
    parse_traffic_event,
)
from data_handler.congestion_and_construction.tii_api_client import (
    DUBLIN_BOUNDING_BOX,
    BoundingBox,
    TIIApiClient,
)

__all__ = [
    "DUBLIN_BOUNDING_BOX",
    "BoundingBox",
    "ParsedTrafficEvent",
    "TIIApiClient",
    "TrafficDataFetchLog",
    "TrafficEvent",
    "TrafficEventType",
    "clear_all_traffic_events",
    "determine_event_type",
    "events_to_geojson",
    "export_to_csv",
    "export_to_geojson",
    "export_to_html_map",
    "fetch_and_store_traffic_data",
    "get_current_traffic_events",
    "parse_api_response",
    "parse_traffic_event",
]
