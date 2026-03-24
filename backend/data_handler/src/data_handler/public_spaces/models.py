from typing import ClassVar

from geoalchemy2 import Geometry
from sqlalchemy import Enum as SQLEnum
from sqlalchemy import Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from data_handler.db import Base
from data_handler.public_spaces.public_space_osmium_handler import PublicSpaceType
from data_handler.settings.database_settings import get_db_settings

DB_SCHEMA = get_db_settings().postgres_schema


class PublicSpace(Base):
    __tablename__ = "public_spaces"
    __table_args__: ClassVar[dict] = (
        Index("idx_public_spaces_geom", "geom", postgresql_using="gist"),
        {"schema": DB_SCHEMA},
    )

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    type: Mapped[PublicSpaceType] = mapped_column(
        SQLEnum(PublicSpaceType, schema=DB_SCHEMA), nullable=False
    )
    subtype: Mapped[str] = mapped_column(String(100), nullable=False)

    # Geometry column: SRID 4326 (WGS 84) point for lat/lon
    # spatial_index=False here because we defined it explicitly in __table_args__
    geom: Mapped[Geometry] = mapped_column(
        Geometry(geometry_type="POINT", srid=4326, spatial_index=False),
        nullable=False,
    )

    def __repr__(self) -> str:
        return f"PublicSpace(id={self.id}, name={self.name!r}, type={self.type!r}, subtype={self.subtype!r})"
