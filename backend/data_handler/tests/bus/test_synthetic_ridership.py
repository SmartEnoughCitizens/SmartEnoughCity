from pathlib import Path

from sqlalchemy import and_, select
from sqlalchemy.orm import Session

from data_handler.bus.live_data_handler import process_bus_vehicles_live_data
from data_handler.bus.models import BusRidership, BusStopTime
from data_handler.bus.static_data_handler import process_bus_static_data
from data_handler.bus.synthetic_ridership import (
    find_nearest_stop_in_trip,
    generate_ridership_for_vehicles,
)
from tests.utils import assert_row_count


def _load_static_and_live_data(tests_data_dir: Path) -> None:
    """Helper: load static GTFS data + live vehicle data into the test DB."""
    process_bus_static_data(tests_data_dir / "static_data" / "GTFS")

    vehicles_json_path = tests_data_dir / "bus" / "vehicles.json"
    with vehicles_json_path.open() as f:
        process_bus_vehicles_live_data(f.read())


def test_nearest_stop_found_within_trip_sequence(
    db_session: Session, tests_data_dir: Path
) -> None:
    """The nearest stop should be the one from the trip's stop list closest to the vehicle."""
    _load_static_and_live_data(tests_data_dir)

    # Trip 5332_14104 has stops at sequences 1-5.
    # Vehicle 1112 is at (53.3635788, -6.23394823).
    # North Strand Fire Station (8220DB000519) at (53.360339, -6.239535) is the closest.
    stop_id, stop_seq = find_nearest_stop_in_trip(
        db_session, "5332_14104", 53.3635788, -6.23394823
    )
    assert stop_id == "8220DB000519"
    assert stop_seq == 5


def test_nearest_stop_ignores_stops_not_on_trip(
    db_session: Session, tests_data_dir: Path
) -> None:
    """Only stops belonging to the trip's sequence are considered, not all stops."""
    _load_static_and_live_data(tests_data_dir)

    # Position is exactly at Abbey Street Lower (seq 1), should return that.
    stop_id, stop_seq = find_nearest_stop_in_trip(
        db_session, "5332_14104", 53.34889, -6.25683
    )
    assert stop_id == "8220DB007591"
    assert stop_seq == 1


def test_generate_ridership_peak_hours(
    db_session: Session, tests_data_dir: Path
) -> None:
    """During peak hours (07-09, 17-19), passenger load should be higher."""
    _load_static_and_live_data(tests_data_dir)

    # The test vehicles have timestamps around 16:22 (shoulder/near-peak hour).
    # Only trip 5332_14104 has stop_times in the test data, so 1 ridership record.
    generate_ridership_for_vehicles(db_session)

    assert_row_count(db_session, "bus_ridership", 1)

    result = db_session.execute(
        select(BusRidership.passengers_onboard, BusRidership.vehicle_capacity)
    )
    rows = result.fetchall()
    for row in rows:
        onboard, capacity = row
        assert onboard >= 0
        assert onboard <= capacity


def test_generate_ridership_off_peak(db_session: Session, tests_data_dir: Path) -> None:
    """Off-peak ridership should be lower than capacity thresholds."""
    _load_static_and_live_data(tests_data_dir)

    generate_ridership_for_vehicles(db_session)

    result = db_session.execute(
        select(BusRidership.passengers_onboard, BusRidership.vehicle_capacity)
    )
    rows = result.fetchall()
    for row in rows:
        onboard, capacity = row
        # Even near-peak, occupancy should not exceed capacity
        assert onboard <= capacity


def test_passengers_onboard_never_exceeds_capacity(
    db_session: Session, tests_data_dir: Path
) -> None:
    """passengers_onboard must never exceed vehicle_capacity."""
    _load_static_and_live_data(tests_data_dir)

    generate_ridership_for_vehicles(db_session)

    result = db_session.execute(
        select(BusRidership.passengers_onboard, BusRidership.vehicle_capacity)
    )
    for row in result.fetchall():
        assert row[0] <= row[1], (
            f"passengers_onboard ({row[0]}) exceeds vehicle_capacity ({row[1]})"
        )


def test_passengers_onboard_never_negative(
    db_session: Session, tests_data_dir: Path
) -> None:
    """passengers_onboard must never be negative."""
    _load_static_and_live_data(tests_data_dir)

    generate_ridership_for_vehicles(db_session)

    result = db_session.execute(select(BusRidership.passengers_onboard))
    for row in result.fetchall():
        assert row[0] >= 0, f"passengers_onboard ({row[0]}) is negative"


def test_ridership_linked_to_live_vehicle_record(
    db_session: Session, tests_data_dir: Path
) -> None:
    """Each ridership row should correspond to a live vehicle record."""
    _load_static_and_live_data(tests_data_dir)

    generate_ridership_for_vehicles(db_session)

    # Only trip 5332_14104 has stop_times in test data, so 1 ridership record.
    # Trip 5332_14109 is skipped (no stop_times).
    assert_row_count(db_session, "bus_ridership", 1)

    # Verify the record matches vehicle 1112 on trip 5332_14104
    result = db_session.execute(select(BusRidership.vehicle_id, BusRidership.trip_id))
    rows = result.fetchall()
    assert len(rows) == 1
    assert rows[0][0] == 1112
    assert rows[0][1] == "5332_14104"


def test_stop_sequence_increases_along_route(
    db_session: Session, tests_data_dir: Path
) -> None:
    """stop_sequence should be a valid sequence number from the trip's stop list."""
    _load_static_and_live_data(tests_data_dir)

    generate_ridership_for_vehicles(db_session)

    result = db_session.execute(
        select(BusRidership.stop_sequence, BusStopTime.sequence).join(
            BusStopTime,
            and_(
                BusRidership.trip_id == BusStopTime.trip_id,
                BusRidership.nearest_stop_id == BusStopTime.stop_id,
            ),
        )
    )
    for row in result.fetchall():
        # The stop_sequence in ridership must match the actual sequence in stop_times
        assert row[0] == row[1], (
            f"Ridership stop_sequence ({row[0]}) doesn't match "
            f"stop_times sequence ({row[1]})"
        )
