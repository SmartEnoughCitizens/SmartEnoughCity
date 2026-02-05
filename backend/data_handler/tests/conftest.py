import os
from collections.abc import Generator
from pathlib import Path

import pytest
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session
from testcontainers.postgres import PostgresContainer

_postgres_container = None


def _start_postgres_and_configure_env() -> None:
    global _postgres_container  # noqa: PLW0603

    _postgres_container = PostgresContainer(
        image="postgres:18-alpine",
        username="postgres",
        password="postgres",  # noqa: S106
        dbname="test",
        driver=None,
    )
    _postgres_container.start()

    host = _postgres_container.get_container_host_ip()
    port = int(_postgres_container.get_exposed_port(5432))

    os.environ["APP_ENV"] = "test"
    os.environ["DB_HOST"] = host
    os.environ["DB_PORT"] = str(port)

    from data_handler.settings.database_settings import get_db_settings  # noqa: PLC0415

    get_db_settings.cache_clear()

    settings = get_db_settings()
    engine = create_engine(
        str(settings.dsn),
        pool_pre_ping=True,
    )

    with engine.begin() as conn:
        conn.execute(text(f"CREATE SCHEMA IF NOT EXISTS {settings.postgres_schema}"))

    engine.dispose()


def _stop_postgres() -> None:
    global _postgres_container  # noqa: PLW0603
    if _postgres_container is not None:
        _postgres_container.stop()
        _postgres_container = None


def pytest_configure(config: pytest.Config) -> None:
    _start_postgres_and_configure_env()


def pytest_sessionfinish(session: pytest.Session, exitstatus: int) -> None:
    _stop_postgres()


@pytest.fixture(scope="session")
def db_engine() -> Engine:
    from data_handler.db import engine  # noqa: PLC0415

    return engine


@pytest.fixture
def db_session(db_engine: Engine) -> Generator[Session, None, None]:
    """
    Function-scoped database session for integration tests.

    Registers all ORM models, creates tables, yields a session,
    then drops tables so each test gets a clean schema.
    """

    from data_handler.db import Base, SessionLocal  # noqa: PLC0415

    Base.metadata.create_all(bind=db_engine)

    session = SessionLocal()
    yield session
    session.close()

    Base.metadata.drop_all(bind=db_engine)


@pytest.fixture(scope="session")
def tests_data_dir() -> Path:
    """Returns the path to the directory containing test data."""
    return Path("tests/data")
