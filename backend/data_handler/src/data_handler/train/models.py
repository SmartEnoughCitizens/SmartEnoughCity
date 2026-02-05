from datetime import date, time, datetime
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
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


class TrainAgency(Base):
    __tablename__ = "train_agencies"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    url: Mapped[str] = mapped_column(String, nullable=False)
    timezone: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    routes: Mapped[list["TrainRoute"]] = relationship(back_populates="agency")


class TrainCalendarSchedule(Base):
    __tablename__ = "train_calendar_schedule"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "service_id", "start_date", "end_date", name="uq_train_service_date_range"
        ),
        CheckConstraint("end_date >= start_date", name="chk_train_date_range"),
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
    trips: Mapped[list["TrainTrip"]] = relationship(
        primaryjoin="TrainCalendarSchedule.service_id == foreign(TrainTrip.service_id)",
        back_populates="service",
        viewonly=True,
    )


class TrainCalendarDate(Base):
    __tablename__ = "train_calendar_dates"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "service_id", "date", name="uq_train_calendar_date"
        ),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    service_id: Mapped[int] = mapped_column(Integer, nullable=False, index=True)
    date: Mapped[date] = mapped_column(Date, nullable=False)
    exception_type: Mapped[int] = mapped_column(Integer, nullable=False)


class TrainRoute(Base):
    __tablename__ = "train_routes"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[str] = mapped_column(String, primary_key=True)
    agency_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.train_agencies.id"), nullable=False
    )
    short_name: Mapped[str] = mapped_column(String, nullable=False)
    long_name: Mapped[str] = mapped_column(String, nullable=False)
    route_type: Mapped[int] = mapped_column(Integer, nullable=False, default=2)
    route_color: Mapped[str | None] = mapped_column(String)
    route_text_color: Mapped[str | None] = mapped_column(String)

    # Relationships
    agency: Mapped["TrainAgency"] = relationship(back_populates="routes")
    trips: Mapped[list["TrainTrip"]] = relationship(back_populates="route")


