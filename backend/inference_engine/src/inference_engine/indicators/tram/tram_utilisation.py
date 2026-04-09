"""Tram utilisation analysis and recommendation engine.

Calculates per-stop utilisation by comparing estimated passenger demand
against scheduled tram capacity. Filters out short depot movements
(<10 stops) to count only real passenger services.

Utilisation = estimated_passengers / (real_trams_at_stop x tram_capacity)
"""

import json
import logging
from dataclasses import dataclass, field

import pandas as pd
import requests
from sqlalchemy import text

from inference_engine.db import engine
from inference_engine.settings.api_settings import get_api_settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ── Constants ─────────────────────────────────────────────────────

TRAM_CAPACITY = {"red": 200, "green": 300}
_FALLBACK_DAILY_PASSENGERS = {"red": 86_000.0, "green": 84_000.0}
MIN_STOPS_PER_TRIP = 10  # trips with fewer stops are depot moves, not real services

# ── Thresholds ────────────────────────────────────────────────────

OVER_UTILISED_THRESHOLD = 0.75
UNDER_UTILISED_THRESHOLD = 0.25
SEGMENT_CONTRAST_RATIO = 1.5
MIN_SEGMENT_LENGTH = 3
DIRECTION_IMBALANCE_RATIO = 2.0

# ── Time Periods ──────────────────────────────────────────────────

TIME_PERIODS = [
    {"key": "early", "label": "Early (05:30-07:00)", "start": 5, "end": 7},
    {"key": "morning", "label": "Morning Peak (07:00-10:00)", "start": 7, "end": 10},
    {"key": "interpeak", "label": "Inter-Peak (10:00-16:00)", "start": 10, "end": 16},
    {"key": "evening", "label": "Evening Peak (16:00-19:00)", "start": 16, "end": 19},
    {"key": "offpeak", "label": "Off-Peak (19:00-23:00)", "start": 19, "end": 23},
]

# ── Data Classes ──────────────────────────────────────────────────


@dataclass
class StopMetrics:
    stop_id: str
    stop_name: str
    line: str
    sequence: float
    inbound_trips: int = 0
    outbound_trips: int = 0
    total_trips: int = 0
    est_inbound_pax: float = 0.0
    est_outbound_pax: float = 0.0
    est_total_pax: float = 0.0
    capacity: float = 0.0
    utilisation: float = 0.0
    avg_delay_mins: float = 0.0
    delay_count: int = 0


@dataclass
class Recommendation:
    type: str
    line: str
    time_period: str
    time_label: str
    description: str
    severity: str
    details: dict = field(default_factory=dict)


# ── Data Loading ──────────────────────────────────────────────────


def load_daily_passengers() -> dict[str, float]:
    weekly_query = text("""
        SELECT line_label, value FROM external_data.tram_passenger_journeys
        WHERE week_code = (SELECT MAX(week_code) FROM external_data.tram_passenger_journeys)
        AND value IS NOT NULL
    """)
    with engine.connect() as conn:
        rows = conn.execute(weekly_query).fetchall()

    result: dict[str, float] = {}
    for label, value in rows:
        key = label.strip().lower()
        if "red" in key:
            result["red"] = value / 7.0
        elif "green" in key:
            result["green"] = value / 7.0

    if result:
        logger.info(
            "Daily passengers from CSO: red=%.0f, green=%.0f",
            result.get("red", 0),
            result.get("green", 0),
        )
        return result

    monthly_query = text("""
        SELECT statistic_label, value FROM external_data.tram_passenger_numbers
        WHERE year = (SELECT MAX(year) FROM external_data.tram_passenger_numbers)
          AND month_code = (SELECT MAX(month_code) FROM external_data.tram_passenger_numbers
                            WHERE year = (SELECT MAX(year) FROM external_data.tram_passenger_numbers))
          AND value IS NOT NULL
    """)
    with engine.connect() as conn:
        rows = conn.execute(monthly_query).fetchall()
    for label, value in rows:
        key = label.strip().lower()
        if "red" in key:
            result["red"] = value / 30.0
        elif "green" in key:
            result["green"] = value / 30.0
    if result:
        logger.info(
            "Daily passengers from CSO monthly: red=%.0f, green=%.0f",
            result.get("red", 0),
            result.get("green", 0),
        )
        return result

    logger.warning("No CSO data, using fallback.")
    return _FALLBACK_DAILY_PASSENGERS.copy()


