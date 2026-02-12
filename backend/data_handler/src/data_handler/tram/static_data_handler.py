import logging
from pathlib import Path

from sqlalchemy import delete

from data_handler.csv_utils import read_csv_file
from data_handler.db import SessionLocal
from data_handler.tram.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time
from data_handler.tram.models import (
    TramAgency,
    TramCalendarDate,
    TramCalendarSchedule,
    TramHourlyDistribution,
    TramPassengerJourney,
    TramPassengerNumber,
    TramRoute,
    TramStop,
    TramStopTime,
    TramTrip,
    TramTripShape,
    TramWeeklyFlow,
)

logger = logging.getLogger(__name__)


# ── Helpers ─────────────────────────────────────────────────────────


def _safe_int(val: str | None) -> int | None:
    if val is None or val.strip() == "":
        return None
    try:
        return int(val.strip())
    except ValueError:
        return None


def _safe_float(val: str | None) -> float | None:
    if val is None or val.strip() == "":
        return None
    try:
        return float(val.strip())
    except ValueError:
        return None


# ── GTFS Row Parsers ────────────────────────────────────────────────


def parse_agency_row(row: dict[str, str]) -> TramAgency:
    return TramAgency(
        id=int(row["agency_id"]),
        name=row["agency_name"].strip(),
        url=row["agency_url"].strip(),
        timezone=row["agency_timezone"].strip(),
    )


