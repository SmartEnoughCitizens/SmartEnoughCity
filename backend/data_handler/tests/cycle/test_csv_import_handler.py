"""Tests for historical CSV import handler."""

import sys
from datetime import UTC, datetime
from pathlib import Path
from unittest.mock import Mock

import pytest
from sqlalchemy.orm import DeclarativeBase


from data_handler.cycle.csv_import_handler import (
    REQUIRED_HEADERS,
    import_all_station_history_csvs,
    import_station_history_csv,
    parse_station_history_csv_row,
)


class TestParseStationHistoryCsvRow:
    """Test transforming CSV rows to database records."""

    def test_parses_valid_row(self) -> None:
        """Test parsing a complete valid CSV row."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "8",
            "num_docks_available": "2",
            "is_installed": "true",
            "is_renting": "true",
            "is_returning": "true",
            "name": "Mary Street",
            "short_name": "001",
            "address": "Mary Street Dublin 1",
            "lat": "53.349316",
            "lon": "-6.262876",
            "region_id": "dublin_central",
            "capacity": "30",
        }

        result = parse_station_history_csv_row(row)

        assert result["station_id"] == 1
        assert result["available_bikes"] == 8
        assert result["available_docks"] == 2
        assert result["is_installed"] is True
        assert result["is_renting"] is True
        assert result["is_returning"] is True
        assert result["timestamp"] == datetime(2025, 7, 15, 14, 30, 0, tzinfo=UTC)

    def test_converts_string_booleans(self) -> None:
        """Test that string booleans are converted to Python booleans."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "false",
            "is_renting": "false",
            "is_returning": "true",
        }

        result = parse_station_history_csv_row(row)

        assert result["is_installed"] is False
        assert result["is_renting"] is False
        assert result["is_returning"] is True

    def test_ignores_redundant_station_metadata(self) -> None:
        """Test that redundant fields (name, address, lat, etc.) are ignored."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "true",
            "is_renting": "true",
            "is_returning": "true",
            "name": "Should be ignored",
            "address": "Should be ignored",
        }

        result = parse_station_history_csv_row(row)

        assert "name" not in result
        assert "address" not in result
        assert "lat" not in result
        assert "lon" not in result

    def test_raises_on_missing_required_field(self) -> None:
        """Test that missing required field raises ValueError."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            # Missing station_id
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "true",
            "is_renting": "true",
            "is_returning": "true",
        }

        with pytest.raises(ValueError):
            parse_station_history_csv_row(row)

    def test_raises_on_invalid_boolean(self) -> None:
        """Test that invalid boolean string raises ValueError."""
        row = {
            "system_id": "dublin",
            "last_reported": "2025-07-15T14:30:00+00:00",
            "station_id": "1",
            "num_bikes_available": "5",
            "num_docks_available": "10",
            "is_installed": "maybe",  # Invalid
            "is_renting": "true",
            "is_returning": "true",
        }

        with pytest.raises(ValueError):
            parse_station_history_csv_row(row)


class TestRequiredHeaders:
    """Test the REQUIRED_HEADERS constant."""

    def test_required_headers_contains_essential_fields(self) -> None:
        """Test that REQUIRED_HEADERS includes all essential CSV columns."""
        essential = [
            "station_id",
            "last_reported",
            "num_bikes_available",
            "num_docks_available",
            "is_installed",
            "is_renting",
            "is_returning",
        ]
        for field in essential:
            assert field in REQUIRED_HEADERS


