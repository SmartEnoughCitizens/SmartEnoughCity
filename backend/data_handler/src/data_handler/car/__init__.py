# data_handler/car/__init__.py

from data_handler.car.models import (
    ScatsSite,
    TrafficVolume,
    TaxationClass,
    FuelType,
    LicensingAuthority,
    EmissionBand,
    VehicleRegistrationType,
    VehicleFirstTime,
    VehicleLicensingArea,
    VehicleNewLicensed,
    VehicleYearly,
    PrivateCarEmission,
    EVChargingPoint,
)
from data_handler.car.process_car_data import process_car_static_data

__all__ = [
    # Models
    "ScatsSite",
    "TrafficVolume",
    "TaxationClass",
    "FuelType",
    "LicensingAuthority",
    "EmissionBand",
    "VehicleRegistrationType",
    "VehicleFirstTime",
    "VehicleLicensingArea",
    "VehicleNewLicensed",
    "VehicleYearly",
    "PrivateCarEmission",
    "EVChargingPoint",
    # Processing
    "process_car_static_data",
]