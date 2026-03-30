import logging
from pathlib import Path

from sqlalchemy import delete
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import DeclarativeBase, Session

from data_handler.bus.models import (
    BusAgency,
    BusCalendarSchedule,
    BusRoute,
    BusStop,
    BusStopTime,
    BusTrip,
    BusTripShape,
)
from data_handler.common.gtfs_parsing_utils import parse_gtfs_date, parse_gtfs_time
from data_handler.csv_utils import read_csv_file
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)

_CHUNKED_FILES = {"shapes.txt", "stop_times.txt", "trips.txt", "stops.txt"}


def parse_agency_row(row: dict[str, str]) -> dict[str, object]:
    return {
        "id": int(row["agency_id"]),
        "name": row["agency_name"].strip(),
        "url": row["agency_url"].strip(),
        "timezone": row["agency_timezone"].strip(),
    }


def parse_calendar_row(row: dict[str, str]) -> dict[str, object]:
    return {
        "service_id": int(row["service_id"]),
        "monday": bool(int(row["monday"])),
        "tuesday": bool(int(row["tuesday"])),
        "wednesday": bool(int(row["wednesday"])),
        "thursday": bool(int(row["thursday"])),
        "friday": bool(int(row["friday"])),
        "saturday": bool(int(row["saturday"])),
        "sunday": bool(int(row["sunday"])),
        "start_date": parse_gtfs_date(row["start_date"]),
        "end_date": parse_gtfs_date(row["end_date"]),
    }


def parse_route_row(row: dict[str, str]) -> dict[str, object]:
    return {
        "id": row["route_id"].strip(),
        "agency_id": int(row["agency_id"]),
        "short_name": row["route_short_name"].strip(),
        "long_name": row["route_long_name"].strip(),
    }


def parse_stop_row(row: dict[str, str]) -> dict[str, object]:
    description = row.get("stop_desc")
    return {
        "id": row["stop_id"].strip(),
        "code": int(row["stop_code"]),
        "name": row["stop_name"].strip(),
        "description": description.strip()
        if description and description.strip()
        else None,
        "lat": float(row["stop_lat"]),
        "lon": float(row["stop_lon"]),
    }


def parse_shape_row(row: dict[str, str]) -> dict[str, object]:
    return {
        "shape_id": row["shape_id"].strip(),
        "pt_sequence": int(row["shape_pt_sequence"]),
        "pt_lat": float(row["shape_pt_lat"]),
        "pt_lon": float(row["shape_pt_lon"]),
        "dist_traveled": float(row["shape_dist_traveled"]),
    }


def parse_trip_row(row: dict[str, str]) -> dict[str, object]:
    return {
        "id": row["trip_id"].strip(),
        "route_id": row["route_id"].strip(),
        "service_id": int(row["service_id"]),
        "headsign": row["trip_headsign"].strip(),
        "short_name": row["trip_short_name"].strip(),
        "direction_id": int(row["direction_id"]),
        "shape_id": row["shape_id"].strip(),
    }


def parse_stop_time_row(row: dict[str, str]) -> dict[str, object]:
    headsign = row.get("stop_headsign")
    return {
        "trip_id": row["trip_id"],
        "stop_id": row["stop_id"],
        "arrival_time": parse_gtfs_time(row["arrival_time"]),
        "departure_time": parse_gtfs_time(row["departure_time"]),
        "sequence": int(row["stop_sequence"]),
        "headsign": headsign.strip() if headsign and headsign.strip() else None,
    }


