from functools import lru_cache
from pathlib import Path

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

from data_handler.settings.app_settings import get_app_mode


class DataSourcesSettings(BaseSettings):
    """
    Data source feature toggle settings.

    This class manages which data sources should be enabled/disabled.
    Allows toggling API calls for different data sources via environment variables.
    """

    enable_cycle_data: bool = Field(True, alias="ENABLE_CYCLE_DATA")
    enable_car_data: bool = Field(True, alias="ENABLE_CAR_DATA")
    enable_bus_data: bool = Field(True, alias="ENABLE_BUS_DATA")
    enable_train_data: bool = Field(True, alias="ENABLE_TRAIN_DATA")
    enable_tram_data: bool = Field(True, alias="ENABLE_TRAM_DATA")
    enable_construction_data: bool = Field(True, alias="ENABLE_CONSTRUCTION_DATA")
    enable_events_data: bool = Field(True, alias="ENABLE_EVENTS_DATA")
    enable_population_data: bool = Field(True, alias="ENABLE_POPULATION_DATA")

    base_static_data_dir: Path = Field(
        Path("static_data"), alias="BASE_STATIC_DATA_DIR"
    )

    # GTFS ZIP URLs (TfI public data)
    bus_gtfs_static_zip_url: str = Field(
        "https://www.transportforireland.ie/transitData/Data/GTFS_All.zip",
        alias="BUS_GTFS_STATIC_ZIP_URL",
    )
    train_gtfs_zip_url: str = Field(
        "https://www.transportforireland.ie/transitData/Data/GTFS_Irish_Rail.zip",
        alias="TRAIN_GTFS_ZIP_URL",
    )
    tram_gtfs_zip_url: str = Field(
        "https://www.transportforireland.ie/transitData/Data/GTFS_LUAS.zip",
        alias="TRAM_GTFS_ZIP_URL",
    )

    # Tram CSO dataset URLs (CSO PxStat API)
    tram_cso_tii03_url: str = Field(
        "https://ws.cso.ie/public/api.restful/PxStat.Data.Cube_API.ReadDataset/TII03/CSV/1.0/en",
        alias="TRAM_CSO_TII03_URL",
    )
    tram_cso_toa11_url: str = Field(
        "https://ws.cso.ie/public/api.restful/PxStat.Data.Cube_API.ReadDataset/TOA11/CSV/1.0/en",
        alias="TRAM_CSO_TOA11_URL",
    )
    tram_cso_toa09_url: str = Field(
        "https://ws.cso.ie/public/api.restful/PxStat.Data.Cube_API.ReadDataset/TOA09/CSV/1.0/en",
        alias="TRAM_CSO_TOA09_URL",
    )
    tram_cso_toa02_url: str = Field(
        "https://ws.cso.ie/public/api.restful/PxStat.Data.Cube_API.ReadDataset/TOA02/CSV/1.0/en",
        alias="TRAM_CSO_TOA02_URL",
    )

    # Population data URLs
    population_boundaries_url: str = Field(
        "https://data-osi.opendata.arcgis.com/api/download/v1/items/9472cff586d74f2ba3c240d4344c5720/geojson?layers=0",
        alias="POPULATION_BOUNDARIES_URL",
    )
    population_census_url: str = Field(
        "https://www.cso.ie/en/media/csoie/census/census2022/SAPS_2022_Small_Area_UR_171024.csv",
        alias="POPULATION_CENSUS_URL",
    )

    # Car static data — Google Drive folder ID
    car_gdrive_folder_id: str | None = Field(None, alias="CAR_GDRIVE_FOLDER_ID")

    @field_validator("base_static_data_dir")
    @classmethod
    def _ensure_base_dir(cls, p: Path) -> Path:
        p.mkdir(parents=True, exist_ok=True)
        return p

    model_config = SettingsConfigDict(
        extra="ignore",
        populate_by_name=True,
    )


@lru_cache(maxsize=1)
def get_data_sources_settings() -> DataSourcesSettings:
    """
    Get data sources settings instance, environment-aware.

    This function automatically detects the current environment and loads settings
    accordingly:
    - In development mode: loads from `.env.development` file
    - In test mode: loads from `.env.test` file
    - In production mode: loads from environment variables

    The result is cached to avoid repeated initialization.

    Returns:
        DataSourcesSettings: Configured data sources settings instance

    Note:
        This is the preferred way to obtain data sources settings. Do not instantiate
        `DataSourcesSettings` directly.
    """

    if get_app_mode() == "dev":
        return DataSourcesSettings(
            _env_file=".env.development", _env_file_encoding="utf-8"
        )
    if get_app_mode() == "test":
        return DataSourcesSettings(_env_file=".env.test", _env_file_encoding="utf-8")

    return DataSourcesSettings()