"""EV Charging API endpoints."""

from fastapi import APIRouter, HTTPException

from inference_engine import ev_service

router = APIRouter(prefix="/ev", tags=["EV Charging"])


@router.get("/areas-geojson")
def get_areas_geojson() -> dict:
    """Return a GeoJSON FeatureCollection of Dublin electoral divisions with charging demand data."""
    try:
        return ev_service.get_areas_geojson()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.get("/charging-stations")
def get_charging_stations() -> dict:
    """Return all Dublin EV charging stations with location and charger count."""
    try:
        return ev_service.get_charging_stations()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@router.get("/charging-demand")
def get_charging_demand() -> dict:
    """Return per-area EV charging demand statistics for Dublin."""
    try:
        return ev_service.get_charging_demand()
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
