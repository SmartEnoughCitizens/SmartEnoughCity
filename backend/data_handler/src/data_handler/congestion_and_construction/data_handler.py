"""Handler for fetching and storing traffic and construction data."""

import logging
from datetime import UTC, datetime

import httpx
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.orm import Session

from data_handler.congestion_and_construction.models import (
    TrafficEvent,
    TrafficEventType,
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
from data_handler.db import SessionLocal
from data_handler.settings.api_settings import get_api_settings

logger = logging.getLogger(__name__)

# Maps TrafficEventType → Hermes disruptionType
_DISRUPTION_TYPE_MAP: dict[TrafficEventType, str] = {
    TrafficEventType.ROADWORKS: "CONSTRUCTION",
    TrafficEventType.CONGESTION: "CONGESTION",
    TrafficEventType.CLOSURE_INCIDENT: "ACCIDENT",
    TrafficEventType.WARNING: "DELAY",
}

# Maps TrafficEventType → Hermes severity
_SEVERITY_MAP: dict[TrafficEventType, str] = {
    TrafficEventType.CLOSURE_INCIDENT: "HIGH",
    TrafficEventType.CONGESTION: "MEDIUM",
    TrafficEventType.ROADWORKS: "MEDIUM",
    TrafficEventType.WARNING: "LOW",
}


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


def fetch_and_store_traffic_data(
    bounding_box: BoundingBox = DUBLIN_BOUNDING_BOX,
    session: Session | None = None,
) -> tuple[int, datetime]:
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
            return 0, fetched_at

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
        return events_count, fetched_at

    finally:
        if _owns_session:
            session.close()


def push_traffic_events_to_hermes(fetched_at: datetime) -> None:
    """
    Read all traffic events written in the current fetch cycle and POST each
    one to Hermes as a DisruptionDetectionRequest.

    Only events from this fetch cycle (matching fetched_at) are forwarded to
    avoid re-sending stale rows on every run.
    """
    hermes_url = get_api_settings().hermes_url
    endpoint = f"{hermes_url}/api/v1/disruptions/detect"

    with SessionLocal() as session:
        rows = (
            session.execute(
                select(TrafficEvent).where(TrafficEvent.fetched_at == fetched_at)
            )
            .scalars()
            .all()
        )

    if not rows:
        logger.info("No traffic events to push to Hermes.")
        return

    logger.info("Pushing %d traffic events to Hermes...", len(rows))
    succeeded = 0
    failed = 0

    with httpx.Client(timeout=10) as client:
        for event in rows:
            payload = {
                "disruptionType": _DISRUPTION_TYPE_MAP[event.event_type],
                "severity": _SEVERITY_MAP[event.event_type],
                "description": event.title,
                "latitude": event.lat,
                "longitude": event.lon,
                "detectedAt": event.fetched_at.isoformat(),
                "dataSource": "PYTHON_SERVICE",
                "sourceReferenceId": event.source_id,
                "constructionProject": event.description
                if event.event_type == TrafficEventType.ROADWORKS
                else None,
                "additionalNotes": event.description,
            }
            try:
                response = client.post(endpoint, json=payload)
                response.raise_for_status()
                succeeded += 1
            except httpx.HTTPError:
                logger.warning(
                    "Failed to push traffic event %s to Hermes", event.source_id
                )
                failed += 1

    logger.info("Hermes push complete: %d succeeded, %d failed.", succeeded, failed)
