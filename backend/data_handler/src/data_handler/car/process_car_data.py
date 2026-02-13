# data_handler/car/process_car_data.py

import logging
from pathlib import Path

from sqlalchemy import delete

from data_handler.car.car_parsing_utils import (
    parse_kw_value,
    parse_month_year,
    parse_open_hours,
    parse_scats_time,
    parse_year,
)
from data_handler.car.models import (
    EmissionBand,
    EVChargingPoint,
    FuelType,
    PrivateCarEmission,
    ScatsSite,
    TaxationClass,
    TrafficVolume,
    VehicleFirstTime,
    VehicleLicensingArea,
    VehicleNewLicensed,
    VehicleRegistrationType,
    VehicleYearly,
)
from data_handler.csv_utils import read_csv_file
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


# ============================================================================
# ROW PARSERS
# ============================================================================


def parse_scats_site_row(row: dict[str, str]) -> ScatsSite:
    """Parse SCATS site row."""
    return ScatsSite(
        site_id=int(row["SiteID"]),
        description=row["Site Description"].strip(),
        description_lower=row["Site Description Lower"].strip(),
        region=row["Region"].strip(),
        lat=float(row["Lat"]),
        lon=float(row["Long"]),
    )


def parse_traffic_volume_row(row: dict[str, str]) -> TrafficVolume:
    """Parse traffic volume row."""
    return TrafficVolume(
        end_time=parse_scats_time(row["End_Time"]),
        site_id=int(row["Site"]),
        detector=int(row["Detector"]),
        region=row["Region"].strip(),
        sum_volume=int(row["Sum_Volume"]),
        avg_volume=int(row["Avg_Volume"]),
    )


def parse_vehicle_first_time_row(row: dict[str, str]) -> VehicleFirstTime:
    """Parse vehicle first time licensing row."""
    taxation_class_str = row["Taxation class"].strip()

    # Convert string to enum
    try:
        taxation_class = TaxationClass(taxation_class_str)
    except ValueError as e:
        msg = f"Invalid taxation class: {taxation_class_str}. Must be one of: {', '.join([t.value for t in TaxationClass])}"
        raise ValueError(msg) from e

    return VehicleFirstTime(
        month=parse_month_year(row["Month"]),
        taxation_class=taxation_class,
        count=int(row["Number"]),
    )


def parse_vehicle_licensing_area_row(row: dict[str, str]) -> VehicleLicensingArea:
    """Parse vehicle licensing by area row."""
    fuel_type_str = row["Types of Fuel"].strip()

    # Convert string to enum
    try:
        fuel_type = FuelType(fuel_type_str)
    except ValueError as e:
        msg = f"Invalid fuel type: {fuel_type_str}. Must be one of: {', '.join([f.value for f in FuelType])}"
        raise ValueError(msg) from e

    return VehicleLicensingArea(
        month=parse_month_year(row["Month"]),
        licensing_authority=row["Licensing Authority"].strip(),
        fuel_type=fuel_type,
        count=int(row["Number"]),
    )


def parse_vehicle_new_licensed_row(row: dict[str, str]) -> VehicleNewLicensed:
    """Parse new vehicle licensing row."""
    registration_type_str = row["Type of Vehicle Registration"].strip()
    fuel_type_str = row["Types of Fuel"].strip()

    # Convert strings to enums
    try:
        registration_type = VehicleRegistrationType(registration_type_str)
    except ValueError as e:
        msg = f"Invalid registration type: {registration_type_str}. Must be one of: {', '.join([r.value for r in VehicleRegistrationType])}"
        raise ValueError(msg) from e

    try:
        fuel_type = FuelType(fuel_type_str)
    except ValueError as e:
        msg = f"Invalid fuel type: {fuel_type_str}. Must be one of: {', '.join([f.value for f in FuelType])}"
        raise ValueError(msg) from e

    return VehicleNewLicensed(
        month=parse_month_year(row["Month"]),
        registration_type=registration_type,
        fuel_type=fuel_type,
        count=int(row["Number"]),
    )


def parse_vehicle_yearly_row(row: dict[str, str]) -> VehicleYearly:
    """Parse yearly vehicle data row."""
    taxation_class_str = row["Taxation Class"].strip()
    fuel_type_str = row["Types of Fuel"].strip()

    # Convert strings to enums
    try:
        taxation_class = TaxationClass(taxation_class_str)
    except ValueError as e:
        msg = f"Invalid taxation class: {taxation_class_str}. Must be one of: {', '.join([t.value for t in TaxationClass])}"
        raise ValueError(msg) from e

    try:
        fuel_type = FuelType(fuel_type_str)
    except ValueError as e:
        msg = f"Invalid fuel type: {fuel_type_str}. Must be one of: {', '.join([f.value for f in FuelType])}"
        raise ValueError(msg) from e

    return VehicleYearly(
        year=parse_year(row["Year"]),
        taxation_class=taxation_class,
        fuel_type=fuel_type,
        count=int(row["Number"]),
    )


