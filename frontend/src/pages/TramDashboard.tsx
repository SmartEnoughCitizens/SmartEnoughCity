/**
 * Tram dashboard — matches CycleDashboard design
 * Right-side floating panel, MUI Tabs, theme-aware
 *
 * 6 tabs: Live | Delays | Usage | Common Delays | Simulation | Recommendations
 * Map: Live/Delays=standard, Usage=sized markers, CommonDelays=delay-sized markers,
 *      Simulation=demand-coloured markers
 */

import { useState, useEffect, useMemo, useCallback } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  IconButton,
  Tabs,
  Tab,
  Chip,
  TextField,
  InputAdornment,
  ListItemButton,
  Select,
  MenuItem,
  Button,
  Slider,
  FormControl,
  InputLabel,
  Autocomplete,
} from "@mui/material";
import type { SelectChangeEvent } from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import TramIcon from "@mui/icons-material/Tram";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import PeopleIcon from "@mui/icons-material/People";
import SearchIcon from "@mui/icons-material/Search";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import HistoryIcon from "@mui/icons-material/History";
import SpeedIcon from "@mui/icons-material/Speed";
import LightbulbIcon from "@mui/icons-material/Lightbulb";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import {
  useTramKpis,
  useTramLiveForecasts,
  useTramDelays,
  useTramStopUsage,
  useTramCommonDelays,
  useTramStopDemand,
  useSimulateTramDemand,
  useTramRecommendations,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import { safeJsonParse } from "@/utils/safeJsonParse";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

import type {
  TramLiveForecast,
  TramStopDemand,
  TramStopUsage,
  TramRecommendationItem,
} from "@/types";

// ── Constants ────────────────────────────────────────────────────

const DUBLIN_CENTER: [number, number] = [53.3398, -6.2603];
type LineFilter = "" | "red" | "green";

/** Simple schema that validates parsed JSON is an array of recommendation items. */
const tramRecommendationItemsSchema = {
  parse(data: unknown): TramRecommendationItem[] {
    if (!Array.isArray(data)) return [];
    return data as TramRecommendationItem[];
  },
};

const LINE_COLORS: Record<string, string> = {
  red: "#DC2626",
  green: "#16A34A",
};

const TIME_PERIODS = [
  {
    key: "early",
    label: "Early Service (05:30–07:00)",
    startHour: 5,
    endHour: 7,
  },
  {
    key: "morning",
    label: "Morning Peak (07:00–10:00)",
    startHour: 7,
    endHour: 10,
  },
  {
    key: "interpeak",
    label: "Inter-Peak (10:00–16:00)",
    startHour: 10,
    endHour: 16,
  },
  {
    key: "evening",
    label: "Evening Peak (16:00–19:00)",
    startHour: 16,
    endHour: 19,
  },
  {
    key: "offpeak",
    label: "Evening Off-Peak (19:00–23:00)",
    startHour: 19,
    endHour: 23,
  },
  {
    key: "latenight",
    label: "Late Night (23:00–00:30)",
    startHour: 23,
    endHour: 1,
  },
] as const;

// ── CSS ──────────────────────────────────────────────────────────

const INJECTED_CSS = `
  .leaflet-popup-content-wrapper { background:rgba(13,17,23,0.96)!important;color:#e6edf3!important;border:1px solid rgba(255,255,255,0.10)!important;border-radius:12px!important;box-shadow:0 8px 32px rgba(0,0,0,0.55)!important;padding:0!important; }
  .leaflet-popup-tip { background:rgba(13,17,23,0.96)!important; }
  .leaflet-popup-close-button { color:#8b949e!important;top:8px!important;right:8px!important; }
  .leaflet-popup-content { margin:14px 16px!important; }
  .tram-sim-popup .leaflet-popup-content-wrapper { background:#fff!important;color:#111827!important;border:1px solid rgba(0,0,0,0.08)!important;border-radius:12px!important;box-shadow:0 6px 24px rgba(0,0,0,0.14)!important; }
  .tram-sim-popup .leaflet-popup-tip { background:#fff!important; }
  .tram-sim-popup .leaflet-popup-close-button { color:#6b7280!important; }
  .tram-sim-popup .leaflet-popup-content { color:#111827!important; }
  .tram-sim-popup .leaflet-popup-content * { color:#111827!important; }
  .tram-sim-popup .leaflet-popup-content .MuiTypography-root { color:#111827!important; }
  .tram-sim-popup .leaflet-popup-content .MuiChip-root { color:inherit!important; }
  .tram-pin { border-radius:50%;display:flex;align-items:center;justify-content:center;font-family:-apple-system,BlinkMacSystemFont,sans-serif;font-weight:800;color:#fff;box-shadow:0 2px 8px rgba(0,0,0,0.50),0 0 0 2px rgba(255,255,255,0.20);transition:transform 0.15s ease;cursor:pointer; }
  .tram-pin:hover { transform:scale(1.25); }
`;

// ── Icons ────────────────────────────────────────────────────────

function makeStopIcon(line: string): L.DivIcon {
  const c = LINE_COLORS[line] ?? "#607D8B";
  const l = line === "red" ? "R" : line === "green" ? "G" : "T";
  return L.divIcon({
    html: `<div class="tram-pin" style="width:22px;height:22px;background:${c};font-size:9px;">${l}</div>`,
    className: "",
    iconSize: [22, 22],
    iconAnchor: [11, 11],
    popupAnchor: [0, -14],
  });
}

function makeUsageIcon(pax: number, maxPax: number, line: string): L.DivIcon {
  const ratio = maxPax > 0 ? pax / maxPax : 0;
  const size = Math.round(14 + ratio * 28);
  let color: string;
  if (line === "red") {
    const g = Math.round(200 - ratio * 190);
    color = `rgb(220,${g},${g})`;
  } else {
    const r = Math.round(200 - ratio * 180);
    const b = Math.round(200 - ratio * 170);
    color = `rgb(${r},${Math.round(180 - ratio * 30)},${b})`;
  }
  const letter = line === "red" ? "R" : "G";
  return L.divIcon({
    html: `<div class="tram-pin" style="width:${size}px;height:${size}px;background:${color};font-size:${Math.max(8, size * 0.3)}px;">${letter}</div>`,
    className: "",
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2 - 4],
  });
}

function makeDelayIcon(avgDelay: number): L.DivIcon {
  const size = Math.min(16 + avgDelay * 2, 40);
  const c = avgDelay >= 10 ? "#DC2626" : avgDelay >= 5 ? "#d29922" : "#1565C0";
  return L.divIcon({
    html: `<div class="tram-pin" style="width:${size}px;height:${size}px;background:${c};font-size:${Math.max(8, size * 0.35)}px;">+${Math.round(avgDelay)}</div>`,
    className: "",
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2 - 4],
  });
}

// ── Demand colour helpers (simulation tab) ────────────────────────

/** hue 120 (green) → 0 (red) based on normalised demand score 0–1 */
function demandColor(score: number): string {
  const hue = Math.round((1 - score) * 120);
  return `hsl(${hue}, 80%, 42%)`;
}

