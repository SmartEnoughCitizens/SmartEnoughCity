"""Tests for real-time station snapshot handler."""

import sys
from datetime import UTC, datetime
from unittest.mock import Mock

from sqlalchemy.orm import DeclarativeBase

# Mock db module before importing handler
if "data_handler.db" not in sys.modules:

    class _Base(DeclarativeBase):
        pass

    _mock_db = type(sys)("data_handler.db")
    _mock_db.Base = _Base
    _mock_db.SessionLocal = Mock()
    sys.modules["data_handler.db"] = _mock_db

import pytest

from data_handler.cycle.realtime_handler import (
    _parse_last_reported,
    _transform_station_records,
    fetch_and_store_station_snapshots,
)


class TestTransformStationRecords:
    """Test transforming API station status to database records."""

    def test_transforms_complete_station_record(self) -> None:
        """Test transforming a fully populated station status record."""
        stations = [
            {
                "station_id": "1",
                "num_bikes_available": 8,
                "num_docks_available": 22,
                "num_bikes_disabled": 1,
                "num_docks_disabled": 2,
                "is_installed": True,
                "is_renting": True,
                "is_returning": True,
                "last_reported": "2026-01-22T17:30:00+00:00",
            }
        ]
        fetch_ts = datetime(2026, 1, 22, 17, 31, 0, tzinfo=UTC)

        records = _transform_station_records(stations, fetch_ts)

        assert len(records) == 1
        rec = records[0]
        assert rec["station_id"] == 1
        assert rec["timestamp"] == fetch_ts
        assert rec["available_bikes"] == 8
        assert rec["available_docks"] == 22
        assert rec["disabled_bikes"] == 1
        assert rec["disabled_docks"] == 2
        assert rec["is_installed"] is True
        assert rec["is_renting"] is True
        assert rec["is_returning"] is True
        assert rec["last_reported"] == datetime(2026, 1, 22, 17, 30, 0, tzinfo=UTC)

    def test_defaults_disabled_counts_to_zero(self) -> None:
        """Test that missing disabled fields default to 0."""
        stations = [
            {
                "station_id": "1",
                "num_bikes_available": 5,
                "num_docks_available": 10,
                # No disabled fields
                "is_installed": True,
                "is_renting": True,
                "is_returning": True,
                "last_reported": "2026-01-22T17:30:00+00:00",
            }
        ]
        fetch_ts = datetime(2026, 1, 22, 17, 31, 0, tzinfo=UTC)

        records = _transform_station_records(stations, fetch_ts)

        assert records[0]["disabled_bikes"] == 0
        assert records[0]["disabled_docks"] == 0

    def test_transforms_multiple_stations(self) -> None:
        """Test transforming multiple station records."""
        stations = [
            {
                "station_id": "1",
                "num_bikes_available": 5,
                "num_docks_available": 10,
                "is_installed": True,
                "is_renting": True,
                "is_returning": True,
                "last_reported": "2026-01-22T17:30:00+00:00",
            },
            {
                "station_id": "2",
                "num_bikes_available": 3,
                "num_docks_available": 7,
                "is_installed": True,
                "is_renting": False,
                "is_returning": True,
                "last_reported": "2026-01-22T17:30:00+00:00",
            },
        ]
        fetch_ts = datetime(2026, 1, 22, 17, 31, 0, tzinfo=UTC)

        records = _transform_station_records(stations, fetch_ts)

        assert len(records) == 2
        assert records[0]["station_id"] == 1
        assert records[1]["station_id"] == 2
        assert records[1]["is_renting"] is False

    def test_handles_unix_timestamp_last_reported(self) -> None:
        """Test that integer Unix timestamps are parsed correctly."""
        stations = [
            {
                "station_id": "1",
                "num_bikes_available": 5,
                "num_docks_available": 10,
                "is_installed": True,
                "is_renting": True,
                "is_returning": True,
                "last_reported": 1769194200,  # Unix timestamp
            }
        ]
        fetch_ts = datetime(2026, 1, 22, 17, 31, 0, tzinfo=UTC)

        records = _transform_station_records(stations, fetch_ts)

        assert isinstance(records[0]["last_reported"], datetime)
        assert records[0]["last_reported"].tzinfo is not None

    def test_empty_stations_returns_empty_list(self) -> None:
        """Test that empty stations list returns empty records."""
        fetch_ts = datetime(2026, 1, 22, 17, 31, 0, tzinfo=UTC)

        records = _transform_station_records([], fetch_ts)

        assert records == []


