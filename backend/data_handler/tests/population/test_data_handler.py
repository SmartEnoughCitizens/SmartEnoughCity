from sqlalchemy.orm import Session

from data_handler.population.data_handler import process_population_static_data
from data_handler.settings.data_sources_settings import get_data_sources_settings
from tests.utils import ANY, assert_row_count, assert_rows


def test_process_bus_static_data(db_session: Session) -> None:
    assert_row_count(db_session, "small_areas", 0)

    sources_settings = get_data_sources_settings()
    process_population_static_data(sources_settings.population_static_data_dir)

    assert_row_count(db_session, "small_areas", 3)
    assert_rows(db_session, "small_areas", [
        {
            "sa_code": "268098009",
            "county_name": "DUBLIN CITY",
            "population": 254,
            "geom": ANY
        },
        {
            "sa_code": "268083012",
            "county_name": "DUBLIN CITY",
            "population": 247,
            "geom": ANY
        },
        {
            "sa_code": "268144014",
            "county_name": "DUBLIN CITY",
            "population": 171,
            "geom": ANY
        }
    ])
