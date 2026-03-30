# test_app.py
from fastapi.testclient import TestClient

from inference_engine.main import (
    app,  # Assuming your FastAPI app is named `app` in `main.py`
)

client = TestClient(app)  # Initialize the FastAPI test client


# Test Root Endpoint ("/")
def test_root() -> None:
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {
        "service": "Recommendation Engine API",
        "version": "2.0.0",
        "status": "running",
        "scheduler": {
            "enabled": True,
            "interval_hours": 1,
            "data_indicators": ["bus", "car", "train"],
        },
        "endpoints": {
            "generate_recommendation": "POST /recommendations/generate",
            "traffic_recommendations": "POST /recommendations/traffic",
            "trigger_scheduler": "POST /scheduler/trigger-now",
            "scheduler_status": "GET /scheduler/status",
            "mock_data_engine": "GET /mock/data-engine?data_indicator=bus|car|train",
            "mock_notification": "POST /mock/notification",
        },
        "documentation": {
            "swagger_ui": "/docs",
            "redoc": "/redoc",
        },
    }


# Test manual trigger of the scheduler
def test_trigger_scheduler_manually() -> None:
    response = client.post("/scheduler/trigger-now")
    assert response.status_code == 200
    assert "message" in response.json()
    assert response.json()["message"] == "Batch processing completed"


# Test get scheduler status endpoint
def test_get_scheduler_status() -> None:
    response = client.get("/scheduler/status")
    assert response.status_code == 200
    assert "scheduler_running" in response.json()
    assert "fetch_interval_hours" in response.json()
    assert "data_indicators" in response.json()


def test_generate_traffic_recommendations() -> None:
    response = client.post(
        "/recommendations/traffic",
        json=[
            {
                "siteId": 101,
                "lat": 53.34,
                "lon": -6.26,
                "avgVolume": 540,
                "dayType": "weekday",
                "timeSlot": "morning_peak",
            }
        ],
    )
    assert response.status_code == 200
    payload = response.json()
    assert len(payload) == 1
    assert payload[0]["siteId"] == 101
    assert payload[0]["confidenceScore"] >= 0.68
    assert payload[0]["alternativeRoutes"][0]["estimatedTimeSavingsMinutes"] > 0
