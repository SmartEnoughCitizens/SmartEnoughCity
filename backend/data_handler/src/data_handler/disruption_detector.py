import requests
import logging
from typing import List, Dict, Any
from datetime import datetime

# Configure logging
logger = logging.getLogger(__name__)

# Constants
# In a real app, these should be in settings
JAVA_API_URL = "http://localhost:8084/api/v1/disruptions/detect"
BUS_DELAY_THRESHOLD_SECONDS = 1200  # 20 minutes
LUAS_WAIT_THRESHOLD_MINUTES = 20    # 20 minutes

def detect_bus_disruptions(records: List[Dict[str, Any]]):
    """
    Analyze bus trip updates for significant delays.
    """
    if not records:
        return

    logger.info(f"Analyzing {len(records)} bus records for disruptions...")
    
    # Simple aggregation to avoid spamming: group by route
    disrupted_routes = {}

    for row in records:
        arrival_delay = row.get("arrival_delay")
        
        # arrival_delay is in seconds
        if arrival_delay and arrival_delay > BUS_DELAY_THRESHOLD_SECONDS:
            route_id = row.get("route_id", "unknown")
            stop_id = row.get("stop_id", "unknown")
            
            if route_id not in disrupted_routes:
                disrupted_routes[route_id] = {
                    "max_delay": arrival_delay,
                    "stops": set(),
                    "trip_ids": set()
                }
            
            # Update aggregations
            if arrival_delay > disrupted_routes[route_id]["max_delay"]:
                disrupted_routes[route_id]["max_delay"] = arrival_delay
                
            disrupted_routes[route_id]["stops"].add(stop_id)
            disrupted_routes[route_id]["trip_ids"].add(row.get("trip_id"))

    # Send alerts for detected disruptions
    for route_id, data in disrupted_routes.items():
        delay_min = int(data["max_delay"] / 60)
        
        payload = {
            "disruptionType": "DELAY",
            "severity": "HIGH" if delay_min > 30 else "MEDIUM",
            "description": f"Significant delay detected on Bus Route {route_id}",
            "latitude": 53.3498, # Default to Dublin Center for now
            "longitude": -6.2603,
            "affectedArea": "Dublin Bus Network",
            "affectedTransportModes": ["BUS"],
            "affectedRoutes": [str(route_id)],
            "affectedStops": list(data["stops"])[:5], # Limit to 5 stops
            "delayMinutes": delay_min,
            "dataSource": "REAL_TIME_API",
            "sourceReferenceId": f"GTFS-RT-{route_id}-{datetime.now().timestamp()}"
        }
        
        send_disruption_alert(payload)

def detect_luas_disruptions(forecasts: List[Dict[str, Any]]):
    """
    Analyze Luas forecasts for significant wait times.
    """
    if not forecasts:
        return

    logger.info(f"Analyzing {len(forecasts)} Luas forecasts for disruptions...")
    
    disrupted_lines = {}
    
    for row in forecasts:
        due_mins = row.get("due_mins")
        
        # Check for long wait times or "DUE" message indicating issues
        if isinstance(due_mins, int) and due_mins > LUAS_WAIT_THRESHOLD_MINUTES:
            line = row.get("line", "unknown")
            if line not in disrupted_lines:
                disrupted_lines[line] = {
                    "max_wait": due_mins,
                    "stops": set(),
                    "message": row.get("message", "")
                }
            
            if due_mins > disrupted_lines[line]["max_wait"]:
                 disrupted_lines[line]["max_wait"] = due_mins
                 
            disrupted_lines[line]["stops"].add(row.get("stop_id"))

    # Send alerts
    for line, data in disrupted_lines.items():
        payload = {
            "disruptionType": "DELAY",
            "severity": "HIGH",
            "description": f"Long wait times detected on {line}: {data['message']}",
            "latitude": 53.3498,
            "longitude": -6.2603,
            "affectedArea": f"Luas {line}",
            "affectedTransportModes": ["TRAM"],
            "affectedRoutes": [line],
            "affectedStops": list(data["stops"])[:5],
            "delayMinutes": data["max_wait"],
            "dataSource": "REAL_TIME_API",
            "sourceReferenceId": f"LUAS-RT-{line}-{datetime.now().timestamp()}"
        }
        
        send_disruption_alert(payload)

def send_disruption_alert(payload: Dict[str, Any]):
    """
    Send the disruption payload to the Java backend.
    """
    try:
        logger.info(f"Sending disruption alert for {payload.get('affectedRoutes')}")
        response = requests.post(JAVA_API_URL, json=payload, timeout=5)
        
        if response.status_code == 200:
            logger.info("Successfully reported disruption to Hermes backend")
        else:
            logger.error(f"Failed to report disruption: {response.status_code} - {response.text}")
            
    except Exception as e:
        logger.error(f"Error sending disruption alert: {e}")