class TrainTripShape(Base):
    __tablename__ = "train_trip_shapes"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint("shape_id", "pt_sequence", name="uq_train_shape_sequence"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    shape_id: Mapped[str] = mapped_column(String, nullable=False, index=True)
    pt_sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    pt_lat: Mapped[float] = mapped_column(Double, nullable=False)
    pt_lon: Mapped[float] = mapped_column(Double, nullable=False)
    dist_traveled: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    trips: Mapped[list["TrainTrip"]] = relationship(
        primaryjoin="TrainTripShape.shape_id == foreign(TrainTrip.shape_id)",
        back_populates="shape",
        viewonly=True,
    )


class TrainStop(Base):
    __tablename__ = "train_stops"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[str] = mapped_column(String, primary_key=True)
    code: Mapped[int] = mapped_column(Integer, nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    stop_times: Mapped[list["TrainStopTime"]] = relationship(back_populates="stop")


class TrainTrip(Base):
    __tablename__ = "train_trips"
    __table_args__: ClassVar[dict] = (
        Index("ix_train_trips_route_id", "route_id"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[str] = mapped_column(String, primary_key=True)
    route_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.train_routes.id"), nullable=False
    )
    service_id: Mapped[int] = mapped_column(Integer, nullable=False)
    headsign: Mapped[str] = mapped_column(String, nullable=False)
    short_name: Mapped[str] = mapped_column(String, nullable=False)
    direction_id: Mapped[int] = mapped_column(Integer, nullable=False)
    block_id: Mapped[str | None] = mapped_column(String)
    shape_id: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    route: Mapped["TrainRoute"] = relationship(back_populates="trips")
    stop_times: Mapped[list["TrainStopTime"]] = relationship(back_populates="trip")

    service: Mapped["TrainCalendarSchedule"] = relationship(
        primaryjoin="foreign(TrainTrip.service_id) == TrainCalendarSchedule.service_id",
        back_populates="trips",
        viewonly=True,
    )
    shape: Mapped[list["TrainTripShape"]] = relationship(
        primaryjoin="foreign(TrainTrip.shape_id) == TrainTripShape.shape_id",
        back_populates="trips",
        viewonly=True,
    )


class TrainStopTime(Base):
    __tablename__ = "train_stop_times"
    __table_args__: ClassVar[dict] = (
        Index("ix_train_stop_times_trip_id", "trip_id"),
        Index("ix_train_stop_times_stop_id", "stop_id"),
        Index("ix_train_stop_times_trip_stop", "trip_id", "stop_id"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    trip_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.train_trips.id"), nullable=False
    )
    stop_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.train_stops.id"), nullable=False
    )
    arrival_time: Mapped[time] = mapped_column(Time, nullable=False)
    departure_time: Mapped[time] = mapped_column(Time, nullable=False)
    sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    headsign: Mapped[str | None] = mapped_column(String)

    # Relationships
    trip: Mapped["TrainTrip"] = relationship(back_populates="stop_times")
    stop: Mapped["TrainStop"] = relationship(back_populates="stop_times")


# ── Irish Rail Realtime API Models ─────────────────────────────────
# Live data fetched from api.irishrail.ie XML API.


class IrishRailStation(Base):
    """Station from Irish Rail API (getAllStationsXML)."""

    __tablename__ = "irish_rail_stations"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    station_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    station_code: Mapped[str] = mapped_column(String, nullable=False, unique=True, index=True)
    station_desc: Mapped[str] = mapped_column(String, nullable=False)
    station_alias: Mapped[str | None] = mapped_column(String)
    station_type: Mapped[str | None] = mapped_column(String)  # M=Mainline, S=Suburban, D=DART
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    station_data: Mapped[list["IrishRailStationData"]] = relationship(back_populates="station")


class IrishRailCurrentTrain(Base):
    """Running train from Irish Rail API (getCurrentTrainsXML)."""

    __tablename__ = "irish_rail_current_trains"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    train_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    train_date: Mapped[str] = mapped_column(String, nullable=False)
    train_status: Mapped[str] = mapped_column(String, nullable=False)  # N=Not yet running, R=Running
    train_type: Mapped[str | None] = mapped_column(String)  # DART, Intercity, Commuter, etc.
    direction: Mapped[str | None] = mapped_column(String)  # Northbound, Southbound, To <Destination>
    lat: Mapped[float | None] = mapped_column(Double)
    lon: Mapped[float | None] = mapped_column(Double)
    public_message: Mapped[str | None] = mapped_column(Text)
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)


class IrishRailStationData(Base):
    """Train arrival/departure data for a station from Irish Rail API."""

    __tablename__ = "irish_rail_station_data"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    station_code: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.irish_rail_stations.station_code"), nullable=False, index=True
    )
    train_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    train_date: Mapped[str] = mapped_column(String, nullable=False)
    train_type: Mapped[str | None] = mapped_column(String)
    origin: Mapped[str] = mapped_column(String, nullable=False)
    destination: Mapped[str] = mapped_column(String, nullable=False)
    origin_time: Mapped[str | None] = mapped_column(String)
    destination_time: Mapped[str | None] = mapped_column(String)
    status: Mapped[str | None] = mapped_column(String)
    last_location: Mapped[str | None] = mapped_column(String)
    due_in: Mapped[int | None] = mapped_column(Integer)  # Minutes until arrival
    late: Mapped[int | None] = mapped_column(Integer)  # Minutes late
    exp_arrival: Mapped[str | None] = mapped_column(String)
    exp_depart: Mapped[str | None] = mapped_column(String)
    sch_arrival: Mapped[str | None] = mapped_column(String)
    sch_depart: Mapped[str | None] = mapped_column(String)
    direction: Mapped[str | None] = mapped_column(String)
    location_type: Mapped[str | None] = mapped_column(String)  # O=Origin, D=Destination, S=Stop
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)

    # Relationships
    station: Mapped["IrishRailStation"] = relationship(back_populates="station_data")


class IrishRailTrainMovement(Base):
    """Train movement/journey details from Irish Rail API (getTrainMovementsXML)."""

    __tablename__ = "irish_rail_train_movements"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    train_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    train_date: Mapped[str] = mapped_column(String, nullable=False)
    location_code: Mapped[str] = mapped_column(String, nullable=False)
    location_full_name: Mapped[str] = mapped_column(String, nullable=False)
    location_order: Mapped[int] = mapped_column(Integer, nullable=False)
    location_type: Mapped[str] = mapped_column(String, nullable=False)  # O=Origin, S=Stop, T=TimingPoint, D=Destination
    train_origin: Mapped[str] = mapped_column(String, nullable=False)
    train_destination: Mapped[str] = mapped_column(String, nullable=False)
    scheduled_arrival: Mapped[str | None] = mapped_column(String)
    scheduled_departure: Mapped[str | None] = mapped_column(String)
    actual_arrival: Mapped[str | None] = mapped_column(String)
    actual_departure: Mapped[str | None] = mapped_column(String)
    auto_arrival: Mapped[bool | None] = mapped_column(Boolean)
    auto_depart: Mapped[bool | None] = mapped_column(Boolean)
    stop_type: Mapped[str | None] = mapped_column(String)  # C=Current, N=Next
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
