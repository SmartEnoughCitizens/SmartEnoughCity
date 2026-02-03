from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.settings.data_sources_settings import get_data_sources_settings

from tests.utils import assert_row_count, assert_rows


def test_process_bus_static_data(db_session) -> None:
    assert_row_count(db_session, "bus_stops", 0)
    assert_row_count(db_session, "bus_routes", 0)
    assert_row_count(db_session, "bus_trips", 0)
    assert_row_count(db_session, "bus_stop_times", 0)
    assert_row_count(db_session, "bus_trip_shapes", 0)
    assert_row_count(db_session, "bus_agencies", 0)
    assert_row_count(db_session, "bus_calendar_schedule", 0)

    sources_settings = get_data_sources_settings()
    process_bus_static_data(sources_settings.bus_gtfs_static_data_dir)

    assert_row_count(db_session, "bus_stops", 5)
    assert_rows(db_session, "bus_stops", [
        {
            "id": "8220DB007591",
            "code": 7591,
            "name": "Abbey Street Lower",
            "description": None,
            "lat": 53.34889,
            "lon": -6.25683,
        },
        {
            "id": "8220DB000496",
            "code": 496,
            "name": "Busáras",
            "description": None,
            "lat": 53.3494968761341,
            "lon": -6.25222357868164,
        },
        {
            "id": "8220DB000515",
            "code": 515,
            "name": "Five Lamps",
            "description": None,
            "lat": 53.35439,
            "lon": -6.2472,
        },
        {
            "id": "8220DB000516",
            "code": 516,
            "name": "Newcomen Bridge",
            "description": None,
            "lat": 53.35621,
            "lon": -6.244942,
        },
        {
            "id": "8220DB000519",
            "code": 519,
            "name": "North Strand Fire Station",
            "description": None,
            "lat": 53.360339,
            "lon": -6.239535,
        },
    ])
    assert_row_count(db_session, "bus_routes", 5)
    assert_row_count(db_session, "bus_trips", 9)
    assert_row_count(db_session, "bus_stop_times", 5)
    assert_row_count(db_session, "bus_trip_shapes", 5)
    assert_row_count(db_session, "bus_agencies", 2)
    assert_row_count(db_session, "bus_calendar_schedule", 4)
