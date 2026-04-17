import json
import logging
import re
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.linear_model import LinearRegression
from sqlalchemy import text

from inference_engine.db import engine

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def load_stop_times_with_stops() -> pd.DataFrame:
    """
    Load train stop times joined with stop names from the database.

    Performs an inner join between train_stop_times and train_stops on stop_id.
    The headsign column is stop-level and will contain NULLs for most rows —
    these are filled in a later processing step.

    Returns:
        DataFrame with columns: entry_id, stop_id, headsign, name
    """
    query = text("""
        SELECT
            tst.entry_id,
            tst.stop_id,
            tst.headsign,
            ts.name
        FROM external_data.train_stop_times tst
        INNER JOIN external_data.train_stops ts ON tst.stop_id = ts.id
    """)

    logger.info("Loading train stop times with stop names...")
    with engine.connect() as conn:
        df = pd.read_sql(query, conn)
    logger.info("  Loaded %d rows into stop_times dataframe.", len(df))
    return df


def load_train_station_ridership() -> pd.DataFrame:
    """
    Load historical train station ridership data from the database.

    Returns:
        DataFrame with columns: id, station, count_2014 ... count_2024 (no count_2020)
    """
    query = text("SELECT * FROM external_data.train_station_ridership")

    logger.info("Loading train station ridership data...")
    with engine.connect() as conn:
        df = pd.read_sql(query, conn)
    logger.info("  Loaded %d rows into ridership dataframe.", len(df))
    return df


