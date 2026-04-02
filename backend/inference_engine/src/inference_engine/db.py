from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from inference_engine.settings.database_settings import get_db_settings


class Base(DeclarativeBase):
    pass
from sqlalchemy.orm import sessionmaker


engine = create_engine(
    str(get_db_settings().dsn),
    pool_pre_ping=True,
)
from inference_engine.settings.db_settings import get_db_settings

engine = create_engine(str(get_db_settings().dsn), pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)
