import requests
import json
import csv
import webbrowser
import os

# --- CONFIGURATION (DUBLIN AREA) ---
API_URL = "https://www.tiitraffic.ie/api/graphql"

# UPDATED COORDINATES:
# North: 53.65 (Past Swords/Balbriggan)
# South: 53.15 (Past Bray/Greystones)
# West:  -6.70 (Past Maynooth/Leixlip)
# East:  -5.90 (Into the Irish Sea to catch the coast)
GRAPHQL_PAYLOAD = [
    {
        "query": """
        query MapFeatures($input: MapFeaturesArgs!) { 
            mapFeaturesQuery(input: $input) { 
                mapFeatures { 
                    title 
                    tooltip 
                    features { 
                        geometry 
                        properties 
                    } 
                } 
            } 
        }
        """,
        "variables": {
            "input": {
                "north": 53.65, 
                "south": 53.15, 
                "east": -5.90, 
                "west": -6.70,
                "zoom": 11,  # Higher zoom level ensures we get detailed city events
                "layerSlugs": ["roadReports", "roadwork", "weatherWarningsAreaEvents"], 
                "nonClusterableUris": ["404"]
            }
        }
    }
]

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Content-Type": "application/json",
    "Referer": "https://www.tiitraffic.ie/"
}

def fetch_traffic_data():
    """Connects to TII and grabs the raw JSON data."""
    print("🚀 Connecting to TII API (Dublin Area)...")
    try:
        response = requests.post(API_URL, json=GRAPHQL_PAYLOAD, headers=HEADERS, timeout=15)
        response.raise_for_status()
        return response.json()
    except Exception as e:
        print(f"❌ Error fetching data: {e}")
        return None

def determine_event_type(title, description):
    """Categorizes the event based on keywords."""
    text = (f"{title} {description}").lower()
    
    if any(x in text for x in ["congestion", "queue", "delay", "slow"]):
        return "CONGESTION", "orange"
    elif any(x in text for x in ["closed", "blocked", "closure", "impassable", "collision", "crash"]):
        return "CLOSURE/INCIDENT", "red"
    elif any(x in text for x in ["roadworks", "works", "maintenance"]):
        return "ROADWORKS", "black"
    else:
        return "WARNING", "blue"

def process_data(raw_data):
    """Parses the JSON and extracts clean event objects."""
    clean_events = []
    
    try:
        data_block = raw_data[0]['data']['mapFeaturesQuery']['mapFeatures']
    except (KeyError, IndexError, TypeError):
        print("⚠️ No data found in response.")
        return []

    print(f"✅ Processing {len(data_block)} raw items...")

    for item in data_block:
        title = item.get('title') or item.get('tooltip') or "Unknown Event"
        
        features = item.get('features', [])
        if not features: continue
            
        feature = features[0]
        props = feature.get('properties', {})
        geometry = feature.get('geometry', {})
        
        coords = geometry.get('coordinates')
        if not coords: continue
            
        # Handle Point vs LineString
        if isinstance(coords[0], list): 
            lng, lat = coords[0][0], coords[0][1]
        else:
            lng, lat = coords[0], coords[1]

        description = props.get('description') or props.get('encodedDescription') or ""
        
        event_type, color = determine_event_type(title, description)

        clean_events.append({
            "type": event_type,
            "title": title,
            "description": description.replace("\n", " "),
            "lat": lat,
            "lng": lng,
            "color": color
        })
        
    return clean_events

def save_to_csv(events, filename="dublin_traffic_data.csv"):
    if not events: return
    keys = ["type", "title", "lat", "lng", "description"]
    with open(filename, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=keys, extrasaction='ignore')
        writer.writeheader()
        writer.writerows(events)
    print(f"📄 CSV saved to: {filename}")

def save_to_map(events, filename="dublin_traffic_map.html"):
    if not events: return
    js_data = json.dumps(events)
    
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>Dublin Live Traffic</title>
        <meta charset="utf-8" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
        <style>body {{ margin: 0; }} #map {{ height: 100vh; }}</style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var trafficData = {js_data};
            // Centered on Dublin City (O'Connell Bridge area)
            var map = L.map('map').setView([53.3498, -6.2603], 11);
            
            L.tileLayer('https://{{s}}.tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png').addTo(map);

            trafficData.forEach(function(item) {{
                L.circleMarker([item.lat, item.lng], {{
                    color: item.color,
                    fillColor: item.color,
                    fillOpacity: 0.8,
                    radius: 8
                }}).addTo(map)
                .bindPopup("<b>" + item.type + "</b><br>" + item.title + "<br>" + item.description);
            }});
        </script>
    </body>
    </html>
    """
    with open(filename, 'w', encoding='utf-8') as f:
        f.write(html_content)
    print(f"🗺️ Map saved to: {filename}")
    try:
        webbrowser.open('file://' + os.path.realpath(filename))
    except:
        pass

if __name__ == "__main__":
    raw_json = fetch_traffic_data()
    if raw_json:
        events = process_data(raw_json)
        print(f"📊 Found {len(events)} events in Dublin area.")
        save_to_csv(events)
        save_to_map(events)