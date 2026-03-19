import logging
import os
import sys

from data_handler.bus.live_data_handler import process_bus_live_data
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.car.process_car_data import process_car_static_data
from data_handler.congestion_and_construction.data_handler import process_traffic_live_data
from data_handler.cycle.csv_import_handler import import_cycle_history_data
from data_handler.cycle.realtime_handler import process_cycle_live_data
from data_handler.cycle.static_data_handler import process_cycle_station_info
from data_handler.db import Base, engine
from data_handler.events.data_handler import process_event_venue_info, process_events_data
from data_handler.logging import configure_logging
from data_handler.pedestrians.live_data_handler import process_pedestrian_live_data
from data_handler.population.data_handler import process_population_static_data
from data_handler.settings.data_sources_settings import get_data_sources_settings
from data_handler.train.realtime_handler import (
    process_train_live_data,
    process_train_station_info,
)
from data_handler.train.static_data_handler import process_train_static_data
from data_handler.tram.forecast_handler import process_tram_live_data, process_tram_stop_info
from data_handler.tram.static_data_handler import process_tram_static_data

_RUN_VARS = ("RUN_1_MIN", "RUN_5_MIN", "RUN_15_MIN", "RUN_DAILY", "RUN_MONTHLY", "RUN_STATIC")


def _is_set(var: str) -> bool:
    return os.environ.get(var, "").lower() in ("1", "true")


def init_db() -> None:
    Base.metadata.create_all(bind=engine)


def main() -> None:
    configure_logging()
    logger = logging.getLogger(__name__)

    run_1_min = _is_set("RUN_1_MIN")
    run_5_min = _is_set("RUN_5_MIN")
    run_15_min = _is_set("RUN_15_MIN")
    run_daily = _is_set("RUN_DAILY")
    run_monthly = _is_set("RUN_MONTHLY")
    run_static = _is_set("RUN_STATIC")

    if not any([run_1_min, run_5_min, run_15_min, run_daily, run_monthly, run_static]):
        logger.error("No RUN_* variable set. Set one of: %s", ", ".join(_RUN_VARS))
        sys.exit(1)

    logger.info("Initializing the database...")
    init_db()
    logger.info("Database initialized successfully.")

    s = get_data_sources_settings()

    if run_1_min:
        logger.info("Running 1-minute handlers...")
        if s.enable_cycle_data:
            process_cycle_live_data()
        if s.enable_bus_data:
            process_bus_live_data()
        if s.enable_tram_data:
            process_tram_live_data()
        if s.enable_train_data:
            process_train_live_data()

    if run_5_min:
        logger.info("Running 5-minute handlers...")
        if s.enable_construction_data:
            process_traffic_live_data()

    if run_15_min:
        logger.info("Running 15-minute handlers...")
        if s.enable_pedestrian_data:
            process_pedestrian_live_data()

    if run_daily:
        logger.info("Running daily handlers...")
        if s.enable_cycle_data:
            process_cycle_station_info()
        if s.enable_events_data:
            process_events_data()

    if run_monthly:
        logger.info("Running monthly handlers...")
        if s.enable_train_data:
            process_train_station_info()
        if s.enable_tram_data:
            process_tram_stop_info()
        if s.enable_events_data:
            process_event_venue_info()

    if run_static:
        logger.info("Running static data handlers...")
        if s.enable_bus_data and s.bus_gtfs_static_data_dir:
            process_bus_static_data(s.bus_gtfs_static_data_dir)
        elif s.enable_bus_data:
            logger.info("Skipping bus static data: BUS_GTFS_STATIC_DATA_DIR not set.")

        if s.enable_train_data and s.train_gtfs_static_data_dir:
            process_train_static_data(s.train_gtfs_static_data_dir)
        elif s.enable_train_data:
            logger.info("Skipping train static data: TRAIN_GTFS_STATIC_DATA_DIR not set.")

        if s.enable_tram_data and s.tram_gtfs_static_data_dir:
            process_tram_static_data(s.tram_gtfs_static_data_dir, s.tram_cso_data_dir)
        elif s.enable_tram_data:
            logger.info("Skipping tram static data: TRAM_GTFS_STATIC_DATA_DIR not set.")

        if s.enable_car_data and s.car_static_data_dir:
            process_car_static_data(s.car_static_data_dir)
        elif s.enable_car_data:
            logger.info("Skipping car static data: CAR_STATIC_DATA_DIR not set.")

        if s.enable_population_data and s.population_static_data_dir:
            process_population_static_data(s.population_static_data_dir)
        elif s.enable_population_data:
            logger.info("Skipping population static data: POPULATION_STATIC_DATA_DIR not set.")

        if s.enable_cycle_data and s.dublin_bikes_csv_archive_dir:
            import_cycle_history_data(s.dublin_bikes_csv_archive_dir)
        elif s.enable_cycle_data:
            logger.info("Skipping cycle history import: DUBLIN_BIKES_CSV_ARCHIVE_DIR not set.")


if __name__ == "__main__":
    main()
