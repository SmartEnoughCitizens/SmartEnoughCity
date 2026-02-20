from data_handler.congestion_and_construction.models import TrafficEventType
from data_handler.congestion_and_construction.parsing_utils import (
    determine_event_type,
    extract_coordinates,
    parse_api_response,
    parse_traffic_event,
)


class TestDetermineEventType:
    """Tests for determine_event_type."""

    def test_congestion_keyword_in_title(self) -> None:
        """Title containing 'congestion' returns CONGESTION type."""
        event_type, color = determine_event_type("Heavy congestion on M50", None)
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_congestion_keyword_queue(self) -> None:
        """Title containing 'queue' returns CONGESTION type."""
        event_type, color = determine_event_type("Long queue at junction", None)
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_congestion_keyword_delay(self) -> None:
        """Title containing 'delay' returns CONGESTION type."""
        event_type, color = determine_event_type("Significant delay", None)
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_congestion_keyword_slow(self) -> None:
        """Title containing 'slow' returns CONGESTION type."""
        event_type, color = determine_event_type("Slow traffic ahead", None)
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_congestion_keyword_in_description(self) -> None:
        """Description containing congestion keyword returns CONGESTION type."""
        event_type, color = determine_event_type("Traffic alert", "Expect delays")
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_closure_keyword_closed(self) -> None:
        """Title containing 'closed' returns CLOSURE_INCIDENT type."""
        event_type, color = determine_event_type("Road closed", None)
        assert event_type == TrafficEventType.CLOSURE_INCIDENT
        assert color == "red"

    def test_closure_keyword_blocked(self) -> None:
        """Title containing 'blocked' returns CLOSURE_INCIDENT type."""
        event_type, color = determine_event_type("Lane blocked", None)
        assert event_type == TrafficEventType.CLOSURE_INCIDENT
        assert color == "red"

    def test_closure_keyword_collision(self) -> None:
        """Title containing 'collision' returns CLOSURE_INCIDENT type."""
        event_type, color = determine_event_type("Collision reported", None)
        assert event_type == TrafficEventType.CLOSURE_INCIDENT
        assert color == "red"

    def test_closure_keyword_crash(self) -> None:
        """Title containing 'crash' returns CLOSURE_INCIDENT type."""
        event_type, color = determine_event_type("Multi-vehicle crash", None)
        assert event_type == TrafficEventType.CLOSURE_INCIDENT
        assert color == "red"

    def test_roadworks_keyword_roadworks(self) -> None:
        """Title containing 'roadworks' returns ROADWORKS type."""
        event_type, color = determine_event_type("Roadworks on N7", None)
        assert event_type == TrafficEventType.ROADWORKS
        assert color == "black"

    def test_roadworks_keyword_works(self) -> None:
        """Title containing 'works' returns ROADWORKS type."""
        event_type, color = determine_event_type("Ongoing works", None)
        assert event_type == TrafficEventType.ROADWORKS
        assert color == "black"

    def test_roadworks_keyword_maintenance(self) -> None:
        """Title containing 'maintenance' returns ROADWORKS type."""
        event_type, color = determine_event_type("Road maintenance", None)
        assert event_type == TrafficEventType.ROADWORKS
        assert color == "black"

    def test_warning_fallback(self) -> None:
        """Title without matching keywords returns WARNING type."""
        event_type, color = determine_event_type("Speed limit change", None)
        assert event_type == TrafficEventType.WARNING
        assert color == "blue"

    def test_none_description(self) -> None:
        """None description does not cause errors."""
        event_type, color = determine_event_type("Unknown event", None)
        assert event_type == TrafficEventType.WARNING
        assert color == "blue"

    def test_case_insensitive_matching(self) -> None:
        """Keywords are matched case-insensitively."""
        event_type, color = determine_event_type("HEAVY CONGESTION", None)
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_congestion_takes_priority_over_closure(self) -> None:
        """When both congestion and closure keywords present, congestion wins."""
        event_type, color = determine_event_type("Congestion due to crash", None)
        assert event_type == TrafficEventType.CONGESTION
        assert color == "orange"

    def test_closure_takes_priority_over_roadworks(self) -> None:
        """When both closure and roadworks keywords present, closure wins."""
        event_type, color = determine_event_type("Road closed due to roadworks", None)
        assert event_type == TrafficEventType.CLOSURE_INCIDENT
        assert color == "red"


class TestExtractCoordinates:
    """Tests for extract_coordinates."""

    def test_point_geometry(self) -> None:
        """Point geometry returns (lon, lat) tuple."""
        geometry = {"type": "Point", "coordinates": [-6.25, 53.35]}
        result = extract_coordinates(geometry)
        assert result == (-6.25, 53.35)

    def test_linestring_geometry_uses_first_point(self) -> None:
        """LineString geometry returns first coordinate pair."""
        geometry = {
            "type": "LineString",
            "coordinates": [[-6.25, 53.35], [-6.26, 53.36]],
        }
        result = extract_coordinates(geometry)
        assert result == (-6.25, 53.35)

    def test_missing_coordinates_returns_none(self) -> None:
        """Geometry without coordinates returns None."""
        geometry = {"type": "Point"}
        result = extract_coordinates(geometry)
        assert result is None

    def test_empty_coordinates_returns_none(self) -> None:
        """Geometry with empty coordinates list returns None."""
        geometry = {"type": "Point", "coordinates": []}
        result = extract_coordinates(geometry)
        assert result is None

    def test_empty_geometry_returns_none(self) -> None:
        """Empty geometry dict returns None."""
        result = extract_coordinates({})
        assert result is None