def load_luas_stops() -> pd.DataFrame:
    with engine.connect() as conn:
        df = pd.read_sql(
            text(
                "SELECT stop_id, name, line, lat, lon FROM external_data.tram_luas_stops"
            ),
            conn,
        )
    logger.info("Loaded %d Luas stops.", len(df))
    return df


def load_gtfs_stops() -> pd.DataFrame:
    with engine.connect() as conn:
        df = pd.read_sql(text("SELECT id, name FROM external_data.tram_stops"), conn)
    logger.info("Loaded %d GTFS stops.", len(df))
    return df


def load_weekday_stop_times() -> pd.DataFrame:
    """Load weekday stop times, filtering out short depot trips (<10 stops)."""
    query = text("""
        WITH trip_stop_counts AS (
            SELECT trip_id, COUNT(*) as stop_count
            FROM external_data.tram_stop_times
            GROUP BY trip_id
        )
        SELECT st.trip_id, st.stop_id, st.arrival_time, st.sequence,
               t.direction_id, t.route_id, t.headsign,
               r.short_name as route_name
        FROM external_data.tram_stop_times st
        JOIN external_data.tram_trips t ON st.trip_id = t.id
        JOIN external_data.tram_routes r ON t.route_id = r.id
        JOIN external_data.tram_calendar_schedule cs ON t.service_id = cs.service_id
        JOIN trip_stop_counts tsc ON st.trip_id = tsc.trip_id
        WHERE st.arrival_time IS NOT NULL
          AND cs.monday = true
          AND tsc.stop_count >= :min_stops
    """)
    with engine.connect() as conn:
        df = pd.read_sql(query, conn, params={"min_stops": MIN_STOPS_PER_TRIP})
    logger.info(
        "Loaded %d weekday stop time records (trips with >=%d stops).",
        len(df),
        MIN_STOPS_PER_TRIP,
    )
    return df


def load_hourly_distribution() -> pd.DataFrame:
    with engine.connect() as conn:
        df = pd.read_sql(
            text("""
            SELECT line_code, line_label, time_code, time_label, value
            FROM external_data.tram_hourly_distribution
            WHERE year = (SELECT MAX(year) FROM external_data.tram_hourly_distribution)
        """),
            conn,
        )
    logger.info("Loaded %d hourly distribution rows.", len(df))
    return df


def load_delay_history() -> pd.DataFrame:
    with engine.connect() as conn:
        df = pd.read_sql(
            text("""
            SELECT stop_id, stop_name, line,
                   ROUND(AVG(delay_mins)::numeric, 1) AS avg_delay,
                   COUNT(*) AS delay_count
            FROM external_data.tram_delay_history
            GROUP BY stop_id, stop_name, line
        """),
            conn,
        )
    logger.info("Loaded delay history for %d stops.", len(df))
    return df


# ── Name Matching ─────────────────────────────────────────────────


def build_luas_to_gtfs_name_map(
    luas_stops: pd.DataFrame,
    gtfs_stops: pd.DataFrame,
) -> dict[str, str]:
    gtfs_names = set(gtfs_stops["name"].str.lower().unique())
    normalised = {n.replace(" - ", " ").replace("  ", " "): n for n in gtfs_names}
    mapping = {}
    for _, row in luas_stops.iterrows():
        ln = row["name"].lower()
        sid = row["stop_id"]
        if ln in gtfs_names:
            mapping[sid] = ln
        elif ln.replace(" - ", " ").replace("  ", " ") in normalised:
            mapping[sid] = normalised[ln.replace(" - ", " ").replace("  ", " ")]
        else:
            for gn in gtfs_names:
                if gn in ln or ln in gn:
                    mapping[sid] = gn
                    break
    logger.info("Mapped %d / %d Luas stops to GTFS.", len(mapping), len(luas_stops))
    return mapping


def build_gtfs_name_to_ids(gtfs_stops: pd.DataFrame) -> dict[str, list[str]]:
    result = {}
    for _, row in gtfs_stops.iterrows():
        result.setdefault(row["name"].lower(), []).append(row["id"])
    return result


