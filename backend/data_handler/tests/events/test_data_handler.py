"""Integration tests for the events data handler."""

from datetime import UTC, date, datetime
from unittest.mock import MagicMock, patch

from sqlalchemy import text
from sqlalchemy.orm import Session

from data_handler.events.data_handler import (
    _upsert_events,
    _upsert_venues,
    fetch_and_store_events,
    fetch_and_store_venues,
)
from data_handler.events.parsing_utils import ParsedEvent, ParsedVenue
from data_handler.settings.database_settings import get_db_settings
from tests.utils import ANY, assert_row_count, assert_rows

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_parsed_event(  # noqa: PLR0913
    *,
    source: str = "ticketmaster",
    source_id: str = "tm_001",
    event_name: str = "Test Concert",
    event_type: str = "Music",
    venue_name: str = "3Arena",
    venue_ticketmaster_id: str = "KovZpZAEdFtA",
    latitude: float = 53.3478,
    longitude: float = -6.2297,
    event_date: date = date(2026, 3, 1),
    start_time: datetime = datetime(2026, 3, 1, 20, 0, 0, tzinfo=UTC),
    end_time: datetime | None = datetime(2026, 3, 1, 23, 0, 0, tzinfo=UTC),
    is_high_impact: bool = False,
    estimated_attendance: int | None = None,
) -> ParsedEvent:
    return ParsedEvent(
        source=source,
        source_id=source_id,
        event_name=event_name,
        event_type=event_type,
        venue_name=venue_name,
        venue_ticketmaster_id=venue_ticketmaster_id,
        latitude=latitude,
        longitude=longitude,
        event_date=event_date,
        start_time=start_time,
        end_time=end_time,
        is_high_impact=is_high_impact,
        estimated_attendance=estimated_attendance,
    )


def _make_parsed_venue(  # noqa: PLR0913
    *,
    ticketmaster_id: str = "KovZpZAEdFtA",
    name: str = "3Arena",
    address: str | None = "North Wall Quay",
    city: str | None = "Dublin",
    latitude: float = 53.3478,
    longitude: float = -6.2297,
) -> ParsedVenue:
    return ParsedVenue(
        ticketmaster_id=ticketmaster_id,
        name=name,
        address=address,
        city=city,
        latitude=latitude,
        longitude=longitude,
    )


FETCHED_AT = datetime(2026, 2, 10, 12, 0, 0, tzinfo=UTC)


# ---------------------------------------------------------------------------
# _upsert_venues
# ---------------------------------------------------------------------------


class TestUpsertVenues:
    """Tests for _upsert_venues."""

    def test_insert_single_venue(self, db_session: Session) -> None:
        """A single venue is inserted into the venues table."""
        venue = _make_parsed_venue()
        id_map = _upsert_venues(db_session, [venue])
        db_session.commit()

        assert "KovZpZAEdFtA" in id_map
        assert_row_count(db_session, "venues", 1)
        assert_rows(
            db_session,
            "venues",
            [
                {
                    "id": ANY,
                    "ticketmaster_id": "KovZpZAEdFtA",
                    "name": "3Arena",
                    "address": "North Wall Quay",
                    "city": "Dublin",
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "capacity": None,
                    "venue_size_tag": None,
                }
            ],
        )

    def test_upsert_updates_non_capacity_fields(self, db_session: Session) -> None:
        """ON CONFLICT updates name/address/city/lat/lon but preserves capacity."""
        venue = _make_parsed_venue(address="Old Address")
        _upsert_venues(db_session, [venue])
        db_session.commit()

        updated = _make_parsed_venue(address="New Address")
        _upsert_venues(db_session, [updated])
        db_session.commit()

        assert_row_count(db_session, "venues", 1)
        assert_rows(
            db_session,
            "venues",
            [
                {
                    "id": ANY,
                    "ticketmaster_id": "KovZpZAEdFtA",
                    "name": "3Arena",
                    "address": "New Address",
                    "city": "Dublin",
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "capacity": None,
                    "venue_size_tag": None,
                }
            ],
        )

    def test_capacity_sets_venue_size_tag(self, db_session: Session) -> None:
        """Setting capacity on a venue auto-computes venue_size_tag via DB generated column."""
        venue = _make_parsed_venue(name="3Arena")
        _upsert_venues(db_session, [venue])
        db_session.commit()

        schema = get_db_settings().postgres_schema
        db_session.execute(
            text(
                f"UPDATE {schema}.venues SET capacity = 13000"
                " WHERE ticketmaster_id = 'KovZpZAEdFtA'"
            )
        )
        db_session.commit()

        assert_rows(
            db_session,
            "venues",
            [
                {
                    "id": ANY,
                    "ticketmaster_id": "KovZpZAEdFtA",
                    "name": "3Arena",
                    "address": "North Wall Quay",
                    "city": "Dublin",
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "capacity": 13000,
                    "venue_size_tag": "arena",
                }
            ],
        )

    def test_empty_list_returns_empty_map(self, db_session: Session) -> None:
        """An empty venue list inserts nothing and returns an empty map."""
        id_map = _upsert_venues(db_session, [])
        db_session.commit()

        assert id_map == {}
        assert_row_count(db_session, "venues", 0)

    def test_returns_id_and_tag_map(self, db_session: Session) -> None:
        """_upsert_venues returns a dict mapping ticketmaster_id to (db_id, venue_size_tag)."""
        venues = [
            _make_parsed_venue(ticketmaster_id="v1", name="Venue A"),
            _make_parsed_venue(ticketmaster_id="v2", name="Venue B"),
        ]
        id_map = _upsert_venues(db_session, venues)
        db_session.commit()

        assert set(id_map.keys()) == {"v1", "v2"}
        for db_id, tag in id_map.values():
            assert isinstance(db_id, int)
            assert tag is None  # no capacity set yet


