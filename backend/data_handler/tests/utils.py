from deepdiff import DeepDiff
from sqlalchemy import text
from sqlalchemy.orm import Session

from data_handler.settings.database_settings import get_db_settings


class AnyValue:
    """A sentinel class that equals everything."""

    def __eq__(self, other: object) -> bool:
        return True

    def __hash__(self) -> int:
        return hash(AnyValue)

    def __repr__(self) -> str:
        return "<ANY>"


ANY = AnyValue()


def assert_row_count(session: Session, table_name: str, expected_count: int) -> None:
    """
    Assert that the number of rows in the specified table matches the expected count.

    Args:
        session: The SQLAlchemy session to use for querying the database.
        table_name (str): The name of the table to count rows in.
        expected_count (int): The expected number of rows in the table.

    Raises:
        AssertionError: If the actual row count does not match the expected count.
    """

    schema = get_db_settings().postgres_schema
    result = session.execute(text(f"SELECT COUNT(*) FROM {schema}.{table_name}"))
    row = result.fetchone()
    assert row is not None
    assert row[0] == expected_count


def assert_rows(session: Session, table_name: str, expected_rows: list[dict]) -> None:
    """
    Assert that the rows in the specified table match the expected rows.

    Args:
        session: The SQLAlchemy session to use for querying the database.
        table_name (str): The name of the table to fetch rows from.
        expected_rows (list[dict]): The expected rows, as a list of dictionaries.

    Raises:
        AssertionError: If the actual number of rows or any row's values do not match the expected rows.
    """

    schema = get_db_settings().postgres_schema
    result = session.execute(text(f"SELECT * FROM {schema}.{table_name}"))
    actual_rows = [dict(row) for row in result.mappings()]

    assert len(actual_rows) == len(expected_rows)

    diff = DeepDiff(
        expected_rows,
        actual_rows,
        math_epsilon=0.001,
        exclude_obj_callback=lambda obj, path: isinstance(obj, AnyValue),
    )
    assert not diff, f"Differences found: {diff.pretty()}"
