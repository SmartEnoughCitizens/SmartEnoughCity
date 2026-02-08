import logging
from pathlib import Path

from sqlalchemy import delete

from data_handler.train.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time
from data_handler.csv_utils import read_csv_file
from data_handler.db import SessionLocal
from data_handler.train.models import (
    TrainAgency,
    TrainCalendarDate,
    TrainCalendarSchedule,
    TrainRoute,
    TrainStop,
    TrainStopTime,
    TrainTrip,
    TrainTripShape,
)

logger = logging.getLogger(__name__)


# ── GTFS Row Parsers ────────────────────────────────────────────────


def parse_agency_row(row: dict[str, str]) -> TrainAgency:
    return TrainAgency(
        id=int(row["agency_id"]),
        name=row["agency_name"].strip(),
        url=row["agency_url"].strip(),
        timezone=row["agency_timezone"].strip(),
    )


def parse_calendar_row(row: dict[str, str]) -> TrainCalendarSchedule:
    return TrainCalendarSchedule(
        service_id=int(row["service_id"]),
        monday=bool(int(row["monday"])),
        tuesday=bool(int(row["tuesday"])),
        wednesday=bool(int(row["wednesday"])),
        thursday=bool(int(row["thursday"])),
        friday=bool(int(row["friday"])),
        saturday=bool(int(row["saturday"])),
        sunday=bool(int(row["sunday"])),
        start_date=parse_gtfs_date(row["start_date"]),
        end_date=parse_gtfs_date(row["end_date"]),
    )


def parse_calendar_date_row(row: dict[str, str]) -> TrainCalendarDate:
    return TrainCalendarDate(
        service_id=int(row["service_id"]),
        date=parse_gtfs_date(row["date"]),
        exception_type=int(row["exception_type"]),
    )


def parse_route_row(row: dict[str, str]) -> TrainRoute:
    return TrainRoute(
        id=row["route_id"].strip(),
        agency_id=int(row["agency_id"]),
        short_name=row["route_short_name"].strip(),
        long_name=row["route_long_name"].strip(),
        route_type=int(row.get("route_type", "2")),
        route_color=row.get("route_color", "").strip() or None,
        route_text_color=row.get("route_text_color", "").strip() or None,
    )


def parse_stop_row(row: dict[str, str]) -> TrainStop:
    description = row.get("stop_desc")
    return TrainStop(
        id=row["stop_id"].strip(),
        code=int(row["stop_code"]),
        name=row["stop_name"].strip(),
        description=description.strip()
        if description and description.strip()
        else None,
        lat=float(row["stop_lat"]),
        lon=float(row["stop_lon"]),
    )


def parse_shape_row(row: dict[str, str]) -> TrainTripShape:
    return TrainTripShape(
        shape_id=row["shape_id"].strip(),
        pt_sequence=int(row["shape_pt_sequence"]),
        pt_lat=float(row["shape_pt_lat"]),
        pt_lon=float(row["shape_pt_lon"]),
        dist_traveled=float(row["shape_dist_traveled"]),
    )


def parse_trip_row(row: dict[str, str]) -> TrainTrip:
    block_id = row.get("block_id", "").strip() or None
    return TrainTrip(
        id=row["trip_id"].strip(),
        route_id=row["route_id"].strip(),
        service_id=int(row["service_id"]),
        headsign=row["trip_headsign"].strip(),
        short_name=row["trip_short_name"].strip(),
        direction_id=int(row["direction_id"]),
        block_id=block_id,
        shape_id=row["shape_id"].strip(),
    )


def parse_stop_time_row(row: dict[str, str]) -> TrainStopTime:
    headsign = row.get("stop_headsign")
    return TrainStopTime(
        trip_id=row["trip_id"],
        stop_id=row["stop_id"],
        arrival_time=parse_gtfs_time(row["arrival_time"]),
        departure_time=parse_gtfs_time(row["departure_time"]),
        sequence=int(row["stop_sequence"]),
        headsign=headsign.strip() if headsign and headsign.strip() else None,
    )


