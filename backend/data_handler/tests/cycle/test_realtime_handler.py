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

from data_handler.cycle.realtime_handler import (
    _transform_station_records,
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
