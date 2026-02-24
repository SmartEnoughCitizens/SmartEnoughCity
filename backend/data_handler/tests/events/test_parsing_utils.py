"""Tests for events parsing utilities."""

from datetime import UTC, date, datetime

import pytest

from data_handler.events.parsing_utils import (
    HIGH_IMPACT_TAGS,
    VENUE_SIZE_TAG_THRESHOLDS,
    determine_high_impact,
    parse_ticketmaster_event,
    parse_ticketmaster_response,
    parse_ticketmaster_venue,
    parse_ticketmaster_venues_response,
)

# ---------------------------------------------------------------------------
# Fixtures: sample API responses
# ---------------------------------------------------------------------------


def _make_ticketmaster_event(  # noqa: PLR0913
    *,
    event_id: str = "Z7r9jZ1AdJ1a0",
    name: str = "Coldplay: Music of the Spheres",
    segment: str = "Music",
    venue_id: str = "KovZpZAEdFtA",
    venue_name: str = "Croke Park",
    latitude: str = "53.36086568",
    longitude: str = "-6.25101015",
    local_date: str = "2026-02-15",
    start_datetime: str = "2026-02-15T19:00:00Z",
    end_datetime: str | None = "2026-02-15T22:30:00Z",
) -> dict:
    """Build a minimal Ticketmaster event dict matching the Discovery API v2 shape."""
    event = {
        "id": event_id,
        "name": name,
        "type": "event",
        "classifications": [{"segment": {"name": segment}}],
        "dates": {
            "start": {
                "localDate": local_date,
                "dateTime": start_datetime,
            },
        },
        "_embedded": {
            "venues": [
                {
                    "id": venue_id,
                    "name": venue_name,
                    "location": {
                        "latitude": latitude,
                        "longitude": longitude,
                    },
                }
            ]
        },
    }
    if end_datetime is not None:
        event["dates"]["end"] = {"dateTime": end_datetime}
    return event


def _make_raw_venue(  # noqa: PLR0913
    *,
    venue_id: str = "KovZpZAEdFtA",
    name: str = "3Arena",
    latitude: str = "53.3478",
    longitude: str = "-6.2297",
    address_line1: str | None = "North Wall Quay",
    city_name: str | None = "Dublin",
) -> dict:
    """Build a minimal Ticketmaster venue dict."""
    v: dict = {
        "id": venue_id,
        "name": name,
        "location": {"latitude": latitude, "longitude": longitude},
    }
    if address_line1 is not None:
        v["address"] = {"line1": address_line1}
    if city_name is not None:
        v["city"] = {"name": city_name}
    return v


# ---------------------------------------------------------------------------
# determine_high_impact
# ---------------------------------------------------------------------------


class TestDetermineHighImpact:
    """Tests for determine_high_impact."""

    @pytest.mark.parametrize("tag", sorted(HIGH_IMPACT_TAGS))
    def test_high_impact_tag_returns_true(self, tag: str) -> None:
        """Every tag in HIGH_IMPACT_TAGS is high-impact regardless of attendance."""
        assert determine_high_impact(tag, estimated_attendance=None) is True

    def test_theatre_tag_returns_false(self) -> None:
        """Theatre-sized venues are not high-impact."""
        assert determine_high_impact("theatre", estimated_attendance=None) is False

    def test_venue_tag_returns_false(self) -> None:
        """Small venues are not high-impact."""
        assert determine_high_impact("venue", estimated_attendance=None) is False

    def test_none_tag_with_large_attendance_returns_true(self) -> None:
        """Unknown venue with attendance above threshold is high-impact."""
        assert determine_high_impact(None, estimated_attendance=10000) is True

    def test_none_tag_with_small_attendance_returns_false(self) -> None:
        """Unknown venue with attendance below threshold is not high-impact."""
        assert determine_high_impact(None, estimated_attendance=500) is False

    def test_none_tag_with_no_attendance_returns_false(self) -> None:
        """Unknown venue with no attendance info is not high-impact."""
        assert determine_high_impact(None, estimated_attendance=None) is False

    def test_none_tag_at_exact_threshold_returns_true(self) -> None:
        """Attendance exactly at threshold is high-impact."""
        assert determine_high_impact(None, estimated_attendance=8000) is True

    def test_venue_size_tag_thresholds_are_documented(self) -> None:
        """VENUE_SIZE_TAG_THRESHOLDS lists all size categories in descending order."""
        tags = [tag for _, tag in VENUE_SIZE_TAG_THRESHOLDS]
        assert "major_stadium" in tags
        assert "stadium" in tags
        assert "arena" in tags
        assert "theatre" in tags
        assert "venue" in tags
        # Thresholds should be in descending order
        thresholds = [t for t, _ in VENUE_SIZE_TAG_THRESHOLDS]
        assert thresholds == sorted(thresholds, reverse=True)