# ── Main Processor ──────────────────────────────────────────────────


def process_train_static_data(gtfs_dir: Path) -> None:
    """
    Process train static data from GTFS CSV files.

    Expects a directory containing:
      Required: agency.txt, calendar.txt, routes.txt, shapes.txt,
                stop_times.txt, stops.txt, trips.txt
      Optional: calendar_dates.txt

    This function:
    1. Deletes all existing data from relevant tables
    2. Reads data from GTFS CSV files
    3. Inserts the data into the database
    4. All operations happen within a single transaction

    Args:
        gtfs_dir: Path to the directory containing GTFS CSV files.

    Raises:
        FileNotFoundError: If any required CSV file is missing
        ValueError: If any CSV file is missing required headers
    """

    # ── Required GTFS files ──────────────────────────────────────
    gtfs_csv_files = {
        "agency.txt": (
            ["agency_id", "agency_name", "agency_url", "agency_timezone"],
            parse_agency_row,
        ),
        "calendar.txt": (
            [
                "service_id",
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
                "start_date",
                "end_date",
            ],
            parse_calendar_row,
        ),
        "routes.txt": (
            ["route_id", "agency_id", "route_short_name", "route_long_name"],
            parse_route_row,
        ),
        "shapes.txt": (
            [
                "shape_id",
                "shape_pt_lat",
                "shape_pt_lon",
                "shape_pt_sequence",
                "shape_dist_traveled",
            ],
            parse_shape_row,
        ),
        "stop_times.txt": (
            ["trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence"],
            parse_stop_time_row,
        ),
        "stops.txt": (
            ["stop_id", "stop_code", "stop_name", "stop_lat", "stop_lon"],
            parse_stop_row,
        ),
        "trips.txt": (
            [
                "route_id",
                "service_id",
                "trip_id",
                "trip_headsign",
                "trip_short_name",
                "direction_id",
                "shape_id",
            ],
            parse_trip_row,
        ),
    }

    # ── Optional GTFS files ──────────────────────────────────────
    optional_gtfs_files = {
        "calendar_dates.txt": (
            ["service_id", "date", "exception_type"],
            parse_calendar_date_row,
        ),
    }

    for filename in gtfs_csv_files:
        file_path = gtfs_dir / filename
        if not file_path.exists():
            msg = f"Required CSV file not found: {file_path}"
            raise FileNotFoundError(msg)

    logger.info("Processing static train data from %s ...", gtfs_dir)

    session = SessionLocal()

    try:
        # Delete existing data in reverse dependency order
        logger.info("Deleting existing train data...")
        session.execute(delete(TrainStopTime))
        session.execute(delete(TrainTrip))
        session.execute(delete(TrainTripShape))
        session.execute(delete(TrainRoute))
        session.execute(delete(TrainStop))
        session.execute(delete(TrainCalendarDate))
        session.execute(delete(TrainCalendarSchedule))
        session.execute(delete(TrainAgency))

        # Process required GTFS files
        for filename, (required_headers, transform_row) in gtfs_csv_files.items():
            logger.info("Processing %s...", filename)
            file_path = gtfs_dir / filename
            rows = [
                transform_row(row)
                for row in read_csv_file(file_path, required_headers)
            ]
            session.add_all(rows)

        # Process optional GTFS files
        for filename, (required_headers, transform_row) in optional_gtfs_files.items():
            file_path = gtfs_dir / filename
            if file_path.exists():
                logger.info("Processing optional %s...", filename)
                rows = [
                    transform_row(row)
                    for row in read_csv_file(file_path, required_headers)
                ]
                session.add_all(rows)
            else:
                logger.info("Skipping optional %s (not found).", filename)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed static train data.")

    except Exception:
        session.rollback()
        logger.exception("Error processing static train data")
        raise

    finally:
        session.close()
