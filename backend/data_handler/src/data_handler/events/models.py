"""SQLAlchemy models for events data."""

from datetime import date, datetime
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    Date,
    DateTime,
    Double,
    Index,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class Event(Base):
    """
    Model for events from Ticketmaster and TheSportsDB.

    Stores upcoming Dublin events including concerts, sports fixtures,
    and arts/theatre events with location and impact data.
    """

    __tablename__ = "events"
    __table_args__: ClassVar[dict] = (
        UniqueConstraint("source", "source_id", name="uq_event_source"),
        Index("ix_events_event_date", "event_date"),
        Index("ix_events_is_high_impact", "is_high_impact"),
        Index("ix_events_location", "latitude", "longitude"),
        Index("ix_events_source", "source"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    source: Mapped[str] = mapped_column(String(50), nullable=False)
    source_id: Mapped[str] = mapped_column(String(255), nullable=False)
    event_name: Mapped[str] = mapped_column(String(500), nullable=False)
    event_type: Mapped[str] = mapped_column(String(100), nullable=False)
    venue_name: Mapped[str] = mapped_column(String(255), nullable=False)
    latitude: Mapped[float] = mapped_column(Double, nullable=False)
    longitude: Mapped[float] = mapped_column(Double, nullable=False)
    event_date: Mapped[date] = mapped_column(Date, nullable=False)
    start_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    end_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    is_high_impact: Mapped[bool] = mapped_column(Boolean, nullable=False)
    estimated_attendance: Mapped[int | None] = mapped_column(Integer, nullable=True)
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
