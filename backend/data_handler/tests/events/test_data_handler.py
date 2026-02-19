"""Integration tests for the events data handler."""

from datetime import UTC, date, datetime
from unittest.mock import MagicMock, patch

from sqlalchemy.orm import Session

from data_handler.events.data_handler import (
    _upsert_events,
    fetch_and_store_events,
)
from data_handler.events.parsing_utils import ParsedEvent
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
    latitude: float = 53.3478,
    longitude: float = -6.2297,
    event_date: date = date(2026, 3, 1),
    start_time: datetime = datetime(2026, 3, 1, 20, 0, 0, tzinfo=UTC),
    end_time: datetime | None = datetime(2026, 3, 1, 23, 0, 0, tzinfo=UTC),
    is_high_impact: bool = True,
    estimated_attendance: int | None = None,
) -> ParsedEvent:
    return ParsedEvent(
        source=source,
        source_id=source_id,
        event_name=event_name,
        event_type=event_type,
        venue_name=venue_name,
        latitude=latitude,
        longitude=longitude,
        event_date=event_date,
        start_time=start_time,
        end_time=end_time,
        is_high_impact=is_high_impact,
        estimated_attendance=estimated_attendance,
    )


FETCHED_AT = datetime(2026, 2, 10, 12, 0, 0, tzinfo=UTC)


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

    def test_empty_list_returns_zero(self, db_session: Session) -> None:
        """An empty event list inserts nothing and returns 0."""
        count = _upsert_events(db_session, [], FETCHED_AT)
        db_session.commit()

        assert count == 0
        assert_row_count(db_session, "events", 0)


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
        """Events from Ticketmaster are fetched and stored."""
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
