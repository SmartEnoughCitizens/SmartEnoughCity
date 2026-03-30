# data_handler/car/process_car_data.py

import json
import logging
from collections.abc import Callable
from datetime import datetime
from pathlib import Path

from shapely.geometry import MultiPolygon, Polygon, shape
from sqlalchemy import delete
from sqlalchemy.orm import Session

from data_handler.car.car_parsing_utils import (
    parse_kw_value,
    parse_month_year,
    parse_open_hours,
    parse_scats_time,
    parse_year,
)
from data_handler.car.models import (
    EmissionBand,
    EVChargingDemand,
    EVChargingPoint,
    EVElectoralDivision,
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
    """Parse DCC traffic signals site row."""
    return ScatsSite(
        site_id=int(row["SiteID"]),
        description=row["Site_Description_Cap"].strip(),
        description_lower=row["Site_Description_Lower"].strip(),
        region=row["Region"].strip(),
        lat=float(row["Lat"]),
        lon=float(row["Long"]),
    )


def parse_traffic_volume_row(row: dict[str, str]) -> TrafficVolume | None:
    """Parse a SCATS traffic volume CSV row into a TrafficVolume object.

    Returns None for malformed rows (e.g. truncated lines where DictReader
    fills missing fields with None).
    """
    if any(
        row.get(col) is None
        for col in ("End_Time", "Site", "Detector", "Sum_Volume", "Avg_Volume")
    ):
        logger.warning("Skipping malformed traffic volume row: %s", row)
        return None
    try:
        return TrafficVolume(
            end_time=parse_scats_time(row["End_Time"]),
            site_id=int(row["Site"]),
            detector=int(row["Detector"]),
            region=row["Region"].strip(),
            sum_volume=int(row["Sum_Volume"]),
            avg_volume=int(row["Avg_Volume"]),
        )
    except (ValueError, KeyError):
        logger.warning("Skipping unparseable traffic volume row: %s", row)
        return None


def parse_vehicle_first_time_row(row: dict[str, str]) -> VehicleFirstTime | None:
    """Parse vehicle first time licensing row. Returns None for unknown enum values or empty data."""
    if not row.get("VALUE", "").strip():
        return None

    taxation_class_str = row["Taxation Class"].strip()

    try:
        taxation_class = TaxationClass(taxation_class_str)
    except ValueError:
        logger.warning("Skipping unknown taxation class: %s", taxation_class_str)
        return None

    return VehicleFirstTime(
        month=parse_month_year(row["Month"]),
        taxation_class=taxation_class,
        count=int(row["VALUE"]),
    )


def parse_vehicle_licensing_area_row(
    row: dict[str, str],
) -> VehicleLicensingArea | None:
    """Parse vehicle licensing by area row. Returns None for unknown enum values or empty data."""
    if not row.get("VALUE", "").strip():
        return None

    fuel_type_str = row["Type of Fuel"].strip()

    try:
        fuel_type = FuelType(fuel_type_str)
    except ValueError:
        logger.warning("Skipping unknown fuel type: %s", fuel_type_str)
        return None

    return VehicleLicensingArea(
        month=parse_month_year(row["Month"]),
        licensing_authority=row["Licensing Authority"].strip(),
        fuel_type=fuel_type,
        count=int(row["VALUE"]),
    )


def parse_vehicle_new_licensed_row(row: dict[str, str]) -> VehicleNewLicensed | None:
    """Parse new vehicle licensing row. Returns None for unknown enum values or empty data."""
    if not row.get("VALUE", "").strip():
        return None

    registration_type_str = row["Type of Vehicle Registration"].strip()
    fuel_type_str = row["Type of Fuel"].strip()

    try:
        registration_type = VehicleRegistrationType(registration_type_str)
    except ValueError:
        logger.warning("Skipping unknown registration type: %s", registration_type_str)
        return None

    try:
        fuel_type = FuelType(fuel_type_str)
    except ValueError:
        logger.warning("Skipping unknown fuel type: %s", fuel_type_str)
        return None

    year = parse_year(row["Year"])
    return VehicleNewLicensed(
        month=datetime(year, 1, 1),
        registration_type=registration_type,
        fuel_type=fuel_type,
        count=int(row["VALUE"]),
    )


def parse_vehicle_yearly_row(row: dict[str, str]) -> VehicleYearly | None:
    """Parse yearly vehicle data row. Returns None for unknown enum values or empty data."""
    if not row.get("VALUE", "").strip():
        return None

    taxation_class_str = row["Taxation Class"].strip()
    fuel_type_str = row["Type of Fuel"].strip()

    try:
        taxation_class = TaxationClass(taxation_class_str)
    except ValueError:
        logger.warning("Skipping unknown taxation class: %s", taxation_class_str)
        return None

    try:
        fuel_type = FuelType(fuel_type_str)
    except ValueError:
        logger.warning("Skipping unknown fuel type: %s", fuel_type_str)
        return None

    return VehicleYearly(
        year=parse_year(row["Year"]),
        taxation_class=taxation_class,
        fuel_type=fuel_type,
        count=int(row["VALUE"]),
    )


def parse_private_car_emission_row(row: dict[str, str]) -> PrivateCarEmission | None:
    """
    Parse private car emission data row.

    Filters to only summary rows (all engine capacities + all makes).
    Returns None for non-summary rows or unknown enum values.
    """
    if not row.get("VALUE", "").strip():
        return None

    engine_capacity = row.get("Engine Capacity cc", "").strip()
    car_make = row.get("Car Make", "").strip()

    # Only include all-makes / all-engine-capacity summary rows
    if (
        not engine_capacity.lower().startswith("all engine capacities")
        or car_make.lower() != "all makes"
    ):
        return None

    emission_band_str = row["Emission Band"].strip()
    # Normalize double spaces (e.g. "Band  A" -> "Band A")
    emission_band_str = " ".join(emission_band_str.split())

    try:
        emission_band = EmissionBand(emission_band_str)
    except ValueError:
        logger.warning("Skipping unknown emission band: %s", emission_band_str)
        return None

    return PrivateCarEmission(
        year=parse_year(row["Year"]),
        emission_band=emission_band,
        licensing_authority=row["Licensing Authority"].strip(),
        count=int(row["VALUE"]),
    )


def parse_and_filter_ev_charging_point_row(
    row: dict[str, str],
) -> EVChargingPoint | None:
    """
    Parse EV charging point row.

    Returns None if county is not Dublin (as per requirements).
    """
    county = str(row.get("County", "")).strip()

    # Only process Dublin charging points
    if county.lower() != "dublin":
        return None

    # Parse operating hours (returns tuple of (is_24_7, description))
    open_hours_str = str(row.get("Open Hours", ""))
    is_24_7, _hours_description = parse_open_hours(open_hours_str)

    charger_count_str = str(row.get("Nr. Chargers", "1")).strip()
    charger_count = int(charger_count_str) if charger_count_str.isdigit() else 1

    return EVChargingPoint(
        address=str(row.get("Address", "")).strip() or None,
        county="Dublin",  # Hardcoded for consistency
        lat=float(row["Latitude"]),
        lon=float(row["Longitude"]),
        charger_count=charger_count,
        power_rating_of_ccs_connectors_kw=parse_kw_value(str(row.get("CCS kWs", ""))),
        power_rating_of_chademo_connectors_kw=parse_kw_value(
            str(row.get("CHAdeMO kWs", ""))
        ),
        power_rating_of_ac_fast_kw=parse_kw_value(str(row.get("AC Fast kWs", ""))),
        power_rating_of_standard_ac_socket_kw=parse_kw_value(
            str(row.get("AC Socket kWs", ""))
        ),
        is_24_7=is_24_7,
    )


# ============================================================================
# MAIN PROCESSING FUNCTION
# ============================================================================

_CsvTransformFn = Callable[[dict[str, str]], object]

_CSV_FILES: dict[str, tuple[list[str], _CsvTransformFn]] = {
    "dcc_traffic_signals_20221130.csv": (
        [
            "SiteID",
            "Site_Description_Cap",
            "Site_Description_Lower",
            "Region",
            "Lat",
            "Long",
        ],
        parse_scats_site_row,
    ),
    "Vehicles Licensed for the first time.csv": (
        ["Month", "Taxation Class", "VALUE"],
        parse_vehicle_first_time_row,
    ),
    "New and second hand car by licensing area.csv": (
        ["Month", "Licensing Authority", "Type of Fuel", "VALUE"],
        parse_vehicle_licensing_area_row,
    ),
    "New Vehicles licensed for first time.csv": (
        ["Year", "Type of Vehicle Registration", "Type of Fuel", "VALUE"],
        parse_vehicle_new_licensed_row,
    ),
    "New and Second Hand Vehicles.csv": (
        ["Year", "Taxation Class", "Type of Fuel", "VALUE"],
        parse_vehicle_yearly_row,
    ),
}

_EMISSION_FILES = [
    "Private Cars Licensed for the First Time - 1998-2013.csv",
    "Private Cars Licensed for the First Time - 2014-15.csv",
    "Private Cars Licensed for the First Time - 2017-2025.csv",
]

_EMISSION_REQUIRED_HEADERS = [
    "Engine Capacity cc",
    "Car Make",
    "Emission Band",
    "Licensing Authority",
    "Year",
    "VALUE",
]

_EV_CSV_FILE = "ESB-_EV-charge-point-locations.csv"
_EV_CSV_REQUIRED_HEADERS = ["County", "Latitude", "Longitude"]
_EV_GEOJSON_FILE = "location_data.geojson"

_CHARGING_DEMAND_CSV_FILE = "charging_demand.csv"
_CHARGING_DEMAND_CSV_REQUIRED_HEADERS = [
    "CSO Electoral Divisions 2022",
    "Bed-Sit",
    "Flat/Apartment",
    "House/Bungalow",
    "Total",
    "Registered_ev",
    "Bed-Sit_Percentage",
    "Flat/Apartment_Percentage",
    "House/Bungalow_Percentage",
    "home_charge_percentage",
    "charge_frequency",
    "charging_demand",
]

_TRAFFIC_VOLUME_FILE = "SCATSAugust2025.csv"
_TRAFFIC_VOLUME_REQUIRED_HEADERS = [
    "End_Time",
    "Region",
    "Site",
    "Detector",
    "Sum_Volume",
    "Avg_Volume",
]
_TRAFFIC_VOLUME_CHUNK_SIZE = 10_000


def _process_charging_demand(session: Session, path: Path) -> None:
    """Read charging_demand.csv and bulk-add rows to session."""
    logger.info("Processing %s...", path.name)
    rows = [
        EVChargingDemand(
            electoral_division=row["CSO Electoral Divisions 2022"].strip(),
            bed_sit_count=int(row["Bed-Sit"]),
            flat_apartment_count=int(row["Flat/Apartment"]),
            house_bungalow_count=int(row["House/Bungalow"]),
            total_dwellings=int(row["Total"]),
            registered_ev=float(row["Registered_ev"]) if row["Registered_ev"] else None,
            bed_sit_pct=float(row["Bed-Sit_Percentage"]),
            flat_apartment_pct=float(row["Flat/Apartment_Percentage"]),
            house_bungalow_pct=float(row["House/Bungalow_Percentage"]),
            home_charge_pct=float(row["home_charge_percentage"]),
            charge_frequency=float(row["charge_frequency"]),
            charging_demand=float(row["charging_demand"]),
        )
        for row in read_csv_file(path, _CHARGING_DEMAND_CSV_REQUIRED_HEADERS)
    ]
    session.add_all(rows)
    logger.info("  Added %d charging demand rows", len(rows))


def _validate_files(data_dir: Path, ev_csv_path: Path, ev_geojson_path: Path) -> None:
    """Raise if data_dir is None or any required file is missing."""
    if data_dir is None:
        msg = "data_dir cannot be None"
        raise ValueError(msg)

    for filename in list(_CSV_FILES) + _EMISSION_FILES:
        file_path = data_dir / filename
        if not file_path.exists():
            msg = f"Required file not found: {file_path}"
            raise FileNotFoundError(msg)

    if not ev_csv_path.exists():
        msg = f"Required file not found: {ev_csv_path}"
        raise FileNotFoundError(msg)

    if not ev_geojson_path.exists():
        msg = f"Required file not found: {ev_geojson_path}"
        raise FileNotFoundError(msg)

    charging_demand_path = data_dir / _CHARGING_DEMAND_CSV_FILE
    if not charging_demand_path.exists():
        msg = f"Required file not found: {charging_demand_path}"
        raise FileNotFoundError(msg)


def _clear_existing_data(session: Session) -> None:
    """Delete all existing car data in dependency order."""
    logger.info("Deleting existing data...")
    for model in (
        TrafficVolume,
        VehicleFirstTime,
        VehicleLicensingArea,
        VehicleNewLicensed,
        VehicleYearly,
        PrivateCarEmission,
        EVChargingPoint,
        EVChargingDemand,
        EVElectoralDivision,
        ScatsSite,
    ):
        session.execute(delete(model))
    session.commit()


def _to_wkt_multipolygon(geometry: dict) -> str:
    """Convert a GeoJSON geometry dict to a WKT MULTIPOLYGON string."""
    s_geom = shape(geometry)
    if isinstance(s_geom, Polygon):
        s_geom = MultiPolygon([s_geom])
    return f"SRID=4326;{s_geom.wkt}"


def _process_ev_geojson(session: Session, geojson_path: Path) -> None:
    """Read the electoral division GeoJSON and bulk-add Dublin boundary rows."""
    logger.info("Processing %s...", geojson_path.name)
    with geojson_path.open(encoding="utf-8") as f:
        geojson = json.load(f)

    rows = [
        EVElectoralDivision(
            ed_english=feature["properties"]["ED_ENGLISH"],
            county_english=feature["properties"]["COUNTY_ENGLISH"],
            geom=_to_wkt_multipolygon(feature["geometry"]),
        )
        for feature in geojson["features"]
        if (feature["properties"].get("COUNTY_ENGLISH") or "").upper()
        in {"DUBLIN CITY", "FINGAL", "SOUTH DUBLIN"}
    ]

    session.add_all(rows)
    logger.info("  Added %d electoral division boundaries", len(rows))


def _process_csv_file(
    session: Session,
    data_dir: Path,
    filename: str,
    required_headers: list[str],
    transform_row: _CsvTransformFn,
) -> None:
    """Parse one CSV file and bulk-add rows to *session*."""
    logger.info("Processing %s...", filename)
    file_path = data_dir / filename

    rows = []
    for csv_row in read_csv_file(file_path, required_headers):
        parsed_row = transform_row(csv_row)
        if parsed_row is not None:
            rows.append(parsed_row)

    # Deduplicate scats_sites by site_id — source CSV has some Signal Site
    # entries followed by the proper SCATS Site entry for the same ID.
    # Keep the last occurrence (the SCATS Site with a valid region).
    if filename == "dcc_traffic_signals_20221130.csv":
        deduped: dict[int, ScatsSite] = {}
        for site in rows:
            deduped[site.site_id] = site
        rows = list(deduped.values())

    session.add_all(rows)
    logger.info("  Added %d rows from %s", len(rows), filename)


def _process_emission_files(session: Session, data_dir: Path) -> None:
    """Combine all emission CSVs and bulk-add rows to *session*."""
    logger.info("Processing private car emission files...")
    emission_rows = []
    for filename in _EMISSION_FILES:
        file_path = data_dir / filename
        for csv_row in read_csv_file(file_path, _EMISSION_REQUIRED_HEADERS):
            parsed_row = parse_private_car_emission_row(csv_row)
            if parsed_row is not None:
                emission_rows.append(parsed_row)

    session.add_all(emission_rows)
    logger.info(
        "  Added %d emission rows from %d files",
        len(emission_rows),
        len(_EMISSION_FILES),
    )


def _process_traffic_volumes(session: Session, data_dir: Path) -> None:
    """Stream SCATS traffic volume CSV and bulk-insert in chunks.

    The file can be very large (10M+ rows), so rows are inserted in chunks
    rather than accumulating all objects in memory at once.
    The file is optional — if not present, processing is skipped.
    """
    file_path = data_dir / _TRAFFIC_VOLUME_FILE
    if not file_path.exists():
        logger.info(
            "No traffic volume file found (%s), skipping.", _TRAFFIC_VOLUME_FILE
        )
        return

    logger.info("Processing %s (this may take a while)...", _TRAFFIC_VOLUME_FILE)

    session.flush()  # ensure ScatsSites inserted earlier are visible to the query below
    known_site_ids = {site_id for (site_id,) in session.query(ScatsSite.site_id).all()}
    logger.info("  Filtering to %d known SCATS sites", len(known_site_ids))

    chunk: list[TrafficVolume] = []
    total = 0
    skipped = 0

    for csv_row in read_csv_file(file_path, _TRAFFIC_VOLUME_REQUIRED_HEADERS):
        parsed_row = parse_traffic_volume_row(csv_row)
        if parsed_row is None:
            continue
        if parsed_row.site_id not in known_site_ids:
            skipped += 1
            continue
        chunk.append(parsed_row)
        if len(chunk) >= _TRAFFIC_VOLUME_CHUNK_SIZE:
            session.add_all(chunk)
            session.commit()
            total += len(chunk)
            chunk = []
            logger.info("  Committed %d rows...", total)

    if chunk:
        session.add_all(chunk)
        session.flush()
        total += len(chunk)

    logger.info(
        "  Added %d rows from %s (%d skipped — unknown sites)",
        total,
        _TRAFFIC_VOLUME_FILE,
        skipped,
    )


def _process_ev_charging_points(session: Session, ev_csv_path: Path) -> None:
    """Read CSV and bulk-add Dublin EV charging point rows to *session*."""
    logger.info("Processing %s...", ev_csv_path.name)
    ev_rows = []
    for csv_row in read_csv_file(ev_csv_path, _EV_CSV_REQUIRED_HEADERS):
        parsed_row = parse_and_filter_ev_charging_point_row(csv_row)
        if parsed_row is not None:
            ev_rows.append(parsed_row)

    session.add_all(ev_rows)
    logger.info("  Added %d EV charging point rows", len(ev_rows))


def process_car_static_data(data_dir: Path) -> None:
    """
    Process car static data from source files.

    This function:
    1. Validates all required files exist
    2. Deletes all existing data from relevant tables
    3. Reads and parses data from source files
    4. Inserts the data into the database
    5. All operations happen within a single transaction

    Args:
        data_dir: Path to the directory containing car static data files.

    Raises:
        FileNotFoundError: If any required file is missing
        ValueError: If any CSV file is missing required headers or row data is invalid
    """
    ev_csv_path = (
        data_dir / _EV_CSV_FILE if data_dir is not None else Path(_EV_CSV_FILE)
    )
    ev_geojson_path = data_dir / _EV_GEOJSON_FILE

    logger.info("Validating data files...")
    _validate_files(data_dir, ev_csv_path, ev_geojson_path)

    logger.info("Processing static car data...")

    session = SessionLocal()
    try:
        _clear_existing_data(session)

        for filename, (required_headers, transform_row) in _CSV_FILES.items():
            _process_csv_file(
                session, data_dir, filename, required_headers, transform_row
            )

        _process_emission_files(session, data_dir)
        _process_ev_charging_points(session, ev_csv_path)
        _process_ev_geojson(session, ev_geojson_path)
        _process_charging_demand(session, data_dir / _CHARGING_DEMAND_CSV_FILE)
        _process_traffic_volumes(session, data_dir)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed static car data.")

    except Exception:
        session.rollback()
        logger.exception("Error processing static car data")
        raise

    finally:
        session.close()
