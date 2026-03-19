"""Tests for Dublin Bikes GeoJSON API client."""

from unittest.mock import Mock, patch

import pytest
import requests

from data_handler.cycle.api_client import DublinBikesClient


def _make_feature(
    station_id: str = "1",
    lon: float = -6.2625,
    lat: float = 53.3409,
    *,
    bikes: int = 5,
    docks: int = 10,
    is_installed: bool = True,
    is_renting: bool = True,
    is_returning: bool = True,
    last_reported: int = 1769194200,
    name: str = "Test Station",
    capacity: int = 15,
) -> dict:
    return {
        "type": "Feature",
        "geometry": {"type": "Point", "coordinates": [lon, lat]},
        "properties": {
            "station_id": station_id,
            "name": name,
            "short_name": "",
            "address": "Test Address",
            "region_id": "",
            "capacity": capacity,
            "system_id": "dublin_bikes",
            "num_bikes_available": bikes,
            "num_docks_available": docks,
            "is_installed": is_installed,
            "is_renting": is_renting,
            "is_returning": is_returning,
            "last_reported": last_reported,
            "last_updated": last_reported,
            "last_reported_dt": "2026-01-22 17:30:00",
        },
    }


def _make_geojson(features: list) -> dict:
    return {"type": "FeatureCollection", "features": features}


class TestFetchStationInformation:
    """Test fetching static station information."""

    @patch("data_handler.cycle.api_client.requests.get")
    def test_returns_station_list_on_success(self, mock_get: Mock) -> None:
        """Test successful fetch returns list of stations."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson([_make_feature("1")])
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")
        stations = client.fetch_station_information()

        assert len(stations) == 1
        assert stations[0]["name"] == "Test Station"
        mock_get.assert_called_once()

    @patch("data_handler.cycle.api_client.requests.get")
    def test_extracts_lat_lon_from_geometry(self, mock_get: Mock) -> None:
        """Test lat/lon are extracted from GeoJSON geometry."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson(
            [_make_feature("1", lon=-6.2625, lat=53.3409)]
        )
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")
        stations = client.fetch_station_information()

        assert stations[0]["lat"] == 53.3409
        assert stations[0]["lon"] == -6.2625

    @patch("data_handler.cycle.api_client.requests.get")
    def test_converts_empty_string_fields_to_none(self, mock_get: Mock) -> None:
        """Test that empty string short_name and region_id become None."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson([_make_feature("1")])
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")
        stations = client.fetch_station_information()

        assert stations[0]["short_name"] is None
        assert stations[0]["region_id"] is None

    @patch("data_handler.cycle.api_client.requests.get")
    def test_raises_on_invalid_response_structure(self, mock_get: Mock) -> None:
        """Test raises ValueError when response is not a FeatureCollection."""
        mock_response = Mock()
        mock_response.json.return_value = {"invalid": "structure"}
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")

        with pytest.raises(ValueError, match="Invalid GeoJSON response"):
            client.fetch_station_information()

    @patch("data_handler.cycle.api_client.requests.get")
    def test_raises_on_http_error(self, mock_get: Mock) -> None:
        """Test HTTP errors propagate correctly."""
        mock_get.side_effect = requests.HTTPError("503 Service Unavailable")

        client = DublinBikesClient(url="https://example.com")

        with pytest.raises(requests.RequestException):
            client.fetch_station_information()


class TestFetchStationStatus:
    """Test fetching real-time station status."""

    @patch("data_handler.cycle.api_client.requests.get")
    def test_returns_station_status_records(self, mock_get: Mock) -> None:
        """Test successful fetch returns station status."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson([_make_feature("1", bikes=5)])
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")
        stations = client.fetch_station_status()

        assert len(stations) == 1
        assert stations[0]["num_bikes_available"] == 5

    @patch("data_handler.cycle.api_client.requests.get")
    def test_raises_on_invalid_station_record(self, mock_get: Mock) -> None:
        """Test raises when a station record fails validation."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson(
            [_make_feature("1", bikes=-1)]  # Invalid: negative
        )
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")

        with pytest.raises(ValueError, match="Invalid num_bikes_available"):
            client.fetch_station_status()

    @patch("data_handler.cycle.api_client.requests.get")
    def test_returns_multiple_stations(self, mock_get: Mock) -> None:
        """Test multiple stations are all returned."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson(
            [_make_feature("1"), _make_feature("2")]
        )
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")
        stations = client.fetch_station_status()

        assert len(stations) == 2


class TestFetchFeatures:
    """Test the underlying GeoJSON fetch."""

    @patch("data_handler.cycle.api_client.requests.get")
    def test_request_includes_timeout(self, mock_get: Mock) -> None:
        """Test that requests include a timeout."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson([])
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://example.com")
        client.fetch_station_information()

        call_kwargs = mock_get.call_args[1]
        assert "timeout" in call_kwargs
        assert call_kwargs["timeout"] == 10

    @patch("data_handler.cycle.api_client.requests.get")
    def test_uses_configured_url(self, mock_get: Mock) -> None:
        """Test that the configured URL is used."""
        mock_response = Mock()
        mock_response.json.return_value = _make_geojson([])
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        client = DublinBikesClient(url="https://my-custom-url.example.com/stations.geojson")
        client.fetch_station_information()

        call_args = mock_get.call_args[0]
        assert call_args[0] == "https://my-custom-url.example.com/stations.geojson"
