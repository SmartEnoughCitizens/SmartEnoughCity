"""Handler for fetching and storing events data."""

import logging
from datetime import UTC, datetime, timedelta
from typing import Any

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from data_handler.db import SessionLocal
from data_handler.events.models import Event, Venue
from data_handler.events.parsing_utils import (
    ParsedEvent,
    ParsedVenue,
    determine_high_impact,
    parse_ticketmaster_response,
    parse_ticketmaster_venues_response,
)
from data_handler.events.ticketmaster_client import get_ticketmaster_client

logger = logging.getLogger(__name__)

# Default event durations (hours) by event type, used when the API
# does not provide an end time.
_DEFAULT_DURATION_HOURS: dict[str, float] = {
    "Music": 4,
    "Sports": 3,
    "Miscellaneous": 2,
    "Arts & Theatre": 2.5,
    "Film": 2,
}
_FALLBACK_DURATION_HOURS: float = 3


def _estimate_end_time(event: ParsedEvent) -> datetime:
    """Estimate an end time from the event type's default duration."""
    hours = _DEFAULT_DURATION_HOURS.get(event.event_type, _FALLBACK_DURATION_HOURS)
    return event.start_time + timedelta(hours=hours)


def _upsert_venues(
    session: Session,
    venues: list[ParsedVenue],
) -> dict[str, tuple[int, str | None]]:
    """
    Upsert venues into the database.

    ON CONFLICT on ticketmaster_id:
    - Updates name, address, city, latitude, longitude.
    - Does NOT update capacity -- preserves manual DB edits.
    - venue_size_tag is a DB-computed column and is never set directly.

    Returns:
        Mapping of ticketmaster_id -> (db_id, venue_size_tag) for FK and
        is_high_impact resolution in _upsert_events.
    """
    if not venues:
        return {}

    for venue in venues:
        stmt = pg_insert(Venue).values(
            ticketmaster_id=venue.ticketmaster_id,
            name=venue.name,
            address=venue.address,
            city=venue.city,
            latitude=venue.latitude,
            longitude=venue.longitude,
        )
        stmt = stmt.on_conflict_do_update(
            index_elements=["ticketmaster_id"],
            set_={
                "name": stmt.excluded.name,
                "address": stmt.excluded.address,
                "city": stmt.excluded.city,
                "latitude": stmt.excluded.latitude,
                "longitude": stmt.excluded.longitude,
                # capacity intentionally omitted -- preserves manual DB edits
            },
        )
        session.execute(stmt)

    session.flush()

    tm_ids = [v.ticketmaster_id for v in venues]
    rows = session.execute(
        select(Venue.ticketmaster_id, Venue.id, Venue.venue_size_tag).where(
            Venue.ticketmaster_id.in_(tm_ids)
        )
    ).all()

    return {row.ticketmaster_id: (row.id, row.venue_size_tag) for row in rows}


def _extract_venues_from_events(raw_data: dict[str, Any]) -> list[ParsedVenue]:
    """
    Extract ParsedVenue stubs from an events API response.

    Walks each event's embedded venue data so the venues table is populated
    during normal dynamic runs without requiring a separate /venues.json call.
    Deduplicates by ticketmaster_id.
    """
    seen: set[str] = set()
    venues: list[ParsedVenue] = []

    embedded = raw_data.get("_embedded", {})
    for raw_event in embedded.get("events", []):
        event_venues = raw_event.get("_embedded", {}).get("venues", [])
        if not event_venues:
            continue

        v = event_venues[0]
        tm_id = v.get("id", "")
        if not tm_id or tm_id in seen:
            continue

        location = v.get("location", {})
        try:
            lat = float(location["latitude"])
            lon = float(location["longitude"])
        except (KeyError, TypeError, ValueError):
            continue

        address_block = v.get("address", {})
        city_block = v.get("city", {})
        venues.append(
            ParsedVenue(
                ticketmaster_id=tm_id,
                name=v.get("name", ""),
                address=address_block.get("line1") if address_block else None,
                city=city_block.get("name") if city_block else None,
                latitude=lat,
                longitude=lon,
            )
        )
        seen.add(tm_id)

    return venues


