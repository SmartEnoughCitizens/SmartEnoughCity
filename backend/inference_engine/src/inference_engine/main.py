import asyncio
import logging
from collections.abc import Generator
from contextlib import asynccontextmanager
from datetime import datetime
from pathlib import Path
from typing import Any

import httpx
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from fastapi import BackgroundTasks, FastAPI, HTTPException
from pydantic import BaseModel

from inference_engine.settings.api_settings import get_api_settings

# ============== ML MODEL IMPORT ==============
# Import the ML-powered recommendation model
from ml_model import MLRecommendationModel
# =============================================

FETCH_INTERVAL_HOURS = 1
DATA_INDICATORS = ["bus", "car", "train"]


@asynccontextmanager
async def lifespan(app: FastAPI) -> Generator[None, Any, None]:
    """Handle startup and shutdown events"""
    logger.info("🚀 Application starting up...")
    
    # ============== LOAD ML MODEL ON STARTUP ==============
    global recommendation_service
    recommendation_service = RecommendationService()
    logger.info("🤖 ML Model loaded: %s", 
                "Yes" if recommendation_service.model.ml_model.is_trained else "No (using fallback)")
    # ======================================================
    
    start_scheduler()
    logger.info("✅ Application ready!")

    yield

    logger.info("🛑 Application shutting down...")
    shutdown_scheduler()
    logger.info("✅ Application stopped!")


scheduler = AsyncIOScheduler()
recommendations_store = {}
app = FastAPI(
    title="Recommendation Engine API",
    lifespan=lifespan,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

settings = get_api_settings()
DATA_ENGINE_URL: str = (
    settings.hermes_url + "/api/v1/recommendation-engine/indicators/query"
)
NOTIFICATION_API_URL: str = settings.hermes_url + "/notification/v1"


# Request/Response Models
class RecommendationRequest(BaseModel):
    context: dict[str, Any] | None = None


class RecommendationResponse(BaseModel):
    recommendation_id: str
    status: str
    message: str
    timestamp: str


class NotificationPayload(BaseModel):
    data_indicator: str
    recommendation: dict[str, Any]
    priority: str = "normal"


## Integration Layer
async def fetch_data_from_engine(data_indicator: str) -> dict[str, Any]:
    """Fetch data from Data Analysis Engine"""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.get(
                DATA_ENGINE_URL,
                params={"indicatorType": data_indicator},
            )
            response.raise_for_status()
            data = response.json()
            logger.info(
                "Fetched data from Data Engine for data indicator %s", data_indicator
            )
            return data
    except httpx.HTTPError:
        logger.exception("Error fetching data from Data Engine")
        return {}
    except Exception:
        logger.exception("Unexpected error")
        return {}


async def send_notification(payload: NotificationPayload) -> bool:
    """Send notification to Notification Handler"""
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                NOTIFICATION_API_URL,
                json=payload.dict(),
            )
            response.raise_for_status()
            logger.info(
                "Notification sent for data indicator %s", payload.data_indicator
            )
            return True
    except httpx.HTTPError:
        logger.exception("Error sending notification")
        return False
    except Exception:
        logger.exception("Unexpected error sending notification")
        return False


