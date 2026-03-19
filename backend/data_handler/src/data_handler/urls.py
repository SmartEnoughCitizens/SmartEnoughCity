import requests
import os
import re
from data_handler.settings.data_sources_settings import get_data_sources_settings

# Mapping of folder names to their Google Drive folder IDs


folder_ids = get_data_sources_settings()

def download_drive_folder(folder_name: str, base_dir: str) -> str:
    """
    Downloads a public Google Drive folder based on a predefined folder name.

    Args:
        folder_name: Logical name of the dataset (e.g. "car", "tram", "train", "population")
        base_dir: Base directory where the folder will be saved (default: "static_data")

    Returns:
        Path to the downloaded folder

    Raises:
        ValueError: If the folder_name is not found in FOLDER_IDS
    """
    if folder_name not in folder_ids:
        raise ValueError(f"Unknown folder '{folder_name}'. Available: {list(folder_ids.keys())}")

    folder_id = folder_ids[folder_name]
    download_dir = os.path.join(base_dir, folder_name)
    os.makedirs(download_dir, exist_ok=True)

    # Scrape folder contents from public Google Drive page
    url = f"https://drive.google.com/drive/folders/{folder_id}"
    response = requests.get(url)
    response.raise_for_status()

    # Extract and deduplicate file IDs from the HTML
    file_ids = list(set(re.findall(r'"([a-zA-Z0-9_-]{33})"', response.text)))
    print(f"[{folder_name}] {len(file_ids)} file(s) found.")

    for file_id in file_ids:
        download_url = f"https://drive.google.com/uc?export=download&id={file_id}"
        r = requests.get(download_url, allow_redirects=True)
        r.raise_for_status()

        # Read filename from response header, fall back to file ID
        content_disposition = r.headers.get("Content-Disposition", "")
        match = re.findall(r'filename="(.+)"', content_disposition)
        filename = match[0] if match else f"{file_id}.bin"

        filepath = os.path.join(download_dir, filename)
        with open(filepath, "wb") as f:
            f.write(r.content)
        print(f"  ✓ {filename}")

    print(f"[{folder_name}] Done! Saved to: {download_dir}")
    return download_dir