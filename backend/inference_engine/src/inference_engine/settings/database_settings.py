from functools import lru_cache

from pydantic import Field, PostgresDsn, computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict

from inference_engine.settings.app_settings import is_dev


class DatabaseSettings(BaseSettings):
    """
    Database configuration settings for the inference engine.

    Attributes:
        host: Database host address (from DB_HOST environment variable)
        port: Database port number (from DB_PORT environment variable)
        name: Database name (from DB_NAME environment variable)
        user: Database user for inference engine (from DB_INFERENCE_ENGINE_USER environment variable)
        password: Database password for inference engine (from DB_INFERENCE_ENGINE_PASSWORD environment variable)
        postgres_schema: Database schema (from DB_INFERENCE_ENGINE_SCHEMA environment variable)
        dsn: Full Postgres DSN for the database (derived from the other settings)
    """

    host: str = Field(..., alias="DB_HOST")
    port: int = Field(..., alias="DB_PORT")
    name: str = Field(..., alias="DB_NAME")
    user: str = Field(..., alias="DB_INFERENCE_ENGINE_USER")
    password: str = Field(..., alias="DB_INFERENCE_ENGINE_PASSWORD")
    postgres_schema: str = Field(..., alias="DB_INFERENCE_ENGINE_SCHEMA")

    @computed_field
    @property
    def dsn(self) -> PostgresDsn:
        return PostgresDsn.build(
            scheme="postgresql+psycopg",
            username=self.user,
            password=self.password,
            host=self.host,
            port=self.port,
            path=self.name,
        )

    model_config = SettingsConfigDict(
        extra="ignore",
        populate_by_name=True,
    )


@lru_cache(maxsize=1)
def get_db_settings() -> DatabaseSettings:
    """
    Get database settings instance, environment-aware.

    Returns:
        DatabaseSettings: Configured database settings instance
    """
    if is_dev():
        return DatabaseSettings(
            _env_file=".env.development", _env_file_encoding="utf-8"
        )
    return DatabaseSettings()
