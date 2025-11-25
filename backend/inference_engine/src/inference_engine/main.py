from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel
from typing import Optional, Dict, Any
import httpx
import logging
from datetime import datetime
import asyncio

# app = FastAPI()
app = FastAPI(title="Recommendation Engine API")

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# Configuration
DATA_ENGINE_URL = "http://localhost:8080/api/bus_data_recommendation"
NOTIFICATION_API_URL = "http://localhost:8081/api/send_notification"


# Request/Response Models
class RecommendationRequest(BaseModel):
    user_id: str
    context: Optional[Dict[str, Any]] = None
    
class RecommendationResponse(BaseModel):
    recommendation_id: str
    status: str
    message: str
    timestamp: str

class NotificationPayload(BaseModel):
    user_id: str
    recommendation: Dict[str, Any]
    priority: str = "normal"

## Integration Layer
async def fetch_data_from_engine(user_id: str) -> Dict[str, Any]:
    """Fetch data from Data Analysis Engine"""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                DATA_ENGINE_URL,
                params={"user_id": user_id}
            )
            response.raise_for_status()
            data = response.json()
            logger.info(f"Fetched data from Data Engine for user {user_id}")
            return data
    except httpx.HTTPError as e:
        logger.error(f"Error fetching data from Data Engine: {str(e)}")
        raise HTTPException(status_code=503, detail="Data Engine unavailable")
    except Exception as e:
        logger.error(f"Unexpected error: {str(e)}")
        ## look into the raise 
        raise HTTPException(status_code=500, detail="Internal server error")


async def send_notification(payload: NotificationPayload) -> bool:
    """Send notification to Notification Handler"""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                NOTIFICATION_API_URL,
                json=payload.dict()
            )
            response.raise_for_status()
            logger.info(f"Notification sent for user {payload.user_id}")
            return True
    except httpx.HTTPError as e:
        logger.error(f"Error sending notification: {str(e)}")
        return False
    except Exception as e:
        logger.error(f"Unexpected error sending notification: {str(e)}")
        return False


