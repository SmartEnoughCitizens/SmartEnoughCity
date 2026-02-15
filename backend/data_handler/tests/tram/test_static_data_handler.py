import shutil
from pathlib import Path

import pytest
from sqlalchemy import text
from sqlalchemy.orm import Session

from data_handler.settings.database_settings import get_db_settings
from data_handler.tram.static_data_handler import process_tram_static_data
from tests.utils import assert_row_count, assert_rows


class TestProcessTramStaticData:
    """Integration tests for process_tram_static_data."""

    def test_process_gtfs_data_inserts_all_tables(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Processing GTFS + CSO data populates all expected tables."""
        # Build a combined data dir with both GTFS and CSO files
        tram_data_dir = tests_data_dir / "tram" / "combined"
        tram_data_dir.mkdir(parents=True, exist_ok=True)

        # Copy GTFS files
        gtfs_dir = tests_data_dir / "tram" / "gtfs"
        for f in gtfs_dir.iterdir():
            shutil.copy2(f, tram_data_dir / f.name)

        # Copy CSO files
        cso_dir = tests_data_dir / "tram" / "cso"
        for f in cso_dir.iterdir():
            shutil.copy2(f, tram_data_dir / f.name)

        # Verify tables are empty before processing
        assert_row_count(db_session, "tram_agencies", 0)
        assert_row_count(db_session, "tram_calendar_schedule", 0)
        assert_row_count(db_session, "tram_calendar_dates", 0)
        assert_row_count(db_session, "tram_routes", 0)
        assert_row_count(db_session, "tram_stops", 0)
        assert_row_count(db_session, "tram_trip_shapes", 0)
        assert_row_count(db_session, "tram_trips", 0)
        assert_row_count(db_session, "tram_stop_times", 0)

        # Process
        process_tram_static_data(tram_data_dir)

        # ── GTFS tables ────────────────────────────────────────────
        assert_row_count(db_session, "tram_agencies", 1)
        assert_rows(
            db_session,
            "tram_agencies",
            [
                {
                    "id": 1,
                    "name": "Luas",
                    "url": "https://www.luas.ie/",
                    "timezone": "Europe/Dublin",
                },
            ],
        )

        assert_row_count(db_session, "tram_calendar_schedule", 3)
        assert_row_count(db_session, "tram_calendar_dates", 2)
        assert_row_count(db_session, "tram_routes", 2)

        assert_row_count(db_session, "tram_stops", 4)
        assert_rows(
            db_session,
            "tram_stops",
            [
                {
                    "id": "LUAS1",
                    "code": 8001,
                    "name": "St. Stephen's Green",
                    "description": "Green Line terminus",
                    "lat": 53.339428,
                    "lon": -6.261495,
                },
                {
                    "id": "LUAS2",
                    "code": 8002,
                    "name": "Harcourt",
                    "description": None,
                    "lat": 53.333333,
                    "lon": -6.262222,
                },
                {
                    "id": "LUAS3",
                    "code": 8003,
                    "name": "Charlemont",
                    "description": None,
                    "lat": 53.330556,
                    "lon": -6.258889,
                },
                {
                    "id": "LUAS4",
                    "code": 8004,
                    "name": "The Point",
                    "description": "Red Line stop",
                    "lat": 53.348056,
                    "lon": -6.229167,
                },
            ],
        )

        assert_row_count(db_session, "tram_trip_shapes", 5)
        assert_row_count(db_session, "tram_trips", 5)
        assert_row_count(db_session, "tram_stop_times", 4)

        # ── CSO tables ─────────────────────────────────────────────
        assert_row_count(db_session, "tram_passenger_journeys", 3)
        assert_row_count(db_session, "tram_passenger_numbers", 2)
        assert_row_count(db_session, "tram_hourly_distribution", 2)
        assert_row_count(db_session, "tram_weekly_flow", 2)

        # Cleanup combined dir
        shutil.rmtree(tram_data_dir)

    def test_missing_required_gtfs_file_raises_error(
        self,
        db_session: Session,
        tmp_path: Path,
    ) -> None:
        """Missing a required GTFS file raises FileNotFoundError."""
        # Create directory with only agency.txt (missing all others)
        data_dir = tmp_path / "incomplete_tram"
        data_dir.mkdir()
        (data_dir / "agency.txt").write_text(
            "agency_id,agency_name,agency_url,agency_timezone\n"
            "1,Luas,https://luas.ie,Europe/Dublin\n"
        )

        with pytest.raises(FileNotFoundError, match="Required CSV file not found"):
            process_tram_static_data(data_dir)

    def test_optional_calendar_dates_skipped_when_missing(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Processing works even without optional calendar_dates.txt."""
        # Build dir with GTFS but no calendar_dates.txt
        tram_data_dir = tests_data_dir / "tram" / "no_cal_dates"
        tram_data_dir.mkdir(parents=True, exist_ok=True)

        gtfs_dir = tests_data_dir / "tram" / "gtfs"
        for f in gtfs_dir.iterdir():
            if f.name != "calendar_dates.txt":
                shutil.copy2(f, tram_data_dir / f.name)

        process_tram_static_data(tram_data_dir)

        # calendar_dates should be empty, everything else populated
        assert_row_count(db_session, "tram_calendar_dates", 0)
        assert_row_count(db_session, "tram_agencies", 1)
        assert_row_count(db_session, "tram_stops", 4)

        shutil.rmtree(tram_data_dir)

    def test_optional_cso_files_skipped_when_missing(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Processing works without any CSO files present."""
        # GTFS-only directory (no CSO files)
        tram_data_dir = tests_data_dir / "tram" / "gtfs_only"
        tram_data_dir.mkdir(parents=True, exist_ok=True)

        gtfs_dir = tests_data_dir / "tram" / "gtfs"
        for f in gtfs_dir.iterdir():
            shutil.copy2(f, tram_data_dir / f.name)

        process_tram_static_data(tram_data_dir)

        # GTFS populated, CSO tables empty
        assert_row_count(db_session, "tram_agencies", 1)
        assert_row_count(db_session, "tram_trips", 5)
        assert_row_count(db_session, "tram_passenger_journeys", 0)
        assert_row_count(db_session, "tram_passenger_numbers", 0)
        assert_row_count(db_session, "tram_hourly_distribution", 0)
        assert_row_count(db_session, "tram_weekly_flow", 0)

        shutil.rmtree(tram_data_dir)

    def test_idempotent_reprocessing(
        self,
        db_session: Session,
        tests_data_dir: Path,
    ) -> None:
        """Running process twice produces same row counts (delete-then-insert)."""
        tram_data_dir = tests_data_dir / "tram" / "idempotent"
        tram_data_dir.mkdir(parents=True, exist_ok=True)

        gtfs_dir = tests_data_dir / "tram" / "gtfs"
        for f in gtfs_dir.iterdir():
            shutil.copy2(f, tram_data_dir / f.name)

        schema = get_db_settings().postgres_schema

        process_tram_static_data(tram_data_dir)
        first_count = db_session.execute(
            text(f"SELECT COUNT(*) FROM {schema}.tram_stops")
        ).scalar()

        process_tram_static_data(tram_data_dir)
        second_count = db_session.execute(
            text(f"SELECT COUNT(*) FROM {schema}.tram_stops")
        ).scalar()

        assert first_count == second_count == 4

        shutil.rmtree(tram_data_dir)
