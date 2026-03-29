import logging
import os
import sys
from collections.abc import Callable

from data_handler.bus.live_data_handler import process_bus_live_data
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.car.process_car_data import process_car_static_data
from data_handler.congestion_and_construction.data_handler import (
    process_traffic_live_data,
)
from data_handler.cycle.realtime_handler import process_cycle_live_data
from data_handler.cycle.static_data_handler import process_cycle_station_info
from data_handler.db import Base, engine
from data_handler.events.data_handler import (
    process_event_venue_info,
    process_events_data,
)
from data_handler.logging2 import configure_logging
from data_handler.pedestrians.live_data_handler import process_pedestrian_live_data
from data_handler.population.data_handler import process_population_static_data
from data_handler.settings.data_sources_settings import (
    DataSourcesSettings,
    get_data_sources_settings,
)
from data_handler.train.realtime_handler import (
    process_train_live_data,
    process_train_station_info,
)
from data_handler.train.static_data_handler import process_train_static_data
from data_handler.tram.forecast_handler import (
    process_tram_live_data,
    process_tram_stop_info,
)
from data_handler.tram.static_data_handler import process_tram_static_data
from data_handler.urls import (
    delete_static_data,
    download_and_extract_zip,
    download_file,
    download_google_drive_folder,
)

_RUN_VARS = [
    "RUN_1_MIN",
    "RUN_5_MIN",
    "RUN_15_MIN",
    "RUN_DAILY",
    "RUN_MONTHLY",
    "RUN_STATIC",
]


def _is_set(var: str) -> bool:
    return os.environ.get(var, "").lower() in {"1", "true", "yes"}


def init_db() -> None:
    Base.metadata.create_all(bind=engine)


def _run_handler(logger: logging.Logger, name: str, fn: "Callable[[], None]") -> None:  # type: ignore[type-arg]
    try:
        fn()
    except Exception:
        logger.exception("Handler %s failed — continuing.", name)


def main_1_min() -> None:
    logger = logging.getLogger(__name__)
    settings = get_data_sources_settings()
    if settings.enable_cycle_data:
        logger.info("Processing cycle live data...")
        _run_handler(logger, "cycle_live", process_cycle_live_data)
    if settings.enable_bus_data:
        logger.info("Processing bus live data...")
        _run_handler(logger, "bus_live", process_bus_live_data)
    if settings.enable_tram_data:
        logger.info("Processing tram live data...")
        _run_handler(logger, "tram_live", process_tram_live_data)
    if settings.enable_train_data:
        logger.info("Processing train live data...")
        _run_handler(logger, "train_live", process_train_live_data)


def main_5_min() -> None:
    logger = logging.getLogger(__name__)
    settings = get_data_sources_settings()
    if settings.enable_construction_data:
        logger.info("Processing traffic live data...")
        _run_handler(logger, "traffic_live", process_traffic_live_data)


def main_15_min() -> None:
    logger = logging.getLogger(__name__)
    settings = get_data_sources_settings()
    if settings.enable_pedestrian_data:
        logger.info("Processing pedestrian live data...")
        _run_handler(logger, "pedestrian_live", process_pedestrian_live_data)


def main_daily() -> None:
    logger = logging.getLogger(__name__)
    settings = get_data_sources_settings()
    if settings.enable_cycle_data:
        logger.info("Processing cycle station info...")
        _run_handler(logger, "cycle_station_info", process_cycle_station_info)
    if settings.enable_events_data:
        logger.info("Processing events data...")
        _run_handler(logger, "events_data", process_events_data)


def main_monthly() -> None:
    logger = logging.getLogger(__name__)
    settings = get_data_sources_settings()
    if settings.enable_train_data:
        logger.info("Processing train station info...")
        _run_handler(logger, "train_station_info", process_train_station_info)
    if settings.enable_tram_data:
        logger.info("Processing tram stop info...")
        _run_handler(logger, "tram_stop_info", process_tram_stop_info)
    if settings.enable_events_data:
        logger.info("Processing event venue info...")
        _run_handler(logger, "event_venue_info", process_event_venue_info)


def _run_bus_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:  # type: ignore[type-arg]
    if not settings.enable_bus_data:
        logger.info("Skipping bus static data...")
        return
    bus_dir = str(settings.base_static_data_dir / "bus")
    download_and_extract_zip(settings.bus_gtfs_static_zip_url, bus_dir)
    process_bus_static_data(settings.base_static_data_dir / "bus")
    delete_static_data(bus_dir)


def _run_car_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:  # type: ignore[type-arg]
    if not settings.enable_car_data or not settings.car_gdrive_folder_id:
        logger.info("Skipping car static data...")
        return
    car_dir = str(settings.base_static_data_dir / "car")
    download_google_drive_folder(settings.car_gdrive_folder_id, car_dir)
    process_car_static_data(settings.base_static_data_dir / "car")
    delete_static_data(car_dir)


def _run_cycle_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:  # type: ignore[type-arg]
    if not settings.enable_cycle_data:
        logger.info("Skipping cycle static data...")
        return
    logger.info("Processing Dublin Bikes station info...")
    process_cycle_station_info()


def _run_train_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:  # type: ignore[type-arg]
    if not settings.enable_train_data:
        logger.info("Skipping train static data...")
        return
    train_dir = str(settings.base_static_data_dir / "train")
    download_and_extract_zip(settings.train_gtfs_zip_url, train_dir)
    process_train_static_data(settings.base_static_data_dir / "train")
    delete_static_data(train_dir)


def _run_tram_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:  # type: ignore[type-arg]
    if not settings.enable_tram_data:
        logger.info("Skipping tram static data...")
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


def _run_population_static(
    settings: DataSourcesSettings, logger: logging.Logger
) -> None:  # type: ignore[type-arg]
    if not settings.enable_population_data:
        logger.info("Skipping population static data...")
        return
    population_dir = settings.base_static_data_dir / "population"
    population_dir.mkdir(parents=True, exist_ok=True)
    download_file(
        settings.population_boundaries_url,
        str(population_dir / "small_area_boundaries_2022.geojson"),
    )
    download_file(
        settings.population_census_url,
        str(population_dir / "population_census_2022.csv"),
    )
    process_population_static_data(population_dir)
    delete_static_data(str(population_dir))


def main_static() -> None:
    logger = logging.getLogger(__name__)
    logger.info("Processing static data...")
    settings = get_data_sources_settings()
    _run_bus_static(settings, logger)
    _run_car_static(settings, logger)
    _run_cycle_static(settings, logger)
    _run_train_static(settings, logger)
    _run_tram_static(settings, logger)
    _run_population_static(settings, logger)
    logger.info("Finished processing static data.")


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

    if run_1_min:
        main_1_min()
    if run_5_min:
        main_5_min()
    if run_15_min:
        main_15_min()
    if run_daily:
        main_daily()
    if run_monthly:
        main_monthly()
    if run_static:
        main_static()


if __name__ == "__main__":
    main()