## Model Layer 
class RecommendationModel:
    """Simple recommendation model (replace with actual ML model)"""
    
    @staticmethod
    def generate_recommendations(data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Process data and generate recommendations
        Replace this with your actual ML model logic
        """
        # Example: Simple rule-based recommendation
        recommendations = {
            "transport_mode": "bus",
            "routes": [],
            "estimated_time": 0,
            "alternatives": []
        }
        
        # Simulate model processing
        if data.get("bus_data"):
            bus_data = data["bus_data"]
            recommendations["routes"] = bus_data.get("available_routes", [])
            recommendations["estimated_time"] = bus_data.get("avg_time", 0)
            
            # Generate alternatives
            if bus_data.get("congestion_level", 0) > 0.7:
                recommendations["alternatives"] = ["bicycle", "walking"]
        
        recommendations["confidence_score"] = 0.85
        recommendations["generated_at"] = datetime.utcnow().isoformat()
        
        return recommendations

## Service Layer

class RecommendationService:
    """Business logic for recommendation workflow"""
    
    def __init__(self):
        self.model = RecommendationModel()
    
    async def process_recommendation(self, user_id: str, context: Optional[Dict] = None) -> str:
        """
        Main workflow:
        1. Fetch data from Data Engine
        2. Generate recommendation using model
        3. Send notification
        """
        recommendation_id = f"rec_{user_id}_{int(datetime.utcnow().timestamp())}"
        
        try:
            # Update status
            recommendations_store[recommendation_id] = {
                "status": "processing",
                "user_id": user_id,
                "created_at": datetime.utcnow().isoformat()
            }
            
            # Step 1: Fetch data
            logger.info(f"Fetching data for user {user_id}")
            data = await fetch_data_from_engine(user_id)
            
            # Step 2: Generate recommendation
            logger.info(f"Generating recommendation for user {user_id}")
            recommendation = self.model.generate_recommendations(data)
            
            # Add context if provided
            if context:
                recommendation["context"] = context
            
            # Step 3: Send notification
            logger.info(f"Sending notification for user {user_id}")
            notification_payload = NotificationPayload(
                user_id=user_id,
                recommendation=recommendation,
                priority="normal"
            )
            
            notification_sent = await send_notification(notification_payload)
            
            # Update final status
            recommendations_store[recommendation_id] = {
                "status": "completed" if notification_sent else "notification_failed",
                "user_id": user_id,
                "recommendation": recommendation,
                "notification_sent": notification_sent,
                "created_at": recommendations_store[recommendation_id]["created_at"],
                "completed_at": datetime.utcnow().isoformat()
            }
            
            logger.info(f"Recommendation {recommendation_id} completed")
            return recommendation_id
            
        except Exception as e:
            logger.error(f"Error processing recommendation: {str(e)}")
            recommendations_store[recommendation_id] = {
                "status": "failed",
                "user_id": user_id,
                "error": str(e),
                "created_at": recommendations_store[recommendation_id]["created_at"],
                "failed_at": datetime.utcnow().isoformat()
            }
            raise

# Initialize service
recommendation_service = RecommendationService()

## API Endpoints

@app.post("/recommendations/generate", response_model=RecommendationResponse)
async def generate_recommendation(
    request: RecommendationRequest,
    background_tasks: BackgroundTasks
):
    """
    Generate recommendation for a user
    This endpoint triggers the full workflow asynchronously
    """
    try:
        # Process in background
        recommendation_id = await recommendation_service.process_recommendation(
            request.user_id,
            request.context
        )
        
        return RecommendationResponse(
            recommendation_id=recommendation_id,
            status="completed",
            message="Recommendation generated and notification sent",
            timestamp=datetime.utcnow().isoformat()
        )
    except Exception as e:
        logger.error(f"Error in generate_recommendation: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))
    
## Testing Endpoint
@app.post("/test/trigger")
async def test_trigger(user_id: str = "test_user_123"):
    """Test endpoint to trigger the full workflow"""
    request = RecommendationRequest(
        user_id=user_id,
        context={"test": True, "timestamp": datetime.utcnow().isoformat()}
    )
    return await generate_recommendation(request, BackgroundTasks())

# ==================== MOCK ENDPOINTS (FOR TESTING) ====================

@app.get("/mock/data-engine")
async def mock_data_engine(data_indicator: str):
    """
    🧪 MOCK: Simulates Data Analysis Engine
    This endpoint pretends to be the Spring Boot Data Engine
    
    Parameters:
    - data_indicator: Type of transport data (bus, car, train)
    
    Example: GET /mock/data-engine?data_indicator=bus
    """
    logger.info(f"🧪 Mock Data Engine called for data_indicator: {data_indicator}")
    
    # Different mock data based on transport type
    mock_data_templates = {
        "bus": {
            "transport_type": "bus",
            "available_routes": ["Route 101", "Route 202", "Route 303"],
            "congestion_level": 0.6,
            "avg_time": 25,
            "stops_count": 12,
            "next_arrival": "5 minutes",
            "fare": "$2.50"
        },
        "car": {
            "transport_type": "car",
            "available_routes": ["Highway A", "Main Street", "Park Avenue"],
            "congestion_level": 0.8,
            "avg_time": 18,
            "parking_availability": "limited",
            "traffic_status": "heavy",
            "estimated_cost": "$5.00"
        },
        "train": {
            "transport_type": "train",
            "available_routes": ["Red Line", "Blue Line", "Green Line"],
            "congestion_level": 0.3,
            "avg_time": 15,
            "next_departure": "10 minutes",
            "platform": "Platform 3",
            "fare": "$3.75"
        }
    }
    
    # Get data based on indicator, default to bus if not found
    data_indicator_lower = data_indicator.lower()
    if data_indicator_lower not in mock_data_templates:
        logger.warning(f"⚠️ Unknown data_indicator: {data_indicator}, defaulting to 'bus'")
        data_indicator_lower = "bus"
    
    mock_data = {
        "data": mock_data_templates[data_indicator_lower],
        "metadata": {
            "timestamp": datetime.utcnow().isoformat(),
            "data_source": "Mock Data Analysis Engine",
            "version": "1.0"
        }
    }
    
    return mock_data

@app.post("/mock/notification")
async def mock_notification_handler(payload: dict):
    """
    🧪 MOCK: Simulates Notification Handler
    This endpoint pretends to be the Notification Service
    """
    logger.info(f"🧪 Mock Notification Handler called")
    logger.info(f"📧 Notification would be sent to user: {payload.get('user_id')}")
    logger.info(f"📋 Recommendation: {payload.get('recommendation')}")
    
    return {
        "status": "success",
        "message": "Notification sent successfully",
        "notification_id": f"notif_{payload.get('user_id')}_{int(datetime.utcnow().timestamp())}"
    }
# @app.get("/")
# def hello_world():
#     return {"Hello": "World"}

@app.get("/")
async def root():
    """Root endpoint with API information"""
    return {
        "service": "Recommendation Engine API",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "generate_recommendation": "POST /recommendations/generate",
            "mock_data_engine": "GET /mock/data-engine?data_indicator=bus|car|train",
            "mock_notification": "POST /mock/notification"
        },
        "documentation": {
            "swagger_ui": "/docs",
            "redoc": "/redoc"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)