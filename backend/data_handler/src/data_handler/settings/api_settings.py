from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from data_handler.settings.app_settings import is_dev


class APISettings(BaseSettings):
    """Settings for external API credentials."""

    hermes_url: str = Field(..., alias="HERMES_URL")
    gtfs_api_key: str = Field(..., alias="GTFS_API_KEY")

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


@lru_cache(maxsize=1)
def get_api_settings() -> APISettings:
    """Return API settings. Loads `.env.development` in dev mode, otherwise from env."""
    if is_dev():
        return APISettings(_env_file=".env.development", _env_file_encoding="utf-8")
    return APISettings()