class TestParseTrafficEvent:
    """Tests for parse_traffic_event."""

    def test_valid_point_event(self) -> None:
        """Valid event with Point geometry is parsed correctly."""
        item = {
            "title": "Heavy congestion on M50",
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {
                        "description": "Expect delays northbound",
                        "id": "evt-123",
                    },
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.event_type == TrafficEventType.CONGESTION
        assert result.title == "Heavy congestion on M50"
        assert result.description == "Expect delays northbound"
        assert result.lat == 53.35
        assert result.lon == -6.25
        assert result.color == "orange"
        assert result.source_id == "evt-123"

    def test_event_with_linestring_geometry(self) -> None:
        """Event with LineString geometry uses first coordinate pair."""
        item = {
            "title": "Road closed",
            "features": [
                {
                    "geometry": {
                        "type": "LineString",
                        "coordinates": [[-6.30, 53.40], [-6.31, 53.41]],
                    },
                    "properties": {},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.lat == 53.40
        assert result.lon == -6.30

    def test_event_without_features_returns_none(self) -> None:
        """Event with empty features list returns None."""
        item = {"title": "Some event", "features": []}
        result = parse_traffic_event(item)
        assert result is None

    def test_event_without_geometry_returns_none(self) -> None:
        """Event with no coordinates in geometry returns None."""
        item = {
            "title": "Some event",
            "features": [{"geometry": {}, "properties": {}}],
        }
        result = parse_traffic_event(item)
        assert result is None

    def test_falls_back_to_tooltip_for_title(self) -> None:
        """When title is missing, tooltip is used."""
        item = {
            "tooltip": "Tooltip text",
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.title == "Tooltip text"

    def test_falls_back_to_unknown_event_title(self) -> None:
        """When both title and tooltip are missing, 'Unknown Event' is used."""
        item = {
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.title == "Unknown Event"

    def test_description_newlines_replaced(self) -> None:
        """Newlines in description are replaced with spaces."""
        item = {
            "title": "Road works",
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {"description": "Line one\nLine two\nLine three"},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.description == "Line one Line two Line three"

    def test_encoded_description_fallback(self) -> None:
        """Falls back to encodedDescription when description is missing."""
        item = {
            "title": "Event",
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {"encodedDescription": "Encoded desc"},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.description == "Encoded desc"

    def test_source_id_from_uri(self) -> None:
        """Falls back to uri for source_id when id is missing."""
        item = {
            "title": "Event",
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {"uri": "some-uri-123"},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.source_id == "some-uri-123"

    def test_no_source_id(self) -> None:
        """When neither id nor uri present, source_id is None."""
        item = {
            "title": "Event",
            "features": [
                {
                    "geometry": {"type": "Point", "coordinates": [-6.25, 53.35]},
                    "properties": {},
                }
            ],
        }
        result = parse_traffic_event(item)
        assert result is not None
        assert result.source_id is None


class TestParseApiResponse:
    """Tests for parse_api_response."""

    def test_valid_response_with_multiple_events(self) -> None:
        """Parses multiple events from a valid API response."""
        raw_data = [
            {
                "data": {
                    "mapFeaturesQuery": {
                        "mapFeatures": [
                            {
                                "title": "Congestion on M50",
                                "features": [
                                    {
                                        "geometry": {
                                            "type": "Point",
                                            "coordinates": [-6.25, 53.35],
                                        },
                                        "properties": {"id": "1"},
                                    }
                                ],
                            },
                            {
                                "title": "Road closed on N7",
                                "features": [
                                    {
                                        "geometry": {
                                            "type": "Point",
                                            "coordinates": [-6.30, 53.40],
                                        },
                                        "properties": {"id": "2"},
                                    }
                                ],
                            },
                        ]
                    }
                }
            }
        ]
        result = parse_api_response(raw_data)
        assert len(result) == 2
        assert result[0].title == "Congestion on M50"
        assert result[1].title == "Road closed on N7"

    def test_empty_response_returns_empty_list(self) -> None:
        """Empty raw_data list returns empty list."""
        result = parse_api_response([])
        assert result == []

    def test_missing_data_key_returns_empty_list(self) -> None:
        """Response without 'data' key returns empty list."""
        result = parse_api_response([{"other": "value"}])
        assert result == []

    def test_missing_map_features_returns_empty_list(self) -> None:
        """Response without mapFeatures returns empty list."""
        result = parse_api_response(
            [{"data": {"mapFeaturesQuery": {"other": "value"}}}]
        )
        assert result == []

    def test_skips_unparseable_events(self) -> None:
        """Events that fail to parse (no features) are skipped."""
        raw_data = [
            {
                "data": {
                    "mapFeaturesQuery": {
                        "mapFeatures": [
                            {"title": "Bad event", "features": []},
                            {
                                "title": "Good event with delay",
                                "features": [
                                    {
                                        "geometry": {
                                            "type": "Point",
                                            "coordinates": [-6.25, 53.35],
                                        },
                                        "properties": {},
                                    }
                                ],
                            },
                        ]
                    }
                }
            }
        ]
        result = parse_api_response(raw_data)
        assert len(result) == 1
        assert result[0].title == "Good event with delay"