# ── Core Analysis ─────────────────────────────────────────────────


def _parse_hour(label: str) -> int:
    try:
        return int(label[:2].strip())
    except (ValueError, IndexError):
        return -1


def _expand_hours(start: int, end: int) -> list[int]:
    if start <= end:
        return list(range(start, end))
    return list(range(start, 25)) + list(range(end))


def compute_stop_metrics(  # noqa: PLR0913, PLR0912, PLR0915
    luas_stops: pd.DataFrame,
    gtfs_stops: pd.DataFrame,
    stop_times: pd.DataFrame,
    hourly_dist: pd.DataFrame,
    delay_df: pd.DataFrame,
    daily_passengers: dict[str, float],
    start_hour: int,
    end_hour: int,
) -> list[StopMetrics]:
    luas_to_gtfs = build_luas_to_gtfs_name_map(luas_stops, gtfs_stops)
    name_to_ids = build_gtfs_name_to_ids(gtfs_stops)

    gtfs_id_to_name = {}
    for name, ids in name_to_ids.items():
        for gid in ids:
            gtfs_id_to_name[gid] = name

    hours = _expand_hours(start_hour, end_hour)
    hour_set = set(hours)

    st = stop_times.copy()
    st["hour"] = pd.to_datetime(
        st["arrival_time"].astype(str), format="%H:%M:%S", errors="coerce"
    ).dt.hour
    st = st[st["hour"].isin(hour_set)]

    st["gtfs_name"] = st["stop_id"].map(gtfs_id_to_name)
    st = st.dropna(subset=["gtfs_name"])

    gtfs_name_to_line = {}
    for _, row in luas_stops.iterrows():
        gname = luas_to_gtfs.get(row["stop_id"])
        if gname:
            gtfs_name_to_line[gname] = row["line"]
    st["line"] = st["gtfs_name"].map(gtfs_name_to_line)

    # Count unique real-service trams per stop per direction
    trip_counts = (
        st.groupby(["gtfs_name", "direction_id"])["trip_id"]
        .nunique()
        .unstack(fill_value=0)
        .rename(columns={0: "outbound", 1: "inbound"})
    )
    for col in ("outbound", "inbound"):
        if col not in trip_counts.columns:
            trip_counts[col] = 0

    line_totals = st.groupby("line")["trip_id"].nunique().to_dict()
    avg_seq = st.groupby("gtfs_name")["sequence"].mean()

    # Hourly % per line from CSO
    hourly_pct = {}
    for _, row in hourly_dist.iterrows():
        h = _parse_hour(row["time_label"])
        if h in hour_set and row["value"] is not None:
            lk = str(row.get("line_label", row["line_code"])).strip().lower()
            if "red" in lk:
                hourly_pct["red"] = hourly_pct.get("red", 0.0) + row["value"]
            elif "green" in lk:
                hourly_pct["green"] = hourly_pct.get("green", 0.0) + row["value"]
            else:
                hourly_pct["_all"] = hourly_pct.get("_all", 0.0) + row["value"]

    delay_lookup = {}
    for _, row in delay_df.iterrows():
        delay_lookup[row["stop_id"]] = (
            float(row["avg_delay"]) if row["avg_delay"] else 0.0,
            int(row["delay_count"]) if row["delay_count"] else 0,
        )

    metrics = []
    for _, luas_row in luas_stops.iterrows():
        sid = luas_row["stop_id"]
        line = luas_row["line"]
        gname = luas_to_gtfs.get(sid)
        if gname is None:
            continue

        tc = (
            trip_counts.loc[gname]
            if gname in trip_counts.index
            else pd.Series({"outbound": 0, "inbound": 0})
        )
        out_trips = int(tc.get("outbound", 0))
        in_trips = int(tc.get("inbound", 0))
        total_trips = out_trips + in_trips

        lt = line_totals.get(line, 1)
        pct = hourly_pct.get(line, hourly_pct.get("_all", 0.0)) / 100.0
        daily = daily_passengers.get(line, 80_000.0)

        est_in = (in_trips / max(1, lt)) * pct * daily
        est_out = (out_trips / max(1, lt)) * pct * daily
        est_total = est_in + est_out

        cap = TRAM_CAPACITY.get(line, 200)
        capacity = total_trips * cap
        util = est_total / capacity if capacity > 0 else 0.0

        d_avg, d_count = delay_lookup.get(sid, (0.0, 0))
        seq = float(avg_seq.get(gname, 0.0))

        metrics.append(
            StopMetrics(
                stop_id=sid,
                stop_name=luas_row["name"],
                line=line,
                sequence=seq,
                inbound_trips=in_trips,
                outbound_trips=out_trips,
                total_trips=total_trips,
                est_inbound_pax=round(est_in, 1),
                est_outbound_pax=round(est_out, 1),
                est_total_pax=round(est_total, 1),
                capacity=round(capacity, 1),
                utilisation=round(util, 4),
                avg_delay_mins=d_avg,
                delay_count=d_count,
            )
        )

    return metrics


