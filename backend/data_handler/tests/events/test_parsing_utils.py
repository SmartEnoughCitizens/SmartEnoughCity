"""Tests for events parsing utilities."""

from datetime import UTC, date, datetime

import pytest

from data_handler.events.parsing_utils import (
    HIGH_IMPACT_VENUES,
    determine_high_impact,
    parse_ticketmaster_event,
    parse_ticketmaster_response,
)

# ---------------------------------------------------------------------------
# Fixtures: sample API responses
# ---------------------------------------------------------------------------


def _make_ticketmaster_event(  # noqa: PLR0913
    *,
    event_id: str = "Z7r9jZ1AdJ1a0",
    name: str = "Coldplay: Music of the Spheres",
    segment: str = "Music",
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


# ---------------------------------------------------------------------------
# determine_high_impact
# ---------------------------------------------------------------------------


class TestDetermineHighImpact:
    """Tests for determine_high_impact."""

    @pytest.mark.parametrize("venue", list(HIGH_IMPACT_VENUES))
    def test_high_impact_venue_returns_true(self, venue: str) -> None:
        """Every venue in HIGH_IMPACT_VENUES is high-impact regardless of capacity."""
        assert determine_high_impact(venue, capacity_or_spectators=None) is True

    def test_unknown_venue_with_large_capacity_returns_true(self) -> None:
        """Unknown venue with capacity above threshold is high-impact."""
        assert determine_high_impact("Some Arena", capacity_or_spectators=10000) is True

    def test_unknown_venue_with_small_capacity_returns_false(self) -> None:
        """Unknown venue with capacity below threshold is not high-impact."""
        assert determine_high_impact("Small Club", capacity_or_spectators=500) is False

    def test_unknown_venue_with_no_capacity_returns_false(self) -> None:
        """Unknown venue with no capacity info is not high-impact."""
        assert (
            determine_high_impact("Mystery Venue", capacity_or_spectators=None) is False
        )

    def test_unknown_venue_at_exact_threshold_returns_true(self) -> None:
        """Capacity exactly at threshold is high-impact."""
        assert (
            determine_high_impact("Medium Arena", capacity_or_spectators=8000) is True
        )


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
        assert result.latitude == pytest.approx(53.36086568)
        assert result.longitude == pytest.approx(-6.25101015)
        assert result.event_date == date(2026, 2, 15)
        assert result.start_time == datetime(2026, 2, 15, 19, 0, 0, tzinfo=UTC)
        assert result.end_time == datetime(2026, 2, 15, 22, 30, 0, tzinfo=UTC)
        assert result.is_high_impact is True  # Croke Park is high-impact

    def test_event_without_end_time(self) -> None:
        """An event with no end time has end_time = None."""
        raw = _make_ticketmaster_event(end_datetime=None)
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.end_time is None

    def test_event_at_non_high_impact_venue(self) -> None:
        """An event at a small venue is not high-impact."""
        raw = _make_ticketmaster_event(
            venue_name="Whelan's",
            latitude="53.3364",
            longitude="-6.2631",
        )
        result = parse_ticketmaster_event(raw)

        assert result is not None
        assert result.is_high_impact is False

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
        assert result.is_high_impact is True


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

