"""Tests for train demand, simulation, and utilisation endpoints."""

from __future__ import annotations

import json
import math
from typing import Any
from unittest.mock import MagicMock, patch

import pandas as pd
import pytest
from fastapi.testclient import TestClient

from inference_engine.main import app

client = TestClient(app)

# ── Helpers ───────────────────────────────────────────────────────────────────

_SAMPLE_STOP: dict[str, Any] = {
    "stop_id": "stop_1",
    "name": "Connolly",
    "lat": 53.35,
    "lon": -6.25,
    "trip_count": 100,
    "ridership_count": 3650,
    "catchment_population": 5000,
    "station_type": "DART",
    "footfall_count": 200,
    "norm_ridership": 1.0,
    "norm_uptake": 0.5,
    "norm_pressure": 0.8,
    "norm_footfall": 0.4,
    "raw_pressure": 0.04,
    "max_pressure": 0.05,
    "demand_score": 0.75,
}

_SAMPLE_STOP_2: dict[str, Any] = {
    **_SAMPLE_STOP,
    "stop_id": "stop_2",
    "name": "Tara Street",
    "ridership_count": 0,
    "demand_score": 0.3,
}


# ── Unit: _simulate_stop ──────────────────────────────────────────────────────


def test_simulate_stop_with_ridership() -> None:
    from inference_engine.train_router import _simulate_stop

    result = _simulate_stop(_SAMPLE_STOP, extra=20)

    assert result["trip_count"] == 120
    assert result["demand_score"] < _SAMPLE_STOP["demand_score"]
    assert 0 <= result["demand_score"] <= 1
    assert result["norm_pressure"] < _SAMPLE_STOP["norm_pressure"]


def test_simulate_stop_no_ridership_decays_score() -> None:
    from inference_engine.train_router import _simulate_stop

    result = _simulate_stop(_SAMPLE_STOP_2, extra=10)

    # With no ridership, score decays by pressure_factor
    relief = 10 / max(_SAMPLE_STOP_2["trip_count"], 1)
    expected_factor = math.exp(-relief * 13.0)
    assert abs(result["demand_score"] - _SAMPLE_STOP_2["demand_score"] * expected_factor) < 1e-6


def test_simulate_stop_zero_trips_guard() -> None:
    from inference_engine.train_router import _simulate_stop

    stop = {**_SAMPLE_STOP, "trip_count": 0}
    result = _simulate_stop(stop, extra=5)
    # Should not raise; raw_pressure = 0 when new_trips is 0... but new_trips=5 here
    assert result["trip_count"] == 5


def test_simulate_stop_unknown_station_type_uses_default() -> None:
    from inference_engine.train_router import _simulate_stop

    stop = {**_SAMPLE_STOP, "station_type": None}
    result = _simulate_stop(stop, extra=10)
    assert result["trip_count"] == 110


# ── Unit: load_demand_scores_from_db ─────────────────────────────────────────


def test_load_demand_scores_from_db_success() -> None:
    from inference_engine.indicators.train.train_demand import load_demand_scores_from_db

    mock_df = pd.DataFrame([_SAMPLE_STOP])
    with patch(
        "inference_engine.indicators.train.train_demand.engine"
    ) as mock_engine:
        mock_conn = MagicMock()
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        with patch("pandas.read_sql", return_value=mock_df):
            result = load_demand_scores_from_db()

    assert len(result) == 1
    assert result[0]["stop_id"] == "stop_1"


def test_load_demand_scores_from_db_returns_empty_on_error() -> None:
    from inference_engine.indicators.train.train_demand import load_demand_scores_from_db

    with patch(
        "inference_engine.indicators.train.train_demand.engine"
    ) as mock_engine:
        mock_engine.connect.side_effect = Exception("DB unavailable")
        result = load_demand_scores_from_db()

    assert result == []


# ── Unit: ensure_demand_scores_table ─────────────────────────────────────────


def test_ensure_demand_scores_table() -> None:
    from inference_engine.indicators.train.train_demand import ensure_demand_scores_table

    with patch(
        "inference_engine.indicators.train.train_demand.engine"
    ) as mock_engine:
        mock_conn = MagicMock()
        mock_engine.begin.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.begin.return_value.__exit__ = MagicMock(return_value=False)
        ensure_demand_scores_table()

    mock_conn.execute.assert_called_once()