def _execute_batch(
    session: Session,
    model: type[DeclarativeBase],
    batch: list[dict[str, object]],
    conflict_target: list[str] | str | None,
    update_cols: list[str],
) -> None:
    if not batch:
        return
    chunk_size = max(1, 65535 // len(batch[0]))
    for i in range(0, len(batch), chunk_size):
        chunk = batch[i : i + chunk_size]
        stmt = pg_insert(model).values(chunk)
        if conflict_target is not None:
            set_ = {col: getattr(stmt.excluded, col) for col in update_cols}
            if isinstance(conflict_target, str):
                stmt = stmt.on_conflict_do_update(constraint=conflict_target, set_=set_)
            else:
                stmt = stmt.on_conflict_do_update(
                    index_elements=conflict_target, set_=set_
                )
        session.execute(stmt)


def process_bus_static_data(gtfs_dir: Path) -> None:
    """
    Process bus static data from GTFS CSV files.

    Upserts agency, calendar, route, stop, shape, and trip data so that existing
    live-data FK references (vehicles, trip updates, ridership) are preserved.
    Stop times are deleted and re-inserted fresh (no live tables reference them).

    Args:
        gtfs_dir: Path to the directory containing GTFS CSV files.

    Raises:
        FileNotFoundError: If any required CSV file is missing
        ValueError: If any CSV file is missing required headers or the row data is invalid
    """
    # (required_headers, parse_row_fn, model, conflict_target, update_cols)
    # conflict_target=None → plain insert (stop_times pre-deleted at start)
    # Order matters: stop_times must be last (FKs to trips and stops)
    csv_files = {
        "agency.txt": (
            ["agency_id", "agency_name", "agency_url", "agency_timezone"],
            parse_agency_row,
            BusAgency,
            ["id"],
            ["name", "url", "timezone"],
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
            BusCalendarSchedule,
            "uq_service_date_range",
            [
                "monday",
                "tuesday",
                "wednesday",
                "thursday",
                "friday",
                "saturday",
                "sunday",
            ],
        ),
        "routes.txt": (
            ["route_id", "agency_id", "route_short_name", "route_long_name"],
            parse_route_row,
            BusRoute,
            ["id"],
            ["agency_id", "short_name", "long_name"],
        ),
        "stops.txt": (
            ["stop_id", "stop_code", "stop_name", "stop_lat", "stop_lon"],
            parse_stop_row,
            BusStop,
            ["id"],
            ["code", "name", "description", "lat", "lon"],
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
            BusTrip,
            ["id"],
            [
                "route_id",
                "service_id",
                "headsign",
                "short_name",
                "direction_id",
                "shape_id",
            ],
        ),
        "stop_times.txt": (
            ["trip_id", "arrival_time", "departure_time", "stop_id", "stop_sequence"],
            parse_stop_time_row,
            BusStopTime,
            None,
            [],
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
            BusTripShape,
            "uq_shape_sequence",
            ["pt_lat", "pt_lon", "dist_traveled"],
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
        # Delete only stop_times — upsert handles all other tables,
        # preserving live-data FK references to trips and stops.
        logger.info("Deleting existing stop time data...")
        session.execute(delete(BusStopTime))
        session.commit()

        for filename, (
            required_headers,
            parse_row,
            model,
            conflict_target,
            update_cols,
        ) in csv_files.items():
            logger.info("Processing %s...", filename)
            file_path = gtfs_dir / filename

            if filename in _CHUNKED_FILES:
                chunk: list[dict[str, object]] = []
                chunk_size: int | None = None
                total = 0
                for row in read_csv_file(file_path, required_headers):
                    parsed = parse_row(row)
                    if chunk_size is None:
                        chunk_size = max(1, 65535 // len(parsed))
                    chunk.append(parsed)
                    if len(chunk) >= chunk_size:
                        _execute_batch(
                            session, model, chunk, conflict_target, update_cols
                        )
                        total += len(chunk)
                        chunk = []
                        logger.info("  Flushed %d rows...", total)
                if chunk:
                    _execute_batch(session, model, chunk, conflict_target, update_cols)
                    total += len(chunk)
                logger.info("  Total: %d rows from %s", total, filename)
            else:
                rows = [
                    parse_row(row) for row in read_csv_file(file_path, required_headers)
                ]
                _execute_batch(session, model, rows, conflict_target, update_cols)

        session.commit()
        logger.info("Successfully processed static bus data.")

    except Exception:
        session.rollback()
        logger.exception("Error processing static bus data")
        raise

    finally:
        session.close()
