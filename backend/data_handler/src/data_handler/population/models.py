from typing import ClassVar

from geoalchemy2 import Geometry
from sqlalchemy import Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from data_handler.db import Base
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class SmallArea(Base):
    __tablename__ = "small_areas"
    __table_args__: ClassVar[dict] = (
        Index("ix_small_areas_sa_code", "sa_code"),
        Index("idx_small_areas_geom", "geom", postgresql_using="gist"),
        {"schema": DB_SCHEMA},
    )

    sa_code: Mapped[str] = mapped_column(String(50), primary_key=True)
    county_name: Mapped[str] = mapped_column(String(100), nullable=False)
    population: Mapped[int] = mapped_column(Integer, nullable=False)

    # Geometry column: SRID 4326 (WGS 84 / Lat-Long)
    # spatial_index=False here because we defined it explicitly in __table_args__
    geom: Mapped[Geometry] = mapped_column(
        Geometry(geometry_type="MULTIPOLYGON", srid=4326, spatial_index=False),
        nullable=False
    )

    def __repr__(self) -> str:
        return f"SmallArea(sa_code={self.sa_code!r}, county={self.county_name!r}, pop={self.population})"
