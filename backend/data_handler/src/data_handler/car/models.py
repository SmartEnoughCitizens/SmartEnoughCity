# data_handler/car/models.py

import enum
from datetime import datetime
from typing import ClassVar

from sqlalchemy import DateTime, Float, ForeignKey, Integer, String
from sqlalchemy import Enum as SQLEnum
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base


class EmissionBand(enum.Enum):
    """EU vehicle emission bands."""
    BAND_A = "Band A"
    BAND_B = "Band B"
    BAND_C = "Band C"
    BAND_D = "Band D"
    BAND_E = "Band E"
    BAND_F = "Band F"
    BAND_G = "Band G"


class ScatsSite(Base):
    """SCATS traffic monitoring site locations."""
    __tablename__ = "scats_sites"

    site_id: Mapped[int] = mapped_column(Integer, primary_key=True)
    description: Mapped[str] = mapped_column(String, nullable=False)
    description_lower: Mapped[str] = mapped_column(String, nullable=False)
    region: Mapped[str] = mapped_column(String, nullable=False)
    lat: Mapped[float] = mapped_column(Float, nullable=False)
    lon: Mapped[float] = mapped_column(Float, nullable=False)

    # Relationships
    traffic_volumes: Mapped[list["TrafficVolume"]] = relationship(back_populates="site")


class TrafficVolume(Base):
    """Hourly traffic volume data from SCATS detectors."""
    __tablename__ = "traffic_volumes"

    # Composite primary key
    end_time: Mapped[datetime] = mapped_column(DateTime, primary_key=True)
    site_id: Mapped[int] = mapped_column(
        ForeignKey("scats_sites.site_id"), primary_key=True
    )
    detector: Mapped[int] = mapped_column(Integer, primary_key=True)

    region: Mapped[str] = mapped_column(String, nullable=False)
    sum_volume: Mapped[int] = mapped_column(Integer, nullable=False)  # Total vehicles in hour
    avg_volume: Mapped[int] = mapped_column(Integer, nullable=False)  # Avg per 5-min interval

    # Relationships
    site: Mapped["ScatsSite"] = relationship(back_populates="traffic_volumes")


class PrivateCarEmission(Base):
    """Private cars licensed by emission band and licensing authority."""
    __tablename__ = "private_car_emissions"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    year: Mapped[int] = mapped_column(Integer, nullable=False)
    emission_band: Mapped[EmissionBand] = mapped_column(
        SQLEnum(EmissionBand), nullable=False
    )
    licensing_authority: Mapped[str] = mapped_column(String, nullable=False)
    count: Mapped[int] = mapped_column(Integer, nullable=False)


class EVChargingPoint(Base):
    """Electric vehicle charging point locations and specifications."""
    __tablename__ = "ev_charging_points"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    county: Mapped[str] = mapped_column(String, nullable=False)
    address: Mapped[str | None] = mapped_column(String)
    lat: Mapped[float] = mapped_column(Float, nullable=False)
    lon: Mapped[float] = mapped_column(Float, nullable=False)
    
    # Charging specifications
    max_sim_ccs: Mapped[int | None] = mapped_column(Integer)
    max_sim_chademo: Mapped[int | None] = mapped_column(Integer)
    max_sim_fast_ac: Mapped[int | None] = mapped_column(Integer)
    max_sim_ac_socket: Mapped[int | None] = mapped_column(Integer)
    
    ccs_kw: Mapped[str | None] = mapped_column(String)
    chademo_kw: Mapped[str | None] = mapped_column(String)
    ac_fast_kw: Mapped[str | None] = mapped_column(String)
    ac_socket_kw: Mapped[str | None] = mapped_column(String)
    
    open_hours: Mapped[str | None] = mapped_column(String)