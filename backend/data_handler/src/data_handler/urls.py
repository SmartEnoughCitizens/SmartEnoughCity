import logging
import shutil
import tempfile
import zipfile
from pathlib import Path

import gdown
import requests

logger = logging.getLogger(__name__)

_DOWNLOAD_TIMEOUT = 300  # seconds


def download_file(url: str, dest_path: str) -> None:
    """
    Downloads a file from a URL and saves it to dest_path.

    Args:
        url: URL of the file to download
        dest_path: Full local path to save the file to (including filename)
    """
    logger.info("Downloading %s ...", url)
    response = requests.get(url, stream=True, timeout=_DOWNLOAD_TIMEOUT)
    response.raise_for_status()

    dest = Path(dest_path)
    dest.parent.mkdir(parents=True, exist_ok=True)
    with dest.open("wb") as f:
        f.writelines(response.iter_content(chunk_size=8192))
    logger.info("  + %s", dest.name)


def download_and_extract_zip(url: str, extract_dir: str) -> str:
    """
    Downloads a ZIP file from a URL and extracts its contents into a local directory.

    Args:
        url: URL of the ZIP file to download
        extract_dir: Directory to extract the ZIP contents into

    Returns:
        Path to the extract directory
    """
    Path(extract_dir).mkdir(parents=True, exist_ok=True)

    logger.info("Downloading %s ...", url)
    response = requests.get(url, stream=True, timeout=_DOWNLOAD_TIMEOUT)
    response.raise_for_status()

    with tempfile.NamedTemporaryFile(suffix=".zip", delete=False) as tmp:
        tmp.writelines(response.iter_content(chunk_size=8192))
        tmp_path = tmp.name

    tmp_file = Path(tmp_path)
    try:
        with zipfile.ZipFile(tmp_path) as zf:
            zf.extractall(extract_dir)
            logger.info("Extracted %d file(s) to %s", len(zf.namelist()), extract_dir)
    finally:
        tmp_file.unlink()

    return extract_dir


def download_google_drive_folder(folder_id: str, download_dir: str) -> str:
    """
    Downloads all files from a Google Drive folder into a local directory.

    Args:
        folder_id: Google Drive folder ID
        download_dir: Local directory to download files into

    Returns:
        Path to the download directory
    """
    Path(download_dir).mkdir(parents=True, exist_ok=True)
    url = f"https://drive.google.com/drive/folders/{folder_id}"
    logger.info("Downloading Google Drive folder %s to %s ...", folder_id, download_dir)
    gdown.download_folder(url, output=download_dir, quiet=False, use_cookies=False)
    return download_dir


def delete_static_data(download_dir: str) -> None:
    """Deletes a downloaded static data directory after processing."""
    if Path(download_dir).exists():
        shutil.rmtree(download_dir)
        logger.info("Deleted: %s", download_dir)
