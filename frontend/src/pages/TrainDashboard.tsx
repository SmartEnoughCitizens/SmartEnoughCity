/**
 * Train data dashboard — full-viewport map with floating control panel.
 * Four views: Stations | Live Trains | Utilization | Delays
 */

import { useState, useEffect, useRef } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  IconButton,
  Divider,
  Chip,
  Tabs,
  Tab,
  Tooltip,
  LinearProgress,
  ToggleButton,
  ToggleButtonGroup,
  List,
  ListItem,
  ListItemText,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import TrainIcon from "@mui/icons-material/Train";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import SpeedIcon from "@mui/icons-material/Speed";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMap,
} from "react-leaflet";
import {
  useTrainData,
  useTrainKpis,
  useTrainLiveTrains,
  useTrainUtilization,
  useTrainDelayPatterns,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import type { TrainDelayPattern, TrainStationUtilization, TimeOfDay } from "@/types";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// ── constants & helpers ────────────────────────────────────────────

const isRunning = (status?: string | null): boolean => {
  if (!status) return false;
  const s = status.toUpperCase();
  return s === "R" || s === "RUNNING";
};

const UTIL_COLOR: Record<string, string> = {
  HIGH: "#ef5350",
  MEDIUM: "#ffa726",
  LOW: "#26a69a",
};
const UTIL_LABEL: Record<string, string> = {
  HIGH: "Over-used",
  MEDIUM: "Normal",
  LOW: "Under-used",
};

const SEV_COLOR: Record<string, string> = {
  SEVERE: "#d32f2f",
  MODERATE: "#f57c00",
  MINOR: "#fbc02d",
};
const SEV_LABEL: Record<string, string> = {
  SEVERE: "Severe ≥10 min",
  MODERATE: "Moderate 5–10 min",
  MINOR: "Minor 1–5 min",
};

const TOD_LABEL: Record<TimeOfDay, string> = {
  MORNING_PEAK: "Morning Peak",
  MIDDAY: "Midday",
  AFTERNOON: "Afternoon",
  EVENING_PEAK: "Evening Peak",
  NIGHT: "Night",
};
const TOD_SHORT: Record<TimeOfDay, string> = {
  MORNING_PEAK: "6–9",
  MIDDAY: "9–13",
  AFTERNOON: "13–17",
  EVENING_PEAK: "17–21",
  NIGHT: "21–6",
};

function makeColorDot(color: string, size = 14): L.DivIcon {
  return L.divIcon({
    html: `<div style="width:${size}px;height:${size}px;background:${color};border-radius:50%;border:2px solid rgba(255,255,255,0.85);box-shadow:0 1px 4px rgba(0,0,0,0.45);"></div>`,
    className: "",
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -(size / 2 + 4)],
  });
}

const RUNNING_TRAIN_ICON = L.divIcon({
  html: `<div style="width:18px;height:18px;background:#4caf50;border-radius:50%;border:2px solid rgba(255,255,255,0.85);animation:trainPulse 1.4s ease-out infinite;"></div>`,
  className: "",
  iconSize: [18, 18],
  iconAnchor: [9, 9],
  popupAnchor: [0, -13],
});

const stationIcon = makeColorDot("#1976d2", 14);

if (typeof document !== "undefined" && !document.getElementById("train-pulse-style")) {
  const s = document.createElement("style");
  s.id = "train-pulse-style";
  s.textContent = `@keyframes trainPulse{0%{box-shadow:0 0 0 0 rgba(76,175,80,0.6)}70%{box-shadow:0 0 0 8px rgba(76,175,80,0)}100%{box-shadow:0 0 0 0 rgba(76,175,80,0)}}`;
  document.head.appendChild(s);
}

// ── Sub-components ─────────────────────────────────────────────────

interface FlyTarget { lat: number; lon: number; zoom?: number }

