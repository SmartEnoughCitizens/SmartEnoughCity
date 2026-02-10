from datetime import UTC, datetime, timedelta, timezone
from pathlib import Path

from sqlalchemy.orm import Session

from data_handler.pedestrians.live_data_handler import (
    process_batch_job_result,
    process_pedestrian_channel_data,
    process_pedestrian_measures_data,
    process_pedestrian_sites,
)
from tests.utils import ANY, assert_row_count, assert_rows


def test_process_pedestrian_sites(db_session: Session, tests_data_dir: Path) -> None:
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        sites_json_string = f.read()

    assert_row_count(db_session, "pedestrian_counter_sites", 0)

    updated_ids = process_pedestrian_sites(sites_json_string)
    assert len(updated_ids) == 3
    assert set(updated_ids) == {100000425, 100001297, 100001484}

    assert_row_count(db_session, "pedestrian_counter_sites", 3)
    assert_rows(
        db_session,
        "pedestrian_counter_sites",
        [
            {
                "id": 100000425,
                "name": "Glenageary",
                "description": None,
                "lat": 53.28141,
                "lon": -6.12319,
                "first_data": datetime(
                    2010, 10, 24, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT1H",
                "pedestrian_sensor": True,
                "bike_sensor": True,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
            {
                "id": 100001297,
                "name": "Westmoreland WEST old",
                "description": None,
                "lat": 53.3460338367334,
                "lon": -6.25927465033187,
                "first_data": datetime(
                    2011, 5, 5, 3, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
            {
                "id": 100001484,
                "name": "O'Connell St/Pennys",
                "description": "Counter Re-located on 8/3/17 to Pennys as PL poles outside Burger King and Schuh interfering with counter. Mounted at correct height on new pole.",
                "lat": 53.34879,
                "lon": -6.25969,
                "first_data": datetime(
                    2011, 6, 7, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
        ],
    )


def test_process_pedestrian_sites_upsert(
    db_session: Session, tests_data_dir: Path
) -> None:
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        sites_json_string = f.read()

    sites_upsert_json_path = tests_data_dir / "pedestrians" / "sites2.json"
    with sites_upsert_json_path.open() as f:
        sites_upsert_json_string = f.read()

    assert_row_count(db_session, "pedestrian_counter_sites", 0)

    process_pedestrian_sites(sites_json_string)
    updated_ids = process_pedestrian_sites(sites_upsert_json_string)
    assert len(updated_ids) == 2
    assert set(updated_ids) == {100000425, 100001485}

    assert_row_count(db_session, "pedestrian_counter_sites", 4)
    assert_rows(
        db_session,
        "pedestrian_counter_sites",
        [
            {
                "id": 100001297,
                "name": "Westmoreland WEST old",
                "description": None,
                "lat": 53.3460338367334,
                "lon": -6.25927465033187,
                "first_data": datetime(
                    2011, 5, 5, 3, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
            {
                "id": 100001484,
                "name": "O'Connell St/Pennys",
                "description": "Counter Re-located on 8/3/17 to Pennys as PL poles outside Burger King and Schuh interfering with counter. Mounted at correct height on new pole.",
                "lat": 53.34879,
                "lon": -6.25969,
                "first_data": datetime(
                    2011, 6, 7, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
            {
                "id": 100000425,
                "name": "Glenageary modified",
                "description": "Modified description",
                "lat": 13.28141,
                "lon": -13.12319,
                "first_data": datetime(
                    2020, 10, 24, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": False,
                "has_timestamped_data": True,
                "has_weather": False,
            },
            {
                "id": 100001485,
                "name": "O'Connell st/Princes st North",
                "description": "Outside Clerys 15M Pyro. New type pyrobox.",
                "lat": 53.34895,
                "lon": -6.26004,
                "first_data": datetime(
                    2011, 6, 7, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
        ],
    )


def test_process_pedestrian_channel_data(
    db_session: Session, tests_data_dir: Path
) -> None:
    # Sites must exist first (FK from channels to sites)
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        process_pedestrian_sites(f.read())

    channels_csv_path = tests_data_dir / "pedestrians" / "channels.csv"
    assert_row_count(db_session, "pedestrian_channels", 0)

    with channels_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_channel_data(f)

    assert_row_count(db_session, "pedestrian_channels", 4)
    assert_rows(
        db_session,
        "pedestrian_channels",
        [
            {
                "channel_id": 101000425,
                "site_id": 100000425,
                "mobility_type": "BIKE",
                "direction": "IN",
                "time_step": 3600,
            },
            {
                "channel_id": 102000425,
                "site_id": 100000425,
                "mobility_type": "BIKE",
                "direction": "OUT",
                "time_step": 3600,
            },
            {
                "channel_id": 103000425,
                "site_id": 100000425,
                "mobility_type": "PEDESTRIAN",
                "direction": "IN",
                "time_step": 3600,
            },
            {
                "channel_id": 104000425,
                "site_id": 100000425,
                "mobility_type": "PEDESTRIAN",
                "direction": "OUT",
                "time_step": 3600,
            },
        ],
    )


def test_process_pedestrian_channel_data_upsert(
    db_session: Session, tests_data_dir: Path
) -> None:
    # Sites must exist first (FK from channels to sites)
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        process_pedestrian_sites(f.read())

    channels_csv_path = tests_data_dir / "pedestrians" / "channels.csv"
    with channels_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_channel_data(f)

    channels_csv_upsert_path = tests_data_dir / "pedestrians" / "channels2.csv"

    with channels_csv_upsert_path.open(encoding="utf-8") as f:
        process_pedestrian_channel_data(f)

    assert_row_count(db_session, "pedestrian_channels", 4)
    assert_rows(
        db_session,
        "pedestrian_channels",
        [
            {
                "channel_id": 103000425,
                "site_id": 100000425,
                "mobility_type": "PEDESTRIAN",
                "direction": "IN",
                "time_step": 3600,
            },
            {
                "channel_id": 104000425,
                "site_id": 100000425,
                "mobility_type": "PEDESTRIAN",
                "direction": "OUT",
                "time_step": 3600,
            },
            {
                "channel_id": 101000425,
                "site_id": 100000425,
                "mobility_type": "BIKE",
                "direction": "IN",
                "time_step": 900,
            },
            {
                "channel_id": 102000425,
                "site_id": 100000425,
                "mobility_type": "BIKE",
                "direction": "OUT",
                "time_step": 900,
            },
        ],
    )


def test_process_pedestrian_measures_data(
    db_session: Session, tests_data_dir: Path
) -> None:
    # Sites and channels must exist first (FK from measures to channels)
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        process_pedestrian_sites(f.read())

    # Measures must exist first (FK from measures to channels)
    channels_csv_path = tests_data_dir / "pedestrians" / "channels.csv"
    with channels_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_channel_data(f)

    measures_csv_path = tests_data_dir / "pedestrians" / "measures.csv"
    assert_row_count(db_session, "pedestrian_counter_measures", 0)

    with measures_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_measures_data(f)

    assert_row_count(db_session, "pedestrian_counter_measures", 4)
    assert_rows(
        db_session,
        "pedestrian_counter_measures",
        [
            {
                "id": ANY,
                "channel_id": 101000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 13, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 14, 0, 0, tzinfo=UTC),
                "count": 9,
            },
            {
                "id": ANY,
                "channel_id": 101000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 14, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 15, 0, 0, tzinfo=UTC),
                "count": 15,
            },
            {
                "id": ANY,
                "channel_id": 102000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 16, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 17, 0, 0, tzinfo=UTC),
                "count": 9,
            },
            {
                "id": ANY,
                "channel_id": 103000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 11, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 12, 0, 0, tzinfo=UTC),
                "count": 30,
            },
        ],
    )


def test_process_pedestrian_measures_data_upsert(
    db_session: Session, tests_data_dir: Path
) -> None:
    # Sites and channels must exist first
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        process_pedestrian_sites(f.read())

    # Channels must exist first (FK from measures to channels)
    channels_csv_path = tests_data_dir / "pedestrians" / "channels.csv"
    with channels_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_channel_data(f)

    measures_csv_path = tests_data_dir / "pedestrians" / "measures.csv"
    with measures_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_measures_data(f)

    # measures2.csv updates one row (same channel_id, start_datetime, end_datetime)
    # and leaves others unchanged
    measures2_csv_path = tests_data_dir / "pedestrians" / "measures2.csv"
    with measures2_csv_path.open(encoding="utf-8") as f:
        process_pedestrian_measures_data(f)

    # Still 4 rows (one updated, no new row)
    assert_row_count(db_session, "pedestrian_counter_measures", 4)
    assert_rows(
        db_session,
        "pedestrian_counter_measures",
        [
            {
                "id": ANY,
                "channel_id": 101000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 14, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 15, 0, 0, tzinfo=UTC),
                "count": 15,
            },
            {
                "id": ANY,
                "channel_id": 102000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 16, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 17, 0, 0, tzinfo=UTC),
                "count": 9,
            },
            {
                "id": ANY,
                "channel_id": 103000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 11, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 12, 0, 0, tzinfo=UTC),
                "count": 30,
            },
            {
                "id": ANY,
                "channel_id": 101000425,
                "counter_id": "UPDATED",
                "start_datetime": datetime(2026, 2, 8, 13, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 14, 0, 0, tzinfo=UTC),
                "count": 99,
            },
        ],
    )


def test_process_batch_job_result(db_session: Session, tests_data_dir: Path) -> None:
    sites_json_path = tests_data_dir / "pedestrians" / "sites.json"
    with sites_json_path.open() as f:
        sites_json_string = f.read()

    assert_row_count(db_session, "pedestrian_counter_sites", 0)

    updated_ids = process_pedestrian_sites(sites_json_string)
    assert len(updated_ids) == 3
    assert set(updated_ids) == {100000425, 100001297, 100001484}

    batch_job_result_zip_path = tests_data_dir / "pedestrians" / "response.zip"

    assert_row_count(db_session, "pedestrian_channels", 0)
    assert_row_count(db_session, "pedestrian_counter_measures", 0)

    with batch_job_result_zip_path.open(mode="rb") as f:
        process_batch_job_result(f.read())

    assert_row_count(db_session, "pedestrian_counter_sites", 3)
    assert_rows(
        db_session,
        "pedestrian_counter_sites",
        [
            {
                "id": 100000425,
                "name": "Glenageary",
                "description": None,
                "lat": 53.28141,
                "lon": -6.12319,
                "first_data": datetime(
                    2010, 10, 24, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT1H",
                "pedestrian_sensor": True,
                "bike_sensor": True,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
            {
                "id": 100001297,
                "name": "Westmoreland WEST old",
                "description": None,
                "lat": 53.3460338367334,
                "lon": -6.25927465033187,
                "first_data": datetime(
                    2011, 5, 5, 3, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
            {
                "id": 100001484,
                "name": "O'Connell St/Pennys",
                "description": "Counter Re-located on 8/3/17 to Pennys as PL poles outside Burger King and Schuh interfering with counter. Mounted at correct height on new pole.",
                "lat": 53.34879,
                "lon": -6.25969,
                "first_data": datetime(
                    2011, 6, 7, 0, 0, 0, tzinfo=timezone(timedelta(hours=1))
                ),
                "granularity": "PT15M",
                "pedestrian_sensor": True,
                "bike_sensor": False,
                "directional": True,
                "has_timestamped_data": False,
                "has_weather": True,
            },
        ],
    )

    assert_row_count(db_session, "pedestrian_channels", 4)
    assert_rows(
        db_session,
        "pedestrian_channels",
        [
            {
                "channel_id": 101000425,
                "site_id": 100000425,
                "mobility_type": "BIKE",
                "direction": "IN",
                "time_step": 3600,
            },
            {
                "channel_id": 102000425,
                "site_id": 100000425,
                "mobility_type": "BIKE",
                "direction": "OUT",
                "time_step": 3600,
            },
            {
                "channel_id": 103000425,
                "site_id": 100000425,
                "mobility_type": "PEDESTRIAN",
                "direction": "IN",
                "time_step": 3600,
            },
            {
                "channel_id": 104000425,
                "site_id": 100000425,
                "mobility_type": "PEDESTRIAN",
                "direction": "OUT",
                "time_step": 3600,
            },
        ],
    )

    assert_row_count(db_session, "pedestrian_counter_measures", 4)
    assert_rows(
        db_session,
        "pedestrian_counter_measures",
        [
            {
                "id": ANY,
                "channel_id": 101000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 13, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 14, 0, 0, tzinfo=UTC),
                "count": 9,
            },
            {
                "id": ANY,
                "channel_id": 101000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 14, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 15, 0, 0, tzinfo=UTC),
                "count": 15,
            },
            {
                "id": ANY,
                "channel_id": 102000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 16, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 17, 0, 0, tzinfo=UTC),
                "count": 9,
            },
            {
                "id": ANY,
                "channel_id": 103000425,
                "counter_id": "X2H23070904",
                "start_datetime": datetime(2026, 2, 8, 11, 0, 0, tzinfo=UTC),
                "end_datetime": datetime(2026, 2, 8, 12, 0, 0, tzinfo=UTC),
                "count": 30,
            },
        ],
    )
