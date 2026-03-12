"""
EV Charging endpoints — served directly from the inference engine.
"""

import json
import unicodedata
from pathlib import Path

import pandas as pd
from fastapi import APIRouter, HTTPException

router = APIRouter(prefix="/ev", tags=["EV Charging"])

# ── Paths ────────────────────────────────────────────────────────────────────
_DATA_DIR = (
    Path(__file__).resolve().parent.parent.parent
    / "recommendation"
    / "ev_points"
    / "data"
)
_GEOJSON_PATH = _DATA_DIR / "location_data.geojson"
_DEMAND_PATH = _DATA_DIR / "charging_demand.csv"
_STATIONS_PATH = _DATA_DIR / "charging_point_location.csv"

# Dublin counties present in location_data.geojson
_DUBLIN_COUNTIES = {"DUBLIN CITY", "FINGAL", "SOUTH DUBLIN"}


def _normalize(name: str) -> str:
    """Strip accents and lowercase for fuzzy key matching."""
    nfkd = unicodedata.normalize("NFKD", name)
    return "".join(c for c in nfkd if not unicodedata.combining(c)).lower().strip()


# ── GET /ev/areas-geojson ─────────────────────────────────────────────────────
@router.get("/areas-geojson")
def get_areas_geojson() -> dict:
    """
    Merged GeoJSON: Dublin electoral divisions with charging_demand injected.
    Replicates the Python map script logic from ev_charging_estimate.py.
    """
    try:
        with open(_GEOJSON_PATH, encoding="utf-8") as f:
            geojson = json.load(f)

        df = pd.read_csv(
            _DEMAND_PATH,
            usecols=[
                "CSO Electoral Divisions 2022",
                "charging_demand",
                "Registered_ev",
            ],
        )

        # Build lookup keyed by normalised division name (before the comma)
        demand_lookup: dict = {}
        for _, row in df.iterrows():
            full_name = str(row["CSO Electoral Divisions 2022"])
            division = full_name.split(",")[0].strip()
            demand_lookup[_normalize(division)] = {
                "charging_demand": row["charging_demand"],
                "registered_ev": row["Registered_ev"],
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

    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


# ── GET /ev/charging-stations ─────────────────────────────────────────────────
@router.get("/charging-stations")
def get_charging_stations() -> dict:
    """All EV charging stations with location and charger count."""
    try:
        df = pd.read_csv(_STATIONS_PATH, encoding="latin-1")

        # Keep only Dublin stations with valid coordinates
        dublin_mask = (
            df["County"]
            .str.upper()
            .isin({"DUBLIN", "CO. DUBLIN", "DUBLIN CITY", "DUN LAOGHAIRE"})
            if "County" in df.columns
            else pd.Series([True] * len(df))
        )

        df = df[dublin_mask].dropna(subset=["Latitude", "Longitude"])

        stations = []
        for _, row in df.iterrows():
            stations.append(
                {
                    "address": str(row.get("Address", "")),
                    "county": str(row.get("County", "")),
                    "latitude": float(row["Latitude"]),
                    "longitude": float(row["Longitude"]),
                    "charger_count": int(row.get("Nr. Chargers", 1)),
                    "open_hours": str(row.get("Open Hours", "24 x 7")),
                }
            )

        return {"total_stations": len(stations), "stations": stations}

    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


# ── GET /ev/charging-demand ───────────────────────────────────────────────────
@router.get("/charging-demand")
def get_charging_demand() -> dict:
    """Summary and per-area charging demand stats."""
    try:
        df = pd.read_csv(_DEMAND_PATH)

        # Filter to Dublin areas only
        dublin_df = df[
            df["CSO Electoral Divisions 2022"].str.contains(
                "Dublin City|Fingal|South Dublin", case=False, na=False
            )
        ]

        high_priority = dublin_df[dublin_df["charging_demand"] >= 20]

        areas = []
        for _, row in dublin_df.iterrows():
            areas.append(
                {
                    "area": str(row["CSO Electoral Divisions 2022"]),
                    "registered_evs": int(row.get("Registered_ev", 0)),
                    "charging_demand": int(row.get("charging_demand", 0)),
                    "home_charge_percentage": float(
                        row.get("home_charge_percentage", 0)
                    ),
                    "charge_frequency": float(row.get("charge_frequency", 0)),
                }
            )

        return {
            "summary": {
                "total_areas": len(dublin_df),
                "high_priority_count": len(high_priority),
            },
            "high_priority_areas": high_priority[
                "CSO Electoral Divisions 2022"
            ].tolist(),
            "areas": areas,
        }

    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
