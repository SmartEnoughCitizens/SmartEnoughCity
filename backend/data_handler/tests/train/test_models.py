from datetime import date, datetime

import pytest
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from data_handler.train.models import (
    IrishRailCurrentTrain,
    IrishRailStation,
    IrishRailStationData,
    IrishRailTrainMovement,
    TrainAgency,
    TrainCalendarDate,
    TrainCalendarSchedule,
    TrainRoute,
    TrainStop,
    TrainStopTime,
    TrainTrip,
    TrainTripShape,
)

# ── GTFS Model Structure Tests ──────────────────────────────────────


class TestTrainAgencyModel:
    """Test TrainAgency model structure."""

    def test_has_required_fields(self) -> None:
        required = {"id", "name", "url", "timezone"}
        actual = set(TrainAgency.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_id(self) -> None:
        pk = [col.name for col in TrainAgency.__table__.primary_key]
        assert pk == ["id"]

    def test_can_create_instance(self, db_session: Session) -> None:
        agency = TrainAgency(
            id=1, name="Irish Rail",
            url="https://irishrail.ie", timezone="Europe/Dublin",
        )
        db_session.add(agency)
        db_session.commit()

        result = db_session.get(TrainAgency, 1)
        assert result is not None
        assert result.name == "Irish Rail"


class TestTrainCalendarScheduleModel:
    """Test TrainCalendarSchedule model structure and constraints."""

    def test_has_required_fields(self) -> None:
        required = {
            "entry_id", "service_id", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday", "start_date", "end_date",
        }
        actual = set(TrainCalendarSchedule.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_service_date_range(self) -> None:
        constraints = TrainCalendarSchedule.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_train_service_date_range" in unique_names

    def test_check_constraint_date_range(self) -> None:
        constraints = TrainCalendarSchedule.__table__.constraints
        check_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "chk_train_date_range" in check_names

    def test_can_create_instance(self, db_session: Session) -> None:
        schedule = TrainCalendarSchedule(
            service_id=200,
            monday=True, tuesday=True, wednesday=True,
            thursday=True, friday=True, saturday=False, sunday=False,
            start_date=date(2026, 1, 1),
            end_date=date(2026, 6, 30),
        )
        db_session.add(schedule)
        db_session.commit()

        result = db_session.query(TrainCalendarSchedule).filter_by(service_id=200).first()
        assert result is not None
        assert result.monday is True
        assert result.saturday is False

    def test_duplicate_service_date_range_raises_error(self, db_session: Session) -> None:
        s1 = TrainCalendarSchedule(
            service_id=300, monday=True, tuesday=False, wednesday=False,
            thursday=False, friday=False, saturday=False, sunday=False,
            start_date=date(2026, 1, 1), end_date=date(2026, 6, 30),
        )
        s2 = TrainCalendarSchedule(
            service_id=300, monday=False, tuesday=True, wednesday=False,
            thursday=False, friday=False, saturday=False, sunday=False,
            start_date=date(2026, 1, 1), end_date=date(2026, 6, 30),
        )
        db_session.add(s1)
        db_session.commit()
        db_session.add(s2)
        with pytest.raises(IntegrityError):
            db_session.commit()


class TestTrainCalendarDateModel:
    """Test TrainCalendarDate model."""

    def test_has_required_fields(self) -> None:
        required = {"entry_id", "service_id", "date", "exception_type"}
        actual = set(TrainCalendarDate.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_service_date(self) -> None:
        constraints = TrainCalendarDate.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_train_calendar_date" in unique_names


class TestTrainRouteModel:
    """Test TrainRoute model structure."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "agency_id", "short_name", "long_name",
            "route_type", "route_color", "route_text_color",
        }
        actual = set(TrainRoute.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_id(self) -> None:
        pk = [col.name for col in TrainRoute.__table__.primary_key]
        assert pk == ["id"]

    def test_can_create_with_agency_fk(self, db_session: Session) -> None:
        agency = TrainAgency(
            id=1, name="Irish Rail",
            url="https://irishrail.ie", timezone="Europe/Dublin",
        )
        db_session.add(agency)
        db_session.commit()

        route = TrainRoute(
            id="DART-NORTH", agency_id=1,
            short_name="DART", long_name="Dublin - Howth/Malahide",
            route_type=2, route_color="00A651", route_text_color="FFFFFF",
        )
        db_session.add(route)
        db_session.commit()

        result = db_session.get(TrainRoute, "DART-NORTH")
        assert result is not None
        assert result.agency_id == 1
        assert result.short_name == "DART"


class TestTrainStopModel:
    """Test TrainStop model structure."""

    def test_has_required_fields(self) -> None:
        required = {"id", "code", "name", "description", "lat", "lon"}
        actual = set(TrainStop.__table__.columns.keys())
        assert required.issubset(actual)

    def test_can_create_instance(self, db_session: Session) -> None:
        stop = TrainStop(
            id="CNLLY", code=9001, name="Connolly",
            description="Dublin Connolly Station",
            lat=53.352925, lon=-6.249463,
        )
        db_session.add(stop)
        db_session.commit()

        result = db_session.get(TrainStop, "CNLLY")
        assert result is not None
        assert result.name == "Connolly"
        assert result.description == "Dublin Connolly Station"


class TestTrainTripShapeModel:
    """Test TrainTripShape model."""

    def test_has_required_fields(self) -> None:
        required = {"entry_id", "shape_id", "pt_sequence", "pt_lat", "pt_lon", "dist_traveled"}
        actual = set(TrainTripShape.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_shape_sequence(self) -> None:
        constraints = TrainTripShape.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_train_shape_sequence" in unique_names

    def test_duplicate_shape_sequence_raises_error(self, db_session: Session) -> None:
        s1 = TrainTripShape(shape_id="SH1", pt_sequence=1, pt_lat=53.35, pt_lon=-6.25, dist_traveled=0.0)
        s2 = TrainTripShape(shape_id="SH1", pt_sequence=1, pt_lat=53.36, pt_lon=-6.26, dist_traveled=10.0)
        db_session.add(s1)
        db_session.commit()
        db_session.add(s2)
        with pytest.raises(IntegrityError):
            db_session.commit()


class TestTrainTripModel:
    """Test TrainTrip model structure."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "route_id", "service_id", "headsign",
            "short_name", "direction_id", "block_id", "shape_id",
        }
        actual = set(TrainTrip.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_id(self) -> None:
        pk = [col.name for col in TrainTrip.__table__.primary_key]
        assert pk == ["id"]


class TestTrainStopTimeModel:
    """Test TrainStopTime model structure."""

    def test_has_required_fields(self) -> None:
        required = {
            "entry_id", "trip_id", "stop_id", "arrival_time",
            "departure_time", "sequence", "headsign",
        }
        actual = set(TrainStopTime.__table__.columns.keys())
        assert required.issubset(actual)

    def test_has_performance_indexes(self) -> None:
        index_names = {idx.name for idx in TrainStopTime.__table__.indexes}
        assert "ix_train_stop_times_trip_id" in index_names
        assert "ix_train_stop_times_stop_id" in index_names
        assert "ix_train_stop_times_trip_stop" in index_names


# ── Irish Rail Realtime Model Tests ──────────────────────────────────


class TestIrishRailStationModel:
    """Test IrishRailStation model."""

    def test_has_required_fields(self) -> None:
        required = {
            "station_id", "station_code", "station_desc",
            "station_alias", "station_type", "lat", "lon",
        }
        actual = set(IrishRailStation.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_station_id(self) -> None:
        pk = [col.name for col in IrishRailStation.__table__.primary_key]
        assert pk == ["station_id"]

    def test_station_code_is_unique(self) -> None:
        columns = IrishRailStation.__table__.columns
        assert columns["station_code"].unique is True

    def test_can_create_instance(self, db_session: Session) -> None:
        station = IrishRailStation(
            station_id=100, station_code="CNLLY",
            station_desc="Connolly", station_alias=None,
            station_type="D", lat=53.352925, lon=-6.249463,
        )
        db_session.add(station)
        db_session.commit()

        result = db_session.get(IrishRailStation, 100)
        assert result is not None
        assert result.station_code == "CNLLY"
        assert result.station_type == "D"


class TestIrishRailCurrentTrainModel:
    """Test IrishRailCurrentTrain model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "train_code", "train_date", "train_status",
            "train_type", "direction", "lat", "lon",
            "public_message", "fetched_at",
        }
        actual = set(IrishRailCurrentTrain.__table__.columns.keys())
        assert required.issubset(actual)

    def test_can_create_instance(self, db_session: Session) -> None:
        now = datetime(2026, 1, 22, 10, 30, 0)
        train = IrishRailCurrentTrain(
            train_code="E109", train_date="22 Jan 2026",
            train_status="R", train_type="DART",
            direction="Northbound",
            lat=53.35, lon=-6.25,
            public_message=None, fetched_at=now,
        )
        db_session.add(train)
        db_session.commit()

        result = db_session.query(IrishRailCurrentTrain).first()
        assert result is not None
        assert result.train_code == "E109"
        assert result.train_status == "R"

    def test_lat_lon_can_be_null(self, db_session: Session) -> None:
        """Trains not yet running may not have coordinates."""
        now = datetime(2026, 1, 22, 10, 30, 0)
        train = IrishRailCurrentTrain(
            train_code="A123", train_date="22 Jan 2026",
            train_status="N", train_type="Intercity",
            direction=None, lat=None, lon=None,
            public_message=None, fetched_at=now,
        )
        db_session.add(train)
        db_session.commit()

        result = db_session.query(IrishRailCurrentTrain).first()
        assert result is not None
        assert result.lat is None
        assert result.lon is None


class TestIrishRailStationDataModel:
    """Test IrishRailStationData model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "station_code", "train_code", "train_date",
            "train_type", "origin", "destination",
            "origin_time", "destination_time", "status",
            "last_location", "due_in", "late",
            "exp_arrival", "exp_depart", "sch_arrival", "sch_depart",
            "direction", "location_type", "fetched_at",
        }
        actual = set(IrishRailStationData.__table__.columns.keys())
        assert required.issubset(actual)

    def test_can_create_with_station_fk(self, db_session: Session) -> None:
        station = IrishRailStation(
            station_id=100, station_code="CNLLY",
            station_desc="Connolly", station_alias=None,
            station_type="D", lat=53.352925, lon=-6.249463,
        )
        db_session.add(station)
        db_session.commit()

        now = datetime(2026, 1, 22, 10, 30, 0)
        data = IrishRailStationData(
            station_code="CNLLY", train_code="E109",
            train_date="22 Jan 2026", train_type="DART",
            origin="Greystones", destination="Howth",
            origin_time="09:30", destination_time="10:45",
            status="En Route", last_location="Departed Tara Street",
            due_in=5, late=2,
            exp_arrival="10:35", exp_depart="10:36",
            sch_arrival="10:33", sch_depart="10:34",
            direction="Northbound", location_type="S",
            fetched_at=now,
        )
        db_session.add(data)
        db_session.commit()

        result = db_session.query(IrishRailStationData).first()
        assert result is not None
        assert result.station_code == "CNLLY"
        assert result.due_in == 5
        assert result.late == 2

    def test_due_in_and_late_can_be_null(self, db_session: Session) -> None:
        station = IrishRailStation(
            station_id=100, station_code="CNLLY",
            station_desc="Connolly", station_alias=None,
            station_type="D", lat=53.352925, lon=-6.249463,
        )
        db_session.add(station)
        db_session.commit()

        now = datetime(2026, 1, 22, 10, 30, 0)
        data = IrishRailStationData(
            station_code="CNLLY", train_code="A200",
            train_date="22 Jan 2026", origin="Dublin Heuston",
            destination="Cork", due_in=None, late=None,
            fetched_at=now,
        )
        db_session.add(data)
        db_session.commit()

        result = db_session.query(IrishRailStationData).first()
        assert result is not None
        assert result.due_in is None
        assert result.late is None


class TestIrishRailTrainMovementModel:
    """Test IrishRailTrainMovement model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "train_code", "train_date",
            "location_code", "location_full_name", "location_order",
            "location_type", "train_origin", "train_destination",
            "scheduled_arrival", "scheduled_departure",
            "actual_arrival", "actual_departure",
            "auto_arrival", "auto_depart", "stop_type", "fetched_at",
        }
        actual = set(IrishRailTrainMovement.__table__.columns.keys())
        assert required.issubset(actual)

    def test_can_create_instance(self, db_session: Session) -> None:
        now = datetime(2026, 1, 22, 10, 30, 0)
        movement = IrishRailTrainMovement(
            train_code="E109", train_date="22 Jan 2026",
            location_code="CNLLY", location_full_name="Connolly",
            location_order=1, location_type="O",
            train_origin="Greystones", train_destination="Howth",
            scheduled_arrival=None, scheduled_departure="09:30:00",
            actual_arrival=None, actual_departure="09:31:00",
            auto_arrival=None, auto_depart=True,
            stop_type="C", fetched_at=now,
        )
        db_session.add(movement)
        db_session.commit()

        result = db_session.query(IrishRailTrainMovement).first()
        assert result is not None
        assert result.location_code == "CNLLY"
        assert result.location_type == "O"
        assert result.auto_depart is True