def parse_calendar_row(row: dict[str, str]) -> TramCalendarSchedule:
    return TramCalendarSchedule(
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


def parse_calendar_date_row(row: dict[str, str]) -> TramCalendarDate:
    return TramCalendarDate(
        service_id=int(row["service_id"]),
        date=parse_gtfs_date(row["date"]),
        exception_type=int(row["exception_type"]),
    )


def parse_route_row(row: dict[str, str]) -> TramRoute:
    return TramRoute(
        id=row["route_id"].strip(),
        agency_id=int(row["agency_id"]),
        short_name=row["route_short_name"].strip(),
        long_name=row["route_long_name"].strip(),
        route_type=int(row.get("route_type", "0")),
        route_color=row.get("route_color", "").strip() or None,
        route_text_color=row.get("route_text_color", "").strip() or None,
    )


def parse_stop_row(row: dict[str, str]) -> TramStop:
    description = row.get("stop_desc")
    return TramStop(
        id=row["stop_id"].strip(),
        code=int(row["stop_code"]),
        name=row["stop_name"].strip(),
        description=description.strip()
        if description and description.strip()
        else None,
        lat=float(row["stop_lat"]),
        lon=float(row["stop_lon"]),
    )


def parse_shape_row(row: dict[str, str]) -> TramTripShape:
    return TramTripShape(
        shape_id=row["shape_id"].strip(),
        pt_sequence=int(row["shape_pt_sequence"]),
        pt_lat=float(row["shape_pt_lat"]),
        pt_lon=float(row["shape_pt_lon"]),
        dist_traveled=float(row["shape_dist_traveled"]),
    )


def parse_trip_row(row: dict[str, str]) -> TramTrip:
    block_id = row.get("block_id", "").strip() or None
    return TramTrip(
        id=row["trip_id"].strip(),
        route_id=row["route_id"].strip(),
        service_id=int(row["service_id"]),
        headsign=row["trip_headsign"].strip(),
        short_name=row["trip_short_name"].strip(),
        direction_id=int(row["direction_id"]),
        block_id=block_id,
        shape_id=row["shape_id"].strip(),
    )


def parse_stop_time_row(row: dict[str, str]) -> TramStopTime:
    headsign = row.get("stop_headsign")
    return TramStopTime(
        trip_id=row["trip_id"],
        stop_id=row["stop_id"],
        arrival_time=parse_gtfs_time(row["arrival_time"]),
        departure_time=parse_gtfs_time(row["departure_time"]),
        sequence=int(row["stop_sequence"]),
        headsign=headsign.strip() if headsign and headsign.strip() else None,
    )


# ── CSO CSV Row Parsers ─────────────────────────────────────────────
# These CSO files are downloaded from data.gov.ie and placed in the
# same data directory. Column headers match the CSO PxStat CSV export
# format exactly.


def parse_passenger_journey_row(row: dict[str, str]) -> TramPassengerJourney:
    """TII03 - Passenger Journeys by Luas."""
    return TramPassengerJourney(
        statistic=row["STATISTIC"].strip(),
        statistic_label=row["Statistic Label"].strip(),
        week_code=row["TLIST(W1)"].strip(),
        week_label=row["Week"].strip(),
        line_code=row["C03132V03784"].strip(),
        line_label=row["Luas Line"].strip(),
        unit=row["UNIT"].strip(),
        value=_safe_int(row.get("VALUE")),
    )


def parse_passenger_number_row(row: dict[str, str]) -> TramPassengerNumber:
    """TOA11 - Luas Passenger Numbers."""
    return TramPassengerNumber(
        statistic=row["STATISTIC"].strip(),
        statistic_label=row["Statistic Label"].strip(),
        year=row["TLIST(A1)"].strip(),
        month_code=row["C01885V02316"].strip(),
        month_label=row["Month"].strip(),
        unit=row["UNIT"].strip(),
        value=_safe_int(row.get("VALUE")),
    )


def parse_hourly_distribution_row(row: dict[str, str]) -> TramHourlyDistribution:
    """TOA09 - Percentage of daily Luas passengers by hour and by line."""
    return TramHourlyDistribution(
        statistic=row["STATISTIC"].strip(),
        statistic_label=row["Statistic Label"].strip(),
        year=row["TLIST(A1)"].strip(),
        line_code=row["C03132V03784"].strip(),
        line_label=row["Luas Line"].strip(),
        time_code=row["C03841V04591"].strip(),
        time_label=row["Time of day"].strip(),
        unit=row["UNIT"].strip(),
        value=_safe_float(row.get("VALUE")),
    )


def parse_weekly_flow_row(row: dict[str, str]) -> TramWeeklyFlow:
    """TOA02 - Average weekly flow of Luas passengers."""
    return TramWeeklyFlow(
        statistic=row["STATISTIC"].strip(),
        statistic_label=row["Statistic Label"].strip(),
        year=row["TLIST(A1)"].strip(),
        day_code=row["C02639V03196"].strip(),
        day_label=row["Days of Week"].strip(),
        unit=row["UNIT"].strip(),
        value=_safe_int(row.get("VALUE")),
    )


# ── File Definitions ────────────────────────────────────────────────


def _get_gtfs_csv_files() -> dict:
    """Return required GTFS file definitions (filename → headers, parser)."""
    return {
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


def _get_optional_gtfs_files() -> dict:
    """Return optional GTFS file definitions."""
    return {
        "calendar_dates.txt": (
            ["service_id", "date", "exception_type"],
            parse_calendar_date_row,
        ),
    }


def _get_cso_csv_files() -> dict:
    """Return CSO dataset file definitions (downloaded from data.gov.ie)."""
    return {
        "TII03.csv": (
            ["STATISTIC", "Statistic Label", "TLIST(W1)", "Week",
             "C03132V03784", "Luas Line", "UNIT", "VALUE"],
            parse_passenger_journey_row,
        ),
        "TOA11.csv": (
            ["STATISTIC", "Statistic Label", "TLIST(A1)", "Year",
             "C01885V02316", "Month", "UNIT", "VALUE"],
            parse_passenger_number_row,
        ),
        "TOA09.csv": (
            ["STATISTIC", "Statistic Label", "TLIST(A1)", "Year",
             "C03132V03784", "Luas Line", "C03841V04591", "Time of day",
             "UNIT", "VALUE"],
            parse_hourly_distribution_row,
        ),
        "TOA02.csv": (
            ["STATISTIC", "Statistic Label", "TLIST(A1)", "Year",
             "C02639V03196", "Days of Week", "UNIT", "VALUE"],
            parse_weekly_flow_row,
        ),
    }


def _delete_all_tram_data(session: object) -> None:
    """Delete all existing tram data in reverse dependency order."""
    logger.info("Deleting existing tram data...")
    # CSO tables (no FK dependencies)
    session.execute(delete(TramWeeklyFlow))
    session.execute(delete(TramHourlyDistribution))
    session.execute(delete(TramPassengerNumber))
    session.execute(delete(TramPassengerJourney))
    # GTFS tables (reverse FK order)
    session.execute(delete(TramStopTime))
    session.execute(delete(TramTrip))
    session.execute(delete(TramTripShape))
    session.execute(delete(TramRoute))
    session.execute(delete(TramStop))
    session.execute(delete(TramCalendarDate))
    session.execute(delete(TramCalendarSchedule))
    session.execute(delete(TramAgency))


# ── Main Processor ──────────────────────────────────────────────────


def process_tram_static_data(data_dir: Path) -> None:
    """
    Process tram data from GTFS CSV files and CSO dataset CSV files.

    Expects a directory containing:
      GTFS files:  agency.txt, calendar.txt, routes.txt, shapes.txt,
                   stop_times.txt, stops.txt, trips.txt
      Optional GTFS: calendar_dates.txt
      CSO files:   TII03.csv, TOA11.csv, TOA09.csv, TOA02.csv

    This function:
    1. Deletes all existing data from relevant tables
    2. Reads data from CSV files
    3. Inserts the data into the database
    4. All operations happen within a single transaction

    Args:
        data_dir: Path to the directory containing the data files.

    Raises:
        FileNotFoundError: If any required GTFS CSV file is missing
        ValueError: If any CSV file is missing required headers
    """
    gtfs_csv_files = _get_gtfs_csv_files()
    optional_gtfs_files = _get_optional_gtfs_files()
    cso_csv_files = _get_cso_csv_files()

    for filename in gtfs_csv_files:
        file_path = data_dir / filename
        if not file_path.exists():
            msg = f"Required CSV file not found: {file_path}"
            raise FileNotFoundError(msg)

    logger.info("Processing static tram data from %s ...", data_dir)

    session = SessionLocal()

    try:
        _delete_all_tram_data(session)

        # Process required GTFS files
        for filename, (required_headers, transform_row) in gtfs_csv_files.items():
            logger.info("Processing %s...", filename)
            file_path = data_dir / filename
            rows = [
                transform_row(row)
                for row in read_csv_file(file_path, required_headers)
            ]
            session.add_all(rows)

        # Process optional GTFS files
        for filename, (required_headers, transform_row) in optional_gtfs_files.items():
            file_path = data_dir / filename
            if file_path.exists():
                logger.info("Processing optional %s...", filename)
                rows = [
                    transform_row(row)
                    for row in read_csv_file(file_path, required_headers)
                ]
                session.add_all(rows)
            else:
                logger.info("Skipping optional %s (not found).", filename)

        # Process optional CSO dataset files
        for filename, (required_headers, transform_row) in cso_csv_files.items():
            file_path = data_dir / filename
            if file_path.exists():
                logger.info("Processing CSO dataset %s...", filename)
                rows = [
                    transform_row(row)
                    for row in read_csv_file(file_path, required_headers)
                ]
                session.add_all(rows)
                logger.info("Loaded %d rows from %s.", len(rows), filename)
            else:
                logger.info(
                    "Skipping optional CSO dataset %s (not found). "
                    "Download from data.gov.ie and place in %s to enable.",
                    filename,
                    data_dir,
                )

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed static tram data.")

    except Exception:
        session.rollback()
        logger.exception("Error processing static tram data")
        raise

    finally:
        session.close()
