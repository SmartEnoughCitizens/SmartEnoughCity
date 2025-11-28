import requests
import pandas as pd
from sqlalchemy import text
from sqlalchemy.exc import ProgrammingError, DBAPIError
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings


URL = "https://data.smartdublin.ie/dublinbikes-api/bikes/dublin_bikes/current/stations.geojson"


def fetch_cycle_data():
    """Fetch GeoJSON der Dublin Bikes API und gib DataFrame zurück."""
    resp = requests.get(URL)
    resp.raise_for_status()

    data = resp.json()

    rows = []
    for feature in data.get("features", []):
        props = feature.get("properties", {})
        geom = feature.get("geometry", {})
        coords = geom.get("coordinates") or [None, None]

        rows.append({
            "station_id": props.get("station_id"),
            "name": props.get("name"),
            "address": props.get("address"),
            "capacity": props.get("capacity"),
            "num_bikes_available": props.get("num_bikes_available"),
            "num_docks_available": props.get("num_docks_available"),
            "is_installed": props.get("is_installed"),
            "is_renting": props.get("is_renting"),
            "is_returning": props.get("is_returning"),
            "last_reported": props.get("last_reported"),
            "last_reported_dt": props.get("last_reported_dt"),
            "lon": coords[0],
            "lat": coords[1],
        })

    df = pd.DataFrame(rows)
    return df


def cycle_stations_to_csv(filepath):
    df = fetch_cycle_data()
    if df.empty:
        print("No cycle data to save.")
        return

    df.to_csv(filepath, index=False)
    print(f"Saved {len(df)} cycle rows to {filepath}")


def cycle_stations_to_db():
    """Schreibe aktuelle Dublin Bikes Daten in DB (batch)."""
    df = fetch_cycle_data()

    if df.empty:
        print("No cycle data to save.")
        return

    # Create dict records for DB insert
    records = []
    for _, row in df.iterrows():
        records.append({
            "station_id": row["station_id"],
            "name": row["name"],
            "address": row["address"],
            "capacity": row["capacity"],
            "num_bikes_available": row["num_bikes_available"],
            "num_docks_available": row["num_docks_available"],
            "is_installed": row["is_installed"],
            "is_renting": row["is_renting"],
            "is_returning": row["is_returning"],
            "last_reported": row["last_reported"],
            "last_reported_dt": row["last_reported_dt"],
            "lat": row["lat"],
            "lon": row["lon"],
        })

    s = get_db_settings()
    schema = s.postgres_schema

    insert_sql = f"""
    INSERT INTO {schema}.cycle_stations
        (station_id, name, address, capacity,
         num_bikes_available, num_docks_available,
         is_installed, is_renting, is_returning,
         last_reported, last_reported_dt,
         lat, lon)
    VALUES
        (:station_id, :name, :address, :capacity,
         :num_bikes_available, :num_docks_available,
         :is_installed, :is_renting, :is_returning,
         :last_reported, :last_reported_dt,
         :lat, :lon)
    ON CONFLICT (station_id) DO UPDATE SET
        name = EXCLUDED.name,
        address = EXCLUDED.address,
        capacity = EXCLUDED.capacity,
        num_bikes_available = EXCLUDED.num_bikes_available,
        num_docks_available = EXCLUDED.num_docks_available,
        is_installed = EXCLUDED.is_installed,
        is_renting = EXCLUDED.is_renting,
        is_returning = EXCLUDED.is_returning,
        last_reported = EXCLUDED.last_reported,
        last_reported_dt = EXCLUDED.last_reported_dt,
        lat = EXCLUDED.lat,
        lon = EXCLUDED.lon;
    """

    try:
        with SessionLocal() as db:
            db.execute(text(insert_sql), records)
            db.commit()
        print(f"Inserted/updated {len(records)} cycle rows into {schema}.cycle_stations")
    except ProgrammingError as e:
        msg = str(e).lower()
        if 'permission denied' in msg:
            print("Permission error: DB user lacks privileges.")
        elif 'does not exist' in msg:
            print(f"Table {schema}.cycle_stations does not exist. Run postgres-init first.")
        else:
            print("Database programming error:", e)
    except DBAPIError as e:
        print("Database error:", e)
