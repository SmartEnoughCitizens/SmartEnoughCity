"""Handler for processing static station information from JCDecaux GBFS API."""

import logging
from decimal import Decimal

from sqlalchemy.dialects.postgresql import insert as pg_insert

from data_handler.cycle.api_client import get_jcdecaux_client
from data_handler.cycle.models import DublinBikesStation
from data_handler.db import SessionLocal

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

    session = SessionLocal()
    try:
        if records:
            stmt = pg_insert(DublinBikesStation).values(records)
            stmt = stmt.on_conflict_do_update(
                index_elements=["station_id"],
                set_={
                    "system_id": stmt.excluded.system_id,
                    "name": stmt.excluded.name,
                    "short_name": stmt.excluded.short_name,
                    "address": stmt.excluded.address,
                    "latitude": stmt.excluded.latitude,
                    "longitude": stmt.excluded.longitude,
                    "capacity": stmt.excluded.capacity,
                    "region_id": stmt.excluded.region_id,
                },
            )
            session.execute(stmt)

            # Remove stations no longer in API response
            active_ids = [r["station_id"] for r in records]
            session.query(DublinBikesStation).filter(
                DublinBikesStation.station_id.notin_(active_ids)
            ).delete(synchronize_session=False)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed %d stations.", len(records))

    except Exception:
        session.rollback()
        logger.exception("Error processing station information")
        raise

    finally:
        session.close()
