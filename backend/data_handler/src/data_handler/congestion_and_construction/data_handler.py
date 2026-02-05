"""Handler for fetching and storing traffic and construction data."""

import logging
from datetime import datetime, timedelta

from sqlalchemy import delete, select
from sqlalchemy.dialects.postgresql import insert as pg_insert

from data_handler.db import SessionLocal
from data_handler.congestion_and_construction.models import (
    TrafficDataFetchLog,
    TrafficEvent,
)
from data_handler.congestion_and_construction.parsing_utils import (
    ParsedTrafficEvent,
    parse_api_response,
)
from data_handler.congestion_and_construction.tii_api_client import (
    DUBLIN_BOUNDING_BOX,
    BoundingBox,
    TIIApiClient,
)

logger = logging.getLogger(__name__)


def _log_fetch_operation(
    session,
    bounding_box: BoundingBox,
    events_count: int,
    success: bool,
    error_message: str | None = None,
) -> None:
    """Log a fetch operation to the database."""
    log_entry = TrafficDataFetchLog(
        fetched_at=datetime.utcnow(),
        events_count=events_count,
        success=success,
        error_message=error_message,
        bounding_box_north=bounding_box.north,
        bounding_box_south=bounding_box.south,
        bounding_box_east=bounding_box.east,
        bounding_box_west=bounding_box.west,
    )
    session.add(log_entry)


def _upsert_traffic_events(
    session, events: list[ParsedTrafficEvent], fetched_at: datetime
) -> int:
    """
    Upsert traffic events into the database.

    Uses PostgreSQL's ON CONFLICT for events with source_id.
    Events without source_id are always inserted as new.

    Returns:
        Number of events processed
    """
    events_with_source_id = [e for e in events if e.source_id]
    events_without_source_id = [e for e in events if not e.source_id]

    # Upsert events with source_id
    if events_with_source_id:
        for event in events_with_source_id:
            stmt = pg_insert(TrafficEvent).values(
                event_type=event.event_type,
                title=event.title,
                description=event.description,
                lat=event.lat,
                lon=event.lon,
                color=event.color,
                fetched_at=fetched_at,
                source_id=event.source_id,
            )
            stmt = stmt.on_conflict_do_update(
                index_elements=["source_id"],
                set_={
                    "event_type": stmt.excluded.event_type,
                    "title": stmt.excluded.title,
                    "description": stmt.excluded.description,
                    "lat": stmt.excluded.lat,
                    "lon": stmt.excluded.lon,
                    "color": stmt.excluded.color,
                    "fetched_at": stmt.excluded.fetched_at,
                },
            )
            session.execute(stmt)

    # Insert events without source_id
    for event in events_without_source_id:
        db_event = TrafficEvent(
            event_type=event.event_type,
            title=event.title,
            description=event.description,
            lat=event.lat,
            lon=event.lon,
            color=event.color,
            fetched_at=fetched_at,
            source_id=None,
        )
        session.add(db_event)

    return len(events)


def fetch_and_store_traffic_data(
    bounding_box: BoundingBox = DUBLIN_BOUNDING_BOX,
    delete_old_events: bool = True,
    max_event_age_hours: int = 24,
) -> int:
    """
    Fetch traffic data from TII API and store in database.

    This function:
    1. Fetches current traffic data from the TII API
    2. Parses the response into structured events
    3. Optionally deletes events older than max_event_age_hours
    4. Upserts new events into the database
    5. Logs the operation

    Args:
        bounding_box: Geographic area to query
        delete_old_events: Whether to delete events older than max_event_age_hours
        max_event_age_hours: Maximum age of events to keep in hours

    Returns:
        Number of events processed

    Raises:
        requests.RequestException: If the API request fails
    """
    client = TIIApiClient(bounding_box=bounding_box)
    session = SessionLocal()
    fetched_at = datetime.utcnow()

    try:
        # Fetch data from API
        raw_data = client.fetch_traffic_data()

        if raw_data is None:
            _log_fetch_operation(
                session,
                bounding_box,
                events_count=0,
                success=False,
                error_message="No data returned from API",
            )
            session.commit()
            return 0

        # Parse the response
        events = parse_api_response(raw_data)
        logger.info("Parsed %d traffic events from API response", len(events))

        # Delete old events if requested
        if delete_old_events:
            cutoff_time = fetched_at - timedelta(hours=max_event_age_hours)
            delete_stmt = delete(TrafficEvent).where(
                TrafficEvent.fetched_at < cutoff_time
            )
            result = session.execute(delete_stmt)
            logger.info("Deleted %d old traffic events", result.rowcount)

        # Upsert new events
        events_count = _upsert_traffic_events(session, events, fetched_at)

        # Log successful operation
        _log_fetch_operation(
            session,
            bounding_box,
            events_count=events_count,
            success=True,
        )

        session.commit()
        logger.info("Successfully stored %d traffic events", events_count)
        return events_count

    except Exception as e:
        session.rollback()
        logger.exception("Error fetching/storing traffic data")

        # Try to log the failed operation
        try:
            session = SessionLocal()
            _log_fetch_operation(
                session,
                bounding_box,
                events_count=0,
                success=False,
                error_message=str(e),
            )
            session.commit()
        except Exception:
            logger.exception("Failed to log error to database")

        raise

    finally:
        session.close()


def get_current_traffic_events(
    bounding_box: BoundingBox | None = None,
    max_age_hours: int = 24,
) -> list[TrafficEvent]:
    """
    Retrieve current traffic events from the database.

    Args:
        bounding_box: Optional filter by geographic area
        max_age_hours: Maximum age of events to return

    Returns:
        List of TrafficEvent objects
    """
    session = SessionLocal()

    try:
        cutoff_time = datetime.utcnow() - timedelta(hours=max_age_hours)

        stmt = select(TrafficEvent).where(TrafficEvent.fetched_at >= cutoff_time)

        if bounding_box:
            stmt = stmt.where(
                TrafficEvent.lat >= bounding_box.south,
                TrafficEvent.lat <= bounding_box.north,
                TrafficEvent.lon >= bounding_box.west,
                TrafficEvent.lon <= bounding_box.east,
            )

        stmt = stmt.order_by(TrafficEvent.fetched_at.desc())

        result = session.execute(stmt)
        return list(result.scalars().all())

    finally:
        session.close()


def clear_all_traffic_events() -> int:
    """
    Delete all traffic events from the database.

    Returns:
        Number of events deleted
    """
    session = SessionLocal()

    try:
        result = session.execute(delete(TrafficEvent))
        session.commit()
        logger.info("Deleted all traffic events (%d rows)", result.rowcount)
        return result.rowcount

    except Exception:
        session.rollback()
        logger.exception("Error clearing traffic events")
        raise

    finally:
        session.close()
