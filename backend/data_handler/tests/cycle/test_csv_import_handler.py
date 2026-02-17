"""Tests for historical CSV import handler."""

import sys
from datetime import UTC, datetime
from pathlib import Path
from unittest.mock import Mock

import pytest
from sqlalchemy.orm import DeclarativeBase

# Mock db module before importing handler
if "data_handler.db" not in sys.modules:

    class _Base(DeclarativeBase):
        pass

    _mock_db = type(sys)("data_handler.db")
    _mock_db.Base = _Base
    _mock_db.SessionLocal = Mock()
    sys.modules["data_handler.db"] = _mock_db

from data_handler.cycle.csv_import_handler import (
    REQUIRED_HEADERS,
    import_station_history_csv,
    parse_station_history_csv_row,
)


class TestParseStationHistoryCsvRow:
    """Test transforming CSV rows to database records."""

    def test_parses_valid_row(self) -> None:
        """Test parsing a complete valid CSV row."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "8",
            "num_docks_available": "2",
            "is_installed": "true",
            "is_renting": "true",
            "is_returning": "true",
            "name": "Mary Street",
            "short_name": "001",
            "address": "Mary Street Dublin 1",
            "lat": "53.349316",
            "lon": "-6.262876",
            "region_id": "dublin_central",
            "capacity": "30",
        }

        result = parse_station_history_csv_row(row)

        assert result["station_id"] == 1
        assert result["available_bikes"] == 8
        assert result["available_docks"] == 2
        assert result["is_installed"] is True
        assert result["is_renting"] is True
        assert result["is_returning"] is True
        assert result["timestamp"] == datetime(2025, 7, 15, 14, 30, 0, tzinfo=UTC)

    def test_converts_string_booleans(self) -> None:
        """Test that string booleans are converted to Python booleans."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "false",
            "is_renting": "false",
            "is_returning": "true",
        }

        result = parse_station_history_csv_row(row)

        assert result["is_installed"] is False
        assert result["is_renting"] is False
        assert result["is_returning"] is True

    def test_ignores_redundant_station_metadata(self) -> None:
        """Test that redundant fields (name, address, lat, etc.) are ignored."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "true",
            "is_renting": "true",
            "is_returning": "true",
            "name": "Should be ignored",
            "address": "Should be ignored",
        }

        result = parse_station_history_csv_row(row)

        assert "name" not in result
        assert "address" not in result
        assert "lat" not in result
        assert "lon" not in result

    def test_raises_on_missing_required_field(self) -> None:
        """Test that missing required field raises ValueError."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            # Missing station_id
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "true",
            "is_renting": "true",
            "is_returning": "true",
        }

        with pytest.raises(ValueError):
            parse_station_history_csv_row(row)

    def test_raises_on_invalid_boolean(self) -> None:
        """Test that invalid boolean string raises ValueError."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "maybe",  # Invalid
            "is_renting": "true",
            "is_returning": "true",
        }

        with pytest.raises(ValueError):
            parse_station_history_csv_row(row)


class TestRequiredHeaders:
    """Test the REQUIRED_HEADERS constant."""

    def test_required_headers_contains_essential_fields(self) -> None:
        """Test that REQUIRED_HEADERS includes all essential CSV columns."""
        essential = [
            "station_id",
            "last_reported",
            "num_bikes_available",
            "num_docks_available",
            "is_installed",
            "is_renting",
            "is_returning",
        ]
        for field in essential:
            assert field in REQUIRED_HEADERS


class TestImportStationHistoryCsv:
    """Test CSV file import function."""

    def test_raises_on_missing_file(self) -> None:
        """Test that missing CSV file raises FileNotFoundError."""
        fake_path = Path("/nonexistent/file.csv")
        with pytest.raises(FileNotFoundError):
            import_station_history_csv(fake_path)
