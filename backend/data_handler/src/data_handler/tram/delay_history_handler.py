"""Calculate tram delays from DB data and persist to tram_delay_history."""

import logging
from datetime import datetime, time, timezone

from sqlalchemy import select, text
from sqlalchemy.orm import Session

from data_handler.db import SessionLocal
from data_handler.tram.models import (
    TramDelayHistory,
    TramLuasForecast,
    TramLuasStop,
    TramStop,
    TramStopTime,
)

logger = logging.getLogger(__name__)


def _get_dublin_time(session: Session) -> time:
    """Get current Dublin time from the database."""
    row = session.execute(text("SELECT NOW() AT TIME ZONE 'Europe/Dublin'")).scalar()
    if row is None:
        return datetime.now(tz=timezone.utc).time()
    return row.time() if isinstance(row, datetime) else row


def _build_name_to_gtfs_ids(session: Session) -> dict[str, list[str]]:
    """Build mapping of lowercase stop name → list of GTFS stop IDs."""
    gtfs_stops = session.execute(select(TramStop.id, TramStop.name)).all()
    mapping: dict[str, list[str]] = {}
    for gtfs_id, gtfs_name in gtfs_stops:
        mapping.setdefault(gtfs_name.lower(), []).append(gtfs_id)
    return mapping


def _find_gtfs_ids(
    stop_name: str, name_to_gtfs: dict[str, list[str]]
) -> list[str]:
    """Find GTFS stop IDs by exact or partial name match."""
    ids = name_to_gtfs.get(stop_name.lower(), [])
    if ids:
        return ids
    # Partial match fallback
    for gname, gids in name_to_gtfs.items():
        if gname in stop_name.lower() or stop_name.lower() in gname:
            return gids
    return []


def _find_next_scheduled(
    session: Session, gtfs_stop_ids: list[str], now: time
) -> time | None:
    """Find the next scheduled arrival within 90 minutes."""
    now_mins = now.hour * 60 + now.minute
    end_mins = min(now_mins + 90, 23 * 60 + 59)
    end_time = time(hour=end_mins // 60, minute=end_mins % 60)

    nearest: time | None = None
    for gtfs_id in gtfs_stop_ids:
        arr = session.execute(
            select(TramStopTime.arrival_time)
            .where(TramStopTime.stop_id == gtfs_id)
            .where(TramStopTime.arrival_time >= now)
            .where(TramStopTime.arrival_time <= end_time)
            .order_by(TramStopTime.arrival_time)
            .limit(1)
        ).scalar()
        if arr is not None and (nearest is None or arr < nearest):
            nearest = arr
    return nearest


def store_delay_snapshot() -> None:
    """Calculate delays by comparing forecasts vs schedule and store in history."""
    session = SessionLocal()

    try:
        current_time = _get_dublin_time(session)
        logger.info("Calculating delays at Dublin time %s", current_time)

        forecasts = session.execute(
            select(TramLuasForecast, TramLuasStop.name).join(
                TramLuasStop, TramLuasForecast.stop_id == TramLuasStop.stop_id
            )
        ).all()

        if not forecasts:
            logger.info("No forecasts in DB — nothing to calculate.")
            return

        # Group by stop+direction, keep soonest
        soonest: dict[str, tuple] = {}
        for forecast, stop_name in forecasts:
            if forecast.due_mins is None:
                continue
            key = f"{forecast.stop_id}|{forecast.direction}"
            if key not in soonest or forecast.due_mins < soonest[key][0].due_mins:
                soonest[key] = (forecast, stop_name)

        name_to_gtfs = _build_name_to_gtfs_ids(session)
        now_utc = datetime.now(tz=timezone.utc)
        delay_count = 0

        for _key, (forecast, stop_name) in soonest.items():
            gtfs_ids = _find_gtfs_ids(stop_name, name_to_gtfs)
            if not gtfs_ids:
                continue

            predicted_mins = current_time.hour * 60 + current_time.minute + forecast.due_mins
            predicted_time = time(hour=min(predicted_mins // 60, 23), minute=predicted_mins % 60)

            next_scheduled = _find_next_scheduled(session, gtfs_ids, current_time)
            if next_scheduled is None:
                continue

            delay_mins = (
                predicted_time.hour * 60 + predicted_time.minute
            ) - (next_scheduled.hour * 60 + next_scheduled.minute)

            if delay_mins <= 0:
                continue

            session.add(
                TramDelayHistory(
                    recorded_at=now_utc,
                    stop_id=forecast.stop_id,
                    stop_name=stop_name,
                    line=forecast.line,
                    direction=forecast.direction,
                    destination=forecast.destination,
                    scheduled_time=next_scheduled.strftime("%H:%M"),
                    due_mins=forecast.due_mins,
                    delay_mins=delay_mins,
                    estimated_affected_passengers=0.0,
                )
            )
            delay_count += 1

        session.commit()
        logger.info("Stored %d delay records.", delay_count)

    except Exception:
        session.rollback()
        logger.exception("Error calculating/storing delay history")
        raise

    finally:
        session.close()