# ============== ML MODEL LAYER (REPLACES OLD RecommendationModel) ==============
class RecommendationService:
    """Business logic for recommendation workflow - NOW WITH ML!"""

    def __init__(self) -> None:
        # ============== KEY CHANGE: Use ML Model ==============
        # Look for trained model in common locations
        model_paths = [
            "models/transport_model.pkl",
            "/app/models/transport_model.pkl",
            Path(__file__).parent / "models" / "transport_model.pkl",
        ]
        
        model_path = None
        for path in model_paths:
            if Path(path).exists():
                model_path = str(path)
                break
        
        self.model = MLRecommendationModel(model_path)
        
        if self.model.ml_model.is_trained:
            logger.info("✅ ML Model loaded successfully from %s", model_path)
            logger.info("📊 Model metrics: %s", self.model.ml_model.training_metrics)
        else:
            logger.warning("⚠️ No trained ML model found. Using rule-based fallback.")
            logger.warning("   Run 'python train_model.py' to train a model.")
        # ======================================================

    async def process_recommendation(
        self, data_indicator: str, context: dict | None = None
    ) -> str:
        """
        Main workflow:
        1. Fetch data from Data Engine
        2. Generate recommendation using ML MODEL
        3. Send notification
        """
        recommendation_id = f"rec_{data_indicator}_{int(datetime.utcnow().timestamp())}"
        created_at = datetime.utcnow().isoformat()

        try:
            recommendations_store[data_indicator] = {
                "status": "processing",
                "data_indicator": data_indicator,
                "created_at": created_at,
            }

            # Step 1: Fetch data
            logger.info("Fetching data for indicator %s", data_indicator)
            data = await fetch_data_from_engine(data_indicator)

            # ============== STEP 2: ML PREDICTION ==============
            # The ML model now generates recommendations
            logger.info("🤖 Generating ML recommendation for indicator %s", data_indicator)
            
            # Add context info that helps the model
            if context:
                data["context"] = context
            
            # Add distance if available (important for ML model)
            if context and "distance_km" in context:
                data["distance_km"] = context["distance_km"]
            
            # Add weather if available
            if context and "weather_score" in context:
                data["weather_score"] = context["weather_score"]
            
            recommendation = self.model.generate_recommendations(data)
            
            # Log if we used ML or fallback
            if recommendation.get("ml_powered"):
                logger.info("✅ ML model prediction: %s (confidence: %.1f%%)", 
                           recommendation["transport_mode"],
                           recommendation["confidence_score"] * 100)
            else:
                logger.info("⚠️ Used rule-based fallback")
            # ===================================================

            if context:
                recommendation["context"] = context

            # Step 3: Send notification
            logger.info("Sending notification for service indicator %s", data_indicator)
            notification_payload = NotificationPayload(
                data_indicator=data_indicator,
                recommendation=recommendation,
                priority="high" if recommendation.get("confidence_score", 0) > 0.9 else "normal",
            )

            notification_sent = await send_notification(notification_payload)

            recommendations_store[recommendation_id] = {
                "status": "completed" if notification_sent else "notification_failed",
                "data_indicator": data_indicator,
                "recommendation": recommendation,
                "notification_sent": notification_sent,
                "created_at": created_at,
                "completed_at": datetime.utcnow().isoformat(),
                "ml_powered": recommendation.get("ml_powered", False),
            }

            recommendations_store.pop(data_indicator, None)

        except Exception as e:
            logger.exception("Error processing recommendation")
            recommendations_store[recommendation_id] = {
                "status": "failed",
                "data_indicator": data_indicator,
                "error": str(e),
                "created_at": created_at,
                "failed_at": datetime.utcnow().isoformat(),
            }
            raise

        else:
            logger.info("Recommendation %s completed", recommendation_id)
            return recommendation_id

    async def process_batch_recommendations(self) -> dict:
        logger.info("=" * 80)
        logger.info("⏰ SCHEDULED TASK TRIGGERED")
        logger.info("📅 Time: %s", datetime.utcnow().isoformat())
        logger.info("🚗 Data indicators: %s", DATA_INDICATORS)
        logger.info("🤖 ML Model active: %s", self.model.ml_model.is_trained)
        logger.info("=" * 80)

        results = {
            "total_processed": 0,
            "successful": 0,
            "failed": 0,
            "details": [],
        }

        for data_indicator in DATA_INDICATORS:
            try:
                logger.info("🔄 Processing: %s", data_indicator)
                rec_id = await self.process_recommendation(
                    data_indicator=data_indicator,
                    context={
                        "source": "scheduled_task",
                        "scheduled_at": datetime.utcnow().isoformat(),
                    },
                )
                results["successful"] += 1
                results["details"].append(
                    {
                        "data_indicator": data_indicator,
                        "status": "success",
                        "recommendation_id": rec_id,
                    }
                )
            except Exception as e:
                logger.exception("❌ Failed for %s", data_indicator)
                results["failed"] += 1
                results["details"].append(
                    {
                        "data_indicator": data_indicator,
                        "status": "failed",
                        "error": str(e),
                    }
                )

            results["total_processed"] += 1
            await asyncio.sleep(0.5)

        logger.info("=" * 80)
        logger.info("✅ SCHEDULED TASK COMPLETED")
        logger.info(
            "📊 Total: %s | ✅ Success: %s | ❌ Failed: %s",
            results["total_processed"],
            results["successful"],
            results["failed"],
        )
        logger.info("=" * 80)

        return results


