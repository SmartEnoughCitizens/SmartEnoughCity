"""
Train Transport Recommendation Model

This script demonstrates the complete ML workflow:
1. Generate synthetic training data
2. Train the model
3. Evaluate performance
4. Save the trained model

Usage:
    python train_model.py
    python train_model.py --samples 20000 --visualize
"""

import argparse
import json
import logging
from pathlib import Path

import numpy as np

from generate_synthetic_data import TransportDataGenerator, analyze_data
from ml_model import TransportRecommendationModel, TransportFeatures

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)


def train_and_evaluate(n_samples: int = 10000, model_name: str = "transport_model") -> dict:
    """
    Complete training pipeline:
    1. Generate data
    2. Train model
    3. Evaluate
    4. Save
    """
    
    logger.info("=" * 60)
    logger.info("🚀 TRANSPORT RECOMMENDATION MODEL TRAINING")
    logger.info("=" * 60)
    
    # Step 1: Generate synthetic data
    logger.info("\n📦 Step 1: Generating synthetic training data...")
    generator = TransportDataGenerator(seed=42)
    samples = generator.generate_samples(n_samples)
    training_data = generator.to_training_format(samples)
    
    stats = analyze_data(samples)
    logger.info("  Generated %d samples", stats["total_samples"])
    logger.info("  Label distribution: %s", 
                {k: f"{v:.1f}%" for k, v in stats["label_distribution"].items()})
    
    # Step 2: Prepare data
    logger.info("\n🔧 Step 2: Preparing training data...")
    features = np.array(training_data["features"])
    labels = np.array(training_data["labels"])
    travel_times = np.array(training_data["travel_times"])
    reliability_scores = np.array(training_data["reliability_scores"])
    
    logger.info("  Feature matrix shape: %s", features.shape)
    logger.info("  Labels shape: %s", labels.shape)
    
    # Step 3: Train model
    logger.info("\n🎯 Step 3: Training model...")
    model = TransportRecommendationModel(model_dir="models")
    metrics = model.train(features, labels, travel_times, reliability_scores)
    
    logger.info("\n📊 Training Metrics:")
    logger.info("  Mode Classification Accuracy: %.2f%%", metrics["mode_accuracy"] * 100)
    logger.info("  Time Prediction R²: %.4f", metrics["time_r2"])
    logger.info("  Reliability Prediction R²: %.4f", metrics["reliability_r2"])
    
    # Step 4: Feature importance
    logger.info("\n🔍 Step 4: Analyzing feature importance...")
    importance = model.get_feature_importance()
    
    logger.info("\n  Top 5 features for mode classification:")
    for name, score in importance["mode_classifier"][:5]:
        logger.info("    %s: %.4f", name, score)
    
    # Step 5: Test predictions
    logger.info("\n🧪 Step 5: Testing predictions...")
    
    # Test case 1: Rush hour, long distance
    test_features_1 = TransportFeatures(
        bus_congestion=0.7,
        car_congestion=0.9,
        train_congestion=0.4,
        bus_time=45,
        car_time=35,
        train_time=25,
        bus_cost=0.3,
        car_cost=0.6,
        train_cost=0.4,
        bus_availability=0.8,
        car_availability=0.4,
        train_availability=0.9,
        hour_of_day=8,
        day_of_week=1,
        is_rush_hour=True,
        weather_score=0.9,
        distance_km=25.0,
        prefer_eco=0.5,
        prefer_comfort=0.5,
        prefer_speed=0.7,
        prefer_cost=0.5
    )
    
    pred_1 = model.predict(test_features_1)
    logger.info("\n  Test 1: Rush hour, long distance, speed preference")
    logger.info("    Recommended: %s (confidence: %.1f%%)", 
                pred_1["recommended_mode"], pred_1["confidence_score"] * 100)
    logger.info("    Estimated time: %.1f min", pred_1["estimated_travel_time"])
    logger.info("    Reliability: %.2f", pred_1["reliability_score"])
    
    # Test case 2: Weekend, short distance, eco preference
    test_features_2 = TransportFeatures(
        bus_congestion=0.3,
        car_congestion=0.4,
        train_congestion=0.2,
        bus_time=15,
        car_time=10,
        train_time=20,
        bus_cost=0.15,
        car_cost=0.35,
        train_cost=0.25,
        bus_availability=0.9,
        car_availability=0.8,
        train_availability=0.7,
        hour_of_day=11,
        day_of_week=6,
        is_rush_hour=False,
        weather_score=0.95,
        distance_km=5.0,
        prefer_eco=0.9,
        prefer_comfort=0.3,
        prefer_speed=0.4,
        prefer_cost=0.6
    )
    
    pred_2 = model.predict(test_features_2)
    logger.info("\n  Test 2: Weekend, short distance, eco preference")
    logger.info("    Recommended: %s (confidence: %.1f%%)", 
                pred_2["recommended_mode"], pred_2["confidence_score"] * 100)
    logger.info("    Eco score: %.2f", pred_2["eco_score"])
    
    # Test case 3: Bad weather, budget conscious
    test_features_3 = TransportFeatures(
        bus_congestion=0.5,
        car_congestion=0.7,
        train_congestion=0.3,
        bus_time=30,
        car_time=22,
        train_time=18,
        bus_cost=0.2,
        car_cost=0.55,
        train_cost=0.35,
        bus_availability=0.85,
        car_availability=0.6,
        train_availability=0.9,
        hour_of_day=14,
        day_of_week=3,
        is_rush_hour=False,
        weather_score=0.25,  # Bad weather
        distance_km=12.0,
        prefer_eco=0.4,
        prefer_comfort=0.6,
        prefer_speed=0.5,
        prefer_cost=0.9  # Budget conscious
    )
    
    pred_3 = model.predict(test_features_3)
    logger.info("\n  Test 3: Bad weather, budget conscious")
    logger.info("    Recommended: %s (confidence: %.1f%%)", 
                pred_3["recommended_mode"], pred_3["confidence_score"] * 100)
    logger.info("    Mode probabilities: %s", pred_3["mode_probabilities"])
    
    # Step 6: Save model
    logger.info("\n💾 Step 6: Saving model...")
    model_path = model.save(model_name)
    logger.info("  Model saved to: %s", model_path)
    
    # Also save training data for reference
    data_path = Path("models") / "training_data.json"
    with open(data_path, "w") as f:
        json.dump(training_data, f)
    logger.info("  Training data saved to: %s", data_path)
    
    logger.info("\n" + "=" * 60)
    logger.info("✅ TRAINING COMPLETE!")
    logger.info("=" * 60)
    
    return {
        "metrics": metrics,
        "model_path": model_path,
        "data_stats": stats,
        "test_predictions": [pred_1, pred_2, pred_3]
    }


