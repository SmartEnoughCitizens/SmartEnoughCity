"""SQLAlchemy models for traffic and construction data."""

from datetime import datetime
from enum import StrEnum
from typing import ClassVar

from sqlalchemy import (
    DateTime,
    Double,
    Index,
    Integer,
    String,
    Text,
)
from sqlalchemy import (
    Enum as SQLEnum,
)
from sqlalchemy.orm import Mapped, mapped_column

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class TrafficEventType(StrEnum):
    """Types of traffic events from TII API."""

    CONGESTION = "CONGESTION"
    CLOSURE_INCIDENT = "CLOSURE/INCIDENT"
    ROADWORKS = "ROADWORKS"
    WARNING = "WARNING"


class TrafficEvent(Base):
    """
    Model for traffic events from TII (Transport Infrastructure Ireland).

    Stores real-time traffic information including roadworks, incidents,
    congestion, and weather warnings in the Dublin area.
    """

    __tablename__ = "traffic_events"
    __table_args__: ClassVar[dict] = (
        Index("ix_traffic_events_type", "event_type"),
        Index("ix_traffic_events_location", "lat", "lon"),
        Index("ix_traffic_events_fetched_at", "fetched_at"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    event_type: Mapped[TrafficEventType] = mapped_column(
        SQLEnum(TrafficEventType), nullable=False
    )
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)
    color: Mapped[str] = mapped_column(String(20), nullable=False)
    fetched_at: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, default=datetime.utcnow
    )
    source_id: Mapped[str | None] = mapped_column(
        String(255), unique=True, nullable=True
    )