# ---------------------------------------------------------------------------
# _upsert_events
# ---------------------------------------------------------------------------


class TestUpsertEvents:
    """Tests for _upsert_events."""

    def test_insert_single_event(self, db_session: Session) -> None:
        """A single new event is inserted into the events table."""
        event = _make_parsed_event()

        count = _upsert_events(db_session, [event], FETCHED_AT)
        db_session.commit()

        assert count == 1
        assert_row_count(db_session, "events", 1)
        assert_rows(
            db_session,
            "events",
            [
                {
                    "id": ANY,
                    "source": "ticketmaster",
                    "source_id": "tm_001",
                    "event_name": "Test Concert",
                    "event_type": "Music",
                    "venue_name": "3Arena",
                    "venue_id": None,
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "event_date": date(2026, 3, 1),
                    "start_time": datetime(2026, 3, 1, 20, 0, 0),
                    "end_time": datetime(2026, 3, 1, 23, 0, 0),
                    "is_high_impact": False,
                    "estimated_attendance": None,
                    "fetched_at": ANY,
                }
            ],
        )

    def test_insert_multiple_events(self, db_session: Session) -> None:
        """Multiple events are all inserted."""
        events = [
            _make_parsed_event(source_id="tm_001"),
            _make_parsed_event(
                source_id="tm_002",
                event_name="Second Concert",
                venue_name="Aviva Stadium",
            ),
        ]

        count = _upsert_events(db_session, events, FETCHED_AT)
        db_session.commit()

        assert count == 2
        assert_row_count(db_session, "events", 2)

    def test_upsert_updates_existing_event(self, db_session: Session) -> None:
        """Re-inserting an event with the same source+source_id updates its fields."""
        original = _make_parsed_event(event_name="Original Name")
        _upsert_events(db_session, [original], FETCHED_AT)
        db_session.commit()

        updated = _make_parsed_event(event_name="Updated Name")
        updated_fetched_at = datetime(2026, 2, 11, 12, 0, 0, tzinfo=UTC)
        _upsert_events(db_session, [updated], updated_fetched_at)
        db_session.commit()

        assert_row_count(db_session, "events", 1)
        assert_rows(
            db_session,
            "events",
            [
                {
                    "id": ANY,
                    "source": "ticketmaster",
                    "source_id": "tm_001",
                    "event_name": "Updated Name",
                    "event_type": "Music",
                    "venue_name": "3Arena",
                    "venue_id": None,
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "event_date": date(2026, 3, 1),
                    "start_time": datetime(2026, 3, 1, 20, 0, 0),
                    "end_time": datetime(2026, 3, 1, 23, 0, 0),
                    "is_high_impact": False,
                    "estimated_attendance": None,
                    "fetched_at": ANY,
                }
            ],
        )

    def test_empty_list_returns_zero(self, db_session: Session) -> None:
        """An empty event list inserts nothing and returns 0."""
        count = _upsert_events(db_session, [], FETCHED_AT)
        db_session.commit()

        assert count == 0
        assert_row_count(db_session, "events", 0)


# ---------------------------------------------------------------------------
# _upsert_events with venue FK
# ---------------------------------------------------------------------------


