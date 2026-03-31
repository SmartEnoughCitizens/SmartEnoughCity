import csv
import io
import json
import logging
import time
import zipfile
import zoneinfo
from collections import defaultdict
from datetime import date, datetime, timedelta

import requests
from pydantic import BaseModel, ConfigDict, Field, TypeAdapter, field_validator
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

_DUBLIN_TZ = zoneinfo.ZoneInfo("Europe/Dublin")


class SiteLocation(BaseModel):
    lat: float | None = None
    lon: float | None = None


class PedestrianSitePayload(BaseModel):
    model_config = ConfigDict(extra="ignore", populate_by_name=True)

    id: int
    name: str
    description: str | None = None
    location: SiteLocation | None = None
    first_data: datetime = Field(alias="firstData")
    granularity: str = Field(alias="granularity")
    travel_modes: list[str] = Field(alias="travelModes")
    directional: bool = Field(alias="directional")
    has_timestamped_data: bool = Field(alias="hasTimestampedData")
    has_weather: bool = Field(alias="hasWeather")

    @field_validator("location", mode="before")
    @classmethod
    def _empty_location_to_none(cls, v: object) -> object:
        if isinstance(v, dict) and not v:
            return None
        return v

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
        first_data = first_data.replace(tzinfo=_DUBLIN_TZ)
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


def process_pedestrian_sites(json_string: str) -> dict[int, PedestrianGranularity]:
    """
    Parse, validate, and persist pedestrian counter sites from JSON.

    Expects a JSON array of site objects matching the API shape (camelCase).
    If a site id already exists in the database, the row is updated with the
    new data; otherwise a new row is inserted. Sites without location data
    are skipped.

    Args:
        json_string: Raw JSON string (array of site objects).

    Returns:
        A mapping of site id to granularity for all sites that were upserted.

    Raises:
        ValueError: If JSON is invalid or a required field is invalid.
        ValidationError: If the structure does not match (Pydantic validation).
    """
    try:
        payloads = _sites_adapter.validate_json(json_string)
    except json.JSONDecodeError as e:
        msg = "Invalid JSON"
        raise ValueError(msg) from e

    valid_payloads = [
        p
        for p in payloads
        if p.location is not None
        and p.location.lat is not None
        and p.location.lon is not None
    ]
    skipped = len(payloads) - len(valid_payloads)
    if skipped:
        logger.warning(
            "Skipped %d pedestrian counter site(s) — missing location data.",
            skipped,
        )
    sites = [_payload_to_site(p) for p in valid_payloads]

    with SessionLocal() as session:
        try:
            for site in sites:
                session.merge(site)
            session.commit()
        except Exception:
            session.rollback()
            logger.exception("Failed to persist pedestrian counter sites data.")
            raise
        finally:
            session.close()

    logger.info("Upserted %d pedestrian counter site record(s).", len(sites))
    return {site.id: site.granularity for site in sites}


def send_batch_job_request(
    site_ids: list[int], export_date: date, granularity: PedestrianGranularity
) -> int:
    """
    Sends a batch job request to the Eco Counter API to export mobility data
    for the given site IDs, date, and granularity.

    Args:
        site_ids: A list of site IDs for which to request mobility counting data.
        export_date: The starting date for the export (inclusive).
        granularity: The time granularity for the exported data, matching each
            site's native granularity.

    Returns:
        The job ID assigned by the Eco Counter API to track the export request.

    Raises:
        requests.HTTPError: If the API call fails or returns a non-2xx response.
    """
    api_settings = get_api_settings()
    headers = {"x-api-key": api_settings.eco_counter_api_key}
    batch_job_url = f"{api_settings.eco_counter_api_base_url}/exports"

    request_body = {
        "startDate": export_date.strftime("%Y-%m-%d"),
        "endDate": (export_date + timedelta(days=1)).strftime("%Y-%m-%d"),
        "schema": "mobility_counting_schema",
        "siteIds": site_ids,
        "granularity": granularity.value,
        "validatedDataOnly": False,
        "gapFilling": False,
        "validateSchema": False,
    }

    logger.info(
        "Sending batch job request for date %s with granularity %s (%d site(s))...",
        export_date,
        granularity.value,
        len(site_ids),
    )
    response = requests.post(
        batch_job_url, headers=headers, json=request_body, timeout=30
    )
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
                "Upserted %d pedestrian channel record(s).",
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
                "Upserted %d pedestrian counter measure record(s).",
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


