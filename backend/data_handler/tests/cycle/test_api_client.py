"""Tests for JCDecaux GBFS API client."""

from unittest.mock import Mock, patch

import pytest
import requests

from data_handler.cycle.api_client import JCDecauxGBFSClient


class TestJCDecauxGBFSClientInit:
    """Test JCDecaux GBFS API client initialisation."""

    def test_client_stores_api_key(self) -> None:
        """Test client stores the provided API key."""
        client = JCDecauxGBFSClient(api_key="test_key")
        assert client.api_key == "test_key"

    def test_client_default_base_url_is_empty(self) -> None:
        """Test client base URL defaults to empty when not provided."""
        client = JCDecauxGBFSClient()
        assert client.base_url == ""

    def test_client_strips_trailing_slash_from_base_url(self) -> None:
        """Test base URL trailing slash is removed."""
        client = JCDecauxGBFSClient(base_url="https://example.com/api/")
        assert not client.base_url.endswith("/")

    def test_client_works_without_api_key(self) -> None:
        """Test client can be created without API key."""
        client = JCDecauxGBFSClient()
        assert client.api_key is None


class TestFetchStationInformation:
    """Test fetching static station information."""

    @patch("data_handler.cycle.api_client.requests.get")
    def test_returns_station_list_on_success(self, mock_get: Mock) -> None:
        """Test successful fetch returns list of stations."""
        mock_response = Mock()
        mock_response.json.return_value = {
            "data": {
                "stations": [
                    {
                        "station_id": "1",
                        "name": "Test Station",
                        "lat": 53.349316,
                        "lon": -6.262876,
                        "capacity": 30,
                    }
                ]
            }
        }
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient()
        stations = client.fetch_station_information()

        assert len(stations) == 1
        assert stations[0]["name"] == "Test Station"
        mock_get.assert_called_once()

    @patch("data_handler.cycle.api_client.requests.get")
    def test_raises_on_invalid_response_structure(self, mock_get: Mock) -> None:
        """Test raises ValueError when response structure is invalid."""
        mock_response = Mock()
        mock_response.json.return_value = {"invalid": "structure"}
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient()

        with pytest.raises(ValueError, match="Invalid API response structure"):
            client.fetch_station_information()

    @patch("data_handler.cycle.api_client.requests.get")
    def test_raises_on_http_error(self, mock_get: Mock) -> None:
        """Test HTTP errors propagate correctly."""
        mock_get.side_effect = requests.HTTPError("404 Not Found")

        client = JCDecauxGBFSClient()

        with pytest.raises(requests.RequestException):
            client.fetch_station_information()


class TestFetchStationStatus:
    """Test fetching real-time station status."""

    @patch("data_handler.cycle.api_client.requests.get")
    def test_returns_validated_station_status(self, mock_get: Mock) -> None:
        """Test returns validated station status records."""
        mock_response = Mock()
        mock_response.json.return_value = {
            "data": {
                "stations": [
                    {
                        "station_id": "1",
                        "num_bikes_available": 5,
                        "num_docks_available": 10,
                        "is_installed": True,
                        "is_renting": True,
                        "is_returning": True,
                        "last_reported": "2026-01-22T17:30:00+00:00",
                    }
                ]
            }
        }
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient()
        stations = client.fetch_station_status()

        assert len(stations) == 1
        assert stations[0]["num_bikes_available"] == 5

    @patch("data_handler.cycle.api_client.requests.get")
    def test_raises_on_invalid_station_record(self, mock_get: Mock) -> None:
        """Test raises when a station record fails validation."""
        mock_response = Mock()
        mock_response.json.return_value = {
            "data": {
                "stations": [
                    {
                        "station_id": "1",
                        "num_bikes_available": -1,  # Invalid: negative
                        "num_docks_available": 10,
                        "is_installed": True,
                        "is_renting": True,
                        "is_returning": True,
                        "last_reported": "2026-01-22T17:30:00+00:00",
                    }
                ]
            }
        }
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient()

        with pytest.raises(ValueError, match="Invalid num_bikes_available"):
            client.fetch_station_status()


class TestMakeRequest:
    """Test the underlying HTTP request mechanism."""

    @patch("data_handler.cycle.api_client.requests.get")
    def test_request_includes_timeout(self, mock_get: Mock) -> None:
        """Test that all requests include a timeout."""
        mock_response = Mock()
        mock_response.json.return_value = {"data": {"stations": []}}
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient()
        client.fetch_station_information()

        call_kwargs = mock_get.call_args[1]
        assert "timeout" in call_kwargs
        assert call_kwargs["timeout"] == 10

    @patch("data_handler.cycle.api_client.requests.get")
    def test_request_includes_api_key_when_provided(self, mock_get: Mock) -> None:
        """Test that API key is sent as a query parameter."""
        mock_response = Mock()
        mock_response.json.return_value = {"data": {"stations": []}}
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient(api_key="my_secret_key")
        client.fetch_station_information()

        call_kwargs = mock_get.call_args[1]
        assert call_kwargs["params"]["apiKey"] == "my_secret_key"

    @patch("data_handler.cycle.api_client.requests.get")
    def test_request_omits_api_key_when_not_provided(self, mock_get: Mock) -> None:
        """Test no apiKey param when client has no key."""
        mock_response = Mock()
        mock_response.json.return_value = {"data": {"stations": []}}
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = JCDecauxGBFSClient()
        client.fetch_station_information()

        call_kwargs = mock_get.call_args[1]
        assert "apiKey" not in call_kwargs.get("params", {})
