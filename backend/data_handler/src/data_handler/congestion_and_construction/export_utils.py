"""Export utilities for traffic data visualization."""

import csv
import json
import logging
import webbrowser
from pathlib import Path

from data_handler.congestion_and_construction.models import TrafficEvent

logger = logging.getLogger(__name__)


def export_to_csv(
    events: list[TrafficEvent],
    output_path: Path | str,
) -> Path:
    """
    Export traffic events to a CSV file.

    Args:
        events: List of TrafficEvent objects to export
        output_path: Path for the output CSV file

    Returns:
        Path to the created CSV file
    """
    output_path = Path(output_path)
    
    fieldnames = ["type", "title", "lat", "lon", "description", "fetched_at"]

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()

        for event in events:
            writer.writerow({
                "type": event.event_type.value,
                "title": event.title,
                "lat": event.lat,
                "lon": event.lon,
                "description": event.description or "",
                "fetched_at": event.fetched_at.isoformat(),
            })

    logger.info("Exported %d events to CSV: %s", len(events), output_path)
    return output_path


def export_to_html_map(
    events: list[TrafficEvent],
    output_path: Path | str,
    center_lat: float = 53.3498,
    center_lon: float = -6.2603,
    zoom: int = 11,
    open_in_browser: bool = False,
) -> Path:
    """
    Export traffic events to an interactive HTML map using Leaflet.

    Args:
        events: List of TrafficEvent objects to export
        output_path: Path for the output HTML file
        center_lat: Map center latitude (default: Dublin)
        center_lon: Map center longitude (default: Dublin)
        zoom: Initial map zoom level
        open_in_browser: Whether to open the map in default browser

    Returns:
        Path to the created HTML file
    """
    output_path = Path(output_path)

    # Convert events to JSON-serializable format
    events_data = [
        {
            "type": event.event_type.value,
            "title": event.title,
            "description": event.description or "",
            "lat": event.lat,
            "lng": event.lon,
            "color": event.color,
        }
        for event in events
    ]

    js_data = json.dumps(events_data)

    html_content = f"""<!DOCTYPE html>
<html>
<head>
    <title>Dublin Live Traffic</title>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        body {{ margin: 0; padding: 0; }}
        #map {{ height: 100vh; width: 100%; }}
        .legend {{
            background: white;
            padding: 10px;
            border-radius: 5px;
            box-shadow: 0 0 15px rgba(0,0,0,0.2);
        }}
        .legend h4 {{ margin: 0 0 10px 0; }}
        .legend-item {{ display: flex; align-items: center; margin: 5px 0; }}
        .legend-color {{
            width: 20px;
            height: 20px;
            border-radius: 50%;
            margin-right: 8px;
        }}
    </style>
</head>
<body>
    <div id="map"></div>
    <script>
        var trafficData = {js_data};
        var map = L.map('map').setView([{center_lat}, {center_lon}], {zoom});
        
        L.tileLayer('https://{{s}}.tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {{
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }}).addTo(map);

        // Add markers for each event
        trafficData.forEach(function(item) {{
            L.circleMarker([item.lat, item.lng], {{
                color: item.color,
                fillColor: item.color,
                fillOpacity: 0.8,
                radius: 8
            }}).addTo(map)
            .bindPopup(
                "<b>" + item.type + "</b><br>" + 
                item.title + 
                (item.description ? "<br><small>" + item.description + "</small>" : "")
            );
        }});

        // Add legend
        var legend = L.control({{position: 'bottomright'}});
        legend.onAdd = function(map) {{
            var div = L.DomUtil.create('div', 'legend');
            div.innerHTML = `
                <h4>Event Types</h4>
                <div class="legend-item">
                    <div class="legend-color" style="background: red;"></div>
                    <span>Closure/Incident</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background: orange;"></div>
                    <span>Congestion</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background: black;"></div>
                    <span>Roadworks</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background: blue;"></div>
                    <span>Warning</span>
                </div>
            `;
            return div;
        }};
        legend.addTo(map);

        // Display event count
        var info = L.control({{position: 'topright'}});
        info.onAdd = function(map) {{
            var div = L.DomUtil.create('div', 'legend');
            div.innerHTML = '<b>' + trafficData.length + ' events</b>';
            return div;
        }};
        info.addTo(map);
    </script>
</body>
</html>"""

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html_content)

    logger.info("Exported %d events to HTML map: %s", len(events), output_path)

    if open_in_browser:
        try:
            webbrowser.open(f"file://{output_path.resolve()}")
        except Exception as e:
            logger.warning("Could not open browser: %s", e)

    return output_path


def events_to_geojson(events: list[TrafficEvent]) -> dict:
    """
    Convert traffic events to GeoJSON format.

    Args:
        events: List of TrafficEvent objects

    Returns:
        GeoJSON FeatureCollection dictionary
    """
    features = []

    for event in events:
        feature = {
            "type": "Feature",
            "geometry": {
                "type": "Point",
                "coordinates": [event.lon, event.lat],
            },
            "properties": {
                "event_type": event.event_type.value,
                "title": event.title,
                "description": event.description,
                "color": event.color,
                "fetched_at": event.fetched_at.isoformat(),
            },
        }
        features.append(feature)

    return {
        "type": "FeatureCollection",
        "features": features,
    }


def export_to_geojson(
    events: list[TrafficEvent],
    output_path: Path | str,
) -> Path:
    """
    Export traffic events to a GeoJSON file.

    Args:
        events: List of TrafficEvent objects to export
        output_path: Path for the output GeoJSON file

    Returns:
        Path to the created GeoJSON file
    """
    output_path = Path(output_path)

    geojson_data = events_to_geojson(events)

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(geojson_data, f, indent=2)

    logger.info("Exported %d events to GeoJSON: %s", len(events), output_path)
    return output_path