# ---------------------------------------------------------------------------
# parse_ticketmaster_event
# ---------------------------------------------------------------------------


class TestParseTicketmasterEvent:
    """Tests for parse_ticketmaster_event."""

    def test_valid_event_parses_correctly(self) -> None:
        """A valid Ticketmaster event produces a correct ParsedEvent."""
        raw = _make_ticketmaster_event()
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.source == "ticketmaster"
        assert result.source_id == "Z7r9jZ1AdJ1a0"
        assert result.event_name == "Coldplay: Music of the Spheres"
        assert result.event_type == "Music"
        assert result.venue_name == "Croke Park"
        assert result.venue_ticketmaster_id == "KovZpZAEdFtA"
        assert result.latitude == pytest.approx(53.36086568)
        assert result.longitude == pytest.approx(-6.25101015)
        assert result.event_date == date(2026, 2, 15)
        assert result.start_time == datetime(2026, 2, 15, 19, 0, 0, tzinfo=UTC)
        assert result.end_time == datetime(2026, 2, 15, 22, 30, 0, tzinfo=UTC)
        # is_high_impact is False at parse time — recomputed in _upsert_events from venue tag
        assert result.is_high_impact is False

    def test_event_without_end_time(self) -> None:
        """An event with no end time has end_time = None."""
        raw = _make_ticketmaster_event(end_datetime=None)
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.end_time is None

    def test_venue_id_extracted(self) -> None:
        """The Ticketmaster venue ID is extracted for FK resolution."""
        raw = _make_ticketmaster_event(venue_id="TM_VENUE_123")
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.venue_ticketmaster_id == "TM_VENUE_123"

    def test_missing_venue_returns_none(self) -> None:
        """An event without venue data is skipped."""
        raw = _make_ticketmaster_event()
        raw["_embedded"]["venues"] = []
        result = parse_ticketmaster_event(raw)

        assert result is None

    def test_missing_embedded_returns_none(self) -> None:
        """An event without _embedded is skipped."""
        raw = _make_ticketmaster_event()
        del raw["_embedded"]
        result = parse_ticketmaster_event(raw)

        assert result is None

    def test_missing_location_returns_none(self) -> None:
        """An event where the venue has no location is skipped."""
        raw = _make_ticketmaster_event()
        del raw["_embedded"]["venues"][0]["location"]
        result = parse_ticketmaster_event(raw)

        assert result is None

    def test_missing_classifications_defaults_event_type(self) -> None:
        """An event without classifications gets 'Other' as event_type."""
        raw = _make_ticketmaster_event()
        raw["classifications"] = []
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.event_type == "Other"

    def test_sports_event_type_extracted(self) -> None:
        """Sports classification is correctly extracted."""
        raw = _make_ticketmaster_event(
            name="Ireland v Italy",
            segment="Sports",
            venue_name="Aviva Stadium",
            latitude="53.3353",
            longitude="-6.2285",
        )
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.event_type == "Sports"
        # is_high_impact is False at parse time — recomputed from venue tag in _upsert_events
        assert result.is_high_impact is False


# ---------------------------------------------------------------------------
# parse_ticketmaster_response
# ---------------------------------------------------------------------------