def _upsert_events(
    session: Session,
    events: list[ParsedEvent],
    fetched_at: datetime,
    venue_id_map: dict[str, tuple[int, str | None]] | None = None,
) -> int:
    """
    Upsert events into the database.

    Uses PostgreSQL's ON CONFLICT on (source, source_id) to update
    existing events or insert new ones.

    Args:
        venue_id_map: Optional mapping of ticketmaster_id -> (db_id, venue_size_tag).
                      When provided, sets venue_id FK and computes is_high_impact
                      from the venue's tag.

    Returns:
        Number of events processed.
    """
    if not events:
        return 0

    if venue_id_map is None:
        venue_id_map = {}

    for event in events:
        if event.end_time is None:
            event.end_time = _estimate_end_time(event)

        venue_entry = venue_id_map.get(event.venue_ticketmaster_id)
        venue_db_id, venue_tag = venue_entry or (None, None)
        is_high_impact = determine_high_impact(venue_tag, event.estimated_attendance)

        stmt = pg_insert(Event).values(
            source=event.source,
            source_id=event.source_id,
            event_name=event.event_name,
            event_type=event.event_type,
            venue_name=event.venue_name,
            venue_id=venue_db_id,
            latitude=event.latitude,
            longitude=event.longitude,
            event_date=event.event_date,
            start_time=event.start_time,
            end_time=event.end_time,
            is_high_impact=is_high_impact,
            estimated_attendance=event.estimated_attendance,
            fetched_at=fetched_at,
        )
        stmt = stmt.on_conflict_do_update(
            constraint="uq_event_source",
            set_={
                "event_name": stmt.excluded.event_name,
                "event_type": stmt.excluded.event_type,
                "venue_name": stmt.excluded.venue_name,
                "venue_id": stmt.excluded.venue_id,
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


def fetch_and_store_venues(session: Session | None = None) -> int:
    """
    Fetch all Dublin venues from Ticketmaster and seed the venues table.

    Called during static data processing. Fetches venues from the
    Ticketmaster /venues.json endpoint and upserts them, preserving
    any manually-set capacity values.

    Args:
        session: Optional SQLAlchemy session (for testing). If None, creates one.

    Returns:
        Number of venues processed.
    """
    own_session = session is None
    if own_session:
        session = SessionLocal()

    try:
        tm_client = get_ticketmaster_client()
        raw = tm_client.fetch_venues()
        if raw is None:
            logger.warning("No venue data returned from Ticketmaster")
            return 0

        venues = parse_ticketmaster_venues_response(raw)
        logger.info("Parsed %d venues from Ticketmaster", len(venues))

        _upsert_venues(session, venues)

        if own_session:
            session.commit()

        logger.info("Successfully stored %d venues", len(venues))
        return len(venues)

    except Exception:
        if own_session:
            session.rollback()
        logger.exception("Error storing venues in database")
        raise
    finally:
        if own_session:
            session.close()


def fetch_and_store_events(session: Session | None = None) -> int:
    """
    Fetch events from Ticketmaster and store in database.

    This function:
    1. Fetches events from Ticketmaster
    2. Parses the responses into structured events
    3. Upserts venues extracted from event payloads (for FK + impact resolution)
    4. Upserts all events with venue_id FKs and is_high_impact set from venue tag

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
    embedded_venues: list[ParsedVenue] = []

    try:
        tm_client = get_ticketmaster_client()
        tm_raw = tm_client.fetch_events()
        if tm_raw is not None:
            tm_events = parse_ticketmaster_response(tm_raw)
            all_events.extend(tm_events)
            embedded_venues.extend(_extract_venues_from_events(tm_raw))
            logger.info("Parsed %d events from Ticketmaster", len(tm_events))
    except Exception:
        logger.exception("Failed to fetch events from Ticketmaster")

    try:
        venue_id_map = _upsert_venues(session, embedded_venues)
        events_count = _upsert_events(session, all_events, fetched_at, venue_id_map)
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
