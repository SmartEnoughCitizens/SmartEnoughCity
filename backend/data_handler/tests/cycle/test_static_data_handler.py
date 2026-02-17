"""Tests for static station information handler."""

import sys
from decimal import Decimal
from unittest.mock import Mock

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

from data_handler.cycle.static_data_handler import parse_station_information_record


class TestParseStationInformationRecord:
    """Test transforming API response to model instance."""

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

        station = parse_station_information_record(record)

        assert station.station_id == 1
        assert station.system_id == "dublin"
        assert station.name == "Mary Street"
        assert station.short_name == "001"
        assert station.address == "Mary Street, Dublin 1"
        assert station.latitude == Decimal("53.349316")
        assert station.longitude == Decimal("-6.262876")
        assert station.capacity == 30
        assert station.region_id == "dublin_central"

    def test_handles_missing_optional_fields(self) -> None:
        """Test parsing record with missing optional fields."""
        record = {
            "station_id": "2",
            "name": "Stoneybatter",
            "lat": 53.356,
            "lon": -6.289,
            "capacity": 15,
        }

        station = parse_station_information_record(record)

        assert station.station_id == 2
        assert station.name == "Stoneybatter"
        assert station.short_name is None
        assert station.address is None
        assert station.region_id is None

    def test_station_id_converted_to_int(self) -> None:
        """Test that string station_id is converted to integer."""
        record = {
            "station_id": "42",
            "name": "Test",
            "lat": 53.0,
            "lon": -6.0,
            "capacity": 10,
        }

        station = parse_station_information_record(record)
        assert station.station_id == 42
        assert isinstance(station.station_id, int)

    def test_latitude_longitude_precision(self) -> None:
        """Test that lat/lon retain decimal precision."""
        record = {
            "station_id": "1",
            "name": "Precise",
            "lat": 53.349316,
            "lon": -6.262876,
            "capacity": 10,
        }

        station = parse_station_information_record(record)
        assert station.latitude == Decimal("53.349316")
        assert station.longitude == Decimal("-6.262876")
