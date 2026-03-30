"""EV Charging service."""

import unicodedata
from collections.abc import Generator
from contextlib import contextmanager

import psycopg2

from inference_engine.settings.api_settings import get_db_settings


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
    with _get_db() as conn, conn.cursor() as cur:
        cur.execute(
<<<<<<< Updated upstream
            "SELECT electoral_division, charging_demand, registered_ev"
            " FROM external_data.ev_charging_demand"
        )

    demand_lookup: dict = {}
=======
            "SELECT ed_english, county_english,"
            "       ST_AsGeoJSON(geom)::json AS geometry"
            " FROM external_data.ev_electoral_divisions"
        )
        division_rows = cur.fetchall()

        cur.execute(
            "SELECT electoral_division, charging_demand, registered_ev"
            " FROM external_data.ev_charging_demand"
        )
        demand_rows = cur.fetchall()

    demand_lookup: dict = {}
    for electoral_division, charging_demand, registered_ev in demand_rows:
>>>>>>> Stashed changes
        division = str(electoral_division).split(",", maxsplit=1)[0].strip()
        demand_lookup[_normalize(division)] = {
            "charging_demand": charging_demand,
            "registered_ev": registered_ev,
        }

<<<<<<< Updated upstream

=======
    features = []
    for ed_english, county_english, geometry in division_rows:
        data = demand_lookup.get(_normalize(ed_english), {})
        features.append({
            "type": "Feature",
            "geometry": geometry,
            "properties": {
                "ED_ENGLISH": ed_english,
                "COUNTY_ENGLISH": county_english,
                "display_name": ed_english.title(),
                "charging_demand": data.get("charging_demand"),
                "registered_ev": data.get("registered_ev"),
            },
        })

    return {"type": "FeatureCollection", "features": features}
>>>>>>> Stashed changes


def get_charging_stations() -> dict:
    """Return all Dublin EV charging stations with location and charger count."""
<<<<<<< Updated upstream
    )

    stations = [
        {
        }
=======
    with _get_db() as conn, conn.cursor() as cur:
        cur.execute(
            "SELECT address, county, lat, lon, charger_count, is_24_7"
            " FROM external_data.ev_charging_points"
        )
        rows = cur.fetchall()

    stations = [
        {
            "address": address or "",
            "county": county,
            "latitude": lat,
            "longitude": lon,
            "charger_count": charger_count,
            "open_hours": "24 x 7" if is_24_7 else "See station for hours",
        }
        for address, county, lat, lon, charger_count, is_24_7 in rows
>>>>>>> Stashed changes
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