# ── Recommendation Logic ──────────────────────────────────────────


def _detect_frequency_change(
    metrics: list[StopMetrics],
    line: str,
    period: dict,
) -> list[Recommendation]:
    stops = [m for m in metrics if m.line == line and m.total_trips > 0]
    if not stops:
        return []

    utils = [m.utilisation for m in stops]
    avg_util = sum(utils) / len(utils)
    max_util = max(utils)
    avg_delay = sum(m.avg_delay_mins for m in stops) / len(stops)

    over_count = sum(1 for u in utils if u > OVER_UTILISED_THRESHOLD)
    under_count = sum(1 for u in utils if u < UNDER_UTILISED_THRESHOLD)
    monitor_count = sum(1 for u in utils if 0.50 <= u <= OVER_UTILISED_THRESHOLD)
    over_pct = over_count / len(stops)
    under_pct = under_count / len(stops)
    monitor_pct = monitor_count / len(stops)

    recs = []

    # ── HIGH/MEDIUM: >50% of stops overloaded (>75%) ──
    if over_pct > 0.50:
        hrs = max(1, period["end"] - period["start"])
        avg_tph = sum(m.total_trips for m in stops) / len(stops) / hrs
        target = 0.70
        extra = (
            max(1, round((avg_util / target - 1.0) * avg_tph))
            if avg_util > target
            else 1
        )
        sev = "high" if avg_util > 0.85 else "medium"

        recs.append(
            Recommendation(
                type="add_frequency",
                line=line,
                time_period=period["key"],
                time_label=period["label"],
                severity=sev,
                description=(
                    f"[{sev.upper()} PRIORITY] {line.capitalize()} Line is overcrowded during "
                    f"{period['label']}. Current load: {avg_util:.0%} of capacity "
                    f"({int(avg_util * TRAM_CAPACITY.get(line, 200))}/{TRAM_CAPACITY.get(line, 200)} "
                    f"passengers per tram). {over_count} out of {len(stops)} stops are above 75% capacity. "
                    f"Average delay: {avg_delay:.1f} minutes. "
                    f"Action: Increase service by {extra} additional tram(s) per hour to bring "
                    f"utilisation down to 60%."
                ),
                details={
                    "avg_utilisation": round(avg_util, 4),
                    "max_utilisation": round(max_util, 4),
                    "avg_delay_mins": round(avg_delay, 1),
                    "overloaded_stop_count": over_count,
                    "total_stop_count": len(stops),
                    "extra_trams_per_hour": extra,
                },
            )
        )

    # ── MONITOR: >50% of stops in 50-75% range ──
    elif monitor_pct > 0.50:
        recs.append(
            Recommendation(
                type="monitor",
                line=line,
                time_period=period["key"],
                time_label=period["label"],
                severity="very_low",
                description=(
                    f"{line.capitalize()} Line is approaching capacity during "
                    f"{period['label']}. Current load: {avg_util:.0%} of capacity "
                    f"({int(avg_util * TRAM_CAPACITY.get(line, 200))}/{TRAM_CAPACITY.get(line, 200)} "
                    f"passengers per tram). {monitor_count} out of {len(stops)} stops are between "
                    f"50-75% capacity. Average delay: {avg_delay:.1f} minutes. "
                    f"No immediate change needed. Continue monitoring — if demand increases, "
                    f"additional trams may be required."
                ),
                details={
                    "avg_utilisation": round(avg_util, 4),
                    "max_utilisation": round(max_util, 4),
                    "avg_delay_mins": round(avg_delay, 1),
                    "monitor_stop_count": monitor_count,
                    "total_stop_count": len(stops),
                },
            )
        )

    # ── REDUCE: >60% of stops underutilised (<25%) ──
    elif under_pct > 0.60:
        hrs = max(1, period["end"] - period["start"])
        avg_tph = sum(m.total_trips for m in stops) / len(stops) / hrs
        headway = 60 / avg_tph if avg_tph > 0 else 15
        new_headway = min(20, headway * (0.40 / max(0.01, avg_util)))

        recs.append(
            Recommendation(
                type="reduce_frequency",
                line=line,
                time_period=period["key"],
                time_label=period["label"],
                severity="low",
                description=(
                    f"[LOW PRIORITY] {line.capitalize()} Line is underutilised during "
                    f"{period['label']}. Current load: only {avg_util:.0%} of capacity "
                    f"({int(avg_util * TRAM_CAPACITY.get(line, 200))}/{TRAM_CAPACITY.get(line, 200)} "
                    f"passengers per tram). {under_count} out of {len(stops)} stops are below 25% capacity. "
                    f"Current frequency: every ~{headway:.0f} minutes. "
                    f"Action: Reduce frequency to every ~{new_headway:.0f} minutes to optimise "
                    f"operational costs while maintaining adequate service."
                ),
                details={
                    "avg_utilisation": round(avg_util, 4),
                    "underutilised_stop_count": under_count,
                    "total_stop_count": len(stops),
                    "current_headway_mins": round(headway, 1),
                    "recommended_headway_mins": round(new_headway, 1),
                },
            )
        )

    return recs


