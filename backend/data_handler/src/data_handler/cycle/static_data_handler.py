"""Handler for processing static station information from JCDecaux GBFS API."""

import logging
from decimal import Decimal

from sqlalchemy import text

from data_handler.cycle.api_client import get_jcdecaux_client
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings

logger = logging.getLogger(__name__)


def parse_station_information_record(record: dict) -> dict:
    """Transform API station information record to a dict for upsert."""
    return {
        "station_id": int(record["station_id"]),
        "system_id": "dublin",
        "name": record["name"],
        "short_name": record.get("short_name"),
        "address": record.get("address"),
        "latitude": Decimal(str(record["lat"])),
        "longitude": Decimal(str(record["lon"])),
        "capacity": int(record["capacity"]),
        "region_id": record.get("region_id"),
    }


def process_station_information() -> None:
    """
    Fetch and store static station information from JCDecaux GBFS API.

    This function:
    1. Fetches station information from API
    2. Upserts station records (insert or update on conflict)
    3. Removes stations no longer present in the API response
    4. All operations in a single transaction
    """
    logger.info("Processing static station information...")

    client = get_jcdecaux_client()
    stations_data = client.fetch_station_information()

    records = [parse_station_information_record(r) for r in stations_data]

    schema = get_db_settings().postgres_schema
    table = f"{schema}.dublin_bikes_stations" if schema else "dublin_bikes_stations"

    upsert_sql = f"""
    INSERT INTO {table}
        (station_id, system_id, name, short_name, address, latitude, longitude, capacity, region_id)
    VALUES
        (:station_id, :system_id, :name, :short_name, :address, :latitude, :longitude, :capacity, :region_id)
    ON CONFLICT (station_id) DO UPDATE SET
        system_id = EXCLUDED.system_id,
        name = EXCLUDED.name,
        short_name = EXCLUDED.short_name,
        address = EXCLUDED.address,
        latitude = EXCLUDED.latitude,
        longitude = EXCLUDED.longitude,
        capacity = EXCLUDED.capacity,
        region_id = EXCLUDED.region_id,
        updated_at = NOW();
    """

    session = SessionLocal()
    try:
        if records:
            session.execute(text(upsert_sql), records)

            # Remove stations no longer in API response
            active_ids = [r["station_id"] for r in records]
            session.execute(
                text(f"DELETE FROM {table} WHERE station_id != ALL(:ids)"),
                {"ids": active_ids},
            )

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed %d stations.", len(records))

    except Exception:
        session.rollback()
        logger.exception("Error processing station information")
        raise

    finally:
        session.close()
