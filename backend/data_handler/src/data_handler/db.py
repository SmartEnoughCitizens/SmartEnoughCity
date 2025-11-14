# backend/data_handler/src/data_handler/db.py
from typing import Generator

from data_handler.settings.database_settings import get_db_settings
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session

_settings = get_db_settings()
# Use psycopg (psycopg v3) dialect which is installed via `psycopg[binary]`.
# SQLAlchemy dialect string for psycopg v3 is 'postgresql+psycopg'.
DATABASE_URL = f"postgresql+psycopg://{_settings.user}:{_settings.password}@{_settings.host}:{_settings.port}/{_settings.name}"

engine = create_engine(
    DATABASE_URL,
    pool_pre_ping=True,     
    future=True,
)

# Session factory
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)

# Dependency / Contextmanager
def get_session() -> Generator[Session, None, None]:
    """Yield a SQLAlchemy Session for use as a dependency or context manager.

"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()