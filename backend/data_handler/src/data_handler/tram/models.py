from datetime import date, time
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    Double,
    Float,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    Time,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


# ── GTFS Static Models ─────────────────────────────────────────────


class TramAgency(Base):
    __tablename__ = "tram_agencies"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    url: Mapped[str] = mapped_column(String, nullable=False)
    timezone: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    routes: Mapped[list["TramRoute"]] = relationship(back_populates="agency")


class TramCalendarSchedule(Base):
    __tablename__ = "tram_calendar_schedule"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "service_id", "start_date", "end_date", name="uq_tram_service_date_range"
        ),
        CheckConstraint("end_date >= start_date", name="chk_tram_date_range"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    service_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    monday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    tuesday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    wednesday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    thursday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    friday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    saturday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    sunday: Mapped[bool] = mapped_column(Boolean, nullable=False)
    start_date: Mapped[date] = mapped_column(Date, nullable=False)
    end_date: Mapped[date] = mapped_column(Date, nullable=False)

    # Relationships
    trips: Mapped[list["TramTrip"]] = relationship(
        primaryjoin="TramCalendarSchedule.service_id == foreign(TramTrip.service_id)",
        back_populates="service",
        viewonly=True,
    )


class TramCalendarDate(Base):
    __tablename__ = "tram_calendar_dates"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint("service_id", "date", name="uq_tram_calendar_date"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    service_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    date: Mapped[date] = mapped_column(Date, nullable=False)
    exception_type: Mapped[int] = mapped_column(Integer, nullable=False)


class TramRoute(Base):
    __tablename__ = "tram_routes"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[str] = mapped_column(String, primary_key=True)
    agency_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.tram_agencies.id"), nullable=False
    )
    short_name: Mapped[str] = mapped_column(String, nullable=False)
    long_name: Mapped[str] = mapped_column(String, nullable=False)
    route_type: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    route_color: Mapped[str | None] = mapped_column(String)
    route_text_color: Mapped[str | None] = mapped_column(String)

    # Relationships
    agency: Mapped["TramAgency"] = relationship(back_populates="routes")
    trips: Mapped[list["TramTrip"]] = relationship(back_populates="route")


class TramTripShape(Base):
    __tablename__ = "tram_trip_shapes"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint("shape_id", "pt_sequence", name="uq_tram_shape_sequence"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    shape_id: Mapped[str] = mapped_column(String, nullable=False, index=True)
    pt_sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    pt_lat: Mapped[float] = mapped_column(Double, nullable=False)
    pt_lon: Mapped[float] = mapped_column(Double, nullable=False)
    dist_traveled: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    trips: Mapped[list["TramTrip"]] = relationship(
        primaryjoin="TramTripShape.shape_id == foreign(TramTrip.shape_id)",
        back_populates="shape",
        viewonly=True,
    )


class TramStop(Base):
    __tablename__ = "tram_stops"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[str] = mapped_column(String, primary_key=True)
    code: Mapped[int] = mapped_column(Integer, nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    stop_times: Mapped[list["TramStopTime"]] = relationship(back_populates="stop")


class TramTrip(Base):
    __tablename__ = "tram_trips"
    __table_args__: ClassVar[dict] = (
        Index("ix_tram_trips_route_id", "route_id"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[str] = mapped_column(String, primary_key=True)
    route_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.tram_routes.id"), nullable=False
    )
    service_id: Mapped[int] = mapped_column(Integer, nullable=False)
    headsign: Mapped[str] = mapped_column(String, nullable=False)
    short_name: Mapped[str] = mapped_column(String, nullable=False)
    direction_id: Mapped[int] = mapped_column(Integer, nullable=False)
    block_id: Mapped[str | None] = mapped_column(String)
    shape_id: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    route: Mapped["TramRoute"] = relationship(back_populates="trips")
    stop_times: Mapped[list["TramStopTime"]] = relationship(back_populates="trip")

    service: Mapped["TramCalendarSchedule"] = relationship(
        primaryjoin="foreign(TramTrip.service_id) == TramCalendarSchedule.service_id",
        back_populates="trips",
        viewonly=True,
    )
    shape: Mapped[list["TramTripShape"]] = relationship(
        primaryjoin="foreign(TramTrip.shape_id) == TramTripShape.shape_id",
        back_populates="trips",
        viewonly=True,
    )


class TramStopTime(Base):
    __tablename__ = "tram_stop_times"
    __table_args__: ClassVar[dict] = (
        Index("ix_tram_stop_times_trip_id", "trip_id"),
        Index("ix_tram_stop_times_stop_id", "stop_id"),
        Index("ix_tram_stop_times_trip_stop", "trip_id", "stop_id"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    trip_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.tram_trips.id"), nullable=False
    )
    stop_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.tram_stops.id"), nullable=False
    )
    arrival_time: Mapped[time] = mapped_column(Time, nullable=False)
    departure_time: Mapped[time] = mapped_column(Time, nullable=False)
    sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    headsign: Mapped[str | None] = mapped_column(String)

    # Relationships
    trip: Mapped["TramTrip"] = relationship(back_populates="stop_times")
    stop: Mapped["TramStop"] = relationship(back_populates="stop_times")


# ── CSO Dataset Models ──────────────────────────────────────────────
# These map to CSV files downloaded from data.gov.ie and placed in the
# tram data directory. Processed exactly like GTFS files — read from
# disk, parse rows, bulk-insert.


class TramPassengerJourney(Base):
    """TII03 - Passenger Journeys by Luas (weekly, by line)."""

    __tablename__ = "tram_passenger_journeys"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint("week_code", "line_code", name="uq_tram_pj_week_line"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    statistic: Mapped[str] = mapped_column(String, nullable=False)
    statistic_label: Mapped[str] = mapped_column(String, nullable=False)
    week_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    week_label: Mapped[str] = mapped_column(String, nullable=False)
    line_code: Mapped[str] = mapped_column(String, nullable=False)
    line_label: Mapped[str] = mapped_column(String, nullable=False)
    unit: Mapped[str] = mapped_column(String, nullable=False)
    value: Mapped[int | None] = mapped_column(Integer)


class TramPassengerNumber(Base):
    """TOA11 - Luas Passenger Numbers (monthly, by line)."""

    __tablename__ = "tram_passenger_numbers"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "year", "month_code", "statistic", name="uq_tram_pn_year_month_stat"
        ),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    statistic: Mapped[str] = mapped_column(String, nullable=False)
    statistic_label: Mapped[str] = mapped_column(String, nullable=False)
    year: Mapped[str] = mapped_column(String, nullable=False, index=True)
    month_code: Mapped[str] = mapped_column(String, nullable=False)
    month_label: Mapped[str] = mapped_column(String, nullable=False)
    unit: Mapped[str] = mapped_column(String, nullable=False)
    value: Mapped[int | None] = mapped_column(Integer)


class TramHourlyDistribution(Base):
    """TOA09 - Percentage of daily Luas passengers by hour and by line."""

    __tablename__ = "tram_hourly_distribution"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "year", "line_code", "time_code", name="uq_tram_hd_year_line_time"
        ),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    statistic: Mapped[str] = mapped_column(String, nullable=False)
    statistic_label: Mapped[str] = mapped_column(String, nullable=False)
    year: Mapped[str] = mapped_column(String, nullable=False, index=True)
    line_code: Mapped[str] = mapped_column(String, nullable=False)
    line_label: Mapped[str] = mapped_column(String, nullable=False)
    time_code: Mapped[str] = mapped_column(String, nullable=False)
    time_label: Mapped[str] = mapped_column(String, nullable=False)
    unit: Mapped[str] = mapped_column(String, nullable=False)
    value: Mapped[float | None] = mapped_column(Float)


class TramWeeklyFlow(Base):
    """TOA02 - Average weekly flow of Luas passengers (by day of week)."""

    __tablename__ = "tram_weekly_flow"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "year", "day_code", "statistic", name="uq_tram_wf_year_day_stat"
        ),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    statistic: Mapped[str] = mapped_column(String, nullable=False)
    statistic_label: Mapped[str] = mapped_column(String, nullable=False)
    year: Mapped[str] = mapped_column(String, nullable=False, index=True)
    day_code: Mapped[str] = mapped_column(String, nullable=False)
    day_label: Mapped[str] = mapped_column(String, nullable=False)
    unit: Mapped[str] = mapped_column(String, nullable=False)
    value: Mapped[int | None] = mapped_column(Integer)


# ── Luas Forecasting API Models ─────────────────────────────────────
# Live data fetched from luasforecasts.rpa.ie XML API.


class TramLuasStop(Base):
    """Luas stop from the forecasting API."""

    __tablename__ = "tram_luas_stops"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    stop_id: Mapped[str] = mapped_column(String, primary_key=True)
    line: Mapped[str] = mapped_column(String, nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    pronunciation: Mapped[str] = mapped_column(String, nullable=False, default="")
    park_ride: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    cycle_ride: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    forecasts: Mapped[list["TramLuasForecast"]] = relationship(back_populates="stop")


class TramLuasForecast(Base):
    """Live forecast entry from the Luas forecasting API."""

    __tablename__ = "tram_luas_forecasts"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    stop_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.tram_luas_stops.stop_id"), nullable=False, index=True
    )
    line: Mapped[str] = mapped_column(String, nullable=False)
    direction: Mapped[str] = mapped_column(String, nullable=False)
    destination: Mapped[str] = mapped_column(String, nullable=False)
    due_mins: Mapped[int | None] = mapped_column(Integer)
    message: Mapped[str] = mapped_column(String, nullable=False, default="")

    # Relationships
    stop: Mapped["TramLuasStop"] = relationship(back_populates="forecasts")