# ── Unit: _fetch_latest_simulation ───────────────────────────────────────────


def test_fetch_latest_simulation_returns_dict() -> None:
    from inference_engine.train_router import _fetch_latest_simulation

    payload = json.dumps({"step": "run more trains"})
    mock_row = MagicMock()
    mock_row.simulation = payload

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchone.return_value = mock_row
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _fetch_latest_simulation()

    assert result == {"step": "run more trains"}


def test_fetch_latest_simulation_returns_none_when_no_row() -> None:
    from inference_engine.train_router import _fetch_latest_simulation

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchone.return_value = None
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _fetch_latest_simulation()

    assert result is None


def test_fetch_latest_simulation_handles_bad_json() -> None:
    from inference_engine.train_router import _fetch_latest_simulation

    mock_row = MagicMock()
    mock_row.simulation = "not valid json {"

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchone.return_value = mock_row
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _fetch_latest_simulation()

    assert result is None


# ── Endpoint: GET /train/demand ───────────────────────────────────────────────


def test_get_demand_returns_scores() -> None:
    with patch(
        "inference_engine.train_router.load_demand_scores_from_db",
        return_value=[_SAMPLE_STOP],
    ):
        response = client.get("/train/demand")

    assert response.status_code == 200
    data = response.json()
    assert len(data) == 1
    assert data[0]["stop_id"] == "stop_1"


def test_get_demand_returns_500_on_error() -> None:
    with patch(
        "inference_engine.train_router.load_demand_scores_from_db",
        side_effect=Exception("DB error"),
    ):
        response = client.get("/train/demand")

    assert response.status_code == 500


# ── Endpoint: POST /train/demand/simulate ─────────────────────────────────────


def _make_route_row(route_id: str, stop_id: str, seq: int) -> MagicMock:
    row = MagicMock()
    row.route_id = route_id
    row.stop_id = stop_id
    row.seq = seq
    return row


def test_simulate_demand_empty_corridors() -> None:
    with patch(
        "inference_engine.train_router.load_demand_scores_from_db",
        return_value=[_SAMPLE_STOP],
    ):
        response = client.post("/train/demand/simulate", json={"corridors": []})

    assert response.status_code == 200
    body = response.json()
    assert body["affected_stop_ids"] == []
    assert len(body["base_demand"]) == 1
    assert len(body["simulated_demand"]) == 1


