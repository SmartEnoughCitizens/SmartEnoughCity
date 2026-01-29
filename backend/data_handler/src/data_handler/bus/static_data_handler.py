import logging
from pathlib import Path

from sqlalchemy import delete

from data_handler.bus.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time
from data_handler.bus.models import (
    BusAgency,
    BusCalendarSchedule,
    BusRoute,
    BusStop,
    BusStopTime,
    BusTrip,
    BusTripShape,
)
from data_handler.csv_utils import read_csv_file
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


def parse_agency_row(row: dict[str, str]) -> BusAgency:
    return BusAgency(
        id=int(row["agency_id"]),
        name=row["agency_name"].strip(),
        url=row["agency_url"].strip(),
        timezone=row["agency_timezone"].strip(),
    )


def parse_calendar_row(row: dict[str, str]) -> BusCalendarSchedule:
    return BusCalendarSchedule(
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


def parse_route_row(row: dict[str, str]) -> BusRoute:
    return BusRoute(
        id=row["route_id"].strip(),
        agency_id=int(row["agency_id"]),
        short_name=row["route_short_name"].strip(),
        long_name=row["route_long_name"].strip(),
    )


def parse_stop_row(row: dict[str, str]) -> BusStop:
    description = row.get("stop_desc")
    return BusStop(
        id=row["stop_id"].strip(),
        code=int(row["stop_code"]),
        name=row["stop_name"].strip(),
        description=description.strip()
        if description and description.strip()
        else None,
        lat=float(row["stop_lat"]),
        lon=float(row["stop_lon"]),
    )


def parse_shape_row(row: dict[str, str]) -> BusTripShape:
    return BusTripShape(
        shape_id=row["shape_id"].strip(),
        pt_sequence=int(row["shape_pt_sequence"]),
        pt_lat=float(row["shape_pt_lat"]),
        pt_lon=float(row["shape_pt_lon"]),
        dist_traveled=float(row["shape_dist_traveled"]),
    )


def parse_trip_row(row: dict[str, str]) -> BusTrip:
    return BusTrip(
        id=row["trip_id"].strip(),
        route_id=row["route_id"].strip(),
        service_id=int(row["service_id"]),
        headsign=row["trip_headsign"].strip(),
        short_name=row["trip_short_name"].strip(),
        direction_id=int(row["direction_id"]),
        shape_id=row["shape_id"].strip(),
    )


def parse_stop_time_row(row: dict[str, str]) -> BusStopTime:
    headsign = row.get("stop_headsign")
    return BusStopTime(
        trip_id=row["trip_id"],
        stop_id=row["stop_id"],
        arrival_time=parse_gtfs_time(row["arrival_time"]),
        departure_time=parse_gtfs_time(row["departure_time"]),
        sequence=int(row["stop_sequence"]),
        headsign=headsign.strip() if headsign and headsign.strip() else None,
    )


def process_bus_static_data(gtfs_dir: Path) -> None:
    """
    Process bus static data from GTFS CSV files.

    This function:
    1. Deletes all existing data from relevant tables
    2. Reads data from GTFS CSV files
    3. Inserts the data into the database
    4. All operations happen within a single transaction

    Args:
        gtfs_dir: Path to the directory containing GTFS CSV files.

    Raises:
        FileNotFoundError: If any required CSV file is missing
        ValueError: If any CSV file is missing required headers or the row data is invalid
    """

    # csv file name -> (required headers, transform row function)
    csv_files = {
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

    for filename in csv_files:
        file_path = gtfs_dir / filename
        if not file_path.exists():
            msg = f"Required CSV file not found: {file_path}"
            raise FileNotFoundError(msg)

    logger.info("Processing static bus data...")

    session = SessionLocal()

    try:
        # Delete existing data in reverse dependency order
        logger.info("Deleting existing data...")
        session.execute(delete(BusStopTime))
        session.execute(delete(BusTrip))
        session.execute(delete(BusTripShape))
        session.execute(delete(BusRoute))
        session.execute(delete(BusStop))
        session.execute(delete(BusCalendarSchedule))
        session.execute(delete(BusAgency))

        for filename, (required_headers, transform_row) in csv_files.items():
            logger.info("Processing %s...", filename)
            file_path = gtfs_dir / filename
            rows = [
                transform_row(row) for row in read_csv_file(file_path, required_headers)
            ]
            session.add_all(rows)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed static bus data.")

    except Exception:
        session.rollback()
        logger.exception("Error processing static bus data")
        raise

    finally:
        session.close()