class TestImportStationHistoryCsv:
    """Test CSV file import function."""

    def test_raises_on_missing_file(self) -> None:
        """Test that missing CSV file raises FileNotFoundError."""
        fake_path = Path("/nonexistent/file.csv")
        with pytest.raises(FileNotFoundError):
            import_station_history_csv(fake_path)

    def test_imports_csv_successfully(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Test successful CSV import with valid data."""
        csv_file = tmp_path / "test.csv"
        csv_file.write_text(
            "system_id,last_reported,station_id,num_bikes_available,"
            "num_docks_available,is_installed,is_renting,is_returning\n"
            "dublin,2025-07-15T14:30:00+00:00,1,8,2,true,true,true\n"
            "dublin,2025-07-15T14:30:00+00:00,2,5,10,true,false,true\n"
        )

        mock_session = Mock()
        mock_session_ctx = Mock()
        mock_session_ctx.__enter__ = Mock(return_value=mock_session)
        mock_session_ctx.__exit__ = Mock(return_value=False)
        monkeypatch.setattr(
            "data_handler.cycle.csv_import_handler.SessionLocal",
            lambda: mock_session_ctx,
        )

        import_station_history_csv(csv_file)

        mock_session.execute.assert_called_once()
        mock_session.commit.assert_called_once()

    def test_returns_early_on_empty_csv(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Test that CSV with only headers returns early."""
        csv_file = tmp_path / "empty.csv"
        csv_file.write_text(
            "system_id,last_reported,station_id,num_bikes_available,"
            "num_docks_available,is_installed,is_renting,is_returning\n"
        )

        import_station_history_csv(csv_file)

    def test_raises_on_db_error(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Test that database errors are propagated."""
        csv_file = tmp_path / "test.csv"
        csv_file.write_text(
            "system_id,last_reported,station_id,num_bikes_available,"
            "num_docks_available,is_installed,is_renting,is_returning\n"
            "dublin,2025-07-15T14:30:00+00:00,1,8,2,true,true,true\n"
        )

        mock_session = Mock()
        mock_session.execute.side_effect = Exception("DB error")
        mock_session_ctx = Mock()
        mock_session_ctx.__enter__ = Mock(return_value=mock_session)
        mock_session_ctx.__exit__ = Mock(return_value=False)
        monkeypatch.setattr(
            "data_handler.cycle.csv_import_handler.SessionLocal",
            lambda: mock_session_ctx,
        )

        with pytest.raises(Exception, match="DB error"):
            import_station_history_csv(csv_file)


class TestImportAllStationHistoryCsvs:
    """Test batch CSV import function."""

    def test_raises_on_invalid_directory(self) -> None:
        """Test that non-existent directory raises ValueError."""
        with pytest.raises(ValueError, match="Invalid directory"):
            import_all_station_history_csvs(Path("/nonexistent/dir"))

    def test_raises_on_file_instead_of_directory(self, tmp_path: Path) -> None:
        """Test that passing a file path raises ValueError."""
        f = tmp_path / "not_a_dir.csv"
        f.write_text("data")
        with pytest.raises(ValueError, match="Invalid directory"):
            import_all_station_history_csvs(f)

    def test_handles_empty_directory(self, tmp_path: Path) -> None:
        """Test that empty directory logs warning and returns."""
        import_all_station_history_csvs(tmp_path)

    def test_imports_matching_csv_files(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Test that matching CSV files are imported."""
        csv_content = (
            "system_id,last_reported,station_id,num_bikes_available,"
            "num_docks_available,is_installed,is_renting,is_returning\n"
            "dublin,2025-07-15T14:30:00+00:00,1,8,2,true,true,true\n"
        )
        (tmp_path / "dublin-bikes_station_status_2025-07.csv").write_text(csv_content)
        (tmp_path / "dublin-bikes_station_status_2025-08.csv").write_text(csv_content)
        (tmp_path / "other_file.csv").write_text(csv_content)  # should not match

        mock_session = Mock()
        mock_session_ctx = Mock()
        mock_session_ctx.__enter__ = Mock(return_value=mock_session)
        mock_session_ctx.__exit__ = Mock(return_value=False)
        monkeypatch.setattr(
            "data_handler.cycle.csv_import_handler.SessionLocal",
            lambda: mock_session_ctx,
        )

        import_all_station_history_csvs(tmp_path)

        # Should have imported 2 files (not the "other_file.csv")
        assert mock_session.execute.call_count == 2
        assert mock_session.commit.call_count == 2

    def test_continues_on_import_error(
        self, tmp_path: Path, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Test that failing imports don't stop processing other files."""
        csv_content = (
            "system_id,last_reported,station_id,num_bikes_available,"
            "num_docks_available,is_installed,is_renting,is_returning\n"
            "dublin,2025-07-15T14:30:00+00:00,1,8,2,true,true,true\n"
        )
        (tmp_path / "dublin-bikes_station_status_2025-07.csv").write_text(csv_content)
        (tmp_path / "dublin-bikes_station_status_2025-08.csv").write_text(csv_content)

        call_count = 0

        def mock_session_factory() -> Mock:
            nonlocal call_count
            call_count += 1
            mock_session = Mock()
            mock_session_ctx = Mock()
            if call_count == 1:
                mock_session.execute.side_effect = Exception("DB error")
            mock_session_ctx.__enter__ = Mock(return_value=mock_session)
            mock_session_ctx.__exit__ = Mock(return_value=False)
            return mock_session_ctx

        monkeypatch.setattr(
            "data_handler.cycle.csv_import_handler.SessionLocal",
            mock_session_factory,
        )

        # Should NOT raise - errors are caught and logged
        import_all_station_history_csvs(tmp_path)
