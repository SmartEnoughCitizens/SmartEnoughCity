import argparse
import logging

from sqlalchemy import func, select

from data_handler.bus.live_data_handler import process_bus_live_data
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.car.process_car_data import process_car_static_data
from data_handler.cycle.models import DublinBikesStation
from data_handler.cycle.realtime_handler import fetch_and_store_station_snapshots
from data_handler.cycle.static_data_handler import process_station_information
from data_handler.db import Base, SessionLocal, engine, repair_known_schema_drift
from data_handler.events.data_handler import (
    fetch_and_store_events,
    fetch_and_store_venues,
)
from data_handler.logging2 import configure_logging
from data_handler.pedestrians.live_data_handler import process_pedestrian_live_data
from data_handler.population.data_handler import process_population_static_data
from data_handler.settings.data_sources_settings import (
    DataSourcesSettings,
    get_data_sources_settings,
)
from data_handler.settings.database_settings import get_db_settings
from data_handler.train.realtime_handler import irish_rail_realtime_to_db
from data_handler.train.static_data_handler import process_train_static_data
from data_handler.tram.forecast_handler import luas_forecasts_to_db, luas_stops_to_db
from data_handler.tram.static_data_handler import process_tram_static_data
from data_handler.urls import (
    delete_static_data,
    download_and_extract_zip,
    download_file,
    download_google_drive_folder,
)


def get_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="SmartEnoughCity Data Handler")
    parser.add_argument(
        "--static", action="store_true", help="Process static data only"
    )
    return parser.parse_args()


def init_db() -> None:
    Base.metadata.create_all(bind=engine)
    repair_known_schema_drift()


def _process_bus_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_bus_data:
        logger.info("Skipping bus static data processing...")
        return
    bus_dir = str(settings.base_static_data_dir / "bus")
    download_and_extract_zip(settings.bus_gtfs_static_zip_url, bus_dir)
    process_bus_static_data(settings.base_static_data_dir / "bus")
    delete_static_data(bus_dir)


def _process_car_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_car_data:
        logger.info("Skipping car static data processing...")
        return
    car_dir = str(settings.base_static_data_dir / "car")
    download_google_drive_folder(settings.car_gdrive_folder_id, car_dir)
    process_car_static_data(settings.base_static_data_dir / "car")
    delete_static_data(car_dir)


def _process_cycle_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_cycle_data:
        logger.info("Skipping cycle static data processing...")
        return
    logger.info("Processing Dublin Bikes static station information...")
    process_station_information()


def _process_train_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_train_data:
        logger.info("Skipping train static data processing...")
        return
    train_dir = str(settings.base_static_data_dir / "train")
    download_and_extract_zip(settings.train_gtfs_zip_url, train_dir)
    process_train_static_data(settings.base_static_data_dir / "train")
    delete_static_data(train_dir)


def _process_tram_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_tram_data:
        logger.info("Skipping tram static data processing...")
        return
    tram_dir = settings.base_static_data_dir / "tram"
    tram_dir_str = str(tram_dir)
    download_and_extract_zip(settings.tram_gtfs_zip_url, tram_dir_str)
    for filename, url in [
        ("TII03.csv", settings.tram_cso_tii03_url),
        ("TOA11.csv", settings.tram_cso_toa11_url),
        ("TOA09.csv", settings.tram_cso_toa09_url),
        ("TOA02.csv", settings.tram_cso_toa02_url),
    ]:
        download_file(url, str(tram_dir / filename))
    process_tram_static_data(tram_dir, tram_dir)
    delete_static_data(tram_dir_str)


def _process_events_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_events_data:
        logger.info("Skipping events venue seeding...")
        return
    logger.info("Seeding event venues from Ticketmaster...")
    fetch_and_store_venues()


def _process_population_static(settings: DataSourcesSettings) -> None:
    logger = logging.getLogger(__name__)
    if not settings.enable_population_data:
        logger.info("Skipping population static data processing...")
        return
    population_dir = settings.base_static_data_dir / "population"
    population_dir_str = str(population_dir)
    download_file(
        settings.population_boundaries_url,
        str(population_dir / "small_area_boundaries_2022.geojson"),
    )
    download_file(
        settings.population_census_url,
        str(population_dir / "population_census_2022.csv"),
    )
    process_population_static_data(population_dir)
    delete_static_data(population_dir_str)


def main_static() -> None:
    logger = logging.getLogger(__name__)
    logger.info("Processing static data...")
    settings = get_data_sources_settings()
    _process_bus_static(settings)
    _process_car_static(settings)
    _process_cycle_static(settings)
    _process_train_static(settings)
    _process_tram_static(settings)
    _process_events_static(settings)
    _process_population_static(settings)
    logger.info("Finished processing static data.")


def _run_train_dynamic(logger: logging.Logger) -> None:
    logger.info("Processing train data...")
    irish_rail_realtime_to_db()


def _run_cycle_dynamic(logger: logging.Logger) -> None:
    logger.info("Processing cycle data...")
    with SessionLocal() as session:
        station_count = session.scalar(
            select(func.count()).select_from(DublinBikesStation)
        )
    if station_count == 0:
        logger.info("No stations found, seeding station information...")
        process_station_information()
    logger.info("Fetching Dublin Bikes station snapshots...")
    fetch_and_store_station_snapshots()


def _run_bus_dynamic(logger: logging.Logger) -> None:
    logger.info("Processing bus data...")
    process_bus_live_data()


def _run_tram_dynamic(logger: logging.Logger) -> None:
    logger.info("Processing tram data...")
    luas_stops_to_db()
    luas_forecasts_to_db()


def _run_events_dynamic(logger: logging.Logger) -> None:
    logger.info("Processing events data...")
    fetch_and_store_events()


def _run_pedestrian_dynamic(logger: logging.Logger) -> None:
    logger.info("Processing pedestrian data...")
    process_pedestrian_live_data()


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
    logger.info("  - Bus data: %s", sources_settings.enable_bus_data)
    logger.info("  - Train data: %s", sources_settings.enable_train_data)
    logger.info("  - Tram data: %s", sources_settings.enable_tram_data)
    logger.info("  - Events data: %s", sources_settings.enable_events_data)
    logger.info("  - Pedestrian data: %s", sources_settings.enable_pedestrian_data)

    # Each source is isolated — one failure does not block the others
    for enabled, run_fn, label in [
        (sources_settings.enable_train_data, _run_train_dynamic, "Train"),
        (sources_settings.enable_cycle_data, _run_cycle_dynamic, "Cycle"),
        (sources_settings.enable_bus_data, _run_bus_dynamic, "Bus"),
        (sources_settings.enable_tram_data, _run_tram_dynamic, "Tram"),
        (sources_settings.enable_events_data, _run_events_dynamic, "Events"),
        (
            sources_settings.enable_pedestrian_data,
            _run_pedestrian_dynamic,
            "Pedestrian",
        ),
    ]:
        if enabled:
            try:
                run_fn(logger)
            except Exception:
                logger.exception("%s data processing failed — skipping.", label)


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
