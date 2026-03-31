"""Calculate tram delays from DB data and persist to tram_delay_history.

Compares live forecasts (tram_luas_forecasts) against the GTFS schedule
(tram_stop_times) to detect delays, then stores them in tram_delay_history.
No Hermes API call required — works entirely from the database.
"""

import logging
from datetime import UTC, datetime, time

from sqlalchemy import select, text

from data_handler.db import SessionLocal
from data_handler.tram.models import (
    TramDelayHistory,
    TramLuasForecast,
    TramLuasStop,
    TramStop,
    TramStopTime,
)

logger = logging.getLogger(__name__)


def store_delay_snapshot() -> None:
    """Calculate delays by comparing forecasts vs schedule and store in history."""
    session = SessionLocal()

    try:
        # 1. Get current Dublin time
        row = session.execute(
            text("SELECT NOW() AT TIME ZONE 'Europe/Dublin'")
        ).scalar()
        now_dublin = row or datetime.now(tz=UTC)
        current_time = now_dublin.time() if hasattr(now_dublin, "time") else now_dublin
        if isinstance(current_time, datetime):
            current_time = current_time.time()

        logger.info("Calculating delays at Dublin time %s", current_time)

        # 2. Get all forecasts with their stop names
        forecasts = session.execute(
            select(TramLuasForecast, TramLuasStop.name)
            .join(TramLuasStop, TramLuasForecast.stop_id == TramLuasStop.stop_id)
        ).all()

        if not forecasts:
            logger.info("No forecasts in DB — nothing to calculate.")
            return

        # 3. Group by stop+direction, keep soonest
        soonest: dict[str, tuple] = {}
        for forecast, stop_name in forecasts:
            if forecast.due_mins is None:
                continue
            key = f"{forecast.stop_id}|{forecast.direction}"
            if key not in soonest or forecast.due_mins < soonest[key][0].due_mins:
                soonest[key] = (forecast, stop_name)

        # 4. Build name → GTFS stop IDs mapping
        gtfs_stops = session.execute(select(TramStop.id, TramStop.name)).all()
        name_to_gtfs_ids: dict[str, list[str]] = {}
        for gtfs_id, gtfs_name in gtfs_stops:
            name_to_gtfs_ids.setdefault(gtfs_name.lower(), []).append(gtfs_id)

        # 5. For each soonest forecast, find next scheduled arrival
        now_utc = datetime.now(tz=UTC)
        delay_count = 0

        for key, (forecast, stop_name) in soonest.items():
            # Find GTFS stop IDs by name
            gtfs_ids = name_to_gtfs_ids.get(stop_name.lower(), [])
            if not gtfs_ids:
                # Try partial match
                for gname, gids in name_to_gtfs_ids.items():
                    if gname in stop_name.lower() or stop_name.lower() in gname:
                        gtfs_ids = gids
                        break
            if not gtfs_ids:
                continue

            # Predicted arrival = now + due_mins
            predicted_mins = (
                current_time.hour * 60 + current_time.minute + forecast.due_mins
            )
            predicted_time = time(
                hour=min(predicted_mins // 60, 23),
                minute=predicted_mins % 60,
            )

            # Find next scheduled arrival at this stop
            next_scheduled = _find_next_scheduled(
                session, gtfs_ids, current_time
            )
            if next_scheduled is None:
                continue

            # Calculate delay
            predicted_total = predicted_time.hour * 60 + predicted_time.minute
            scheduled_total = next_scheduled.hour * 60 + next_scheduled.minute
            delay_mins = predicted_total - scheduled_total

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


def _find_next_scheduled(
    session, gtfs_stop_ids: list[str], now: time
) -> time | None:
    """Find the next scheduled arrival at any of the given GTFS stop IDs."""
    # Look ahead 90 minutes
    now_mins = now.hour * 60 + now.minute
    end_mins = now_mins + 90
    end_time = time(hour=min(end_mins // 60, 23), minute=end_mins % 60)

    nearest = None

    for gtfs_id in gtfs_stop_ids:
        rows = session.execute(
            select(TramStopTime.arrival_time)
            .where(TramStopTime.stop_id == gtfs_id)
            .where(TramStopTime.arrival_time >= now)
            .where(TramStopTime.arrival_time <= end_time)
            .order_by(TramStopTime.arrival_time)
            .limit(1)
        ).scalar()

        if rows is not None:
            arr = rows
            if nearest is None or arr < nearest:
                nearest = arr

    return nearest
