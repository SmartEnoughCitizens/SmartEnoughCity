from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from data_handler.settings.database_settings import get_db_settings


class Base(DeclarativeBase):
    pass


engine = create_engine(
    str(get_db_settings().dsn),
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)
