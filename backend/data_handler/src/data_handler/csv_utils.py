import csv
from collections.abc import Iterator
from pathlib import Path


def validate_csv_headers(
    csv_reader: csv.DictReader, required_headers: list[str]
) -> bool:
    """
    Validate that the CSV reader contains all required headers.

    Args:
        csv_reader (csv.DictReader): The CSV reader object.
        required_headers (list[str]): List of required header names.

    Returns:
        bool: True if all required headers are present, False otherwise.
    """
    headers = csv_reader.fieldnames
    if headers is None:
        return False
    return all(header in headers for header in required_headers)


def read_csv_file(
    file_path: Path, required_headers: list[str] | None = None
) -> Iterator[dict[str, str]]:
    """
    Reads a CSV file and yields each row as a dictionary.

    Args:
        file_path (Path): The path to the CSV file to read.
        required_headers (list[str] | None, optional):
            List of required header names. If provided, the function
            will validate that all required headers are present in the file.

    Yields:
        dict[str, str]: Each row of the CSV as a dictionary with column names as keys.

    Raises:
        ValueError: If required_headers are specified and the file does not contain all required headers.
    """
    with file_path.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        if required_headers is not None and not validate_csv_headers(
            reader, required_headers
        ):
            msg = f"File {file_path} is missing required headers. Expected: {required_headers}, Found: {reader.fieldnames}"
            raise ValueError(msg)
        yield from reader
