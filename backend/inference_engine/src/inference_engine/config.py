from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    """Application settings"""
    
    # Service Configuration
    SERVICE_NAME: str = "Recommendation Engine"
    SERVICE_PORT: int = 8000
    
    # External API URLs
    DATA_ENGINE_URL: str = "http://localhost:8080/api/v1/recommendation-engine/indicators/query"
    NOTIFICATION_API_URL: str = "http://localhost:8081/api/v1/notification"
    
    # Timeout settings
    HTTP_TIMEOUT: int = 30
    
    # Model Configuration
    MODEL_CONFIDENCE_THRESHOLD: float = 0.7
    
    # Logging
    LOG_LEVEL: str = "INFO"
    
    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()