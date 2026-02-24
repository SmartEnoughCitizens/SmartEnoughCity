from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest
from sqlalchemy.orm import Session

from data_handler.congestion_and_construction.data_handler import (
    _upsert_traffic_events,
    fetch_and_store_traffic_data,
)
from data_handler.congestion_and_construction.models import TrafficEventType
from data_handler.congestion_and_construction.parsing_utils import ParsedTrafficEvent
from tests.utils import ANY, assert_row_count, assert_rows


class TestUpsertTrafficEvents:
    """Tests for _upsert_traffic_events."""

    def test_inserts_events_with_source_id(self, db_session: Session) -> None:
        """Events with source_id are upserted into the database."""
        events = [
            ParsedTrafficEvent(
                event_type=TrafficEventType.CONGESTION,
                title="Heavy congestion on M50",
                description="Expect delays",
                lat=53.35,
                lon=-6.25,
                color="orange",
                source_id="evt-001",
            ),
        ]
        fetched_at = datetime(2026, 1, 29, 12, 0, 0)

        assert_row_count(db_session, "traffic_events", 0)
        count = _upsert_traffic_events(db_session, events, fetched_at)
        db_session.commit()

        assert count == 1
        assert_row_count(db_session, "traffic_events", 1)
        assert_rows(
            db_session,
            "traffic_events",
            [
                {
                    "id": ANY,
                    "event_type": "CONGESTION",
                    "title": "Heavy congestion on M50",
                    "description": "Expect delays",
                    "lat": 53.35,
                    "lon": -6.25,
                    "color": "orange",
                    "fetched_at": fetched_at,
                    "source_id": "evt-001",
                },
            ],
        )

    def test_inserts_events_without_source_id(self, db_session: Session) -> None:
        """Events without source_id are inserted as new rows."""
        events = [
            ParsedTrafficEvent(
                event_type=TrafficEventType.WARNING,
                title="Speed limit change",
                description=None,
                lat=53.40,
                lon=-6.30,
                color="blue",
                source_id=None,
            ),
        ]
        fetched_at = datetime(2026, 1, 29, 12, 0, 0)

        count = _upsert_traffic_events(db_session, events, fetched_at)
        db_session.commit()

        assert count == 1
        assert_row_count(db_session, "traffic_events", 1)
        assert_rows(
            db_session,
            "traffic_events",
            [
                {
                    "id": ANY,
                    "event_type": "WARNING",
                    "title": "Speed limit change",
                    "description": None,
                    "lat": 53.40,
                    "lon": -6.30,
                    "color": "blue",
                    "fetched_at": fetched_at,
                    "source_id": None,
                },
            ],
        )

    def test_upsert_updates_existing_event_by_source_id(
        self, db_session: Session
    ) -> None:
        """Re-inserting event with same source_id updates existing row."""
        fetched_at_1 = datetime(2026, 1, 29, 12, 0, 0)
        fetched_at_2 = datetime(2026, 1, 29, 13, 0, 0)

        events_v1 = [
            ParsedTrafficEvent(
                event_type=TrafficEventType.CONGESTION,
                title="Congestion on M50",
                description="Minor delay",
                lat=53.35,
                lon=-6.25,
                color="orange",
                source_id="evt-001",
            ),
        ]
        _upsert_traffic_events(db_session, events_v1, fetched_at_1)
        db_session.commit()
        assert_row_count(db_session, "traffic_events", 1)

        events_v2 = [
            ParsedTrafficEvent(
                event_type=TrafficEventType.CLOSURE_INCIDENT,
                title="Road closed on M50",
                description="Full closure",
                lat=53.36,
                lon=-6.26,
                color="red",
                source_id="evt-001",
            ),
        ]
        _upsert_traffic_events(db_session, events_v2, fetched_at_2)
        db_session.commit()

        # Should still be 1 row, updated in place
        assert_row_count(db_session, "traffic_events", 1)
        assert_rows(
            db_session,
            "traffic_events",
            [
                {
                    "id": ANY,
                    "event_type": "CLOSURE_INCIDENT",
                    "title": "Road closed on M50",
                    "description": "Full closure",
                    "lat": 53.36,
                    "lon": -6.26,
                    "color": "red",
                    "fetched_at": fetched_at_2,
                    "source_id": "evt-001",
                },
            ],
        )

    def test_mixed_events_with_and_without_source_id(self, db_session: Session) -> None:
        """Handles mix of events with and without source_id."""
        events = [
            ParsedTrafficEvent(
                event_type=TrafficEventType.CONGESTION,
                title="Congestion",
                description=None,
                lat=53.35,
                lon=-6.25,
                color="orange",
                source_id="evt-001",
            ),
            ParsedTrafficEvent(
                event_type=TrafficEventType.WARNING,
                title="Warning",
                description=None,
                lat=53.40,
                lon=-6.30,
                color="blue",
                source_id=None,
            ),
        ]
        fetched_at = datetime(2026, 1, 29, 12, 0, 0)

        count = _upsert_traffic_events(db_session, events, fetched_at)
        db_session.commit()

        assert count == 2
        assert_row_count(db_session, "traffic_events", 2)

    def test_empty_events_list(self, db_session: Session) -> None:
        """Empty events list inserts nothing."""
        fetched_at = datetime(2026, 1, 29, 12, 0, 0)
        count = _upsert_traffic_events(db_session, [], fetched_at)
        db_session.commit()

        assert count == 0
        assert_row_count(db_session, "traffic_events", 0)


class TestFetchAndStoreTrafficData:
    """Tests for fetch_and_store_traffic_data."""

    @patch("data_handler.congestion_and_construction.data_handler.TIIApiClient")
    def test_returns_zero_when_api_returns_none(
        self, mock_client_cls: MagicMock
    ) -> None:
        """Returns 0 when API returns None."""
        mock_client = MagicMock()
        mock_client.fetch_traffic_data.return_value = None
        mock_client_cls.return_value = mock_client

        result = fetch_and_store_traffic_data()
        assert result == 0

    @patch("data_handler.congestion_and_construction.data_handler.TIIApiClient")
    def test_processes_valid_api_response(
        self, mock_client_cls: MagicMock, db_session: Session
    ) -> None:
        """Processes and stores events from valid API response."""
        raw_data = [  # ← real data, not [...]
            {
                "data": {
                    "mapFeaturesQuery": {
                        "mapFeatures": [
                            {
                                "title": "Heavy congestion",
                                "features": [
                                    {
                                        "geometry": {
                                            "type": "Point",
                                            "coordinates": [-6.25, 53.35],
                                        },
                                        "properties": {"id": "evt-1"},
                                    }
                                ],
                            },
                        ]
                    }
                }
            }
        ]

        mock_client = MagicMock()
        mock_client.fetch_traffic_data.return_value = raw_data
        mock_client_cls.return_value = mock_client

        result = fetch_and_store_traffic_data(session=db_session)
        assert result == 1

    @patch("data_handler.congestion_and_construction.data_handler.TIIApiClient")
    def test_raises_on_api_failure(self, mock_client_cls: MagicMock) -> None:
        """Raises exception when API call fails."""
        mock_client = MagicMock()
        mock_client.fetch_traffic_data.side_effect = Exception("API down")
        mock_client_cls.return_value = mock_client

        with pytest.raises(Exception, match="API down"):
            fetch_and_store_traffic_data()
