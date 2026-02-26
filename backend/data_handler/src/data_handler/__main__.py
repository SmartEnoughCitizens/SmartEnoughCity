import argparse
import logging

from data_handler.bus.live_data_handler import process_bus_live_data
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.car.process_car_data import process_car_static_data
from data_handler.cycle.csv_import_handler import import_all_station_history_csvs
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


def get_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="SmartEnoughCity Data Handler")
    parser.add_argument(
        "--static", action="store_true", help="Process static data only"
    )
    parser.add_argument(
        "--import-history",
        action="store_true",
        help="Import historical Dublin Bikes CSV archives",
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

    if sources_settings.enable_events_data:
        logger.info("Seeding event venues from Ticketmaster...")
        fetch_and_store_venues()
    else:
        logger.info("Skipping events venue seeding...")

    logger.info("Finished processing static data.")


def main_dynamic() -> None:
    print("Processing dynamic data...")

    db_settings = get_db_settings()
    sources_settings = get_data_sources_settings()

    print(
        f"Connected to database: {db_settings.name} at {db_settings.host}:{db_settings.port}"
    )
    print("\nData sources enabled:")
    print(f"  - Cycle data: {sources_settings.enable_cycle_data}")
    print(f"  - Car data: {sources_settings.enable_car_data}")
    print(f"  - Bus data: {sources_settings.enable_bus_data}")
    print(f"  - Train data: {sources_settings.enable_train_data}")
    print(f"  - Tram data: {sources_settings.enable_tram_data}")
    print(f"  - Construction data: {sources_settings.enable_construction_data}")
    print(f"  - Events data: {sources_settings.enable_events_data}\n")

    # Process data sources based on enabled toggles
    if sources_settings.enable_train_data:
        print("Processing train data...")

    if sources_settings.enable_cycle_data:
        print("Processing cycle data...")
        fetch_and_store_station_snapshots()

    if sources_settings.enable_car_data:
        print("Processing car data...")

    if sources_settings.enable_bus_data:
        print("Processing bus data...")
        process_bus_live_data()

    if sources_settings.enable_tram_data:
        print("Processing tram data...")

    if sources_settings.enable_construction_data:
        print("Processing construction data...")

    if sources_settings.enable_events_data:
        print("Processing events data...")
        fetch_and_store_events()


def main_import_history() -> None:
    """Import historical Dublin Bikes CSV archives."""
    logger = logging.getLogger(__name__)

    sources_settings = get_data_sources_settings()

    if not sources_settings.dublin_bikes_csv_archive_dir:
        logger.error("DUBLIN_BIKES_CSV_ARCHIVE_DIR not configured")
        return

    logger.info("Importing Dublin Bikes historical CSV data...")
    import_all_station_history_csvs(sources_settings.dublin_bikes_csv_archive_dir)


def main() -> None:
    configure_logging()
    logger = logging.getLogger(__name__)

    args = get_args()

    logger.info("Initializing the database...")
    init_db()
    logger.info("Database initialized successfully.")

    if args.static:
        main_static()
    elif args.import_history:
        main_import_history()
    else:
        main_dynamic()


if __name__ == "__main__":
    main()
