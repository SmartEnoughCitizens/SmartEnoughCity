import csv
import io
import json
import logging
import time
import zipfile
from datetime import date, datetime, timedelta

import requests
from pydantic import BaseModel, ConfigDict, Field, TypeAdapter
from sqlalchemy.dialects.postgresql import insert as pg_insert

from data_handler.csv_utils import validate_csv_headers
from data_handler.db import SessionLocal
from data_handler.pedestrians.models import (
    ChannelDirection,
    MobilityType,
    PedestrianChannel,
    PedestrianCounterMeasure,
    PedestrianCounterSite,
    PedestrianGranularity,
)
from data_handler.settings.api_settings import get_api_settings

logger = logging.getLogger(__name__)

GRANULARITY_VALUES = frozenset(e.value for e in PedestrianGranularity)


class SiteLocation(BaseModel):
    lat: float
    lon: float


class PedestrianSitePayload(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: int
    name: str
    description: str | None = None
    location: SiteLocation
    first_data: datetime = Field(alias="firstData")
    granularity: str = Field(alias="granularity")
    travel_modes: list[str] = Field(alias="travelModes")
    directional: bool = Field(alias="directional")
    has_timestamped_data: bool = Field(alias="hasTimestampedData")
    has_weather: bool = Field(alias="hasWeather")

    @property
    def pedestrian_sensor(self) -> bool:
        return "pedestrian" in self.travel_modes

    @property
    def bike_sensor(self) -> bool:
        return "bike" in self.travel_modes


def _validate_granularity(value: str) -> PedestrianGranularity:
    if value not in GRANULARITY_VALUES:
        msg = f"Invalid granularity: {value!r}. Expected one of: {sorted(GRANULARITY_VALUES)}."
        raise ValueError(msg)
    return PedestrianGranularity(value)


def _payload_to_site(payload: PedestrianSitePayload) -> PedestrianCounterSite:
    granularity = _validate_granularity(payload.granularity)
    first_data = payload.first_data
    if first_data.tzinfo is None:
        first_data = first_data.replace(tzinfo=datetime.now().astimezone().tzinfo)
    return PedestrianCounterSite(
        id=payload.id,
        name=payload.name,
        description=payload.description or None,
        lat=payload.location.lat,
        lon=payload.location.lon,
        first_data=first_data,
        granularity=granularity,
        pedestrian_sensor=payload.pedestrian_sensor,
        bike_sensor=payload.bike_sensor,
        directional=payload.directional,
        has_timestamped_data=payload.has_timestamped_data,
        has_weather=payload.has_weather,
    )


_sites_adapter = TypeAdapter(list[PedestrianSitePayload])


def process_pedestrian_sites(json_string: str) -> list[int]:
    """
    Parse, validate, and persist pedestrian counter sites from JSON.

    Expects a JSON array of site objects matching the API shape (camelCase).
    If a site id already exists in the database, the row is updated with the
    new data; otherwise a new row is inserted.

    Args:
        json_string: Raw JSON string (array of site objects).

    Returns:
        A list of site ids that were updated or inserted.

    Raises:
        ValueError: If JSON is invalid or a required field is invalid.
        ValidationError: If the structure does not match (Pydantic validation).
    """
    try:
        payloads = _sites_adapter.validate_json(json_string)
    except json.JSONDecodeError as e:
        msg = "Invalid JSON"
        raise ValueError(msg) from e

    sites = [_payload_to_site(p) for p in payloads]
    updated_ids: list[int] = []

    with SessionLocal() as session:
        try:
            for site in sites:
                session.merge(site)
                updated_ids.append(site.id)
            session.commit()
        except Exception:
            session.rollback()
            logger.exception("Failed to persist pedestrian counter sites data.")
            raise
        finally:
            session.close()

    logger.info("Persisted %d pedestrian counter site record(s).", len(updated_ids))
    return updated_ids


def send_batch_job_request(site_ids: list[int], date: date) -> int:
    """
    Sends a batch job request to the Eco Counter API to export mobility data for the given site IDs and date.

    Args:
        site_ids: A list of site IDs for which to request mobility counting data.
        date: A `date` object specifying the starting date for the export (inclusive).

    Returns:
        The job ID assigned by the Eco Counter API to track the export request.

    Raises:
        requests.HTTPError: If the API call fails or returns a non-2xx response.
    """
    api_settings = get_api_settings()
    headers = {"x-api-key": api_settings.eco_counter_api_key}
    batch_job_url = f"{api_settings.eco_counter_api_base_url}/exports"

    request_body = {
        "startDate": date.strftime("%Y-%m-%d"),
        "endDate": (date + timedelta(days=1)).strftime("%Y-%m-%d"),
        "schema": "mobility_counting_schema",
        "siteIds": site_ids,
        "granularity": "PT15M",
        "validatedDataOnly": False,
        "gapFilling": False,
        "validateSchema": False,
    }

    logger.info("Sending batch job request for date %s...", date)
    response = requests.post(batch_job_url, headers=headers, json=request_body)
    response.raise_for_status()
    job_id = response.json()["id"]
    logger.info("Batch job request sent successfully. Job ID: %s", job_id)
    return job_id


CHANNEL_CSV_HEADERS = [
    "channel_id",
    "site_id",
    "mobility_type",
    "provider_direction_code",
    "time_step",
]


def _parse_channel_row(row: dict[str, str]) -> PedestrianChannel:
    return PedestrianChannel(
        channel_id=int(row["channel_id"]),
        site_id=int(row["site_id"]),
        mobility_type=MobilityType(row["mobility_type"].strip()),
        direction=ChannelDirection(row["provider_direction_code"].strip()),
        time_step=int(row["time_step"]),
    )


def process_pedestrian_channel_data(csv_text: io.TextIOWrapper) -> None:
    """
    Parse and persist pedestrian channel data from a CSV stream.

    Expects a CSV with headers including channel_id, site_id, mobility_type,
    direction, and time_step. Rows are upserted by channel_id (existing rows
    are updated).

    Args:
        csv_text: UTF-8 decoded text stream of the CSV content.

    Raises:
        ValueError: If required headers are missing or row data is invalid.
    """
    reader = csv.DictReader(csv_text)
    if not validate_csv_headers(reader, CHANNEL_CSV_HEADERS):
        msg = (
            f"Channel CSV is missing required headers. Expected: {CHANNEL_CSV_HEADERS}, "
            f"Found: {reader.fieldnames}"
        )
        raise ValueError(msg)

    channels = [_parse_channel_row(row) for row in reader]

    with SessionLocal() as session:
        try:
            for channel in channels:
                session.merge(channel)
            session.commit()
            logger.info(
                "Persisted %d pedestrian channel record(s).",
                len(channels),
            )
        except Exception:
            session.rollback()
            logger.exception("Failed to persist pedestrian channel data.")
            raise
        finally:
            session.close()


MEASURES_CSV_HEADERS = [
    "channel_id",
    "counter_id",
    "start_datetime",
    "end_datetime",
    "count",
]


def _parse_measure_datetime(value: str) -> datetime:
    s = value.strip()
    if s.upper().endswith("Z"):
        s = s[:-1] + "+00:00"
    return datetime.fromisoformat(s)


def _parse_measure_row(row: dict[str, str]) -> dict:
    return {
        "channel_id": int(row["channel_id"]),
        "counter_id": row["counter_id"].strip(),
        "start_datetime": _parse_measure_datetime(row["start_datetime"]),
        "end_datetime": _parse_measure_datetime(row["end_datetime"]),
        "count": int(row["count"]),
    }


def process_pedestrian_measures_data(csv_text: io.TextIOWrapper) -> None:
    """
    Parse and persist pedestrian counter measures from a CSV stream.

    Expects a CSV with headers channel_id, counter_id, start_datetime,
    end_datetime, and count. Rows are upserted by (channel_id, start_datetime,
    end_datetime): existing rows are updated (counter_id, count), new rows are
    inserted.

    Args:
        csv_text: UTF-8 decoded text stream of the CSV content.

    Raises:
        ValueError: If required headers are missing or row data is invalid.
    """
    reader = csv.DictReader(csv_text)
    if not validate_csv_headers(reader, MEASURES_CSV_HEADERS):
        msg = (
            f"Measures CSV is missing required headers. Expected: {MEASURES_CSV_HEADERS}, "
            f"Found: {reader.fieldnames}"
        )
        raise ValueError(msg)

    rows = [_parse_measure_row(row) for row in reader]

    with SessionLocal() as session:
        try:
            for r in rows:
                stmt = pg_insert(PedestrianCounterMeasure).values(
                    channel_id=r["channel_id"],
                    counter_id=r["counter_id"],
                    start_datetime=r["start_datetime"],
                    end_datetime=r["end_datetime"],
                    count=r["count"],
                )
                stmt = stmt.on_conflict_do_update(
                    index_elements=["channel_id", "start_datetime", "end_datetime"],
                    set_={
                        "counter_id": stmt.excluded.counter_id,
                        "count": stmt.excluded.count,
                    },
                )
                session.execute(stmt)
            session.commit()
            logger.info(
                "Persisted %d pedestrian counter measure record(s).",
                len(rows),
            )
        except Exception:
            session.rollback()
            logger.exception("Failed to persist pedestrian counter measures data.")
            raise
        finally:
            session.close()


def process_batch_job_result(job_result_content: bytes) -> None:
    """
    Processes the result of a batch job from the Eco Counter API.

    This function expects the content of a ZIP file as bytes, which should contain
    at least two CSV files: "channels.csv" and "measures.csv". The function extracts
    these files, parses the data, and persists the pedestrian channel and measure
    records to the database.

    Args:
        job_result_content: The bytes content of the ZIP file returned by a completed
            batch job export from the Eco Counter API.

    Raises:
        zipfile.BadZipFile: If the provided content is not a valid ZIP file.
        KeyError: If expected files ("channels.csv" or "measures.csv") are missing in the ZIP.
        ValueError: If the CSV content is invalid or required headers are missing.
        Exception: Any error that occurs during the parsing or database persisting steps.
    """
    logger.info("Processing batch job result...")
    zip_buffer = io.BytesIO(job_result_content)
    with zipfile.ZipFile(zip_buffer) as result_zip_file:
        with result_zip_file.open("channels.csv") as csv_bytes:
            csv_text = io.TextIOWrapper(csv_bytes, encoding="utf-8")
            logger.info("Processing channels data...")
            process_pedestrian_channel_data(csv_text)
        with result_zip_file.open("measures.csv") as csv_bytes:
            csv_text = io.TextIOWrapper(csv_bytes, encoding="utf-8")
            logger.info("Processing measures data...")
            process_pedestrian_measures_data(csv_text)
    logger.info("Batch job result processed successfully.")


def process_pedestrian_live_data() -> None:
    """
    Fetches the latest pedestrian counter sites and measures data from the Eco Counter API
    and persists the data to the database.

    This function performs the following steps:
      1. Fetches the list of active pedestrian counter sites from the Eco Counter API.
      2. Parses and upserts site information into the database.
      3. Requests the creation of a batch export job for the latest pedestrian measures.
      4. Polls for the batch job completion and downloads the resulting data (a ZIP containing CSV files).
      5. Processes and persists the contained pedestrian channel and measure data.

    Retries the batch job result download up to five times in case of transient errors.

    Raises:
        requests.RequestException: If a network or server error occurs at any stage.
        zipfile.BadZipFile: If the downloaded batch data is not a valid ZIP.
        KeyError: If expected files are missing in the ZIP.
        ValueError: If the CSV or site data are invalid.
        Exception: If an error occurs processing data or with database operations.
    """
    api_settings = get_api_settings()
    headers = {"x-api-key": api_settings.eco_counter_api_key}
    sites_url = f"{api_settings.eco_counter_api_base_url}/sites"

    logger.info("Fetching pedestrian counter sites data...")
    sites_response = requests.get(sites_url, headers=headers)
    sites_response.raise_for_status()
    updated_site_ids = process_pedestrian_sites(sites_response.text)

    job_id = send_batch_job_request(updated_site_ids, date.today())
    job_result_url = f"{api_settings.eco_counter_api_base_url}/exports/{job_id}/data"

    max_attempts = 5
    for attempt in range(max_attempts):
        try:
            logger.info(
                "Fetching batch job result for job ID %s (attempt %d/%d)...",
                job_id,
                attempt + 1,
                max_attempts,
            )
            job_result_response = requests.get(job_result_url, headers=headers)
            job_result_response.raise_for_status()
            process_batch_job_result(job_result_response.content)
            break
        except requests.RequestException as e:
            if attempt < max_attempts - 1:
                delay = 2**attempt
                logger.warning(
                    "Batch job result fetch failed (attempt %d/%d): %s. Retrying in %ds...",
                    attempt + 1,
                    max_attempts,
                    e,
                    delay,
                )
                time.sleep(delay)
            else:
                logger.exception(
                    "Batch job result fetch failed after %d attempts.",
                    max_attempts,
                )
                raise