def _detect_partial_run(
    metrics: list[StopMetrics],
    line: str,
    period: dict,
) -> list[Recommendation]:
    stops = sorted(
        [m for m in metrics if m.line == line and m.total_trips > 0],
        key=lambda m: m.sequence,
    )
    if len(stops) < MIN_SEGMENT_LENGTH + 2:
        return []

    flags = [m.utilisation > OVER_UTILISED_THRESHOLD for m in stops]
    segments = []
    seg = []
    for i, f in enumerate(flags):
        if f:
            seg.append(i)
        else:
            if len(seg) >= MIN_SEGMENT_LENGTH:
                segments.append(seg)
            seg = []
    if len(seg) >= MIN_SEGMENT_LENGTH:
        segments.append(seg)

    if not segments:
        return []

    recs = []
    for si in segments:
        # Reject if segment covers >80% of all stops — that's a full-line
        # problem handled by add_frequency, not a partial-run
        if len(si) > len(stops) * 0.80:
            continue

        ss = [stops[i] for i in si]
        oi = [i for i in range(len(stops)) if i not in si]
        os_ = [stops[i] for i in oi]

        # Must have outer stops to compare against
        if not os_:
            continue

        sa = sum(m.utilisation for m in ss) / len(ss)
        oa = sum(m.utilisation for m in os_) / len(os_)

        if oa > 0 and sa / oa < SEGMENT_CONTRAST_RATIO:
            continue
        if oa > OVER_UTILISED_THRESHOLD:
            continue

        fi = max(0, si[0] - 1)
        li = min(len(stops) - 1, si[-1] + 1)

        recs.append(
            Recommendation(
                type="partial_run",
                line=line,
                time_period=period["key"],
                time_label=period["label"],
                severity="medium",
                description=(
                    f"[MEDIUM PRIORITY] A section of the {line.capitalize()} Line between "
                    f"{stops[fi].stop_name} and {stops[li].stop_name} is overloaded during "
                    f"{period['label']}. This {len(ss)}-stop segment has {sa:.0%} utilisation "
                    f"while the rest of the line is at {oa:.0%}. "
                    f"Action: Add a short-run tram service covering only this segment "
                    f"to relieve pressure without adding full-line services."
                ),
                details={
                    "start_stop_name": stops[fi].stop_name,
                    "end_stop_name": stops[li].stop_name,
                    "segment_stop_count": len(ss),
                    "segment_stops": [s.stop_name for s in ss],
                    "segment_avg_utilisation": round(sa, 4),
                    "outer_avg_utilisation": round(oa, 4),
                },
            )
        )
    return recs