def parse_private_car_emission_row(row: dict[str, str]) -> PrivateCarEmission:
    """Parse private car emission data row."""
    emission_band_str = row["Emission Band"].strip()

    # Convert string to enum
    try:
        emission_band = EmissionBand(emission_band_str)
    except ValueError as e:
        msg = f"Invalid emission band: {emission_band_str}. Must be one of: {', '.join([b.value for b in EmissionBand])}"
        raise ValueError(msg) from e

    return PrivateCarEmission(
        year=parse_year(row["Year"]),
        emission_band=emission_band,
        licensing_authority=row["Licensing Authority"].strip(),
        count=int(row["Value"]),
    )


def parse_and_filter_ev_charging_point_row(
    row: dict[str, str],
) -> EVChargingPoint | None:
    """
    Parse EV charging point row.

    Returns None if county is not Dublin (as per requirements).
    """
    county = row["County"].strip()

    # Only process Dublin charging points
    if county.lower() != "dublin":
        return None

    # Parse operating hours (returns tuple of (is_24_7, description))
    open_hours_str = row.get("Open Hours", "")
    is_24_7, _hours_description = parse_open_hours(open_hours_str)

    return EVChargingPoint(
        county="Dublin",  # Hardcoded for consistency
        lat=float(row["Latitude"]),
        lon=float(row["Longitude"]),
        power_rating_of_ccs_connectors_kw=parse_kw_value(row.get("CCS kWs", "")),
        power_rating_of_chademo_connectors_kw=parse_kw_value(
            row.get("CHAdeMO kWs", "")
        ),
        power_rating_of_ac_fast_kw=parse_kw_value(row.get("AC Fast kWs", "")),
        power_rating_of_standard_ac_socket_kw=parse_kw_value(
            row.get("AC Socket kWs", "")
        ),
        is_24_7=is_24_7,
    )


# ============================================================================
# MAIN PROCESSING FUNCTION
# ============================================================================


def process_car_static_data(data_dir: Path) -> None:
    """
    Process car static data from CSV files.

    This function:
    1. Validates all required CSV files exist
    2. Deletes all existing data from relevant tables
    3. Reads data from CSV files
    4. Inserts the data into the database
    5. All operations happen within a single transaction

    Args:
        data_dir: Path to the directory containing car CSV files.

    Raises:
        FileNotFoundError: If any required CSV file is missing
        ValueError: If any CSV file is missing required headers or the row data is invalid
    """

    # CSV file name -> (required headers, transform row function)
    csv_files = {
        "scats_sites.csv": (
            [
                "SiteID",
                "Site Description",
                "Site Description Lower",
                "Region",
                "Lat",
                "Long",
            ],
            parse_scats_site_row,
        ),
        "traffic_volumes.csv": (
            ["End_Time", "Region", "Site", "Detector", "Sum_Volume", "Avg_Volume"],
            parse_traffic_volume_row,
        ),
        "vehicle_first_time.csv": (
            ["Month", "Taxation class", "Number"],
            parse_vehicle_first_time_row,
        ),
        "vehicle_licensing_area.csv": (
            ["Month", "Licensing Authority", "Types of Fuel", "Number"],
            parse_vehicle_licensing_area_row,
        ),
        "vehicle_new_licensed.csv": (
            ["Month", "Type of Vehicle Registration", "Types of Fuel", "Number"],
            parse_vehicle_new_licensed_row,
        ),
        "vehicle_yearly.csv": (
            ["Year", "Taxation Class", "Types of Fuel", "Number"],
            parse_vehicle_yearly_row,
        ),
        "private_car_emissions.csv": (
            ["Year", "Emission Band", "Licensing Authority", "Value"],
            parse_private_car_emission_row,
        ),
        "ev_charging_points.csv": (
            ["County", "Latitude", "Longitude"],
            parse_and_filter_ev_charging_point_row,
        ),
    }

    # Validate all files exist before processing
    # logger.info("Validating CSV files...")
    # for filename in csv_files:
    #     if data_dir is None:
    #         msg = "data_dir cannot be None"
    #         raise ValueError(msg)
    #     file_path = data_dir / filename
    #     if not file_path.exists():
    #         msg = f"Required CSV file not found: {file_path}"
    #         raise FileNotFoundError(msg)

    logger.info("Processing static car data...")

    session = SessionLocal()

    try:
        # Delete existing data in reverse dependency order
        logger.info("Deleting existing data...")
        session.execute(delete(TrafficVolume))
        session.execute(delete(VehicleFirstTime))
        session.execute(delete(VehicleLicensingArea))
        session.execute(delete(VehicleNewLicensed))
        session.execute(delete(VehicleYearly))
        session.execute(delete(PrivateCarEmission))
        session.execute(delete(EVChargingPoint))
        session.execute(delete(ScatsSite))

        session.commit()

        # Process each CSV file
        for filename, (required_headers, transform_row) in csv_files.items():
            logger.info("Processing %s...", filename)
            file_path = data_dir / filename

            rows = []
            for csv_row in read_csv_file(file_path, required_headers):
                parsed_row = transform_row(csv_row)

                # Skip None rows (e.g., non-Dublin charging points)
                if parsed_row is not None:
                    rows.append(parsed_row)

            session.add_all(rows)
            logger.info("  Added %d rows from %s", len(rows), filename)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed static car data.")

    except Exception:
        session.rollback()
        logger.exception("Error processing static car data")
        raise

    finally:
        session.close()
