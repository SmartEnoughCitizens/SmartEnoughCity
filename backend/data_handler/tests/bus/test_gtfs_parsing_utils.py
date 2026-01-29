from datetime import date, time

import pytest

from data_handler.bus.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time


class TestParseGtfsDate:
    """Tests for parse_gtfs_date."""

    def test_valid_standard_date(self) -> None:
        """Parse a valid YYYYMMDD string returns correct date."""
        result = parse_gtfs_date("20260126")
        assert result == date(2026, 1, 26)

    def test_valid_first_day_of_year(self) -> None:
        """Parse first day of year."""
        result = parse_gtfs_date("20250101")
        assert result == date(2025, 1, 1)

    def test_valid_last_day_of_year(self) -> None:
        """Parse last day of year."""
        result = parse_gtfs_date("20251231")
        assert result == date(2025, 12, 31)

    def test_valid_leap_year_date(self) -> None:
        """Parse date in leap year (Feb 29)."""
        result = parse_gtfs_date("20240229")
        assert result == date(2024, 2, 29)

    def test_strips_leading_whitespace(self) -> None:
        """Leading whitespace is stripped before parsing."""
        result = parse_gtfs_date("  20260126")
        assert result == date(2026, 1, 26)

    def test_strips_trailing_whitespace(self) -> None:
        """Trailing whitespace is stripped before parsing."""
        result = parse_gtfs_date("20260126  ")
        assert result == date(2026, 1, 26)

    def test_strips_both_ends_whitespace(self) -> None:
        """Whitespace on both ends is stripped."""
        result = parse_gtfs_date("  20260126  ")
        assert result == date(2026, 1, 26)

    def test_rejects_too_short_string(self) -> None:
        """String shorter than 8 characters raises ValueError."""
        with pytest.raises(ValueError, match="Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("2026012")

    def test_rejects_too_long_string(self) -> None:
        """String longer than 8 characters raises ValueError."""
        with pytest.raises(ValueError, match="Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("202601261")

    def test_rejects_empty_string(self) -> None:
        """Empty string raises ValueError."""
        with pytest.raises(ValueError, match="Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("")

    def test_rejects_whitespace_only_string(self) -> None:
        """Whitespace-only string raises ValueError (length after strip)."""
        with pytest.raises(ValueError, match="Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("        ")

    def test_rejects_non_numeric_string(self) -> None:
        """Non-numeric string raises ValueError from date constructor."""
        with pytest.raises(ValueError):
            parse_gtfs_date("abcdefgh")

    def test_rejects_invalid_month(self) -> None:
        """Month 13 raises ValueError (invalid date)."""
        with pytest.raises(ValueError):
            parse_gtfs_date("20261301")

    def test_rejects_invalid_day(self) -> None:
        """Day 32 in January raises ValueError (invalid date)."""
        with pytest.raises(ValueError):
            parse_gtfs_date("20260132")

    def test_rejects_feb_30_non_leap_year(self) -> None:
        """Feb 30 in non-leap year raises ValueError."""
        with pytest.raises(ValueError):
            parse_gtfs_date("20250230")

    def test_rejects_feb_29_non_leap_year(self) -> None:
        """Feb 29 in non-leap year (2025) raises ValueError."""
        with pytest.raises(ValueError):
            parse_gtfs_date("20250229")


class TestParseGtfsTime:
    """Tests for parse_gtfs_time."""

    def test_valid_full_time(self) -> None:
        """Parse valid HH:MM:SS returns correct time."""
        result = parse_gtfs_time("07:20:00")
        assert result == time(7, 20, 0)

    def test_valid_midnight(self) -> None:
        """Parse midnight 00:00:00."""
        result = parse_gtfs_time("00:00:00")
        assert result == time(0, 0, 0)

    def test_valid_end_of_day(self) -> None:
        """Parse 23:59:59."""
        result = parse_gtfs_time("23:59:59")
        assert result == time(23, 59, 59)

    def test_valid_hh_mm_only_seconds_default_zero(self) -> None:
        """HH:MM format uses 0 for seconds."""
        result = parse_gtfs_time("12:30")
        assert result == time(12, 30, 0)

    def test_valid_single_digit_hours(self) -> None:
        """Single-digit hours parse correctly."""
        result = parse_gtfs_time("9:05:00")
        assert result == time(9, 5, 0)

    def test_strips_leading_whitespace(self) -> None:
        """Leading whitespace is stripped."""
        result = parse_gtfs_time("  07:20:00")
        assert result == time(7, 20, 0)

    def test_strips_trailing_whitespace(self) -> None:
        """Trailing whitespace is stripped."""
        result = parse_gtfs_time("07:20:00  ")
        assert result == time(7, 20, 0)

    def test_hours_over_24_wraps_via_modulo(self) -> None:
        """GTFS allows times past midnight; hours are taken mod 24."""
        result = parse_gtfs_time("25:00:00")
        assert result == time(1, 0, 0)

    def test_hours_48_wraps_to_midnight(self) -> None:
        """48:00:00 wraps to 00:00:00."""
        result = parse_gtfs_time("48:00:00")
        assert result == time(0, 0, 0)

    def test_rejects_no_colon(self) -> None:
        """String without colon raises ValueError."""
        with pytest.raises(ValueError, match="Invalid time format.*Expected HH:MM:SS"):
            parse_gtfs_time("072000")

    def test_rejects_single_part(self) -> None:
        """Single part (no colon) raises ValueError."""
        with pytest.raises(ValueError, match="Invalid time format.*Expected HH:MM:SS"):
            parse_gtfs_time("07")

    def test_rejects_empty_string(self) -> None:
        """Empty string has fewer than 2 parts after split."""
        with pytest.raises(ValueError, match="Invalid time format.*Expected HH:MM:SS"):
            parse_gtfs_time("")

    def test_rejects_non_numeric_hours(self) -> None:
        """Non-numeric hours raise ValueError."""
        with pytest.raises(ValueError):
            parse_gtfs_time("ab:30:00")

    def test_rejects_non_numeric_minutes(self) -> None:
        """Non-numeric minutes raise ValueError."""
        with pytest.raises(ValueError):
            parse_gtfs_time("07:xx:00")

    def test_rejects_non_numeric_seconds(self) -> None:
        """Non-numeric seconds raise ValueError."""
        with pytest.raises(ValueError):
            parse_gtfs_time("07:30:zz")

    def test_rejects_invalid_minutes(self) -> None:
        """Minutes 60 raise ValueError (invalid time)."""
        with pytest.raises(ValueError):
            parse_gtfs_time("07:60:00")

    def test_rejects_invalid_seconds(self) -> None:
        """Seconds 60 raise ValueError (invalid time)."""
        with pytest.raises(ValueError):
            parse_gtfs_time("07:30:60")
