"""
Cycle station availability risk engine.

Trains one LogisticRegression per Dublin Bikes station using 30 days of
snapshot history, then writes (empty_risk_2h, full_risk_2h) predictions to
backend.cycle_station_risk_scores every 5 minutes (backend schema, backend_user).

Features used per station:
  bike_ratio            current available_bikes / capacity
  hour_of_day           0-23 (Europe/Dublin)
  day_of_week           0=Mon … 6=Sun
  empty_risk_this_hour  historical P(empty) for this station at this hour
  departure_rate_30m    avg bikes leaving per interval over last 30 min
  arrival_rate_30m      avg bikes arriving per interval over last 30 min

Schedule:
  Train on startup, then once per calendar day at midnight.
  Score every 5 minutes.
"""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from datetime import UTC, datetime
from zoneinfo import ZoneInfo

import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler
from sqlalchemy import Engine, text

logger = logging.getLogger(__name__)

_DUBLIN = ZoneInfo("Europe/Dublin")

# ── constants ─────────────────────────────────────────────────────────────────

TRAINING_DAYS = 30
EMPTY_THRESHOLD = 2  # available_bikes ≤ this → station considered "empty"
FULL_RATIO = 0.95  # bikes / capacity ≥ this → station considered "full"
HORIZON_H = 2  # predict emptiness / fullness within this many hours
MIN_ROWS = 50  # skip station if not enough training rows
SCORE_INTERVAL_S = 300  # score every 5 minutes

FEATURES = [
    "bike_ratio",
    "hour_of_day",
    "day_of_week",
    "empty_risk_this_hour",
    "departure_rate_30m",
    "arrival_rate_30m",
]

# ── SQL ───────────────────────────────────────────────────────────────────────

_SNAPSHOTS_SQL = text("""
    SELECT
        s.station_id,
        s.timestamp,
        s.available_bikes,
        st.capacity
    FROM external_data.dublin_bikes_station_snapshots s
    JOIN external_data.dublin_bikes_stations st
      ON s.station_id = st.station_id
    WHERE s.timestamp >= NOW() - INTERVAL '30 days'
      AND s.is_installed = true
    ORDER BY s.station_id, s.timestamp
""")

_HOURLY_RISK_SQL = text("""
    SELECT
        station_id,
        EXTRACT(HOUR FROM timestamp AT TIME ZONE 'Europe/Dublin')::int AS hour_of_day,
        COUNT(CASE WHEN available_bikes <= 2 THEN 1 END)::float
            / NULLIF(COUNT(*), 0) AS empty_risk
    FROM external_data.dublin_bikes_station_snapshots
    WHERE timestamp >= NOW() - INTERVAL '30 days'
      AND is_installed = true
    GROUP BY station_id, hour_of_day
""")

_CURRENT_SNAPSHOT_SQL = text("""
    SELECT DISTINCT ON (s.station_id)
        s.station_id,
        s.available_bikes,
        st.capacity
    FROM external_data.dublin_bikes_station_snapshots s
    JOIN external_data.dublin_bikes_stations st
      ON s.station_id = st.station_id
    WHERE s.is_installed = true
    ORDER BY s.station_id, s.timestamp DESC
""")

_RECENT_FLOW_SQL = text("""
    SELECT
        station_id,
        AVG(CASE WHEN delta <= -1 AND delta >= -5 THEN -delta ELSE 0 END)
            AS departure_rate_30m,
        AVG(CASE WHEN delta >=  1 AND delta <=  5 THEN  delta ELSE 0 END)
            AS arrival_rate_30m
    FROM (
        SELECT
            station_id,
            available_bikes
                - LAG(available_bikes) OVER (PARTITION BY station_id ORDER BY timestamp)
                AS delta
        FROM external_data.dublin_bikes_station_snapshots
        WHERE timestamp >= NOW() - INTERVAL '30 minutes'
          AND is_installed = true
    ) deltas
    WHERE delta IS NOT NULL
    GROUP BY station_id
""")

_CREATE_SCORES_TABLE_SQL = text("""
    CREATE TABLE IF NOT EXISTS backend.cycle_station_risk_scores (
        station_id        INTEGER PRIMARY KEY,
        empty_risk_2h     DOUBLE PRECISION NOT NULL,
        full_risk_2h      DOUBLE PRECISION NOT NULL,
        scored_at         TIMESTAMPTZ NOT NULL,
        model_trained_at  TIMESTAMPTZ NOT NULL
    )
""")

_UPSERT_SQL = text("""
    INSERT INTO backend.cycle_station_risk_scores
        (station_id, empty_risk_2h, full_risk_2h, scored_at, model_trained_at)
    VALUES (:station_id, :empty_risk_2h, :full_risk_2h, :scored_at, :model_trained_at)
    ON CONFLICT (station_id) DO UPDATE SET
        empty_risk_2h    = EXCLUDED.empty_risk_2h,
        full_risk_2h     = EXCLUDED.full_risk_2h,
        scored_at        = EXCLUDED.scored_at,
        model_trained_at = EXCLUDED.model_trained_at
""")

