from datetime import date

import pytest
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from data_handler.tram.models import (
    TramAgency,
    TramCalendarDate,
    TramCalendarSchedule,
    TramHourlyDistribution,
    TramLuasForecast,
    TramLuasStop,
    TramPassengerJourney,
    TramPassengerNumber,
    TramRoute,
    TramStop,
    TramStopTime,
    TramTrip,
    TramTripShape,
    TramWeeklyFlow,
)


# ── GTFS Model Structure Tests ──────────────────────────────────────


class TestTramAgencyModel:
    """Test TramAgency model structure."""

    def test_has_required_fields(self) -> None:
        required = {"id", "name", "url", "timezone"}
        actual = set(TramAgency.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_id(self) -> None:
        pk = [col.name for col in TramAgency.__table__.primary_key]
        assert pk == ["id"]

    def test_can_create_instance(self, db_session: Session) -> None:
        agency = TramAgency(id=1, name="Luas", url="https://luas.ie", timezone="Europe/Dublin")
        db_session.add(agency)
        db_session.commit()

        result = db_session.get(TramAgency, 1)
        assert result is not None
        assert result.name == "Luas"


class TestTramCalendarScheduleModel:
    """Test TramCalendarSchedule model structure and constraints."""

    def test_has_required_fields(self) -> None:
        required = {
            "entry_id", "service_id", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday", "sunday", "start_date", "end_date",
        }
        actual = set(TramCalendarSchedule.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_service_date_range(self) -> None:
        constraints = TramCalendarSchedule.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_service_date_range" in unique_names

    def test_check_constraint_date_range(self) -> None:
        constraints = TramCalendarSchedule.__table__.constraints
        check_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "chk_tram_date_range" in check_names

    def test_can_create_instance(self, db_session: Session) -> None:
        schedule = TramCalendarSchedule(
            service_id=100,
            monday=True, tuesday=True, wednesday=True,
            thursday=True, friday=True, saturday=False, sunday=False,
            start_date=date(2026, 1, 1),
            end_date=date(2026, 6, 30),
        )
        db_session.add(schedule)
        db_session.commit()

        result = db_session.query(TramCalendarSchedule).filter_by(service_id=100).first()
        assert result is not None
        assert result.monday is True
        assert result.saturday is False

    def test_duplicate_service_date_range_raises_error(self, db_session: Session) -> None:
        s1 = TramCalendarSchedule(
            service_id=200, monday=True, tuesday=False, wednesday=False,
            thursday=False, friday=False, saturday=False, sunday=False,
            start_date=date(2026, 1, 1), end_date=date(2026, 6, 30),
        )
        s2 = TramCalendarSchedule(
            service_id=200, monday=False, tuesday=True, wednesday=False,
            thursday=False, friday=False, saturday=False, sunday=False,
            start_date=date(2026, 1, 1), end_date=date(2026, 6, 30),
        )
        db_session.add(s1)
        db_session.commit()
        db_session.add(s2)
        with pytest.raises(IntegrityError):
            db_session.commit()


class TestTramCalendarDateModel:
    """Test TramCalendarDate model."""

    def test_has_required_fields(self) -> None:
        required = {"entry_id", "service_id", "date", "exception_type"}
        actual = set(TramCalendarDate.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_service_date(self) -> None:
        constraints = TramCalendarDate.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_calendar_date" in unique_names


class TestTramRouteModel:
    """Test TramRoute model structure."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "agency_id", "short_name", "long_name",
            "route_type", "route_color", "route_text_color",
        }
        actual = set(TramRoute.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_id(self) -> None:
        pk = [col.name for col in TramRoute.__table__.primary_key]
        assert pk == ["id"]

    def test_can_create_with_agency_fk(self, db_session: Session) -> None:
        agency = TramAgency(id=1, name="Luas", url="https://luas.ie", timezone="Europe/Dublin")
        db_session.add(agency)
        db_session.commit()

        route = TramRoute(
            id="GREEN", agency_id=1,
            short_name="Green", long_name="Luas Green Line",
            route_type=0, route_color="00FF00", route_text_color="FFFFFF",
        )
        db_session.add(route)
        db_session.commit()

        result = db_session.get(TramRoute, "GREEN")
        assert result is not None
        assert result.agency_id == 1
        assert result.short_name == "Green"


class TestTramStopModel:
    """Test TramStop model structure."""

    def test_has_required_fields(self) -> None:
        required = {"id", "code", "name", "description", "lat", "lon"}
        actual = set(TramStop.__table__.columns.keys())
        assert required.issubset(actual)

    def test_can_create_instance(self, db_session: Session) -> None:
        stop = TramStop(
            id="LUAS1", code=8001, name="St. Stephen's Green",
            lat=53.339428, lon=-6.261495,
        )
        db_session.add(stop)
        db_session.commit()

        result = db_session.get(TramStop, "LUAS1")
        assert result is not None
        assert result.name == "St. Stephen's Green"


class TestTramTripShapeModel:
    """Test TramTripShape model."""

    def test_has_required_fields(self) -> None:
        required = {"entry_id", "shape_id", "pt_sequence", "pt_lat", "pt_lon", "dist_traveled"}
        actual = set(TramTripShape.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_shape_sequence(self) -> None:
        constraints = TramTripShape.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_shape_sequence" in unique_names

    def test_duplicate_shape_sequence_raises_error(self, db_session: Session) -> None:
        s1 = TramTripShape(shape_id="SH1", pt_sequence=1, pt_lat=53.34, pt_lon=-6.26, dist_traveled=0.0)
        s2 = TramTripShape(shape_id="SH1", pt_sequence=1, pt_lat=53.35, pt_lon=-6.27, dist_traveled=10.0)
        db_session.add(s1)
        db_session.commit()
        db_session.add(s2)
        with pytest.raises(IntegrityError):
            db_session.commit()


class TestTramTripModel:
    """Test TramTrip model structure."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "route_id", "service_id", "headsign",
            "short_name", "direction_id", "block_id", "shape_id",
        }
        actual = set(TramTrip.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_id(self) -> None:
        pk = [col.name for col in TramTrip.__table__.primary_key]
        assert pk == ["id"]


class TestTramStopTimeModel:
    """Test TramStopTime model structure."""

    def test_has_required_fields(self) -> None:
        required = {
            "entry_id", "trip_id", "stop_id", "arrival_time",
            "departure_time", "sequence", "headsign",
        }
        actual = set(TramStopTime.__table__.columns.keys())
        assert required.issubset(actual)

    def test_has_performance_indexes(self) -> None:
        index_names = {idx.name for idx in TramStopTime.__table__.indexes}
        assert "ix_tram_stop_times_trip_id" in index_names
        assert "ix_tram_stop_times_stop_id" in index_names
        assert "ix_tram_stop_times_trip_stop" in index_names


# ── CSO Dataset Model Tests ─────────────────────────────────────────


class TestTramPassengerJourneyModel:
    """Test TramPassengerJourney model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "statistic", "statistic_label", "week_code",
            "week_label", "line_code", "line_label", "unit", "value",
        }
        actual = set(TramPassengerJourney.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_week_line(self) -> None:
        constraints = TramPassengerJourney.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_pj_week_line" in unique_names

    def test_can_create_instance(self, db_session: Session) -> None:
        pj = TramPassengerJourney(
            statistic="TII03", statistic_label="Passenger Journeys by Luas",
            week_code="2024W01", week_label="2024 Week 01",
            line_code="10", line_label="Green Line",
            unit="Number", value=125000,
        )
        db_session.add(pj)
        db_session.commit()

        result = db_session.query(TramPassengerJourney).first()
        assert result is not None
        assert result.value == 125000


class TestTramPassengerNumberModel:
    """Test TramPassengerNumber model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "statistic", "statistic_label", "year",
            "month_code", "month_label", "unit", "value",
        }
        actual = set(TramPassengerNumber.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_year_month_stat(self) -> None:
        constraints = TramPassengerNumber.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_pn_year_month_stat" in unique_names


class TestTramHourlyDistributionModel:
    """Test TramHourlyDistribution model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "statistic", "statistic_label", "year",
            "line_code", "line_label", "time_code", "time_label",
            "unit", "value",
        }
        actual = set(TramHourlyDistribution.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_year_line_time(self) -> None:
        constraints = TramHourlyDistribution.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_hd_year_line_time" in unique_names


class TestTramWeeklyFlowModel:
    """Test TramWeeklyFlow model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "statistic", "statistic_label", "year",
            "day_code", "day_label", "unit", "value",
        }
        actual = set(TramWeeklyFlow.__table__.columns.keys())
        assert required.issubset(actual)

    def test_unique_constraint_year_day_stat(self) -> None:
        constraints = TramWeeklyFlow.__table__.constraints
        unique_names = {
            c.name for c in constraints
            if hasattr(c, "name") and c.name is not None
        }
        assert "uq_tram_wf_year_day_stat" in unique_names


# ── Luas Forecast Model Tests ───────────────────────────────────────


class TestTramLuasStopModel:
    """Test TramLuasStop model."""

    def test_has_required_fields(self) -> None:
        required = {
            "stop_id", "line", "name", "pronunciation",
            "park_ride", "cycle_ride", "lat", "lon",
        }
        actual = set(TramLuasStop.__table__.columns.keys())
        assert required.issubset(actual)

    def test_primary_key_is_stop_id(self) -> None:
        pk = [col.name for col in TramLuasStop.__table__.primary_key]
        assert pk == ["stop_id"]

    def test_can_create_instance(self, db_session: Session) -> None:
        stop = TramLuasStop(
            stop_id="STG", line="green", name="St. Stephen's Green",
            pronunciation="Saint Stephens Green",
            park_ride=False, cycle_ride=True,
            lat=53.339428, lon=-6.261495,
        )
        db_session.add(stop)
        db_session.commit()

        result = db_session.get(TramLuasStop, "STG")
        assert result is not None
        assert result.name == "St. Stephen's Green"
        assert result.cycle_ride is True


class TestTramLuasForecastModel:
    """Test TramLuasForecast model."""

    def test_has_required_fields(self) -> None:
        required = {
            "id", "stop_id", "line", "direction",
            "destination", "due_mins", "message",
        }
        actual = set(TramLuasForecast.__table__.columns.keys())
        assert required.issubset(actual)

    def test_can_create_with_stop_fk(self, db_session: Session) -> None:
        stop = TramLuasStop(
            stop_id="STG", line="green", name="St. Stephen's Green",
            pronunciation="", park_ride=False, cycle_ride=False,
            lat=53.339428, lon=-6.261495,
        )
        db_session.add(stop)
        db_session.commit()

        forecast = TramLuasForecast(
            stop_id="STG", line="green", direction="Inbound",
            destination="Broombridge", due_mins=3, message="",
        )
        db_session.add(forecast)
        db_session.commit()

        result = db_session.query(TramLuasForecast).first()
        assert result is not None
        assert result.due_mins == 3
        assert result.stop_id == "STG"

    def test_due_mins_can_be_null(self, db_session: Session) -> None:
        """due_mins is None when tram displays 'DUE' or 'No Service'."""
        stop = TramLuasStop(
            stop_id="STG", line="green", name="St. Stephen's Green",
            pronunciation="", park_ride=False, cycle_ride=False,
            lat=53.339428, lon=-6.261495,
        )
        db_session.add(stop)
        db_session.commit()

        forecast = TramLuasForecast(
            stop_id="STG", line="green", direction="Inbound",
            destination="Broombridge", due_mins=None, message="No Service",
        )
        db_session.add(forecast)
        db_session.commit()

        result = db_session.query(TramLuasForecast).first()
        assert result is not None
        assert result.due_mins is None
        assert result.message == "No Service"
