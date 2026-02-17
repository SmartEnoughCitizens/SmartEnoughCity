"""Tests for static station information handler."""

import sys
from decimal import Decimal
from unittest.mock import Mock

import pytest
from sqlalchemy.orm import DeclarativeBase

# Mock db and settings modules before importing handler (avoids PostgreSQL engine creation)
if "data_handler.db" not in sys.modules:

    class _Base(DeclarativeBase):
        pass

    _mock_db = type(sys)("data_handler.db")
    _mock_db.Base = _Base
    _mock_db.SessionLocal = Mock()
    sys.modules["data_handler.db"] = _mock_db

if "data_handler.settings.database_settings" not in sys.modules:
    _mock_settings = Mock()
    _mock_settings.get_db_settings.return_value.postgres_schema = "public"
    sys.modules["data_handler.settings.database_settings"] = _mock_settings

from data_handler.cycle.static_data_handler import (
    parse_station_information_record,
    process_station_information,
)


class TestParseStationInformationRecord:
    """Test transforming API response to dict for upsert."""

    def test_parses_all_required_fields(self) -> None:
        """Test parsing a complete station information record."""
        record = {
            "station_id": "1",
            "name": "Mary Street",
            "short_name": "001",
            "address": "Mary Street, Dublin 1",
            "lat": 53.349316,
            "lon": -6.262876,
            "capacity": 30,
            "region_id": "dublin_central",
        }

        result = parse_station_information_record(record)

        assert result["station_id"] == 1
        assert result["system_id"] == "dublin"
        assert result["name"] == "Mary Street"
        assert result["short_name"] == "001"
        assert result["address"] == "Mary Street, Dublin 1"
        assert result["latitude"] == Decimal("53.349316")
        assert result["longitude"] == Decimal("-6.262876")
        assert result["capacity"] == 30
        assert result["region_id"] == "dublin_central"

    def test_handles_missing_optional_fields(self) -> None:
        """Test parsing record with missing optional fields."""
        record = {
            "station_id": "2",
            "name": "Stoneybatter",
            "lat": 53.356,
            "lon": -6.289,
            "capacity": 15,
        }

        result = parse_station_information_record(record)

        assert result["station_id"] == 2
        assert result["name"] == "Stoneybatter"
        assert result["short_name"] is None
        assert result["address"] is None
        assert result["region_id"] is None

    def test_station_id_converted_to_int(self) -> None:
        """Test that string station_id is converted to integer."""
        record = {
            "station_id": "42",
            "name": "Test",
            "lat": 53.0,
            "lon": -6.0,
            "capacity": 10,
        }

        result = parse_station_information_record(record)
        assert result["station_id"] == 42
        assert isinstance(result["station_id"], int)

    def test_latitude_longitude_precision(self) -> None:
        """Test that lat/lon retain decimal precision."""
        record = {
            "station_id": "1",
            "name": "Precise",
            "lat": 53.349316,
            "lon": -6.262876,
            "capacity": 10,
        }

        result = parse_station_information_record(record)
        assert result["latitude"] == Decimal("53.349316")
        assert result["longitude"] == Decimal("-6.262876")


class TestProcessStationInformation:
    """Test the full process_station_information workflow."""

    def test_fetches_and_stores_stations(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Test successful fetch and store of station information."""
        mock_client = Mock()
        mock_client.fetch_station_information.return_value = [
            {
                "station_id": "1",
                "name": "Mary Street",
                "lat": 53.349,
                "lon": -6.262,
                "capacity": 30,
            },
            {
                "station_id": "2",
                "name": "Stoneybatter",
                "lat": 53.356,
                "lon": -6.289,
                "capacity": 15,
            },
        ]
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.get_jcdecaux_client",
            lambda: mock_client,
        )

        mock_settings = Mock()
        mock_settings.postgres_schema = "public"
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.get_db_settings",
            lambda: mock_settings,
        )

        mock_session = Mock()
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.SessionLocal",
            lambda: mock_session,
        )

        process_station_information()

        mock_client.fetch_station_information.assert_called_once()
        assert mock_session.execute.call_count == 2  # upsert + delete stale
        mock_session.commit.assert_called_once()
        mock_session.close.assert_called_once()

    def test_rolls_back_on_error(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Test that database errors trigger rollback."""
        mock_client = Mock()
        mock_client.fetch_station_information.return_value = [
            {
                "station_id": "1",
                "name": "Test",
                "lat": 53.0,
                "lon": -6.0,
                "capacity": 10,
            },
        ]
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.get_jcdecaux_client",
            lambda: mock_client,
        )

        mock_settings = Mock()
        mock_settings.postgres_schema = "public"
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.get_db_settings",
            lambda: mock_settings,
        )

        mock_session = Mock()
        mock_session.commit.side_effect = Exception("DB error")
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.SessionLocal",
            lambda: mock_session,
        )

        with pytest.raises(Exception, match="DB error"):
            process_station_information()

        mock_session.rollback.assert_called_once()
        mock_session.close.assert_called_once()

    def test_empty_stations_list(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Test processing when API returns no stations."""
        mock_client = Mock()
        mock_client.fetch_station_information.return_value = []
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.get_jcdecaux_client",
            lambda: mock_client,
        )

        mock_settings = Mock()
        mock_settings.postgres_schema = "public"
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.get_db_settings",
            lambda: mock_settings,
        )

        mock_session = Mock()
        monkeypatch.setattr(
            "data_handler.cycle.static_data_handler.SessionLocal",
            lambda: mock_session,
        )

        process_station_information()

        mock_session.execute.assert_not_called()
        mock_session.commit.assert_called_once()
