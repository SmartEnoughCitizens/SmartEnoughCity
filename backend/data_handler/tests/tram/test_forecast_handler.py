from unittest.mock import Mock, patch

import pytest
import requests
from sqlalchemy.orm import Session

from data_handler.tram.forecast_handler import (
    fetch_forecast_for_stop,
    fetch_luas_stops,
    luas_forecasts_to_db,
    luas_stops_to_db,
)
from data_handler.tram.models import TramLuasStop
from tests.utils import assert_row_count

# ── Sample XML responses ─────────────────────────────────────────────

STOPS_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<stops>
  <line name="Luas Green Line">
    <stop abrev="STG" isParkRide="0" isCycleRide="1" lat="53.339428" long="-6.261495" pronunciation="Saint Stephens Green">St. Stephen's Green</stop>
    <stop abrev="HAR" isParkRide="0" isCycleRide="0" lat="53.333333" long="-6.262222" pronunciation="Harcourt">Harcourt</stop>
  </line>
  <line name="Luas Red Line">
    <stop abrev="TPT" isParkRide="0" isCycleRide="0" lat="53.348056" long="-6.229167" pronunciation="The Point">The Point</stop>
  </line>
</stops>
"""

FORECAST_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<stopInfo stop="STG" stopAbv="STG">
  <message>Green Line services operating normally</message>
  <direction name="Inbound">
    <tram destination="Broombridge" dueMins="3" />
    <tram destination="Broombridge" dueMins="12" />
  </direction>
  <direction name="Outbound">
    <tram destination="Bride's Glen" dueMins="DUE" />
  </direction>
</stopInfo>
"""

FORECAST_EMPTY_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<stopInfo stop="HAR" stopAbv="HAR">
  <message>No trams forecast</message>
</stopInfo>
"""

STOPS_XML_SINGLE_LINE_SINGLE_STOP = """\
<?xml version="1.0" encoding="utf-8"?>
<stops>
  <line name="Luas Red Line">
    <stop abrev="TPT" isParkRide="0" isCycleRide="0" lat="53.348056" long="-6.229167" pronunciation="The Point">The Point</stop>
  </line>
