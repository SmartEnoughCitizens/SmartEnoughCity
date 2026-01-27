"""
Transport Recommendation ML Model

This module implements a machine learning model for recommending optimal transport modes
based on various features like congestion, time, cost, weather, and user preferences.

The model uses a multi-output approach:
1. Classification: Predict the best transport mode (bus, car, train)
2. Regression: Predict estimated travel time and reliability score
"""

import json
import logging
import pickle
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

import numpy as np
from sklearn.ensemble import GradientBoostingClassifier, GradientBoostingRegressor
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class TransportFeatures:
    """Features extracted from transport data for ML prediction"""
    
    # Congestion levels (0-1 scale)
    bus_congestion: float
    car_congestion: float
    train_congestion: float
    
    # Average travel times (minutes)
    bus_time: float
    car_time: float
    train_time: float
    
    # Cost (normalized 0-1)
    bus_cost: float
    car_cost: float
    train_cost: float
    
    # Availability scores (0-1)
    bus_availability: float
    car_availability: float  # parking availability
    train_availability: float
    
    # Context features
    hour_of_day: int  # 0-23
    day_of_week: int  # 0-6 (Monday=0)
    is_rush_hour: bool
    weather_score: float  # 0-1 (1=good weather)
    distance_km: float
    
    # User preferences (optional, default neutral)
    prefer_eco: float = 0.5  # 0-1 scale
    prefer_comfort: float = 0.5
    prefer_speed: float = 0.5
    prefer_cost: float = 0.5
    
    def to_array(self) -> np.ndarray:
        """Convert features to numpy array for model input"""
        return np.array([
            self.bus_congestion,
            self.car_congestion,
            self.train_congestion,
            self.bus_time,
            self.car_time,
            self.train_time,
            self.bus_cost,
            self.car_cost,
            self.train_cost,
            self.bus_availability,
            self.car_availability,
            self.train_availability,
            self.hour_of_day / 23.0,  # Normalize hour
            self.day_of_week / 6.0,   # Normalize day
            float(self.is_rush_hour),
            self.weather_score,
            min(self.distance_km / 50.0, 1.0),  # Normalize distance (cap at 50km)
            self.prefer_eco,
            self.prefer_comfort,
            self.prefer_speed,
            self.prefer_cost,
        ])
    
    @classmethod
    def feature_names(cls) -> list[str]:
        """Return feature names for interpretability"""
        return [
            "bus_congestion", "car_congestion", "train_congestion",
            "bus_time", "car_time", "train_time",
            "bus_cost", "car_cost", "train_cost",
            "bus_availability", "car_availability", "train_availability",
            "hour_normalized", "day_normalized", "is_rush_hour",
            "weather_score", "distance_normalized",
            "prefer_eco", "prefer_comfort", "prefer_speed", "prefer_cost"
        ]


