"""Ticketmaster Discovery API v2 client for fetching Dublin events and venues."""

import logging
from datetime import UTC, datetime, timedelta
from typing import Any

import requests

logger = logging.getLogger(__name__)


class TicketmasterClient:
    """Client for interacting with the Ticketmaster Discovery API v2."""

    DEFAULT_TIMEOUT: int = 15

    def __init__(
        self,
        api_key: str,
        base_url: str,
        timeout: int = DEFAULT_TIMEOUT,
    ) -> None:
        self.api_key = api_key
        self.base_url = base_url
        self.timeout = timeout

    def _get(
        self, endpoint: str, extra_params: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        """
        Perform a GET request against the Ticketmaster Discovery API.

        Args:
            endpoint: API endpoint path (e.g. "/events.json").
            extra_params: Additional query parameters merged with the defaults.

        Returns:
            Parsed JSON response body.

        Raises:
            requests.Timeout: If the request times out.
            requests.RequestException: On any other HTTP error.
        """
        params: dict[str, Any] = {
            "apikey": self.api_key,
            "city": "Dublin",
            "countryCode": "IE",
            "size": 200,
        }
        if extra_params:
            params.update(extra_params)

        try:
            response = requests.get(
                f"{self.base_url}{endpoint}",
                params=params,
                timeout=self.timeout,
            )
            response.raise_for_status()
        except requests.Timeout:
            logger.exception("Timeout while fetching %s from Ticketmaster", endpoint)
            raise
        except requests.RequestException:
            logger.exception("Error fetching %s from Ticketmaster", endpoint)
            raise

        return response.json()

    def fetch_events(self, days_ahead: int = 7) -> dict[str, Any]:
        """
        Fetch upcoming Dublin events from the Ticketmaster API.

        Args:
            days_ahead: Number of days into the future to search.

        Returns:
            Raw JSON response from the API.

        Raises:
            requests.Timeout: If the request times out.
            requests.RequestException: On any other HTTP error.
        """
        now = datetime.now(tz=UTC)
        start = now.strftime("%Y-%m-%dT%H:%M:%SZ")
        end = (now + timedelta(days=days_ahead)).strftime("%Y-%m-%dT%H:%M:%SZ")

        logger.info(
            "Fetching events from Ticketmaster for Dublin (next %d days)", days_ahead
        )

        data = self._get(
            "/events.json",
            extra_params={"startDateTime": start, "endDateTime": end},
        )
        logger.info("Successfully fetched events from Ticketmaster")
        return data

    def fetch_venues(self) -> dict[str, Any]:
        """
        Fetch all Dublin venues from the Ticketmaster Discovery API.

        Returns:
            Raw JSON response from the API.

        Raises:
            requests.Timeout: If the request times out.
            requests.RequestException: On any other HTTP error.
        """
        logger.info("Fetching venues from Ticketmaster for Dublin")

        data = self._get("/venues.json")
        logger.info("Successfully fetched venues from Ticketmaster")
        return data


def get_ticketmaster_client() -> TicketmasterClient:
    """Factory function to create a TicketmasterClient from settings."""
    from data_handler.settings.api_settings import get_api_settings  # noqa: PLC0415

    settings = get_api_settings()
    return TicketmasterClient(
        api_key=settings.ticketmaster_api_key,
        base_url=settings.ticketmaster_api_base_url,
    )