</stops>
"""


# ── fetch_luas_stops unit tests ──────────────────────────────────────


class TestFetchLuasStops:
    """Test parsing of Luas stops XML API response."""

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_fetch_green_line_stops(self, mock_get: Mock) -> None:
        """Parsing green line returns correct stops."""
        mock_response = Mock()
        mock_response.text = STOPS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        df = fetch_luas_stops("green")

        assert len(df) == 2
        assert list(df["stop_id"]) == ["STG", "HAR"]
        assert list(df["line"]) == ["green", "green"]
        assert df.iloc[0]["name"] == "St. Stephen's Green"
        assert df.iloc[0]["lat"] == 53.339428
        assert df.iloc[0]["lon"] == -6.261495
        assert not df.iloc[0]["park_ride"]
        assert df.iloc[0]["cycle_ride"]

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_fetch_red_line_stops(self, mock_get: Mock) -> None:
        """Parsing red line returns only red stops."""
        mock_response = Mock()
        mock_response.text = STOPS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        df = fetch_luas_stops("red")

        assert len(df) == 1
        assert df.iloc[0]["stop_id"] == "TPT"
        assert df.iloc[0]["name"] == "The Point"

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_fetch_stops_empty_line_returns_empty_df(self, mock_get: Mock) -> None:
        """Requesting a line with no matching stops returns empty DataFrame."""
        mock_response = Mock()
        mock_response.text = STOPS_XML_SINGLE_LINE_SINGLE_STOP
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        df = fetch_luas_stops("green")

        assert len(df) == 0


# ── fetch_forecast_for_stop unit tests ───────────────────────────────


class TestFetchForecastForStop:
    """Test parsing of Luas forecast XML API response."""

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_parse_forecast_with_multiple_directions(self, mock_get: Mock) -> None:
        """Forecast with inbound and outbound trams parsed correctly."""
        mock_response = Mock()
        mock_response.text = FORECAST_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        entries = fetch_forecast_for_stop("STG")

        assert len(entries) == 3

        # Inbound trams
        assert entries[0]["direction"] == "Inbound"
        assert entries[0]["destination"] == "Broombridge"
        assert entries[0]["due_mins"] == 3

        assert entries[1]["direction"] == "Inbound"
        assert entries[1]["due_mins"] == 12

        # Outbound tram with "DUE" (non-numeric → None)
        assert entries[2]["direction"] == "Outbound"
        assert entries[2]["destination"] == "Bride's Glen"
        assert entries[2]["due_mins"] is None

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_parse_forecast_message_preserved(self, mock_get: Mock) -> None:
        """Message from stopInfo is attached to each forecast entry."""
        mock_response = Mock()
        mock_response.text = FORECAST_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        entries = fetch_forecast_for_stop("STG")

        for entry in entries:
            assert entry["message"] == "Green Line services operating normally"

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_empty_forecast_returns_empty_list(self, mock_get: Mock) -> None:
        """Stop with no forecasts returns empty list."""
        mock_response = Mock()
        mock_response.text = FORECAST_EMPTY_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        entries = fetch_forecast_for_stop("HAR")

        assert entries == []

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_http_error_raises_exception(self, mock_get: Mock) -> None:
        """HTTP error is propagated."""

        mock_get.side_effect = requests.HTTPError("503 Service Unavailable")

        with pytest.raises(requests.HTTPError):
            fetch_forecast_for_stop("STG")


# ── luas_stops_to_db integration tests ───────────────────────────────


class TestLuasStopsToDb:
    """Integration tests for luas_stops_to_db (mocked API, real DB)."""

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_inserts_stops_for_both_lines(
        self,
        mock_get: Mock,
        db_session: Session,
    ) -> None:
        """Fetching stops for red and green inserts all into DB."""
        mock_response = Mock()
        mock_response.text = STOPS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        assert_row_count(db_session, "tram_luas_stops", 0)

        luas_stops_to_db()

        assert_row_count(db_session, "tram_luas_stops", 3)

        stg = db_session.get(TramLuasStop, "STG")
        assert stg is not None
        assert stg.name == "St. Stephen's Green"
        assert stg.line == "green"
        assert stg.cycle_ride is True

        tpt = db_session.get(TramLuasStop, "TPT")
        assert tpt is not None
        assert tpt.name == "The Point"
        assert tpt.line == "red"

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_upserts_existing_stops(
        self,
        mock_get: Mock,
        db_session: Session,
    ) -> None:
        """Running twice updates existing stops rather than duplicating."""
        mock_response = Mock()
        mock_response.text = STOPS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        luas_stops_to_db()
        assert_row_count(db_session, "tram_luas_stops", 3)

        # Run again — should still be 3 (upsert)
        luas_stops_to_db()
        assert_row_count(db_session, "tram_luas_stops", 3)


# ── luas_forecasts_to_db integration tests ───────────────────────────


class TestLuasForecastsToDb:
    """Integration tests for luas_forecasts_to_db (mocked API, real DB)."""

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_inserts_forecasts_for_all_stops(
        self,
        mock_get: Mock,
        db_session: Session,
    ) -> None:
        """Forecasts are fetched and inserted for every known stop."""
        # First, insert stops
        db_session.add(
            TramLuasStop(
                stop_id="STG",
                line="green",
                name="St. Stephen's Green",
                pronunciation="",
                park_ride=False,
                cycle_ride=True,
                lat=53.339428,
                lon=-6.261495,
            )
        )
        db_session.add(
            TramLuasStop(
                stop_id="HAR",
                line="green",
                name="Harcourt",
                pronunciation="",
                park_ride=False,
                cycle_ride=False,
                lat=53.333333,
                lon=-6.262222,
            )
        )
        db_session.commit()

        # Mock different responses per URL
        def side_effect_get(url: str, **kwargs: object) -> Mock:
            resp = Mock()
            resp.raise_for_status = Mock()
            if "stop=STG" in url:
                resp.text = FORECAST_XML_RESPONSE
            else:
                resp.text = FORECAST_EMPTY_XML_RESPONSE
            return resp

        mock_get.side_effect = side_effect_get

        assert_row_count(db_session, "tram_luas_forecasts", 0)

        luas_forecasts_to_db()

        # STG has 3 forecast entries, HAR has 0
        assert_row_count(db_session, "tram_luas_forecasts", 3)

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_clears_old_forecasts_before_inserting(
        self,
        mock_get: Mock,
        db_session: Session,
    ) -> None:
        """Old forecasts are deleted before fresh ones are inserted."""
        # Insert stop
        db_session.add(
            TramLuasStop(
                stop_id="STG",
                line="green",
                name="St. Stephen's Green",
                pronunciation="",
                park_ride=False,
                cycle_ride=True,
                lat=53.339428,
                lon=-6.261495,
            )
        )
        db_session.commit()

        mock_response = Mock()
        mock_response.text = FORECAST_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        # Insert forecasts twice
        luas_forecasts_to_db()
        assert_row_count(db_session, "tram_luas_forecasts", 3)

        luas_forecasts_to_db()
        # Should still be 3 — old ones deleted, new ones inserted
        assert_row_count(db_session, "tram_luas_forecasts", 3)

    @patch("data_handler.tram.forecast_handler.requests.get")
    def test_no_stops_in_db_returns_early(
        self,
        mock_get: Mock,
        db_session: Session,
    ) -> None:
        """When no stops exist in DB, function returns without calling API."""
        luas_forecasts_to_db()

        mock_get.assert_not_called()
        assert_row_count(db_session, "tram_luas_forecasts", 0)
