"""JCDecaux GBFS API client for Dublin Bikes."""

import logging
from typing import Any

import requests

from data_handler.cycle.gbfs_parsing_utils import validate_station_status_record

logger = logging.getLogger(__name__)


class JCDecauxGBFSClient:
    """Client for JCDecaux Dublin Bikes GBFS API."""

    def __init__(
        self,
        api_key: str = None,
        base_url: str = "https://api.cyclocity.fr/contracts/dublin/gbfs",
    ):
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")

    def _make_request(self, endpoint: str, timeout: int = 10) -> dict[str, Any]:
        """Make HTTP request to GBFS API."""
        url = f"{self.base_url}/{endpoint}"

        params = {}
        if self.api_key:
            params["apiKey"] = self.api_key

        logger.info("Fetching data from %s", url)

        try:
            response = requests.get(url, params=params, timeout=timeout)
            response.raise_for_status()
            return response.json()
        except requests.RequestException:
            logger.exception("Failed to fetch data from %s", url)
            raise

    def fetch_station_information(self) -> list[dict[str, Any]]:
        """Fetch static station information."""
        data = self._make_request("station_information.json")

        if "data" not in data or "stations" not in data["data"]:
            raise ValueError(
                "Invalid API response structure for station_information"
            )

        stations = data["data"]["stations"]
        logger.info("Fetched %d station information records", len(stations))
        return stations

    def fetch_station_status(self) -> list[dict[str, Any]]:
        """Fetch real-time station status."""
        data = self._make_request("station_status.json")

        if "data" not in data or "stations" not in data["data"]:
            raise ValueError(
                "Invalid API response structure for station_status"
            )

        stations = data["data"]["stations"]

        for station in stations:
            validate_station_status_record(station)

        logger.info("Fetched %d station status records", len(stations))
        return stations


def get_jcdecaux_client() -> JCDecauxGBFSClient:
    """Factory function to create API client with settings."""
    from data_handler.settings.data_sources_settings import (
        get_data_sources_settings,
    )

    settings = get_data_sources_settings()
    return JCDecauxGBFSClient(api_key=settings.jcdecaux_api_key)
