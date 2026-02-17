"""Tests for Dublin Bikes database models."""

from datetime import datetime
from decimal import Decimal

import pytest
from sqlalchemy.orm import Session

from data_handler.cycle.models import (
    DublinBikesStation,
    DublinBikesStationHistory,
    DublinBikesStationSnapshot,
)


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

    def test_can_create_station_instance(self, db_session: Session) -> None:
        """Test creating a station instance."""
        station = DublinBikesStation(
            station_id=1,
            system_id="dublin",
            name="Test Station",
            latitude=Decimal("53.349316"),
            longitude=Decimal("-6.262876"),
            capacity=30,
        )
        db_session.add(station)
        db_session.commit()

        result = db_session.query(DublinBikesStation).filter_by(station_id=1).first()
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

    def test_can_create_snapshot_with_foreign_key(self, db_session: Session) -> None:
        """Test creating snapshot linked to station."""
        station = DublinBikesStation(
            station_id=1,
            system_id="dublin",
            name="Test Station",
            latitude=Decimal("53.349316"),
            longitude=Decimal("-6.262876"),
            capacity=30,
        )
        db_session.add(station)
        db_session.commit()

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
        db_session.add(snapshot)
        db_session.commit()

        result = db_session.query(DublinBikesStationSnapshot).first()
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

        has_unique = any(
            "station_id" in [col.name for col in c.columns]
            and "timestamp" in [col.name for col in c.columns]
            for c in unique_constraints
        )
        assert has_unique, "Missing unique constraint on (station_id, timestamp)"

    def test_cannot_insert_duplicate_station_timestamp(
        self, db_session: Session
    ) -> None:
        """Test that duplicate (station_id, timestamp) raises error."""
        station = DublinBikesStation(
            station_id=1,
            system_id="dublin",
            name="Test Station",
            latitude=Decimal("53.349316"),
            longitude=Decimal("-6.262876"),
            capacity=30,
        )
        db_session.add(station)
        db_session.commit()

        timestamp = datetime(2026, 1, 22, 17, 30, 0)

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
        db_session.add(history1)
        db_session.commit()

        history2 = DublinBikesStationHistory(
            station_id=1,
            timestamp=timestamp,
            last_reported=timestamp,
            available_bikes=10,
            available_docks=20,
            is_installed=True,
            is_renting=True,
            is_returning=True,
        )
        db_session.add(history2)

        with pytest.raises(Exception):  # noqa: B017
            db_session.commit()