class TransportRecommendationModel:
    """
    ML model for transport mode recommendation.
    
    Uses ensemble methods (Gradient Boosting) for:
    - Mode classification (which transport to recommend)
    - Travel time prediction
    - Reliability score prediction
    """
    
    TRANSPORT_MODES = ["bus", "car", "train"]
    MODEL_VERSION = "1.0.0"
    
    def __init__(self, model_dir: str = "models"):
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(exist_ok=True)
        
        # Models
        self.mode_classifier: GradientBoostingClassifier | None = None
        self.time_regressor: GradientBoostingRegressor | None = None
        self.reliability_regressor: GradientBoostingRegressor | None = None
        
        # Preprocessing
        self.scaler: StandardScaler | None = None
        self.label_encoder: LabelEncoder | None = None
        
        # Model metadata
        self.is_trained = False
        self.training_metrics: dict[str, Any] = {}
        
    def _initialize_models(self) -> None:
        """Initialize model architectures"""
        # Classification model for transport mode
        self.mode_classifier = GradientBoostingClassifier(
            n_estimators=100,
            learning_rate=0.1,
            max_depth=5,
            min_samples_split=10,
            min_samples_leaf=5,
            random_state=42
        )
        
        # Regression model for travel time
        self.time_regressor = GradientBoostingRegressor(
            n_estimators=100,
            learning_rate=0.1,
            max_depth=4,
            min_samples_split=10,
            random_state=42
        )
        
        # Regression model for reliability score
        self.reliability_regressor = GradientBoostingRegressor(
            n_estimators=80,
            learning_rate=0.1,
            max_depth=4,
            min_samples_split=10,
            random_state=42
        )
        
        self.scaler = StandardScaler()
        self.label_encoder = LabelEncoder()
        self.label_encoder.fit(self.TRANSPORT_MODES)
        
    def train(
        self,
        features: np.ndarray,
        labels: np.ndarray,
        travel_times: np.ndarray,
        reliability_scores: np.ndarray,
        test_size: float = 0.2
    ) -> dict[str, Any]:
        """
        Train all models on the provided data.
        
        Args:
            features: Feature matrix (n_samples, n_features)
            labels: Transport mode labels (n_samples,)
            travel_times: Actual travel times (n_samples,)
            reliability_scores: Reliability scores 0-1 (n_samples,)
            test_size: Fraction of data to use for testing
            
        Returns:
            Dictionary with training metrics
        """
        logger.info("Starting model training with %d samples", len(features))
        
        self._initialize_models()
        
        # Encode labels
        y_encoded = self.label_encoder.transform(labels)
        
        # Split data
        (X_train, X_test, 
         y_train, y_test,
         time_train, time_test,
         rel_train, rel_test) = train_test_split(
            features, y_encoded, travel_times, reliability_scores,
            test_size=test_size, random_state=42, stratify=y_encoded
        )
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Train mode classifier
        logger.info("Training mode classifier...")
        self.mode_classifier.fit(X_train_scaled, y_train)
        mode_accuracy = self.mode_classifier.score(X_test_scaled, y_test)
        logger.info("Mode classifier accuracy: %.4f", mode_accuracy)
        
        # Train time regressor
        logger.info("Training time regressor...")
        self.time_regressor.fit(X_train_scaled, time_train)
        time_r2 = self.time_regressor.score(X_test_scaled, time_test)
        logger.info("Time regressor R²: %.4f", time_r2)
        
        # Train reliability regressor
        logger.info("Training reliability regressor...")
        self.reliability_regressor.fit(X_train_scaled, rel_train)
        rel_r2 = self.reliability_regressor.score(X_test_scaled, rel_test)
        logger.info("Reliability regressor R²: %.4f", rel_r2)
        
        # Store metrics
        self.training_metrics = {
            "mode_accuracy": float(mode_accuracy),
            "time_r2": float(time_r2),
            "reliability_r2": float(rel_r2),
            "n_training_samples": len(X_train),
            "n_test_samples": len(X_test),
            "trained_at": datetime.utcnow().isoformat(),
            "model_version": self.MODEL_VERSION
        }
        
        self.is_trained = True
        logger.info("Training complete!")
        
        return self.training_metrics
    
    def predict(self, features: TransportFeatures) -> dict[str, Any]:
        """
        Generate recommendation from features.
        
        Args:
            features: TransportFeatures object
            
        Returns:
            Recommendation dictionary with mode, time, reliability, and alternatives
        """
        if not self.is_trained:
            raise RuntimeError("Model not trained. Call train() or load() first.")
        
        # Prepare input
        X = features.to_array().reshape(1, -1)
        X_scaled = self.scaler.transform(X)
        
        # Get predictions
        mode_probs = self.mode_classifier.predict_proba(X_scaled)[0]
        predicted_time = self.time_regressor.predict(X_scaled)[0]
        predicted_reliability = self.reliability_regressor.predict(X_scaled)[0]
        
        # Get ranked modes
        mode_rankings = sorted(
            zip(self.TRANSPORT_MODES, mode_probs),
            key=lambda x: x[1],
            reverse=True
        )
        
        best_mode = mode_rankings[0][0]
        confidence = float(mode_rankings[0][1])
        
        # Build alternatives list
        alternatives = []
        for mode, prob in mode_rankings[1:]:
            if prob > 0.15:  # Only include if reasonable probability
                alternatives.append({
                    "mode": mode,
                    "confidence": float(prob),
                    "estimated_time": self._estimate_mode_time(features, mode)
                })
        
        # Calculate eco-friendliness score
        eco_scores = {"bus": 0.9, "train": 0.95, "car": 0.3}
        
        return {
            "recommended_mode": best_mode,
            "confidence_score": round(confidence, 3),
            "estimated_travel_time": round(max(predicted_time, 5), 1),  # Min 5 minutes
            "reliability_score": round(np.clip(predicted_reliability, 0, 1), 3),
            "eco_score": eco_scores.get(best_mode, 0.5),
            "alternatives": alternatives,
            "mode_probabilities": {
                mode: round(float(prob), 3) 
                for mode, prob in zip(self.TRANSPORT_MODES, mode_probs)
            },
            "generated_at": datetime.utcnow().isoformat(),
            "model_version": self.MODEL_VERSION
        }
    
    def _estimate_mode_time(self, features: TransportFeatures, mode: str) -> float:
        """Estimate travel time for a specific mode"""
        times = {
            "bus": features.bus_time,
            "car": features.car_time,
            "train": features.train_time
        }
        congestions = {
            "bus": features.bus_congestion,
            "car": features.car_congestion,
            "train": features.train_congestion
        }
        
        base_time = times.get(mode, 20)
        congestion_factor = 1 + (congestions.get(mode, 0.5) * 0.5)
        
        return round(base_time * congestion_factor, 1)
    
    def save(self, filename: str = "transport_model") -> str:
        """Save model to disk"""
        if not self.is_trained:
            raise RuntimeError("Cannot save untrained model")
        
        model_path = self.model_dir / f"{filename}.pkl"
        
        model_data = {
            "mode_classifier": self.mode_classifier,
            "time_regressor": self.time_regressor,
            "reliability_regressor": self.reliability_regressor,
            "scaler": self.scaler,
            "label_encoder": self.label_encoder,
            "training_metrics": self.training_metrics,
            "model_version": self.MODEL_VERSION
        }
        
        with open(model_path, "wb") as f:
            pickle.dump(model_data, f)
        
        # Also save metrics as JSON for easy inspection
        metrics_path = self.model_dir / f"{filename}_metrics.json"
        with open(metrics_path, "w") as f:
            json.dump(self.training_metrics, f, indent=2)
        
        logger.info("Model saved to %s", model_path)
        return str(model_path)
    
    def load(self, filename: str = "transport_model") -> None:
        """Load model from disk"""
        model_path = self.model_dir / f"{filename}.pkl"
        
        if not model_path.exists():
            raise FileNotFoundError(f"Model not found at {model_path}")
        
        with open(model_path, "rb") as f:
            model_data = pickle.load(f)
        
        self.mode_classifier = model_data["mode_classifier"]
        self.time_regressor = model_data["time_regressor"]
        self.reliability_regressor = model_data["reliability_regressor"]
        self.scaler = model_data["scaler"]
        self.label_encoder = model_data["label_encoder"]
        self.training_metrics = model_data["training_metrics"]
        self.is_trained = True
        
        logger.info("Model loaded from %s", model_path)
        logger.info("Model metrics: %s", self.training_metrics)
    
    def get_feature_importance(self) -> dict[str, list[tuple[str, float]]]:
        """Get feature importance for interpretability"""
        if not self.is_trained:
            raise RuntimeError("Model not trained")
        
        feature_names = TransportFeatures.feature_names()
        
        return {
            "mode_classifier": sorted(
                zip(feature_names, self.mode_classifier.feature_importances_),
                key=lambda x: x[1], reverse=True
            ),
            "time_regressor": sorted(
                zip(feature_names, self.time_regressor.feature_importances_),
                key=lambda x: x[1], reverse=True
            ),
            "reliability_regressor": sorted(
                zip(feature_names, self.reliability_regressor.feature_importances_),
                key=lambda x: x[1], reverse=True
            )
        }


