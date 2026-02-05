from datetime import date, datetime, time
from pathlib import Path

from sqlalchemy.orm import Session

from data_handler.bus.live_data_handler import (
    process_bus_trip_updates_live_data,
    process_bus_vehicles_live_data,
)
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.settings.data_sources_settings import get_data_sources_settings
from tests.utils import ANY, assert_row_count, assert_rows


def test_process_bus_vehicles_live_data(db_session: Session, tests_data_dir: Path) -> None:
    # Static data needed for live data due to foreign key constraints
    sources_settings = get_data_sources_settings()
    process_bus_static_data(sources_settings.bus_gtfs_static_data_dir)

    vehicles_json_path = tests_data_dir / "bus" / "vehicles.json"
    with vehicles_json_path.open() as f:
        vehicles_json_string = f.read()

    assert_row_count(db_session, "bus_live_vehicles", 0)

    process_bus_vehicles_live_data(vehicles_json_string)

    assert_row_count(db_session, "bus_live_vehicles", 2)
    assert_rows(db_session, "bus_live_vehicles", [
        {
            "entry_id": ANY,
            "vehicle_id": 439,
            "trip_id": "5332_14109",
            "start_time": time(15, 10, 0),
            "start_date": date(2026, 1, 29),
            "schedule_relationship": "scheduled",
            "direction_id": 0,
            "lat": 53.3882332,
            "lon": -6.07012177,
            "timestamp": datetime(2026, 1, 29, 16, 22, 36),
        },
        {
            "entry_id": ANY,
            "vehicle_id": 1112,
            "trip_id": "5332_14104",
            "start_time": time(16, 10, 0),
            "start_date": date(2026, 1, 29),
            "schedule_relationship": "scheduled",
            "direction_id": 0,
            "lat": 53.3635788,
            "lon": -6.23394823,
            "timestamp": datetime(2026, 1, 29, 16, 22, 25),
        },
    ])


def test_process_bus_trip_updates_live_data(db_session: Session, tests_data_dir: Path) -> None:
    # Static data needed for live data due to foreign key constraints
    sources_settings = get_data_sources_settings()
    process_bus_static_data(sources_settings.bus_gtfs_static_data_dir)

    trip_updates_json_path = tests_data_dir / "bus" / "TripUpdates.json"
    with trip_updates_json_path.open() as f:
        trip_updates_json_string = f.read()

    assert_row_count(db_session, "bus_live_trip_updates", 0)
    assert_row_count(db_session, "bus_live_trip_updates_stop_time_updates", 0)

    process_bus_trip_updates_live_data(trip_updates_json_string)

    assert_row_count(db_session, "bus_live_trip_updates", 2)
    assert_rows(db_session, "bus_live_trip_updates", [
        {
            "entry_id": ANY,
            "trip_id": "5332_14106",
            "start_time": time(14, 10, 0),
            "start_date": date(2026, 1, 29),
            "schedule_relationship": "scheduled",
            "direction_id": 0,
            "vehicle_id": 665,
            "timestamp": datetime(2026, 1, 29, 15, 35, 53),
        },
        {
            "entry_id": ANY,
            "trip_id": "5332_14109",
            "start_time": time(15, 10, 0),
            "start_date": date(2026, 1, 29),
            "schedule_relationship": "scheduled",
            "direction_id": 0,
            "vehicle_id": 439,
            "timestamp": datetime(2026, 1, 29, 15, 35, 53),
        },
    ])
    assert_row_count(db_session, "bus_live_trip_updates_stop_time_updates", 8)
    assert_rows(db_session, "bus_live_trip_updates_stop_time_updates", [
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB007591",
            "stop_sequence": 21,
            "schedule_relationship": "scheduled",
            "arrival_delay": 32,
            "departure_delay": 32,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB000496",
            "stop_sequence": 22,
            "schedule_relationship": "scheduled",
            "arrival_delay": 93,
            "departure_delay": 93,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB000515",
            "stop_sequence": 27,
            "schedule_relationship": "scheduled",
            "arrival_delay": 29,
            "departure_delay": 29,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB000516",
            "stop_sequence": 29,
            "schedule_relationship": "scheduled",
            "arrival_delay": -52,
            "departure_delay": -52,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB000519",
            "stop_sequence": 40,
            "schedule_relationship": "scheduled",
            "arrival_delay": 11,
            "departure_delay": 11,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB007591",
            "stop_sequence": 5,
            "schedule_relationship": "scheduled",
            "arrival_delay": None,
            "departure_delay": 198,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB000496",
            "stop_sequence": 9,
            "schedule_relationship": "scheduled",
            "arrival_delay": 117,
            "departure_delay": 117,
        },
        {
            "entry_id": ANY,
            "trip_update_entry_id": ANY,
            "stop_id": "8220DB000515",
            "stop_sequence": 11,
            "schedule_relationship": "scheduled",
            "arrival_delay": 178,
            "departure_delay": 178,
        },
    ])
