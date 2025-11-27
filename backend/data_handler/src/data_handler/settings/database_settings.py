from functools import lru_cache
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

from data_handler.settings.app_settings import is_dev


class DatabaseSettings(BaseSettings):
    """
    Database configuration settings.

    This class should not be used directly. Instead, use `get_db_settings()` which
    is environment-aware and handles loading settings based on the current environment
    (development vs production).

    Attributes:
        host: Database host address (from DB_HOST environment variable)
        port: Database port number (from DB_PORT environment variable)
        name: Database name (from DB_NAME environment variable)
        user: Database user for data handler (from DB_DATA_HANDLER_USER environment variable)
        password: Database password for data handler (from DB_DATA_HANDLER_PASSWORD environment variable)
        postgres_schema: Database schema for data handler (from DB_DATA_HANDLER_SCHEMA environment variable)
    """

    host: str = Field(..., alias="DB_HOST")
    port: int = Field(..., alias="DB_PORT")
    name: str = Field(..., alias="DB_NAME")
    user: str = Field(..., alias="DB_DATA_HANDLER_USER")
    password: str = Field(..., alias="DB_DATA_HANDLER_PASSWORD")
    postgres_schema: str = Field(..., alias="DB_DATA_HANDLER_SCHEMA")

    model_config = SettingsConfigDict(
        extra="ignore",
        populate_by_name=True,
    )


@lru_cache(maxsize=1)
def get_db_settings() -> DatabaseSettings:
    """
    Get database settings instance, environment-aware.

    This function automatically detects the current environment and loads settings
    accordingly:
    - In development mode: loads from `.env.development` file
    - In production mode: loads from environment variables

    The result is cached to avoid repeated initialization.

    Returns:
        DatabaseSettings: Configured database settings instance

    Note:
        This is the preferred way to obtain database settings. Do not instantiate
        `DatabaseSettings` directly.
    """
    
    if is_dev():
        return DatabaseSettings(_env_file=".env.development", _env_file_encoding="utf-8")

    return DatabaseSettings()
