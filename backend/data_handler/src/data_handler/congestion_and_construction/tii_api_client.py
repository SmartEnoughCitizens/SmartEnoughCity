"""TII (Transport Infrastructure Ireland) API client for traffic data."""

import logging
from dataclasses import dataclass
from typing import Any

import requests

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class BoundingBox:
    """Geographic bounding box for API queries."""

    north: float
    south: float
    east: float
    west: float

    def to_dict(self) -> dict[str, float]:
        """Convert to dictionary for API payload."""
        return {
            "north": self.north,
            "south": self.south,
            "east": self.east,
            "west": self.west,
        }


# Default bounding box for Dublin area
DUBLIN_BOUNDING_BOX = BoundingBox(
    north=53.65,  # Past Swords/Balbriggan
    south=53.15,  # Past Bray/Greystones
    east=-5.90,  # Into the Irish Sea to catch the coast
    west=-6.70,  # Past Maynooth/Leixlip
)


class TIIApiClient:
    """Client for interacting with the TII Traffic API."""

    API_URL = "https://www.tiitraffic.ie/api/graphql"
    DEFAULT_HEADERS = {
        "User-Agent": (
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/120.0.0.0 Safari/537.36"
        ),
        "Content-Type": "application/json",
        "Referer": "https://www.tiitraffic.ie/",
    }
    DEFAULT_LAYER_SLUGS = ["roadReports", "roadwork", "weatherWarningsAreaEvents"]
    DEFAULT_ZOOM = 11
    DEFAULT_TIMEOUT = 15

    def __init__(
        self,
        bounding_box: BoundingBox = DUBLIN_BOUNDING_BOX,
        layer_slugs: list[str] | None = None,
        zoom: int = DEFAULT_ZOOM,
        timeout: int = DEFAULT_TIMEOUT,
    ):
        """
        Initialize the TII API client.

        Args:
            bounding_box: Geographic area to query
            layer_slugs: Types of traffic layers to fetch
            zoom: Map zoom level for query detail
            timeout: Request timeout in seconds
        """
        self.bounding_box = bounding_box
        self.layer_slugs = layer_slugs or self.DEFAULT_LAYER_SLUGS
        self.zoom = zoom
        self.timeout = timeout

    def _build_query_payload(self) -> list[dict[str, Any]]:
        """Build the GraphQL query payload."""
        return [
            {
                "query": """
                query MapFeatures($input: MapFeaturesArgs!) { 
                    mapFeaturesQuery(input: $input) { 
                        mapFeatures { 
                            title 
                            tooltip 
                            features { 
                                geometry 
                                properties 
                            } 
                        } 
                    } 
                }
                """,
                "variables": {
                    "input": {
                        **self.bounding_box.to_dict(),
                        "zoom": self.zoom,
                        "layerSlugs": self.layer_slugs,
                        "nonClusterableUris": ["404"],
                    }
                },
            }
        ]

    def fetch_traffic_data(self) -> dict[str, Any] | None:
        """
        Fetch traffic data from the TII API.

        Returns:
            Raw JSON response from the API, or None if the request fails.

        Raises:
            requests.RequestException: If the API request fails
        """
        logger.info(
            "Fetching traffic data from TII API for bounding box: %s",
            self.bounding_box,
        )

        try:
            response = requests.post(
                self.API_URL,
                json=self._build_query_payload(),
                headers=self.DEFAULT_HEADERS,
                timeout=self.timeout,
            )
            response.raise_for_status()
            data = response.json()
            logger.info("Successfully fetched traffic data from TII API")
            return data

        except requests.Timeout:
            logger.error("Timeout while fetching traffic data from TII API")
            raise

        except requests.RequestException as e:
            logger.error("Error fetching traffic data from TII API: %s", e)
            raise
