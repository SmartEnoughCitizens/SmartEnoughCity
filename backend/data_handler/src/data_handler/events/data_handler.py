"""Handler for fetching and storing events data."""

import logging
from datetime import UTC, datetime,timedelta

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from data_handler.db import SessionLocal
from data_handler.events.models import Event
from data_handler.events.parsing_utils import ParsedEvent, parse_ticketmaster_response
from data_handler.events.ticketmaster_client import get_ticketmaster_client

logger = logging.getLogger(__name__)


def _upsert_events(
    session: Session, events: list[ParsedEvent], fetched_at: datetime
) -> int:
    """
    Upsert events into the database.

    Uses PostgreSQL's ON CONFLICT on (source, source_id) to update
    existing events or insert new ones.

    Returns:
        Number of events processed.
    """
    if not events:
        return 0

    for event in events:
        if event.end_time is None:
            if event.event_type == "Music":
                event.end_time = event.start_time + timedelta(hours=4)
            elif event.event_type == "Sports":
                event.end_time = event.start_time + timedelta(hours=3)
            elif event.event_type == "Miscellaneous":
                event.end_time = event.start_time + timedelta(hours=2)
            elif event.event_type == "Arts & Theatre":
                event.end_time = event.start_time + timedelta(hours=2.5)
            elif event.event_type == "Film":
                event.end_time = event.start_time + timedelta(hours=2)       
            else:
                event.end_time = event.start_time + timedelta(hours=3)

        stmt = pg_insert(Event).values(
            source=event.source,
            source_id=event.source_id,
            event_name=event.event_name,
            event_type=event.event_type,
            venue_name=event.venue_name,
            latitude=event.latitude,
            longitude=event.longitude,
            event_date=event.event_date,
            start_time=event.start_time,
            end_time=event.end_time,
            is_high_impact=event.is_high_impact,
            estimated_attendance=event.estimated_attendance,
            fetched_at=fetched_at,
        )
        stmt = stmt.on_conflict_do_update(
            constraint="uq_event_source",
            set_={
                "event_name": stmt.excluded.event_name,
                "event_type": stmt.excluded.event_type,
                "venue_name": stmt.excluded.venue_name,
                "latitude": stmt.excluded.latitude,
                "longitude": stmt.excluded.longitude,
                "event_date": stmt.excluded.event_date,
                "start_time": stmt.excluded.start_time,
                "end_time": stmt.excluded.end_time,
                "is_high_impact": stmt.excluded.is_high_impact,
                "estimated_attendance": stmt.excluded.estimated_attendance,
                "fetched_at": stmt.excluded.fetched_at,
            },
        )
        session.execute(stmt)

    return len(events)


def fetch_and_store_events(session: Session | None = None) -> int:
    """
    Fetch events from Ticketmaster and store in database.

    This function:
    1. Fetches events from Ticketmaster
    2. Parses the responses into structured events
    3. Upserts all events into the database

    Args:
        session: Optional SQLAlchemy session (for testing). If None, creates one.

    Returns:
        Number of events processed.
    """
    own_session = session is None
    if own_session:
        session = SessionLocal()

    fetched_at = datetime.now(tz=UTC)
    all_events: list[ParsedEvent] = []

    # Fetch from Ticketmaster
    try:
        tm_client = get_ticketmaster_client()
        tm_raw = tm_client.fetch_events()
        if tm_raw is not None:
            tm_events = parse_ticketmaster_response(tm_raw)
            all_events.extend(tm_events)
            logger.info("Parsed %d events from Ticketmaster", len(tm_events))
    except Exception:
        logger.exception("Failed to fetch events from Ticketmaster")

    try:
        events_count = _upsert_events(session, all_events, fetched_at)
        if own_session:
            session.commit()
    except Exception:
        if own_session:
            session.rollback()
        logger.exception("Error storing events in database")
        raise
    else:
        logger.info("Successfully stored %d events", events_count)
        return events_count
    finally:
        if own_session:
            session.close()