class TestUpsertEventsWithVenueFK:
    """Tests for venue FK resolution and is_high_impact computation in _upsert_events."""

    def test_venue_id_and_impact_resolved_for_arena(self, db_session: Session) -> None:
        """Events linked to an arena-tagged venue get venue_id set and is_high_impact=True."""
        venue = _make_parsed_venue(name="3Arena")
        id_map_initial = _upsert_venues(db_session, [venue])
        db_session.commit()

        venue_db_id = id_map_initial["KovZpZAEdFtA"][0]

        # Construct the id_map as if the venue had capacity=13000 (arena tag)
        venue_id_map: dict[str, tuple[int, str | None]] = {
            "KovZpZAEdFtA": (venue_db_id, "arena")
        }

        event = _make_parsed_event(venue_ticketmaster_id="KovZpZAEdFtA")
        _upsert_events(db_session, [event], FETCHED_AT, venue_id_map=venue_id_map)
        db_session.commit()

        assert_rows(
            db_session,
            "events",
            [
                {
                    "id": ANY,
                    "source": "ticketmaster",
                    "source_id": "tm_001",
                    "event_name": "Test Concert",
                    "event_type": "Music",
                    "venue_name": "3Arena",
                    "venue_id": venue_db_id,
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "event_date": date(2026, 3, 1),
                    "start_time": datetime(2026, 3, 1, 20, 0, 0),
                    "end_time": datetime(2026, 3, 1, 23, 0, 0),
                    "is_high_impact": True,
                    "estimated_attendance": None,
                    "fetched_at": ANY,
                }
            ],
        )

    def test_venue_id_null_and_impact_false_when_not_in_map(
        self, db_session: Session
    ) -> None:
        """Events whose venue_ticketmaster_id is not in the map get venue_id=None and is_high_impact=False."""
        event = _make_parsed_event(venue_ticketmaster_id="nonexistent_id")
        _upsert_events(db_session, [event], FETCHED_AT, venue_id_map={})
        db_session.commit()

        assert_rows(
            db_session,
            "events",
            [
                {
                    "id": ANY,
                    "source": "ticketmaster",
                    "source_id": "tm_001",
                    "event_name": "Test Concert",
                    "event_type": "Music",
                    "venue_name": "3Arena",
                    "venue_id": None,
                    "latitude": 53.3478,
                    "longitude": -6.2297,
                    "event_date": date(2026, 3, 1),
                    "start_time": datetime(2026, 3, 1, 20, 0, 0),
                    "end_time": datetime(2026, 3, 1, 23, 0, 0),
                    "is_high_impact": False,
                    "estimated_attendance": None,
                    "fetched_at": ANY,
                }
            ],
        )

    def test_major_stadium_tag_is_high_impact(self, db_session: Session) -> None:
        """Events at a major_stadium-tagged venue are also high-impact."""
        venue = _make_parsed_venue(name="Croke Park", ticketmaster_id="croke_tm")
        id_map_initial = _upsert_venues(db_session, [venue])
        db_session.commit()

        venue_db_id = id_map_initial["croke_tm"][0]
        venue_id_map: dict[str, tuple[int, str | None]] = {
            "croke_tm": (venue_db_id, "major_stadium")
        }

        event = _make_parsed_event(
            source_id="tm_croke",
            venue_name="Croke Park",
            venue_ticketmaster_id="croke_tm",
        )
        _upsert_events(db_session, [event], FETCHED_AT, venue_id_map=venue_id_map)
        db_session.commit()

        assert_rows(
            db_session,
            "events",
            [
                {
                    "id": ANY,
                    "source": ANY,
                    "source_id": "tm_croke",
                    "event_name": ANY,
                    "event_type": ANY,
                    "venue_name": ANY,
                    "venue_id": venue_db_id,
                    "latitude": ANY,
                    "longitude": ANY,
                    "event_date": ANY,
                    "start_time": ANY,
                    "end_time": ANY,
                    "is_high_impact": True,
                    "estimated_attendance": ANY,
                    "fetched_at": ANY,
                }
            ],
        )

    def test_theatre_tag_is_not_high_impact(self, db_session: Session) -> None:
        """Events at a theatre-tagged venue are not high-impact."""
        venue = _make_parsed_venue(name="Olympia Theatre", ticketmaster_id="olympia_tm")
        id_map_initial = _upsert_venues(db_session, [venue])
        db_session.commit()

        venue_db_id = id_map_initial["olympia_tm"][0]
        venue_id_map: dict[str, tuple[int, str | None]] = {
            "olympia_tm": (venue_db_id, "theatre")
        }

        event = _make_parsed_event(
            source_id="tm_olympia",
            venue_name="Olympia Theatre",
            venue_ticketmaster_id="olympia_tm",
        )
        _upsert_events(db_session, [event], FETCHED_AT, venue_id_map=venue_id_map)
        db_session.commit()

        assert_rows(
            db_session,
            "events",
            [
                {
                    "id": ANY,
                    "source": ANY,
                    "source_id": "tm_olympia",
                    "event_name": ANY,
                    "event_type": ANY,
                    "venue_name": ANY,
                    "venue_id": venue_db_id,
                    "latitude": ANY,
                    "longitude": ANY,
                    "event_date": ANY,
                    "start_time": ANY,
                    "end_time": ANY,
                    "is_high_impact": False,
                    "estimated_attendance": ANY,
                    "fetched_at": ANY,
                }
            ],
        )


