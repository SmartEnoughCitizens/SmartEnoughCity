from datetime import datetime, timezone
import pytest

from data_handler.cycle.gbfs_parsing_utils import (
    parse_iso_timestamp,
    parse_csv_boolean,
    parse_csv_timestamp,
    validate_station_status_record,
    validate_csv_station_history_row,
)


class TestParseIsoTimestamp:
    """Test ISO timestamp parsing from GBFS API."""

    def test_parse_standard_iso_with_timezone(self) -> None:
        """Test parsing standard ISO format with timezone offset."""
        result = parse_iso_timestamp("2026-01-22T17:30:00+00:00")
        expected = datetime(2026, 1, 22, 17, 30, 0, tzinfo=timezone.utc)
        assert result == expected

    def test_parse_iso_with_z_suffix(self) -> None:
        """Test parsing ISO format with Z (Zulu) timezone."""
        result = parse_iso_timestamp("2026-01-22T17:30:00Z")
        expected = datetime(2026, 1, 22, 17, 30, 0, tzinfo=timezone.utc)
        assert result == expected

    def test_parse_invalid_format_raises_error(self) -> None:
        """Test that invalid format raises ValueError."""
        with pytest.raises(ValueError, match="Invalid ISO timestamp format"):
            parse_iso_timestamp("not-a-timestamp")

    def test_parse_empty_string_raises_error(self) -> None:
        """Test that empty string raises ValueError."""
        with pytest.raises(ValueError):
            parse_iso_timestamp("")


class TestParseCsvBoolean:
    """Test CSV boolean parsing."""

    @pytest.mark.parametrize("input_value,expected", [
        ("true", True),
        ("True", True),
        ("TRUE", True),
        ("1", True),
        ("yes", True),
        ("false", False),
        ("False", False),
        ("FALSE", False),
        ("0", False),
        ("no", False),
        ("  true  ", True),  # Test whitespace handling
    ])
    def test_parse_valid_booleans(self, input_value: str, expected: bool) -> None:
        """Test parsing various valid boolean representations."""
        assert parse_csv_boolean(input_value) == expected

    def test_parse_invalid_boolean_raises_error(self) -> None:
        """Test that invalid boolean raises ValueError."""
        with pytest.raises(ValueError, match="Invalid boolean value"):
            parse_csv_boolean("maybe")


class TestValidateStationStatusRecord:
    """Test API station status record validation."""

    def test_valid_record_passes(self) -> None:
        """Test that valid record passes validation."""
        record = {
            'station_id': '1',
            'num_bikes_available': 5,
            'num_docks_available': 10,
            'is_installed': True,
            'is_renting': True,
            'is_returning': True,
            'last_reported': '2026-01-22T17:30:00+00:00',
        }
        # Should not raise
        validate_station_status_record(record)

    def test_missing_required_field_raises_error(self) -> None:
        """Test that missing required field raises ValueError."""
        record = {
            'station_id': '1',
            # Missing num_bikes_available
            'num_docks_available': 10,
            'is_installed': True,
            'is_renting': True,
            'is_returning': True,
            'last_reported': '2026-01-22T17:30:00+00:00',
        }
        with pytest.raises(ValueError, match="Missing required field"):
            validate_station_status_record(record)

    def test_negative_bikes_raises_error(self) -> None:
        """Test that negative bike count raises ValueError."""
        record = {
            'station_id': '1',
            'num_bikes_available': -5,  # Invalid
            'num_docks_available': 10,
            'is_installed': True,
            'is_renting': True,
            'is_returning': True,
            'last_reported': '2026-01-22T17:30:00+00:00',
        }
        with pytest.raises(ValueError, match="Invalid num_bikes_available"):
            validate_station_status_record(record)

    def test_invalid_station_id_raises_error(self) -> None:
        """Test that invalid station_id raises ValueError."""
        record = {
            'station_id': 0,  # Invalid (must be > 0)
            'num_bikes_available': 5,
            'num_docks_available': 10,
            'is_installed': True,
            'is_renting': True,
            'is_returning': True,
            'last_reported': '2026-01-22T17:30:00+00:00',
        }
        with pytest.raises(ValueError, match="Invalid station_id"):
            validate_station_status_record(record)

    def test_non_boolean_field_raises_error(self) -> None:
        """Test that non-boolean field raises ValueError."""
        record = {
            'station_id': '1',
            'num_bikes_available': 5,
            'num_docks_available': 10,
            'is_installed': "true",  # Should be bool, not string
            'is_renting': True,
            'is_returning': True,
            'last_reported': '2026-01-22T17:30:00+00:00',
        }
        with pytest.raises(ValueError, match="must be boolean"):
            validate_station_status_record(record)


class TestValidateCsvStationHistoryRow:
    """Test CSV station history row validation."""

    def test_valid_row_passes(self) -> None:
        """Test that valid CSV row passes validation."""
        row = {
            'station_id': '1',
            'last_reported': '2026-01-22T17:30:00+00:00',
            'num_bikes_available': '5',
            'num_docks_available': '10',
            'is_installed': 'true',
            'is_renting': 'true',
            'is_returning': 'true',
        }
        # Should not raise
        validate_csv_station_history_row(row)

    def test_empty_field_raises_error(self) -> None:
        """Test that empty required field raises ValueError."""
        row = {
            'station_id': '',  # Empty
            'last_reported': '2026-01-22T17:30:00+00:00',
            'num_bikes_available': '5',
            'num_docks_available': '10',
            'is_installed': 'true',
            'is_renting': 'true',
            'is_returning': 'true',
        }
        with pytest.raises(ValueError, match="Missing or empty required field"):
            validate_csv_station_history_row(row)

    def test_invalid_numeric_field_raises_error(self) -> None:
        """Test that non-numeric value in numeric field raises ValueError."""
        row = {
            'station_id': 'abc',  # Not a number
            'last_reported': '2026-01-22T17:30:00+00:00',
            'num_bikes_available': '5',
            'num_docks_available': '10',
            'is_installed': 'true',
            'is_renting': 'true',
            'is_returning': 'true',
        }
        with pytest.raises(ValueError, match="Invalid station_id"):
            validate_csv_station_history_row(row)