class MLRecommendationModel:
    """
    Drop-in replacement for the rule-based RecommendationModel.
    Wraps TransportRecommendationModel for compatibility with the existing API.
    """
    
    def __init__(self, model_path: str | None = None):
        self.ml_model = TransportRecommendationModel()
        
        if model_path and Path(model_path).exists():
            self.ml_model.load(Path(model_path).stem)
        else:
            logger.warning("No trained model found. Using fallback rule-based logic.")
    
    def generate_recommendations(self, data: dict[str, Any]) -> dict[str, Any]:
        """
        Generate recommendations from data engine response.
        Compatible with existing RecommendationModel interface.
        """
        # Extract features from the data
        features = self._extract_features(data)
        
        if self.ml_model.is_trained:
            # Use ML model
            prediction = self.ml_model.predict(features)
            
            return {
                "transport_mode": prediction["recommended_mode"],
                "routes": self._get_routes_for_mode(data, prediction["recommended_mode"]),
                "estimated_time": prediction["estimated_travel_time"],
                "alternatives": [alt["mode"] for alt in prediction["alternatives"]],
                "confidence_score": prediction["confidence_score"],
                "reliability_score": prediction["reliability_score"],
                "eco_score": prediction["eco_score"],
                "mode_probabilities": prediction["mode_probabilities"],
                "generated_at": prediction["generated_at"],
                "model_version": prediction["model_version"],
                "ml_powered": True
            }
        else:
            # Fallback to simple rules
            return self._fallback_recommendation(data)
    
    def _extract_features(self, data: dict[str, Any]) -> TransportFeatures:
        """Extract ML features from raw data"""
        now = datetime.utcnow()
        hour = now.hour
        day = now.weekday()
        is_rush = hour in [7, 8, 9, 17, 18, 19]
        
        # Extract transport-specific data
        bus_data = data.get("bus_data") or data.get("data", {})
        car_data = data.get("car_data", {})
        train_data = data.get("train_data", {})
        
        # Handle single transport type response
        transport_type = bus_data.get("transport_type", "")
        if transport_type == "car":
            car_data = bus_data
            bus_data = {}
        elif transport_type == "train":
            train_data = bus_data
            bus_data = {}
        
        return TransportFeatures(
            bus_congestion=bus_data.get("congestion_level", 0.5),
            car_congestion=car_data.get("congestion_level", 0.7),
            train_congestion=train_data.get("congestion_level", 0.3),
            bus_time=bus_data.get("avg_time", 25),
            car_time=car_data.get("avg_time", 20),
            train_time=train_data.get("avg_time", 15),
            bus_cost=self._normalize_cost(bus_data.get("fare", "$2.50")),
            car_cost=self._normalize_cost(car_data.get("estimated_cost", "$5.00")),
            train_cost=self._normalize_cost(train_data.get("fare", "$3.75")),
            bus_availability=0.9 if bus_data.get("available_routes") else 0.5,
            car_availability=self._parking_to_score(car_data.get("parking_availability", "available")),
            train_availability=0.9 if train_data.get("available_routes") else 0.5,
            hour_of_day=hour,
            day_of_week=day,
            is_rush_hour=is_rush,
            weather_score=data.get("weather_score", 0.8),
            distance_km=data.get("distance_km", 10.0)
        )
    
    def _normalize_cost(self, cost_str: str) -> float:
        """Convert cost string to normalized 0-1 value"""
        if isinstance(cost_str, (int, float)):
            return min(cost_str / 10.0, 1.0)
        try:
            cost = float(cost_str.replace("$", "").replace(",", ""))
            return min(cost / 10.0, 1.0)  # Normalize assuming max $10
        except (ValueError, AttributeError):
            return 0.5
    
    def _parking_to_score(self, parking: str) -> float:
        """Convert parking availability to score"""
        mapping = {
            "available": 0.9,
            "limited": 0.5,
            "scarce": 0.2,
            "none": 0.1
        }
        return mapping.get(parking.lower(), 0.5)
    
    def _get_routes_for_mode(self, data: dict[str, Any], mode: str) -> list[str]:
        """Get available routes for a transport mode"""
        mode_data = data.get(f"{mode}_data") or data.get("data", {})
        return mode_data.get("available_routes", [])
    
    def _fallback_recommendation(self, data: dict[str, Any]) -> dict[str, Any]:
        """Simple rule-based fallback when ML model not available"""
        bus_data = data.get("bus_data") or data.get("data", {})
        
        recommendations = {
            "transport_mode": "bus",
            "routes": bus_data.get("available_routes", []),
            "estimated_time": bus_data.get("avg_time", 0),
            "alternatives": [],
            "confidence_score": 0.7,
            "generated_at": datetime.utcnow().isoformat(),
            "ml_powered": False
        }
        
        if bus_data.get("congestion_level", 0) > 0.7:
            recommendations["alternatives"] = ["train", "bicycle"]
        
        return recommendations


# Convenience function for training from command line
def train_from_file(data_file: str, model_output: str = "transport_model") -> dict[str, Any]:
    """Train model from a JSON data file"""
    with open(data_file) as f:
        data = json.load(f)
    
    features = np.array(data["features"])
    labels = np.array(data["labels"])
    travel_times = np.array(data["travel_times"])
    reliability_scores = np.array(data["reliability_scores"])
    
    model = TransportRecommendationModel()
    metrics = model.train(features, labels, travel_times, reliability_scores)
    model.save(model_output)
    
    return metrics


if __name__ == "__main__":
    # Example usage
    print("Transport Recommendation ML Model")
    print("=" * 50)
    print("Use generate_synthetic_data.py to create training data")
    print("Then train with: train_from_file('training_data.json')")