# Initialize service (will be re-initialized in lifespan)
recommendation_service = RecommendationService()


## Scheduler Functions
async def scheduled_recommendation_task() -> None:
    try:
        await recommendation_service.process_batch_recommendations()
    except Exception:
        logger.exception("❌ Scheduled task failed")


def start_scheduler() -> None:
    logger.info("🕐 Initializing scheduler...")
    scheduler.add_job(
        scheduled_recommendation_task,
        trigger=IntervalTrigger(minutes=1),
        id="fetch_recommendations",
        name="Fetch and generate recommendations",
        replace_existing=True,
    )
    scheduler.start()
    logger.info(
        "✅ Scheduler started! Task will run every %s hour(s)", FETCH_INTERVAL_HOURS
    )
    logger.info("🚗 Data indicators: %s", DATA_INDICATORS)


def shutdown_scheduler() -> None:
    logger.info("🛑 Shutting down scheduler...")
    scheduler.shutdown()
    logger.info("✅ Scheduler stopped")


## API Endpoints
@app.post("/recommendations/generate", response_model=RecommendationResponse)
async def generate_recommendation(
    request: RecommendationRequest,
    background_tasks: BackgroundTasks,
) -> RecommendationResponse:
    try:
        if request.context and "data_indicator" in request.context:
            request.context["data_indicator"]

        recommendation_id = await recommendation_service.process_recommendation(
            request.context.get("data_indicator", "bus") if request.context else "bus",
            request.context,
        )

        return RecommendationResponse(
            recommendation_id=recommendation_id,
            status="completed",
            message="Recommendation generated and notification sent",
            timestamp=datetime.utcnow().isoformat(),
        )
    except Exception as e:
        logger.exception("Error in generate_recommendation")
        raise HTTPException(status_code=500, detail=str(e)) from None


@app.post("/scheduler/trigger-now")
async def trigger_scheduler_manually() -> dict:
    logger.info("🔧 Manual trigger of scheduled task requested")
    try:
        results = await recommendation_service.process_batch_recommendations()
        return {
            "message": "Batch processing completed",
            "results": results,
            "timestamp": datetime.utcnow().isoformat(),
        }
    except Exception as e:
        logger.exception("❌ Manual trigger failed")
        raise HTTPException(status_code=500, detail=str(e)) from None


@app.get("/scheduler/status")
async def get_scheduler_status() -> dict:
    jobs = scheduler.get_jobs()
    return {
        "scheduler_running": scheduler.running,
        "fetch_interval_hours": FETCH_INTERVAL_HOURS,
        "data_indicators": DATA_INDICATORS,
        "ml_model_active": recommendation_service.model.ml_model.is_trained,
        "ml_model_metrics": recommendation_service.model.ml_model.training_metrics,
        "scheduled_jobs": [
            {
                "id": job.id,
                "name": job.name,
                "next_run_time": str(job.next_run_time),
            }
            for job in jobs
        ],
    }


# ============== NEW: ML MODEL INFO ENDPOINT ==============
@app.get("/model/info")
async def get_model_info() -> dict:
    """Get information about the ML model"""
    model = recommendation_service.model.ml_model
    
    info = {
        "ml_powered": model.is_trained,
        "model_version": model.MODEL_VERSION,
        "training_metrics": model.training_metrics if model.is_trained else None,
    }
    
    if model.is_trained:
        importance = model.get_feature_importance()
        info["top_features"] = {
            "mode_classification": importance["mode_classifier"][:5],
            "time_prediction": importance["time_regressor"][:5],
        }
    
    return info
