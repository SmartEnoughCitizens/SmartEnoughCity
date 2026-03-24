from pathlib import Path

from sqlalchemy.orm import Session

from data_handler.population.data_handler import process_population_static_data
from tests.utils import ANY, assert_row_count, assert_rows


def test_process_population_static_data(
    db_session: Session, tests_data_dir: Path
) -> None:
    assert_row_count(db_session, "small_areas", 0)

    process_population_static_data(tests_data_dir / "static_data" / "population")

    assert_row_count(db_session, "small_areas", 3)
    assert_rows(
        db_session,
        "small_areas",
        [
            {
                "sa_code": "268098009",
                "county_name": "DUBLIN CITY",
                "population": 254,
                "geom": ANY,
            },
            {
                "sa_code": "268083012",
                "county_name": "DUBLIN CITY",
                "population": 247,
                "geom": ANY,
            },
            {
                "sa_code": "268144014",
                "county_name": "DUBLIN CITY",
                "population": 171,
                "geom": ANY,
            },
        ],
    )
