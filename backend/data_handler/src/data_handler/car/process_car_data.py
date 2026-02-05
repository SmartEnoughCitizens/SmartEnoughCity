# data_handler/car/process_car_data.py

import logging
from pathlib import Path

from sqlalchemy import delete

from data_handler.car.car_parsing_utils import (
    parse_scats_time,
    parse_year,
    safe_int,
)
from data_handler.car.models import (
    EmissionBand,
    ScatsSite,
    TrafficVolume,
    PrivateCarEmission,
    EVChargingPoint,
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


def parse_private_car_emission_row(row: dict[str, str]) -> PrivateCarEmission:
    """Parse private car emission data row."""
    emission_band_str = row["Emission Band"].strip()
    
    # Convert string to enum
    try:
        emission_band = EmissionBand(emission_band_str)
    except ValueError as e:
        msg = f"Invalid emission band: {emission_band_str}. Must be one of: {', '.join([b.value for b in EmissionBand])}"
        raise ValueError(msg) from e
    
    licensing_authority = row["Licensing Authority"].strip()
    
    return PrivateCarEmission(
        year=parse_year(row["Year"]),
        emission_band=emission_band,
        licensing_authority=licensing_authority,
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

    # CSV file name -> (required headers, transform row function)
    csv_files = {
        "scats_sites.csv": (
            ["SiteID", "Site Description", "Site Description Lower", "Region", "Lat", "Long"],
            parse_scats_site_row,
        ),
        "traffic_volumes.csv": (
            ["End_Time", "Region", "Site", "Detector", "Sum_Volume", "Avg_Volume"],
            parse_traffic_volume_row,
        ),
        "private_car_emissions.csv": (
            ["Year", "Emission Band", "Licensing Authority", "Value"],
            parse_private_car_emission_row,
        ),
        "ev_charging_points.csv": (
            ["County", "Latitude", "Longitude"],
            parse_ev_charging_point_row,
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