# ---------------------------------------------------------------------------
# fetch_and_store_venues
# ---------------------------------------------------------------------------


class TestFetchAndStoreVenues:
    """Tests for fetch_and_store_venues."""

    @patch("data_handler.events.data_handler.get_ticketmaster_client")
    def test_venues_stored(
        self,
        mock_get_client: MagicMock,
        db_session: Session,
    ) -> None:
        """Venues from Ticketmaster are fetched and stored."""
        mock_tm = MagicMock()
        mock_tm.fetch_venues.return_value = {
            "_embedded": {
                "venues": [
                    {
                        "id": "v1",
                        "name": "Croke Park",
                        "location": {
                            "latitude": "53.36086568",
                            "longitude": "-6.25101015",
                        },
                        "address": {"line1": "Jones' Road"},
                        "city": {"name": "Dublin"},
                    }
                ]
            }
        }
        mock_get_client.return_value = mock_tm

        count = fetch_and_store_venues(session=db_session)

        assert count == 1
        assert_row_count(db_session, "venues", 1)

    @patch("data_handler.events.data_handler.get_ticketmaster_client")
    def test_empty_response_stores_nothing(
        self,
        mock_get_client: MagicMock,
        db_session: Session,
    ) -> None:
        """Empty API response results in zero venues stored."""
        mock_tm = MagicMock()
        mock_tm.fetch_venues.return_value = {}
        mock_get_client.return_value = mock_tm

        count = fetch_and_store_venues(session=db_session)

        assert count == 0
        assert_row_count(db_session, "venues", 0)


# ---------------------------------------------------------------------------
# fetch_and_store_events
# ---------------------------------------------------------------------------


class TestFetchAndStoreEvents:
    """Tests for fetch_and_store_events (end-to-end with mocked clients)."""

    @patch("data_handler.events.data_handler.get_ticketmaster_client")
    def test_ticketmaster_events_stored(
        self,
        mock_get_tm_client: MagicMock,
        db_session: Session,
    ) -> None:
        """Events from Ticketmaster are fetched, stored, and venues populated."""
        mock_tm = MagicMock()
        mock_tm.fetch_events.return_value = {
            "_embedded": {
                "events": [
                    {
                        "id": "tm_e1",
                        "name": "Concert A",
                        "classifications": [{"segment": {"name": "Music"}}],
                        "dates": {
                            "start": {
                                "localDate": "2026-03-01",
                                "dateTime": "2026-03-01T20:00:00Z",
                            }
                        },
                        "_embedded": {
                            "venues": [
                                {
                                    "id": "KovZpZAEdFtA",
                                    "name": "3Arena",
                                    "location": {
                                        "latitude": "53.3478",
                                        "longitude": "-6.2297",
                                    },
                                }
                            ]
                        },
                    }
                ]
            }
        }
        mock_get_tm_client.return_value = mock_tm

        count = fetch_and_store_events(session=db_session)

        assert count == 1
        assert_row_count(db_session, "events", 1)
        assert_row_count(db_session, "venues", 1)

    @patch("data_handler.events.data_handler.get_ticketmaster_client")
    def test_empty_api_response(
        self,
        mock_get_tm_client: MagicMock,
        db_session: Session,
    ) -> None:
        """Empty API response results in zero events stored."""
        mock_tm = MagicMock()
        mock_tm.fetch_events.return_value = {}
        mock_get_tm_client.return_value = mock_tm

        count = fetch_and_store_events(session=db_session)

        assert count == 0
        assert_row_count(db_session, "events", 0)

    @patch("data_handler.events.data_handler.get_ticketmaster_client")
    def test_ticketmaster_failure_stores_nothing(
        self,
        mock_get_tm_client: MagicMock,
        db_session: Session,
    ) -> None:
        """If Ticketmaster fails, no events are stored."""
        mock_tm = MagicMock()
        mock_tm.fetch_events.side_effect = Exception("API down")
        mock_get_tm_client.return_value = mock_tm

        count = fetch_and_store_events(session=db_session)

        assert count == 0
        assert_row_count(db_session, "events", 0)