function MapController({ target }: { target: FlyTarget | null }) {
  const map = useMap();
  const prevRef = useRef<FlyTarget | null>(null);
  useEffect(() => {
    if (target && target !== prevRef.current) {
      prevRef.current = target;
      map.flyTo([target.lat, target.lon], target.zoom ?? 15, { duration: 0.8 });
    }
  }, [map, target]);
  return null;
}

function KpiCard({ icon, label, value, color = "primary.main" }: { icon: React.ReactNode; label: string; value: string | number; color?: string }) {
  return (
    <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, p: 1.5, borderRadius: 2, bgcolor: (t) => t.palette.mode === "dark" ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.04)" }}>
      <Box sx={{ color, fontSize: 26, display: "flex" }}>{icon}</Box>
      <Box>
        <Typography variant="h6" fontWeight={700} lineHeight={1}>{value}</Typography>
        <Typography variant="caption" color="text.secondary">{label}</Typography>
      </Box>
    </Box>
  );
}

function UtilizationBar({ station, max, onClick }: { station: TrainStationUtilization; max: number; onClick: () => void }) {
  const color = UTIL_COLOR[station.utilizationLevel] ?? "#90a4ae";
  const pct = max > 0 ? (station.trainServiceCount / max) * 100 : 0;
  return (
    <ListItem onClick={onClick} sx={{ px: 1.5, py: 0.75, cursor: "pointer", "&:hover": { bgcolor: "action.hover" } }} divider>
      <Box sx={{ width: "100%" }}>
        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 0.3 }}>
          <Typography variant="body2" fontWeight={600} noWrap sx={{ flex: 1, mr: 1 }}>{station.stationDesc}</Typography>
          <Chip size="small" label={UTIL_LABEL[station.utilizationLevel]} sx={{ height: 18, fontSize: "0.65rem", bgcolor: color, color: "#fff", fontWeight: 700 }} />
        </Box>
        <LinearProgress variant="determinate" value={pct} sx={{ height: 5, borderRadius: 3, bgcolor: "action.hover", "& .MuiLinearProgress-bar": { bgcolor: color } }} />
        <Box sx={{ display: "flex", justifyContent: "space-between", mt: 0.3 }}>
          <Typography variant="caption" color="text.secondary">{station.trainServiceCount} services</Typography>
          {station.avgDelayMinutes > 0 && <Typography variant="caption" color="warning.main">+{station.avgDelayMinutes.toFixed(1)} min delay</Typography>}
        </Box>
      </Box>
    </ListItem>
  );
}

function DelayPatternCard({ pattern, onClick }: { pattern: TrainDelayPattern; onClick: () => void }) {
  const color = SEV_COLOR[pattern.severityLevel] ?? "#90a4ae";
  return (
    <ListItem onClick={onClick} sx={{ px: 1.5, py: 1, cursor: "pointer", "&:hover": { bgcolor: "action.hover" }, alignItems: "flex-start" }} divider>
      <Box sx={{ width: "100%" }}>
        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 0.25 }}>
          <Typography variant="body2" fontWeight={700} noWrap sx={{ flex: 1, mr: 1 }}>{pattern.stationDesc}</Typography>
          <Chip size="small" label={SEV_LABEL[pattern.severityLevel]} sx={{ height: 18, fontSize: "0.62rem", bgcolor: color + "22", color, fontWeight: 700, border: `1px solid ${color}` }} />
        </Box>
        <Typography variant="caption" color="text.secondary" noWrap sx={{ display: "block" }}>
          {pattern.origin} → {pattern.destination}
          {pattern.trainType && <Box component="span" sx={{ ml: 0.75, opacity: 0.8 }}>· {pattern.trainType}</Box>}
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.75, mt: 0.5, flexWrap: "wrap" }}>
          <Chip size="small" label={`${TOD_LABEL[pattern.timeOfDay]} (${TOD_SHORT[pattern.timeOfDay]})`} sx={{ height: 17, fontSize: "0.6rem" }} />
          <Typography variant="caption" fontWeight={700} sx={{ color }}>avg {pattern.avgDelayMinutes.toFixed(1)} min</Typography>
          <Typography variant="caption" color="text.disabled">· max {pattern.maxDelayMinutes} min</Typography>
          <Typography variant="caption" color="text.secondary">· {pattern.latePercent.toFixed(0)}% late</Typography>
          <Typography variant="caption" color="text.disabled">({pattern.occurrenceCount} records)</Typography>
        </Box>
      </Box>
    </ListItem>
  );
}

