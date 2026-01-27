"""
Synthetic Data Generator for Transport Recommendation Model

Generates realistic synthetic training data for the transport recommendation ML model.
The data simulates various scenarios with logical relationships between features and outcomes.

Usage:
    python generate_synthetic_data.py --samples 10000 --output training_data.json
    python generate_synthetic_data.py --samples 5000 --output training_data.json --visualize
"""

import argparse
import json
import logging
from dataclasses import asdict, dataclass
from datetime import datetime
from pathlib import Path

import numpy as np

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class SyntheticSample:
    """A single synthetic data sample with all features and labels"""
    
    # Features
    bus_congestion: float
    car_congestion: float
    train_congestion: float
    bus_time: float
    car_time: float
    train_time: float
    bus_cost: float
    car_cost: float
    train_cost: float
    bus_availability: float
    car_availability: float
    train_availability: float
    hour_of_day: int
    day_of_week: int
    is_rush_hour: bool
    weather_score: float
    distance_km: float
    prefer_eco: float
    prefer_comfort: float
    prefer_speed: float
    prefer_cost: float
    
    # Labels
    best_mode: str
    actual_travel_time: float
    reliability_score: float


class TransportDataGenerator:
    """
    Generates synthetic training data with realistic patterns:
    
    1. Rush hour patterns: Higher congestion 7-9am and 5-7pm
    2. Weather effects: Rain increases car preference, decreases bike/walking
    3. Distance effects: Train better for long distances, bus for medium, car for short
    4. Cost sensitivity: Budget-conscious users prefer bus/train
    5. Time sensitivity: Time-pressed users prefer car/train
    6. Weekend patterns: Less congestion, different preferences
    """
    
    TRANSPORT_MODES = ["bus", "car", "train"]
    
    def __init__(self, seed: int = 42):
        self.rng = np.random.default_rng(seed)
        
    def generate_samples(self, n_samples: int) -> list[SyntheticSample]:
        """Generate n synthetic samples with realistic patterns"""
        samples = []
        
        for _ in range(n_samples):
            sample = self._generate_single_sample()
            samples.append(sample)
        
        return samples
    
    def _generate_single_sample(self) -> SyntheticSample:
        """Generate a single realistic sample"""
        
        # Time context
        hour = self.rng.integers(0, 24)
        day = self.rng.integers(0, 7)
        is_weekend = day >= 5
        is_rush_hour = hour in [7, 8, 9, 17, 18, 19] and not is_weekend
        
        # Weather (0=bad, 1=good)
        weather_score = self.rng.beta(5, 2)  # Skewed towards good weather
        is_bad_weather = weather_score < 0.4
        
        # Distance (1-50 km, log-normal distribution centered around 10km)
        distance_km = np.clip(self.rng.lognormal(2.3, 0.7), 1, 50)
        is_long_distance = distance_km > 20
        is_short_distance = distance_km < 5
        
        # Base congestion levels (affected by rush hour and weekend)
        base_congestion = 0.3 if is_weekend else 0.5
        rush_multiplier = 1.5 if is_rush_hour else 1.0
        
        car_congestion = np.clip(
            base_congestion * rush_multiplier * self.rng.uniform(0.8, 1.2) + 
            (0.2 if is_bad_weather else 0),  # More cars in bad weather
            0, 1
        )
        
        bus_congestion = np.clip(
            base_congestion * rush_multiplier * self.rng.uniform(0.7, 1.1),
            0, 1
        )
        
        train_congestion = np.clip(
            (base_congestion * 0.7) * rush_multiplier * self.rng.uniform(0.6, 1.0),
            0, 1
        )
        
        # Travel times (affected by congestion and distance)
        # Base speeds: car=40km/h, bus=25km/h, train=60km/h (including wait times)
        car_time = (distance_km / 40) * 60 * (1 + car_congestion * 0.8) + self.rng.uniform(5, 15)
        bus_time = (distance_km / 25) * 60 * (1 + bus_congestion * 0.5) + self.rng.uniform(5, 20)
        train_time = (distance_km / 60) * 60 + self.rng.uniform(10, 25)  # Fixed schedule
        
        # Costs (normalized 0-1, where 1 = $10)
        bus_cost = np.clip(0.15 + distance_km * 0.01, 0.1, 0.5)  # $1.50 - $5
        car_cost = np.clip(0.30 + distance_km * 0.02 + (0.2 if is_rush_hour else 0), 0.2, 0.8)  # $3 - $8
        train_cost = np.clip(0.25 + distance_km * 0.015, 0.15, 0.6)  # $2.50 - $6
        
        # Availability
        bus_availability = self.rng.uniform(0.7, 1.0) * (0.8 if is_weekend else 1.0)
        car_availability = self.rng.uniform(0.5, 1.0) * (0.7 if is_rush_hour else 1.0)  # Parking
        train_availability = self.rng.uniform(0.8, 1.0) * (0.7 if is_long_distance else 0.9)
        
        # User preferences (random but consistent profiles)
        preference_profile = self.rng.choice(["eco", "speed", "budget", "comfort", "balanced"], 
                                              p=[0.15, 0.25, 0.25, 0.15, 0.20])
        
        if preference_profile == "eco":
            prefer_eco, prefer_comfort, prefer_speed, prefer_cost = 0.9, 0.4, 0.3, 0.5
        elif preference_profile == "speed":
            prefer_eco, prefer_comfort, prefer_speed, prefer_cost = 0.3, 0.5, 0.9, 0.4
        elif preference_profile == "budget":
            prefer_eco, prefer_comfort, prefer_speed, prefer_cost = 0.5, 0.3, 0.4, 0.9
        elif preference_profile == "comfort":
            prefer_eco, prefer_comfort, prefer_speed, prefer_cost = 0.4, 0.9, 0.5, 0.3
        else:  # balanced
            prefer_eco, prefer_comfort, prefer_speed, prefer_cost = 0.5, 0.5, 0.5, 0.5
        
        # Add noise to preferences
        prefer_eco = np.clip(prefer_eco + self.rng.normal(0, 0.1), 0, 1)
        prefer_comfort = np.clip(prefer_comfort + self.rng.normal(0, 0.1), 0, 1)
        prefer_speed = np.clip(prefer_speed + self.rng.normal(0, 0.1), 0, 1)
        prefer_cost = np.clip(prefer_cost + self.rng.normal(0, 0.1), 0, 1)
        
        # Determine best mode based on utility function
        best_mode, utility_scores = self._calculate_best_mode(
            bus_congestion, car_congestion, train_congestion,
            bus_time, car_time, train_time,
            bus_cost, car_cost, train_cost,
            bus_availability, car_availability, train_availability,
            is_bad_weather, is_long_distance, is_short_distance,
            prefer_eco, prefer_comfort, prefer_speed, prefer_cost
        )
        
        # Calculate actual travel time (with realistic noise)
        mode_times = {"bus": bus_time, "car": car_time, "train": train_time}
        actual_travel_time = mode_times[best_mode] * self.rng.uniform(0.9, 1.15)
        
        # Calculate reliability score
        reliability_score = self._calculate_reliability(
            best_mode, 
            {"bus": bus_congestion, "car": car_congestion, "train": train_congestion},
            weather_score,
            is_rush_hour
        )
        
        return SyntheticSample(
            bus_congestion=round(bus_congestion, 3),
            car_congestion=round(car_congestion, 3),
            train_congestion=round(train_congestion, 3),
            bus_time=round(bus_time, 1),
            car_time=round(car_time, 1),
            train_time=round(train_time, 1),
            bus_cost=round(bus_cost, 3),
            car_cost=round(car_cost, 3),
            train_cost=round(train_cost, 3),
            bus_availability=round(bus_availability, 3),
            car_availability=round(car_availability, 3),
            train_availability=round(train_availability, 3),
            hour_of_day=hour,
            day_of_week=day,
            is_rush_hour=is_rush_hour,
            weather_score=round(weather_score, 3),
            distance_km=round(distance_km, 2),
            prefer_eco=round(prefer_eco, 3),
            prefer_comfort=round(prefer_comfort, 3),
            prefer_speed=round(prefer_speed, 3),
            prefer_cost=round(prefer_cost, 3),
            best_mode=best_mode,
            actual_travel_time=round(actual_travel_time, 1),
            reliability_score=round(reliability_score, 3)
        )
    
    def _calculate_best_mode(
        self,
        bus_cong, car_cong, train_cong,
        bus_time, car_time, train_time,
        bus_cost, car_cost, train_cost,
        bus_avail, car_avail, train_avail,
        is_bad_weather, is_long_distance, is_short_distance,
        prefer_eco, prefer_comfort, prefer_speed, prefer_cost
    ) -> tuple[str, dict[str, float]]:
        """Calculate best transport mode using weighted utility function"""
        
        # Eco scores (train best, then bus, then car)
        eco_scores = {"bus": 0.75, "car": 0.3, "train": 0.9}
        
        # Comfort scores (car best in bad weather and short trips)
        comfort_scores = {
            "bus": 0.45,
            "car": 0.85 + (0.1 if is_bad_weather else 0) + (0.1 if is_short_distance else 0),
            "train": 0.65 + (0.15 if is_long_distance else 0)
        }
        
        # Normalize times (lower is better) - give more weight to actual time differences
        min_time = min(bus_time, car_time, train_time)
        time_scores = {
            "bus": min_time / bus_time if bus_time > 0 else 0.5,
            "car": min_time / car_time if car_time > 0 else 0.5,
            "train": min_time / train_time if train_time > 0 else 0.5
        }
        
        # Normalize costs (lower is better)
        min_cost = min(bus_cost, car_cost, train_cost)
        cost_scores = {
            "bus": min_cost / bus_cost if bus_cost > 0 else 0.5,
            "car": min_cost / car_cost if car_cost > 0 else 0.5,
            "train": min_cost / train_cost if train_cost > 0 else 0.5
        }
        
        # Availability scores (directly used)
        avail_scores = {
            "bus": bus_avail,
            "car": car_avail,
            "train": train_avail
        }
        
        # Calculate weighted utilities with balanced weights
        utilities = {}
        for mode in self.TRANSPORT_MODES:
            # Base utility from preferences
            utility = (
                prefer_eco * eco_scores[mode] * 0.8 +
                prefer_comfort * comfort_scores[mode] * 1.0 +
                prefer_speed * time_scores[mode] * 1.2 +
                prefer_cost * cost_scores[mode] * 1.0
            )
            
            # Apply availability as a multiplier
            utility *= (0.5 + 0.5 * avail_scores[mode])
            
            # Distance modifiers (more nuanced)
            if is_long_distance:
                if mode == "train":
                    utility *= 1.15
                elif mode == "car":
                    utility *= 0.85
                elif mode == "bus":
                    utility *= 0.95
            elif is_short_distance:
                if mode == "car":
                    utility *= 1.25  # Car excels for short trips
                elif mode == "train":
                    utility *= 0.8   # Train has overhead for short trips
                elif mode == "bus":
                    utility *= 1.1
            
            # Weather modifier - car becomes much more attractive in bad weather
            if is_bad_weather:
                if mode == "car":
                    utility *= 1.3
                elif mode == "bus":
                    utility *= 0.9
            
            # Congestion penalty (affects car most)
            congestion = {"bus": bus_cong, "car": car_cong, "train": train_cong}
            if mode == "car" and congestion[mode] > 0.7:
                utility *= 0.85
            elif mode == "bus" and congestion[mode] > 0.8:
                utility *= 0.9
            
            # Add random noise for variety
            utility *= self.rng.uniform(0.9, 1.1)
            
            utilities[mode] = utility
        
        best_mode = max(utilities, key=utilities.get)
        return best_mode, utilities
    
    def _calculate_reliability(
        self,
        mode: str,
        congestion: dict[str, float],
        weather_score: float,
        is_rush_hour: bool
    ) -> float:
        """Calculate reliability score for a transport mode"""
        
        # Base reliability
        base_reliability = {"bus": 0.75, "car": 0.85, "train": 0.90}
        
        reliability = base_reliability[mode]
        
        # Congestion penalty
        reliability -= congestion[mode] * 0.2
        
        # Weather penalty (affects car most)
        if mode == "car":
            reliability -= (1 - weather_score) * 0.15
        elif mode == "bus":
            reliability -= (1 - weather_score) * 0.1
        
        # Rush hour penalty
        if is_rush_hour:
            reliability -= 0.1 if mode == "car" else 0.05
        
        # Add noise
        reliability += self.rng.normal(0, 0.05)
        
        return np.clip(reliability, 0.3, 0.99)
    
    def to_training_format(self, samples: list[SyntheticSample]) -> dict:
        """Convert samples to training data format"""
        
        features = []
        labels = []
        travel_times = []
        reliability_scores = []
        
        for sample in samples:
            # Extract features in the correct order
            feature_vector = [
                sample.bus_congestion,
                sample.car_congestion,
                sample.train_congestion,
                sample.bus_time,
                sample.car_time,
                sample.train_time,
                sample.bus_cost,
                sample.car_cost,
                sample.train_cost,
                sample.bus_availability,
                sample.car_availability,
                sample.train_availability,
                sample.hour_of_day / 23.0,
                sample.day_of_week / 6.0,
                float(sample.is_rush_hour),
                sample.weather_score,
                min(sample.distance_km / 50.0, 1.0),
                sample.prefer_eco,
                sample.prefer_comfort,
                sample.prefer_speed,
                sample.prefer_cost,
            ]
            
            features.append(feature_vector)
            labels.append(sample.best_mode)
            travel_times.append(sample.actual_travel_time)
            reliability_scores.append(sample.reliability_score)
        
        return {
            "features": features,
            "labels": labels,
            "travel_times": travel_times,
            "reliability_scores": reliability_scores,
            "feature_names": [
                "bus_congestion", "car_congestion", "train_congestion",
                "bus_time", "car_time", "train_time",
                "bus_cost", "car_cost", "train_cost",
                "bus_availability", "car_availability", "train_availability",
                "hour_normalized", "day_normalized", "is_rush_hour",
                "weather_score", "distance_normalized",
                "prefer_eco", "prefer_comfort", "prefer_speed", "prefer_cost"
            ],
            "metadata": {
                "n_samples": len(samples),
                "generated_at": datetime.utcnow().isoformat(),
                "label_distribution": {
                    mode: labels.count(mode) for mode in self.TRANSPORT_MODES
                }
            }
        }
    
    def to_raw_format(self, samples: list[SyntheticSample]) -> list[dict]:
        """Convert samples to raw dictionary format"""
        return [asdict(sample) for sample in samples]


