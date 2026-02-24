"""SQLAlchemy models for events data."""

from datetime import date, datetime
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    Computed,
    Date,
    DateTime,
    Double,
    ForeignKey,
    Index,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


def _fk(table_col: str) -> str:
    """Build a schema-qualified ForeignKey reference."""
    return f"{DB_SCHEMA}.{table_col}" if DB_SCHEMA else table_col


class Venue(Base):
    """
    Model for event venues sourced from Ticketmaster.

    Stores static venue metadata including location and capacity.
    Capacity is manually set in the database after seeding; it is NOT
    sourced from the Ticketmaster API (which does not expose it).

    venue_size_tag is auto-computed from capacity using:
        capacity >= 50,000  →  "major_stadium"   (e.g. Croke Park ~82k, Aviva ~51k)
        capacity >= 20,000  →  "stadium"          (e.g. RDS Arena ~22k)
        capacity >= 8,000   →  "arena"            (e.g. 3Arena ~13k, Tallaght Stadium ~10k)
        capacity >= 1,000   →  "theatre"          (e.g. Olympia, Vicar Street)
        capacity IS NOT NULL → "venue"            (small clubs and intimate spaces)
        capacity IS NULL    →  NULL               (unknown — capacity not yet set)

    Events with venue tag in ("arena", "stadium", "major_stadium")
    are classified as high-impact (capacity >= 8,000).
    """

    __tablename__ = "venues"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    ticketmaster_id: Mapped[str] = mapped_column(
        String(255), nullable=False, unique=True
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    address: Mapped[str | None] = mapped_column(String(500), nullable=True)
    city: Mapped[str | None] = mapped_column(String(100), nullable=True)
    latitude: Mapped[float] = mapped_column(Double, nullable=False)
    longitude: Mapped[float] = mapped_column(Double, nullable=False)
    capacity: Mapped[int | None] = mapped_column(Integer, nullable=True)
    venue_size_tag: Mapped[str | None] = mapped_column(
        String(50),
        Computed(
            """
            CASE
                WHEN capacity >= 50000 THEN 'major_stadium'
                WHEN capacity >= 20000 THEN 'stadium'
                WHEN capacity >= 8000  THEN 'arena'
                WHEN capacity >= 1000  THEN 'theatre'
                WHEN capacity IS NOT NULL THEN 'venue'
                ELSE NULL
            END
            """,
            persisted=True,
        ),
        nullable=True,
    )

    events: Mapped[list["Event"]] = relationship(back_populates="venue")


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
    venue_id: Mapped[int | None] = mapped_column(
        ForeignKey(_fk("venues.id"), ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    latitude: Mapped[float] = mapped_column(Double, nullable=False)
    longitude: Mapped[float] = mapped_column(Double, nullable=False)
    event_date: Mapped[date] = mapped_column(Date, nullable=False)
    start_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    end_time: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    is_high_impact: Mapped[bool] = mapped_column(Boolean, nullable=False)
    estimated_attendance: Mapped[int | None] = mapped_column(Integer, nullable=True)
    fetched_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)

    venue: Mapped["Venue | None"] = relationship(back_populates="events")
