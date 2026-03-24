import json
import logging
from pathlib import Path

import pandas as pd
from shapely.geometry import MultiPolygon, Polygon, shape
from sqlalchemy import delete

from data_handler.db import SessionLocal
from data_handler.population.models import SmallArea

logger = logging.getLogger(__name__)


def is_relevant_area(feature: dict) -> bool:
    props = feature.get("properties", {})
    return props.get("COUNTY_ENGLISH") in ["DUBLIN CITY", "SOUTH DUBLIN"]


def to_wkt_geom(geometry: dict) -> str:
    s_geom = shape(geometry)
    if isinstance(s_geom, Polygon):
        s_geom = MultiPolygon([s_geom])
    return f"SRID=4326;{s_geom.wkt}"


def process_population_static_data(data_dir: Path) -> None:
    required_files = [
        "population_census_2022.csv",
        "small_area_boundaries_2022.geojson",
    ]
    for filename in required_files:
        file_path = data_dir / filename
        if not file_path.exists():
            msg = f"Required file not found: {file_path}"
            raise FileNotFoundError(msg)

    logger.info("Processing population static data...")

    session = SessionLocal()

    try:
        logger.info("Deleting existing population data...")
        session.execute(delete(SmallArea))

        geojson_path = data_dir / "small_area_boundaries_2022.geojson"
        with geojson_path.open(encoding="utf-8") as f:
            geo_data = json.load(f)

        geo_df = pd.DataFrame(
            [
                {
                    "sa_code": feature["properties"]["SA_PUB2022"],
                    "county_name": feature["properties"]["COUNTY_ENGLISH"],
                    "geometry": feature["geometry"],
                }
                for feature in geo_data["features"]
                if is_relevant_area(feature)
            ]
        )
        geo_df["sa_code"] = geo_df["sa_code"].astype(str)

        csv_df = pd.read_csv(data_dir / "population_census_2022.csv")
        csv_df = csv_df[["GEOGID", "T1_1AGETT"]]
        csv_df.columns = ["sa_code", "total_population"]
        csv_df["sa_code"] = csv_df["sa_code"].astype(str)

        merged_df = pd.merge(geo_df, csv_df, on="sa_code", how="inner")

        small_areas = [
            SmallArea(
                sa_code=row["sa_code"],
                county_name=row["county_name"],
                population=int(row["total_population"]),
                geom=to_wkt_geom(row["geometry"]),
            )
            for _, row in merged_df.iterrows()
        ]

        if not small_areas:
            msg = "No population records found."
            raise ValueError(msg)  # noqa: TRY301

        session.add_all(small_areas)

        logger.info("Committing changes to database...")
        session.commit()
        logger.info(
            "Successfully processed population static data (%d records).",
            len(small_areas),
        )

    except Exception:
        session.rollback()
        logger.exception("Error processing population static data")
        raise

    finally:
        session.close()
