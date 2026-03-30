import logging
from pathlib import Path

import pandas as pd
from sqlalchemy import delete

from data_handler.db import SessionLocal
from data_handler.public_spaces.models import PublicSpace
from data_handler.public_spaces.public_space_osmium_handler import PublicSpaceHandler

logger = logging.getLogger(__name__)


def process_public_spaces_file(osm_file: Path) -> pd.DataFrame:
    logger.info("Parsing OSM file: %s", osm_file.name)
    handler = PublicSpaceHandler()
    handler.apply_file(osm_file, locations=True)
    df = pd.DataFrame(handler.data)
    logger.info("Parsed %d public space features from OSM.", len(df))
    return df


def _point_wkt(lon: float, lat: float) -> str:
    """Return WKT for a point in SRID 4326 (lon, lat order for WKT)."""
    return f"SRID=4326;POINT({lon} {lat})"


def save_public_spaces_to_database(public_spaces: pd.DataFrame) -> None:
    if public_spaces.empty:
        logger.warning("No public spaces to save.")
        return

    session = SessionLocal()
    try:
        session.execute(delete(PublicSpace))

        records = [
            PublicSpace(
                name=row["name"] if pd.notna(row["name"]) else None,
                type=row["type"],
                subtype=row["subtype"],
                geom=_point_wkt(row["lon"], row["lat"]),
            )
            for _, row in public_spaces.iterrows()
        ]
        session.add_all(records)
        session.commit()
        logger.info("Inserted %d public space record(s). Public spaces import complete.", len(records))
    except Exception:
        session.rollback()
        logger.exception("Error saving public spaces to database")
        raise
    finally:
        session.close()


def process_public_spaces(osm_file: Path) -> None:
    """
    Process public spaces from an OSM file.

    This function:
    1. Reads the OSM file
    2. Extracts the public spaces
    3. Inserts the public spaces into the database
    """

    public_spaces = process_public_spaces_file(osm_file)
    save_public_spaces_to_database(public_spaces)
