from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from data_handler.settings.app_settings import get_app_mode


class APISettings(BaseSettings):
    """Settings for external API credentials."""

    hermes_url: str = Field(..., alias="HERMES_URL")
    gtfs_api_key: str = Field(..., alias="GTFS_API_KEY")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


@lru_cache(maxsize=1)
def get_api_settings() -> APISettings:
    """
    Get API settings instance, environment-aware.

    This function automatically detects the current environment and loads settings
    accordingly:
    - In development mode: loads from `.env.development` file
    - In test mode: loads from `.env.test` file
    - In production mode: loads from environment variables

    The result is cached to avoid repeated initialization.

    Returns:
        APISettings: Configured API settings instance

    Note:
        This is the preferred way to obtain API settings. Do not instantiate
        `APISettings` directly.
    """

    if get_app_mode() == "dev":
        return APISettings(_env_file=".env.development", _env_file_encoding="utf-8")
    if get_app_mode() == "test":
        return APISettings(_env_file=".env.test", _env_file_encoding="utf-8")

    return APISettings()
