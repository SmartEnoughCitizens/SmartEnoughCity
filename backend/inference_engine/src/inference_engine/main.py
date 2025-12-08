from fastapi import FastAPI, HTTPException, BackgroundTasks
from pydantic import BaseModel
from typing import Optional, Dict, Any
import httpx
import logging
from datetime import datetime
import asyncio
from contextlib import asynccontextmanager
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger

# app = FastAPI()

FETCH_INTERVAL_HOURS = 1  # Fetch data every 1 hour (change to 24 for daily)
DATA_INDICATORS = ["bus", "car", "train"]  # Transport types to process

# Initialize scheduler
scheduler = AsyncIOScheduler()
recommendations_store = {}  # In-memory storage
app = FastAPI(title="Recommendation Engine API")

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# # Configuration
# DATA_ENGINE_URL: str = "http://localhost:8080/api/v1/recommendation-engine/indicators/query"
# NOTIFICATION_API_URL: str = "http://localhost:8081/api/v1/notification"


# Change these lines in your code:
DATA_ENGINE_URL: str = "http://localhost:8000/mock/data-engine"  # Point to your own mock
NOTIFICATION_API_URL: str = "http://localhost:8000/mock/notification"  # Point to your own mock

# Request/Response Models
class RecommendationRequest(BaseModel):
    context: Optional[Dict[str, Any]] = None
    
class RecommendationResponse(BaseModel):
    recommendation_id: str
    status: str
    message: str
    timestamp: str

class NotificationPayload(BaseModel):
    data_indicator: str
    recommendation: Dict[str, Any]
    priority: str = "normal"

## Integration Layer
async def fetch_data_from_engine(data_indicator: str) -> Dict[str, Any]:
    """Fetch data from Data Analysis Engine"""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                DATA_ENGINE_URL,
                params={"indicatorType": data_indicator}
            )
            response.raise_for_status()
            data = response.json()
            logger.info(f"Fetched data from Data Engine for data indicator {data_indicator}")
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
            logger.info(f"Notification sent for data indicator {payload.data_indicator}")
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
    
    async def process_recommendation(self, data_indicator: str, context: Optional[Dict] = None) -> str:
        """
        Main workflow:
        1. Fetch data from Data Engine
        2. Generate recommendation using model
        3. Send notification
        """
        recommendation_id = f"rec_{data_indicator}_{int(datetime.utcnow().timestamp())}"
        created_at = datetime.utcnow().isoformat()  # Store timestamp once

        try:
            # Update status - FIXED: Use data_indicator as temporary key
            recommendations_store[data_indicator] = {
                "status": "processing",
                "data_indicator": data_indicator,
                "created_at": created_at
            }
            
            # Step 1: Fetch data
            logger.info(f"Fetching data for indicator {data_indicator}")
            data = await fetch_data_from_engine(data_indicator)
            
            # Step 2: Generate recommendation
            logger.info(f"Generating recommendation for indicator {data_indicator}")
            recommendation = self.model.generate_recommendations(data)
            
            # Add context if provided
            if context:
                recommendation["context"] = context
            
            # Step 3: Send notification
            logger.info(f"Sending notification for service indicator {data_indicator}")
            notification_payload = NotificationPayload(
                data_indicator=data_indicator,
                recommendation=recommendation,
                priority="normal"
            )
            
            notification_sent = await send_notification(notification_payload)
            
            # Update final status - FIXED: Now we can safely use created_at
            recommendations_store[recommendation_id] = {
                "status": "completed" if notification_sent else "notification_failed",
                "data_indicator": data_indicator,
                "recommendation": recommendation,
                "notification_sent": notification_sent,
                "created_at": created_at,  # Use the stored timestamp
                "completed_at": datetime.utcnow().isoformat()
            }
            
            # Clean up temporary key
            if data_indicator in recommendations_store:
                del recommendations_store[data_indicator]
            
            logger.info(f"Recommendation {recommendation_id} completed")
            return recommendation_id
            
        except Exception as e:
            logger.error(f"Error processing recommendation: {str(e)}")
            recommendations_store[recommendation_id] = {
                "status": "failed",
                "data_indicator": data_indicator,
                "error": str(e),
                "created_at": created_at,  # Use the stored timestamp
                "failed_at": datetime.utcnow().isoformat()
            }
            raise
    async def process_batch_recommendations(self):
        logger.info("=" * 80)
        logger.info("⏰ SCHEDULED TASK TRIGGERED")
        logger.info(f"📅 Time: {datetime.utcnow().isoformat()}")
        logger.info(f"🚗 Data indicators: {DATA_INDICATORS}")
        logger.info("=" * 80)
        
        results = {
            "total_processed": 0,
            "successful": 0,
            "failed": 0,
            "details": []
        }
        
        # Process each data indicator
        for data_indicator in DATA_INDICATORS:
            try:
                logger.info(f"🔄 Processing: {data_indicator}")
                rec_id = await self.process_recommendation(
                    data_indicator=data_indicator,
                    context={
                        "source": "scheduled_task",
                        "scheduled_at": datetime.utcnow().isoformat()
                    }
                )
                results["successful"] += 1
                results["details"].append({
                    "data_indicator": data_indicator,
                    "status": "success",
                    "recommendation_id": rec_id
                })
            except Exception as e:
                logger.error(f"❌ Failed for {data_indicator}: {str(e)}")
                results["failed"] += 1
                results["details"].append({
                    "data_indicator": data_indicator,
                    "status": "failed",
                    "error": str(e)
                })
            
            results["total_processed"] += 1
            
            # Small delay between requests
            await asyncio.sleep(0.5)
        
        logger.info("=" * 80)
        logger.info(f"✅ SCHEDULED TASK COMPLETED")
        logger.info(f"📊 Total: {results['total_processed']} | "
                   f"✅ Success: {results['successful']} | "
                   f"❌ Failed: {results['failed']}")
        logger.info("=" * 80)
        
        return results



