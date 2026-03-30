# data_handler/tram/disruption_service.py

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import datetime

from sqlalchemy import text
from sqlalchemy.orm import Session

logger = logging.getLogger(__name__)





import random

DEBUG_FORCE_DISRUPTION = False

FAKE_DISRUPTIONS = [
    "Service disruption: delays expected due to signal failure.",
    "Line suspended due to maintenance work.",
    "Severe delays on all routes due to weather conditions."
    ]

def _is_disrupted(forecasts: list[dict]) -> str | None:
    """Return disruption message if any forecast entry signals a disruption."""
    for entry in forecasts:
        msg = (entry.get("message") or "").lower()
        if any(kw in msg for kw in DISRUPTION_KEYWORDS):
            return entry["message"]
    return None

DISRUPTION_KEYWORDS = {
    "not in service", "disruption", "suspended",
    "delay", "fault", "no service", "terminated",
}

_ALTERNATE_QUERY = text("""
WITH affected AS (
    SELECT lat, lon
    FROM external_data.tram_luas_stops
    WHERE stop_id = :stop_id
),
bus AS (
    SELECT
        'bus'            AS transport_type,
        bs.id            AS stop_id,
        bs.name          AS stop_name,
        bs.lat,
        bs.lon,
        NULL::integer    AS available_bikes,
        NULL::integer    AS capacity,
        EARTH_DISTANCE(
            LL_TO_EARTH(a.lat, a.lon),
            LL_TO_EARTH(bs.lat, bs.lon)
        )::integer       AS distance_m
    FROM external_data.bus_stops bs, affected a
    WHERE EARTH_DISTANCE(
        LL_TO_EARTH(a.lat, a.lon),
        LL_TO_EARTH(bs.lat, bs.lon)
    ) <= :radius_m
),
rail AS (
    SELECT
        'rail'               AS transport_type,
        rs.station_code      AS stop_id,
        rs.station_desc      AS stop_name,
        rs.lat,
        rs.lon,
        NULL::integer        AS available_bikes,
        NULL::integer        AS capacity,
        EARTH_DISTANCE(
            LL_TO_EARTH(a.lat, a.lon),
            LL_TO_EARTH(rs.lat, rs.lon)
        )::integer           AS distance_m
    FROM external_data.irish_rail_stations rs, affected a
    WHERE EARTH_DISTANCE(
        LL_TO_EARTH(a.lat, a.lon),
        LL_TO_EARTH(rs.lat, rs.lon)
    ) <= :radius_m
),
bikes AS (
    SELECT
        'bike'                              AS transport_type,
        dbs.system_id                       AS stop_id,
        dbs.name                            AS stop_name,
        dbs.latitude::double precision      AS lat,
        dbs.longitude::double precision     AS lon,
        snap.available_bikes,
        dbs.capacity,
        EARTH_DISTANCE(
            LL_TO_EARTH(a.lat, a.lon),
            LL_TO_EARTH(
                dbs.latitude::double precision,
                dbs.longitude::double precision
            )
        )::integer                          AS distance_m
    FROM external_data.dublin_bikes_stations dbs
    JOIN (
        SELECT DISTINCT ON (station_id)
            station_id, available_bikes
        FROM external_data.dublin_bikes_station_snapshots
        WHERE is_renting = true
        ORDER BY station_id, timestamp DESC
    ) snap ON snap.station_id = dbs.station_id,
    affected a
    WHERE EARTH_DISTANCE(
        LL_TO_EARTH(a.lat, a.lon),
        LL_TO_EARTH(
            dbs.latitude::double precision,
            dbs.longitude::double precision
        )
    ) <= :radius_m
    AND snap.available_bikes > 0
)
SELECT * FROM bus
UNION ALL SELECT * FROM rail
UNION ALL SELECT * FROM bikes
ORDER BY transport_type, distance_m
""")


@dataclass
class AlternateOption:
    transport_type: str        # "bus" | "rail" | "bike"
    stop_id: str
    stop_name: str
    lat: float
    lon: float
    distance_m: int
    available_bikes: int | None = None
    capacity: int | None = None


@dataclass
class DisruptionReport:
    stop_id: str
    stop_name: str
    line: str
    message: str
    detected_at: datetime
    alternates: list[AlternateOption] = field(default_factory=list)





def _get_alternates(session: Session, stop_id: str, radius_m: int) -> list[AlternateOption]:
    rows = session.execute(
        _ALTERNATE_QUERY,
        {"stop_id": stop_id, "radius_m": radius_m}
    ).fetchall()

    return [
        AlternateOption(
            transport_type=r.transport_type,
            stop_id=r.stop_id,
            stop_name=r.stop_name,
            lat=r.lat,
            lon=r.lon,
            distance_m=r.distance_m,
            available_bikes=r.available_bikes,
            capacity=r.capacity,
        )
        for r in rows
    ]

def check_for_disruptions(
    session: Session,
    radius_m: int = 500,
) -> list[DisruptionReport]:
    """
    For every tram stop that has a disruption message in its current
    forecast, build a DisruptionReport with nearby alternate transport.

    Pass the already-open session from process_tram_live_data() so
    this runs in the same transaction against fresh forecast data.
    """
    from data_handler.tram.forecast_handler import fetch_forecast_for_stop  # avoid circular import
    stops = session.execute(
        text("""
            SELECT stop_id, name, line
            FROM external_data.tram_luas_stops
        """)
    ).fetchall()

    reports: list[DisruptionReport] = []

    for stop in stops:
        entries = fetch_forecast_for_stop(stop.stop_id)
        disruption_msg = _is_disrupted(entries)

        if not disruption_msg:
            continue

        logger.warning(
            "Disruption at %s (%s line): %s",
            stop.name, stop.line, disruption_msg,
        )

        alternates = _get_alternates(session, stop.stop_id, radius_m)

        reports.append(DisruptionReport(
            stop_id=stop.stop_id,
            stop_name=stop.name,
            line=stop.line,
            message=disruption_msg,
            detected_at=datetime.utcnow(),
            alternates=alternates,
        ))

    return reports


def format_report_for_provider(report: DisruptionReport) -> dict:
    """
    Serialise a DisruptionReport into a JSON-safe dict ready to POST
    to City Service Provider webhooks or push onto a message queue.
    """
    by_type: dict[str, list] = {"bus": [], "rail": [], "bike": []}

    for alt in report.alternates:
        entry: dict = {
            "stop_id": alt.stop_id,
            "name": alt.stop_name,
            "distance_m": alt.distance_m,
            "lat": alt.lat,
            "lon": alt.lon,
        }
        if alt.transport_type == "bike":
            entry["available_bikes"] = alt.available_bikes
            entry["capacity"] = alt.capacity

        by_type[alt.transport_type].append(entry)

    return {
        "stop_id": report.stop_id,
        "stop_name": report.stop_name,
        "line": report.line,
        "message": report.message,
        "detected_at": report.detected_at.isoformat(),
        "alternates": by_type,
    }