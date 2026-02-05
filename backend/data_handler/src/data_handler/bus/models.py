import enum
from datetime import date, datetime, time
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    Date,
    DateTime,
    Double,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    Time,
    UniqueConstraint,
)
from sqlalchemy import (
    Enum as SQLEnum,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class BusAgency(Base):
    __tablename__ = "bus_agencies"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    url: Mapped[str] = mapped_column(String, nullable=False)
    timezone: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    routes: Mapped[list["BusRoute"]] = relationship(back_populates="agency")


class BusCalendarSchedule(Base):
    __tablename__ = "bus_calendar_schedule"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint(
            "service_id", "start_date", "end_date", name="uq_service_date_range"
        ),
        CheckConstraint("end_date >= start_date", name="chk_date_range"),
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
    trips: Mapped[list["BusTrip"]] = relationship(
        primaryjoin="BusCalendarSchedule.service_id == foreign(BusTrip.service_id)",
        back_populates="service",
        viewonly=True,
    )


class BusRoute(Base):
    __tablename__ = "bus_routes"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[str] = mapped_column(String, primary_key=True)
    agency_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.bus_agencies.id"), nullable=False
    )
    short_name: Mapped[str] = mapped_column(String, nullable=False)
    long_name: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    agency: Mapped["BusAgency"] = relationship(back_populates="routes")
    trips: Mapped[list["BusTrip"]] = relationship(back_populates="route")


class BusTripShape(Base):
    __tablename__ = "bus_trip_shapes"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint("shape_id", "pt_sequence", name="uq_shape_sequence"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    shape_id: Mapped[str] = mapped_column(String, nullable=False, index=True)
    pt_sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    pt_lat: Mapped[float] = mapped_column(Double, nullable=False)
    pt_lon: Mapped[float] = mapped_column(Double, nullable=False)
    dist_traveled: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    trips: Mapped[list["BusTrip"]] = relationship(
        primaryjoin="BusTripShape.shape_id == foreign(BusTrip.shape_id)",
        back_populates="shape",
        viewonly=True,
    )


class BusStop(Base):
    __tablename__ = "bus_stops"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[str] = mapped_column(String, primary_key=True)
    code: Mapped[int] = mapped_column(Integer, nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)

    # Relationships
    stop_times: Mapped[list["BusStopTime"]] = relationship(back_populates="stop")


class BusTrip(Base):
    __tablename__ = "bus_trips"
    __table_args__: ClassVar[dict] = (
        Index("ix_bus_trips_route_id", "route_id"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[str] = mapped_column(String, primary_key=True)
    route_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.bus_routes.id"), nullable=False
    )
    service_id: Mapped[int] = mapped_column(Integer, nullable=False)
    headsign: Mapped[str] = mapped_column(String, nullable=False)
    short_name: Mapped[str] = mapped_column(String, nullable=False)
    direction_id: Mapped[int] = mapped_column(Integer, nullable=False)
    shape_id: Mapped[str] = mapped_column(String, nullable=False)

    # Relationships
    route: Mapped["BusRoute"] = relationship(back_populates="trips")
    stop_times: Mapped[list["BusStopTime"]] = relationship(back_populates="trip")
    live_vehicles: Mapped[list["BusLiveVehicle"]] = relationship(back_populates="trip")

    service: Mapped["BusCalendarSchedule"] = relationship(
        primaryjoin="foreign(BusTrip.service_id) == BusCalendarSchedule.service_id",
        back_populates="trips",
        viewonly=True,  # service_id is not a unique PK in the calendar schedule table
    )
    shape: Mapped[list["BusTripShape"]] = relationship(
        primaryjoin="foreign(BusTrip.shape_id) == BusTripShape.shape_id",
        back_populates="trips",
        viewonly=True,  # shape_id is not a unique PK in the trip shapes table
    )


class BusStopTime(Base):
    __tablename__ = "bus_stop_times"
    __table_args__: ClassVar[dict] = (
        Index("ix_bus_stop_times_trip_id", "trip_id"),
        Index("ix_bus_stop_times_stop_id", "stop_id"),
        Index("ix_bus_stop_times_trip_stop", "trip_id", "stop_id"),
        {"schema": DB_SCHEMA},
    )

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    trip_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.bus_trips.id"), nullable=False
    )
    stop_id: Mapped[str] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.bus_stops.id"), nullable=False
    )
    arrival_time: Mapped[time] = mapped_column(Time, nullable=False)
    departure_time: Mapped[time] = mapped_column(Time, nullable=False)
    sequence: Mapped[int] = mapped_column(Integer, nullable=False)
    headsign: Mapped[str | None] = mapped_column(String)

    # Relationships
    trip: Mapped["BusTrip"] = relationship(back_populates="stop_times")
    stop: Mapped["BusStop"] = relationship(back_populates="stop_times")


class ScheduleRelationship(enum.Enum):
    scheduled = "scheduled"
    unscheduled = "unscheduled"
    added = "added"
    skipped = "skipped"
    no_data = "no_data"


class BusLiveVehicle(Base):
    __tablename__ = "bus_live_vehicles"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    entry_id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    vehicle_id: Mapped[int] = mapped_column(Integer, nullable=False)
    trip_id: Mapped[str] = mapped_column(
        String, ForeignKey(f"{DB_SCHEMA}.bus_trips.id"), nullable=False
    )
    start_time: Mapped[time] = mapped_column(Time, nullable=False)
    start_date: Mapped[date] = mapped_column(Date, nullable=False)
    schedule_relationship: Mapped[ScheduleRelationship] = mapped_column(
        SQLEnum(ScheduleRelationship), nullable=False
    )
    direction_id: Mapped[int] = mapped_column(Integer, nullable=False)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)

    # Relationships
    trip: Mapped["BusTrip"] = relationship(back_populates="live_vehicles")