def _detect_direction_imbalance(
    metrics: list[StopMetrics],
    line: str,
    period: dict,
) -> list[Recommendation]:
    stops = [m for m in metrics if m.line == line and m.total_trips > 0]
    if not stops:
        return []

    ti = sum(m.est_inbound_pax for m in stops)
    to = sum(m.est_outbound_pax for m in stops)
    if ti == 0 or to == 0:
        return []

    ratio = max(ti, to) / min(ti, to)
    if ratio < DIRECTION_IMBALANCE_RATIO:
        return []

    hd = "Inbound" if ti > to else "Outbound"
    ld = "Outbound" if ti > to else "Inbound"
    cap = TRAM_CAPACITY.get(line, 200)
    ht = (
        sum(m.inbound_trips for m in stops)
        if ti > to
        else sum(m.outbound_trips for m in stops)
    )
    hc = ht * cap
    hu = max(ti, to) / hc if hc > 0 else 0.0

    if hu < 0.60:
        return []

    return [
        Recommendation(
            type="rebalance",
            line=line,
            time_period=period["key"],
            time_label=period["label"],
            severity="medium",
            description=(
                f"[MEDIUM PRIORITY] {line.capitalize()} Line has a significant direction imbalance "
                f"during {period['label']}. {hd} demand is {ratio:.1f}x higher than {ld} "
                f"({max(ti, to):.0f} vs {min(ti, to):.0f} estimated passengers). "
                f"{hd} utilisation: {hu:.0%}. "
                f"Action: Reallocate 1 tram per hour from {ld} to {hd} direction "
                f"to balance capacity with demand."
            ),
            details={
                "heavy_direction": hd,
                "light_direction": ld,
                "inbound_pax": round(ti, 0),
                "outbound_pax": round(to, 0),
                "imbalance_ratio": round(ratio, 2),
                "heavy_direction_utilisation": round(hu, 4),
            },
        )
    ]


# ── Orchestration ─────────────────────────────────────────────────


def analyse_all_periods() -> list[Recommendation]:
    logger.info("Starting tram utilisation analysis...")

    luas_stops = load_luas_stops()
    gtfs_stops = load_gtfs_stops()
    stop_times = load_weekday_stop_times()
    hourly_dist = load_hourly_distribution()
    delay_df = load_delay_history()
    daily_pax = load_daily_passengers()

    all_recs = []

    for period in TIME_PERIODS:
        logger.info(
            "Analysing: %s (%02d:00-%02d:00)",
            period["key"],
            period["start"],
            period["end"],
        )

        metrics = compute_stop_metrics(
            luas_stops,
            gtfs_stops,
            stop_times,
            hourly_dist,
            delay_df,
            daily_pax,
            period["start"],
            period["end"],
        )

        for line in ("red", "green"):
            lm = [m for m in metrics if m.line == line and m.total_trips > 0]
            if not lm:
                continue

            utils = [m.utilisation for m in lm]
            # Show top 3 and bottom 3 stops
            sorted_stops = sorted(lm, key=lambda m: m.utilisation, reverse=True)
            top3 = sorted_stops[:3]
            bot3 = sorted_stops[-3:]

            logger.info(
                "  %s: %d stops, avg=%.1f%%, max=%.1f%%, min=%.1f%%",
                line.capitalize(),
                len(utils),
                100 * sum(utils) / len(utils),
                100 * max(utils),
                100 * min(utils),
            )
            for s in top3:
                logger.info(
                    "    HIGH: %s — %d trams, %.0f pax, util=%.1f%%",
                    s.stop_name,
                    s.total_trips,
                    s.est_total_pax,
                    s.utilisation * 100,
                )
            for s in bot3:
                logger.info(
                    "    LOW:  %s — %d trams, %.0f pax, util=%.1f%%",
                    s.stop_name,
                    s.total_trips,
                    s.est_total_pax,
                    s.utilisation * 100,
                )

            all_recs.extend(_detect_frequency_change(metrics, line, period))
            all_recs.extend(_detect_partial_run(metrics, line, period))
            all_recs.extend(_detect_direction_imbalance(metrics, line, period))

    # Sort by severity: high first, then medium, then low
    severity_order = {"high": 0, "medium": 1, "low": 2, "very_low": 3}
    all_recs.sort(key=lambda r: severity_order.get(r.severity, 3))

    logger.info("Total recommendations: %d", len(all_recs))
    return all_recs


