"""Ticketmaster Discovery API v2 client for fetching Dublin events."""

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

    def fetch_events(self, days_ahead: int = 7) -> dict[str, Any] | None:
        """
        Fetch upcoming Dublin events from the Ticketmaster API.

        Args:
            days_ahead: Number of days into the future to search.

        Returns:
            Raw JSON response from the API, or None if the request fails.
        """
        now = datetime.now(tz=UTC)
        start = now.strftime("%Y-%m-%dT%H:%M:%SZ")
        end = (now + timedelta(days=days_ahead)).strftime("%Y-%m-%dT%H:%M:%SZ")

        params = {
            "apikey": self.api_key,
            "city": "Dublin",
            "countryCode": "IE",
            "startDateTime": start,
            "endDateTime": end,
            "size": 200,
        }

        logger.info(
            "Fetching events from Ticketmaster for Dublin (next %d days)", days_ahead
        )

        try:
            response = requests.get(
                self.base_url,
                params=params,
                timeout=self.timeout,
            )
            response.raise_for_status()
        except requests.Timeout:
            logger.exception("Timeout while fetching events from Ticketmaster")
            raise
        except requests.RequestException:
            logger.exception("Error fetching events from Ticketmaster")
            raise
        else:
            data = response.json()
            logger.info("Successfully fetched events from Ticketmaster")
            return data


def get_ticketmaster_client() -> TicketmasterClient:
    """Factory function to create a TicketmasterClient from settings."""
    from data_handler.settings.api_settings import get_api_settings  # noqa: PLC0415

    settings = get_api_settings()
    return TicketmasterClient(
        api_key=settings.ticketmaster_api_key,
        base_url=settings.ticketmaster_api_base_url,
    )
