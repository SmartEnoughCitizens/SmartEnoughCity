import os
from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class APISettings(BaseSettings):
    """Settings for external API endpoints."""

    hermes_url: str = Field(..., alias="HERMES_URL")
    http_timeout: int = Field(default=30, alias="HTTP_TIMEOUT")

    model_config = SettingsConfigDict(
        extra="ignore",
        populate_by_name=True,
    )


@lru_cache(maxsize=1)
def get_api_settings() -> APISettings:
    """Return API settings. Loads `.env.development` in dev mode, otherwise from env."""
    if os.getenv("APP_ENV", "dev") == "dev":
        return APISettings(_env_file=".env.development", _env_file_encoding="utf-8")
    return APISettings()


class DBSettings(BaseSettings):
    """Settings for the PostgreSQL database connection."""

    db_host: str = Field(default="localhost", alias="DB_HOST")
    db_port: int = Field(default=5432, alias="DB_PORT")
    db_name: str = Field(default="smart_enough_city", alias="DB_NAME")
    db_user: str = Field(..., alias="DB_USER")
    db_password: str = Field(..., alias="DB_PASSWORD")

    model_config = SettingsConfigDict(
        extra="ignore",
        populate_by_name=True,
    )


@lru_cache(maxsize=1)
def get_db_settings() -> DBSettings:
    """Return DB settings. Loads `.env.development` in dev mode, otherwise from env."""
    if is_dev():
        return DBSettings(_env_file=".env.development", _env_file_encoding="utf-8")
    return DBSettings()
