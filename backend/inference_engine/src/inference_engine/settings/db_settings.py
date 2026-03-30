import os
from functools import lru_cache

from pydantic import Field, PostgresDsn, computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict


class DatabaseSettings(BaseSettings):
    host: str = Field(..., alias="DB_HOST")
    port: int = Field(..., alias="DB_PORT")
    name: str = Field(..., alias="DB_NAME")
    user: str = Field(..., alias="DB_DATA_HANDLER_USER")
    password: str = Field(..., alias="DB_DATA_HANDLER_PASSWORD")

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

    model_config = SettingsConfigDict(extra="ignore", populate_by_name=True)


@lru_cache(maxsize=1)
def get_db_settings() -> DatabaseSettings:
    if os.getenv("APP_ENV", "dev") == "dev":
        return DatabaseSettings(
            _env_file=".env.development", _env_file_encoding="utf-8"
        )
    return DatabaseSettings()