def test_simulate_demand_with_corridor() -> None:
    route_rows = [
        _make_route_row("R1", "stop_1", 0),
        _make_route_row("R1", "stop_2", 1),
    ]

    with (
        patch(
            "inference_engine.train_router.load_demand_scores_from_db",
            return_value=[_SAMPLE_STOP, _SAMPLE_STOP_2],
        ),
        patch("inference_engine.train_router.engine") as mock_engine,
    ):
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchall.return_value = route_rows
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)

        response = client.post(
            "/train/demand/simulate",
            json={
                "corridors": [
                    {
                        "origin_stop_id": "stop_1",
                        "destination_stop_id": "stop_2",
                        "train_count": 10,
                    }
                ]
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert "stop_1" in body["affected_stop_ids"] or "stop_2" in body["affected_stop_ids"]


def test_simulate_demand_corridor_fallback_to_endpoints() -> None:
    """When no route contains both stops, fallback marks just the endpoints."""
    with (
        patch(
            "inference_engine.train_router.load_demand_scores_from_db",
            return_value=[_SAMPLE_STOP, _SAMPLE_STOP_2],
        ),
        patch("inference_engine.train_router.engine") as mock_engine,
    ):
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchall.return_value = []  # no routes
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)

        response = client.post(
            "/train/demand/simulate",
            json={
                "corridors": [
                    {
                        "origin_stop_id": "stop_1",
                        "destination_stop_id": "stop_2",
                        "train_count": 5,
                    }
                ]
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert set(body["affected_stop_ids"]) == {"stop_1", "stop_2"}


def test_simulate_demand_caps_train_count() -> None:
    """train_count > 20 should be capped to 20; < 1 to 1."""
    with (
        patch(
            "inference_engine.train_router.load_demand_scores_from_db",
            return_value=[_SAMPLE_STOP],
        ),
        patch("inference_engine.train_router.engine") as mock_engine,
    ):
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchall.return_value = []
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)

        response = client.post(
            "/train/demand/simulate",
            json={
                "corridors": [
                    {
                        "origin_stop_id": "stop_1",
                        "destination_stop_id": "stop_x",
                        "train_count": 999,
                    }
                ]
            },
        )

    assert response.status_code == 200


def test_simulate_demand_max_3_corridors() -> None:
    """Only first 3 corridors are processed."""
    with (
        patch(
            "inference_engine.train_router.load_demand_scores_from_db",
            return_value=[_SAMPLE_STOP],
        ),
        patch("inference_engine.train_router.engine") as mock_engine,
    ):
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchall.return_value = []
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)

        corridors = [
            {"origin_stop_id": f"a{i}", "destination_stop_id": f"b{i}", "train_count": 1}
            for i in range(5)
        ]
        response = client.post("/train/demand/simulate", json={"corridors": corridors})

    assert response.status_code == 200


def test_simulate_demand_returns_500_on_error() -> None:
    with patch(
        "inference_engine.train_router.load_demand_scores_from_db",
        side_effect=Exception("boom"),
    ):
        response = client.post(
            "/train/demand/simulate",
            json={"corridors": [{"origin_stop_id": "a", "destination_stop_id": "b", "train_count": 1}]},
        )

    assert response.status_code == 500


# ── Endpoint: GET /train/utilisation (cached) ─────────────────────────────────


def test_get_utilisation_from_cache() -> None:
    import inference_engine.train_router as tr

    tr._UTILISATION_CACHE["headsigns"] = [{"headsign": "Connolly", "stations": []}]
    tr._UTILISATION_CACHE["timestamp"] = 1e18  # far future — cache valid

    with patch(
        "inference_engine.train_router._fetch_latest_simulation",
        return_value=None,
    ):
        response = client.get("/train/utilisation")

    assert response.status_code == 200
    body = response.json()
    assert "headsigns" in body
    assert len(body["headsigns"]) == 1

    # Reset cache
    tr._UTILISATION_CACHE["headsigns"] = None
    tr._UTILISATION_CACHE["timestamp"] = 0.0


def test_get_utilisation_cache_miss_runs_pipeline() -> None:
    import inference_engine.train_router as tr

    tr._UTILISATION_CACHE["headsigns"] = None
    tr._UTILISATION_CACHE["timestamp"] = 0.0

    mock_df = pd.DataFrame()

    with (
        patch("inference_engine.train_router.load_stop_times_with_stops", return_value=mock_df),
        patch("inference_engine.train_router.load_train_station_ridership", return_value=mock_df),
        patch(
            "inference_engine.train_router.process_stop_times",
            return_value=(mock_df, mock_df),
        ),
        patch("inference_engine.train_router.predict_ridership_2025", return_value=mock_df),
        patch("inference_engine.train_router.distribute_ridership_weighted", return_value=mock_df),
        patch("inference_engine.train_router.compute_utilisation", return_value=mock_df),
        patch("inference_engine.train_router.build_utilisation_json", return_value=[]),
        patch("inference_engine.train_router.save_recommendation_to_db"),
        patch("inference_engine.train_router._build_headsigns_response", return_value=[]),
        patch("inference_engine.train_router._fetch_latest_simulation", return_value=None),
    ):
        response = client.get("/train/utilisation")

    assert response.status_code == 200
    assert response.json() == {"headsigns": [], "simulation": None}


def test_get_utilisation_returns_500_on_pipeline_error() -> None:
    import inference_engine.train_router as tr

    tr._UTILISATION_CACHE["headsigns"] = None
    tr._UTILISATION_CACHE["timestamp"] = 0.0

    with patch(
        "inference_engine.train_router.load_stop_times_with_stops",
        side_effect=Exception("pipeline fail"),
    ):
        response = client.get("/train/utilisation")

    assert response.status_code == 500


# ── Endpoint: POST /train/utilisation/simulate ────────────────────────────────


def test_run_train_simulation_no_pending() -> None:
    with (
        patch(
            "inference_engine.train_router.fetch_pending_recommendation",
            return_value=None,
        ),
        patch(
            "inference_engine.train_router._fetch_latest_simulation",
            return_value={"plan": "add 5 trains"},
        ),
    ):
        response = client.post("/train/utilisation/simulate")

    assert response.status_code == 200
    body = response.json()
    assert body["already_simulated"] is True
    assert body["simulation"] == {"plan": "add 5 trains"}


def test_run_train_simulation_runs_gemini() -> None:
    sim_json = json.dumps({"recommendation": "add services"})
    with (
        patch(
            "inference_engine.train_router.fetch_pending_recommendation",
            return_value=(42, {"headsigns": []}),
        ),
        patch(
            "inference_engine.train_router.run_simulation",
            return_value=sim_json,
        ),
        patch("inference_engine.train_router.store_simulation"),
    ):
        response = client.post("/train/utilisation/simulate")

    assert response.status_code == 200
    body = response.json()
    assert body["already_simulated"] is False
    assert body["simulation"] == {"recommendation": "add services"}


def test_run_train_simulation_invalid_json_falls_back_to_raw() -> None:
    with (
        patch(
            "inference_engine.train_router.fetch_pending_recommendation",
            return_value=(1, {}),
        ),
        patch(
            "inference_engine.train_router.run_simulation",
            return_value="not json {{{",
        ),
        patch("inference_engine.train_router.store_simulation"),
    ):
        response = client.post("/train/utilisation/simulate")

    assert response.status_code == 200
    assert response.json()["simulation"] == {"raw": "not json {{{"}


def test_run_train_simulation_returns_500_on_error() -> None:
    with patch(
        "inference_engine.train_router.fetch_pending_recommendation",
        side_effect=Exception("gemini down"),
    ):
        response = client.post("/train/utilisation/simulate")

    assert response.status_code == 500


# ── Unit: warm_demand_cache ───────────────────────────────────────────────────


def test_warm_demand_cache_success() -> None:
    from inference_engine.train_router import warm_demand_cache

    with patch(
        "inference_engine.train_router.compute_and_save_demand_scores",
        return_value=[_SAMPLE_STOP],
    ):
        warm_demand_cache()  # should not raise


def test_warm_demand_cache_handles_exception() -> None:
    from inference_engine.train_router import warm_demand_cache

    with patch(
        "inference_engine.train_router.compute_and_save_demand_scores",
        side_effect=Exception("DB gone"),
    ):
        warm_demand_cache()  # should swallow exception, not raise


# ── Unit: warm_utilisation_cache ──────────────────────────────────────────────


def test_warm_utilisation_cache_success() -> None:
    from inference_engine.train_router import warm_utilisation_cache

    mock_df = pd.DataFrame()
    with (
        patch("inference_engine.train_router.load_stop_times_with_stops", return_value=mock_df),
        patch("inference_engine.train_router.load_train_station_ridership", return_value=mock_df),
        patch(
            "inference_engine.train_router.process_stop_times",
            return_value=(mock_df, mock_df),
        ),
        patch("inference_engine.train_router.predict_ridership_2025", return_value=mock_df),
        patch("inference_engine.train_router.distribute_ridership_weighted", return_value=mock_df),
        patch("inference_engine.train_router.compute_utilisation", return_value=mock_df),
        patch("inference_engine.train_router.build_utilisation_json", return_value=[]),
        patch("inference_engine.train_router.save_recommendation_to_db"),
        patch("inference_engine.train_router._build_headsigns_response", return_value=[]),
    ):
        warm_utilisation_cache()


def test_warm_utilisation_cache_handles_exception() -> None:
    from inference_engine.train_router import warm_utilisation_cache

    with patch(
        "inference_engine.train_router.load_stop_times_with_stops",
        side_effect=Exception("no DB"),
    ):
        warm_utilisation_cache()  # should swallow


# ── EV router coverage ────────────────────────────────────────────────────────


def test_ev_areas_geojson_success() -> None:
    mock_data = {"type": "FeatureCollection", "features": []}
    with patch("inference_engine.ev_service.get_areas_geojson", return_value=mock_data):
        response = client.get("/ev/areas-geojson")
    assert response.status_code == 200
    assert response.json()["type"] == "FeatureCollection"


def test_ev_areas_geojson_error() -> None:
    with patch(
        "inference_engine.ev_service.get_areas_geojson",
        side_effect=Exception("fail"),
    ):
        response = client.get("/ev/areas-geojson")
    assert response.status_code == 500


def test_ev_charging_stations_success() -> None:
    mock_data = {"total_stations": 5, "stations": []}
    with patch("inference_engine.ev_service.get_charging_stations", return_value=mock_data):
        response = client.get("/ev/charging-stations")
    assert response.status_code == 200


def test_ev_charging_stations_error() -> None:
    with patch(
        "inference_engine.ev_service.get_charging_stations",
        side_effect=Exception("fail"),
    ):
        response = client.get("/ev/charging-stations")
    assert response.status_code == 500


def test_ev_charging_demand_success() -> None:
    mock_data = {"areas": [], "high_priority_areas": []}
    with patch("inference_engine.ev_service.get_charging_demand", return_value=mock_data):
        response = client.get("/ev/charging-demand")
    assert response.status_code == 200


def test_ev_charging_demand_error() -> None:
    with patch(
        "inference_engine.ev_service.get_charging_demand",
        side_effect=Exception("fail"),
    ):
        response = client.get("/ev/charging-demand")
    assert response.status_code == 500


# ── Unit: _load_stop_coords ───────────────────────────────────────────────────


def test_load_stop_coords() -> None:
    from inference_engine.train_router import _load_stop_coords

    mock_row = MagicMock()
    mock_row.name = "Connolly"
    mock_row.lat = 53.35
    mock_row.lon = -6.25

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchall.return_value = [mock_row]
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _load_stop_coords()

    assert result["Connolly"] == {"lat": 53.35, "lon": -6.25}


# ── Unit: _load_ordered_stop_sequence ────────────────────────────────────────


def test_load_ordered_stop_sequence() -> None:
    from inference_engine.train_router import _load_ordered_stop_sequence

    rows = []
    for name, headsign in [("Connolly", "Malahide"), ("Tara Street", None), ("Howth Jct", "Malahide")]:
        r = MagicMock()
        r.headsign = headsign
        r.name = name
        r.entry_id = name
        rows.append(r)

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchall.return_value = rows
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _load_ordered_stop_sequence()

    assert "Malahide" in result
    assert "Connolly" in result["Malahide"]


# ── Unit: _fetch_today_recommendation ────────────────────────────────────────


def test_fetch_today_recommendation_list_result() -> None:
    from inference_engine.train_router import _fetch_today_recommendation

    mock_row = MagicMock()
    mock_row.recommendation = [{"Train Name": "Malahide"}]

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchone.return_value = mock_row
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _fetch_today_recommendation()

    assert result is not None
    assert result[0]["Train Name"] == "Malahide"


def test_fetch_today_recommendation_string_json() -> None:
    from inference_engine.train_router import _fetch_today_recommendation

    mock_row = MagicMock()
    mock_row.recommendation = json.dumps([{"Train Name": "Howth"}])

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchone.return_value = mock_row
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _fetch_today_recommendation()

    assert result[0]["Train Name"] == "Howth"


def test_fetch_today_recommendation_none_row() -> None:
    from inference_engine.train_router import _fetch_today_recommendation

    with patch("inference_engine.train_router.engine") as mock_engine:
        mock_conn = MagicMock()
        mock_conn.execute.return_value.fetchone.return_value = None
        mock_engine.connect.return_value.__enter__ = MagicMock(return_value=mock_conn)
        mock_engine.connect.return_value.__exit__ = MagicMock(return_value=False)
        result = _fetch_today_recommendation()

    assert result is None


# ── Unit: demand score weights sum to 1 ──────────────────────────────────────


def test_demand_weights_sum_to_one() -> None:
    from inference_engine.indicators.train.train_demand import (
        W_FOOTFALL,
        W_PRESSURE,
        W_RIDERSHIP,
        W_UPTAKE,
    )

    assert abs(W_RIDERSHIP + W_UPTAKE + W_PRESSURE + W_FOOTFALL - 1.0) < 1e-9


# ── Unit: TRAIN_TYPE_CAPACITY keys ───────────────────────────────────────────


def test_train_type_capacity_keys() -> None:
    from inference_engine.indicators.train.train_demand import TRAIN_TYPE_CAPACITY

    assert "DART" in TRAIN_TYPE_CAPACITY
    assert "SUBURBAN" in TRAIN_TYPE_CAPACITY
    assert "MAINLINE" in TRAIN_TYPE_CAPACITY
    assert TRAIN_TYPE_CAPACITY["DART"] == 350
