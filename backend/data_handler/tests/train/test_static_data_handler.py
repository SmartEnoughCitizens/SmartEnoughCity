from pathlib import Path

import pytest
from sqlalchemy.orm import Session

from data_handler.train.static_data_handler import process_train_static_data
from tests.utils import assert_row_count, assert_rows


class TestProcessTrainStaticData:
    """Integration tests for process_train_static_data."""

    def test_process_gtfs_data_inserts_all_tables(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Processing GTFS data populates all expected tables."""
        train_data_dir = tests_data_dir / "train" / "gtfs"

        # Verify tables are empty before processing
        assert_row_count(db_session, "train_agencies", 0)
        assert_row_count(db_session, "train_calendar_schedule", 0)
        assert_row_count(db_session, "train_calendar_dates", 0)
        assert_row_count(db_session, "train_routes", 0)
        assert_row_count(db_session, "train_stops", 0)
        assert_row_count(db_session, "train_trip_shapes", 0)
        assert_row_count(db_session, "train_trips", 0)
        assert_row_count(db_session, "train_stop_times", 0)

        # Process
        process_train_static_data(train_data_dir)

        # ── Verify agencies ────────────────────────────────────────
        assert_row_count(db_session, "train_agencies", 1)
        assert_rows(
            db_session,
            "train_agencies",
            [
                {
                    "id": 1,
                    "name": "Irish Rail",
                    "url": "https://www.irishrail.ie/",
                    "timezone": "Europe/Dublin",
                },
            ],
        )

        # ── Verify calendar ────────────────────────────────────────
        assert_row_count(db_session, "train_calendar_schedule", 3)
        assert_row_count(db_session, "train_calendar_dates", 2)

        # ── Verify routes ──────────────────────────────────────────
        assert_row_count(db_session, "train_routes", 2)

        # ── Verify stops ───────────────────────────────────────────
        assert_row_count(db_session, "train_stops", 4)
        assert_rows(
            db_session,
            "train_stops",
            [
                {
                    "id": "CNLLY",
                    "code": 9001,
                    "name": "Connolly",
                    "description": "Dublin Connolly Station",
                    "lat": 53.352925,
                    "lon": -6.249463,
                },
                {
                    "id": "TARA",
                    "code": 9002,
                    "name": "Tara Street",
                    "description": None,
                    "lat": 53.347778,
                    "lon": -6.254444,
                },
                {
                    "id": "PEARSE",
                    "code": 9003,
                    "name": "Pearse",
                    "description": None,
                    "lat": 53.343611,
                    "lon": -6.249722,
                },
                {
                    "id": "BYSDE",
                    "code": 9004,
                    "name": "Bayside",
                    "description": None,
                    "lat": 53.384722,
                    "lon": -6.136111,
                },
            ],
        )

        # ── Verify shapes, trips, stop_times ───────────────────────
        assert_row_count(db_session, "train_trip_shapes", 5)
        assert_row_count(db_session, "train_trips", 5)
        assert_row_count(db_session, "train_stop_times", 4)

    def test_missing_required_gtfs_file_raises_error(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Missing a required GTFS file raises FileNotFoundError."""
        data_dir = tests_data_dir / "train" / "incomplete"

        with pytest.raises(FileNotFoundError, match="Required CSV file not found"):
            process_train_static_data(data_dir)

    def test_optional_calendar_dates_skipped_when_missing(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Processing works even without optional calendar_dates.txt."""
        train_data_dir = tests_data_dir / "train" / "no_cal_dates"

        process_train_static_data(train_data_dir)

        assert_row_count(db_session, "train_calendar_dates", 0)
        assert_row_count(db_session, "train_agencies", 1)
        assert_row_count(db_session, "train_stops", 4)

    def test_idempotent_reprocessing(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Running process twice produces same row counts (delete-then-insert)."""
        train_data_dir = tests_data_dir / "train" / "gtfs"

        process_train_static_data(train_data_dir)
        assert_row_count(db_session, "train_stops", 4)

        process_train_static_data(train_data_dir)
        assert_row_count(db_session, "train_stops", 4)
