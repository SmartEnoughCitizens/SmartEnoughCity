"""Tests for tram delay history handler."""

from datetime import datetime, time
from unittest.mock import MagicMock, Mock, patch

import pytest
from sqlalchemy.orm import Session

from data_handler.tram.delay_history_handler import (
    _build_name_to_gtfs_ids,
    _find_gtfs_ids,
    _find_next_scheduled,
    _get_dublin_time,
    store_delay_snapshot,
)
from data_handler.tram.models import TramStop

# ── _get_dublin_time unit tests ──────────────────────────────────────


class TestGetDublinTime:
    """Test retrieval of current Dublin time from DB."""

    def test_returns_time_from_db_datetime(self) -> None:
        """When DB returns a datetime, extract its .time() component."""
        session = MagicMock()
        db_now = datetime(2025, 6, 15, 14, 30, 0)
        session.execute.return_value.scalar.return_value = db_now
        result = _get_dublin_time(session)
        assert result == time(14, 30, 0)

    def test_returns_time_object_directly(self) -> None:
        """When DB returns a time object directly, use it as-is."""
        session = MagicMock()
        db_time = time(9, 15, 0)
        session.execute.return_value.scalar.return_value = db_time
        result = _get_dublin_time(session)
        assert result == time(9, 15, 0)

    def test_falls_back_to_utc_when_db_returns_none(self) -> None:
        """When DB returns None, fall back to UTC now."""
        session = MagicMock()
        session.execute.return_value.scalar.return_value = None
        result = _get_dublin_time(session)
        assert isinstance(result, time)


# ── _build_name_to_gtfs_ids unit tests ───────────────────────────────


class TestBuildNameToGtfsIds:
    """Test building the stop name → GTFS ID mapping."""

    def test_builds_mapping_from_stops(self, db_session: Session) -> None:
        """Multiple stops with same name grouped under one key."""
        db_session.add(
            TramStop(id="STOP1", code=1, name="Harcourt", lat=53.33, lon=-6.26)
        )
        db_session.add(
            TramStop(id="STOP2", code=2, name="Harcourt", lat=53.33, lon=-6.26)
        )
        db_session.add(
            TramStop(id="STOP3", code=3, name="Abbey Street", lat=53.35, lon=-6.26)
        )
        db_session.commit()

        result = _build_name_to_gtfs_ids(db_session)
        assert result == {
            "harcourt": ["STOP1", "STOP2"],
            "abbey street": ["STOP3"],
        }

    def test_returns_empty_mapping_when_no_stops(self, db_session: Session) -> None:
        """No stops in DB produces empty mapping."""
        result = _build_name_to_gtfs_ids(db_session)
        assert result == {}


# ── _find_gtfs_ids unit tests ────────────────────────────────────────


class TestFindGtfsIds:
    """Test GTFS ID lookup by exact and partial name match."""

    def test_exact_match(self) -> None:
        mapping = {"harcourt": ["S1", "S2"], "abbey street": ["S3"]}
        assert _find_gtfs_ids("Harcourt", mapping) == ["S1", "S2"]

    def test_partial_match_stop_name_in_gtfs_name(self) -> None:
        """Partial match: GTFS name contains stop name."""
        mapping = {"harcourt street": ["S1"]}
        assert _find_gtfs_ids("Harcourt", mapping) == ["S1"]

    def test_partial_match_gtfs_name_in_stop_name(self) -> None:
        """Partial match: stop name contains GTFS name."""
        mapping = {"harcourt": ["S1"]}
        assert _find_gtfs_ids("Harcourt Street Station", mapping) == ["S1"]

    def test_no_match_returns_empty(self) -> None:
        mapping = {"harcourt": ["S1"]}
        assert _find_gtfs_ids("Sandyford", mapping) == []

    def test_case_insensitive(self) -> None:
        mapping = {"abbey street": ["S3"]}
        assert _find_gtfs_ids("ABBEY STREET", mapping) == ["S3"]


# ── _find_next_scheduled unit tests (mocked DB) ─────────────────────


