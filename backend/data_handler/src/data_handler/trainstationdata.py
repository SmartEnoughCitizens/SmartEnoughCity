import requests
import xml.etree.ElementTree as ET
import pandas as pd
from sqlalchemy import text
from sqlalchemy.exc import ProgrammingError, DBAPIError
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings

BASE = "https://api.irishrail.ie/realtime/realtime.asmx"

def parse_xml(url):
    response = requests.get(url)
    response.raise_for_status()
    root = ET.fromstring(response.content)
    ns = root.tag.split('}')[0].strip('{')
    return root, ns


def get_all_stations_with_type():
    all_stations = []

    for t in ['A', 'M', 'S', 'D']:  # All, Mainline, Suburban, DART
        root, ns = parse_xml(f"{BASE}/getAllStationsXML_WithStationType?StationType={t}")
        for st in root.findall(f".//{{{ns}}}objStation"):
            all_stations.append({
                "desc": st.findtext(f"{{{ns}}}StationDesc"),
                "code": st.findtext(f"{{{ns}}}StationCode"),
                "lat": float(st.findtext(f"{{{ns}}}StationLatitude") or 0),
                "lon": float(st.findtext(f"{{{ns}}}StationLongitude") or 0),
                "type": t
            })

    df = pd.DataFrame(all_stations)

    df = (df.groupby(["code", "desc", "lat", "lon"], as_index=False)
            .agg({"type": lambda x: ";".join(sorted(set(x)))})
         )
    return df

# NOTE: Schema/table creation is handled by orchestration (postgres-init).
# data_handler should only perform inserts/queries at runtime.


def train_stations_to_csv(filepath):
    stations = get_all_stations_with_type()
    print(f"\nTotal stations: {len(stations)}")
    stations.to_csv(filepath, index=False)

def train_stations_to_db():
    """Hole Stations und schreibe in DB (batch)."""
    # 1) Hole DataFrame
    df = get_all_stations_with_type()

    if df.empty:
        print("No stations to save.")
        return

    # 2) Assumes schema/table already created by orchestration (postgres-init)

    # 3) Konvertiere in dict-Records passend zu Spalten
    # Spalten: station_code, station_desc, lat, lon, station_types
    records = []
    for _, row in df.iterrows():
        records.append({
            "station_code": row["code"],
            "station_desc": row["desc"],
            "lat": float(row["lat"]) if not pd.isna(row["lat"]) else None,
            "lon": float(row["lon"]) if not pd.isna(row["lon"]) else None,
            "station_types": row["type"],
        })

    # 4) Batch-Insert (executemany)
    s = get_db_settings()
    schema = s.schema
    insert_sql = f"""
    INSERT INTO {schema}.train_stations
        (station_code, station_desc, lat, lon, station_types)
    VALUES
        (:station_code, :station_desc, :lat, :lon, :station_types)
    ON CONFLICT DO NOTHING;  -- optional, falls du unique constraints hinzufügst
    """

    try:
        with SessionLocal() as db:
            db.execute(text(insert_sql), records)  # SQLAlchemy führt das als batch aus
            db.commit()
        print(f"Inserted {len(records)} station rows into {schema}.train_stations")
    except ProgrammingError as e:
        # Common causes: missing schema/table or insufficient privileges
        msg = str(e).lower()
        if 'permission denied' in msg:
            print("Permission error: the DB user lacks privileges to write to the database. Ensure postgres-init granted necessary rights.")
        elif 'does not exist' in msg or 'undefined_table' in msg:
            print(f"Database table {schema}.train_stations does not exist. Ensure the postgres-init job created the schema and table.")
        else:
            print("Database programming error:", e)
    except DBAPIError as e:
        print("Database error:", e)

