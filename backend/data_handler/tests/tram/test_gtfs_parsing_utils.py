"""Tests for tram GTFS parsing utilities.

These utilities are shared across all transport handlers. The canonical
implementation and its tests live in data_handler.bus.gtfs_parsing_utils
and tests.bus.test_gtfs_parsing_utils respectively.

This module re-imports and runs the same test suite against the tram
re-export to verify the wiring is correct.
"""

from data_handler.tram.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time  # noqa: F401
from tests.bus.test_gtfs_parsing_utils import TestParseGtfsDate as _BaseDateTests
from tests.bus.test_gtfs_parsing_utils import TestParseGtfsTime as _BaseTimeTests


class TestTramParseGtfsDate(_BaseDateTests):
    """Verify tram re-export of parse_gtfs_date works correctly."""


class TestTramParseGtfsTime(_BaseTimeTests):
    """Verify tram re-export of parse_gtfs_time works correctly."""