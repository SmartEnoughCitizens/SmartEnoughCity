"""Database models for Dublin Bikes data."""

from datetime import datetime
from typing import ClassVar, Optional
from decimal import Decimal

from sqlalchemy import (
    Boolean,
    DateTime,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    UniqueConstraint,
    DECIMAL,
    func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class DublinBikesStation(Base):
    """Static station metadata (Table 1: stations)."""

    __tablename__ = "dublin_bikes_stations"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    station_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    system_id: Mapped[str] = mapped_column(String, nullable=False, default='dublin')
    name: Mapped[str] = mapped_column(String, nullable=False)
    short_name: Mapped[Optional[str]] = mapped_column(String)
    address: Mapped[Optional[str]] = mapped_column(Text)
    latitude: Mapped[Decimal] = mapped_column(DECIMAL(10, 6), nullable=False)
    longitude: Mapped[Decimal] = mapped_column(DECIMAL(10, 6), nullable=False)
    capacity: Mapped[int] = mapped_column(Integer, nullable=False)
    region_id: Mapped[Optional[str]] = mapped_column(String)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=datetime.utcnow
    )

    # Relationships
    snapshots: Mapped[list["DublinBikesStationSnapshot"]] = relationship(
        back_populates="station"
    )
    history: Mapped[list["DublinBikesStationHistory"]] = relationship(
        back_populates="station"
    )


class DublinBikesStationSnapshot(Base):
    """Real-time station status (Table 2: station_snapshots)."""

    __tablename__ = "dublin_bikes_station_snapshots"
    __table_args__: ClassVar[dict] = (
        Index("idx_snapshot_station_timestamp", "station_id", "timestamp"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    station_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.dublin_bikes_stations.station_id"), nullable=False
    )
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_reported: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    available_bikes: Mapped[int] = mapped_column(Integer, nullable=False)
    available_docks: Mapped[int] = mapped_column(Integer, nullable=False)
    disabled_bikes: Mapped[int] = mapped_column(Integer, default=0)
    disabled_docks: Mapped[int] = mapped_column(Integer, default=0)
    is_installed: Mapped[bool] = mapped_column(Boolean, nullable=False)
    is_renting: Mapped[bool] = mapped_column(Boolean, nullable=False)
    is_returning: Mapped[bool] = mapped_column(Boolean, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    station: Mapped["DublinBikesStation"] = relationship(back_populates="snapshots")


class DublinBikesStationHistory(Base):
    """Historical station status archive (Table 3: station_history)."""

    __tablename__ = "dublin_bikes_station_history"
    __table_args__: ClassVar[dict] = (
        Index("idx_history_station_timestamp", "station_id", "timestamp"),
        Index("idx_history_date", "timestamp"),
        UniqueConstraint("station_id", "timestamp", name="uq_station_timestamp"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    station_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.dublin_bikes_stations.station_id"), nullable=False
    )
    timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    last_reported: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    available_bikes: Mapped[int] = mapped_column(Integer, nullable=False)
    available_docks: Mapped[int] = mapped_column(Integer, nullable=False)
    is_installed: Mapped[bool] = mapped_column(Boolean, nullable=False)
    is_renting: Mapped[bool] = mapped_column(Boolean, nullable=False)
    is_returning: Mapped[bool] = mapped_column(Boolean, nullable=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    # Relationships
    station: Mapped["DublinBikesStation"] = relationship(back_populates="history")
