/**
 * Tram dashboard — Google Maps-style smart city view (matches TrainDashboard design)
 *
 * Features:
 *  - Full-viewport map: OSM (light) / Carto dark (dark)
 *  - Left glass panel: KPI strip, line filters, Live | Delays | Usage tabs, search
 *  - Red/Green coloured stop markers on map
 *  - Line filter applies to panel lists; map always shows all stops
 *  - Click a stop in the list → map flies to it
 *  - Map legend overlay (bottom-left)
 */

import { useState, useEffect, useMemo, useCallback } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  IconButton,
  Divider,
  Chip,
  TextField,
  InputAdornment,
  ListItemButton,
  Slider,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import TramIcon from "@mui/icons-material/Tram";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import PeopleIcon from "@mui/icons-material/People";
import SearchIcon from "@mui/icons-material/Search";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import ArrowDownwardIcon from "@mui/icons-material/ArrowDownward";
import ArrowUpwardIcon from "@mui/icons-material/ArrowUpward";
import HistoryIcon from "@mui/icons-material/History";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import {
  useTramKpis,
  useTramLiveForecasts,
  useTramDelays,
  useTramStopUsage,
  useTramCommonDelays,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

import type { TramLiveForecast } from "@/types";

// ── Constants ───────────────────────────────────────────────────────

const DUBLIN_CENTER: [number, number] = [53.3398, -6.2603];

type LineFilter = "" | "red" | "green";
type PanelTab = "live" | "delays" | "usage" | "history";

const LINE_COLORS: Record<string, string> = {
  red: "#DC2626",
  green: "#16A34A",
};

// ── CSS ──────────────────────────────────────────────────────────────

const INJECTED_CSS = `
  .leaflet-popup-content-wrapper {
    background: rgba(13,17,23,0.96) !important;
    color: #e6edf3 !important;
    border: 1px solid rgba(255,255,255,0.10) !important;
    border-radius: 12px !important;
    box-shadow: 0 8px 32px rgba(0,0,0,0.55) !important;
  }
  .leaflet-popup-tip { background: rgba(13,17,23,0.96) !important; }
  .leaflet-popup-close-button {
    color: #8b949e !important; top: 8px !important; right: 8px !important;
  }
  .leaflet-popup-content { margin: 14px 16px !important; }
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

// ── Leaflet icon factory ─────────────────────────────────────────────

function makeStopIcon(line: string): L.DivIcon {
  const color = LINE_COLORS[line] ?? "#607D8B";
  const letter = line === "red" ? "R" : line === "green" ? "G" : "T";
  return L.divIcon({
    html: `<div class="tram-pin" style="width:22px;height:22px;background:${color};font-size:9px;">${letter}</div>`,
    className: "",
    iconSize: [22, 22],
    iconAnchor: [11, 11],
    popupAnchor: [0, -14],
  });
}

// ── Map controller ───────────────────────────────────────────────────

function MapController({
  target,
}: {
  target: { center: [number, number]; id: number } | null;
}) {
  const map = useMap();
  useEffect(() => {
    if (target)
      map.flyTo(target.center, 15, { duration: 1.2, easeLinearity: 0.25 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);
  return null;
}

// ── KPI Card (matches TrainDashboard) ────────────────────────────────

function KpiCard({
  icon,
  label,
  value,
  accent,
}: {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  accent: string;
}) {
  return (
    <Box
      sx={{
        p: 1.25,
        borderRadius: 2,
        background: "rgba(22,27,34,0.75)",
        border: "1px solid rgba(48,54,61,0.6)",
        display: "flex",
        flexDirection: "column",
        gap: 0.5,
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
        <Box sx={{ color: accent, display: "flex", fontSize: 14 }}>{icon}</Box>
        <Typography
          variant="caption"
          sx={{
            color: "#8b949e",
            fontSize: "0.62rem",
            textTransform: "uppercase",
            letterSpacing: 0.6,
          }}
        >
          {label}
        </Typography>
      </Box>
      <Typography
        variant="h6"
        fontWeight={700}
        sx={{ color: "#e6edf3", lineHeight: 1, fontSize: "1.05rem" }}
      >
        {value}
      </Typography>
    </Box>
  );
}

// ── Map Legend ────────────────────────────────────────────────────────

function MapLegend() {
  return (
    <Box
      sx={{
        position: "absolute",
        bottom: 36,
        left: 16,
        zIndex: 1000,
        background: "rgba(13,17,23,0.92)",
        backdropFilter: "blur(14px)",
        border: "1px solid rgba(255,255,255,0.08)",
        borderRadius: 2,
        p: 1.5,
        minWidth: 120,
      }}
    >
      <Typography
        variant="caption"
        sx={{
          color: "#484f58",
          textTransform: "uppercase",
          letterSpacing: 0.8,
          fontSize: "0.6rem",
          display: "block",
          mb: 0.75,
        }}
      >
        Luas Lines
      </Typography>
      {(["red", "green"] as const).map((line) => (
        <Box
          key={line}
          sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}
        >
          <Box
            sx={{
              width: 14,
              height: 14,
              borderRadius: "50%",
              bgcolor: LINE_COLORS[line],
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexShrink: 0,
            }}
          >
            <Typography sx={{ fontSize: 7, fontWeight: 800, color: "#fff" }}>
              {line === "red" ? "R" : "G"}
            </Typography>
          </Box>
          <Typography
            variant="caption"
            sx={{
              color: "#c9d1d9",
              fontSize: "0.7rem",
              textTransform: "capitalize",
            }}
          >
            {line} Line
          </Typography>
        </Box>
      ))}
    </Box>
  );
}

// ── Helpers ──────────────────────────────────────────────────────────

/** Deduplicate forecasts into unique stops; pick top 2 inbound + top 2 outbound by soonest due */
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
    if (!map.has(f.stopId)) {
      map.set(f.stopId, {
        lat: f.lat,
        lon: f.lon,
        name: f.stopName,
        line: f.line,
        forecasts: [],
      });
    }
    map.get(f.stopId)!.forecasts.push(f);
  }
  const dueSorter = (a: TramLiveForecast, b: TramLiveForecast) =>
    (a.dueMins ?? 999) - (b.dueMins ?? 999);
  for (const stop of map.values()) {
    const inbound = stop.forecasts
      .filter((f) => f.direction.toLowerCase() === "inbound")
      .toSorted(dueSorter)
      .slice(0, 2);
    const outbound = stop.forecasts
      .filter((f) => f.direction.toLowerCase() === "outbound")
      .toSorted(dueSorter)
      .slice(0, 2);
    stop.forecasts = [...inbound, ...outbound];
  }
  return [...map.values()];
}

// ── Main Component ───────────────────────────────────────────────────

export const TramDashboard = () => {
  const [panelOpen, setPanelOpen] = useState(true);
  const [activeTab, setActiveTab] = useState<PanelTab>("live");
  const [lineFilter, setLineFilter] = useState<LineFilter>("");
  const [search, setSearch] = useState("");
  const [flyTarget, setFlyTarget] = useState<{
    center: [number, number];
    id: number;
  } | null>(null);
  const [selectedStopId, setSelectedStopId] = useState<string | null>(null);
  const [usageHour, setUsageHour] = useState<number>(8); // Default to 8am peak

  const theme = useAppSelector((state) => state.ui.theme);
  const { data: kpis } = useTramKpis();
  const {
    data: liveForecasts,
    isLoading: forecastsLoading,
    error,
  } = useTramLiveForecasts();
  const { data: delays } = useTramDelays();
  const { data: stopUsage } = useTramStopUsage(usageHour);
  const { data: commonDelays } = useTramCommonDelays();

  // Inject CSS once
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

  // All stops for map (always unfiltered)
  const allStops = useMemo(
    () => buildStopMap(liveForecasts ?? []),
    [liveForecasts],
  );

  // Lookup: stopId → {lat, lon} for fly-to from delay/usage list
  const stopCoords = useMemo(() => {
    const coords = new Map<string, { lat: number; lon: number }>();
    for (const stop of allStops) {
      const id = stop.forecasts[0]?.stopId ?? stop.name;
      coords.set(id, { lat: stop.lat, lon: stop.lon });
    }
    return coords;
  }, [allStops]);

  // Filtered forecasts for panel — sorted by soonest due time
  const filteredForecasts = useMemo(() => {
    let list = liveForecasts ?? [];
    if (lineFilter) list = list.filter((f) => f.line === lineFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (f) =>
          f.stopName.toLowerCase().includes(q) ||
          f.destination.toLowerCase().includes(q),
      );
    }
    return list.toSorted((a, b) => (a.dueMins ?? 999) - (b.dueMins ?? 999));
  }, [liveForecasts, lineFilter, search]);

  // Filtered delays for panel
  const filteredDelays = useMemo(() => {
    let list = delays ?? [];
    if (lineFilter) list = list.filter((d) => d.line === lineFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (d) =>
          d.stopName.toLowerCase().includes(q) ||
          d.destination.toLowerCase().includes(q),
      );
    }
    return list;
  }, [delays, lineFilter, search]);

  // Filtered usage for panel
  const filteredUsage = useMemo(() => {
    let list = stopUsage ?? [];
    if (lineFilter) list = list.filter((u) => u.line === lineFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter((u) => u.stopName.toLowerCase().includes(q));
    }
    return list;
  }, [stopUsage, lineFilter, search]);

  // Filtered common delays for panel
  const filteredCommonDelays = useMemo(() => {
    let list = commonDelays ?? [];
    if (lineFilter) list = list.filter((d) => d.line === lineFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter((d) => d.stopName.toLowerCase().includes(q));
    }
    return list;
  }, [commonDelays, lineFilter, search]);

  const handleStopClick = useCallback(
    (lat: number, lon: number, stopId: string) => {
      setSelectedStopId(stopId);
      setFlyTarget({ center: [lat, lon], id: Date.now() });
    },
    [],
  );

  const PANEL_W = 400;

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
        >
          <TileLayer attribution={tileAttr} url={tileUrl} />
          <MapController target={flyTarget} />

          {/* Stop markers — always show all */}
          {allStops.map((stop) => (
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
                      → {f.destination}:{" "}
                      <strong>{f.dueMins ?? "—"} min</strong>{" "}
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
                </Box>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </Box>

      {/* Map legend */}
      <MapLegend />

      {/* Error */}
      {error && (
        <Alert
          severity="error"
          sx={{
            position: "absolute",
            top: 16,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1001,
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
            left: 16,
            zIndex: 1001,
            bgcolor: "rgba(13,17,23,0.92)",
            border: "1px solid rgba(255,255,255,0.10)",
            backdropFilter: "blur(12px)",
            color: "#e6edf3",
            "&:hover": { bgcolor: "rgba(22,27,34,0.96)" },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* ── Left glass panel ── */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: 0,
            left: 0,
            bottom: 0,
            width: PANEL_W,
            zIndex: 1000,
            borderRadius: 0,
            borderRight: "1px solid rgba(48,54,61,0.5)",
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            background: "rgba(13,17,23,0.95)",
            backdropFilter: "blur(20px)",
          }}
        >
          {/* Header */}
          <Box
            sx={{
              px: 2,
              py: 1.75,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              borderBottom: "1px solid rgba(48,54,61,0.5)",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1.25 }}>
              <Box
                sx={{
                  width: 34,
                  height: 34,
                  borderRadius: "50%",
                  background:
                    "linear-gradient(135deg, #DC2626 0%, #16A34A 100%)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  boxShadow: "0 2px 8px rgba(220,38,38,0.3)",
                }}
              >
                <TramIcon sx={{ fontSize: 18, color: "#fff" }} />
              </Box>
              <Box>
                <Typography
                  variant="subtitle1"
                  fontWeight={700}
                  sx={{
                    color: "#e6edf3",
                    lineHeight: 1.2,
                    letterSpacing: -0.2,
                  }}
                >
                  Luas Tram Network
                </Typography>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
                  <FiberManualRecordIcon
                    sx={{ fontSize: 7, color: "#2ea043" }}
                  />
                  <Typography
                    variant="caption"
                    sx={{
                      color: "#2ea043",
                      fontSize: "0.62rem",
                      letterSpacing: 0.4,
                    }}
                  >
                    LIVE · Red & Green Lines
                  </Typography>
                </Box>
              </Box>
            </Box>
            <IconButton
              size="small"
              onClick={() => setPanelOpen(false)}
              sx={{ color: "#8b949e" }}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* KPI grid */}
          {kpis && (
            <Box
              sx={{
                p: 1.75,
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 1,
              }}
            >
              <KpiCard
                icon={<LocationOnIcon fontSize="inherit" />}
                label="Total Stops"
                value={kpis.totalStops}
                accent="#00ACC1"
              />
              <KpiCard
                icon={<TramIcon fontSize="inherit" />}
                label="Forecasts"
                value={kpis.activeForecastCount}
                accent="#2ea043"
              />
              <KpiCard
                icon={<AccessTimeIcon fontSize="inherit" />}
                label="Avg Due Time"
                value={`${kpis.avgDueMins.toFixed(1)} min`}
                accent={kpis.avgDueMins <= 10 ? "#2ea043" : "#d29922"}
              />
              <KpiCard
                icon={<WarningAmberIcon fontSize="inherit" />}
                label="Delays"
                value={filteredDelays.length}
                accent={filteredDelays.length === 0 ? "#2ea043" : "#DC2626"}
              />
            </Box>
          )}

          <Divider sx={{ borderColor: "rgba(48,54,61,0.5)" }} />

          {/* Tab toggle */}
          <Box
            sx={{
              px: 1.75,
              pt: 1.25,
              pb: 1,
              display: "flex",
              gap: 0.5,
            }}
          >
            {(
              [
                { key: "live", label: "Live", count: filteredForecasts.length },
                {
                  key: "delays",
                  label: "Delays",
                  count: filteredDelays.length,
                },
                {
                  key: "usage",
                  label: "Usage",
                  count: filteredUsage.length,
                },
                {
                  key: "history",
                  label: "History",
                  count: filteredCommonDelays.length,
                },
              ] as { key: PanelTab; label: string; count: number | null }[]
            ).map(({ key, label, count }) => {
              const active = activeTab === key;
              return (
                <Box
                  key={key}
                  onClick={() => setActiveTab(key)}
                  sx={{
                    px: 1.5,
                    py: 0.5,
                    borderRadius: 1.5,
                    cursor: "pointer",
                    bgcolor: active ? "rgba(21,101,192,0.2)" : "transparent",
                    border: "1px solid",
                    borderColor: active ? "#1565C0" : "rgba(48,54,61,0.6)",
                    display: "flex",
                    alignItems: "center",
                    gap: 0.75,
                    transition: "all 0.15s",
                    "&:hover": {
                      bgcolor: active
                        ? "rgba(21,101,192,0.25)"
                        : "rgba(255,255,255,0.04)",
                    },
                  }}
                >
                  <Typography
                    sx={{
                      fontSize: "0.75rem",
                      fontWeight: active ? 600 : 400,
                      color: active ? "#e6edf3" : "#8b949e",
                    }}
                  >
                    {label}
                  </Typography>
                  {count !== null && (
                    <Box
                      sx={{
                        px: 0.75,
                        py: 0.15,
                        borderRadius: 1,
                        bgcolor: active ? "#1565C0" : "rgba(48,54,61,0.6)",
                        minWidth: 20,
                        textAlign: "center",
                      }}
                    >
                      <Typography
                        sx={{
                          fontSize: "0.62rem",
                          fontWeight: 700,
                          color: "#e6edf3",
                        }}
                      >
                        {count}
                      </Typography>
                    </Box>
                  )}
                </Box>
              );
            })}
          </Box>

          {/* Line filter */}
          <Box sx={{ px: 1.75, pb: 1 }}>
            <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
              {(["", "red", "green"] as LineFilter[]).map((key) => {
                const active = lineFilter === key;
                const label =
                  key === "" ? "All" : key === "red" ? "Red" : "Green";
                const accent =
                  key === "" ? "#1565C0" : (LINE_COLORS[key] ?? "#607D8B");
                return (
                  <Chip
                    key={key || "all"}
                    size="small"
                    label={label}
                    onClick={() => setLineFilter(key)}
                    sx={{
                      fontSize: "0.7rem",
                      cursor: "pointer",
                      bgcolor: active ? accent : "rgba(22,27,34,0.7)",
                      color: active ? "#fff" : "#8b949e",
                      border: "1px solid",
                      borderColor: active ? accent : "rgba(48,54,61,0.6)",
                      transition: "all 0.15s",
                      "&:hover": { opacity: 0.85 },
                    }}
                  />
                );
              })}
            </Box>
          </Box>

          {/* Search — all tabs */}
          <Box sx={{ px: 1.75, pb: 1.25 }}>
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
                      <SearchIcon sx={{ fontSize: 16, color: "#8b949e" }} />
                    </InputAdornment>
                  ),
                  sx: {
                    bgcolor: "rgba(22,27,34,0.7)",
                    color: "#e6edf3",
                    fontSize: "0.85rem",
                    borderRadius: 2,
                    "& .MuiOutlinedInput-notchedOutline": {
                      borderColor: "rgba(48,54,61,0.6)",
                    },
                    "&:hover .MuiOutlinedInput-notchedOutline": {
                      borderColor: "rgba(255,255,255,0.18)",
                    },
                    "&.Mui-focused .MuiOutlinedInput-notchedOutline": {
                      borderColor: "#1565C0",
                    },
                    "& input": {
                      color: "#e6edf3",
                      "&::placeholder": { color: "#8b949e", opacity: 1 },
                    },
                  },
                },
              }}
            />
          </Box>

          {/* Hour slider — usage tab only */}
          {activeTab === "usage" && (
            <Box sx={{ px: 2.5, pb: 1 }}>
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  mb: 0.5,
                }}
              >
                <Typography
                  sx={{
                    fontSize: "0.68rem",
                    color: "#8b949e",
                    textTransform: "uppercase",
                    letterSpacing: 0.5,
                  }}
                >
                  Hour of day
                </Typography>
                <Typography
                  sx={{
                    fontSize: "0.85rem",
                    fontWeight: 700,
                    color: "#e6edf3",
                  }}
                >
                  {`${usageHour.toString().padStart(2, "0")}:00`}
                </Typography>
              </Box>
              <Slider
                value={usageHour}
                onChange={(_, val) => setUsageHour(val)}
                min={5}
                max={23}
                step={1}
                marks={[
                  { value: 5, label: "05" },
                  { value: 8, label: "08" },
                  { value: 12, label: "12" },
                  { value: 17, label: "17" },
                  { value: 23, label: "23" },
                ]}
                sx={{
                  color: "#1565C0",
                  "& .MuiSlider-markLabel": {
                    fontSize: "0.6rem",
                    color: "#484f58",
                  },
                  "& .MuiSlider-thumb": {
                    width: 14,
                    height: 14,
                    bgcolor: "#1565C0",
                    border: "2px solid #e6edf3",
                  },
                  "& .MuiSlider-track": { height: 4 },
                  "& .MuiSlider-rail": {
                    height: 4,
                    bgcolor: "rgba(48,54,61,0.6)",
                  },
                }}
              />
            </Box>
          )}

          {/* Count caption */}
          <Box sx={{ px: 2, pb: 0.5 }}>
            <Typography
              variant="caption"
              sx={{ color: "#484f58", fontSize: "0.68rem" }}
            >
              {activeTab === "live"
                ? `${filteredForecasts.length} forecast${filteredForecasts.length === 1 ? "" : "s"}${lineFilter ? ` · ${lineFilter} line` : ""}${search.trim() ? ` · "${search}"` : ""}`
                : activeTab === "delays"
                  ? `${filteredDelays.length} delay${filteredDelays.length === 1 ? "" : "s"}${lineFilter ? ` · ${lineFilter} line` : ""}`
                  : activeTab === "history"
                    ? `${filteredCommonDelays.length} stop${filteredCommonDelays.length === 1 ? "" : "s"} with delay history${lineFilter ? ` · ${lineFilter} line` : ""}`
                    : `${filteredUsage.length} stop${filteredUsage.length === 1 ? "" : "s"} · estimated passengers at ${usageHour.toString().padStart(2, "0")}:00${lineFilter ? ` · ${lineFilter} line` : ""}`}
            </Typography>
          </Box>

          {/* ── Scrollable list ── */}
          {forecastsLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
              <CircularProgress size={24} sx={{ color: "#1565C0" }} />
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {/* ── LIVE FORECASTS LIST ── */}
              {activeTab === "live" &&
                filteredForecasts.map((f, idx) => {
                  const color = LINE_COLORS[f.line] ?? "#607D8B";
                  const selected = selectedStopId === f.stopId;
                  return (
                    <ListItemButton
                      key={`${f.stopId}-${f.direction}-${idx}`}
                      onClick={() =>
                        f.lat != null &&
                        f.lon != null &&
                        handleStopClick(f.lat, f.lon, f.stopId)
                      }
                      disabled={f.lat == null}
                      sx={{
                        py: 0.875,
                        px: 2,
                        bgcolor: selected
                          ? "rgba(21,101,192,0.12)"
                          : "transparent",
                        borderLeft: selected
                          ? "3px solid #1565C0"
                          : "3px solid transparent",
                        "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
                        transition: "all 0.12s",
                      }}
                    >
                      <Box
                        sx={{
                          width: 22,
                          height: 22,
                          borderRadius: "50%",
                          bgcolor: color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          mr: 1.5,
                        }}
                      >
                        <Typography
                          sx={{ fontSize: 9, fontWeight: 800, color: "#fff" }}
                        >
                          {f.line === "red" ? "R" : "G"}
                        </Typography>
                      </Box>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography
                          noWrap
                          sx={{
                            fontSize: "0.855rem",
                            fontWeight: selected ? 600 : 400,
                            color: selected ? "#e6edf3" : "#c9d1d9",
                            lineHeight: 1.25,
                          }}
                        >
                          {f.stopName}
                        </Typography>
                        <Typography
                          sx={{
                            fontSize: "0.68rem",
                            color: "#484f58",
                            lineHeight: 1.2,
                          }}
                        >
                          {f.direction} → {f.destination}
                        </Typography>
                      </Box>
                      <Chip
                        size="small"
                        label={
                          f.dueMins === null ? "No trams" : `${f.dueMins} min`
                        }
                        sx={{
                          fontSize: "0.62rem",
                          height: 18,
                          ml: 0.5,
                          flexShrink: 0,
                          bgcolor:
                            f.dueMins === null
                              ? "rgba(48,54,61,0.6)"
                              : f.dueMins <= 3
                                ? "#2ea04320"
                                : "#d2992220",
                          color:
                            f.dueMins === null
                              ? "#8b949e"
                              : f.dueMins <= 3
                                ? "#2ea043"
                                : "#d29922",
                          border: `1px solid ${f.dueMins === null ? "#484f58" : f.dueMins <= 3 ? "#2ea043" : "#d29922"}40`,
                        }}
                      />
                    </ListItemButton>
                  );
                })}

              {/* ── DELAYS LIST ── */}
              {activeTab === "delays" &&
                filteredDelays.map((d, idx) => {
                  const color = LINE_COLORS[d.line] ?? "#607D8B";
                  const delayColor =
                    d.delayMins >= 10
                      ? "#DC2626"
                      : d.delayMins >= 5
                        ? "#d29922"
                        : "#8b949e";
                  return (
                    <ListItemButton
                      key={`${d.stopId}-${d.direction}-${idx}`}
                      onClick={() => {
                        const coords = stopCoords.get(d.stopId);
                        if (coords)
                          handleStopClick(coords.lat, coords.lon, d.stopId);
                      }}
                      sx={{
                        py: 0.875,
                        px: 2,
                        "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
                        transition: "all 0.12s",
                      }}
                    >
                      <Box
                        sx={{
                          width: 22,
                          height: 22,
                          borderRadius: "50%",
                          bgcolor: color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          mr: 1.5,
                        }}
                      >
                        <Typography
                          sx={{ fontSize: 9, fontWeight: 800, color: "#fff" }}
                        >
                          {d.line === "red" ? "R" : "G"}
                        </Typography>
                      </Box>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography
                          noWrap
                          sx={{
                            fontSize: "0.855rem",
                            fontWeight: 500,
                            color: "#c9d1d9",
                            lineHeight: 1.25,
                          }}
                        >
                          {d.stopName}
                        </Typography>
                        <Typography
                          sx={{
                            fontSize: "0.68rem",
                            color: "#484f58",
                            lineHeight: 1.2,
                          }}
                        >
                          {d.direction} → {d.destination} · Due {d.dueMins} min
                        </Typography>
                      </Box>
                      <Chip
                        size="small"
                        label={`+${d.delayMins} min`}
                        sx={{
                          fontSize: "0.62rem",
                          height: 18,
                          ml: 0.5,
                          flexShrink: 0,
                          bgcolor: delayColor + "20",
                          color: delayColor,
                          border: `1px solid ${delayColor}40`,
                          fontWeight: 700,
                        }}
                      />
                    </ListItemButton>
                  );
                })}

              {/* ── USAGE LIST ── */}
              {activeTab === "usage" &&
                filteredUsage.map((u) => {
                  const color = LINE_COLORS[u.line] ?? "#607D8B";
                  const selected = selectedStopId === u.stopId;
                  return (
                    <ListItemButton
                      key={u.stopId}
                      onClick={() => {
                        if (u.lat != null && u.lon != null) {
                          handleStopClick(u.lat, u.lon, u.stopId);
                        } else {
                          const coords = stopCoords.get(u.stopId);
                          if (coords)
                            handleStopClick(coords.lat, coords.lon, u.stopId);
                        }
                      }}
                      sx={{
                        py: 0.875,
                        px: 2,
                        bgcolor: selected
                          ? "rgba(21,101,192,0.12)"
                          : "transparent",
                        borderLeft: selected
                          ? "3px solid #1565C0"
                          : "3px solid transparent",
                        "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
                        transition: "all 0.12s",
                      }}
                    >
                      {/* Line dot */}
                      <Box
                        sx={{
                          width: 22,
                          height: 22,
                          borderRadius: "50%",
                          bgcolor: color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          mr: 1.5,
                        }}
                      >
                        <Typography
                          sx={{ fontSize: 9, fontWeight: 800, color: "#fff" }}
                        >
                          {u.line === "red" ? "R" : "G"}
                        </Typography>
                      </Box>

                      {/* Stop name + inbound/outbound breakdown */}
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography
                          noWrap
                          sx={{
                            fontSize: "0.855rem",
                            fontWeight: selected ? 600 : 400,
                            color: selected ? "#e6edf3" : "#c9d1d9",
                            lineHeight: 1.25,
                          }}
                        >
                          {u.stopName}
                        </Typography>
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                          }}
                        >
                          <Typography
                            sx={{
                              fontSize: "0.68rem",
                              color: "#2ea043",
                              lineHeight: 1.2,
                              display: "flex",
                              alignItems: "center",
                              gap: 0.25,
                            }}
                          >
                            <ArrowDownwardIcon sx={{ fontSize: 10 }} />
                            {u.estimatedInboundPassengers.toLocaleString()} in
                          </Typography>
                          <Typography
                            sx={{
                              fontSize: "0.68rem",
                              color: "#d29922",
                              lineHeight: 1.2,
                              display: "flex",
                              alignItems: "center",
                              gap: 0.25,
                            }}
                          >
                            <ArrowUpwardIcon sx={{ fontSize: 10 }} />
                            {u.estimatedOutboundPassengers.toLocaleString()} out
                          </Typography>
                        </Box>
                      </Box>

                      {/* Total passengers badge */}
                      <Chip
                        size="small"
                        icon={
                          <PeopleIcon sx={{ fontSize: "12px !important" }} />
                        }
                        label={`${u.estimatedTotalPassengers.toLocaleString()}`}
                        sx={{
                          fontSize: "0.62rem",
                          height: 18,
                          ml: 0.5,
                          flexShrink: 0,
                          bgcolor:
                            u.estimatedTotalPassengers >= 500
                              ? "#DC262620"
                              : u.estimatedTotalPassengers >= 200
                                ? "#d2992220"
                                : "rgba(48,54,61,0.6)",
                          color:
                            u.estimatedTotalPassengers >= 500
                              ? "#DC2626"
                              : u.estimatedTotalPassengers >= 200
                                ? "#d29922"
                                : "#8b949e",
                          border: `1px solid ${u.estimatedTotalPassengers >= 500 ? "#DC2626" : u.estimatedTotalPassengers >= 200 ? "#d29922" : "#484f58"}40`,
                          fontWeight: 700,
                          "& .MuiChip-icon": {
                            color: "inherit",
                            ml: "4px",
                          },
                        }}
                      />
                    </ListItemButton>
                  );
                })}

              {/* Empty states */}
              {activeTab === "live" && filteredForecasts.length === 0 && (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <TramIcon sx={{ fontSize: 32, color: "#30363d", mb: 1 }} />
                  <Typography sx={{ fontSize: "0.8rem", color: "#484f58" }}>
                    No live forecasts at the moment
                  </Typography>
                </Box>
              )}
              {activeTab === "delays" && filteredDelays.length === 0 && (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <AccessTimeIcon
                    sx={{ fontSize: 32, color: "#2ea043", mb: 1 }}
                  />
                  <Typography sx={{ fontSize: "0.8rem", color: "#2ea043" }}>
                    No delays — all trams on schedule
                  </Typography>
                </Box>
              )}
              {activeTab === "usage" && filteredUsage.length === 0 && (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <PeopleIcon sx={{ fontSize: 32, color: "#30363d", mb: 1 }} />
                  <Typography sx={{ fontSize: "0.8rem", color: "#484f58" }}>
                    No usage data available
                  </Typography>
                </Box>
              )}

              {/* ── COMMON DELAYS HISTORY LIST ── */}
              {activeTab === "history" &&
                filteredCommonDelays.map((d) => {
                  const color = LINE_COLORS[d.line] ?? "#607D8B";
                  const selected = selectedStopId === d.stopId;
                  const delayColor =
                    d.avgDelayMins >= 10
                      ? "#DC2626"
                      : d.avgDelayMins >= 5
                        ? "#d29922"
                        : "#8b949e";
                  return (
                    <ListItemButton
                      key={d.stopId}
                      onClick={() => {
                        if (d.lat != null && d.lon != null) {
                          handleStopClick(d.lat, d.lon, d.stopId);
                        } else {
                          const coords = stopCoords.get(d.stopId);
                          if (coords)
                            handleStopClick(coords.lat, coords.lon, d.stopId);
                        }
                      }}
                      sx={{
                        py: 0.875,
                        px: 2,
                        bgcolor: selected
                          ? "rgba(21,101,192,0.12)"
                          : "transparent",
                        borderLeft: selected
                          ? "3px solid #1565C0"
                          : "3px solid transparent",
                        "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
                        transition: "all 0.12s",
                      }}
                    >
                      <Box
                        sx={{
                          width: 22,
                          height: 22,
                          borderRadius: "50%",
                          bgcolor: color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          mr: 1.5,
                        }}
                      >
                        <Typography
                          sx={{ fontSize: 9, fontWeight: 800, color: "#fff" }}
                        >
                          {d.line === "red" ? "R" : "G"}
                        </Typography>
                      </Box>

                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography
                          noWrap
                          sx={{
                            fontSize: "0.855rem",
                            fontWeight: selected ? 600 : 400,
                            color: selected ? "#e6edf3" : "#c9d1d9",
                            lineHeight: 1.25,
                          }}
                        >
                          {d.stopName}
                        </Typography>
                        <Typography
                          sx={{
                            fontSize: "0.68rem",
                            color: "#484f58",
                            lineHeight: 1.2,
                          }}
                        >
                          {d.delayCount} delays recorded · max +{d.maxDelayMins}{" "}
                          min
                        </Typography>
                      </Box>

                      <Chip
                        size="small"
                        icon={
                          <HistoryIcon sx={{ fontSize: "12px !important" }} />
                        }
                        label={`avg +${d.avgDelayMins} min`}
                        sx={{
                          fontSize: "0.62rem",
                          height: 18,
                          ml: 0.5,
                          flexShrink: 0,
                          bgcolor: delayColor + "20",
                          color: delayColor,
                          border: `1px solid ${delayColor}40`,
                          fontWeight: 700,
                          "& .MuiChip-icon": {
                            color: "inherit",
                            ml: "4px",
                          },
                        }}
                      />
                    </ListItemButton>
                  );
                })}
              {activeTab === "history" &&
                filteredCommonDelays.length === 0 && (
                  <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                    <HistoryIcon
                      sx={{ fontSize: 32, color: "#30363d", mb: 1 }}
                    />
                    <Typography sx={{ fontSize: "0.8rem", color: "#484f58" }}>
                      No delay history recorded yet
                    </Typography>
                  </Box>
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
              Luas · Red & Green Lines · GTFS + CSO Data
            </Typography>
          </Box>
        </Paper>
      )}
    </Box>
  );
};