class TestParseLastReported:
    """Test parsing last_reported values."""

    def test_parses_unix_timestamp(self) -> None:
        """Test parsing a Unix timestamp integer."""
        result = _parse_last_reported(1769194200)
        assert isinstance(result, datetime)
        assert result.tzinfo is not None

    def test_parses_iso_string(self) -> None:
        """Test parsing an ISO format timestamp string."""
        result = _parse_last_reported("2026-01-22T17:30:00+00:00")
        assert result == datetime(2026, 1, 22, 17, 30, 0, tzinfo=UTC)

    def test_handles_negative_unix_timestamp(self) -> None:
        """Test that negative Unix timestamps return epoch minimum."""
        result = _parse_last_reported(-1)
        assert result == datetime(1, 1, 1, tzinfo=UTC)

    def test_parses_float_timestamp(self) -> None:
        """Test that float timestamps are handled."""
        result = _parse_last_reported(1769194200.5)
        assert isinstance(result, datetime)
        assert result.tzinfo is not None


class TestFetchAndStoreStationSnapshots:
    """Test the full fetch and store workflow."""

    def _make_station_record(self, station_id: str = "1") -> dict:
        return {
            "station_id": station_id,
            "num_bikes_available": 5,
            "num_docks_available": 10,
            "num_bikes_disabled": 0,
            "num_docks_disabled": 0,
            "is_installed": True,
            "is_renting": True,
            "is_returning": True,
            "last_reported": "2026-01-22T17:30:00+00:00",
        }

    def test_fetches_and_stores_snapshots(
        self, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Test successful fetch and store of station snapshots."""
        mock_client = Mock()
        mock_client.fetch_station_status.return_value = [
            self._make_station_record("1"),
            self._make_station_record("2"),
        ]
        monkeypatch.setattr(
            "data_handler.cycle.realtime_handler.get_jcdecaux_client",
            lambda: mock_client,
        )

        mock_session = Mock()
        mock_session_ctx = Mock()
        mock_session_ctx.__enter__ = Mock(return_value=mock_session)
        mock_session_ctx.__exit__ = Mock(return_value=False)
        monkeypatch.setattr(
            "data_handler.cycle.realtime_handler.SessionLocal",
            lambda: mock_session_ctx,
        )

        fetch_and_store_station_snapshots()

        mock_client.fetch_station_status.assert_called_once()
        assert mock_session.execute.call_count == 2  # snapshots + history
        mock_session.commit.assert_called_once()

    def test_returns_early_on_empty_data(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Test that empty station data returns early without DB ops."""
        mock_client = Mock()
        mock_client.fetch_station_status.return_value = []
        monkeypatch.setattr(
            "data_handler.cycle.realtime_handler.get_jcdecaux_client",
            lambda: mock_client,
        )

        fetch_and_store_station_snapshots()

        mock_client.fetch_station_status.assert_called_once()

    def test_raises_on_db_error(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Test that database errors are propagated."""
        mock_client = Mock()
        mock_client.fetch_station_status.return_value = [
            self._make_station_record(),
        ]
        monkeypatch.setattr(
            "data_handler.cycle.realtime_handler.get_jcdecaux_client",
            lambda: mock_client,
        )

        mock_session = Mock()
        mock_session.execute.side_effect = Exception("DB error")
        mock_session_ctx = Mock()
        mock_session_ctx.__enter__ = Mock(return_value=mock_session)
        mock_session_ctx.__exit__ = Mock(return_value=False)
        monkeypatch.setattr(
            "data_handler.cycle.realtime_handler.SessionLocal",
            lambda: mock_session_ctx,
        )

        with pytest.raises(Exception, match="DB error"):
            fetch_and_store_station_snapshots()
