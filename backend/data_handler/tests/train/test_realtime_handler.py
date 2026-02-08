from datetime import datetime
from unittest.mock import Mock, patch

import pytest
from sqlalchemy.orm import Session

from data_handler.train.realtime_handler import (
    _ensure_list,
    _safe_float,
    _safe_int,
    fetch_all_stations,
    fetch_current_trains,
    fetch_station_data,
    fetch_train_movements,
    irish_rail_current_trains_to_db,
    irish_rail_station_data_to_db,
    irish_rail_stations_to_db,
    irish_rail_train_movements_to_db,
)
from data_handler.train.models import (
    IrishRailCurrentTrain,
    IrishRailStation,
    IrishRailStationData,
    IrishRailTrainMovement,
)
from tests.utils import assert_row_count


# ── Sample XML responses ─────────────────────────────────────────────

STATIONS_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<ArrayOfObjStation>
  <objStation>
    <StationDesc>Connolly</StationDesc>
    <StationCode>CNLLY</StationCode>
    <StationId>100</StationId>
    <StationAlias></StationAlias>
    <StationLatitude>53.352925</StationLatitude>
    <StationLongitude>-6.249463</StationLongitude>
  </objStation>
  <objStation>
    <StationDesc>Tara Street</StationDesc>
    <StationCode>TARA</StationCode>
    <StationId>101</StationId>
    <StationAlias></StationAlias>
    <StationLatitude>53.347778</StationLatitude>
    <StationLongitude>-6.254444</StationLongitude>
  </objStation>
</ArrayOfObjStation>
"""

STATIONS_DART_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<ArrayOfObjStation>
  <objStation>
    <StationDesc>Connolly</StationDesc>
    <StationCode>CNLLY</StationCode>
    <StationId>100</StationId>
    <StationAlias></StationAlias>
    <StationLatitude>53.352925</StationLatitude>
    <StationLongitude>-6.249463</StationLongitude>
  </objStation>
</ArrayOfObjStation>
"""

CURRENT_TRAINS_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<ArrayOfObjTrainPositions>
  <objTrainPositions>
    <TrainCode>E109</TrainCode>
    <TrainDate>22 Jan 2026</TrainDate>
    <TrainStatus>R</TrainStatus>
    <TrainType>DART</TrainType>
    <Direction>Northbound</Direction>
    <TrainLatitude>53.352</TrainLatitude>
    <TrainLongitude>-6.249</TrainLongitude>
    <PublicMessage>E109\n09:30 - Greystones to Howth (1 mins late)</PublicMessage>
  </objTrainPositions>
  <objTrainPositions>
    <TrainCode>A200</TrainCode>
    <TrainDate>22 Jan 2026</TrainDate>
    <TrainStatus>N</TrainStatus>
    <TrainType>Intercity</TrainType>
    <Direction></Direction>
    <TrainLatitude></TrainLatitude>
    <TrainLongitude></TrainLongitude>
    <PublicMessage></PublicMessage>
  </objTrainPositions>
</ArrayOfObjTrainPositions>
"""

STATION_DATA_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<ArrayOfObjStationData>
  <objStationData>
    <Traincode>E109</Traincode>
    <Traindate>22 Jan 2026</Traindate>
    <Traintype>DART</Traintype>
    <Origin>Greystones</Origin>
    <Destination>Howth</Destination>
    <Origintime>09:30</Origintime>
    <Destinationtime>10:45</Destinationtime>
    <Status>En Route</Status>
    <Lastlocation>Departed Tara Street</Lastlocation>
    <Duein>5</Duein>
    <Late>1</Late>
    <Exparrival>10:35</Exparrival>
    <Expdepart>10:36</Expdepart>
    <Scharrival>10:33</Scharrival>
    <Schdepart>10:34</Schdepart>
    <Direction>Northbound</Direction>
    <Locationtype>S</Locationtype>
  </objStationData>
</ArrayOfObjStationData>
"""

