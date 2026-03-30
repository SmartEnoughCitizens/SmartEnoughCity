# tests/car/test_car_static_data.py

from datetime import datetime
from pathlib import Path

from sqlalchemy.orm import Session

from data_handler.car.process_car_data import process_car_static_data
from tests.utils import ANY, assert_row_count, assert_rows


def test_process_car_static_data(db_session: Session, tests_data_dir: Path) -> None:
    """Test complete car static data import process."""

    # ARRANGE: Verify all tables are empty
    assert_row_count(db_session, "scats_sites", 0)
    assert_row_count(db_session, "traffic_volumes", 0)
    assert_row_count(db_session, "vehicle_first_time", 0)
    assert_row_count(db_session, "vehicle_licensing_area", 0)
    assert_row_count(db_session, "vehicle_new_licensed", 0)
    assert_row_count(db_session, "vehicle_yearly", 0)
    assert_row_count(db_session, "private_car_emissions", 0)
    assert_row_count(db_session, "ev_charging_points", 0)
    assert_row_count(db_session, "ev_charging_demand", 0)
    assert_row_count(db_session, "ev_electoral_divisions", 0)

    # ACT: Import car data
    process_car_static_data(tests_data_dir / "static_data" / "car")

    # ASSERT: Verify row counts
    assert_row_count(db_session, "scats_sites", 3)
    assert_row_count(
        db_session, "traffic_volumes", 3
    )  # 1 unknown site (999) and 1 malformed row filtered out
    assert_row_count(db_session, "vehicle_first_time", 3)
    assert_row_count(db_session, "vehicle_licensing_area", 3)
    assert_row_count(db_session, "vehicle_new_licensed", 2)
    assert_row_count(db_session, "vehicle_yearly", 3)
    assert_row_count(db_session, "private_car_emissions", 3)
    assert_row_count(db_session, "ev_charging_points", 2)  # Only Dublin (Cork filtered)
    assert_row_count(db_session, "ev_charging_demand", 3)
    assert_row_count(db_session, "ev_electoral_divisions", 2)  # Dublin City + Fingal (Cork filtered)

    # ASSERT: Verify SCATS sites data
    assert_rows(
        db_session,
        "scats_sites",
        [
            {
                "site_id": 123,
                "description": "O'Connell Street",
                "description_lower": "o'connell street",
                "region": "Dublin",
                "lat": 53.3498,
                "lon": -6.2603,
            },
            {
                "site_id": 456,
                "description": "Dame Street",
                "description_lower": "dame street",
                "region": "Dublin",
                "lat": 53.3441,
                "lon": -6.2675,
            },
            {
                "site_id": 789,
                "description": "Grafton Street",
                "description_lower": "grafton street",
                "region": "Dublin",
                "lat": 53.3428,
                "lon": -6.2597,
            },
        ],
    )

    # ASSERT: Verify traffic volumes (unknown site 999 and malformed row filtered out)
    assert_rows(
        db_session,
        "traffic_volumes",
        [
            {
                "end_time": datetime(2025, 8, 26, 0, 0, 0),
                "site_id": 123,
                "detector": 1,
                "region": "Dublin",
                "sum_volume": 100,
                "avg_volume": 50,
            },
            {
                "end_time": datetime(2025, 8, 26, 0, 0, 0),
                "site_id": 456,
                "detector": 1,
                "region": "Dublin",
                "sum_volume": 200,
                "avg_volume": 80,
            },
            {
                "end_time": datetime(2025, 8, 26, 0, 0, 0),
                "site_id": 789,
                "detector": 1,
                "region": "Dublin",
                "sum_volume": 150,
                "avg_volume": 60,
            },
        ],
    )

    # ASSERT: Verify emissions data (with enum values)
    assert_rows(
        db_session,
        "private_car_emissions",
        [
            {
                "id": 1,
                "year": 2017,
                "emission_band": "BAND_A",  # Enum value as string
                "licensing_authority": "Dublin",
                "count": 34166,
            },
            {
                "id": 2,
                "year": 2017,
                "emission_band": "BAND_B",
                "licensing_authority": "Dublin",
                "count": 9680,
            },
            {
                "id": 3,
                "year": 2018,
                "emission_band": "BAND_C",
                "licensing_authority": "Cork",
                "count": 15000,
            },
        ],
    )

    # ASSERT: Verify EV charging points (Dublin only, Cork filtered)
    assert_rows(
        db_session,
        "ev_charging_points",
        [
            {
                "id": 1,
                "address": "Chargepoint Dublin 1",
                "county": "Dublin",
                "charger_count": 2,
                "lat": 53.3498,
                "lon": -6.2603,
                "power_rating_of_ccs_connectors_kw": 50.0,
                "power_rating_of_chademo_connectors_kw": 50.0,
                "power_rating_of_ac_fast_kw": 22.0,
                "power_rating_of_standard_ac_socket_kw": 7.0,
                "is_24_7": True,
            },
            {
                "id": 2,
                "address": "Chargepoint Dublin 2",
                "county": "Dublin",
                "charger_count": 1,
                "lat": 53.3441,
                "lon": -6.2675,
                "power_rating_of_ccs_connectors_kw": 150.0,
                "power_rating_of_chademo_connectors_kw": 100.0,
                "power_rating_of_ac_fast_kw": 43.0,
                "power_rating_of_standard_ac_socket_kw": 7.4,
                "is_24_7": False,
            },
        ],
    )

    # ASSERT: Verify electoral divisions (Dublin City + Fingal, Cork filtered)
    assert_rows(
        db_session,
        "ev_electoral_divisions",
        [
            {
                "id": 1,
                "ed_english": "Arran Quay A",
                "county_english": "DUBLIN CITY",
                "geom": ANY,
            },
            {
                "id": 2,
                "ed_english": "Swords Rural",
                "county_english": "FINGAL",
                "geom": ANY,
            },
        ],
    )

    # ASSERT: Verify EV charging demand data (including null registered_ev)
    assert_rows(
        db_session,
        "ev_charging_demand",
        [
            {
                "id": 1,
                "electoral_division": "Arran Quay A, Dublin City",
                "bed_sit_count": 1,
                "flat_apartment_count": 220,
                "house_bungalow_count": 476,
                "total_dwellings": 697,
                "registered_ev": 217.0,
                "bed_sit_pct": 0.1434720229555237,
                "flat_apartment_pct": 31.563845050215207,
                "house_bungalow_pct": 68.29268292682927,
                "home_charge_pct": 0.6048780487804878,
                "charge_frequency": 0.06717073170731708,
                "charging_demand": 4.0,
            },
            {
                "id": 2,
                "electoral_division": "Arran Quay B, Dublin City",
                "bed_sit_count": 5,
                "flat_apartment_count": 1280,
                "house_bungalow_count": 420,
                "total_dwellings": 1705,
                "registered_ev": 481.0,
                "bed_sit_pct": 0.2932551319648094,
                "flat_apartment_pct": 75.0733137829912,
                "house_bungalow_pct": 24.633431085043988,
                "home_charge_pct": 0.47390029325513194,
                "charge_frequency": 0.08943695014662757,
                "charging_demand": 11.0,
            },
            {
                "id": 3,
                "electoral_division": "Arran Quay C, Dublin City",
                "bed_sit_count": 1,
                "flat_apartment_count": 1746,
                "house_bungalow_count": 153,
                "total_dwellings": 1900,
                "registered_ev": None,
                "bed_sit_pct": 0.05263157894736842,
                "flat_apartment_pct": 91.89473684210526,
                "house_bungalow_pct": 8.052631578947368,
                "home_charge_pct": 0.42415789473684207,
                "charge_frequency": 0.09789315789473686,
                "charging_demand": 13.0,
            },
        ],
    )

    # ASSERT: Verify vehicle first time data
    assert_rows(
        db_session,
        "vehicle_first_time",
        [
            {
                "id": 1,
                "month": datetime(1996, 6, 1),
                "taxation_class": "ALL_VEHICLES",
                "count": 5000,
            },
            {
                "id": 2,
                "month": datetime(1996, 6, 1),
                "taxation_class": "NEW_PRIVATE_CARS",
                "count": 3000,
            },
            {
                "id": 3,
                "month": datetime(1996, 7, 1),
                "taxation_class": "SECONDHAND_PRIVATE_CARS",
                "count": 2000,
            },
        ],
    )
