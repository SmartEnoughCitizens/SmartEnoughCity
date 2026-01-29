# data_handler/car/models.py

from datetime import datetime
from sqlalchemy import Column, Integer, String, Float, ForeignKey, DateTime, Boolean
from sqlalchemy.orm import relationship
from data_handler.db import Base


class ScatsSite(Base):
    """SCATS traffic monitoring site locations."""
    __tablename__ = "scats_sites"

    site_id = Column(Integer, primary_key=True)
    description = Column(String, nullable=False)
    description_lower = Column(String, nullable=False)
    region = Column(String, nullable=False)
    lat = Column(Float, nullable=False)
    lon = Column(Float, nullable=False)

    # Relationship to traffic volumes
    traffic_volumes = relationship("TrafficVolume", back_populates="site")


class TrafficVolume(Base):
    """Hourly traffic volume data from SCATS detectors."""
    __tablename__ = "traffic_volumes"

    # Composite primary key
    end_time = Column(DateTime, primary_key=True)
    site_id = Column(Integer, ForeignKey("scats_sites.site_id"), primary_key=True)
    detector = Column(Integer, primary_key=True)

    region = Column(String, nullable=False)
    sum_volume = Column(Integer, nullable=False)  # Total vehicles in hour
    avg_volume = Column(Integer, nullable=False)  # Avg per 5-min interval

    # Relationship to site
    site = relationship("ScatsSite", back_populates="traffic_volumes")


class TaxationClass(Base):
    """Vehicle taxation classes (e.g., New Private Cars, Second-hand)."""
    __tablename__ = "taxation_classes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False)

    # Relationships
    vehicle_first_time = relationship("VehicleFirstTime", back_populates="taxation_class")
    vehicle_yearly = relationship("VehicleYearly", back_populates="taxation_class")


class FuelType(Base):
    """Types of vehicle fuel (Petrol, Diesel, Electric, etc.)."""
    __tablename__ = "fuel_types"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False)

    # Relationships
    vehicle_licensing_area = relationship("VehicleLicensingArea", back_populates="fuel_type")
    vehicle_new_licensed = relationship("VehicleNewLicensed", back_populates="fuel_type")
    vehicle_yearly = relationship("VehicleYearly", back_populates="fuel_type")


class LicensingAuthority(Base):
    """Licensing authorities/regions (Dublin, Carlow, etc.)."""
    __tablename__ = "licensing_authorities"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False)

    # Relationships
    vehicle_licensing_area = relationship("VehicleLicensingArea", back_populates="authority")
    emission_data = relationship("PrivateCarEmission", back_populates="authority")


class EmissionBand(Base):
    """Vehicle emission bands (A, B, C, etc.)."""
    __tablename__ = "emission_bands"

    id = Column(Integer, primary_key=True, autoincrement=True)
    band = Column(String, unique=True, nullable=False)

    # Relationships
    emission_data = relationship("PrivateCarEmission", back_populates="emission_band")


class VehicleRegistrationType(Base):
    """Types of vehicle registration (New, Second-hand)."""
    __tablename__ = "vehicle_registration_types"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False)

    # Relationships
    vehicle_new_licensed = relationship("VehicleNewLicensed", back_populates="registration_type")


class VehicleFirstTime(Base):
    """Vehicles licensed for the first time by month and taxation class."""
    __tablename__ = "vehicle_first_time"

    id = Column(Integer, primary_key=True, autoincrement=True)
    month = Column(DateTime, nullable=False)
    taxation_class_id = Column(Integer, ForeignKey("taxation_classes.id"), nullable=False)
    count = Column(Integer, nullable=False)

    # Relationships
    taxation_class = relationship("TaxationClass", back_populates="vehicle_first_time")


class VehicleLicensingArea(Base):
    """New and second-hand cars by licensing area, fuel type, and month."""
    __tablename__ = "vehicle_licensing_area"

    id = Column(Integer, primary_key=True, autoincrement=True)
    month = Column(DateTime, nullable=False)
    authority_id = Column(Integer, ForeignKey("licensing_authorities.id"), nullable=False)
    fuel_type_id = Column(Integer, ForeignKey("fuel_types.id"), nullable=False)
    count = Column(Integer, nullable=False)

    # Relationships
    authority = relationship("LicensingAuthority", back_populates="vehicle_licensing_area")
    fuel_type = relationship("FuelType", back_populates="vehicle_licensing_area")


class VehicleNewLicensed(Base):
    """New vehicles licensed by registration type and fuel type."""
    __tablename__ = "vehicle_new_licensed"

    id = Column(Integer, primary_key=True, autoincrement=True)
    month = Column(DateTime, nullable=False)
    registration_type_id = Column(Integer, ForeignKey("vehicle_registration_types.id"), nullable=False)
    fuel_type_id = Column(Integer, ForeignKey("fuel_types.id"), nullable=False)
    count = Column(Integer, nullable=False)

    # Relationships
    registration_type = relationship("VehicleRegistrationType", back_populates="vehicle_new_licensed")
    fuel_type = relationship("FuelType", back_populates="vehicle_new_licensed")


class VehicleYearly(Base):
    """New and second-hand vehicles by year, taxation class, and fuel type."""
    __tablename__ = "vehicle_yearly"

    id = Column(Integer, primary_key=True, autoincrement=True)
    year = Column(Integer, nullable=False)
    taxation_class_id = Column(Integer, ForeignKey("taxation_classes.id"), nullable=False)
    fuel_type_id = Column(Integer, ForeignKey("fuel_types.id"), nullable=False)
    count = Column(Integer, nullable=False)

    # Relationships
    taxation_class = relationship("TaxationClass", back_populates="vehicle_yearly")
    fuel_type = relationship("FuelType", back_populates="vehicle_yearly")


class PrivateCarEmission(Base):
    """Private cars licensed by emission band and licensing authority."""
    __tablename__ = "private_car_emissions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    year = Column(Integer, nullable=False)
    emission_band_id = Column(Integer, ForeignKey("emission_bands.id"), nullable=False)
    authority_id = Column(Integer, ForeignKey("licensing_authorities.id"), nullable=False)
    count = Column(Integer, nullable=False)

    # Relationships
    emission_band = relationship("EmissionBand", back_populates="emission_data")
    authority = relationship("LicensingAuthority", back_populates="emission_data")


class EVChargingPoint(Base):
    """Electric vehicle charging point locations and specifications."""
    __tablename__ = "ev_charging_points"

    id = Column(Integer, primary_key=True, autoincrement=True)
    county = Column(String, nullable=False)
    address = Column(String, nullable=True)
    lat = Column(Float, nullable=False)
    lon = Column(Float, nullable=False)
    
    # Charging specifications
    max_sim_ccs = Column(Integer, nullable=True)
    max_sim_chademo = Column(Integer, nullable=True)
    max_sim_fast_ac = Column(Integer, nullable=True)
    max_sim_ac_socket = Column(Integer, nullable=True)
    
    ccs_kw = Column(String, nullable=True)
    chademo_kw = Column(String, nullable=True)
    ac_fast_kw = Column(String, nullable=True)
    ac_socket_kw = Column(String, nullable=True)
    
    open_hours = Column(String, nullable=True)