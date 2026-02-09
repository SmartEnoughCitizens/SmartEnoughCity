# data_handler/car/models.py

import enum
from datetime import datetime
from typing import ClassVar

from sqlalchemy import Boolean, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy import Enum as SQLEnum
from sqlalchemy.orm import Mapped, mapped_column, relationship

from data_handler.db import Base


# ============================================================================
# ENUMS (replacing lookup tables)
# ============================================================================

class EmissionBand(enum.Enum):
    """EU vehicle emission bands."""
    BAND_A = "Band A"
    BAND_B = "Band B"
    BAND_C = "Band C"
    BAND_D = "Band D"
    BAND_E = "Band E"
    BAND_F = "Band F"
    BAND_G = "Band G"
    NA = "Not available"


class TaxationClass(enum.Enum):
    """Vehicle taxation classes."""

    ALL_VEHICLES = "All Vehicles"
    NEW_VEHICLES = "New Vehicles"
    NEW_PRIVATE_CARS = "New Private Cars"
    NEW_GOODS_VEHICLES = "New Goods Vehicles"
    NEW_TRACTORS = "New Tractors"
    NEW_MOTOR_CYCLES = "New Motor Cycles"
    NEW_EXEMPT_VEHICLES = "New Exempt Vehicles"
    NEC = "New public service vehicles, heavy agricultural and plant machinery and vehicles (NEC)"
    SECONDHAND_VEHICLES = "Secondhand Vehicles"
    SECONDHAND_PRIVATE_CARS = "Secondhand Private Cars"
    SECONDHAND_GOODS_VEHICLES = "Secondhand Goods Vehicles"
    SECONDHAND_TRACTORS = "Secondhand Tractors"
    SECONDHAND_OTHER_VEHICLES = "Secondhand Other Vehicles"
    SECONDHAND_MOTOR_CYCLES = "Secondhand Motor Cycles"


class FuelType(enum.Enum):
    """Types of vehicle fuel."""
    PETROL = "Petrol"
    DIESEL = "Diesel"
    ELECTRIC = "Electric"
    PETROL_AND_ELECTRIC_HYBRID = "Petrol and electric hybrid"
    PETROL_OR_DIESEL_PLUG_IN_HYBRID_ELECTRIC = "Petrol or Diesel plug-in hybrid electric"
    DIESEL_AND_ELECTRIC_HYBRID = "Diesel and electric hybrid"
    OTHER_FUEL_TYPES = "Other fuel types"
    ALL_FUEL_TYPES = "All fuel types"


class VehicleRegistrationType(enum.Enum):
    """Types of vehicle registration."""

    ALL_VEHICLES = "All Vehicles"
    NEW_PRIVATE_CARS = "New Private Cars"
    NEW_GOODS_VEHICLES = "New Goods Vehicles"
    NEW_TRACTORS = "New Tractors"
    NEW_MOTOR_CYCLES = "New Motor Cycles"
    NEW_EXEMPT_VEHICLES = "New Exempt Vehicles"
    NEW_PSV = "New Public Service Vehicles"
    NEW_SMALL_PSV = "New Small Public Service Vehicles"
    NEW_LARGE_PSV = "New Large Public Service Vehicles"
    NEW_MACHINES = "New Machines or Contrivances"
    OTHER = "Other Classes New Other Vehicles"


    # Add more as they appear in your CSV


# ============================================================================
# MODELS
# ============================================================================

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
    sum_volume: Mapped[int] = mapped_column(Integer, nullable=False)
    avg_volume: Mapped[int] = mapped_column(Integer, nullable=False)

    # Relationships
    site: Mapped["ScatsSite"] = relationship(back_populates="traffic_volumes")


class VehicleFirstTime(Base):
    """Vehicles licensed for the first time by month and taxation class."""
    __tablename__ = "vehicle_first_time"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    month: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    taxation_class: Mapped[TaxationClass] = mapped_column(
        SQLEnum(TaxationClass), nullable=False
    )
    count: Mapped[int] = mapped_column(Integer, nullable=False)


class VehicleLicensingArea(Base):
    """New and second-hand cars by licensing area, fuel type, and month."""
    __tablename__ = "vehicle_licensing_area"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    month: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    licensing_authority: Mapped[str] = mapped_column(String, nullable=False)
    fuel_type: Mapped[FuelType] = mapped_column(SQLEnum(FuelType), nullable=False)
    count: Mapped[int] = mapped_column(Integer, nullable=False)


class VehicleNewLicensed(Base):
    """New vehicles licensed by registration type and fuel type."""
    __tablename__ = "vehicle_new_licensed"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    month: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    registration_type: Mapped[VehicleRegistrationType] = mapped_column(
        SQLEnum(VehicleRegistrationType), nullable=False
    )
    fuel_type: Mapped[FuelType] = mapped_column(SQLEnum(FuelType), nullable=False)
    count: Mapped[int] = mapped_column(Integer, nullable=False)


class VehicleYearly(Base):
    """New and second-hand vehicles by year, taxation class, and fuel type."""
    __tablename__ = "vehicle_yearly"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    year: Mapped[int] = mapped_column(Integer, nullable=False)
    taxation_class: Mapped[TaxationClass] = mapped_column(
        SQLEnum(TaxationClass), nullable=False
    )
    fuel_type: Mapped[FuelType] = mapped_column(SQLEnum(FuelType), nullable=False)
    count: Mapped[int] = mapped_column(Integer, nullable=False)


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
    # address: Mapped[str | None] = mapped_column(String)
    lat: Mapped[float] = mapped_column(Float, nullable=False)
    lon: Mapped[float] = mapped_column(Float, nullable=False)
    
    # Number of simultaneous connections
    # max_sim_ccs: Mapped[int | None] = mapped_column(Integer)
    # max_sim_chademo: Mapped[int | None] = mapped_column(Integer)
    # max_sim_fast_ac: Mapped[int | None] = mapped_column(Integer)
    # max_sim_ac_socket: Mapped[int | None] = mapped_column(Integer)
    
    # Charging power in kilowatts (kW)
    ccs_kw: Mapped[float | None] = mapped_column(Float)
    chademo_kw: Mapped[float | None] = mapped_column(Float)
    ac_fast_kw: Mapped[float | None] = mapped_column(Float)
    ac_socket_kw: Mapped[float | None] = mapped_column(Float)
    
    # Operating hours
    is_24_7: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)