class TestParseTicketmasterResponse:
    """Tests for parse_ticketmaster_response."""

    def test_multiple_events_parsed(self) -> None:
        """A response with multiple events returns all valid ones."""
        raw_data = {
            "_embedded": {
                "events": [
                    _make_ticketmaster_event(event_id="e1", name="Event One"),
                    _make_ticketmaster_event(event_id="e2", name="Event Two"),
                ]
            }
        }
        result = parse_ticketmaster_response(raw_data)

        assert len(result) == 2
        assert result[0].source_id == "e1"
        assert result[1].source_id == "e2"

    def test_empty_embedded_returns_empty_list(self) -> None:
        """A response with no events returns an empty list."""
        raw_data = {"_embedded": {"events": []}}
        result = parse_ticketmaster_response(raw_data)

        assert result == []

    def test_no_embedded_key_returns_empty_list(self) -> None:
        """A response without _embedded returns an empty list."""
        raw_data = {}
        result = parse_ticketmaster_response(raw_data)

        assert result == []

    def test_mixed_valid_and_invalid_events(self) -> None:
        """Invalid events are skipped, valid ones are returned."""
        good = _make_ticketmaster_event(event_id="good1")
        bad = _make_ticketmaster_event(event_id="bad1")
        bad["_embedded"]["venues"] = []  # make it invalid

        raw_data = {"_embedded": {"events": [good, bad]}}
        result = parse_ticketmaster_response(raw_data)

        assert len(result) == 1
        assert result[0].source_id == "good1"


# ---------------------------------------------------------------------------
# parse_ticketmaster_venue
# ---------------------------------------------------------------------------


class TestParseTicketmasterVenue:
    """Tests for parse_ticketmaster_venue."""

    def test_valid_venue_parses_correctly(self) -> None:
        """A valid venue dict produces a correct ParsedVenue."""
        raw = _make_raw_venue()
        result = parse_ticketmaster_venue(raw)

        assert result is not None
        assert result.ticketmaster_id == "KovZpZAEdFtA"
        assert result.name == "3Arena"
        assert result.latitude == pytest.approx(53.3478)
        assert result.longitude == pytest.approx(-6.2297)
        assert result.address == "North Wall Quay"
        assert result.city == "Dublin"

    def test_missing_id_returns_none(self) -> None:
        """A venue without an id is skipped."""
        raw = _make_raw_venue()
        del raw["id"]
        assert parse_ticketmaster_venue(raw) is None

    def test_missing_name_returns_none(self) -> None:
        """A venue without a name is skipped."""
        raw = _make_raw_venue()
        del raw["name"]
        assert parse_ticketmaster_venue(raw) is None

    def test_missing_location_returns_none(self) -> None:
        """A venue without location data is skipped."""
        raw = _make_raw_venue()
        del raw["location"]
        assert parse_ticketmaster_venue(raw) is None

    def test_nullable_address_and_city(self) -> None:
        """Address and city are optional fields."""
        raw = _make_raw_venue(address_line1=None, city_name=None)
        result = parse_ticketmaster_venue(raw)

        assert result is not None
        assert result.address is None
        assert result.city is None


# ---------------------------------------------------------------------------
# parse_ticketmaster_venues_response
# ---------------------------------------------------------------------------


class TestParseTicketmasterVenuesResponse:
    """Tests for parse_ticketmaster_venues_response."""

    def test_multiple_venues_parsed(self) -> None:
        """A response with multiple venues returns all valid ones."""
        raw_data = {
            "_embedded": {
                "venues": [
                    _make_raw_venue(venue_id="v1", name="Venue A"),
                    _make_raw_venue(venue_id="v2", name="Venue B"),
                ]
            }
        }
        result = parse_ticketmaster_venues_response(raw_data)

        assert len(result) == 2
        assert result[0].ticketmaster_id == "v1"
        assert result[1].ticketmaster_id == "v2"

    def test_no_embedded_returns_empty_list(self) -> None:
        """A response without _embedded returns an empty list."""
        assert parse_ticketmaster_venues_response({}) == []

    def test_invalid_venues_are_skipped(self) -> None:
        """Venues missing required fields are skipped, valid ones returned."""
        raw_data = {
            "_embedded": {
                "venues": [
                    _make_raw_venue(venue_id="v1", name="Good Venue"),
                    {
                        "name": "No ID venue",
                        "location": {"latitude": "53.4", "longitude": "-6.3"},
                    },
                ]
            }
        }
        result = parse_ticketmaster_venues_response(raw_data)

        assert len(result) == 1
        assert result[0].ticketmaster_id == "v1"