class TestFindNextScheduled:
    """Test finding the next scheduled arrival time."""

    def test_returns_nearest_arrival(self) -> None:
        session = MagicMock()
        session.execute.return_value.scalar.return_value = time(10, 15)
        result = _find_next_scheduled(session, ["STOP1"], time(10, 0))
        assert result == time(10, 15)

    def test_returns_none_when_no_arrivals(self) -> None:
        session = MagicMock()
        session.execute.return_value.scalar.return_value = None
        result = _find_next_scheduled(session, ["STOP1"], time(10, 0))
        assert result is None

    def test_picks_earliest_across_multiple_stops(self) -> None:
        session = MagicMock()
        # Return different times for two sequential calls
        session.execute.return_value.scalar.side_effect = [
            time(10, 30),
            time(10, 15),
        ]
        result = _find_next_scheduled(session, ["S1", "S2"], time(10, 0))
        assert result == time(10, 15)

    def test_skips_none_results(self) -> None:
        session = MagicMock()
        session.execute.return_value.scalar.side_effect = [None, time(10, 20)]
        result = _find_next_scheduled(session, ["S1", "S2"], time(10, 0))
        assert result == time(10, 20)


# ── store_delay_snapshot tests (fully mocked session) ────────────────


class TestStoreDelaySnapshot:
    """Tests for store_delay_snapshot with fully mocked DB session."""

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_no_forecasts_stores_nothing(self, mock_session_local: Mock) -> None:
        """When no forecasts exist, nothing is stored."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        # _get_dublin_time query
        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        # forecasts query
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = []

        mock_session.execute.side_effect = [dublin_time_result, forecasts_result]

        store_delay_snapshot()

        mock_session.commit.assert_not_called()
        mock_session.close.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_stores_delay_when_tram_is_late(self, mock_session_local: Mock) -> None:
        """A delayed tram produces a TramDelayHistory record via session.add."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        mock_forecast = MagicMock()
        mock_forecast.stop_id = "STG"
        mock_forecast.direction = "Inbound"
        mock_forecast.due_mins = 15
        mock_forecast.line = "green"
        mock_forecast.destination = "Broombridge"

        # 1) _get_dublin_time
        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        # 2) forecasts join query
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = [
            (mock_forecast, "St. Stephen's Green"),
        ]
        # 3) _build_name_to_gtfs_ids query
        gtfs_stops_result = MagicMock()
        gtfs_stops_result.all.return_value = [
            ("GTFS_STG", "St. Stephen's Green"),
        ]
        # 4) _find_next_scheduled query — scheduled at 10:10
        #    predicted = 10:00 + 15 = 10:15, delay = 10:15 - 10:10 = 5 min
        scheduled_result = MagicMock()
        scheduled_result.scalar.return_value = time(10, 10)

        mock_session.execute.side_effect = [
            dublin_time_result,
            forecasts_result,
            gtfs_stops_result,
            scheduled_result,
        ]

        store_delay_snapshot()

        mock_session.add.assert_called_once()
        added_record = mock_session.add.call_args[0][0]
        assert added_record.delay_mins == 5
        assert added_record.stop_id == "STG"
        assert added_record.direction == "Inbound"
        assert added_record.due_mins == 15
        mock_session.commit.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_no_delay_when_tram_is_on_time(self, mock_session_local: Mock) -> None:
        """A tram arriving on time or early does not produce a delay record."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        mock_forecast = MagicMock()
        mock_forecast.stop_id = "STG"
        mock_forecast.direction = "Inbound"
        mock_forecast.due_mins = 5
        mock_forecast.line = "green"
        mock_forecast.destination = "Broombridge"

        # predicted = 10:00 + 5 = 10:05, scheduled = 10:10 → delay = -5 → skipped
        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = [
            (mock_forecast, "St. Stephen's Green"),
        ]
        gtfs_stops_result = MagicMock()
        gtfs_stops_result.all.return_value = [
            ("GTFS_STG", "St. Stephen's Green"),
        ]
        scheduled_result = MagicMock()
        scheduled_result.scalar.return_value = time(10, 10)

        mock_session.execute.side_effect = [
            dublin_time_result,
            forecasts_result,
            gtfs_stops_result,
            scheduled_result,
        ]

        store_delay_snapshot()

        mock_session.add.assert_not_called()
        mock_session.commit.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_skips_forecast_with_no_gtfs_match(self, mock_session_local: Mock) -> None:
        """Forecasts for stops without GTFS matches are skipped."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        mock_forecast = MagicMock()
        mock_forecast.stop_id = "STG"
        mock_forecast.direction = "Inbound"
        mock_forecast.due_mins = 15
        mock_forecast.line = "green"
        mock_forecast.destination = "Broombridge"

        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = [
            (mock_forecast, "Unknown Stop Name"),
        ]
        # Empty GTFS mapping — no match
        gtfs_stops_result = MagicMock()
        gtfs_stops_result.all.return_value = []

        mock_session.execute.side_effect = [
            dublin_time_result,
            forecasts_result,
            gtfs_stops_result,
        ]

        store_delay_snapshot()

        mock_session.add.assert_not_called()
        mock_session.commit.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_skips_forecast_with_none_due_mins(self, mock_session_local: Mock) -> None:
        """Forecasts with due_mins=None are skipped during grouping."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        mock_forecast = MagicMock()
        mock_forecast.stop_id = "STG"
        mock_forecast.direction = "Inbound"
        mock_forecast.due_mins = None

        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = [
            (mock_forecast, "St. Stephen's Green"),
        ]
        # soonest dict will be empty → still runs GTFS lookup but no iterations
        gtfs_stops_result = MagicMock()
        gtfs_stops_result.all.return_value = []

        mock_session.execute.side_effect = [
            dublin_time_result,
            forecasts_result,
            gtfs_stops_result,
        ]

        store_delay_snapshot()

        mock_session.add.assert_not_called()
        mock_session.commit.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_skips_when_no_scheduled_arrival(self, mock_session_local: Mock) -> None:
        """When no next scheduled arrival is found, the forecast is skipped."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        mock_forecast = MagicMock()
        mock_forecast.stop_id = "STG"
        mock_forecast.direction = "Inbound"
        mock_forecast.due_mins = 15
        mock_forecast.line = "green"
        mock_forecast.destination = "Broombridge"

        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = [
            (mock_forecast, "St. Stephen's Green"),
        ]
        gtfs_stops_result = MagicMock()
        gtfs_stops_result.all.return_value = [
            ("GTFS_STG", "St. Stephen's Green"),
        ]
        # No scheduled arrival found
        scheduled_result = MagicMock()
        scheduled_result.scalar.return_value = None

        mock_session.execute.side_effect = [
            dublin_time_result,
            forecasts_result,
            gtfs_stops_result,
            scheduled_result,
        ]

        store_delay_snapshot()

        mock_session.add.assert_not_called()
        mock_session.commit.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_exception_triggers_rollback(self, mock_session_local: Mock) -> None:
        """On exception, session is rolled back and exception re-raised."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session
        mock_session.execute.side_effect = RuntimeError("DB error")

        with pytest.raises(RuntimeError, match="DB error"):
            store_delay_snapshot()

        mock_session.rollback.assert_called_once()
        mock_session.close.assert_called_once()

    @patch("data_handler.tram.delay_history_handler.SessionLocal")
    def test_keeps_soonest_forecast_per_stop_direction(
        self, mock_session_local: Mock
    ) -> None:
        """When multiple forecasts exist for same stop+direction, keep soonest."""
        mock_session = MagicMock()
        mock_session_local.return_value = mock_session

        forecast_far = MagicMock()
        forecast_far.stop_id = "STG"
        forecast_far.direction = "Inbound"
        forecast_far.due_mins = 20
        forecast_far.line = "green"
        forecast_far.destination = "Broombridge"

        forecast_near = MagicMock()
        forecast_near.stop_id = "STG"
        forecast_near.direction = "Inbound"
        forecast_near.due_mins = 15
        forecast_near.line = "green"
        forecast_near.destination = "Broombridge"

        dublin_time_result = MagicMock()
        dublin_time_result.scalar.return_value = time(10, 0)
        forecasts_result = MagicMock()
        forecasts_result.all.return_value = [
            (forecast_far, "St. Stephen's Green"),
            (forecast_near, "St. Stephen's Green"),
        ]
        gtfs_stops_result = MagicMock()
        gtfs_stops_result.all.return_value = [
            ("GTFS_STG", "St. Stephen's Green"),
        ]
        # scheduled at 10:10, soonest predicted = 10:15 → delay = 5
        scheduled_result = MagicMock()
        scheduled_result.scalar.return_value = time(10, 10)

        mock_session.execute.side_effect = [
            dublin_time_result,
            forecasts_result,
            gtfs_stops_result,
            scheduled_result,
        ]

        store_delay_snapshot()

        mock_session.add.assert_called_once()
        added = mock_session.add.call_args[0][0]
        assert added.due_mins == 15  # soonest was kept
        assert added.delay_mins == 5