// ── Main dashboard ─────────────────────────────────────────────────

type TabIndex = 0 | 1 | 2 | 3;
type DaysWindow = 7 | 30 | 90;

const TIME_OF_DAY_OPTIONS: Array<{ value: TimeOfDay | "ALL"; label: string }> = [
  { value: "ALL", label: "All day" },
  { value: "MORNING_PEAK", label: "Morning" },
  { value: "MIDDAY", label: "Midday" },
  { value: "AFTERNOON", label: "Afternoon" },
  { value: "EVENING_PEAK", label: "Evening" },
  { value: "NIGHT", label: "Night" },
];

export const TrainDashboard = () => {
  const [panelOpen, setPanelOpen] = useState(true);
  const [activeTab, setActiveTab] = useState<TabIndex>(0);
  const [flyTarget, setFlyTarget] = useState<FlyTarget | null>(null);
  const [daysWindow, setDaysWindow] = useState<DaysWindow>(30);
  const [todFilter, setTodFilter] = useState<TimeOfDay | "ALL">("ALL");
  const [sevFilter, setSevFilter] = useState<string>("ALL");

  const theme = useAppSelector((state) => state.ui.theme);
  const { data: trainData, isLoading: dataLoading, error } = useTrainData(500);
  const { data: kpiData } = useTrainKpis();
  const { data: liveTrains = [] } = useTrainLiveTrains();
  const { data: utilData = [], isLoading: utilLoading } = useTrainUtilization();
  const { data: delayRaw = [], isLoading: delayLoading } = useTrainDelayPatterns(daysWindow);

  const defaultCenter: [number, number] = [53.3498, -6.2603];
  const tileUrl = theme === "dark"
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const tileAttribution = theme === "dark"
    ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
    : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  const delayPatterns = delayRaw.filter(
    (p) => (todFilter === "ALL" || p.timeOfDay === todFilter) && (sevFilter === "ALL" || p.severityLevel === sevFilter),
  );

  const maxServiceCount = utilData.length > 0 ? Math.max(...utilData.map((s) => s.trainServiceCount)) : 1;
  const severeCount = delayRaw.filter((p) => p.severityLevel === "SEVERE").length;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Map */}
      <Box sx={{ height: "100%", width: "100%" }}>
        <MapContainer center={defaultCenter} zoom={12} style={{ height: "100%", width: "100%" }} zoomControl={false}>
          <TileLayer attribution={tileAttribution} url={tileUrl} />
          <MapController target={flyTarget} />

          {/* Station markers */}
          {(activeTab === 0) && trainData?.data?.map((station) => (
            <Marker key={station.id} position={[station.lat, station.lon]} icon={stationIcon} eventHandlers={{ click: () => setFlyTarget({ lat: station.lat, lon: station.lon }) }}>
              <Popup>
                <Typography variant="subtitle2" fontWeight={700}>{station.stationDesc}</Typography>
                <Typography variant="body2" color="text.secondary">Code: {station.stationCode}</Typography>
                {station.stationType && <Typography variant="body2" color="text.secondary">Type: {station.stationType}</Typography>}
              </Popup>
            </Marker>
          ))}

          {/* Live train markers */}
          {activeTab === 1 && liveTrains.filter((t) => t.lat && t.lon).map((t) => (
            <Marker key={t.trainCode} position={[t.lat, t.lon]} icon={isRunning(t.status) ? RUNNING_TRAIN_ICON : makeColorDot("#ff9800", 16)} eventHandlers={{ click: () => setFlyTarget({ lat: t.lat, lon: t.lon }) }}>
              <Popup>
                <Typography variant="subtitle2" fontWeight={700}>🚂 {t.trainCode}</Typography>
                {t.direction && <Typography variant="body2" color="text.secondary">Direction: {t.direction}</Typography>}
                {t.trainType && <Typography variant="body2" color="text.secondary">Type: {t.trainType}</Typography>}
                <Chip size="small" label={isRunning(t.status) ? "Running" : "Not yet running"} color={isRunning(t.status) ? "success" : "warning"} sx={{ mt: 0.5, height: 18, fontSize: "0.65rem" }} />
                {t.publicMessage && <Typography variant="body2" sx={{ mt: 0.5 }}>{t.publicMessage}</Typography>}
              </Popup>
            </Marker>
          ))}

          {/* Utilization markers */}
          {activeTab === 2 && utilData.map((s) => (
            <Marker key={s.stationCode} position={[s.lat, s.lon]} icon={makeColorDot(UTIL_COLOR[s.utilizationLevel] ?? "#90a4ae", 16)} eventHandlers={{ click: () => setFlyTarget({ lat: s.lat, lon: s.lon }) }}>
              <Popup>
                <Typography variant="subtitle2" fontWeight={700}>{s.stationDesc}</Typography>
                <Typography variant="body2" color="text.secondary">{s.trainServiceCount} active services</Typography>
                {s.avgDelayMinutes > 0 && <Typography variant="body2" color="warning.main">Avg delay: {s.avgDelayMinutes.toFixed(1)} min</Typography>}
                <Chip size="small" label={UTIL_LABEL[s.utilizationLevel]} sx={{ mt: 0.5, height: 18, fontSize: "0.65rem", bgcolor: UTIL_COLOR[s.utilizationLevel], color: "#fff", fontWeight: 700 }} />
              </Popup>
            </Marker>
          ))}

          {/* Delay pattern markers */}
          {activeTab === 3 && delayPatterns.map((p, i) => (
            <Marker key={`${p.stationCode}-${p.timeOfDay}-${i}`} position={[p.lat, p.lon]} icon={makeColorDot(SEV_COLOR[p.severityLevel] ?? "#90a4ae", 14)} eventHandlers={{ click: () => setFlyTarget({ lat: p.lat, lon: p.lon }) }}>
              <Popup>
                <Typography variant="subtitle2" fontWeight={700}>{p.stationDesc}</Typography>
                <Typography variant="body2" color="text.secondary">{p.origin} → {p.destination}</Typography>
                <Typography variant="body2" sx={{ color: SEV_COLOR[p.severityLevel] }}>Avg delay: {p.avgDelayMinutes.toFixed(1)} min ({TOD_LABEL[p.timeOfDay]})</Typography>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </Box>

      {error && <Alert severity="error" sx={{ position: "absolute", top: 16, left: "50%", transform: "translateX(-50%)", zIndex: 1000, borderRadius: 2 }}>Failed to load train data</Alert>}

      {!panelOpen && (
        <IconButton onClick={() => setPanelOpen(true)} sx={{ position: "absolute", top: 16, right: 16, zIndex: 1000, bgcolor: (t) => t.palette.background.paper, backdropFilter: "blur(12px)", "&:hover": { bgcolor: (t) => t.palette.background.paper } }}>
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* Floating panel */}
      {panelOpen && (
        <Paper elevation={0} sx={{ position: "absolute", top: 16, right: 16, bottom: 16, width: 420, zIndex: 1000, borderRadius: 3, display: "flex", flexDirection: "column", overflow: "hidden" }}>

          {/* Header */}
          <Box sx={{ p: 2, pb: 1.5, display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <Typography variant="h5">🚂 Train Network</Typography>
            <IconButton size="small" onClick={() => setPanelOpen(false)}><CloseIcon fontSize="small" /></IconButton>
          </Box>

          {/* KPIs */}
          {kpiData && (
            <Box sx={{ px: 2, pb: 1.5, display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1 }}>
              <KpiCard icon={<LocationOnIcon fontSize="inherit" />} label="Stations" value={kpiData.totalStations} color="info.main" />
              <KpiCard icon={<TrainIcon fontSize="inherit" />} label="Live Trains" value={kpiData.liveTrainsRunning} color="success.main" />
              <KpiCard icon={<AccessTimeIcon fontSize="inherit" />} label="On Time" value={`${kpiData.onTimePct.toFixed(1)}%`} color={kpiData.onTimePct >= 80 ? "success.main" : "warning.main"} />
              <KpiCard icon={<SpeedIcon fontSize="inherit" />} label="Avg Delay" value={`${kpiData.avgDelayMinutes.toFixed(1)} min`} color={kpiData.avgDelayMinutes <= 2 ? "success.main" : "warning.main"} />
            </Box>
          )}

          <Divider />

          {/* Tabs */}
          <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v as TabIndex)} variant="fullWidth" sx={{ minHeight: 40, "& .MuiTab-root": { minHeight: 40, fontSize: "0.7rem", px: 0.5 } }}>
            <Tab label={`Stations (${trainData?.totalRecords ?? 0})`} />
            <Tab label={
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.4 }}>
                Live
                <Box sx={{ width: 7, height: 7, borderRadius: "50%", bgcolor: liveTrains.filter((t) => isRunning(t.status)).length > 0 ? "success.main" : "text.disabled" }} />
                ({liveTrains.length})
              </Box>
            } />
            <Tab label={<Box sx={{ display: "flex", alignItems: "center", gap: 0.4 }}><TrendingUpIcon sx={{ fontSize: 13 }} />Util.</Box>} />
            <Tab label={
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.4 }}>
                <WarningAmberIcon sx={{ fontSize: 13, color: severeCount > 0 ? "error.main" : "inherit" }} />
                Delays
                {severeCount > 0 && <Box sx={{ width: 6, height: 6, borderRadius: "50%", bgcolor: "error.main" }} />}
              </Box>
            } />
          </Tabs>

          {/* ── Stations ── */}
          {activeTab === 0 && (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {dataLoading ? <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={28} /></Box> : (
                <List sx={{ px: 1 }}>
                  {trainData?.data?.map((station) => (
                    <ListItem key={station.id} divider onClick={() => setFlyTarget({ lat: station.lat, lon: station.lon })} sx={{ cursor: "pointer", "&:hover": { bgcolor: "action.hover" } }}>
                      <ListItemText primary={station.stationDesc} secondary={`${station.stationCode}${station.stationType ? ` · ${station.stationType}` : ""}`} />
                    </ListItem>
                  ))}
                </List>
              )}
            </Box>
          )}

          {/* ── Live Trains ── */}
          {activeTab === 1 && (
            <Box sx={{ flex: 1, overflow: "auto", display: "flex", flexDirection: "column" }}>
              {liveTrains.length > 0 && (
                <Box sx={{ px: 2, py: 1, display: "flex", gap: 1, flexWrap: "wrap" }}>
                  <Chip size="small" label={`🟢 Running (${liveTrains.filter((t) => isRunning(t.status)).length})`} sx={{ fontSize: "0.7rem" }} />
                  <Chip size="small" label={`🟠 Not yet (${liveTrains.filter((t) => !isRunning(t.status)).length})`} sx={{ fontSize: "0.7rem" }} />
                </Box>
              )}
              {liveTrains.length === 0 ? (
                <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 4 }}>No live trains available</Typography>
              ) : (
                <List sx={{ flex: 1, overflow: "auto", px: 1 }}>
                  {[...liveTrains].sort((a, b) => Number(isRunning(b.status)) - Number(isRunning(a.status))).map((t) => (
                    <ListItem key={t.trainCode} divider onClick={() => t.lat && t.lon ? setFlyTarget({ lat: t.lat, lon: t.lon }) : undefined}
                      sx={{ cursor: t.lat && t.lon ? "pointer" : "default", opacity: t.lat && t.lon ? 1 : 0.5, "&:hover": { bgcolor: t.lat && t.lon ? "action.hover" : undefined } }}>
                      <ListItemText
                        primary={<Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                          <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: isRunning(t.status) ? "success.main" : "warning.main", flexShrink: 0 }} />
                          <Typography variant="body2" fontWeight={600}>{t.trainCode}</Typography>
                          {t.trainType && <Typography variant="caption" color="text.secondary">· {t.trainType}</Typography>}
                        </Box>}
                        secondary={t.direction ?? "Unknown direction"}
                      />
                      <Chip size="small" label={isRunning(t.status) ? "Running" : "Soon"} color={isRunning(t.status) ? "success" : "warning"} sx={{ height: 18, fontSize: "0.65rem" }} />
                    </ListItem>
                  ))}
                </List>
              )}
            </Box>
          )}

          {/* ── Utilization ── */}
          {activeTab === 2 && (
            <Box sx={{ flex: 1, overflow: "auto", display: "flex", flexDirection: "column" }}>
              {utilLoading ? <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={28} /></Box> : (
                <>
                  <Box sx={{ px: 2, py: 1, display: "flex", gap: 0.75, flexWrap: "wrap" }}>
                    {(["HIGH", "MEDIUM", "LOW"] as const).map((level) => (
                      <Tooltip key={level} title={level === "HIGH" ? ">1.5× mean — high demand" : level === "LOW" ? "<0.5× mean — under-served" : "Within normal range"}>
                        <Chip size="small" label={`${level === "HIGH" ? "🔴" : level === "LOW" ? "🟢" : "🟡"} ${UTIL_LABEL[level]} (${utilData.filter((s) => s.utilizationLevel === level).length})`} sx={{ fontSize: "0.68rem" }} />
                      </Tooltip>
                    ))}
                  </Box>
                  <Divider />
                  {utilData.length === 0
                    ? <Alert severity="info" sx={{ mx: 2, mt: 2 }}>No utilization data yet — check back once trains are active.</Alert>
                    : <List disablePadding sx={{ flex: 1, overflow: "auto" }}>
                        {utilData.map((s) => <UtilizationBar key={s.stationCode} station={s} max={maxServiceCount} onClick={() => setFlyTarget({ lat: s.lat, lon: s.lon })} />)}
                      </List>
                  }
                </>
              )}
            </Box>
          )}

          {/* ── Delays ── */}
          {activeTab === 3 && (
            <Box sx={{ flex: 1, overflow: "auto", display: "flex", flexDirection: "column" }}>
              {/* Filters */}
              <Box sx={{ px: 1.5, pt: 1.25, pb: 0.75 }}>
                {/* Time window toggle */}
                <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
                  <Typography variant="caption" color="text.secondary" sx={{ flexShrink: 0 }}>Window:</Typography>
                  <ToggleButtonGroup value={daysWindow} exclusive onChange={(_, v) => v && setDaysWindow(v as DaysWindow)} size="small"
                    sx={{ "& .MuiToggleButton-root": { py: 0.25, px: 1.25, fontSize: "0.7rem" } }}>
                    <ToggleButton value={7}>7 days</ToggleButton>
                    <ToggleButton value={30}>30 days</ToggleButton>
                    <ToggleButton value={90}>90 days</ToggleButton>
                  </ToggleButtonGroup>
                </Box>

                {/* Time-of-day chips */}
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, flexWrap: "wrap", mb: 0.75 }}>
                  <Typography variant="caption" color="text.secondary" sx={{ mr: 0.25 }}>Time:</Typography>
                  {TIME_OF_DAY_OPTIONS.map((opt) => (
                    <Chip key={opt.value} size="small" label={opt.label} clickable onClick={() => setTodFilter(opt.value as TimeOfDay | "ALL")}
                      sx={{ height: 20, fontSize: "0.62rem", bgcolor: todFilter === opt.value ? "primary.main" : undefined, color: todFilter === opt.value ? "#fff" : undefined }} />
                  ))}
                </Box>

                {/* Severity chips */}
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, flexWrap: "wrap" }}>
                  <Typography variant="caption" color="text.secondary" sx={{ mr: 0.25 }}>Severity:</Typography>
                  {(["ALL", "SEVERE", "MODERATE", "MINOR"] as const).map((s) => (
                    <Chip key={s} size="small" label={s === "ALL" ? "All" : s.charAt(0) + s.slice(1).toLowerCase()} clickable onClick={() => setSevFilter(s)}
                      sx={{ height: 20, fontSize: "0.62rem", bgcolor: sevFilter === s ? (s === "ALL" ? "primary.main" : SEV_COLOR[s]) : undefined, color: sevFilter === s ? "#fff" : s !== "ALL" ? SEV_COLOR[s] : undefined }} />
                  ))}
                </Box>
              </Box>

              <Divider />

              {/* Results */}
              {delayLoading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={28} /></Box>
              ) : delayPatterns.length === 0 ? (
                <Alert severity="info" sx={{ mx: 2, mt: 2, fontSize: "0.8rem" }}>
                  {delayRaw.length === 0
                    ? "No delay patterns found for this period. Historical data accumulates over time — try a longer window."
                    : "No patterns match the selected filters — try widening your selection."}
                </Alert>
              ) : (
                <>
                  <Box sx={{ px: 2, py: 0.75 }}>
                    <Typography variant="caption" color="text.secondary">
                      {delayPatterns.length} pattern{delayPatterns.length !== 1 ? "s" : ""} · sorted worst-first
                      {severeCount > 0 && <Box component="span" sx={{ color: "error.main", ml: 0.75 }}>· {severeCount} severe</Box>}
                    </Typography>
                  </Box>
                  <List disablePadding sx={{ flex: 1, overflow: "auto" }}>
                    {delayPatterns.map((p, i) => (
                      <DelayPatternCard key={`${p.stationCode}-${p.timeOfDay}-${i}`} pattern={p} onClick={() => setFlyTarget({ lat: p.lat, lon: p.lon })} />
                    ))}
                  </List>
                </>
              )}
            </Box>
          )}

          {/* Footer */}
          <Box
            sx={{ px: 2, py: 1.25, borderTop: "1px solid rgba(48,54,61,0.5)" }}
          >
            <Typography
              variant="caption"
              sx={{ color: "#30363d", fontSize: "0.62rem" }}
            >
              Irish Rail · Greater Dublin Area · Real-time data
            </Typography>
          </Box>
        </Paper>
      )}

      {/* Utilization map legend */}
      {activeTab === 2 && panelOpen && (
        <Paper elevation={0} sx={{ position: "absolute", bottom: 24, left: 16, zIndex: 1000, borderRadius: 2, p: 1.5, display: "flex", flexDirection: "column", gap: 0.5 }}>
          <Typography variant="caption" fontWeight={700} color="text.secondary">SERVICE LOAD</Typography>
          {(["HIGH", "MEDIUM", "LOW"] as const).map((level) => (
            <Box key={level} sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <Box sx={{ width: 12, height: 12, borderRadius: "50%", bgcolor: UTIL_COLOR[level], border: "1.5px solid rgba(255,255,255,0.7)" }} />
              <Typography variant="caption">{UTIL_LABEL[level]}</Typography>
            </Box>
          ))}
        </Paper>
      )}

      {/* Delays map legend */}
      {activeTab === 3 && panelOpen && (
        <Paper elevation={0} sx={{ position: "absolute", bottom: 24, left: 16, zIndex: 1000, borderRadius: 2, p: 1.5, display: "flex", flexDirection: "column", gap: 0.5 }}>
          <Typography variant="caption" fontWeight={700} color="text.secondary">DELAY SEVERITY</Typography>
          {(["SEVERE", "MODERATE", "MINOR"] as const).map((level) => (
            <Box key={level} sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <Box sx={{ width: 12, height: 12, borderRadius: "50%", bgcolor: SEV_COLOR[level], border: "1.5px solid rgba(255,255,255,0.7)" }} />
              <Typography variant="caption">{level.charAt(0) + level.slice(1).toLowerCase()}</Typography>
            </Box>
          ))}
        </Paper>
      )}
    </Box>
  );
};