STATION_DATA_EMPTY_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<ArrayOfObjStationData />
"""

TRAIN_MOVEMENTS_XML_RESPONSE = """\
<?xml version="1.0" encoding="utf-8"?>
<ArrayOfObjTrainMovements>
  <objTrainMovements>
    <TrainCode>E109</TrainCode>
    <TrainDate>22 Jan 2026</TrainDate>
    <LocationCode>GSTON</LocationCode>
    <LocationFullName>Greystones</LocationFullName>
    <LocationOrder>1</LocationOrder>
    <LocationType>O</LocationType>
    <TrainOrigin>Greystones</TrainOrigin>
    <TrainDestination>Howth</TrainDestination>
    <ScheduledArrival></ScheduledArrival>
    <ScheduledDeparture>09:30:00</ScheduledDeparture>
    <Arrival></Arrival>
    <Departure>09:31:00</Departure>
    <AutoArrival>0</AutoArrival>
    <AutoDepart>1</AutoDepart>
    <StopType>C</StopType>
  </objTrainMovements>
  <objTrainMovements>
    <TrainCode>E109</TrainCode>
    <TrainDate>22 Jan 2026</TrainDate>
    <LocationCode>CNLLY</LocationCode>
    <LocationFullName>Connolly</LocationFullName>
    <LocationOrder>5</LocationOrder>
    <LocationType>S</LocationType>
    <TrainOrigin>Greystones</TrainOrigin>
    <TrainDestination>Howth</TrainDestination>
    <ScheduledArrival>10:00:00</ScheduledArrival>
    <ScheduledDeparture>10:01:00</ScheduledDeparture>
    <Arrival></Arrival>
    <Departure></Departure>
    <AutoArrival>0</AutoArrival>
    <AutoDepart>0</AutoDepart>
    <StopType>N</StopType>
  </objTrainMovements>
</ArrayOfObjTrainMovements>
"""


# ── Helper function unit tests ───────────────────────────────────────


class TestSafeInt:
    """Test _safe_int helper."""

    def test_valid_int_string(self) -> None:
        assert _safe_int("42") == 42

    def test_negative_int(self) -> None:
        assert _safe_int("-3") == -3

    def test_none_returns_none(self) -> None:
        assert _safe_int(None) is None

    def test_empty_string_returns_none(self) -> None:
        assert _safe_int("") is None

    def test_whitespace_returns_none(self) -> None:
        assert _safe_int("  ") is None

    def test_non_numeric_returns_none(self) -> None:
        assert _safe_int("abc") is None


class TestSafeFloat:
    """Test _safe_float helper."""

    def test_valid_float_string(self) -> None:
        assert _safe_float("53.352") == 53.352

    def test_negative_float(self) -> None:
        assert _safe_float("-6.249") == -6.249

    def test_none_returns_none(self) -> None:
        assert _safe_float(None) is None

    def test_empty_string_returns_none(self) -> None:
        assert _safe_float("") is None

    def test_non_numeric_returns_none(self) -> None:
        assert _safe_float("abc") is None


class TestEnsureList:
    """Test _ensure_list helper."""

    def test_none_returns_empty_list(self) -> None:
        assert _ensure_list(None) == []

    def test_dict_returns_single_item_list(self) -> None:
        result = _ensure_list({"key": "value"})
        assert result == [{"key": "value"}]

    def test_list_returns_same_list(self) -> None:
        data = [{"a": 1}, {"b": 2}]
        assert _ensure_list(data) == data


# ── fetch_all_stations unit tests ────────────────────────────────────


class TestFetchAllStations:
    """Test parsing of Irish Rail stations XML."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_fetch_returns_station_list(self, mock_get: Mock) -> None:
        mock_response = Mock()
        mock_response.text = STATIONS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        stations = fetch_all_stations("A")

        assert len(stations) == 2
        assert stations[0]["StationCode"] == "CNLLY"
        assert stations[0]["StationDesc"] == "Connolly"
        assert stations[1]["StationCode"] == "TARA"

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_http_error_returns_empty_list(self, mock_get: Mock) -> None:
        import requests
        mock_get.side_effect = requests.HTTPError("503")

        stations = fetch_all_stations("A")

        assert stations == []


# ── fetch_current_trains unit tests ──────────────────────────────────


class TestFetchCurrentTrains:
    """Test parsing of Irish Rail current trains XML."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_fetch_returns_train_list(self, mock_get: Mock) -> None:
        mock_response = Mock()
        mock_response.text = CURRENT_TRAINS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        trains = fetch_current_trains("A")

        assert len(trains) == 2
        assert trains[0]["TrainCode"] == "E109"
        assert trains[0]["TrainStatus"] == "R"
        assert trains[1]["TrainCode"] == "A200"
        assert trains[1]["TrainStatus"] == "N"

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_http_error_returns_empty_list(self, mock_get: Mock) -> None:
        import requests
        mock_get.side_effect = requests.ConnectionError("timeout")

        trains = fetch_current_trains("A")

        assert trains == []


# ── fetch_station_data unit tests ────────────────────────────────────


class TestFetchStationData:
    """Test parsing of station arrival/departure data XML."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_fetch_returns_arrival_data(self, mock_get: Mock) -> None:
        mock_response = Mock()
        mock_response.text = STATION_DATA_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        data = fetch_station_data("CNLLY", num_mins=90)

        assert len(data) == 1
        assert data[0]["Traincode"] == "E109"
        assert data[0]["Origin"] == "Greystones"
        assert data[0]["Destination"] == "Howth"
        assert data[0]["Duein"] == "5"

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_empty_station_returns_empty_list(self, mock_get: Mock) -> None:
        mock_response = Mock()
        mock_response.text = STATION_DATA_EMPTY_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        data = fetch_station_data("BYSDE", num_mins=90)

        assert data == []