# ── feature helpers ───────────────────────────────────────────────────────────


def _add_time_features(df: pd.DataFrame) -> pd.DataFrame:
    ts = df["timestamp"].dt.tz_convert(_DUBLIN)
    df["hour_of_day"] = ts.dt.hour.astype(float)
    df["day_of_week"] = ts.dt.dayofweek.astype(float)
    return df


def _add_flow_features(df: pd.DataFrame) -> pd.DataFrame:
    """Rolling 30-min departure/arrival rates for a single station's dataframe."""
    df = df.copy().set_index("timestamp").sort_index()
    df["delta"] = df["available_bikes"].diff()
    df["dep"] = df["delta"].apply(
        lambda d: -d if pd.notna(d) and -5 <= d <= -1 else 0.0
    )
    df["arr"] = df["delta"].apply(lambda d: d if pd.notna(d) and 1 <= d <= 5 else 0.0)
    df["departure_rate_30m"] = df["dep"].rolling("30min").mean().fillna(0.0)
    df["arrival_rate_30m"] = df["arr"].rolling("30min").mean().fillna(0.0)
    return df.drop(columns=["dep", "arr", "delta"]).reset_index()


def _compute_labels(df: pd.DataFrame) -> pd.DataFrame:
    """
    Two-pointer O(n) scan.
    For each row at time T, label = 1 if the station becomes empty (or full)
    within the next HORIZON_H hours.
    """
    df = df.sort_values("timestamp").reset_index(drop=True)
    ts = df["timestamp"].values
    bikes = df["available_bikes"].values
    cap = df["capacity"].values
    n = len(df)
    horizon = np.timedelta64(HORIZON_H, "h")

    empty_labels = np.zeros(n, dtype=np.int8)
    full_labels = np.zeros(n, dtype=np.int8)

    j = 0
    for i in range(n):
        while j < n and ts[j] <= ts[i]:
            j += 1
        k = j
        while k < n and (ts[k] - ts[i]) <= horizon:
            if bikes[k] <= EMPTY_THRESHOLD:
                empty_labels[i] = 1
            if cap[k] > 0 and bikes[k] / cap[k] >= FULL_RATIO:
                full_labels[i] = 1
            k += 1

    df["will_be_empty_2h"] = empty_labels
    df["will_be_full_2h"] = full_labels
    return df


# ── model container ───────────────────────────────────────────────────────────


@dataclass
class StationModel:
    scaler: StandardScaler
    empty_clf: LogisticRegression
    full_clf: LogisticRegression
    has_empty_variance: bool
    has_full_variance: bool

    def predict(self, features: dict[str, float]) -> tuple[float, float]:
        x_feat = np.array([[features.get(f, 0.0) for f in FEATURES]])
        x_scaled = self.scaler.transform(x_feat)
        empty_p = (
            float(self.empty_clf.predict_proba(x_scaled)[0][1])
            if self.has_empty_variance
            else 0.0
        )
        full_p = (
            float(self.full_clf.predict_proba(x_scaled)[0][1])
            if self.has_full_variance
            else 0.0
        )
        return empty_p, full_p


# ── training ──────────────────────────────────────────────────────────────────


