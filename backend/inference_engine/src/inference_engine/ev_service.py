"""EV Charging service."""

import json
import unicodedata
from collections.abc import Generator
from contextlib import contextmanager
from pathlib import Path

import pandas as pd
import psycopg2

from inference_engine.settings.api_settings import get_db_settings

_DATA_DIR = (
    Path(__file__).resolve().parent.parent.parent
    / "recommendation"
    / "ev_points"
    / "data"
)
_GEOJSON_PATH = _DATA_DIR / "location_data.geojson"
_STATIONS_PATH = _DATA_DIR / "charging_point_location.csv"

_DUBLIN_COUNTIES = {"DUBLIN CITY", "FINGAL", "SOUTH DUBLIN"}


@contextmanager
def _get_db() -> Generator[psycopg2.extensions.connection, None, None]:
    """Yield a database connection and ensure it is closed on exit."""
    s = get_db_settings()
    conn = psycopg2.connect(
        host=s.db_host,
        port=s.db_port,
        dbname=s.db_name,
        user=s.db_user,
        password=s.db_password,
    )
    try:
        yield conn
    finally:
        conn.close()


def _normalize(name: str) -> str:
    """Return an accent-stripped, lowercased string for name matching."""
    nfkd = unicodedata.normalize("NFKD", name)
    return "".join(c for c in nfkd if not unicodedata.combining(c)).lower().strip()


def get_areas_geojson() -> dict:
    """Return a GeoJSON FeatureCollection of Dublin electoral divisions with charging demand data."""
    with _GEOJSON_PATH.open(encoding="utf-8") as f:
        geojson = json.load(f)

    with _get_db() as conn, conn.cursor() as cur:
        cur.execute(
            "SELECT electoral_division, charging_demand, registered_ev"
            " FROM external_data.ev_charging_demand"
        )
        rows = cur.fetchall()

    demand_lookup: dict = {}
    for electoral_division, charging_demand, registered_ev in rows:
        division = str(electoral_division).split(",", maxsplit=1)[0].strip()
        demand_lookup[_normalize(division)] = {
            "charging_demand": charging_demand,
            "registered_ev": registered_ev,
        }

    dublin_features = []
    for feature in geojson["features"]:
        props = feature["properties"]
        county = (props.get("COUNTY_ENGLISH") or "").upper()
        if county not in _DUBLIN_COUNTIES:
            continue

        ed_name = props.get("ED_ENGLISH", "")
        data = demand_lookup.get(_normalize(ed_name), {})

        props["display_name"] = ed_name.title()
        props["charging_demand"] = data.get("charging_demand")
        props["registered_ev"] = data.get("registered_ev")

        dublin_features.append(feature)

    return {"type": "FeatureCollection", "features": dublin_features}


def get_charging_stations() -> dict:
    """Return all Dublin EV charging stations with location and charger count."""
    df = pd.read_csv(_STATIONS_PATH, encoding="latin-1")

    dublin_mask = (
        df["County"]
        .str.upper()
        .isin({"DUBLIN", "CO. DUBLIN", "DUBLIN CITY", "DUN LAOGHAIRE"})
        if "County" in df.columns
        else pd.Series([True] * len(df))
    )
    df = df[dublin_mask].dropna(subset=["Latitude", "Longitude"])

    stations = [
        {
            "address": str(row.get("Address", "")),
            "county": str(row.get("County", "")),
            "latitude": float(row["Latitude"]),
            "longitude": float(row["Longitude"]),
            "charger_count": int(row.get("Nr. Chargers", 1)),
            "open_hours": str(row.get("Open Hours", "24 x 7")),
        }
        for _, row in df.iterrows()
    ]

    return {"total_stations": len(stations), "stations": stations}


def get_charging_demand() -> dict:
    """Return per-area EV charging demand statistics for Dublin."""
    with _get_db() as conn, conn.cursor() as cur:
        cur.execute(
            "SELECT electoral_division, registered_ev, charging_demand,"
            "       home_charge_pct, charge_frequency"
            " FROM external_data.ev_charging_demand"
            " WHERE electoral_division ILIKE ANY"
            "       (ARRAY['%Dublin City%', '%Fingal%', '%South Dublin%'])"
            " ORDER BY charging_demand DESC"
        )
        rows = cur.fetchall()

    areas = [
        {
            "area": electoral_division,
            "registered_evs": int(registered_ev or 0),
            "charging_demand": int(charging_demand or 0),
            "home_charge_percentage": float(home_charge_pct or 0),
            "charge_frequency": float(charge_frequency or 0),
        }
        for electoral_division, registered_ev, charging_demand, home_charge_pct, charge_frequency in rows
    ]

    high_priority = [a["area"] for a in areas if a["charging_demand"] >= 20]

    return {
        "summary": {
            "total_areas": len(areas),
            "high_priority_count": len(high_priority),
        },
        "high_priority_areas": high_priority,
        "areas": areas,
    }
