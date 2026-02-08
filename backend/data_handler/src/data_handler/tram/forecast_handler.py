import logging

import pandas as pd
import requests
import xmltodict
from sqlalchemy import delete, select

from data_handler.db import SessionLocal
from data_handler.tram.models import TramLuasForecast, TramLuasStop

logger = logging.getLogger(__name__)

LUAS_LINES = {
    "red": "Luas Red Line",
    "green": "Luas Green Line",
}

LUAS_STOPS_URL = (
    "https://luasforecasts.rpa.ie/xml/get.ashx?action=stops&encrypt=false"
)
LUAS_FORECAST_URL = (
    "https://luasforecasts.rpa.ie/xml/get.ashx?action=forecast&stop={stop_id}&encrypt=false"
)


# ── Fetch helpers ────────────────────────────────────────────────


def fetch_luas_stops(line_name: str) -> pd.DataFrame:
    """Fetch stops for a given Luas line from the forecasting API."""
    res = requests.get(LUAS_STOPS_URL)
    res.raise_for_status()

    doc = xmltodict.parse(res.text)
    lines = doc.get("stops", {}).get("line", [])

    if isinstance(lines, dict):
        lines = [lines]

    target = LUAS_LINES[line_name]
    rows = []

    for line in lines:
        if line.get("@name") == target:
            stops = line.get("stop", [])
            if isinstance(stops, dict):
                stops = [stops]

            for stop in stops:
                rows.append(
                    {
                        "stop_id": stop["@abrev"],
                        "line": line_name,
                        "name": stop.get("#text", stop.get("@text", "")),
                        "pronunciation": stop.get("@pronunciation", ""),
                        "park_ride": stop.get("@isParkRide") == "1",
                        "cycle_ride": stop.get("@isCycleRide") == "1",
                        "lat": float(stop["@lat"]),
                        "lon": float(stop["@long"]),
                    }
                )

    return pd.DataFrame(rows)


def fetch_forecast_for_stop(stop_id: str) -> list[dict]:
    """Fetch live forecast entries for a single Luas stop."""
    url = LUAS_FORECAST_URL.format(stop_id=stop_id)
    res = requests.get(url)
    res.raise_for_status()

    try:
        doc = xmltodict.parse(res.text)
    except Exception:
        return []

    stop_info = doc.get("stopInfo", {})
    message = stop_info.get("message", "")
    directions = stop_info.get("direction", [])

    if isinstance(directions, dict):
        directions = [directions]

    rows = []

    for d in directions:
        direction_name = d["@name"]
        trams = d.get("tram", [])
        if isinstance(trams, dict):
            trams = [trams]

        for tram in trams:
            due = tram.get("@dueMins")
            rows.append(
                {
                    "direction": direction_name,
                    "destination": tram.get("@destination", ""),
                    "due_mins": int(due) if due and due.isdigit() else None,
                    "message": message,
                }
            )

    return rows


# ── DB writers ───────────────────────────────────────────────────


def luas_stops_to_db() -> None:
    """Fetch Luas stops from the forecasting API and upsert into DB."""
    session = SessionLocal()

    try:
        for line in ["red", "green"]:
            logger.info("Loading LUAS %s line stops...", line)
            df = fetch_luas_stops(line)

            if df.empty:
                logger.warning("No LUAS stops found for %s.", line)
                continue

            for _, row in df.iterrows():
                existing = session.get(TramLuasStop, row["stop_id"])
                if existing:
                    existing.line = row["line"]
                    existing.name = row["name"]
                    existing.pronunciation = row["pronunciation"]
                    existing.park_ride = row["park_ride"]
                    existing.cycle_ride = row["cycle_ride"]
                    existing.lat = row["lat"]
                    existing.lon = row["lon"]
                else:
                    session.add(
                        TramLuasStop(
                            stop_id=row["stop_id"],
                            line=row["line"],
                            name=row["name"],
                            pronunciation=row["pronunciation"],
                            park_ride=row["park_ride"],
                            cycle_ride=row["cycle_ride"],
                            lat=row["lat"],
                            lon=row["lon"],
                        )
                    )

            logger.info(
                "Inserted/updated %d LUAS stops (%s line).", len(df), line
            )

        session.commit()

    except Exception:
        session.rollback()
        logger.exception("Error inserting LUAS stops")
        raise

    finally:
        session.close()


def luas_forecasts_to_db() -> None:
    """Fetch live forecasts for all known stops and insert into DB."""
    session = SessionLocal()

    try:
        stops = session.execute(
            select(TramLuasStop.stop_id, TramLuasStop.line)
        ).fetchall()

        if not stops:
            logger.warning("No stops in DB. Run luas_stops_to_db() first!")
            return

        # Clear old forecasts before inserting fresh data
        session.execute(delete(TramLuasForecast))

        forecast_count = 0

        for stop_id, line_name in stops:
            logger.info("Fetching forecast for %s (%s)...", stop_id, line_name)
            entries = fetch_forecast_for_stop(stop_id)

            for e in entries:
                session.add(
                    TramLuasForecast(
                        stop_id=stop_id,
                        line=line_name,
                        direction=e["direction"],
                        destination=e["destination"],
                        due_mins=e["due_mins"],
                        message=e["message"],
                    )
                )
                forecast_count += 1

        session.commit()
        logger.info("Inserted %d LUAS forecast rows.", forecast_count)

    except Exception:
        session.rollback()
        logger.exception("Error inserting LUAS forecasts")
        raise

    finally:
        session.close()