# =========================================================


## Testing Endpoint
@app.post("/test/trigger")
async def test_trigger(data_indicator: str = "bus") -> RecommendationResponse:
    """Test endpoint to trigger the full workflow"""
    request = RecommendationRequest(
        context={
            "test": True, 
            "timestamp": datetime.utcnow().isoformat(),
            "data_indicator": data_indicator,
        },
    )
    return await generate_recommendation(request, BackgroundTasks())


# ==================== MOCK ENDPOINTS (FOR TESTING) ====================

@app.get("/mock/data-engine")
async def mock_data_engine(indicatorType: str) -> dict:
    """
    🧪 MOCK: Simulates Data Analysis Engine
    """
    logger.info("🧪 Mock Data Engine called for indicator: %s", indicatorType)

    mock_data_templates = {
        "bus": {
            "transport_type": "bus",
            "bus_data": {
                "available_routes": ["Route 101", "Route 202", "Route 303"],
                "congestion_level": 0.6,
                "avg_time": 25,
                "stops_count": 12,
                "next_arrival": "5 minutes",
                "fare": "$2.50",
            }
        },
        "car": {
            "transport_type": "car",
            "car_data": {
                "available_routes": ["Highway A", "Main Street", "Park Avenue"],
                "congestion_level": 0.8,
                "avg_time": 18,
                "parking_availability": "limited",
                "traffic_status": "heavy",
                "estimated_cost": "$5.00",
            }
        },
        "train": {
            "transport_type": "train",
            "train_data": {
                "available_routes": ["Red Line", "Blue Line", "Green Line"],
                "congestion_level": 0.3,
                "avg_time": 15,
                "next_departure": "10 minutes",
                "platform": "Platform 3",
                "fare": "$3.75",
            }
        },
    }

    indicator_lower = indicatorType.lower()
    if indicator_lower not in mock_data_templates:
        logger.warning("⚠️ Unknown indicator: %s, defaulting to 'bus'", indicatorType)
        indicator_lower = "bus"

    return {
        **mock_data_templates[indicator_lower],
        "metadata": {
            "timestamp": datetime.utcnow().isoformat(),
            "data_source": "Mock Data Analysis Engine",
            "version": "1.0",
        },
    }


@app.post("/mock/notification")
async def mock_notification_handler(payload: dict) -> dict:
    """🧪 MOCK: Simulates Notification Handler"""
    logger.info("🧪 Mock Notification Handler called")
    logger.info("📧 Notification for indicator: %s", payload.get("data_indicator"))
    logger.info("📋 Recommendation: %s", payload.get("recommendation"))

    return {
        "status": "success",
        "message": "Notification sent successfully",
        "notification_id": f"notif_{payload.get('data_indicator')}_{int(datetime.utcnow().timestamp())}",
    }


@app.get("/")
async def root() -> dict:
    """Root endpoint with API information"""
    return {
        "service": "Recommendation Engine API",
        "version": "2.0.0",
        "status": "running",
        "ml_model": {
            "active": recommendation_service.model.ml_model.is_trained,
            "version": recommendation_service.model.ml_model.MODEL_VERSION,
        },
        "scheduler": {
            "enabled": True,
            "interval_hours": FETCH_INTERVAL_HOURS,
            "data_indicators": DATA_INDICATORS,
        },
        "endpoints": {
            "generate_recommendation": "POST /recommendations/generate",
            "trigger_scheduler": "POST /scheduler/trigger-now",
            "scheduler_status": "GET /scheduler/status",
            "model_info": "GET /model/info",
            "mock_data_engine": "GET /mock/data-engine?indicatorType=bus|car|train",
            "mock_notification": "POST /mock/notification",
        },
        "documentation": {
            "swagger_ui": "/docs",
            "redoc": "/redoc",
        },
    }
