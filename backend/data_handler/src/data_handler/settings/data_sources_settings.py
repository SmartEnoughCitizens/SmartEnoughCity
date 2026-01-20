from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from data_handler.settings.app_settings import is_dev


class DataSourcesSettings(BaseSettings):
    """
    Data source feature toggle settings.

    This class manages which data sources should be enabled/disabled.
    Allows toggling API calls for different data sources via environment variables.

    Attributes:
        enable_cycle_data: Toggle for cycle data source (from ENABLE_CYCLE_DATA)
        enable_car_data: Toggle for car data source (from ENABLE_CAR_DATA)
        enable_bus_data: Toggle for bus data source (from ENABLE_BUS_DATA)
        enable_train_data: Toggle for train data source (from ENABLE_TRAIN_DATA)
        enable_tram_data: Toggle for tram data source (from ENABLE_TRAM_DATA)
        enable_construction_data: Toggle for construction data source (from ENABLE_CONSTRUCTION_DATA)
    """

    enable_cycle_data: bool = Field(True, alias="ENABLE_CYCLE_DATA")
    enable_car_data: bool = Field(True, alias="ENABLE_CAR_DATA")
    enable_bus_data: bool = Field(True, alias="ENABLE_BUS_DATA")
    enable_train_data: bool = Field(True, alias="ENABLE_TRAIN_DATA")
    enable_tram_data: bool = Field(True, alias="ENABLE_TRAM_DATA")
    enable_construction_data: bool = Field(True, alias="ENABLE_CONSTRUCTION_DATA")

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
    - In production mode: loads from environment variables

    The result is cached to avoid repeated initialization.

    Returns:
        DataSourcesSettings: Configured data sources settings instance

    Note:
        This is the preferred way to obtain data sources settings. Do not instantiate
        `DataSourcesSettings` directly.
    """

    if is_dev():
        return DataSourcesSettings(_env_file=".env.development", _env_file_encoding="utf-8")

    return DataSourcesSettings()
