/**
 * Tram dashboard — matches CycleDashboard design
 * Right-side floating panel, MUI Tabs, theme-aware
 *
 * 5 tabs: Live | Delays | Usage | Common Delays | Recommendations
 * Map: Live/Delays=standard, Usage=sized markers, CommonDelays=delay-sized markers
 */

import { useState, useEffect, useMemo, useCallback } from "react";
import { DisruptionsTabContent } from "@/components/disruption/DisruptionsTabContent";
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
import LightbulbIcon from "@mui/icons-material/Lightbulb";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import {
  useTramKpis,
  useTramLiveForecasts,
  useTramDelays,
  useTramStopUsage,
  useTramCommonDelays,
  useTramRecommendations,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

import { dashboardApi } from "@/api";
import type { TramAlternativeRoute, TramLiveForecast, TramStopUsage } from "@/types";

interface TramRecommendationItem {
  Attributes: {
    severity: string;
    type: string;
    line: string;
    time_label: string;
    description: string;
  };
}

// ── Constants ────────────────────────────────────────────────────

const DUBLIN_CENTER: [number, number] = [53.3398, -6.2603];
type LineFilter = "" | "red" | "green";

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
  .tram-map .leaflet-popup-content-wrapper {
    background: rgba(13,17,23,0.96) !important;
    color: #e6edf3 !important;
    border: 1px solid rgba(255,255,255,0.10) !important;
    border-radius: 12px !important;
    box-shadow: 0 8px 32px rgba(0,0,0,0.55) !important;
  }
  .tram-map .leaflet-popup-tip { background: rgba(13,17,23,0.96) !important; }
  .tram-map .leaflet-popup-close-button {
    color: #8b949e !important; top: 8px !important; right: 8px !important;
  }
  .tram-map .leaflet-popup-content { margin: 14px 16px !important; }
  .tram-pin {
    border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-family: -apple-system, BlinkMacSystemFont, sans-serif;
    font-weight: 800; color: #fff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.50), 0 0 0 2px rgba(255,255,255,0.20);
    transition: transform 0.15s ease; cursor: pointer;
  }
  .tram-pin:hover { transform: scale(1.25); }
`;

// ── Icons ────────────────────────────────────────────────────────

function makeStopIcon(line: string, disrupted = false): L.DivIcon {
  const color = disrupted ? "#f97316" : (LINE_COLORS[line] ?? "#607D8B");
  const letter = line === "red" ? "R" : line === "green" ? "G" : "T";
  return L.divIcon({
    html: `<div class="tram-pin" style="width:22px;height:22px;background:${color};font-size:9px;">${letter}</div>`,
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
const DISRUPTION_KEYWORDS = [
  "not in service",
  "disruption",
  "suspended",
  "suspension",
  "delay",
  "fault",
  "no service",
  "terminated",
  "partial",
];

const isDisrupted = (forecasts: TramLiveForecast[]) =>
  forecasts.some((f) =>
    DISRUPTION_KEYWORDS.some((kw) => f.message?.toLowerCase().includes(kw)),
  );

const iconFor = (type: string) =>
  type === "bus" ? "🚌" : type === "rail" ? "🚂" : "🚲";

const AlternativesSection = ({ stopId }: { stopId: string }) => {
  const [alternatives, setAlternatives] = useState<
    TramAlternativeRoute[] | null
  >(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    const data = await dashboardApi.getTramAlternativeRoutes(stopId);
    setAlternatives(data);
    setLoading(false);
  };

  return (
    <Box sx={{ mt: 1 }}>
      {!alternatives && (
        <Button
          size="small"
          variant="outlined"
          color="warning"
          onClick={load}
          disabled={loading}
          sx={{ fontSize: "0.65rem", py: 0.25 }}
        >
          {loading ? <CircularProgress size={10} /> : "⚠ Show alternatives"}
        </Button>
      )}
      {alternatives?.length === 0 && (
        <Typography sx={{ fontSize: "0.68rem", color: "#8b949e" }}>
          No alternatives nearby
        </Typography>
      )}
      {alternatives && alternatives.length > 0 && (
        <>
          <Typography
            sx={{
              fontSize: "0.68rem",
              fontWeight: 700,
              color: "#e6edf3",
              mb: 0.5,
            }}
          >
            Nearby alternatives:
          </Typography>
          {alternatives.slice(0, 5).map((a, i) => (
            <Typography key={i} sx={{ fontSize: "0.68rem", color: "#c9d1d9" }}>
              {iconFor(a.transportType)} {a.stopName}
              <span style={{ color: "#8b949e" }}> — {a.distanceM}m</span>
            </Typography>
          ))}
        </>
      )}
    </Box>
  );
};
// ── Main Component ───────────────────────────────────────────────────

export const TramDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [panelOpen, setPanelOpen] = useState(true);
  const [lineFilter, setLineFilter] = useState<LineFilter>("");
  const [search, setSearch] = useState("");
  const [flyTarget, setFlyTarget] = useState<{
    center: [number, number];
    id: number;
  } | null>(null);
  const [selectedDisruptionId, setSelectedDisruptionId] = useState<
    number | null
  >(null);
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null);
  const [timePeriod, setTimePeriod] = useState("morning");

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
  const { data: rawRecommendations } = useTramRecommendations();

  // Parse the recommendation JSON strings into typed items
  const recommendations = useMemo(() => {
    if (!rawRecommendations || rawRecommendations.length === 0) return [];
    const items: TramRecommendationItem[] = [];
    for (const rec of rawRecommendations) {
      try {
        const parsed = JSON.parse(rec.recommendation) as TramRecommendationItem[];
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

  const activeTab = (["live", "delays", "usage", "commonDelays", "recommendations"] as const)[
    tabValue
  ];
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
    <Box
      sx={{
        position: "relative",
        height: "100%",
        width: "100%",
        bgcolor: "#0d1117",
      }}
    >
      {/* ── Full-viewport map ── */}
      <Box sx={{ height: "100%", width: "100%" }}>
        <MapContainer
          center={DUBLIN_CENTER}
          zoom={12}
          style={{ height: "100%", width: "100%" }}
          zoomControl={false}
          className="tram-map"
        >
          <TileLayer attribution={tileAttr} url={tileUrl} />
          <MapController target={flyTarget} />

        {/* Live → standard markers */}
        {activeTab === "live" &&
          allStops.map((stop) => (
            <Marker
              key={stop.name}
              position={[stop.lat, stop.lon]}
              icon={makeStopIcon(stop.line, isDisrupted(stop.forecasts))}
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
                  {stop.forecasts[0]?.message && (
                    <Typography
                      sx={{ fontSize: "0.68rem", color: "#8b949e", mt: 0.4 }}
                    >
                      {stop.forecasts[0].message}
                    </Typography>
                  )}
                  {isDisrupted(stop.forecasts) && (
                    <AlternativesSection
                      stopId={stop.forecasts[0]?.stopId ?? ""}
                    />
                  )}
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
      </MapContainer>
      </Box>
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
            variant="fullWidth"
            sx={{
              minHeight: 36,
              px: 1,
              "& .MuiTab-root": {
                minHeight: 36,
                fontSize: "0.72rem",
                textTransform: "none",
              },
            }}
          >
            <Tab label={`Live (${filteredForecasts.length})`} />
            <Tab label={`Delays (${filteredDelays.length})`} />
            <Tab label={`Usage (${filteredUsage.length})`} />
            <Tab label={`Common Delays (${filteredCommonDelays.length})`} />
            <Tab label={`Recs (${filteredRecommendations.length})`} />
          </Tabs>

          {/* Line filter + Search */}
          <Box
            sx={{
              px: 2,
              pt: 1.5,
              display: "flex",
              gap: 1,
              alignItems: "center",
            }}
          >
            {(
              [
                { key: "", label: "All Lines" },
                { key: "red", label: "Red" },
                { key: "green", label: "Green" },
              ] as { key: LineFilter; label: string }[]
            ).map(({ key, label }) => {
              const active = lineFilter === key;
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
            {tabValue === 4 &&
              filteredRecommendations.map((r, idx) => {
                const a = r.Attributes;
                const sevColor =
                  a.severity === "high"
                    ? "error.main"
                    : a.severity === "medium"
                      ? "warning.main"
                      : "info.main";
                const typeLabel =
                  a.type === "add_frequency"
                    ? "Add Trams"
                    : a.type === "reduce_frequency"
                      ? "Reduce Trams"
                      : a.type === "partial_run"
                        ? "Short Run"
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
                        label={typeLabel}
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
                        label={a.line.charAt(0).toUpperCase() + a.line.slice(1)}
                        sx={{
                          fontSize: "0.6rem",
                          height: 18,
                          bgcolor:
                            LINE_COLORS[a.line]
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
            {tabValue === 4 && filteredRecommendations.length === 0 && (
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
