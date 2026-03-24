"""Dublin Bikes GeoJSON API client."""

import logging
from typing import Any

import requests

from data_handler.cycle.gbfs_parsing_utils import validate_station_status_record
from data_handler.settings.api_settings import get_api_settings

logger = logging.getLogger(__name__)


class DublinBikesClient:
    """Client for the Dublin Bikes GeoJSON API (data.smartdublin.ie)."""

    def __init__(self, url: str) -> None:
        self.url = url

    def _fetch_features(self) -> list[dict[str, Any]]:
        """Fetch GeoJSON FeatureCollection and return the features list."""
        logger.info("Fetching data from %s", self.url)
        try:
            response = requests.get(self.url, timeout=10)
            response.raise_for_status()
            data = response.json()
        except requests.RequestException:
            logger.exception("Failed to fetch data from %s", self.url)
            raise

        if data.get("type") != "FeatureCollection" or "features" not in data:
            msg = "Invalid GeoJSON response: expected FeatureCollection with features"
            raise ValueError(msg)

        return data["features"]

    def fetch_station_information(self) -> list[dict[str, Any]]:
        """Return station metadata records (name, location, capacity)."""
        features = self._fetch_features()
        records = []
        for feature in features:
            props = feature["properties"]
            lon, lat = feature["geometry"]["coordinates"]
            records.append(
                {
                    "station_id": props["station_id"],
                    "name": props["name"],
                    "short_name": props.get("short_name") or None,
                    "address": props.get("address") or None,
                    "lat": lat,
                    "lon": lon,
                    "capacity": props["capacity"],
                    "region_id": props.get("region_id") or None,
                }
            )
        logger.info("Fetched %d station information records", len(records))
        return records

    def fetch_station_status(self) -> list[dict[str, Any]]:
        """Return real-time station status records."""
        features = self._fetch_features()
        stations = []
        for feature in features:
            props = feature["properties"]
            station = {
                "station_id": props["station_id"],
                "num_bikes_available": props["num_bikes_available"],
                "num_docks_available": props["num_docks_available"],
                "is_installed": props["is_installed"],
                "is_renting": props["is_renting"],
                "is_returning": props["is_returning"],
                "last_reported": props["last_reported"],
            }
            validate_station_status_record(station)
            stations.append(station)
        logger.info("Fetched %d station status records", len(stations))
        return stations


def get_dublin_bikes_client() -> DublinBikesClient:
    """Factory function to create the API client with settings."""
    settings = get_api_settings()
    return DublinBikesClient(url=settings.dublin_bikes_api_url)
