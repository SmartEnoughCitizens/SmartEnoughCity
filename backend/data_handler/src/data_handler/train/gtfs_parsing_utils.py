"""GTFS parsing utilities for train data.

These utilities are shared across all transport handlers.
The canonical implementation lives in data_handler.bus.gtfs_parsing_utils.
"""

from data_handler.bus.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time

__all__ = ["parse_gtfs_date", "parse_gtfs_time"]