function makeDemandIcon(score: number, affected: boolean): L.DivIcon {
  const color = demandColor(score);
  const ring = affected ? `box-shadow:0 0 0 3px #fff,0 0 0 5px ${color};` : "";
  return L.divIcon({
    html: `<div class="tram-pin" style="width:20px;height:20px;background:${color};border:2px solid rgba(255,255,255,0.85);${ring}"></div>`,
    className: "",
    iconSize: [20, 20],
    iconAnchor: [10, 10],
    popupAnchor: [0, -14],
  });
}

// ── Map controller ───────────────────────────────────────────────

function MapController({
  target,
}: {
  target: { center: [number, number]; id: number } | null;
}) {
  const map = useMap();
  useEffect(() => {
    if (target)
      map.flyTo(target.center, 15, { duration: 1.2, easeLinearity: 0.25 });
  }, [target]); // eslint-disable-line react-hooks/exhaustive-deps
  return null;
}

// ── Helpers ──────────────────────────────────────────────────────

function buildStopMap(forecasts: TramLiveForecast[]) {
  const map = new Map<
    string,
    {
      lat: number;
      lon: number;
      name: string;
      line: string;
      forecasts: TramLiveForecast[];
    }
  >();
  for (const f of forecasts) {
    if (f.lat == null || f.lon == null) continue;
    if (!map.has(f.stopId))
      map.set(f.stopId, {
        lat: f.lat,
        lon: f.lon,
        name: f.stopName,
        line: f.line,
        forecasts: [],
      });
    map.get(f.stopId)!.forecasts.push(f);
  }
  const s = (a: TramLiveForecast, b: TramLiveForecast) =>
    (a.dueMins ?? 999) - (b.dueMins ?? 999);
  for (const stop of map.values()) {
    stop.forecasts = [
      ...stop.forecasts
        .filter((f) => f.direction.toLowerCase() === "inbound")
        .toSorted(s)
        .slice(0, 2),
      ...stop.forecasts
        .filter((f) => f.direction.toLowerCase() === "outbound")
        .toSorted(s)
        .slice(0, 2),
    ];
  }
  return [...map.values()];
}

// ── KPI strip ────────────────────────────────────────────────────

function KpiStrip({
  kpis,
  delayCount,
}: {
  kpis: { totalStops: number; activeForecastCount: number; avgDueMins: number };
  delayCount: number;
}) {
  const items = [
    {
      icon: <LocationOnIcon sx={{ fontSize: 14 }} />,
      label: "Stops",
      value: kpis.totalStops,
      color: "#00ACC1",
    },
    {
      icon: <TramIcon sx={{ fontSize: 14 }} />,
      label: "Forecasts",
      value: kpis.activeForecastCount,
      color: "#2ea043",
    },
    {
      icon: <AccessTimeIcon sx={{ fontSize: 14 }} />,
      label: "Avg Due",
      value: `${kpis.avgDueMins.toFixed(1)}m`,
      color: kpis.avgDueMins <= 10 ? "#2ea043" : "#d29922",
    },
    {
      icon: <WarningAmberIcon sx={{ fontSize: 14 }} />,
      label: "Delays",
      value: delayCount,
      color: delayCount === 0 ? "#2ea043" : "#DC2626",
    },
  ];
  return (
    <Box sx={{ display: "flex", gap: 1.5, px: 2, py: 1.5, flexWrap: "wrap" }}>
      {items.map((item) => (
        <Box
          key={item.label}
          sx={{ display: "flex", alignItems: "center", gap: 0.5 }}
        >
          <Box sx={{ color: item.color }}>{item.icon}</Box>
          <Typography
            variant="caption"
            sx={{ color: "text.secondary", fontSize: "0.65rem" }}
          >
            {item.label}
          </Typography>
          <Typography
            variant="caption"
            fontWeight={700}
            sx={{ fontSize: "0.8rem" }}
          >
            {item.value}
          </Typography>
        </Box>
      ))}
    </Box>
  );
}

// ── Main Component ───────────────────────────────────────────────

