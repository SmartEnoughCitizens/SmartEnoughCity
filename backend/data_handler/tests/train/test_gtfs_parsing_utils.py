from datetime import date, time

import pytest

from data_handler.train.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time


class TestParseGtfsDate:
    """Tests for parse_gtfs_date."""

    def test_valid_standard_date(self) -> None:
        result = parse_gtfs_date("20260126")
        assert result == date(2026, 1, 26)

    def test_valid_first_day_of_year(self) -> None:
        result = parse_gtfs_date("20250101")
        assert result == date(2025, 1, 1)

    def test_valid_last_day_of_year(self) -> None:
        result = parse_gtfs_date("20251231")
        assert result == date(2025, 12, 31)

    def test_valid_leap_year_date(self) -> None:
        result = parse_gtfs_date("20240229")
        assert result == date(2024, 2, 29)

    def test_strips_leading_whitespace(self) -> None:
        result = parse_gtfs_date("  20260126")
        assert result == date(2026, 1, 26)

    def test_strips_trailing_whitespace(self) -> None:
        result = parse_gtfs_date("20260126  ")
        assert result == date(2026, 1, 26)

    def test_strips_both_ends_whitespace(self) -> None:
        result = parse_gtfs_date("  20260126  ")
        assert result == date(2026, 1, 26)

    def test_rejects_too_short_string(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("2026012")

    def test_rejects_too_long_string(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("202601261")

    def test_rejects_empty_string(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("")

    def test_rejects_whitespace_only_string(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid date format.*Expected YYYYMMDD"):
            parse_gtfs_date("        ")

    def test_rejects_non_numeric_string(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_date("abcdefgh")

    def test_rejects_invalid_month(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_date("20261301")

    def test_rejects_invalid_day(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_date("20260132")

    def test_rejects_feb_30_non_leap_year(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_date("20250230")

    def test_rejects_feb_29_non_leap_year(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_date("20250229")


class TestParseGtfsTime:
    """Tests for parse_gtfs_time."""

    def test_valid_full_time(self) -> None:
        result = parse_gtfs_time("07:20:00")
        assert result == time(7, 20, 0)

    def test_valid_midnight(self) -> None:
        result = parse_gtfs_time("00:00:00")
        assert result == time(0, 0, 0)

    def test_valid_end_of_day(self) -> None:
        result = parse_gtfs_time("23:59:59")
        assert result == time(23, 59, 59)

    def test_valid_hh_mm_only_seconds_default_zero(self) -> None:
        result = parse_gtfs_time("12:30")
        assert result == time(12, 30, 0)

    def test_valid_single_digit_hours(self) -> None:
        result = parse_gtfs_time("9:05:00")
        assert result == time(9, 5, 0)

    def test_strips_leading_whitespace(self) -> None:
        result = parse_gtfs_time("  07:20:00")
        assert result == time(7, 20, 0)

    def test_strips_trailing_whitespace(self) -> None:
        result = parse_gtfs_time("07:20:00  ")
        assert result == time(7, 20, 0)

    def test_hours_over_24_wraps_via_modulo(self) -> None:
        result = parse_gtfs_time("25:00:00")
        assert result == time(1, 0, 0)

    def test_hours_48_wraps_to_midnight(self) -> None:
        result = parse_gtfs_time("48:00:00")
        assert result == time(0, 0, 0)

    def test_rejects_no_colon(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid time format.*Expected HH:MM:SS"):
            parse_gtfs_time("072000")

    def test_rejects_single_part(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid time format.*Expected HH:MM:SS"):
            parse_gtfs_time("07")

    def test_rejects_empty_string(self) -> None:
        with pytest.raises(ValueError, match=r"Invalid time format.*Expected HH:MM:SS"):
            parse_gtfs_time("")

    def test_rejects_non_numeric_hours(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_time("ab:30:00")

    def test_rejects_non_numeric_minutes(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_time("07:xx:00")

    def test_rejects_non_numeric_seconds(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_time("07:30:zz")

    def test_rejects_invalid_minutes(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_time("07:60:00")

    def test_rejects_invalid_seconds(self) -> None:
        with pytest.raises(ValueError):
            parse_gtfs_time("07:30:60")