def analyze_data(samples: list[SyntheticSample]) -> dict:
    """Analyze the generated data distribution"""
    
    labels = [s.best_mode for s in samples]
    
    stats = {
        "total_samples": len(samples),
        "label_distribution": {
            mode: labels.count(mode) / len(labels) * 100 
            for mode in ["bus", "car", "train"]
        },
        "feature_stats": {}
    }
    
    # Calculate feature statistics
    numeric_features = [
        "bus_congestion", "car_congestion", "train_congestion",
        "bus_time", "car_time", "train_time",
        "distance_km", "weather_score", "reliability_score"
    ]
    
    for feature in numeric_features:
        values = [getattr(s, feature) for s in samples]
        stats["feature_stats"][feature] = {
            "mean": round(np.mean(values), 3),
            "std": round(np.std(values), 3),
            "min": round(np.min(values), 3),
            "max": round(np.max(values), 3)
        }
    
    # Rush hour distribution
    rush_hour_samples = [s for s in samples if s.is_rush_hour]
    stats["rush_hour_percentage"] = round(len(rush_hour_samples) / len(samples) * 100, 1)
    
    return stats


def generate_visualization(samples: list[SyntheticSample], output_dir: str) -> None:
    """Generate visualization plots of the data"""
    try:
        import matplotlib.pyplot as plt
        
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        fig, axes = plt.subplots(2, 3, figsize=(15, 10))
        
        # 1. Label distribution
        labels = [s.best_mode for s in samples]
        label_counts = {mode: labels.count(mode) for mode in ["bus", "car", "train"]}
        axes[0, 0].bar(label_counts.keys(), label_counts.values(), color=['blue', 'green', 'red'])
        axes[0, 0].set_title("Transport Mode Distribution")
        axes[0, 0].set_ylabel("Count")
        
        # 2. Congestion by mode
        congestions = {
            "bus": [s.bus_congestion for s in samples],
            "car": [s.car_congestion for s in samples],
            "train": [s.train_congestion for s in samples]
        }
        axes[0, 1].boxplot(congestions.values(), labels=congestions.keys())
        axes[0, 1].set_title("Congestion Levels by Mode")
        axes[0, 1].set_ylabel("Congestion (0-1)")
        
        # 3. Travel time distribution
        times = [s.actual_travel_time for s in samples]
        axes[0, 2].hist(times, bins=50, color='purple', alpha=0.7)
        axes[0, 2].set_title("Travel Time Distribution")
        axes[0, 2].set_xlabel("Minutes")
        axes[0, 2].set_ylabel("Count")
        
        # 4. Distance vs Time scatter
        distances = [s.distance_km for s in samples]
        colors = {'bus': 'blue', 'car': 'green', 'train': 'red'}
        for mode in ["bus", "car", "train"]:
            mode_samples = [s for s in samples if s.best_mode == mode]
            axes[1, 0].scatter(
                [s.distance_km for s in mode_samples],
                [s.actual_travel_time for s in mode_samples],
                c=colors[mode], alpha=0.3, label=mode, s=10
            )
        axes[1, 0].set_title("Distance vs Travel Time")
        axes[1, 0].set_xlabel("Distance (km)")
        axes[1, 0].set_ylabel("Travel Time (min)")
        axes[1, 0].legend()
        
        # 5. Hour of day distribution
        hours = [s.hour_of_day for s in samples]
        rush_colors = ['red' if h in [7, 8, 9, 17, 18, 19] else 'blue' for h in range(24)]
        hour_counts = [hours.count(h) for h in range(24)]
        axes[1, 1].bar(range(24), hour_counts, color=rush_colors)
        axes[1, 1].set_title("Hour of Day Distribution (Red=Rush Hour)")
        axes[1, 1].set_xlabel("Hour")
        axes[1, 1].set_ylabel("Count")
        
        # 6. Reliability score distribution
        reliabilities = [s.reliability_score for s in samples]
        axes[1, 2].hist(reliabilities, bins=30, color='orange', alpha=0.7)
        axes[1, 2].set_title("Reliability Score Distribution")
        axes[1, 2].set_xlabel("Reliability (0-1)")
        axes[1, 2].set_ylabel("Count")
        
        plt.tight_layout()
        plt.savefig(output_path / "data_visualization.png", dpi=150)
        plt.close()
        
        logger.info("Visualization saved to %s", output_path / "data_visualization.png")
        
    except ImportError:
        logger.warning("matplotlib not installed. Skipping visualization.")