export const TramDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [panelOpen, setPanelOpen] = useState(true);
  const [lineFilter, setLineFilter] = useState<LineFilter>("");
  const [search, setSearch] = useState("");
  const [flyTarget, setFlyTarget] = useState<{
    center: [number, number];
    id: number;
  } | null>(null);
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null);
  const [timePeriod, setTimePeriod] = useState("morning");

  // Simulation state
  const [simLine, setSimLine] = useState<"red" | "green">("red");
  const [simExtraTrams, setSimExtraTrams] = useState(0);
  const [simOrigin, setSimOrigin] = useState<TramStopDemand | null>(null);
  const [simDest, setSimDest] = useState<TramStopDemand | null>(null);
  const [simTimePeriod, setSimTimePeriod] = useState("morning");
  const [simResult, setSimResult] = useState<{
    baseDemand: TramStopDemand[];
    simulatedDemand: TramStopDemand[];
    affectedStopIds: string[];
  } | null>(null);

  const theme = useAppSelector((state) => state.ui.theme);
  const { data: kpis } = useTramKpis();
  const { data: liveForecasts, isLoading, error } = useTramLiveForecasts();
  const { data: delays } = useTramDelays();
  const selectedPeriod =
    TIME_PERIODS.find((p) => p.key === timePeriod) ?? TIME_PERIODS[1];
  const { data: stopUsage } = useTramStopUsage(
    selectedPeriod.startHour,
    selectedPeriod.endHour,
  );
  const { data: commonDelays } = useTramCommonDelays();
  const simPeriod =
    TIME_PERIODS.find((p) => p.key === simTimePeriod) ?? TIME_PERIODS[1];
  const { data: stopDemandData = [] } = useTramStopDemand(
    simPeriod.startHour,
    simPeriod.endHour,
  );
  const simulateMutation = useSimulateTramDemand();
  const { data: rawRecommendations } = useTramRecommendations();

  // Parse the recommendation JSON strings into typed items
  const recommendations = useMemo(() => {
    if (!rawRecommendations || rawRecommendations.length === 0) return [];
    const items: TramRecommendationItem[] = [];
    for (const rec of rawRecommendations) {
      try {
        const parsed = safeJsonParse(
          rec.recommendation,
          tramRecommendationItemsSchema,
        );
        items.push(...parsed);
      } catch {
        // skip malformed entries
      }
    }
    return items;
  }, [rawRecommendations]);

  useEffect(() => {
    const el = document.createElement("style");
    el.dataset["tramUi"] = "1";
    el.innerHTML = INJECTED_CSS;
    document.head.append(el);
    return () => {
      el.remove();
    };
  }, []);

  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const tileAttr =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

  const allStops = useMemo(
    () => buildStopMap(liveForecasts ?? []),
    [liveForecasts],
  );
  const stopCoords = useMemo(() => {
    const c = new Map<string, { lat: number; lon: number }>();
    for (const s of allStops)
      c.set(s.forecasts[0]?.stopId ?? s.name, { lat: s.lat, lon: s.lon });
    return c;
  }, [allStops]);
  const maxPax = useMemo(
    () =>
      Math.max(1, ...(stopUsage ?? []).map((u) => u.estimatedTotalPassengers)),
    [stopUsage],
  );
  const usageByStopId = useMemo(() => {
    const m = new Map<string, TramStopUsage>();
    for (const u of stopUsage ?? []) m.set(u.stopId, u);
    return m;
  }, [stopUsage]);

  // Group delays by stopId — one marker per stop, popup shows all directions
  const delaysByStop = useMemo(() => {
    const m = new Map<string, typeof filteredDelays>();
    for (const d of delays ?? []) {
      const list = m.get(d.stopId) ?? [];
      list.push(d);
      m.set(d.stopId, list);
    }
    return m;
  }, [delays]);

  const filteredForecasts = (() => {
    const base = liveForecasts ?? [];
    const byLine = lineFilter
      ? base.filter((f) => f.line === lineFilter)
      : base;
    const q = search.trim().toLowerCase();
    const bySearch = q
      ? byLine.filter(
          (f) =>
            f.stopName.toLowerCase().includes(q) ||
            f.destination.toLowerCase().includes(q),
        )
      : byLine;
    return bySearch.toSorted((a, b) => (a.dueMins ?? 999) - (b.dueMins ?? 999));
  })();
  const filteredDelays = (() => {
    const base = delays ?? [];
    const byLine = lineFilter
      ? base.filter((d) => d.line === lineFilter)
      : base;
    const q = search.trim().toLowerCase();
    return q
      ? byLine.filter(
          (d) =>
            d.stopName.toLowerCase().includes(q) ||
            d.destination.toLowerCase().includes(q),
        )
      : byLine;
  })();
  const filteredUsage = (() => {
    const base = stopUsage ?? [];
    const byLine = lineFilter
      ? base.filter((u) => u.line === lineFilter)
      : base;
    return search.trim()
      ? byLine.filter((u) =>
          u.stopName.toLowerCase().includes(search.toLowerCase()),
        )
      : byLine;
  })();
  const filteredCommonDelays = (() => {
    const base = commonDelays ?? [];
    const byLine = lineFilter
      ? base.filter((d) => d.line === lineFilter)
      : base;
    return search.trim()
      ? byLine.filter((d) =>
          d.stopName.toLowerCase().includes(search.toLowerCase()),
        )
      : byLine;
  })();
  const filteredRecommendations = (() => {
    const base = recommendations ?? [];
    const byLine = lineFilter
      ? base.filter((r) => r.Attributes.line === lineFilter)
      : base;
    return search.trim()
      ? byLine.filter((r) =>
          r.Attributes.description.toLowerCase().includes(search.toLowerCase()),
        )
      : byLine;
  })();

  const handleStopClick = useCallback(
    (lat: number, lon: number, stopId: string) => {
      setSelectedStopId(stopId);
      setFlyTarget({ center: [lat, lon], id: Date.now() });
    },
    [],
  );

  // Simulation helpers
  const activeDemand: TramStopDemand[] = useMemo(
    () => simResult?.simulatedDemand ?? stopDemandData,
    [simResult, stopDemandData],
  );
  const affectedSet = useMemo(
    () => new Set(simResult?.affectedStopIds),
    [simResult],
  );
  const simMetrics = useMemo(() => {
    if (!simResult || simResult.affectedStopIds.length === 0) return null;
    const baseMap = new Map(simResult.baseDemand.map((d) => [d.stopId, d]));
    let totalRelief = 0;
    let peakStop: TramStopDemand | null = null;
    let peakRelief = 0;
    let validCount = 0;
    for (const id of simResult.affectedStopIds) {
      const base = baseMap.get(id);
      const sim = simResult.simulatedDemand.find((d) => d.stopId === id);
      if (!base || !sim || base.demandScore === 0) continue;
      const relief =
        ((base.demandScore - sim.demandScore) / base.demandScore) * 100;
      totalRelief += relief;
      if (relief > peakRelief) {
        peakRelief = relief;
        peakStop = sim;
      }
      validCount++;
    }
    return {
      count: simResult.affectedStopIds.length,
      avgReliefPct: validCount > 0 ? totalRelief / validCount : 0,
      peakStop,
      peakRelief,
    };
  }, [simResult]);

  const handleSimulate = () => {
    if (!simOrigin || !simDest) return;
    simulateMutation.reset();
    simulateMutation.mutate(
      {
        line: simLine,
        extraTrams: simExtraTrams,
        originStopId: simOrigin.stopId,
        destinationStopId: simDest.stopId,
        startHour: simPeriod.startHour,
        endHour: simPeriod.endHour,
      },
      {
        onSuccess: (res) =>
          setSimResult({
            simulatedDemand: res.simulatedDemand,
            baseDemand: res.baseDemand,
            affectedStopIds: res.affectedStopIds,
          }),
      },
    );
  };

  // Stops available for origin/destination selection — filtered by selected line
  const simLineStops = useMemo(
    () =>
      stopDemandData
        .filter((s) => s.line === simLine)
        .toSorted((a, b) => a.stopName.localeCompare(b.stopName)),
    [stopDemandData, simLine],
  );

  // Validation: origin and destination must be different
  const simCorridorError =
    simOrigin && simDest && simOrigin.stopId === simDest.stopId
      ? "Origin and destination must be different"
      : null;

  const activeTab = (
    [
      "live",
      "delays",
      "usage",
      "commonDelays",
      "simulation",
      "recommendations",
    ] as const
  )[tabValue];
  const panelWidth = 400;

  if (isLoading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100%",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map */}
      <MapContainer
        center={DUBLIN_CENTER}
        zoom={12}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
        className={activeTab === "simulation" ? "tram-sim-popup" : undefined}
      >
        <TileLayer attribution={tileAttr} url={tileUrl} />
        <MapController target={flyTarget} />

        {/* Live + Recommendations → standard markers */}
        {(activeTab === "live" || activeTab === "recommendations") &&
          allStops.map((stop) => (
            <Marker
              key={stop.name}
              position={[stop.lat, stop.lon]}
              icon={makeStopIcon(stop.line)}
              eventHandlers={{
                click: () =>
                  handleStopClick(
                    stop.lat,
                    stop.lon,
                    stop.forecasts[0]?.stopId ?? stop.name,
                  ),
              }}
            >
              <Popup>
                <Box sx={{ minWidth: 180 }}>
                  <Typography
                    fontWeight={700}
                    sx={{ fontSize: "0.95rem", color: "#e6edf3", mb: 0.5 }}
                  >
                    {stop.name}
                  </Typography>
                  <Chip
                    size="small"
                    label={`${stop.line} line`}
                    sx={{
                      fontSize: "0.65rem",
                      height: 18,
                      textTransform: "capitalize",
                      bgcolor: LINE_COLORS[stop.line] + "22",
                      color: LINE_COLORS[stop.line],
                      border: `1px solid ${LINE_COLORS[stop.line]}44`,
                      mb: 0.75,
                    }}
                  />
                  {stop.forecasts.slice(0, 4).map((f, i) => (
                    <Typography
                      key={i}
                      sx={{ fontSize: "0.72rem", color: "#c9d1d9" }}
                    >
                      → {f.destination}: <strong>{f.dueMins ?? "—"} min</strong>{" "}
                      <span style={{ color: "#8b949e" }}>({f.direction})</span>
                    </Typography>
                  ))}
                </Box>
              </Popup>
            </Marker>
          ))}

        {/* Delays → faded base markers + one bright marker per delayed stop */}
        {activeTab === "delays" && (
          <>
            {allStops.map((stop) => (
              <Marker
                key={`base-${stop.name}`}
                position={[stop.lat, stop.lon]}
                icon={makeStopIcon(stop.line)}
                opacity={0.35}
                eventHandlers={{
                  click: () =>
                    handleStopClick(
                      stop.lat,
                      stop.lon,
                      stop.forecasts[0]?.stopId ?? stop.name,
                    ),
                }}
              >
                <Popup>
                  <Box sx={{ minWidth: 160 }}>
                    <Typography
                      fontWeight={700}
                      sx={{ fontSize: "0.9rem", color: "#e6edf3" }}
                    >
                      {stop.name}
                    </Typography>
                    <Typography sx={{ fontSize: "0.7rem", color: "#8b949e" }}>
                      {stop.line} line · No delays
                    </Typography>
                  </Box>
                </Popup>
              </Marker>
            ))}
            {[...delaysByStop.entries()].map(([stopId, stopDelays]) => {
              const coords = stopCoords.get(stopId);
              if (!coords) return null;
              const worstDelay = Math.max(
                ...stopDelays.map((d) => d.delayMins),
              );
              const size = Math.min(18 + worstDelay * 1.5, 36);
              const c =
                worstDelay >= 10
                  ? "#DC2626"
                  : worstDelay >= 5
                    ? "#d29922"
                    : "#FF6B35";
              const icon = L.divIcon({
                html: `<div class="tram-pin" style="width:${size}px;height:${size}px;background:${c};font-size:${Math.max(8, size * 0.32)}px;">+${worstDelay}</div>`,
                className: "",
                iconSize: [size, size],
                iconAnchor: [size / 2, size / 2],
                popupAnchor: [0, -size / 2 - 4],
              });
              return (
                <Marker
                  key={`delay-${stopId}`}
                  position={[coords.lat, coords.lon]}
                  icon={icon}
                  eventHandlers={{
                    click: () =>
                      handleStopClick(coords.lat, coords.lon, stopId),
                  }}
                >
                  <Popup>
                    <Box sx={{ minWidth: 200 }}>
                      <Typography
                        fontWeight={700}
                        sx={{ fontSize: "0.95rem", color: "#e6edf3", mb: 0.75 }}
                      >
                        {stopDelays[0].stopName}
                      </Typography>
                      {stopDelays.map((d, i) => (
                        <Box
                          key={i}
                          sx={{
                            mb: 0.5,
                            pb: 0.5,
                            borderBottom:
                              i < stopDelays.length - 1
                                ? "1px solid rgba(255,255,255,0.08)"
                                : "none",
                          }}
                        >
                          <Typography
                            sx={{
                              fontSize: "0.72rem",
                              color: "#f85149",
                              fontWeight: 600,
                            }}
                          >
                            {d.direction}: +{d.delayMins} min
                          </Typography>
                          <Typography
                            sx={{ fontSize: "0.65rem", color: "#8b949e" }}
                          >
                            → {d.destination} · Due {d.dueMins}m
                          </Typography>
                        </Box>
                      ))}
                    </Box>
                  </Popup>
                </Marker>
              );
            })}
          </>
        )}

        {/* Usage → sized markers */}
        {activeTab === "usage" &&
          allStops.map((stop) => {
            const sid = stop.forecasts[0]?.stopId ?? stop.name;
            const pax = usageByStopId.get(sid)?.estimatedTotalPassengers ?? 0;
            return (
              <Marker
                key={stop.name}
                position={[stop.lat, stop.lon]}
                icon={makeUsageIcon(pax, maxPax, stop.line)}
                eventHandlers={{
                  click: () => handleStopClick(stop.lat, stop.lon, sid),
                }}
              >
                <Popup>
                  <Box sx={{ minWidth: 180 }}>
                    <Typography
                      fontWeight={700}
                      sx={{ fontSize: "0.95rem", color: "#e6edf3", mb: 0.5 }}
                    >
                      {stop.name}
                    </Typography>
                    <Chip
                      size="small"
                      label={selectedPeriod.label}
                      sx={{
                        fontSize: "0.6rem",
                        height: 18,
                        bgcolor: "rgba(21,101,192,0.2)",
                        color: "#90caf9",
                        border: "1px solid #1565C044",
                        mb: 0.75,
                      }}
                    />
                    {usageByStopId.get(sid) ? (
                      <>
                        <Typography
                          sx={{ fontSize: "0.72rem", color: "#c9d1d9" }}
                        >
                          Est. passengers:{" "}
                          <strong>{pax.toLocaleString()}</strong>
                        </Typography>
                        <Typography
                          sx={{ fontSize: "0.68rem", color: "#2ea043" }}
                        >
                          ↓{" "}
                          {usageByStopId
                            .get(sid)!
                            .estimatedInboundPassengers.toLocaleString()}{" "}
                          inbound
                        </Typography>
                        <Typography
                          sx={{ fontSize: "0.68rem", color: "#d29922" }}
                        >
                          ↑{" "}
                          {usageByStopId
                            .get(sid)!
                            .estimatedOutboundPassengers.toLocaleString()}{" "}
                          outbound
                        </Typography>
                      </>
                    ) : (
                      <Typography
                        sx={{ fontSize: "0.72rem", color: "#484f58" }}
                      >
                        No usage data
                      </Typography>
                    )}
                  </Box>
                </Popup>
              </Marker>
            );
          })}

        {/* Common Delays → delay-sized markers */}
        {activeTab === "commonDelays" &&
          (commonDelays ?? [])
            .filter((d) => d.lat != null && d.lon != null)
            .map((d) => (
              <Marker
                key={d.stopId}
                position={[d.lat!, d.lon!]}
                icon={makeDelayIcon(d.avgDelayMins)}
                eventHandlers={{
                  click: () => handleStopClick(d.lat!, d.lon!, d.stopId),
                }}
              >
                <Popup>
                  <Box sx={{ minWidth: 180 }}>
                    <Typography
                      fontWeight={700}
                      sx={{ fontSize: "0.95rem", color: "#e6edf3", mb: 0.5 }}
                    >
                      {d.stopName}
                    </Typography>
                    <Typography sx={{ fontSize: "0.72rem", color: "#c9d1d9" }}>
                      Avg delay: <strong>+{d.avgDelayMins} min</strong>
                    </Typography>
                    <Typography sx={{ fontSize: "0.68rem", color: "#8b949e" }}>
                      Max: +{d.maxDelayMins} min · {d.delayCount} records
                    </Typography>
                  </Box>
                </Popup>
              </Marker>
            ))}

        {/* Simulation → demand-coloured markers */}
        {activeTab === "simulation" &&
          activeDemand
            .filter((s) => s.lat != null && s.lon != null)
            .map((s) => {
              const isAffected = affectedSet.has(s.stopId);
              const base = simResult?.baseDemand.find(
                (b) => b.stopId === s.stopId,
              );
              return (
                <Marker
                  key={s.stopId}
                  position={[s.lat!, s.lon!]}
                  icon={makeDemandIcon(s.demandScore, isAffected)}
                  eventHandlers={{
                    click: () => handleStopClick(s.lat!, s.lon!, s.stopId),
                  }}
                >
                  <Popup>
                    <Box sx={{ minWidth: 200 }}>
                      <Typography
                        fontWeight={700}
                        sx={{ fontSize: "1rem", color: "#ffffff !important", mb: 0.5, display: "block" }}
                      >
                        {s.stopName}
                      </Typography>
                      <Chip
                        size="small"
                        label={`${s.line} line`}
                        sx={{
                          fontSize: "0.62rem",
                          height: 16,
                          textTransform: "capitalize",
                          bgcolor: LINE_COLORS[s.line] + "22",
                          color: LINE_COLORS[s.line],
                          border: `1px solid ${LINE_COLORS[s.line]}44`,
                          mb: 0.75,
                        }}
                      />
                      {base && isAffected ? (
                        <>
                          <Typography
                            sx={{ fontSize: "0.72rem", color: "#374151" }}
                          >
                            Before:{" "}
                            <strong>
                              {(base.demandScore * 100).toFixed(1)}%
                            </strong>
                          </Typography>
                          <Typography
                            sx={{ fontSize: "0.72rem", color: "#059669" }}
                          >
                            After:{" "}
                            <strong>{(s.demandScore * 100).toFixed(1)}%</strong>{" "}
                            (
                            {(
                              ((base.demandScore - s.demandScore) /
                                base.demandScore) *
                              100
                            ).toFixed(1)}
                            % relief)
                          </Typography>
                          <Typography
                            sx={{
                              fontSize: "0.68rem",
                              color: "#6b7280",
                              mt: 0.5,
                            }}
                          >
                            Trips: {base.tripCount} → {s.tripCount}
                          </Typography>
                        </>
                      ) : (
                        <Typography
                          sx={{ fontSize: "0.72rem", color: "#374151" }}
                        >
                          Demand:{" "}
                          <strong>{(s.demandScore * 100).toFixed(1)}%</strong>
                        </Typography>
                      )}
                    </Box>
                  </Popup>
                </Marker>
              );
            })}
      </MapContainer>

      {error && (
        <Alert
          severity="error"
          sx={{
            position: "absolute",
            top: 16,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1000,
            borderRadius: 2,
          }}
        >
          Failed to load tram data
        </Alert>
      )}

      {/* Toggle button */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
            zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* ── Floating right panel (matches CycleDashboard) ── */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
            bottom: 16,
            width: panelWidth,
            zIndex: 1000,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          {/* Header */}
          <Box
            sx={{
              p: 2,
              pb: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <Typography variant="h5">Luas Tram Network</Typography>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* KPI strip */}
          {kpis && <KpiStrip kpis={kpis} delayCount={filteredDelays.length} />}

          {/* Tabs */}
          <Tabs
            value={tabValue}
            onChange={(_, v) => setTabValue(v)}
            variant="scrollable"
            scrollButtons="auto"
            sx={{
              minHeight: 36,
              px: 1,
              "& .MuiTab-root": {
                minHeight: 36,
                fontSize: "0.72rem",
                textTransform: "none",
                minWidth: 60,
              },
            }}
          >
            <Tab label={`Live (${filteredForecasts.length})`} />
            <Tab label={`Delays (${filteredDelays.length})`} />
            <Tab label={`Usage (${filteredUsage.length})`} />
            <Tab label={`Common Delays (${filteredCommonDelays.length})`} />
            <Tab label="Simulation" />
            <Tab label={`Recommendations (${filteredRecommendations.length})`} />
          </Tabs>

          {/* Line filter + Search — hidden on simulation tab */}
          {tabValue !== 4 && (
            <>
              <Box
                sx={{
                  px: 2,
                  pt: 1.5,
                  display: "flex",
                  gap: 1,
                  alignItems: "center",
                }}
              >
                {(["", "red", "green"] as LineFilter[]).map((key) => {
                  const active = lineFilter === key;
                  const label =
                    key === "" ? "All" : key === "red" ? "Red" : "Green";
                  return (
                    <Chip
                      key={key || "all"}
                      size="small"
                      label={label}
                      onClick={() => setLineFilter(key)}
                      sx={{
                        fontSize: "0.7rem",
                        cursor: "pointer",
                        bgcolor: active
                          ? key === ""
                            ? "primary.main"
                            : LINE_COLORS[key]
                          : "action.hover",
                        color: active ? "#fff" : "text.secondary",
                      }}
                    />
                  );
                })}
              </Box>
              <Box sx={{ px: 2, py: 1 }}>
                <TextField
                  size="small"
                  fullWidth
                  placeholder="Search stops…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  slotProps={{
                    input: {
                      startAdornment: (
                        <InputAdornment position="start">
                          <SearchIcon sx={{ fontSize: 16 }} />
                        </InputAdornment>
                      ),
                      sx: { fontSize: "0.85rem", borderRadius: 2 },
                    },
                  }}
                />
              </Box>
            </>
          )}

          {/* Time period dropdown — Usage tab only */}
          {tabValue === 2 && (
            <Box sx={{ px: 2, pb: 1 }}>
              <Select
                value={timePeriod}
                onChange={(e: SelectChangeEvent) =>
                  setTimePeriod(e.target.value)
                }
                size="small"
                fullWidth
                sx={{ fontSize: "0.8rem", borderRadius: 2 }}
              >
                {TIME_PERIODS.map((p) => (
                  <MenuItem key={p.key} value={p.key}>
                    {p.label}
                  </MenuItem>
                ))}
              </Select>
            </Box>
          )}

          {/* ── Scrollable list ── */}
          <Box sx={{ flex: 1, overflow: "auto", px: 1, pt: 0.5 }}>
            {/* LIVE */}
            {tabValue === 0 &&
              filteredForecasts.map((f, idx) => {
                const sel = selectedStopId === f.stopId;
                const noService = f.dueMins === null;
                const subtitle =
                  noService && f.message
                    ? f.message
                    : `${f.direction} → ${f.destination}`;
                return (
                  <ListItemButton
                    key={`${f.stopId}-${f.direction}-${idx}`}
                    onClick={() =>
                      f.lat != null &&
                      f.lon != null &&
                      handleStopClick(f.lat, f.lon, f.stopId)
                    }
                    disabled={f.lat == null}
                    selected={sel}
                    sx={{ py: 0.75, px: 1.5, borderRadius: 1.5, mb: 0.25 }}
                  >
                    <Box
                      sx={{
                        width: 20,
                        height: 20,
                        borderRadius: "50%",
                        bgcolor: LINE_COLORS[f.line] ?? "#607D8B",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                        mr: 1.5,
                      }}
                    >
                      <Typography
                        sx={{ fontSize: 8, fontWeight: 800, color: "#fff" }}
                      >
                        {f.line === "red" ? "R" : "G"}
                      </Typography>
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography
                        noWrap
                        sx={{
                          fontSize: "0.82rem",
                          fontWeight: sel ? 600 : 400,
                        }}
                      >
                        {f.stopName}
                      </Typography>
                      <Typography
                        noWrap
                        variant="caption"
                        sx={{
                          fontSize: "0.68rem",
                          color: noService ? "error.main" : "text.secondary",
                        }}
                      >
                        {subtitle}
                      </Typography>
                    </Box>
                    <Chip
                      size="small"
                      label={noService ? "No Service" : `${f.dueMins}m`}
                      sx={{
                        fontSize: "0.6rem",
                        height: 20,
                        bgcolor: noService
                          ? "error.main"
                          : (f.dueMins ?? 999) <= 3
                            ? "success.main"
                            : "warning.main",
                        color: "#fff",
                        fontWeight: 700,
                      }}
                    />
                  </ListItemButton>
                );
              })}

            {/* DELAYS */}
            {tabValue === 1 &&
              filteredDelays.map((d, idx) => (
                <ListItemButton
                  key={`${d.stopId}-${d.direction}-${idx}`}
                  onClick={() => {
                    const c = stopCoords.get(d.stopId);
                    if (c) handleStopClick(c.lat, c.lon, d.stopId);
                  }}
                  sx={{ py: 0.75, px: 1.5, borderRadius: 1.5, mb: 0.25 }}
                >
                  <Box
                    sx={{
                      width: 20,
                      height: 20,
                      borderRadius: "50%",
                      bgcolor: LINE_COLORS[d.line] ?? "#607D8B",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                      mr: 1.5,
                    }}
                  >
                    <Typography
                      sx={{ fontSize: 8, fontWeight: 800, color: "#fff" }}
                    >
                      {d.line === "red" ? "R" : "G"}
                    </Typography>
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                      noWrap
                      sx={{ fontSize: "0.82rem", fontWeight: 500 }}
                    >
                      {d.stopName}
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ fontSize: "0.68rem" }}
                    >
                      {d.direction} → {d.destination} · Due {d.dueMins}m
                    </Typography>
                  </Box>
                  <Chip
                    size="small"
                    label={`+${d.delayMins}m`}
                    sx={{
                      fontSize: "0.62rem",
                      height: 20,
                      bgcolor:
                        d.delayMins >= 10
                          ? "error.main"
                          : d.delayMins >= 5
                            ? "warning.main"
                            : "action.hover",
                      color: d.delayMins >= 5 ? "#fff" : "text.primary",
                      fontWeight: 700,
                    }}
                  />
                </ListItemButton>
              ))}

            {/* USAGE */}
            {tabValue === 2 &&
              filteredUsage.map((u) => {
                const sel = selectedStopId === u.stopId;
                return (
                  <ListItemButton
                    key={u.stopId}
                    onClick={() => {
                      if (u.lat != null && u.lon != null)
                        handleStopClick(u.lat, u.lon, u.stopId);
                      else {
                        const c = stopCoords.get(u.stopId);
                        if (c) handleStopClick(c.lat, c.lon, u.stopId);
                      }
                    }}
                    selected={sel}
                    sx={{ py: 0.75, px: 1.5, borderRadius: 1.5, mb: 0.25 }}
                  >
                    <Box
                      sx={{
                        width: 20,
                        height: 20,
                        borderRadius: "50%",
                        bgcolor: LINE_COLORS[u.line] ?? "#607D8B",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                        mr: 1.5,
                      }}
                    >
                      <Typography
                        sx={{ fontSize: 8, fontWeight: 800, color: "#fff" }}
                      >
                        {u.line === "red" ? "R" : "G"}
                      </Typography>
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography
                        noWrap
                        sx={{
                          fontSize: "0.82rem",
                          fontWeight: sel ? 600 : 400,
                        }}
                      >
                        {u.stopName}
                      </Typography>
                      <Box sx={{ display: "flex", gap: 1 }}>
                        <Typography
                          variant="caption"
                          sx={{
                            fontSize: "0.65rem",
                            color: "success.main",
                            display: "flex",
                            alignItems: "center",
                            gap: 0.25,
                          }}
                        >
                          <ArrowDownwardIcon sx={{ fontSize: 10 }} />
                          {u.estimatedInboundPassengers.toLocaleString()}
                        </Typography>
                        <Typography
                          variant="caption"
                          sx={{
                            fontSize: "0.65rem",
                            color: "warning.main",
                            display: "flex",
                            alignItems: "center",
                            gap: 0.25,
                          }}
                        >
                          <ArrowUpwardIcon sx={{ fontSize: 10 }} />
                          {u.estimatedOutboundPassengers.toLocaleString()}
                        </Typography>
                      </Box>
                    </Box>
                    <Chip
                      size="small"
                      icon={<PeopleIcon sx={{ fontSize: "11px !important" }} />}
                      label={u.estimatedTotalPassengers.toLocaleString()}
                      sx={{
                        fontSize: "0.62rem",
                        height: 20,
                        fontWeight: 700,
                        "& .MuiChip-icon": { color: "inherit", ml: "4px" },
                      }}
                    />
                  </ListItemButton>
                );
              })}

            {/* COMMON DELAYS */}
            {tabValue === 3 &&
              filteredCommonDelays.map((d) => {
                const sel = selectedStopId === d.stopId;
                return (
                  <ListItemButton
                    key={d.stopId}
                    onClick={() => {
                      if (d.lat != null && d.lon != null)
                        handleStopClick(d.lat, d.lon, d.stopId);
                      else {
                        const c = stopCoords.get(d.stopId);
                        if (c) handleStopClick(c.lat, c.lon, d.stopId);
                      }
                    }}
                    selected={sel}
                    sx={{ py: 0.75, px: 1.5, borderRadius: 1.5, mb: 0.25 }}
                  >
                    <Box
                      sx={{
                        width: 20,
                        height: 20,
                        borderRadius: "50%",
                        bgcolor: LINE_COLORS[d.line] ?? "#607D8B",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                        mr: 1.5,
                      }}
                    >
                      <Typography
                        sx={{ fontSize: 8, fontWeight: 800, color: "#fff" }}
                      >
                        {d.line === "red" ? "R" : "G"}
                      </Typography>
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography
                        noWrap
                        sx={{
                          fontSize: "0.82rem",
                          fontWeight: sel ? 600 : 400,
                        }}
                      >
                        {d.stopName}
                      </Typography>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ fontSize: "0.65rem" }}
                      >
                        {d.delayCount} records · max +{d.maxDelayMins}m
                      </Typography>
                    </Box>
                    <Chip
                      size="small"
                      icon={
                        <HistoryIcon sx={{ fontSize: "11px !important" }} />
                      }
                      label={`+${d.avgDelayMins}m`}
                      sx={{
                        fontSize: "0.62rem",
                        height: 20,
                        bgcolor:
                          d.avgDelayMins >= 10
                            ? "error.main"
                            : d.avgDelayMins >= 5
                              ? "warning.main"
                              : "action.hover",
                        color: d.avgDelayMins >= 5 ? "#fff" : "text.primary",
                        fontWeight: 700,
                        "& .MuiChip-icon": { color: "inherit", ml: "4px" },
                      }}
                    />
                  </ListItemButton>
                );
              })}

            {/* SIMULATION */}
            {tabValue === 4 && (
              <Box sx={{ px: 1.5, pt: 1 }}>
                {/* Controls */}
                <Paper
                  variant="outlined"
                  sx={{ p: 1.5, mb: 1.5, borderRadius: 2 }}
                >
                  <Typography
                    variant="caption"
                    sx={{
                      fontSize: "0.7rem",
                      textTransform: "uppercase",
                      letterSpacing: 0.5,
                      color: "text.secondary",
                      display: "block",
                      mb: 1,
                    }}
                  >
                    Simulate New Services
                  </Typography>
                  <FormControl size="small" fullWidth sx={{ mb: 1.5 }}>
                    <InputLabel sx={{ fontSize: "0.8rem" }}>Line</InputLabel>
                    <Select
                      value={simLine}
                      label="Line"
                      onChange={(e) => {
                        setSimLine(e.target.value);
                        setSimResult(null);
                        setSimOrigin(null);
                        setSimDest(null);
                      }}
                      sx={{ fontSize: "0.8rem" }}
                    >
                      <MenuItem value="red">
                        <Box
                          sx={{ display: "flex", alignItems: "center", gap: 1 }}
                        >
                          <Box
                            sx={{
                              width: 10,
                              height: 10,
                              borderRadius: "50%",
                              bgcolor: "#DC2626",
                            }}
                          />
                          Red Line
                        </Box>
                      </MenuItem>
                      <MenuItem value="green">
                        <Box
                          sx={{ display: "flex", alignItems: "center", gap: 1 }}
                        >
                          <Box
                            sx={{
                              width: 10,
                              height: 10,
                              borderRadius: "50%",
                              bgcolor: "#16A34A",
                            }}
                          />
                          Green Line
                        </Box>
                      </MenuItem>
                    </Select>
                  </FormControl>

                  <FormControl size="small" fullWidth sx={{ mb: 1.5 }}>
                    <InputLabel sx={{ fontSize: "0.8rem" }}>
                      Time Period
                    </InputLabel>
                    <Select
                      value={simTimePeriod}
                      label="Time Period"
                      onChange={(e) => {
                        setSimTimePeriod(e.target.value);
                        setSimResult(null);
                      }}
                      sx={{ fontSize: "0.8rem" }}
                    >
                      {TIME_PERIODS.map((p) => (
                        <MenuItem key={p.key} value={p.key}>
                          {p.label}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>

                  <Autocomplete
                    size="small"
                    options={simLineStops}
                    getOptionLabel={(o) => o.stopName}
                    value={simOrigin}
                    onChange={(_, v) => {
                      setSimOrigin(v);
                      setSimResult(null);
                    }}
                    renderInput={(params) => (
                      <TextField {...params} label="Origin (Starting Point)" />
                    )}
                    isOptionEqualToValue={(a, b) => a.stopId === b.stopId}
                    sx={{ mb: 1.5 }}
                  />
                  <Autocomplete
                    size="small"
                    options={simLineStops}
                    getOptionLabel={(o) => o.stopName}
                    value={simDest}
                    onChange={(_, v) => {
                      setSimDest(v);
                      setSimResult(null);
                    }}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Destination (Ending Point)"
                        error={!!simCorridorError}
                        helperText={simCorridorError ?? undefined}
                      />
                    )}
                    isOptionEqualToValue={(a, b) => a.stopId === b.stopId}
                    sx={{ mb: 1.5 }}
                  />

                  <Typography
                    variant="caption"
                    sx={{ fontSize: "0.75rem", color: "text.secondary" }}
                  >
                    {simExtraTrams >= 0 ? "Add" : "Remove"} trams:{" "}
                    <strong style={{ color: simExtraTrams >= 0 ? "inherit" : "#DC2626" }}>
                      {simExtraTrams >= 0 ? `+${simExtraTrams}` : simExtraTrams}
                    </strong>
                  </Typography>
                  <Slider
                    value={simExtraTrams}
                    min={-20}
                    max={20}
                    step={1}
                    marks={[
                      { value: -20, label: "-20" },
                      { value: -10, label: "-10" },
                      { value: 0, label: "0" },
                      { value: 10, label: "+10" },
                      { value: 20, label: "+20" },
                    ]}
                    onChange={(_, v) => {
                      setSimExtraTrams(Array.isArray(v) ? v[0] : v);
                      setSimResult(null);
                    }}
                    sx={{
                      mt: 0.5,
                      mb: 1,
                      "& .MuiSlider-track": {
                        bgcolor: simExtraTrams >= 0 ? "primary.main" : "error.main",
                      },
                      "& .MuiSlider-thumb": {
                        bgcolor: simExtraTrams >= 0 ? "primary.main" : "error.main",
                      },
                    }}
                  />
                  <Button
                    variant="contained"
                    fullWidth
                    size="small"
                    onClick={handleSimulate}
                    disabled={
                      simulateMutation.isPending ||
                      !simOrigin ||
                      !simDest ||
                      simExtraTrams === 0 ||
                      !!simCorridorError
                    }
                    startIcon={
                      simulateMutation.isPending ? (
                        <CircularProgress size={14} color="inherit" />
                      ) : (
                        <SpeedIcon sx={{ fontSize: 16 }} />
                      )
                    }
                  >
                    {simulateMutation.isPending ? "Running…" : "Run Simulation"}
                  </Button>
                  {simulateMutation.isError && (
                    <Alert
                      severity="error"
                      sx={{ mt: 1, fontSize: "0.72rem", py: 0.25 }}
                    >
                      Simulation failed
                    </Alert>
                  )}
                </Paper>

                {/* Results summary */}
                {simResult && simMetrics && (
                  <Paper
                    variant="outlined"
                    sx={{
                      p: 1.5,
                      mb: 1.5,
                      borderRadius: 2,
                      bgcolor: "success.light" + "18",
                    }}
                  >
                    <Typography
                      variant="caption"
                      sx={{
                        fontSize: "0.7rem",
                        textTransform: "uppercase",
                        letterSpacing: 0.5,
                        color: "success.main",
                        display: "block",
                        mb: 0.75,
                        fontWeight: 700,
                      }}
                    >
                      Simulation Results
                    </Typography>
                    <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                      <Box>
                        <Typography
                          sx={{
                            fontSize: "0.65rem",
                            color: "text.secondary",
                            textTransform: "uppercase",
                            letterSpacing: 0.4,
                          }}
                        >
                          Stops affected
                        </Typography>
                        <Typography fontWeight={700} sx={{ fontSize: "1rem" }}>
                          {simMetrics.count}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography
                          sx={{
                            fontSize: "0.65rem",
                            color: "text.secondary",
                            textTransform: "uppercase",
                            letterSpacing: 0.4,
                          }}
                        >
                          Avg relief
                        </Typography>
                        <Typography
                          fontWeight={700}
                          sx={{ fontSize: "1rem", color: "success.main" }}
                        >
                          {simMetrics.avgReliefPct.toFixed(1)}%
                        </Typography>
                      </Box>
                      {simMetrics.peakStop && (
                        <Box>
                          <Typography
                            sx={{
                              fontSize: "0.65rem",
                              color: "text.secondary",
                              textTransform: "uppercase",
                              letterSpacing: 0.4,
                            }}
                          >
                            Peak relief
                          </Typography>
                          <Typography
                            fontWeight={700}
                            sx={{ fontSize: "0.85rem", color: "success.dark" }}
                          >
                            {simMetrics.peakStop.stopName} (
                            {simMetrics.peakRelief.toFixed(1)}%)
                          </Typography>
                        </Box>
                      )}
                    </Box>
                    <Button
                      size="small"
                      variant="outlined"
                      color="inherit"
                      sx={{ mt: 1, fontSize: "0.7rem", py: 0.25 }}
                      onClick={() => setSimResult(null)}
                    >
                      Clear
                    </Button>
                  </Paper>
                )}

                {/* Per-stop demand list */}
                {activeDemand
                  .filter((s) => !simLine || s.line === simLine)
                  .toSorted((a, b) => b.demandScore - a.demandScore)
                  .map((s) => {
                    const base = simResult?.baseDemand.find(
                      (b) => b.stopId === s.stopId,
                    );
                    const relief =
                      base && base.demandScore > 0
                        ? ((base.demandScore - s.demandScore) /
                            base.demandScore) *
                          100
                        : 0;
                    const color = demandColor(s.demandScore);
                    return (
                      <ListItemButton
                        key={s.stopId}
                        onClick={() => {
                          if (s.lat != null && s.lon != null)
                            handleStopClick(s.lat, s.lon, s.stopId);
                        }}
                        selected={selectedStopId === s.stopId}
                        sx={{ py: 0.6, px: 1, borderRadius: 1.5, mb: 0.25 }}
                      >
                        <Box
                          sx={{
                            width: 12,
                            height: 12,
                            borderRadius: "50%",
                            bgcolor: color,
                            flexShrink: 0,
                            mr: 1.5,
                            border: affectedSet.has(s.stopId)
                              ? "2px solid #fff"
                              : "none",
                            boxShadow: affectedSet.has(s.stopId)
                              ? `0 0 0 2px ${color}`
                              : "none",
                          }}
                        />
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography noWrap sx={{ fontSize: "0.8rem" }}>
                            {s.stopName}
                          </Typography>
                        </Box>
                        <Box sx={{ textAlign: "right", flexShrink: 0 }}>
                          <Typography
                            sx={{ fontSize: "0.75rem", fontWeight: 700, color }}
                          >
                            {(s.demandScore * 100).toFixed(0)}%
                          </Typography>
                          {relief > 0 && (
                            <Typography
                              sx={{
                                fontSize: "0.62rem",
                                color: "success.main",
                              }}
                            >
                              -{relief.toFixed(0)}%
                            </Typography>
                          )}
                        </Box>
                      </ListItemButton>
                    );
                  })}

                {activeDemand.length === 0 && (
                  <Box sx={{ py: 4, textAlign: "center" }}>
                    <SpeedIcon
                      sx={{ fontSize: 32, color: "text.disabled", mb: 1 }}
                    />
                    <Typography color="text.secondary" fontSize="0.8rem">
                      Select a line and run the simulation
                    </Typography>
                  </Box>
                )}
              </Box>
            )}

            {/* Empty states */}
            {tabValue === 0 && filteredForecasts.length === 0 && (
              <Box sx={{ py: 4, textAlign: "center" }}>
                <TramIcon
                  sx={{ fontSize: 32, color: "text.disabled", mb: 1 }}
                />
                <Typography color="text.secondary" fontSize="0.8rem">
                  No live forecasts
                </Typography>
              </Box>
            )}
            {tabValue === 1 && filteredDelays.length === 0 && (
              <Box sx={{ py: 4, textAlign: "center" }}>
                <AccessTimeIcon
                  sx={{ fontSize: 32, color: "success.main", mb: 1 }}
                />
                <Typography color="success.main" fontSize="0.8rem">
                  All trams on schedule
                </Typography>
              </Box>
            )}
            {tabValue === 2 && filteredUsage.length === 0 && (
              <Box sx={{ py: 4, textAlign: "center" }}>
                <PeopleIcon
                  sx={{ fontSize: 32, color: "text.disabled", mb: 1 }}
                />
                <Typography color="text.secondary" fontSize="0.8rem">
                  No usage data
                </Typography>
              </Box>
            )}
            {tabValue === 3 && filteredCommonDelays.length === 0 && (
              <Box sx={{ py: 4, textAlign: "center" }}>
                <HistoryIcon
                  sx={{ fontSize: 32, color: "text.disabled", mb: 1 }}
                />
                <Typography color="text.secondary" fontSize="0.8rem">
                  No delay history
                </Typography>
              </Box>
            )}

            {/* RECOMMENDATIONS */}
            {tabValue === 5 &&
              filteredRecommendations.map((r, idx) => {
                const a = r.Attributes;
                const sevColor =
                  a.severity === "high"
                    ? "error.main"
                    : a.severity === "medium"
                      ? "warning.main"
                      : a.severity === "low"
                        ? "info.main"
                        : "#78909C"; // very_low — grey-blue
                const sevLabel =
                  a.severity === "high"
                    ? "High"
                    : a.severity === "medium"
                      ? "Medium"
                      : a.severity === "low"
                        ? "Low"
                        : "Very Low";
                const typeLabel =
                  a.type === "add_frequency"
                    ? "Add Trams"
                    : a.type === "reduce_frequency"
                      ? "Reduce Trams"
                      : a.type === "partial_run"
                        ? "Short Run"
                        : a.type === "monitor"
                          ? "Monitor"
                          : "Rebalance";
                return (
                  <Box
                    key={idx}
                    sx={{
                      mx: 1,
                      mb: 1,
                      p: 1.5,
                      borderRadius: 2,
                      border: "1px solid",
                      borderColor: "divider",
                      bgcolor: "background.paper",
                    }}
                  >
                    <Box
                      sx={{
                        display: "flex",
                        gap: 0.75,
                        alignItems: "center",
                        mb: 0.75,
                        flexWrap: "wrap",
                      }}
                    >
                      <Chip
                        size="small"
                        label={sevLabel}
                        sx={{
                          fontSize: "0.6rem",
                          height: 18,
                          fontWeight: 700,
                          bgcolor: sevColor,
                          color: "#fff",
                        }}
                      />
                      <Chip
                        size="small"
                        label={typeLabel}
                        sx={{
                          fontSize: "0.6rem",
                          height: 18,
                          fontWeight: 600,
                          bgcolor: "action.selected",
                          color: "text.primary",
                        }}
                      />
                      <Chip
                        size="small"
                        label={a.line.charAt(0).toUpperCase() + a.line.slice(1)}
                        sx={{
                          fontSize: "0.6rem",
                          height: 18,
                          bgcolor: LINE_COLORS[a.line]
                            ? LINE_COLORS[a.line] + "22"
                            : "action.hover",
                          color: LINE_COLORS[a.line] ?? "text.primary",
                          border: `1px solid ${(LINE_COLORS[a.line] ?? "#607D8B") + "44"}`,
                        }}
                      />
                      <Chip
                        size="small"
                        label={a.time_label}
                        sx={{
                          fontSize: "0.6rem",
                          height: 18,
                          bgcolor: "action.hover",
                          color: "text.secondary",
                        }}
                      />
                    </Box>
                    <Typography sx={{ fontSize: "0.78rem", lineHeight: 1.5 }}>
                      {a.description}
                    </Typography>
                  </Box>
                );
              })}
            {tabValue === 5 && filteredRecommendations.length === 0 && (
              <Box sx={{ py: 4, textAlign: "center" }}>
                <LightbulbIcon
                  sx={{ fontSize: 32, color: "text.disabled", mb: 1 }}
                />
                <Typography color="text.secondary" fontSize="0.8rem">
                  No recommendations available
                </Typography>
                <Typography
                  color="text.disabled"
                  fontSize="0.7rem"
                  sx={{ mt: 0.5 }}
                >
                  Recommendations are generated when utilisation analysis
                  detects service change opportunities
                </Typography>
              </Box>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};
