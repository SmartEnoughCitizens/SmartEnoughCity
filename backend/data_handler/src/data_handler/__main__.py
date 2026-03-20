import argparse
import logging

from data_handler.bus.live_data_handler import process_bus_live_data
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.car.process_car_data import process_car_static_data
from data_handler.cycle.realtime_handler import fetch_and_store_station_snapshots
from data_handler.cycle.static_data_handler import process_station_information
from data_handler.db import Base, engine
from data_handler.events.data_handler import (
    fetch_and_store_events,
    fetch_and_store_venues,
)
from data_handler.logging import configure_logging
from data_handler.settings.data_sources_settings import get_data_sources_settings
from data_handler.settings.database_settings import get_db_settings
from data_handler.train.realtime_handler import irish_rail_realtime_to_db
from data_handler.train.static_data_handler import process_train_static_data
from data_handler.tram.forecast_handler import luas_forecasts_to_db
from data_handler.tram.static_data_handler import process_tram_static_data


def get_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="SmartEnoughCity Data Handler")
    parser.add_argument(
        "--static", action="store_true", help="Process static data only"
    )
    return parser.parse_args()


def init_db() -> None:
    Base.metadata.create_all(bind=engine)


def main_static() -> None:
    logger = logging.getLogger(__name__)

    logger.info("Processing static data...")

    sources_settings = get_data_sources_settings()

    if sources_settings.enable_bus_data and sources_settings.bus_gtfs_static_data_dir:
        process_bus_static_data(sources_settings.bus_gtfs_static_data_dir)
    else:
        logger.info("Skipping bus static data processing...")

    if sources_settings.enable_car_data and sources_settings.car_static_data_dir:
        process_car_static_data(sources_settings.car_static_data_dir)
    else:
        logger.info("Skipping car static data processing...")

    if sources_settings.enable_cycle_data:
        logger.info("Processing Dublin Bikes static station information...")
        process_station_information()
    else:
        logger.info("Skipping cycle static data processing...")

    if (
        sources_settings.enable_train_data
        and sources_settings.train_gtfs_static_data_dir
    ):
        logger.info("Processing train static data...")
        process_train_static_data(sources_settings.train_gtfs_static_data_dir)
    else:
        logger.info("Skipping train static data processing...")

    if sources_settings.enable_events_data:
        logger.info("Seeding event venues from Ticketmaster...")
        fetch_and_store_venues()
    else:
        logger.info("Skipping events venue seeding...")

    if sources_settings.enable_tram_data and sources_settings.tram_gtfs_static_data_dir:
        logger.info("Processing tram static data...")
        process_tram_static_data(
            sources_settings.tram_gtfs_static_data_dir,
            sources_settings.tram_cso_static_data_dir,
        )
    else:
        logger.info("Skipping tram static data processing...")

    logger.info("Finished processing static data.")


def main_dynamic() -> None:
    logger = logging.getLogger(__name__)
    logger.info("Processing dynamic data...")

    db_settings = get_db_settings()
    sources_settings = get_data_sources_settings()

    logger.info(
        "Connected to database: %s at %s:%s",
        db_settings.name,
        db_settings.host,
        db_settings.port,
    )
    logger.info("Data sources enabled:")
    logger.info("  - Cycle data: %s", sources_settings.enable_cycle_data)
    logger.info("  - Car data: %s", sources_settings.enable_car_data)
    logger.info("  - Bus data: %s", sources_settings.enable_bus_data)
    logger.info("  - Train data: %s", sources_settings.enable_train_data)
    logger.info("  - Tram data: %s", sources_settings.enable_tram_data)
    logger.info("  - Construction data: %s", sources_settings.enable_construction_data)
    logger.info("  - Events data: %s", sources_settings.enable_events_data)

    # Process data sources based on enabled toggles
    if sources_settings.enable_train_data:
        logger.info("Processing train data...")
        irish_rail_realtime_to_db()

    if sources_settings.enable_cycle_data:
        logger.info("Processing cycle data...")
        fetch_and_store_station_snapshots()

    if sources_settings.enable_car_data:
        logger.info("Processing car data...")

    if sources_settings.enable_bus_data:
        logger.info("Processing bus data...")
        process_bus_live_data()

    if sources_settings.enable_tram_data:
        logger.info("Processing tram data...")
        luas_forecasts_to_db()

    if sources_settings.enable_construction_data:
        logger.info("Processing construction data...")

    if sources_settings.enable_events_data:
        logger.info("Processing events data...")
        fetch_and_store_events()


def main() -> None:
    configure_logging()
    logger = logging.getLogger(__name__)

    args = get_args()

    logger.info("Initializing the database...")
    init_db()
    logger.info("Database initialized successfully.")

    if args.static:
        main_static()
    else:
        main_dynamic()


if __name__ == "__main__":
    main()
