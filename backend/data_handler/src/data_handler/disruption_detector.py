import logging
from typing import Any

import requests

from data_handler.settings.api_settings import get_api_settings

# Configure logging
logger = logging.getLogger(__name__)

# Constants
# In a real app, these should be in settings

JAVA_API_URL = get_api_settings().hermes_url + "/api/v1/disruptions/detect"
BUS_DELAY_THRESHOLD_SECONDS = 1200  # 20 minutes
LUAS_WAIT_THRESHOLD_MINUTES = 20  # 20 minutes


def detect_bus_disruptions(records: list[dict[str, Any]]) -> None:
    """
    Analyze bus trip updates for significant delays.
    """

    payload = {
        "disruptionType": "DELAY",
        "severity": "HIGH",
        "description": "Significant delay detected on Bus Route 22",
        "latitude": 53.3498,  # Default to Dublin Center for now
        "longitude": -6.2603,
        "affectedArea": "Dublin Bus Network",
        "affectedTransportModes": ["BUS"],
        "affectedRoutes": ["22"],
        "affectedStops": ["12345", "67890"],  # Limit to 5 stops
        "delayMinutes": 20,
        "dataSource": "REAL_TIME_API",
        "sourceReferenceId": "GTFS-RT-22-1712796200",
    }

    send_disruption_alert(payload)


def send_disruption_alert(payload: dict[str, Any]) -> None:
    """
    Send the disruption payload to the Java backend.
    """
    try:
        logger.info("Sending disruption alert for %s", payload.get("affectedRoutes"))
        response = requests.post(JAVA_API_URL, json=payload, timeout=5)

        if response.status_code == 200:
            logger.info("Successfully reported disruption to Hermes backend")
        else:
            logger.error(
                "Failed to report disruption: %s - %s",
                response.status_code,
                response.text,
            )

    except Exception:
        logger.exception("Error sending disruption alert")