def _poll_and_process_batch_result(  # noqa: PLR0913
    job_id: int,
    job_result_url: str,
    headers: dict[str, str],
    *,
    initial_wait: int = 15,
    poll_interval: int = 15,
    max_attempts: int = 20,
) -> None:
    """
    Polls for a completed batch job result and processes the ZIP response.

    The Eco Counter export API is asynchronous: the job result endpoint returns
    404 while the job is still processing. This function waits for an initial
    delay, then polls until the result is available or the attempt limit is
    reached.

    Args:
        job_id: The job ID to poll (used only for logging).
        job_result_url: The full URL to fetch the job result from.
        headers: Request headers (must include API key).
        initial_wait: Seconds to wait before the first poll attempt.
        poll_interval: Seconds to wait between poll attempts.
        max_attempts: Maximum number of poll attempts (default 20 x 15s = 5 min).

    Raises:
        requests.HTTPError: If the server returns a non-404 HTTP error.
        requests.RequestException: If all retry attempts fail.
    """
    logger.info(
        "Waiting %ds for batch job %s to be ready...",
        initial_wait,
        job_id,
    )
    time.sleep(initial_wait)

    for attempt in range(max_attempts):
        logger.info(
            "Fetching batch job result for job ID %s (attempt %d/%d)...",
            job_id,
            attempt + 1,
            max_attempts,
        )
        try:
            job_result_response = requests.get(
                job_result_url, headers=headers, timeout=30
            )
            job_result_response.raise_for_status()
        except requests.HTTPError as e:
            if e.response is not None and e.response.status_code == 404:
                if attempt < max_attempts - 1:
                    logger.info(
                        "Batch job %s not ready yet (attempt %d/%d). Retrying in %ds...",
                        job_id,
                        attempt + 1,
                        max_attempts,
                        poll_interval,
                    )
                    time.sleep(poll_interval)
                    continue
                logger.exception(
                    "Batch job %s not ready after %d attempts (total wait ~%ds).",
                    job_id,
                    max_attempts,
                    initial_wait + poll_interval * (max_attempts - 1),
                )
                raise
            raise
        except requests.RequestException:
            logger.exception(
                "Batch job result fetch failed (attempt %d/%d).",
                attempt + 1,
                max_attempts,
            )
            raise
        else:
            process_batch_job_result(job_result_response.content)
            return


def process_pedestrian_live_data() -> None:
    """
    Fetches the latest pedestrian counter sites and measures data from the Eco Counter API
    and persists the data to the database.

    This function performs the following steps:
      1. Fetches the list of active pedestrian counter sites from the Eco Counter API.
      2. Parses and upserts site information into the database.
      3. Groups sites by their native granularity.
      4. For each granularity group, requests a batch export job, polls for completion,
         and processes the returned ZIP (channels + measures CSV files).

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

    logger.info("Fetching pedestrian counter sites from Eco Counter API...")
    sites_response = requests.get(sites_url, headers=headers, timeout=30)
    sites_response.raise_for_status()
    site_granularity_map = process_pedestrian_sites(sites_response.text)

    by_granularity: dict[PedestrianGranularity, list[int]] = defaultdict(list)
    for site_id, granularity in site_granularity_map.items():
        by_granularity[granularity].append(site_id)

    for granularity, site_ids in by_granularity.items():
        job_id = send_batch_job_request(
            site_ids, date.today() - timedelta(days=1), granularity
        )
        job_result_url = (
            f"{api_settings.eco_counter_api_base_url}/exports/{job_id}/data"
        )
        _poll_and_process_batch_result(job_id, job_result_url, headers)
    logger.info("Pedestrian live data import complete.")
