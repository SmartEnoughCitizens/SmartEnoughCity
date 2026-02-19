# tests/car/test_car_parsing_utils.py

from datetime import datetime

import pytest

from data_handler.car.car_parsing_utils import (
    parse_kw_value,
    parse_month_year,
    parse_open_hours,
    parse_scats_time,
    parse_year,
)


class TestParseScatsTime:
    """Tests for parse_scats_time."""

    def test_valid_standard_datetime(self) -> None:
        """Parse valid YYYYMMDDHHMMSS returns correct datetime."""
        result = parse_scats_time("20250826000000")
        assert result == datetime(2025, 8, 26, 0, 0, 0)

    def test_valid_with_time(self) -> None:
        """Parse datetime with non-zero time."""
        result = parse_scats_time("20241231235959")
        assert result == datetime(2024, 12, 31, 23, 59, 59)

    def test_valid_noon(self) -> None:
        """Parse datetime at noon."""
        result = parse_scats_time("20230101120000")
        assert result == datetime(2023, 1, 1, 12, 0, 0)

    def test_rejects_too_short(self) -> None:
        """String shorter than 14 chars raises ValueError."""
        with pytest.raises(ValueError, match=r"Invalid SCATS time format"):
            parse_scats_time("2025082600000")

    def test_rejects_too_long(self) -> None:
        """String longer than 14 chars raises ValueError."""
        with pytest.raises(ValueError, match=r"Invalid SCATS time format"):
            parse_scats_time("202508260000000")

    def test_rejects_empty_string(self) -> None:
        """Empty string raises ValueError."""
        with pytest.raises(ValueError, match=r"Invalid SCATS time format"):
            parse_scats_time("")

    def test_rejects_non_numeric(self) -> None:
        """Non-numeric string raises ValueError."""
        with pytest.raises(ValueError):
            parse_scats_time("abcd0826000000")


class TestParseMonthYear:
    """Tests for parse_month_year."""

    def test_valid_year_month_format(self) -> None:
        """Parse '1996 June' format."""
        result = parse_month_year("1996 June")
        assert result.year == 1996
        assert result.month == 6
        assert result.day == 1

    def test_valid_month_year_format(self) -> None:
        """Parse 'June 1996' format."""
        result = parse_month_year("June 1996")
        assert result.year == 1996
        assert result.month == 6
        assert result.day == 1

    def test_strips_whitespace(self) -> None:
        """Whitespace is stripped."""
        result = parse_month_year("  1996 June  ")
        assert result.year == 1996
        assert result.month == 6

    def test_rejects_invalid_format(self) -> None:
        """Invalid format raises ValueError."""
        with pytest.raises(ValueError, match=r"Unable to parse"):
            parse_month_year("1996")


class TestParseYear:
    """Tests for parse_year."""

    def test_valid_four_digit_year(self) -> None:
        """Parse '2024' returns 2024."""
        result = parse_year("2024")
        assert result == 2024

    def test_year_with_prefix(self) -> None:
        """Parse 'Year 2023' returns 2023."""
        result = parse_year("Year 2023")
        assert result == 2023

    def test_strips_whitespace(self) -> None:
        """Whitespace is stripped."""
        result = parse_year("  2024  ")
        assert result == 2024

    def test_rejects_no_year(self) -> None:
        """String with no 4-digit year raises ValueError."""
        with pytest.raises(ValueError, match=r"Unable to parse year"):
            parse_year("abc")


class TestParseKwValue:
    """Tests for parse_kw_value."""

    def test_simple_number(self) -> None:
        """Parse '50' returns 50.0."""
        result = parse_kw_value("50")
        assert result == 50.0

    def test_number_with_kw(self) -> None:
        """Parse '50 kW' returns 50.0."""
        result = parse_kw_value("50 kW")
        assert result == 50.0

    def test_number_with_kw_no_space(self) -> None:
        """Parse '50kW' returns 50.0."""
        result = parse_kw_value("50kW")
        assert result == 50.0

    def test_range_returns_max(self) -> None:
        """Parse '50-150' returns 150.0 (max)."""
        result = parse_kw_value("50-150")
        assert result == 150.0

    def test_comma_separated_returns_max(self) -> None:
        """Parse '3.7, 7, 22' returns 22.0 (max)."""
        result = parse_kw_value("3.7, 7, 22")
        assert result == 22.0

    def test_multiplication_format(self) -> None:
        """Parse '2x50' returns 50.0."""
        result = parse_kw_value("2x50")
        assert result == 50.0

    def test_decimal_number(self) -> None:
        """Parse '7.4' returns 7.4."""
        result = parse_kw_value("7.4")
        assert result == 7.4

    def test_empty_string_returns_none(self) -> None:
        """Empty string returns None."""
        result = parse_kw_value("")
        assert result is None

    def test_not_available_returns_none(self) -> None:
        """'Not available' returns None."""
        result = parse_kw_value("Not available")
        assert result is None


class TestParseOpenHours:
    """Tests for parse_open_hours."""

    def test_24_7_slash(self) -> None:
        """'24/7' returns (True, '24/7')."""
        is_24_7, desc = parse_open_hours("24/7")
        assert is_24_7 is True
        assert desc == "24/7"

    def test_24_hours(self) -> None:
        """'24 hours' returns (True, '24 hours')."""
        is_24_7, desc = parse_open_hours("24 hours")
        assert is_24_7 is True
        assert desc == "24 hours"

    def test_always_open(self) -> None:
        """'Always open' returns (True, 'Always open')."""
        is_24_7, desc = parse_open_hours("Always open")
        assert is_24_7 is True
        assert desc == "Always open"

    def test_regular_hours(self) -> None:
        """Regular hours return (False, description)."""
        is_24_7, desc = parse_open_hours("Mon-Fri 9am-5pm")
        assert is_24_7 is False
        assert desc == "Mon-Fri 9am-5pm"

    def test_empty_string(self) -> None:
        """Empty string returns (False, None)."""
        is_24_7, desc = parse_open_hours("")
        assert is_24_7 is False
        assert desc is None