# Initialize service
recommendation_service = RecommendationService()


## Scheduler Functions 
async def scheduled_recommendation_task():
    """
    This function is called by the scheduler
    """
    try:
        await recommendation_service.process_batch_recommendations()
    except Exception as e:
        logger.error(f"❌ Scheduled task failed: {str(e)}")

def start_scheduler():
    """
    Initialize and start the scheduler
    """
    logger.info("🕐 Initializing scheduler...")
    
    # Add the scheduled job
    scheduler.add_job(
        scheduled_recommendation_task,
        ##trigger=IntervalTrigger(hours=FETCH_INTERVAL_HOURS),
        trigger=IntervalTrigger(minutes=1),
        id="fetch_recommendations",
        name="Fetch and generate recommendations",
        replace_existing=True
    )
    
    # Start the scheduler
    scheduler.start()
    logger.info(f"✅ Scheduler started! Task will run every {FETCH_INTERVAL_HOURS} hour(s)")
    logger.info(f"🚗 Data indicators: {DATA_INDICATORS}")

def shutdown_scheduler():
    """
    Gracefully shutdown the scheduler
    """
    logger.info("🛑 Shutting down scheduler...")
    scheduler.shutdown()
    logger.info("✅ Scheduler stopped")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Handle startup and shutdown events
    """
    # Startup
    logger.info("🚀 Application starting up...")
    start_scheduler()
    logger.info("✅ Application ready!")
    
    yield
    
    # Shutdown
    logger.info("🛑 Application shutting down...")
    shutdown_scheduler()
    logger.info("✅ Application stopped!")


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
        # Get data_indicator from context, default to 'bus'
        data_indicator = "bus"
        if request.context and "data_indicator" in request.context:
            data_indicator = request.context["data_indicator"]

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
    

@app.post("/scheduler/trigger-now")
async def trigger_scheduler_manually():
    """
    Manually trigger the scheduled task (for testing)
    """
    logger.info("🔧 Manual trigger of scheduled task requested")
    try:
        results = await recommendation_service.process_batch_recommendations()
        return {
            "message": "Batch processing completed",
            "results": results,
            "timestamp": datetime.utcnow().isoformat()
        }
    except Exception as e:
        logger.error(f"❌ Manual trigger failed: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/scheduler/status")
async def get_scheduler_status():
    """
    Get scheduler status and configuration
    """
    jobs = scheduler.get_jobs()
    return {
        "scheduler_running": scheduler.running,
        "fetch_interval_hours": FETCH_INTERVAL_HOURS,
        "data_indicators": DATA_INDICATORS,
        "scheduled_jobs": [
            {
                "id": job.id,
                "name": job.name,
                "next_run_time": str(job.next_run_time)
            }
            for job in jobs
        ]
    }


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
        "notification_id": f"notif_{payload.get('data_indicator')}_{int(datetime.utcnow().timestamp())}"
    }
# @app.get("/")
# def hello_world():
#     return {"Hello": "World"}

@app.get("/")
async def root():
    """Root endpoint with API information"""
    return {
        "service": "Recommendation Engine API",
        "version": "2.0.0",
        "status": "running",
        "scheduler": {
            "enabled": True,
            "interval_hours": FETCH_INTERVAL_HOURS,
            "data_indicators": DATA_INDICATORS
        },
        "endpoints": {
            "generate_recommendation": "POST /recommendations/generate",
            "trigger_scheduler": "POST /scheduler/trigger-now",
            "scheduler_status": "GET /scheduler/status",
            "mock_data_engine": "GET /mock/data-engine?data_indicator=bus|car|train",
            "mock_notification": "POST /mock/notification"
        },
        "documentation": {
            "swagger_ui": "/docs",
            "redoc": "/redoc"
        }
    }