def process_stop_times(
    stop_times_df: pd.DataFrame,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """
    Process the stop times dataframe by filling null headsigns and extracting
    unique headsign/stop combinations.

    Null headsign values are forward-filled from the nearest preceding non-null
    value, since nulls represent subsequent stops of the same train headsign.

    Args:
        stop_times_df: Raw dataframe from load_stop_times_with_stops()

    Returns:
        Tuple of:
        - filled_df: Full dataframe with headsign nulls forward-filled
        - unique_combinations_df: Unique (headsign, name) pairs
    """
    filled_df = stop_times_df.copy()
    filled_df["headsign"] = filled_df["headsign"].ffill()
    logger.info(
        "  Forward-filled headsign nulls. Remaining nulls: %d",
        filled_df["headsign"].isna().sum(),
    )

    unique_combinations_df = (
        filled_df[["headsign", "name"]].drop_duplicates().reset_index(drop=True)
    )
    logger.info(
        "  Found %d unique headsign/stop combinations.", len(unique_combinations_df)
    )

    return filled_df, unique_combinations_df


def distribute_ridership(
    unique_combinations_df: pd.DataFrame,
    ridership_df: pd.DataFrame,
) -> pd.DataFrame:
    """
    Distribute 2024 station ridership equally across all headsigns serving each station.

    For each station, the count_2024 value is divided equally among all headsigns
    that stop there.

    Args:
        unique_combinations_df: Unique (headsign, name) pairs from process_stop_times()
        ridership_df: Ridership dataframe from load_train_station_ridership()

    Returns:
        DataFrame with columns: headsign, name, count_2024, headsign_count, distributed_count
    """
    headsigns_per_station = (
        unique_combinations_df.groupby("name")["headsign"]
        .count()
        .reset_index(name="headsign_count")
    )

    result_df = unique_combinations_df.merge(headsigns_per_station, on="name")

    result_df = result_df.merge(
        ridership_df[["station", "count_2024"]],
        left_on="name",
        right_on="station",
        how="left",
    ).drop(columns=["station"])

    result_df["distributed_count"] = (
        result_df["count_2024"] / result_df["headsign_count"]
    )

    logger.info(
        "  Distributed ridership across %d headsign/stop combinations.", len(result_df)
    )
    return result_df


_TIER_1_PATTERNS = [
    r"tara",
    r"pearse",
    r"connolly",
    r"dun laoghaire",
    r"bray",
    r"blackrock",
]

_TIER_2_PATTERNS = [
    r"greystones",
    r"shankill",
    r"grand canal",
    r"booterstown",
    r"sydney",
    r"glenageary",
    r"dalkey",
    r"lansdowne",
    r"salthill",
]


def _get_tier_weight(headsign: str) -> int:
    """Return the tier weight (3, 2, or 1) for a headsign based on its destination station."""
    headsign_lower = headsign.lower()
    if any(re.search(p, headsign_lower) for p in _TIER_1_PATTERNS):
        return 3
    if any(re.search(p, headsign_lower) for p in _TIER_2_PATTERNS):
        return 2
    return 1


def distribute_ridership_weighted(
    unique_combinations_df: pd.DataFrame,
    ridership_df: pd.DataFrame,
    predicted_df: pd.DataFrame,
) -> pd.DataFrame:
    """
    Distribute 2024 station ridership and predicted 2025 ridership across headsigns
    using tier-based weights.

    For each station, counts are distributed proportionally based on the tier weight
    of each headsign (destination). Tier 1 headsigns receive the highest share,
    Tier 2 medium, Tier 3 the lowest. Distributed counts sum to the original station
    count for each station.

    Tier weights: Tier 1 = 3, Tier 2 = 2, Tier 3 = 1

    Args:
        unique_combinations_df: Unique (headsign, name) pairs from process_stop_times()
        ridership_df: Ridership dataframe from load_train_station_ridership()
        predicted_df: Predicted 2025 ridership from predict_ridership_2025()

    Returns:
        DataFrame with columns: headsign, tier_weight, name, count_2024,
                                 total_weight, weighted_distributed_count, predicted_count
    """
    result_df = unique_combinations_df.copy()
    result_df["tier_weight"] = result_df["headsign"].apply(_get_tier_weight)

    total_weight_per_station = (
        result_df.groupby("name")["tier_weight"].sum().reset_index(name="total_weight")
    )
    result_df = result_df.merge(total_weight_per_station, on="name")

    result_df = result_df.merge(
        ridership_df[["station", "count_2024"]],
        left_on="name",
        right_on="station",
        how="left",
    ).drop(columns=["station"])

    result_df = result_df.merge(
        predicted_df,
        left_on="name",
        right_on="station",
        how="left",
    ).drop(columns=["station"])

    result_df["weighted_distributed_count"] = (
        result_df["tier_weight"] / result_df["total_weight"] * result_df["count_2024"]
    )

    result_df["predicted_count"] = (
        result_df["tier_weight"]
        / result_df["total_weight"]
        * result_df["predicted_2025"]
    )

    logger.info(
        "  Weighted distribution complete across %d headsign/stop combinations.",
        len(result_df),
    )
    return result_df


_RIDERSHIP_YEARS = [2022, 2023, 2024]


def predict_ridership_2025(ridership_df: pd.DataFrame) -> pd.DataFrame:
    """
    Predict 2025 ridership per station using a per-station linear regression.

    Fits a line through the 10 available year columns (2014-2024, no 2020)
    and extrapolates to 2025. Negative predictions are clipped to 0.
    Stations with a NaN in any of the fit-window years are skipped; they
    will appear as NaN after the downstream left join and pandas' groupby
    sum will drop them cleanly.

    Args:
        ridership_df: Ridership dataframe from load_train_station_ridership()

    Returns:
        DataFrame with columns: station, predicted_2025
    """
    year_cols = [f"count_{y}" for y in _RIDERSHIP_YEARS]
    x = np.array(_RIDERSHIP_YEARS, dtype=float).reshape(-1, 1)
    x_2025 = np.array([[2025.0]])

    usable_df = ridership_df.dropna(subset=year_cols)
    skipped = len(ridership_df) - len(usable_df)
    if skipped:
        logger.info(
            "  Skipping %d station(s) with missing ridership in %s.",
            skipped,
            year_cols,
        )

    model = LinearRegression()
    predictions = []
    for _, row in usable_df.iterrows():
        y = np.array([row[col] for col in year_cols], dtype=float)
        model.fit(x, y)
        predicted = model.predict(x_2025)[0]
        predictions.append(
            {
                "station": row["station"],
                "predicted_2025": max(0.0, predicted),
            }
        )

    predicted_df = pd.DataFrame(predictions)
    logger.info("  Predicted 2025 ridership for %d stations.", len(predicted_df))
    return predicted_df


_TRAIN_CAPACITY = 400
_TRAINS_PER_HEADSIGN = 5
_TOTAL_CAPACITY = _TRAIN_CAPACITY * _TRAINS_PER_HEADSIGN  # 1200

_OVER_UTILISED_THRESHOLD = 0.95  # > 80% of capacity
_UNDER_UTILISED_THRESHOLD = 0.30  # < 20% of capacity


def _classify_utilisation(ridership: float) -> str:
    """Classify a headsign as over-utilised, under-utilised, or normal."""
    ratio = ridership / _TOTAL_CAPACITY
    if ratio > _OVER_UTILISED_THRESHOLD:
        return "over-utilised"
    if ratio < _UNDER_UTILISED_THRESHOLD:
        return "under-utilised"
    return "normal"


def compute_utilisation(result_df: pd.DataFrame) -> pd.DataFrame:
    """
    Classify each headsign as over-utilised, under-utilised, or normal
    based on total 2024 ridership and predicted 2025 ridership vs capacity.

    Total capacity per headsign = 400 seats x 3 trains = 1200.
    Over-utilised: total boarding > 80% of capacity (> 960).
    Under-utilised: total boarding < 20% of capacity (< 240).

    Args:
        result_df: Output of distribute_ridership_weighted()

    Returns:
        DataFrame with columns: headsign, total_2024, utilisation_2024,
                                 total_predicted_2025, utilisation_2025
    """
    actual = (
        result_df.groupby("headsign")["weighted_distributed_count"]
        .sum()
        .reset_index(name="total_2024")
    )

    predicted = (
        result_df.groupby("headsign")["predicted_count"]
        .sum()
        .reset_index(name="total_predicted_2025")
    )

    utilisation_df = actual.merge(predicted, on="headsign")
    utilisation_df["utilisation_2024"] = utilisation_df["total_2024"].apply(
        _classify_utilisation
    )
    utilisation_df["utilisation_2025"] = utilisation_df["total_predicted_2025"].apply(
        _classify_utilisation
    )

    logger.info(
        "  Utilisation 2024 — over: %d, under: %d, normal: %d",
        (utilisation_df["utilisation_2024"] == "over-utilised").sum(),
        (utilisation_df["utilisation_2024"] == "under-utilised").sum(),
        (utilisation_df["utilisation_2024"] == "normal").sum(),
    )
    logger.info(
        "  Utilisation 2025 — over: %d, under: %d, normal: %d",
        (utilisation_df["utilisation_2025"] == "over-utilised").sum(),
        (utilisation_df["utilisation_2025"] == "under-utilised").sum(),
        (utilisation_df["utilisation_2025"] == "normal").sum(),
    )
    return utilisation_df


def _get_recommendation(status: str, total_2024: float) -> str:
    """Return a recommendation string based on utilisation status and ridership."""
    if status == "over-utilised":
        if total_2024 > _TOTAL_CAPACITY * 1.10:
            return "Add 2 Trains"
        return "Add 1 Train"
    return "Reduce by 1 Train and observe for impacts"


def build_utilisation_json(utilisation_df: pd.DataFrame) -> list[dict]:
    """
    Build a JSON-serialisable list of the top 3 over-utilised and top 3
    under-utilised headsigns based on 2024 ridership.

    Recommendation logic:
    - Over-utilised, total_2024 > 110% capacity (>1320): "Add 2 Trains"
    - Over-utilised, total_2024 80-110% capacity (960-1320): "Add 1 Train"
    - Under-utilised: "Reduce by 1 Train and observe for impacts"

    Args:
        utilisation_df: Output of compute_utilisation()

    Returns:
        List of dicts with Train Name and Attributes (Current_count,
        Predicted_count, status, Recommendation)
    """
    over = utilisation_df[
        utilisation_df["utilisation_2024"] == "over-utilised"
    ].nlargest(3, "total_2024")
    under = utilisation_df[
        utilisation_df["utilisation_2024"] == "under-utilised"
    ].nsmallest(3, "total_2024")

    top_trains = pd.concat([over, under], ignore_index=True)

    result = [
        {
            "Train Name": row["headsign"],
            "Attributes": {
                "Current_count": round(row["total_2024"], 2),
                "Predicted_count": round(row["total_predicted_2025"], 2),
                "status": row["utilisation_2024"],
                "Recommendation": _get_recommendation(
                    row["utilisation_2024"], row["total_2024"]
                ),
            },
        }
        for _, row in top_trains.iterrows()
    ]

    logger.info("  Built utilisation JSON for %d headsigns.", len(result))
    return result


def save_recommendation_to_db(utilisation_json: list[dict]) -> None:
    """
    Insert the recommendation JSON into the recommendations table.

    Skips the insert if an identical recommendation already exists in the table
    for the same indicator and usecase.

    Args:
        utilisation_json: Output of build_utilisation_json()
    """
    recommendation_str = json.dumps(utilisation_json)

    check_query = text("""
        SELECT 1 FROM backend.recommendations
        WHERE indicator = :indicator
          AND usecase = :usecase
          AND CAST(recommendation AS text) = CAST(CAST(:recommendation AS jsonb) AS text)
        LIMIT 1
    """)

    insert_query = text("""
        INSERT INTO backend.recommendations (indicator, recommendation, usecase, simulation, deleted, status, created_at)
        VALUES (:indicator, CAST(:recommendation AS jsonb), :usecase, :simulation, :deleted, :status, NOW())
    """)

    params = {
        "indicator": "Train",
        "usecase": "utilisation_train",
        "recommendation": recommendation_str,
        "simulation": "",
        "deleted": False,
        "status": "pending",
    }

    with engine.begin() as conn:
        exists = conn.execute(check_query, params).fetchone()
        if exists:
            logger.info("  Recommendation already exists in DB — skipping insert.")
        else:
            conn.execute(insert_query, params)
            logger.info("  Recommendation inserted into DB successfully.")


def save_distribution_to_csv(result_df: pd.DataFrame) -> None:
    """Save the distributed ridership dataframe to a CSV in the train indicators folder."""
    output_path = Path(__file__).parent / "train_ridership_distribution.csv"
    result_df.to_csv(output_path, index=False)
    logger.info("  Saved distribution CSV to %s", output_path)


if __name__ == "__main__":
    stop_times_df = load_stop_times_with_stops()
    ridership_df = load_train_station_ridership()

    print("\n--- Stop Times (first 5 rows) ---")
    print(stop_times_df.head())
    print("\nShape:", stop_times_df.shape)
    print("Null headsigns:", stop_times_df["headsign"].isna().sum())

    filled_df, unique_combinations_df = process_stop_times(stop_times_df)

    print("\n--- Filled Stop Times (first 5 rows) ---")
    print(filled_df.head())
    print("Null headsigns after fill:", filled_df["headsign"].isna().sum())

    print("\n--- Unique Headsign/Stop Combinations (first 5 rows) ---")
    print(unique_combinations_df.head())
    print("\nShape:", unique_combinations_df.shape)

    print("\n--- Ridership (first 5 rows) ---")
    print(ridership_df.head())
    print("\nShape:", ridership_df.shape)

    predicted_df = predict_ridership_2025(ridership_df)

    print("\n--- Predicted 2025 Ridership (first 5 rows) ---")
    print(predicted_df.head())

    result_df = distribute_ridership_weighted(
        unique_combinations_df, ridership_df, predicted_df
    )

    print("\n--- Weighted Distributed Ridership (first 5 rows) ---")
    print(result_df.head())
    print("\nShape:", result_df.shape)
    print("Stations with no ridership match:", result_df["count_2024"].isna().sum())

    # save_distribution_to_csv(result_df)

    utilisation_df = compute_utilisation(result_df)

    print("\n--- Utilisation Summary ---")
    print(utilisation_df)

    # utilisation_path = Path(__file__).parent / "train_utilisation_summary.csv"
    # utilisation_df.to_csv(utilisation_path, index=False)
    # logger.info("Saved utilisation summary to %s", utilisation_path)

    utilisation_json = build_utilisation_json(utilisation_df)

    print("\n--- Utilisation JSON ---")
    print(json.dumps(utilisation_json, indent=2))

    save_recommendation_to_db(utilisation_json)
