# data_handler/car/process_car_data.py

import logging
from pathlib import Path
from collections import defaultdict

from sqlalchemy import delete

from data_handler.car.car_parsing_utils import (
    parse_scats_time,
    parse_month_year,
    parse_year,
    safe_int,
    safe_float,
)
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
from data_handler.csv_utils import read_csv_file
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


# ============================================================================
# LOOKUP TABLE BUILDERS
# ============================================================================

def build_lookup_tables(session):
    """
    Build and return lookup dictionaries for reference tables.
    
    Returns:
        dict: Dictionary containing all lookup tables
    """
    lookups = {
        "taxation_class": {},
        "fuel_type": {},
        "licensing_authority": {},
        "emission_band": {},
        "registration_type": {},
    }
    
    # Get existing records
    for tax_class in session.query(TaxationClass).all():
        lookups["taxation_class"][tax_class.name] = tax_class.id
    
    for fuel in session.query(FuelType).all():
        lookups["fuel_type"][fuel.name] = fuel.id
    
    for authority in session.query(LicensingAuthority).all():
        lookups["licensing_authority"][authority.name] = authority.id
    
    for band in session.query(EmissionBand).all():
        lookups["emission_band"][band.band] = band.id
    
    for reg_type in session.query(VehicleRegistrationType).all():
        lookups["registration_type"][reg_type.name] = reg_type.id
    
    return lookups


def get_or_create_id(session, lookups, table_name, model_class, value_field, value):
    """
    Get ID from lookup or create new record if doesn't exist.
    
    Args:
        session: Database session
        lookups: Lookup dictionaries
        table_name: Name of the lookup table
        model_class: SQLAlchemy model class
        value_field: Field name for the value
        value: Value to look up or create
        
    Returns:
        int: ID of the record
    """
    value = value.strip()
    
    if value in lookups[table_name]:
        return lookups[table_name][value]
    
    # Create new record
    new_record = model_class(**{value_field: value})
    session.add(new_record)
    session.flush()  # Get the ID without committing
    
    lookups[table_name][value] = new_record.id
    return new_record.id


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


def parse_vehicle_first_time_row(row: dict[str, str], session, lookups) -> VehicleFirstTime:
    """Parse vehicle first time licensing row."""
    taxation_class_id = get_or_create_id(
        session, lookups, "taxation_class", TaxationClass, "name", row["Taxation class"]
    )
    
    return VehicleFirstTime(
        month=parse_month_year(row["Month"]),
        taxation_class_id=taxation_class_id,
        count=int(row["Number"]),
    )


def parse_vehicle_licensing_area_row(row: dict[str, str], session, lookups) -> VehicleLicensingArea:
    """Parse vehicle licensing by area row."""
    authority_id = get_or_create_id(
        session, lookups, "licensing_authority", LicensingAuthority, "name", row["Licensing Authority"]
    )
    
    fuel_type_id = get_or_create_id(
        session, lookups, "fuel_type", FuelType, "name", row["Types of Fuel"]
    )
    
    return VehicleLicensingArea(
        month=parse_month_year(row["Month"]),
        authority_id=authority_id,
        fuel_type_id=fuel_type_id,
        count=int(row["Number"]),
    )


def parse_vehicle_new_licensed_row(row: dict[str, str], session, lookups) -> VehicleNewLicensed:
    """Parse new vehicle licensing row."""
    registration_type_id = get_or_create_id(
        session, lookups, "registration_type", VehicleRegistrationType, "name", row["Type of Vehicle Registration"]
    )
    
    fuel_type_id = get_or_create_id(
        session, lookups, "fuel_type", FuelType, "name", row["Types of Fuel"]
    )
    
    return VehicleNewLicensed(
        month=parse_month_year(row["Month"]),
        registration_type_id=registration_type_id,
        fuel_type_id=fuel_type_id,
        count=int(row["Number"]),
    )


def parse_vehicle_yearly_row(row: dict[str, str], session, lookups) -> VehicleYearly:
    """Parse yearly vehicle data row."""
    taxation_class_id = get_or_create_id(
        session, lookups, "taxation_class", TaxationClass, "name", row["Taxation Class"]
    )
    
    fuel_type_id = get_or_create_id(
        session, lookups, "fuel_type", FuelType, "name", row["Types of Fuel"]
    )
    
    return VehicleYearly(
        year=parse_year(row["Year"]),
        taxation_class_id=taxation_class_id,
        fuel_type_id=fuel_type_id,
        count=int(row["Number"]),
    )


