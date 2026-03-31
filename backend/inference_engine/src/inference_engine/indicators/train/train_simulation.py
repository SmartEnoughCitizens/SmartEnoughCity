import json
import logging
import os

import google.generativeai as genai
from sqlalchemy import text

from inference_engine.db import engine

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

_GEMINI_MODEL = "gemini-2.5-flash"

_SYSTEM_PROMPT = """You are a public transport sustainability analyst for Dublin, Ireland.

You will be given:
1. Train utilisation data (ridership, capacity, utilisation status).
2. A recommendation (trains to add or remove per headsign).

Your tasks:
- Simulate the new utilisation % after applying the recommendation.
- Estimate CO₂ emissions saved assuming each absorbed passenger
  represents one fewer car trip (use 120g CO₂/km, 15km average trip distance).
- Generate a sustainability score from 0-100 reflecting the environmental
  benefit of this recommendation.

Return structured JSON with the following top-level fields:
- results: a list of objects, each with fields:
    headsign, new_utilisation_pct, passengers_absorbed,
    co2_saved_kg, sustainability_score, summary
- overall_summary: one sentence summarising the total carbon emission saved across all recommendations.

ONLY OUTPUT THE JSON AND NO OTHER TEXT BESIDES THIS.
"""


def fetch_pending_recommendation() -> tuple[int, list] | None:
    """
    Fetch the first recommendation from the DB where simulation is empty,
    usecase is 'utilisation_train', and indicator is 'Train'.

    Returns:
        Tuple of (id, recommendation_json) or None if no pending rows.
    """
    query = text("""
        SELECT id, recommendation
        FROM backend.recommendations
        WHERE (simulation IS NULL OR simulation = '')
          AND usecase = :usecase
          AND indicator = :indicator
        ORDER BY created_at ASC
        LIMIT 1
    """)

    with engine.connect() as conn:
        row = conn.execute(
            query, {"usecase": "utilisation_train", "indicator": "Train"}
        ).fetchone()

    if row is None:
        logger.info("No pending recommendations found.")
        return None

    rec_id, recommendation = row
    logger.info("Fetched recommendation id=%d for simulation.", rec_id)
    return rec_id, recommendation


def run_simulation(recommendation: list) -> str:
    """
    Send the recommendation JSON to Gemini and return the simulation output as a string.

    Args:
        recommendation: The recommendation list from the DB.

    Returns:
        The LLM response text as a string.
    """
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        msg = "GEMINI_API_KEY environment variable is not set."
        raise ValueError(msg)

    genai.configure(api_key=api_key)
    model = genai.GenerativeModel(
        model_name=_GEMINI_MODEL,
        system_instruction=_SYSTEM_PROMPT,
    )

    user_message = f"Here is the train utilisation recommendation data:\n\n{json.dumps(recommendation, indent=2)}"

    logger.info("Sending recommendation to Gemini (%s)...", _GEMINI_MODEL)
    response = model.generate_content(user_message)
    simulation_output = response.text
    logger.info("Received simulation response from Gemini.")
    return simulation_output


def store_simulation(rec_id: int, simulation_output: str) -> None:
    """
    Update the simulation column in the recommendations table for the given id.

    Args:
        rec_id: The id of the recommendation row to update.
        simulation_output: The LLM output string to store.
    """
    update_query = text("""
        UPDATE backend.recommendations
        SET simulation = :simulation
        WHERE id = :id
    """)

    with engine.begin() as conn:
        conn.execute(update_query, {"simulation": simulation_output, "id": rec_id})
    logger.info("Stored simulation output for recommendation id=%d.", rec_id)


if __name__ == "__main__":
    result = fetch_pending_recommendation()
    if result is None:
        logger.info("Nothing to process — exiting.")
    else:
        rec_id, recommendation = result
        simulation_output = run_simulation(recommendation)

        print("\n--- Simulation Output ---")
        print(simulation_output)

        store_simulation(rec_id, simulation_output)
        logger.info("Simulation complete.")
