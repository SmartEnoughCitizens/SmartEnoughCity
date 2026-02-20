"""Handler for fetching and storing traffic and construction data."""

import logging
from datetime import UTC, datetime

from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from data_handler.congestion_and_construction.models import TrafficEvent
from data_handler.congestion_and_construction.parsing_utils import (
    ParsedTrafficEvent,
    parse_api_response,
)
from data_handler.congestion_and_construction.tii_api_client import (
    DUBLIN_BOUNDING_BOX,
    BoundingBox,
    TIIApiClient,
)
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


def _upsert_traffic_events(
    session: Session, events: list[ParsedTrafficEvent], fetched_at: datetime
) -> int:
    events_with_source_id = [e for e in events if e.source_id]
    events_without_source_id = [e for e in events if not e.source_id]

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


# data_handler.py

def fetch_and_store_traffic_data(
    bounding_box: BoundingBox = DUBLIN_BOUNDING_BOX,
    session: Session | None = None,  # ← add this
) -> int:
    client = TIIApiClient(bounding_box=bounding_box)

    _owns_session = session is None
    if _owns_session:
        session = SessionLocal()

    fetched_at = datetime.now(UTC)

    try:
        raw_data = client.fetch_traffic_data()

        if raw_data is None:
            if _owns_session:
                session.commit()
            return 0

        events = parse_api_response(raw_data)
        logger.info("Parsed %d traffic events from API response", len(events))

        events_count = _upsert_traffic_events(session, events, fetched_at)

        if _owns_session:
            session.commit()

    except Exception:
        if _owns_session:
            session.rollback()
        logger.exception("Error fetching/storing traffic data")
        raise

    else:
        logger.info("Successfully stored %d traffic events", events_count)
        return events_count

    finally:
        if _owns_session:
            session.close()