def parse_private_car_emission_row(row: dict[str, str], session, lookups) -> PrivateCarEmission:
    """Parse private car emission data row."""
    emission_band_id = get_or_create_id(
        session, lookups, "emission_band", EmissionBand, "band", row["Emission Band"]
    )
    
    authority_id = get_or_create_id(
        session, lookups, "licensing_authority", LicensingAuthority, "name", row["Licensing Authority"]
    )
    
    return PrivateCarEmission(
        year=parse_year(row["Year"]),
        emission_band_id=emission_band_id,
        authority_id=authority_id,
        count=int(row["Value"]),
    )


def parse_ev_charging_point_row(row: dict[str, str]) -> EVChargingPoint | None:
    """
    Parse EV charging point row.
    
    Returns None if county is not Dublin (as per requirements).
    """
    county = row["County"].strip()
    
    # Only process Dublin charging points
    if county.lower() != "dublin":
        return None
    
    address = row.get("Address", "").strip() or None
    
    return EVChargingPoint(
        county=county,
        address=address,
        lat=float(row["Latitude"]),
        lon=float(row["Longitude"]),
        max_sim_ccs=safe_int(row.get("max__sim__ccs")),
        max_sim_chademo=safe_int(row.get("max__sim__chademo")),
        max_sim_fast_ac=safe_int(row.get("max__sim__fast_ac")),
        max_sim_ac_socket=safe_int(row.get("max__sim__ac_socket")),
        ccs_kw=row.get("CCS kWs", "").strip() or None,
        chademo_kw=row.get("CHAdeMO kWs", "").strip() or None,
        ac_fast_kw=row.get("AC Fast kWs", "").strip() or None,
        ac_socket_kw=row.get("AC Socket kWs", "").strip() or None,
        open_hours=row.get("Open Hours", "").strip() or None,
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

    # CSV file name -> (required headers, transform row function, needs_lookups)
    csv_files = {
        "scats_sites.csv": (
            ["SiteID", "Site Description", "Site Description Lower", "Region", "Lat", "Long"],
            parse_scats_site_row,
            False,
        ),
        "traffic_volumes.csv": (
            ["End_Time", "Region", "Site", "Detector", "Sum_Volume", "Avg_Volume"],
            parse_traffic_volume_row,
            False,
        ),
        "vehicle_first_time.csv": (
            ["Month", "Taxation class", "Number"],
            parse_vehicle_first_time_row,
            True,
        ),
        "vehicle_licensing_area.csv": (
            ["Month", "Licensing Authority", "Types of Fuel", "Number"],
            parse_vehicle_licensing_area_row,
            True,
        ),
        "vehicle_new_licensed.csv": (
            ["Month", "Type of Vehicle Registration", "Types of Fuel", "Number"],
            parse_vehicle_new_licensed_row,
            True,
        ),
        "vehicle_yearly.csv": (
            ["Year", "Taxation Class", "Types of Fuel", "Number"],
            parse_vehicle_yearly_row,
            True,
        ),
        "private_car_emissions.csv": (
            ["Year", "Emission Band", "Licensing Authority", "Value"],
            parse_private_car_emission_row,
            True,
        ),
        "ev_charging_points.csv": (
            ["County", "Latitude", "Longitude"],
            parse_ev_charging_point_row,
            False,
        ),
    }

    # Validate all files exist before processing
    logger.info("Validating CSV files...")
    for filename in csv_files:
        file_path = data_dir / filename
        if not file_path.exists():
            msg = f"Required CSV file not found: {file_path}"
            raise FileNotFoundError(msg)

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
        
        # Delete lookup tables
        session.execute(delete(TaxationClass))
        session.execute(delete(FuelType))
        session.execute(delete(LicensingAuthority))
        session.execute(delete(EmissionBand))
        session.execute(delete(VehicleRegistrationType))
        
        # Delete sites last
        session.execute(delete(ScatsSite))
        
        session.commit()

        # Build lookup tables
        lookups = build_lookup_tables(session)

        # Process each CSV file
        for filename, (required_headers, transform_row, needs_lookups) in csv_files.items():
            logger.info("Processing %s...", filename)
            file_path = data_dir / filename
            
            rows = []
            for csv_row in read_csv_file(file_path, required_headers):
                if needs_lookups:
                    parsed_row = transform_row(csv_row, session, lookups)
                else:
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