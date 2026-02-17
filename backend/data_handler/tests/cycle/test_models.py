"""Tests for Dublin Bikes database models."""

import sys
from datetime import datetime
from decimal import Decimal
from unittest.mock import Mock

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker


# Create a test Base to avoid importing db.py (which creates a PostgreSQL engine)
class Base(DeclarativeBase):
    pass


# Mock db and settings modules before importing models
_mock_db = type(sys)("data_handler.db")
_mock_db.Base = Base
_mock_db.SessionLocal = Mock()
sys.modules["data_handler.db"] = _mock_db

_mock_settings = Mock()
_mock_settings.get_db_settings.return_value.postgres_schema = None
sys.modules["data_handler.settings.database_settings"] = _mock_settings

from data_handler.cycle.models import (  # noqa: E402
    DublinBikesStation,
    DublinBikesStationHistory,
    DublinBikesStationSnapshot,
)


@pytest.fixture
def in_memory_db() -> Session:
    """Create an in-memory SQLite database for testing."""
    engine = create_engine("sqlite:///:memory:")
    Base.metadata.create_all(engine)
    session_factory = sessionmaker(bind=engine)
    session = session_factory()
    yield session
    session.close()


class TestDublinBikesStationModel:
    """Test DublinBikesStation model structure and constraints."""

    def test_model_has_required_fields(self) -> None:
        """Test that model has all required fields."""
        required_fields = {
            "station_id",
            "system_id",
            "name",
            "latitude",
            "longitude",
            "capacity",
        }
        model_fields = set(DublinBikesStation.__table__.columns.keys())
        assert required_fields.issubset(model_fields)

    def test_can_create_station_instance(self, in_memory_db: Session) -> None:
        """Test creating a station instance."""
        station = DublinBikesStation(
            station_id=1,
            system_id="dublin",
            name="Test Station",
            latitude=Decimal("53.349316"),
            longitude=Decimal("-6.262876"),
            capacity=30,
        )
        in_memory_db.add(station)
        in_memory_db.commit()

        result = in_memory_db.query(DublinBikesStation).filter_by(station_id=1).first()
        assert result is not None
        assert result.name == "Test Station"
        assert result.capacity == 30

    def test_station_id_is_primary_key(self) -> None:
        """Test that station_id is the primary key."""
        pk_columns = [col.name for col in DublinBikesStation.__table__.primary_key]
        assert "station_id" in pk_columns


class TestDublinBikesStationSnapshotModel:
    """Test DublinBikesStationSnapshot model."""

    def test_model_has_required_fields(self) -> None:
        """Test that model has all required snapshot fields."""
        required_fields = {
            "station_id",
            "timestamp",
            "last_reported",
            "available_bikes",
            "available_docks",
            "is_installed",
            "is_renting",
            "is_returning",
        }
        model_fields = set(DublinBikesStationSnapshot.__table__.columns.keys())
        assert required_fields.issubset(model_fields)

    def test_can_create_snapshot_with_foreign_key(self, in_memory_db: Session) -> None:
        """Test creating snapshot linked to station."""
        # Create station first
        station = DublinBikesStation(
            station_id=1,
            system_id="dublin",
            name="Test Station",
            latitude=Decimal("53.349316"),
            longitude=Decimal("-6.262876"),
            capacity=30,
        )
        in_memory_db.add(station)
        in_memory_db.commit()

        # Create snapshot
        snapshot = DublinBikesStationSnapshot(
            station_id=1,
            timestamp=datetime(2026, 1, 22, 17, 30, 0),
            last_reported=datetime(2026, 1, 22, 17, 30, 0),
            available_bikes=8,
            available_docks=22,
            disabled_bikes=0,
            disabled_docks=0,
            is_installed=True,
            is_renting=True,
            is_returning=True,
        )
        in_memory_db.add(snapshot)
        in_memory_db.commit()

        result = in_memory_db.query(DublinBikesStationSnapshot).first()
        assert result is not None
        assert result.available_bikes == 8
        assert result.is_renting is True


class TestDublinBikesStationHistoryModel:
    """Test DublinBikesStationHistory model."""

    def test_model_has_required_fields(self) -> None:
        """Test that model has all required history fields."""
        required_fields = {
            "station_id",
            "timestamp",
            "last_reported",
            "available_bikes",
            "available_docks",
            "is_installed",
            "is_renting",
            "is_returning",
        }
        model_fields = set(DublinBikesStationHistory.__table__.columns.keys())
        assert required_fields.issubset(model_fields)

    def test_unique_constraint_on_station_timestamp(self) -> None:
        """Test that (station_id, timestamp) has unique constraint."""
        constraints = DublinBikesStationHistory.__table__.constraints
        unique_constraints = [c for c in constraints if hasattr(c, "columns")]

        # Check that there's a unique constraint on station_id and timestamp
        has_unique = any(
            "station_id" in [col.name for col in c.columns]
            and "timestamp" in [col.name for col in c.columns]
            for c in unique_constraints
        )
        assert has_unique, "Missing unique constraint on (station_id, timestamp)"

    def test_cannot_insert_duplicate_station_timestamp(
        self, in_memory_db: Session
    ) -> None:
        """Test that duplicate (station_id, timestamp) raises error."""
        # Create station
        station = DublinBikesStation(
            station_id=1,
            system_id="dublin",
            name="Test Station",
            latitude=Decimal("53.349316"),
            longitude=Decimal("-6.262876"),
            capacity=30,
        )
        in_memory_db.add(station)
        in_memory_db.commit()

        timestamp = datetime(2026, 1, 22, 17, 30, 0)

        # First history record
        history1 = DublinBikesStationHistory(
            station_id=1,
            timestamp=timestamp,
            last_reported=timestamp,
            available_bikes=8,
            available_docks=22,
            is_installed=True,
            is_renting=True,
            is_returning=True,
        )
        in_memory_db.add(history1)
        in_memory_db.commit()

        # Duplicate record (same station_id and timestamp)
        history2 = DublinBikesStationHistory(
            station_id=1,
            timestamp=timestamp,  # Same timestamp
            last_reported=timestamp,
            available_bikes=10,  # Different values
            available_docks=20,
            is_installed=True,
            is_renting=True,
            is_returning=True,
        )
        in_memory_db.add(history2)

        # Should raise IntegrityError
        with pytest.raises(Exception):  # noqa: B017  # SQLite raises IntegrityError
            in_memory_db.commit()
