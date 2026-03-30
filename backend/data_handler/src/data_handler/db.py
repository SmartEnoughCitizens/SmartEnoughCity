from sqlalchemy import create_engine, inspect, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from data_handler.settings.database_settings import get_db_settings


class Base(DeclarativeBase):
    pass


engine = create_engine(
    str(get_db_settings().dsn),
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def repair_known_schema_drift() -> None:
    """
    Patch known local schema drift that `create_all()` cannot fix.

    SQLAlchemy creates missing tables, but it does not add new columns to
    already-existing tables. Older local databases can therefore miss columns
    added later in the models, which breaks static data loads.
    """
    db_settings = get_db_settings()
    schema = db_settings.postgres_schema
    table_name = "ev_charging_points"

    inspector = inspect(engine)
    if not inspector.has_table(table_name, schema=schema):
      return

    existing_columns = {
        column["name"] for column in inspector.get_columns(table_name, schema=schema)
    }
    required_columns = {
        "address": "TEXT",
        "county": "TEXT",
        "lat": "DOUBLE PRECISION",
        "lon": "DOUBLE PRECISION",
        "charger_count": "INTEGER DEFAULT 1 NOT NULL",
        "power_rating_of_ccs_connectors_kw": "DOUBLE PRECISION",
        "power_rating_of_chademo_connectors_kw": "DOUBLE PRECISION",
        "power_rating_of_ac_fast_kw": "DOUBLE PRECISION",
        "power_rating_of_standard_ac_socket_kw": "DOUBLE PRECISION",
        "is_24_7": "BOOLEAN DEFAULT FALSE NOT NULL",
    }

    missing_columns = {
        name: sql_type
        for name, sql_type in required_columns.items()
        if name not in existing_columns
    }
    if not missing_columns:
        return

    with engine.begin() as connection:
        for column_name, sql_type in missing_columns.items():
            connection.execute(
                text(
                    f'ALTER TABLE "{schema}"."{table_name}" '
                    f'ADD COLUMN IF NOT EXISTS "{column_name}" {sql_type}'
                )
            )