# ── JSON + DB + Notification ──────────────────────────────────────


def build_recommendation_json(recs: list[Recommendation]) -> list[dict]:
    return [
        {
            "Name": f"{r.line.capitalize()} Line - {r.time_label}",
            "Attributes": {
                "type": r.type,
                "line": r.line,
                "time_period": r.time_period,
                "time_label": r.time_label,
                "severity": r.severity,
                "description": r.description,
                **r.details,
            },
        }
        for r in recs
    ]


def save_recommendation_to_db(rec_json: list[dict]) -> bool:
    rec_str = json.dumps(rec_json)
    check = text("""
        SELECT 1 FROM backend.recommendations
        WHERE indicator = :indicator AND usecase = :usecase
          AND CAST(recommendation AS text) = CAST(CAST(:recommendation AS jsonb) AS text)
        LIMIT 1
    """)
    delete_old = text("""
        DELETE FROM backend.recommendations
        WHERE indicator = :indicator AND usecase = :usecase
    """)
    insert = text("""
        INSERT INTO backend.recommendations
            (indicator, recommendation, usecase, simulation, deleted, status, created_at)
        VALUES (:indicator, CAST(:recommendation AS jsonb), :usecase, :simulation,
                :deleted, :status, NOW())
    """)
    params = {
        "indicator": "Tram",
        "usecase": "utilisation_tram",
        "recommendation": rec_str,
        "simulation": "",
        "deleted": False,
        "status": "pending",
    }

    with engine.begin() as conn:
        if conn.execute(check, params).fetchone():
            logger.info("Recommendation unchanged, skipping.")
            return False
        # Delete old tram recommendations before inserting fresh ones
        conn.execute(delete_old, {"indicator": "Tram", "usecase": "utilisation_tram"})
        conn.execute(insert, params)
        logger.info("Old recommendations replaced with fresh analysis.")
        return True


def broadcast_notification(recs: list[Recommendation]) -> None:
    if not recs:
        return
    api = get_api_settings()
    url = f"{api.hermes_url}/api/notification/v1/broadcast"

    high = [r for r in recs if r.severity == "high"]
    medium = [r for r in recs if r.severity == "medium"]
    low = [r for r in recs if r.severity == "low"]

    lines: list[str] = [f"Tram analysis generated {len(recs)} recommendation(s)."]
    for label, group in [("HIGH", high), ("MEDIUM", medium), ("LOW", low)]:
        if group:
            lines.append(f"\n{len(group)} {label}:")
            lines.extend(f"  - {r.description}" for r in group[:3])

    payload = {
        "dataIndicator": "tram",
        "subject": f"Tram Recommendations ({len(recs)} new)",
        "body": "\n".join(lines),
        "priority": "high" if high else "medium" if medium else "low",
        "channel": "EMAIL_AND_NOTIFICATION",
    }
    try:
        resp = requests.post(url, json=payload, timeout=api.http_timeout)
        if resp.ok:
            logger.info("Notification sent.")
        else:
            logger.warning("Notification failed: %d", resp.status_code)
    except requests.RequestException as e:
        logger.warning("Could not notify: %s", e)


# ── Entry Point ───────────────────────────────────────────────────

if __name__ == "__main__":
    recs = analyse_all_periods()

    if not recs:
        logger.info("No recommendations generated.")
    else:
        print("\n--- Tram Recommendations ---")
        for r in recs:
            print(f"  [{r.severity.upper()}] [{r.type}] {r.description}")

        rec_json = build_recommendation_json(recs)
        print("\n--- JSON ---")
        print(json.dumps(rec_json, indent=2))

        inserted = save_recommendation_to_db(rec_json)
        logger.info("Done.")
