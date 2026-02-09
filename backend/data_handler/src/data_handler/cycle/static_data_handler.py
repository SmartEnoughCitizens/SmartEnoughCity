"""Handler for processing static station information from JCDecaux GBFS API."""

import logging
from decimal import Decimal

from sqlalchemy import delete

from data_handler.cycle.api_client import get_jcdecaux_client
from data_handler.cycle.models import DublinBikesStation
from data_handler.db import SessionLocal

logger = logging.getLogger(__name__)


def parse_station_information_record(record: dict) -> DublinBikesStation:
    """Transform API station information record to model instance."""
    return DublinBikesStation(
        station_id=int(record["station_id"]),
        system_id="dublin",
        name=record["name"],
        short_name=record.get("short_name"),
        address=record.get("address"),
        latitude=Decimal(str(record["lat"])),
        longitude=Decimal(str(record["lon"])),
        capacity=int(record["capacity"]),
        region_id=record.get("region_id"),
    )


def process_station_information() -> None:
    """
    Fetch and store static station information from JCDecaux GBFS API.

    This function:
    1. Fetches station information from API
    2. Deletes existing station records
    3. Inserts fresh data
    4. All operations in a single transaction
    """
    logger.info("Processing static station information...")

    client = get_jcdecaux_client()
    stations_data = client.fetch_station_information()

    session = SessionLocal()
    try:
        logger.info("Deleting existing station records...")
        session.execute(delete(DublinBikesStation))

        stations = [
            parse_station_information_record(record) for record in stations_data
        ]
        session.add_all(stations)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info("Successfully processed %d stations.", len(stations))

    except Exception:
        session.rollback()
        logger.exception("Error processing station information")
        raise

    finally:
        session.close()