# ── fetch_train_movements unit tests ─────────────────────────────────


class TestFetchTrainMovements:
    """Test parsing of train movements XML."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_fetch_returns_movements(self, mock_get: Mock) -> None:
        mock_response = Mock()
        mock_response.text = TRAIN_MOVEMENTS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        movements = fetch_train_movements("E109", "22 Jan 2026")

        assert len(movements) == 2
        assert movements[0]["LocationCode"] == "GSTON"
        assert movements[0]["LocationType"] == "O"
        assert movements[1]["LocationCode"] == "CNLLY"
        assert movements[1]["LocationOrder"] == "5"


# ── irish_rail_stations_to_db integration tests ──────────────────────


class TestIrishRailStationsToDb:
    """Integration tests for irish_rail_stations_to_db."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_inserts_stations(self, mock_get: Mock, db_session: Session) -> None:
        mock_response = Mock()
        mock_response.text = STATIONS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        assert_row_count(db_session, "irish_rail_stations", 0)

        irish_rail_stations_to_db()

        assert_row_count(db_session, "irish_rail_stations", 2)

        cnlly = db_session.query(IrishRailStation).filter_by(station_code="CNLLY").first()
        assert cnlly is not None
        assert cnlly.station_desc == "Connolly"
        assert cnlly.station_id == 100

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_upserts_existing_stations(self, mock_get: Mock, db_session: Session) -> None:
        mock_response = Mock()
        mock_response.text = STATIONS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        irish_rail_stations_to_db()
        assert_row_count(db_session, "irish_rail_stations", 2)

        # Run again — should still be 2 (upsert)
        irish_rail_stations_to_db()
        assert_row_count(db_session, "irish_rail_stations", 2)

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_empty_api_response_does_not_insert(self, mock_get: Mock, db_session: Session) -> None:
        mock_response = Mock()
        mock_response.text = '<?xml version="1.0"?><ArrayOfObjStation />'
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        irish_rail_stations_to_db()

        assert_row_count(db_session, "irish_rail_stations", 0)


# ── irish_rail_current_trains_to_db integration tests ────────────────


