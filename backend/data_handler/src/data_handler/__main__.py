from __future__ import annotations

import logging
import os
import sys
from typing import TYPE_CHECKING

from data_handler.bus.live_data_handler import process_bus_live_data
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.car.process_car_data import process_car_static_data
from data_handler.congestion_and_construction.data_handler import (
    fetch_and_store_traffic_data,
    push_traffic_events_to_hermes,
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
from data_handler.public_spaces.data_handler import process_public_spaces
from data_handler.settings.data_sources_settings import (
    DataSourcesSettings,
    get_data_sources_settings,
)
from data_handler.train.realtime_handler import (
    process_train_live_data,
    process_train_station_info,
)
from data_handler.train.static_data_handler import (
    process_train_ridership_data,
    process_train_static_data,
)
from data_handler.tram.forecast_handler import (
    process_tram_live_data,
    process_tram_stop_info,
)
from data_handler.tram.static_data_handler import process_tram_static_data
from data_handler.urls import (
    delete_static_data,
    download_and_extract_zip,
    download_arcgis_feature_service_geojson,
    download_file,
    download_google_drive_folder,
)

if TYPE_CHECKING:
    from collections.abc import Callable

_RUN_VARS = [
    "ENABLE_ONE_MIN",
    "ENABLE_ONE_HOUR",
    "ENABLE_ONE_DAY",
    "ENABLE_ONE_MONTH",
    "ENABLE_STATIC",
]


def _is_set(var: str) -> bool:
    return os.environ.get(var, "").lower() in {"1", "true", "yes"}


def init_db() -> None:
    Base.metadata.create_all(bind=engine)


def _run_handler(logger: logging.Logger, name: str, fn: Callable[[], None]) -> None:
    logger.info("→ %s: starting", name)
    try:
        fn()
        logger.info("→ %s: done", name)
    except Exception:
        logger.exception("→ %s: failed — continuing", name)


def main_1_min() -> None:
    logger = logging.getLogger(__name__)
    logger.info("=== [1-min interval] ===")
    settings = get_data_sources_settings()
    if settings.enable_cycle_data:
        _run_handler(logger, "cycle_live", process_cycle_live_data)
    if settings.enable_bus_data:
        _run_handler(logger, "bus_live", process_bus_live_data)
    if settings.enable_tram_data:
        _run_handler(logger, "tram_live", process_tram_live_data)
    if settings.enable_train_data:
        _run_handler(logger, "train_live", process_train_live_data)


def main_1_hour() -> None:
    logger = logging.getLogger(__name__)
    logger.info("=== [1-hour interval] ===")
    settings = get_data_sources_settings()
    if settings.enable_pedestrian_data:
        _run_handler(logger, "pedestrian_live", process_pedestrian_live_data)
    if settings.enable_construction_data:

        def _run_traffic_live() -> None:
            count, fetched_at = fetch_and_store_traffic_data()
            if count > 0:
                push_traffic_events_to_hermes(fetched_at)

        _run_handler(logger, "traffic_live", _run_traffic_live)


def main_1_day() -> None:
    logger = logging.getLogger(__name__)
    logger.info("=== [1-day interval] ===")
    settings = get_data_sources_settings()
    if settings.enable_cycle_data:
        _run_handler(logger, "cycle_station_info", process_cycle_station_info)
    if settings.enable_events_data:
        _run_handler(logger, "events_data", process_events_data)
    if settings.enable_tram_data:
        _run_handler(logger, "tram_stop_info", process_tram_stop_info)
    if settings.enable_bus_data:
        _run_handler(logger, "bus_static", lambda: _run_bus_static(settings, logger))
    if settings.enable_train_data:
        _run_handler(
            logger, "train_static", lambda: _run_train_static(settings, logger)
        )
    if settings.enable_tram_data:
        _run_handler(logger, "tram_static", lambda: _run_tram_static(settings, logger))
    if settings.enable_public_spaces_data:
        _run_handler(
            logger, "public_spaces", lambda: _run_public_spaces(settings, logger)
        )


def main_1_month() -> None:
    logger = logging.getLogger(__name__)
    logger.info("=== [1-month interval] ===")
    settings = get_data_sources_settings()
    if settings.enable_train_data:
        _run_handler(logger, "train_station_info", process_train_station_info)
    if settings.enable_events_data:
        _run_handler(logger, "event_venue_info", process_event_venue_info)


def _run_bus_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:
    bus_dir = str(settings.base_static_data_dir / "bus")
    download_and_extract_zip(settings.bus_gtfs_static_zip_url, bus_dir)
    process_bus_static_data(settings.base_static_data_dir / "bus")
    delete_static_data(bus_dir)


def _run_car_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:
    if not settings.car_gdrive_folder_id:
        logger.info("Skipping car static data (CAR_GDRIVE_FOLDER_ID not set).")
        return
    car_dir = str(settings.base_static_data_dir / "car")
    download_google_drive_folder(settings.car_gdrive_folder_id, car_dir)
    process_car_static_data(settings.base_static_data_dir / "car")
    delete_static_data(car_dir)


def _run_train_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:
    train_dir = str(settings.base_static_data_dir / "train")
    download_and_extract_zip(settings.train_gtfs_zip_url, train_dir)
    process_train_static_data(settings.base_static_data_dir / "train")
    delete_static_data(train_dir)

    if settings.train_ridership_gdrive_folder_id:
        ridership_dir = str(settings.base_static_data_dir / "train_ridership")
        download_google_drive_folder(
            settings.train_ridership_gdrive_folder_id, ridership_dir
        )
        process_train_ridership_data(settings.base_static_data_dir / "train_ridership")
        delete_static_data(ridership_dir)
    else:
        logger.info(
            "Skipping train ridership data (TRAIN_RIDERSHIP_GDRIVE_FOLDER_ID not set)."
        )


def _run_tram_static(settings: DataSourcesSettings, logger: logging.Logger) -> None:
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


def _run_public_spaces(settings: DataSourcesSettings, logger: logging.Logger) -> None:
    osm_file = (
        settings.base_static_data_dir
        / "public_spaces"
        / "ireland-and-northern-ireland-latest.osm.pbf"
    )
    osm_file.parent.mkdir(parents=True, exist_ok=True)
    download_file(settings.public_spaces_osm_url, str(osm_file))
    process_public_spaces(osm_file)
    delete_static_data(str(osm_file.parent))


def _run_population_static(
    settings: DataSourcesSettings, logger: logging.Logger
) -> None:
    population_dir = settings.base_static_data_dir / "population"
    population_dir.mkdir(parents=True, exist_ok=True)
    download_arcgis_feature_service_geojson(
        settings.population_boundaries_feature_service_url,
        str(population_dir / "small_area_boundaries_2022.geojson"),
        where="COUNTY_ENGLISH IN ('DUBLIN CITY','SOUTH DUBLIN')",
        out_fields="SA_PUB2022,COUNTY_ENGLISH",
    )
    download_file(
        settings.population_census_url,
        str(population_dir / "population_census_2022.csv"),
    )
    process_population_static_data(population_dir)
    delete_static_data(str(population_dir))


def main_static() -> None:
    logger = logging.getLogger(__name__)
    logger.info("=== [static interval] ===")
    settings = get_data_sources_settings()
    if settings.enable_car_data:
        _run_handler(logger, "car_static", lambda: _run_car_static(settings, logger))
    if settings.enable_population_data:
        _run_handler(
            logger,
            "population_static",
            lambda: _run_population_static(settings, logger),
        )


def main() -> None:
    configure_logging()
    logger = logging.getLogger(__name__)

    run_1_min = _is_set("ENABLE_ONE_MIN")
    run_1_hour = _is_set("ENABLE_ONE_HOUR")
    run_1_day = _is_set("ENABLE_ONE_DAY")
    run_1_month = _is_set("ENABLE_ONE_MONTH")
    run_static = _is_set("ENABLE_STATIC")

    if not any([run_1_min, run_1_hour, run_1_day, run_1_month, run_static]):
        logger.error("No ENABLE_* variable set. Set one of: %s", ", ".join(_RUN_VARS))
        sys.exit(1)

    logger.info("Initializing the database...")
    init_db()
    logger.info("Database initialized successfully.")

    if run_1_min:
        main_1_min()
    if run_1_hour:
        main_1_hour()
    if run_1_day:
        main_1_day()
    if run_1_month:
        main_1_month()
    if run_static:
        main_static()


if __name__ == "__main__":
    main()