def main():
    parser = argparse.ArgumentParser(
        description="Generate synthetic training data for transport recommendation model"
    )
    parser.add_argument(
        "--samples", "-n", type=int, default=10000,
        help="Number of samples to generate (default: 10000)"
    )
    parser.add_argument(
        "--output", "-o", type=str, default="training_data.json",
        help="Output file path (default: training_data.json)"
    )
    parser.add_argument(
        "--raw-output", type=str, default=None,
        help="Optional: Also save raw samples to this file"
    )
    parser.add_argument(
        "--seed", "-s", type=int, default=42,
        help="Random seed for reproducibility (default: 42)"
    )
    parser.add_argument(
        "--visualize", "-v", action="store_true",
        help="Generate visualization plots"
    )
    parser.add_argument(
        "--output-dir", type=str, default=".",
        help="Output directory for visualizations (default: current directory)"
    )
    
    args = parser.parse_args()
    
    logger.info("Generating %d synthetic samples...", args.samples)
    
    generator = TransportDataGenerator(seed=args.seed)
    samples = generator.generate_samples(args.samples)
    
    # Analyze data
    stats = analyze_data(samples)
    logger.info("\n📊 Data Statistics:")
    logger.info("  Total samples: %d", stats["total_samples"])
    logger.info("  Label distribution:")
    for mode, pct in stats["label_distribution"].items():
        logger.info("    %s: %.1f%%", mode, pct)
    logger.info("  Rush hour samples: %.1f%%", stats["rush_hour_percentage"])
    
    # Save training format
    training_data = generator.to_training_format(samples)
    with open(args.output, "w") as f:
        json.dump(training_data, f, indent=2)
    logger.info("\n✅ Training data saved to: %s", args.output)
    
    # Optionally save raw format
    if args.raw_output:
        raw_data = generator.to_raw_format(samples)
        with open(args.raw_output, "w") as f:
            json.dump(raw_data, f, indent=2)
        logger.info("✅ Raw data saved to: %s", args.raw_output)
    
    # Generate visualizations
    if args.visualize:
        generate_visualization(samples, args.output_dir)
    
    logger.info("\n🎉 Data generation complete!")
    logger.info("Next steps:")
    logger.info("  1. Review the data in %s", args.output)
    logger.info("  2. Train the model with:")
    logger.info("     from ml_model import train_from_file")
    logger.info("     train_from_file('%s')", args.output)


if __name__ == "__main__":
    main()
