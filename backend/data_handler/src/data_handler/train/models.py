import enum
from datetime import date, datetime, time
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    Double,
    Enum,
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


# ── Enums ──────────────────────────────────────────────────────────


class RouteType(enum.IntEnum):
    """GTFS route_type values relevant to Irish Rail."""

    TRAM = 0
    SUBWAY = 1
    RAIL = 2
    BUS = 3
    FERRY = 4


class StationType(enum.StrEnum):
    """Irish Rail station classification."""

    MAINLINE = "M"
    SUBURBAN = "S"
    DART = "D"


class TrainStatus(enum.StrEnum):
    """Running status of a train."""

    NOT_YET_RUNNING = "N"
    RUNNING = "R"


class StationLocationType(enum.StrEnum):
    """Location type at a station for station data."""

    ORIGIN = "O"
    DESTINATION = "D"
    STOP = "S"


class MovementLocationType(enum.StrEnum):
    """Location type for train movement records."""

    ORIGIN = "O"
    STOP = "S"
    TIMING_POINT = "T"
    DESTINATION = "D"


class StopType(enum.StrEnum):
    """Stop type for train movement records."""

    CURRENT = "C"
    NEXT = "N"


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
        UniqueConstraint("service_id", "date", name="uq_train_calendar_date"),
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
    route_type: Mapped[RouteType] = mapped_column(
        Enum(RouteType), nullable=False, default=RouteType.RAIL
    )
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
    station_code: Mapped[str] = mapped_column(
        String, nullable=False, unique=True, index=True
    )
    station_desc: Mapped[str] = mapped_column(String, nullable=False)
    station_alias: Mapped[str | None] = mapped_column(String)
    station_type: Mapped[StationType | None] = mapped_column(Enum(StationType))
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    station_data: Mapped[list["IrishRailStationData"]] = relationship(
        back_populates="station"
    )


class IrishRailCurrentTrain(Base):
    """Running train from Irish Rail API (getCurrentTrainsXML)."""

    __tablename__ = "irish_rail_current_trains"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    train_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    train_date: Mapped[date] = mapped_column(Date, nullable=False)
    train_status: Mapped[TrainStatus] = mapped_column(Enum(TrainStatus), nullable=False)
    train_type: Mapped[str | None] = mapped_column(String)
    direction: Mapped[str | None] = mapped_column(String)
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
        ForeignKey(f"{DB_SCHEMA}.irish_rail_stations.station_code"),
        nullable=False,
        index=True,
    )
    train_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    train_date: Mapped[date] = mapped_column(Date, nullable=False)
    train_type: Mapped[str | None] = mapped_column(String)
    origin: Mapped[str] = mapped_column(String, nullable=False)
    destination: Mapped[str] = mapped_column(String, nullable=False)
    origin_time: Mapped[time | None] = mapped_column(Time)
    destination_time: Mapped[time | None] = mapped_column(Time)
    status: Mapped[str | None] = mapped_column(String)
    last_location: Mapped[str | None] = mapped_column(String)
    due_in_minutes: Mapped[int | None] = mapped_column(Integer)
    late_minutes: Mapped[int | None] = mapped_column(Integer)
    exp_arrival: Mapped[time | None] = mapped_column(Time)
    exp_depart: Mapped[time | None] = mapped_column(Time)
    sch_arrival: Mapped[time | None] = mapped_column(Time)
    sch_depart: Mapped[time | None] = mapped_column(Time)
    direction: Mapped[str | None] = mapped_column(String)
    location_type: Mapped[StationLocationType | None] = mapped_column(
        Enum(StationLocationType)
    )
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)

    # Relationships
    station: Mapped["IrishRailStation"] = relationship(back_populates="station_data")


class IrishRailTrainMovement(Base):
    """Train movement/journey details from Irish Rail API (getTrainMovementsXML)."""

    __tablename__ = "irish_rail_train_movements"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    train_code: Mapped[str] = mapped_column(String, nullable=False, index=True)
    train_date: Mapped[date] = mapped_column(Date, nullable=False)
    location_code: Mapped[str] = mapped_column(String, nullable=False)
    location_full_name: Mapped[str] = mapped_column(String, nullable=False)
    location_order: Mapped[int] = mapped_column(Integer, nullable=False)
    location_type: Mapped[MovementLocationType] = mapped_column(
        Enum(MovementLocationType), nullable=False
    )
    train_origin: Mapped[str] = mapped_column(String, nullable=False)
    train_destination: Mapped[str] = mapped_column(String, nullable=False)
    scheduled_arrival: Mapped[time | None] = mapped_column(Time)
    scheduled_departure: Mapped[time | None] = mapped_column(Time)
    actual_arrival: Mapped[time | None] = mapped_column(Time)
    actual_departure: Mapped[time | None] = mapped_column(Time)
    auto_arrival: Mapped[bool | None] = mapped_column(Boolean)
    auto_depart: Mapped[bool | None] = mapped_column(Boolean)
    stop_type: Mapped[StopType | None] = mapped_column(Enum(StopType))
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
