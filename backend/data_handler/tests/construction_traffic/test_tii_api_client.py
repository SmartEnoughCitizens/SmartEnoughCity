from unittest.mock import MagicMock, patch

import pytest
import requests

from data_handler.congestion_and_construction.tii_api_client import (
    DUBLIN_BOUNDING_BOX,
    BoundingBox,
    TIIApiClient,
)


class TestBoundingBox:
    """Tests for BoundingBox dataclass."""

    def test_to_dict(self) -> None:
        """to_dict returns correct dictionary representation."""
        bbox = BoundingBox(north=53.65, south=53.15, east=-5.90, west=-6.70)
        result = bbox.to_dict()
        assert result == {
            "north": 53.65,
            "south": 53.15,
            "east": -5.90,
            "west": -6.70,
        }

    def test_dublin_bounding_box_values(self) -> None:
        """Default Dublin bounding box has expected values."""
        assert DUBLIN_BOUNDING_BOX.north == 53.65
        assert DUBLIN_BOUNDING_BOX.south == 53.15
        assert DUBLIN_BOUNDING_BOX.east == -5.90
        assert DUBLIN_BOUNDING_BOX.west == -6.70

    def test_frozen_dataclass(self) -> None:
        """BoundingBox is immutable."""
        bbox = BoundingBox(north=53.65, south=53.15, east=-5.90, west=-6.70)
        with pytest.raises(AttributeError):
            bbox.north = 54.0  # type: ignore[misc]


class TestTIIApiClient:
    """Tests for TIIApiClient."""

    def test_default_initialization(self) -> None:
        """Client initializes with default values."""
        client = TIIApiClient()
        assert client.bounding_box == DUBLIN_BOUNDING_BOX
        assert client.layer_slugs == TIIApiClient.DEFAULT_LAYER_SLUGS
        assert client.zoom == TIIApiClient.DEFAULT_ZOOM
        assert client.timeout == TIIApiClient.DEFAULT_TIMEOUT

    def test_custom_initialization(self) -> None:
        """Client initializes with custom values."""
        bbox = BoundingBox(north=54.0, south=53.0, east=-5.0, west=-7.0)
        client = TIIApiClient(
            bounding_box=bbox,
            layer_slugs=["roadReports"],
            zoom=14,
            timeout=30,
        )
        assert client.bounding_box == bbox
        assert client.layer_slugs == ["roadReports"]
        assert client.zoom == 14
        assert client.timeout == 30

    @patch("data_handler.congestion_and_construction.tii_api_client.requests.post")
    def test_fetch_traffic_data_success(self, mock_post: MagicMock) -> None:
        """Successful API call returns parsed JSON."""
        expected_data = [{"data": {"mapFeaturesQuery": {"mapFeatures": []}}}]
        mock_response = MagicMock()
        mock_response.json.return_value = expected_data
        mock_response.raise_for_status.return_value = None
        mock_post.return_value = mock_response

        client = TIIApiClient()
        result = client.fetch_traffic_data()

        assert result == expected_data
        mock_post.assert_called_once()

    @patch("data_handler.congestion_and_construction.tii_api_client.requests.post")
    def test_fetch_traffic_data_timeout(self, mock_post: MagicMock) -> None:
        """Timeout raises requests.Timeout."""
        mock_post.side_effect = requests.Timeout("Connection timed out")

        client = TIIApiClient()
        with pytest.raises(requests.Timeout):
            client.fetch_traffic_data()

    @patch("data_handler.congestion_and_construction.tii_api_client.requests.post")
    def test_fetch_traffic_data_request_error(self, mock_post: MagicMock) -> None:
        """Request error raises requests.RequestException."""
        mock_post.side_effect = requests.RequestException("Connection failed")

        client = TIIApiClient()
        with pytest.raises(requests.RequestException):
            client.fetch_traffic_data()

    @patch("data_handler.congestion_and_construction.tii_api_client.requests.post")
    def test_fetch_traffic_data_http_error(self, mock_post: MagicMock) -> None:
        """HTTP error status raises on raise_for_status."""
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = requests.HTTPError("500 Server Error")
        mock_post.return_value = mock_response

        client = TIIApiClient()
        with pytest.raises(requests.HTTPError):
            client.fetch_traffic_data()

    def test_build_query_payload_structure(self) -> None:
        """Query payload has expected structure."""
        client = TIIApiClient()
        payload = client.build_query_payload()

        assert isinstance(payload, list)
        assert len(payload) == 1
        assert "query" in payload[0]
        assert "variables" in payload[0]

        variables_input = payload[0]["variables"]["input"]
        assert variables_input["north"] == DUBLIN_BOUNDING_BOX.north
        assert variables_input["south"] == DUBLIN_BOUNDING_BOX.south
        assert variables_input["east"] == DUBLIN_BOUNDING_BOX.east
        assert variables_input["west"] == DUBLIN_BOUNDING_BOX.west
        assert variables_input["zoom"] == TIIApiClient.DEFAULT_ZOOM
        assert variables_input["layerSlugs"] == TIIApiClient.DEFAULT_LAYER_SLUGS