class TestIrishRailCurrentTrainsToDb:
    """Integration tests for irish_rail_current_trains_to_db."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_inserts_current_trains(self, mock_get: Mock, db_session: Session) -> None:
        mock_response = Mock()
        mock_response.text = CURRENT_TRAINS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        assert_row_count(db_session, "irish_rail_current_trains", 0)

        irish_rail_current_trains_to_db()

        assert_row_count(db_session, "irish_rail_current_trains", 2)

        trains = db_session.query(IrishRailCurrentTrain).all()
        codes = [t.train_code for t in trains]
        assert "E109" in codes
        assert "A200" in codes

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_clears_old_trains_before_inserting(self, mock_get: Mock, db_session: Session) -> None:
        mock_response = Mock()
        mock_response.text = CURRENT_TRAINS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        irish_rail_current_trains_to_db()
        assert_row_count(db_session, "irish_rail_current_trains", 2)

        irish_rail_current_trains_to_db()
        assert_row_count(db_session, "irish_rail_current_trains", 2)

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_running_train_has_coordinates(self, mock_get: Mock, db_session: Session) -> None:
        mock_response = Mock()
        mock_response.text = CURRENT_TRAINS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        irish_rail_current_trains_to_db()

        running = db_session.query(IrishRailCurrentTrain).filter_by(train_code="E109").first()
        assert running is not None
        assert running.lat == 53.352
        assert running.lon == -6.249

        not_running = db_session.query(IrishRailCurrentTrain).filter_by(train_code="A200").first()
        assert not_running is not None
        assert not_running.lat is None
        assert not_running.lon is None


# ── irish_rail_station_data_to_db integration tests ──────────────────


class TestIrishRailStationDataToDb:
    """Integration tests for irish_rail_station_data_to_db."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_inserts_station_data_for_known_stations(
        self, mock_get: Mock, db_session: Session,
    ) -> None:
        """Station data is fetched and inserted for every station in DB."""
        # Insert station first
        db_session.add(IrishRailStation(
            station_id=100, station_code="CNLLY",
            station_desc="Connolly", lat=53.352925, lon=-6.249463,
        ))
        db_session.commit()

        def side_effect_get(url: str, **kwargs: object) -> Mock:
            resp = Mock()
            resp.raise_for_status = Mock()
            if "getStationDataByCodeXML" in url:
                resp.text = STATION_DATA_XML_RESPONSE
            else:
                resp.text = '<?xml version="1.0"?><ArrayOfObjStation />'
            return resp

        mock_get.side_effect = side_effect_get

        assert_row_count(db_session, "irish_rail_station_data", 0)

        irish_rail_station_data_to_db()

        assert_row_count(db_session, "irish_rail_station_data", 1)

        result = db_session.query(IrishRailStationData).first()
        assert result is not None
        assert result.train_code == "E109"
        assert result.due_in == 5
        assert result.late == 1

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_no_stations_returns_early(self, mock_get: Mock, db_session: Session) -> None:
        """When no stations exist in DB, function returns without calling API."""
        irish_rail_station_data_to_db()

        # Only the initial query to get station codes, no API calls for data
        assert_row_count(db_session, "irish_rail_station_data", 0)

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_clears_old_data_before_inserting(
        self, mock_get: Mock, db_session: Session,
    ) -> None:
        db_session.add(IrishRailStation(
            station_id=100, station_code="CNLLY",
            station_desc="Connolly", lat=53.352925, lon=-6.249463,
        ))
        db_session.commit()

        def side_effect_get(url: str, **kwargs: object) -> Mock:
            resp = Mock()
            resp.raise_for_status = Mock()
            if "getStationDataByCodeXML" in url:
                resp.text = STATION_DATA_XML_RESPONSE
            else:
                resp.text = '<?xml version="1.0"?><ArrayOfObjStation />'
            return resp

        mock_get.side_effect = side_effect_get

        irish_rail_station_data_to_db()
        assert_row_count(db_session, "irish_rail_station_data", 1)

        irish_rail_station_data_to_db()
        assert_row_count(db_session, "irish_rail_station_data", 1)


# ── irish_rail_train_movements_to_db integration tests ───────────────


class TestIrishRailTrainMovementsToDb:
    """Integration tests for irish_rail_train_movements_to_db."""

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_inserts_movements_for_current_trains(
        self, mock_get: Mock, db_session: Session,
    ) -> None:
        # Insert a current train first
        db_session.add(IrishRailCurrentTrain(
            train_code="E109", train_date="22 Jan 2026",
            train_status="R", train_type="DART",
            direction="Northbound", lat=53.352, lon=-6.249,
            fetched_at=datetime(2026, 1, 22, 10, 0, 0),
        ))
        db_session.commit()

        mock_response = Mock()
        mock_response.text = TRAIN_MOVEMENTS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        assert_row_count(db_session, "irish_rail_train_movements", 0)

        irish_rail_train_movements_to_db()

        assert_row_count(db_session, "irish_rail_train_movements", 2)

        movements = db_session.query(IrishRailTrainMovement).all()
        codes = [m.location_code for m in movements]
        assert "GSTON" in codes
        assert "CNLLY" in codes

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_no_trains_returns_early(self, mock_get: Mock, db_session: Session) -> None:
        irish_rail_train_movements_to_db()

        assert_row_count(db_session, "irish_rail_train_movements", 0)

    @patch("data_handler.train.realtime_handler.requests.get")
    def test_clears_old_movements_before_inserting(
        self, mock_get: Mock, db_session: Session,
    ) -> None:
        db_session.add(IrishRailCurrentTrain(
            train_code="E109", train_date="22 Jan 2026",
            train_status="R", train_type="DART",
            direction="Northbound", lat=53.352, lon=-6.249,
            fetched_at=datetime(2026, 1, 22, 10, 0, 0),
        ))
        db_session.commit()

        mock_response = Mock()
        mock_response.text = TRAIN_MOVEMENTS_XML_RESPONSE
        mock_response.raise_for_status = Mock()
        mock_get.return_value = mock_response

        irish_rail_train_movements_to_db()
        assert_row_count(db_session, "irish_rail_train_movements", 2)

        irish_rail_train_movements_to_db()
        assert_row_count(db_session, "irish_rail_train_movements", 2)
