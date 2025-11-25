import requests
import pandas as pd
import math
from sqlalchemy import text
from sqlalchemy.exc import ProgrammingError, DBAPIError
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings
from data_handler.settings.api_settings import get_api_settings


GTFS_URL = "https://api.nationaltransport.ie/gtfsr/v2/TripUpdates?format=json"

# Load API key from environment or .env.development via pydantic settings
_api_settings = get_api_settings()
API_KEY = _api_settings.gtfs_api_key



def fetch_gtfs_trip_updates():
    """Fetch GTFS TripUpdates und return DataFrame.
    stop_sequence → STRING (für DB Primary Key)
    delays → INT oder None
    """
    headers = {"x-api-key": API_KEY}
    resp = requests.get(GTFS_URL, headers=headers)
    resp.raise_for_status()
    data = resp.json()

    rows = []

    for entity in data.get("entity", []):
        tu = entity.get("trip_update", {})
        trip = tu.get("trip", {})
        stops = tu.get("stop_time_update", [])

        # falls keine Stops vorhanden
        if not stops:
            rows.append({
                "entity_id": entity.get("id"),
                "trip_id": trip.get("trip_id"),
                "route_id": trip.get("route_id"),
                "start_time": trip.get("start_time"),
                "start_date": trip.get("start_date"),
                "stop_sequence": None,
                "stop_id": None,
                "arrival_delay": None,
                "departure_delay": None,
                "schedule_relationship": None
            })
            continue

        for st in stops:
            rows.append({
                "entity_id": entity.get("id"),
                "trip_id": trip.get("trip_id"),
                "route_id": trip.get("route_id"),
                "start_time": trip.get("start_time"),
                "start_date": trip.get("start_date"),
                # WICHTIG: stop_sequence ist IMMER ein string
                "stop_sequence": (
                    str(st.get("stop_sequence"))
                    if st.get("stop_sequence") is not None
                    else None
                ),
                "stop_id": st.get("stop_id"),
                "arrival_delay": st.get("arrival", {}).get("delay"),
                "departure_delay": st.get("departure", {}).get("delay"),
                "schedule_relationship": st.get("schedule_relationship")
            })

    return pd.DataFrame(rows)


def trip_updates_to_terminal():
    """Printet TripUpdates ins Terminal."""
    df = fetch_gtfs_trip_updates()

    print("\n=== Trip Updates geladen ===")
    print("Rows:", len(df))

    print("\n=== Erste 10 Einträge ===")
    print(df.head(10))

    print("\n=== arrival_delay max ===")
    print(df["arrival_delay"].max())

    print("\n=== departure_delay max ===")
    print(df["departure_delay"].max())

    print("\n=== stop_sequence types ===")
    print(df["stop_sequence"].map(type).value_counts())

    # Wenn du ALLES sehen willst:
    # print(df.to_string())

    return df


def trip_updates_to_db():
    """Schreibt TripUpdates sicher in die PostgreSQL-Datenbank."""
    df = fetch_gtfs_trip_updates()

    if df.empty:
        print("No GTFS data to save.")
        return

    # Konvertiere DataFrame zu dicts und bereinige NaN-Werte in delay-Feldern
    records = []
    for row in df.to_dict(orient="records"):
        # NaN in delay-Feldern zu None konvertieren (wird NULL in DB)
        if pd.isna(row.get("arrival_delay")):
            row["arrival_delay"] = None
        if pd.isna(row.get("departure_delay")):
            row["departure_delay"] = None
        records.append(row)

    s = get_db_settings()
    schema = s.postgres_schema

    insert_sql = f"""
    INSERT INTO {schema}.bus_trip_updates
        (entity_id, trip_id, route_id, start_time, start_date,
         stop_sequence, stop_id, arrival_delay, departure_delay,
         schedule_relationship)
    VALUES
        (:entity_id, :trip_id, :route_id, :start_time, :start_date,
         :stop_sequence, :stop_id, :arrival_delay, :departure_delay,
         :schedule_relationship)
    ON CONFLICT (entity_id, stop_sequence, stop_id)
    DO UPDATE SET
        trip_id = EXCLUDED.trip_id,
        route_id = EXCLUDED.route_id,
        start_time = EXCLUDED.start_time,
        start_date = EXCLUDED.start_date,
        arrival_delay = EXCLUDED.arrival_delay,
        departure_delay = EXCLUDED.departure_delay,
        schedule_relationship = EXCLUDED.schedule_relationship;
    """

    try:
        with SessionLocal() as db:
            db.execute(text(insert_sql), records)
            db.commit()
        print(f"Inserted/updated {len(records)} GTFS rows into {schema}.bus_trip_updates")

    except ProgrammingError as e:
        msg = str(e).lower()
        if 'permission denied' in msg:
            print("Permission error: DB user lacks privileges.")
        elif 'does not exist' in msg:
            print(f"Table {schema}.bus_trip_updates does not exist.")
        else:
            print("Database programming error:", e)

    except DBAPIError as e:
        print("Database error:", e)