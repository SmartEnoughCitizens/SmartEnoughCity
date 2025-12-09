import requests
import xmltodict
import pandas as pd

from sqlalchemy import text
from sqlalchemy.exc import ProgrammingError, DBAPIError
from data_handler.db import SessionLocal
from data_handler.settings.database_settings import get_db_settings


LUAS_LINES = {
    "red": "Luas Red Line",
    "green": "Luas Green Line",
}


# ---------------------------------------------------------
# Fetch LUAS stops
# ---------------------------------------------------------
def fetch_luas_stops(line_name):
    url = "https://luasforecasts.rpa.ie/xml/get.ashx?action=stops&encrypt=false"
    res = requests.get(url)
    res.raise_for_status()

    doc = xmltodict.parse(res.text)
    lines = doc.get("stops", {}).get("line", [])

    if isinstance(lines, dict):
        lines = [lines]

    target = LUAS_LINES[line_name]
    rows = []

    for line in lines:
        if line.get("@name") == target:
            stops = line.get("stop", [])
            if isinstance(stops, dict):
                stops = [stops]

            for stop in stops:
                rows.append({
                    "stop_id": stop["@abrev"],
                    "line": line_name,
                    "name": stop.get("#text", stop.get("@text", "")),
                    "pronunciation": stop.get("@pronunciation", ""),
                    "park_ride": stop.get("@isParkRide") == "1",
                    "cycle_ride": stop.get("@isCycleRide") == "1",
                    "lat": float(stop["@lat"]),
                    "lon": float(stop["@long"]),
                })

    return pd.DataFrame(rows)


def luas_stops_to_db():
    for line in ["red", "green"]:
        print(f"\n### Loading LUAS {line} line stops...")
        df = fetch_luas_stops(line)

        if df.empty:
            print(f"No LUAS stops found for {line}.")
            continue

        records = df.to_dict(orient="records")

        s = get_db_settings()
        schema = s.postgres_schema

        insert_sql = f"""
        INSERT INTO {schema}.luas_stops
            (stop_id, line, name, pronunciation,
             park_ride, cycle_ride, lat, lon)
        VALUES
            (:stop_id, :line, :name, :pronunciation,
             :park_ride, :cycle_ride, :lat, :lon)
        ON CONFLICT (stop_id)
        DO UPDATE SET
            line = EXCLUDED.line,
            name = EXCLUDED.name,
            pronunciation = EXCLUDED.pronunciation,
            park_ride = EXCLUDED.park_ride,
            cycle_ride = EXCLUDED.cycle_ride,
            lat = EXCLUDED.lat,
            lon = EXCLUDED.lon,
            updated_at = now();
        """

        try:
            with SessionLocal() as db:
                db.execute(text(insert_sql), records)
                db.commit()
            print(f"Inserted/updated {len(records)} LUAS stops ({line} line).")

        except Exception as e:
            print("Error inserting stops:", e)


# ---------------------------------------------------------
# Forecast handling
# ---------------------------------------------------------
def fetch_forecast_for_stop(stop_id):
    url = f"https://luasforecasts.rpa.ie/xml/get.ashx?action=forecast&stop={stop_id}&encrypt=false"
    res = requests.get(url)
    res.raise_for_status()

    try:
        doc = xmltodict.parse(res.text)
    except Exception:
        return []

    stop_info = doc.get("stopInfo", {})
    message = stop_info.get("message", "")
    directions = stop_info.get("direction", [])

    if isinstance(directions, dict):
        directions = [directions]

    rows = []

    for d in directions:
        direction_name = d["@name"]
        trams = d.get("tram", [])
        if isinstance(trams, dict):
            trams = [trams]

        for tram in trams:
            due = tram.get("@dueMins")
            rows.append({
                "direction": direction_name,
                "destination": tram.get("@destination", ""),
                "due_mins": int(due) if due and due.isdigit() else None,
                "message": message,
            })

    return rows


def luas_forecasts_to_db():
    print("\n### Fetching stops from DB...")

    s = get_db_settings()
    schema = s.postgres_schema

    with SessionLocal() as db:
        stops = db.execute(
            text(f"SELECT stop_id, line FROM {schema}.luas_stops")
        ).fetchall()

    if not stops:
        print("No stops in DB. Run luas_stops_to_db() first!")
        return

    forecast_rows = []

    for stop_id, line_name in stops:
        print(f"Fetching forecast for {stop_id} ({line_name})...")

        entries = fetch_forecast_for_stop(stop_id)
        for e in entries:
            e["stop_id"] = stop_id
            e["line"] = line_name
            forecast_rows.append(e)

    print(f"Collected {len(forecast_rows)} forecast rows.")

    if not forecast_rows:
        return

    insert_sql = f"""
    INSERT INTO {schema}.luas_forecasts
        (stop_id, line, direction, destination, due_mins, message)
    VALUES
        (:stop_id, :line, :direction, :destination, :due_mins, :message);
    """

    try:
        with SessionLocal() as db:
            db.execute(text(insert_sql), forecast_rows)
            db.commit()

        print(f"Inserted {len(forecast_rows)} LUAS forecast rows.")

    except Exception as e:
        print("Error inserting forecasts:", e)