def train_models(engine: Engine) -> tuple[dict[int, StationModel], datetime]:
    """
    Load last 30 days of snapshots, build features + labels, and fit one
    LogisticRegression per station.  Returns (models, trained_at).
    """
    logger.info("Fetching %d days of snapshot history for training...", TRAINING_DAYS)
    with engine.connect() as conn:
        df_all = pd.read_sql(str(_SNAPSHOTS_SQL), conn, parse_dates=["timestamp"])
        df_risk = pd.read_sql(str(_HOURLY_RISK_SQL), conn)

    if df_all["timestamp"].dt.tz is None:
        df_all["timestamp"] = df_all["timestamp"].dt.tz_localize("UTC")

    risk_map: dict[tuple[int, int], float] = {
        (int(r.station_id), int(r.hour_of_day)): float(r.empty_risk)
        for r in df_risk.itertuples()
    }

    models: dict[int, StationModel] = {}

    for sid in df_all["station_id"].unique():
        sdf = df_all[df_all["station_id"] == sid].copy().sort_values("timestamp")
        sdf["bike_ratio"] = sdf["available_bikes"] / sdf["capacity"].clip(lower=1)
        sdf = _add_time_features(sdf)
        sdf = _add_flow_features(sdf)
        sdf["empty_risk_this_hour"] = sdf["hour_of_day"].map(
            lambda h, s=sid: risk_map.get((s, int(h)), 0.0)
        )
        sdf = _compute_labels(sdf)
        sdf = sdf.dropna(subset=FEATURES)

        if len(sdf) < MIN_ROWS:
            logger.debug(
                "Station %d: only %d rows (need %d) — skipping", sid, len(sdf), MIN_ROWS
            )
            continue

        x_feat = sdf[FEATURES].to_numpy(dtype=float)
        y_empty = sdf["will_be_empty_2h"].to_numpy()
        y_full = sdf["will_be_full_2h"].to_numpy()

        scaler = StandardScaler()
        x_scaled = scaler.fit_transform(x_feat)

        has_empty_var = len(np.unique(y_empty)) > 1
        has_full_var = len(np.unique(y_full)) > 1

        empty_clf = LogisticRegression(max_iter=300, class_weight="balanced")
        full_clf = LogisticRegression(max_iter=300, class_weight="balanced")

        if has_empty_var:
            empty_clf.fit(x_scaled, y_empty)
        if has_full_var:
            full_clf.fit(x_scaled, y_full)

        models[sid] = StationModel(
            scaler, empty_clf, full_clf, has_empty_var, has_full_var
        )

    trained_at = datetime.now(tz=UTC)
    logger.info(
        "Training complete: %d / %d stations have models",
        len(models),
        df_all["station_id"].nunique(),
    )
    return models, trained_at


# ── scoring ───────────────────────────────────────────────────────────────────


def score_stations(
    models: dict[int, StationModel],
    model_trained_at: datetime,
    engine: Engine,
) -> int:
    """Score all stations and upsert results. Returns number of rows written."""
    with engine.connect() as conn:
        df_snap = pd.read_sql(str(_CURRENT_SNAPSHOT_SQL), conn)
        df_flow = pd.read_sql(str(_RECENT_FLOW_SQL), conn)
        df_risk = pd.read_sql(str(_HOURLY_RISK_SQL), conn)

    now_dublin = datetime.now(tz=UTC).astimezone(_DUBLIN)
    current_hour = now_dublin.hour
    current_dow = now_dublin.weekday()

    flow_map: dict[int, tuple[float, float]] = {
        int(r.station_id): (float(r.departure_rate_30m), float(r.arrival_rate_30m))
        for r in df_flow.itertuples()
    }
    risk_map: dict[tuple[int, int], float] = {
        (int(r.station_id), int(r.hour_of_day)): float(r.empty_risk)
        for r in df_risk.itertuples()
    }

    scored_at = datetime.now(tz=UTC)
    rows = []

    for snap in df_snap.itertuples():
        sid = int(snap.station_id)
        model = models.get(sid)
        if model is None:
            continue
        dep, arr = flow_map.get(sid, (0.0, 0.0))
        features = {
            "bike_ratio": float(snap.available_bikes) / max(float(snap.capacity), 1.0),
            "hour_of_day": float(current_hour),
            "day_of_week": float(current_dow),
            "empty_risk_this_hour": risk_map.get((sid, current_hour), 0.0),
            "departure_rate_30m": dep,
            "arrival_rate_30m": arr,
        }
        empty_p, full_p = model.predict(features)
        rows.append(
            {
                "station_id": sid,
                "empty_risk_2h": empty_p,
                "full_risk_2h": full_p,
                "scored_at": scored_at,
                "model_trained_at": model_trained_at,
            }
        )

    if rows:
        with engine.begin() as conn:
            conn.execute(_UPSERT_SQL, rows)

    logger.info("Scored %d stations", len(rows))
    return len(rows)


# ── main loop ─────────────────────────────────────────────────────────────────


def run(engine: Engine) -> None:
    """
    Entry point for the risk engine.

    - Ensures the output table exists.
    - Trains models immediately on startup, then retrains once per calendar day.
    - Scores every SCORE_INTERVAL_S seconds.

    Designed to run in a background thread (daemon=True) alongside FastAPI.
    """
    with engine.begin() as conn:
        conn.execute(_CREATE_SCORES_TABLE_SQL)
    logger.info("cycle_station_risk_scores table ready")

    models: dict[int, StationModel] = {}
    model_trained_at = datetime.now(tz=UTC)
    last_train_day: int | None = None

    while True:
        today = datetime.now(tz=UTC).toordinal()

        if last_train_day != today:
            try:
                models, model_trained_at = train_models(engine)
                last_train_day = today
            except Exception:
                logger.exception("Training failed — keeping existing models")

        if models:
            try:
                score_stations(models, model_trained_at, engine)
            except Exception:
                logger.exception("Scoring failed — will retry next interval")
        else:
            logger.warning("No models available yet — skipping score")

        time.sleep(SCORE_INTERVAL_S)
