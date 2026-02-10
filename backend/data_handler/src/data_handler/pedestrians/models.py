import enum
from datetime import datetime
from typing import ClassVar

from sqlalchemy import (
    Boolean,
    DateTime,
    Double,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy import Enum as SQLEnum
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class PedestrianGranularity(enum.Enum):
    PT15M = "PT15M"
    PT1H = "PT1H"
    P1D = "P1D"
    P1M = "P1M"
    P1Y = "P1Y"


class MobilityType(enum.Enum):
    BIKE = "BIKE"
    PEDESTRIAN = "PEDESTRIAN"
    UNDEFINED = "UNDEFINED"


class ChannelDirection(enum.Enum):
    IN = "IN"
    OUT = "OUT"


class PedestrianCounterSite(Base):
    __tablename__ = "pedestrian_counter_sites"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String, nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    lat: Mapped[float] = mapped_column(Double, nullable=False)
    lon: Mapped[float] = mapped_column(Double, nullable=False)
    first_data: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    granularity: Mapped[PedestrianGranularity] = mapped_column(
        SQLEnum(PedestrianGranularity, schema=DB_SCHEMA), nullable=False
    )
    pedestrian_sensor: Mapped[bool] = mapped_column(Boolean, nullable=False)
    bike_sensor: Mapped[bool] = mapped_column(Boolean, nullable=False)
    directional: Mapped[bool] = mapped_column(Boolean, nullable=False)
    has_timestamped_data: Mapped[bool] = mapped_column(Boolean, nullable=False)
    has_weather: Mapped[bool] = mapped_column(Boolean, nullable=False)

    channels: Mapped[list["PedestrianChannel"]] = relationship(
        "PedestrianChannel",
        back_populates="site",
        foreign_keys="PedestrianChannel.site_id",
    )


class PedestrianChannel(Base):
    __tablename__ = "pedestrian_channels"
    __table_args__: ClassVar[dict] = {"schema": DB_SCHEMA}

    channel_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    site_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.pedestrian_counter_sites.id"), nullable=False
    )
    mobility_type: Mapped[MobilityType] = mapped_column(
        SQLEnum(MobilityType, schema=DB_SCHEMA), nullable=False
    )
    direction: Mapped[ChannelDirection] = mapped_column(
        SQLEnum(ChannelDirection, schema=DB_SCHEMA), nullable=False
    )
    time_step: Mapped[int] = mapped_column(Integer, nullable=False)

    site: Mapped["PedestrianCounterSite"] = relationship(
        "PedestrianCounterSite", back_populates="channels"
    )
    measures: Mapped[list["PedestrianCounterMeasure"]] = relationship(
        "PedestrianCounterMeasure",
        back_populates="channel",
        foreign_keys="PedestrianCounterMeasure.channel_id",
    )


class PedestrianCounterMeasure(Base):
    __tablename__ = "pedestrian_counter_measures"
    __table_args__: ClassVar[tuple] = (
        UniqueConstraint(
            "channel_id",
            "start_datetime",
            "end_datetime",
            name="uq_pedestrian_counter_measures_channel_start_end",
        ),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    channel_id: Mapped[int] = mapped_column(
        ForeignKey(f"{DB_SCHEMA}.pedestrian_channels.channel_id"), nullable=False
    )
    counter_id: Mapped[str] = mapped_column(String, nullable=False)
    start_datetime: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    end_datetime: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    count: Mapped[int] = mapped_column(Integer, nullable=False)

    channel: Mapped["PedestrianChannel"] = relationship(
        "PedestrianChannel", back_populates="measures"
    )