def main():
    parser = argparse.ArgumentParser(description="Train transport recommendation model")
    parser.add_argument(
        "--samples", "-n", type=int, default=10000,
        help="Number of training samples (default: 10000)"
    )
    parser.add_argument(
        "--model-name", "-m", type=str, default="transport_model",
        help="Name for the saved model (default: transport_model)"
    )
    parser.add_argument(
        "--visualize", "-v", action="store_true",
        help="Generate visualizations"
    )
    
    args = parser.parse_args()
    
    results = train_and_evaluate(args.samples, args.model_name)
    
    if args.visualize:
        try:
            from generate_synthetic_data import generate_visualization, TransportDataGenerator
            generator = TransportDataGenerator(seed=42)
            samples = generator.generate_samples(args.samples)
            generate_visualization(samples, "models")
            logger.info("📊 Visualizations saved to models/")
        except ImportError:
            logger.warning("matplotlib not available for visualization")
    
    # Print summary
    print("\n" + "=" * 60)
    print("📋 SUMMARY")
    print("=" * 60)
    print(f"Model Performance:")
    print(f"  - Classification Accuracy: {results['metrics']['mode_accuracy']*100:.1f}%")
    print(f"  - Time Prediction R²: {results['metrics']['time_r2']:.3f}")
    print(f"  - Reliability R²: {results['metrics']['reliability_r2']:.3f}")
    print(f"\nModel saved to: {results['model_path']}")
    print("\nTo use in your application:")
    print("  from ml_model import MLRecommendationModel")
    print("  model = MLRecommendationModel('models/transport_model.pkl')")
    print("  result = model.generate_recommendations(data)")


if __name__ == "__main__":
    main()
