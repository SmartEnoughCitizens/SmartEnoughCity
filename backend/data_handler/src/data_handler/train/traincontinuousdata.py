import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

import pandas as pd
import requests

BASE = "https://api.irishrail.ie/realtime/realtime.asmx"
TRAIN_TYPES = ["A", "M", "S", "D"]  # All, Mainline, Suburban, DART


def parse_xml(url: str) -> tuple[ET.Element, str]:
    response = requests.get(url, timeout=10)
    response.raise_for_status()
    root = ET.fromstring(response.content)
    ns = root.tag.split("}")[0].strip("{")
    return root, ns


def get_train_positions_history() -> pd.DataFrame:
    rows = []
    fetched_at = datetime.utcnow().isoformat()

    for t in TRAIN_TYPES:
        url = f"{BASE}/getCurrentTrainsXML_WithTrainType?TrainType={t}"
        root, ns = parse_xml(url)

        for tr in root.findall(f".//{{{ns}}}objTrainPositions"):
            rows.append({
                "train_code": tr.findtext(f"{{{ns}}}TrainCode"),
                "status": tr.findtext(f"{{{ns}}}TrainStatus"),
                "lat": float(tr.findtext(f"{{{ns}}}TrainLatitude") or 0),
                "lon": float(tr.findtext(f"{{{ns}}}TrainLongitude") or 0),
                "direction": tr.findtext(f"{{{ns}}}Direction"),
                "train_date": tr.findtext(f"{{{ns}}}TrainDate"),
                "public_message": tr.findtext(f"{{{ns}}}PublicMessage"),
                "train_type": t,                     # <-- WICHTIG
                "fetched_at": fetched_at,
            })

    return pd.DataFrame(rows)


def continuous_data_to_csv(output_path: str) -> None:

        try:
            df = get_train_positions_history()

            file_exists = Path(output_path).is_file()

            df.to_csv(
                output_path,
                mode="a",
                header=not file_exists,
                index=False,
            )

            print(f"[{datetime.now().strftime('%H:%M:%S')}] Saved {len(df)} rows.")

        except Exception as e:
            print(f"Error while scraping: {e}")



