import pandas as pd
from sqlalchemy.orm import Session

from data_handler.public_spaces.data_handler import save_public_spaces_to_database
from data_handler.public_spaces.public_space_osmium_handler import PublicSpaceType
from tests.utils import ANY, assert_row_count, assert_rows


def test_save_public_spaces_to_database(db_session: Session) -> None:
    assert_row_count(db_session, "public_spaces", 0)

    public_spaces = pd.DataFrame(
        [
            {
                "name": "Test Cafe",
                "type": PublicSpaceType.AMENITY,
                "subtype": "cafe",
                "lat": 53.35,
                "lon": -6.26,
            },
            {
                "name": None,
                "type": PublicSpaceType.SHOP,
                "subtype": "convenience",
                "lat": 53.36,
                "lon": -6.25,
            },
        ]
    )

    save_public_spaces_to_database(public_spaces)

    assert_row_count(db_session, "public_spaces", 2)
    assert_rows(
        db_session,
        "public_spaces",
        [
            {
                "id": ANY,
                "name": "Test Cafe",
                "type": "AMENITY",
                "subtype": "cafe",
                "geom": ANY,
            },
            {
                "id": ANY,
                "name": None,
                "type": "SHOP",
                "subtype": "convenience",
                "geom": ANY,
            },
        ],
    )
