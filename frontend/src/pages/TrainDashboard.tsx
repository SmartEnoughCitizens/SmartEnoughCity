/**
 * Train dashboard — Google Maps-style smart city view
 *
 * Features:
 *  - Full-viewport map: OSM (light) / Carto dark (dark) — matches all other dashboards
 *  - Left glass panel: KPI strip, type filters, Stations | Trains tab, search
 *  - Custom coloured station markers per type (DART / Suburban / Mainline)
 *  - Smart live-train markers: type-coloured + directional arrow (↑↓←→)
 *  - Type filter applies to BOTH station markers and train markers on the map
 *  - Trains tab: list every live train with type, direction, status; click → fly to it
 *  - Map-legend overlay (bottom-left)
 *  - Click a station OR train in the list → map flies to it
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
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import TrainIcon from "@mui/icons-material/Train";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import SpeedIcon from "@mui/icons-material/Speed";
import SearchIcon from "@mui/icons-material/Search";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import { useTrainData, useTrainKpis, useTrainLiveTrains } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// ── Constants ───────────────────────────────────────────────────────

const DUBLIN_CENTER: [number, number] = [53.3498, -6.2603];

type TypeKey = "ALL" | "D" | "S" | "M";
type PanelTab = "stations" | "trains";

const TYPE_CONFIG: Record<
  TypeKey,
  { label: string; color: string; short: string }
> = {
  ALL: { label: "All", color: "#607D8B", short: "•" },
  D: { label: "DART", color: "#00ACC1", short: "D" },
  S: { label: "Suburban", color: "#1976D2", short: "S" },
  M: { label: "Mainline", color: "#7B1FA2", short: "M" },
};

// ── CSS ──────────────────────────────────────────────────────────────

const INJECTED_CSS = `
  @keyframes trainPulse {
    0%   { box-shadow: 0 0 0 0    rgba(46,160,67,0.75); }
    70%  { box-shadow: 0 0 0 14px rgba(46,160,67,0);    }
    100% { box-shadow: 0 0 0 0    rgba(46,160,67,0);    }
  }
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
  .stn-pin {
    border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-family: -apple-system, BlinkMacSystemFont, sans-serif;
    font-weight: 800; color: #fff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.50), 0 0 0 2px rgba(255,255,255,0.20);
    transition: transform 0.15s ease; cursor: pointer;
  }
  .stn-pin:hover { transform: scale(1.25); }
  .trn-dot {
    border-radius: 50%;
    border: 2px solid rgba(255,255,255,0.75);
    display: flex; align-items: center; justify-content: center;
    font-family: -apple-system, BlinkMacSystemFont, sans-serif;
    font-weight: 900; color: #fff; font-size: 11px;
    box-shadow: 0 2px 6px rgba(0,0,0,0.4);
    cursor: pointer;
  }
  .trn-dot.running { animation: trainPulse 1.6s ease-out infinite; }
`;

// ── Helpers ──────────────────────────────────────────────────────────

/** Map an Irish Rail trainType string to our TypeKey */
function getTrainTypeKey(trainType?: string | null): TypeKey {
  if (!trainType) return "ALL";
  const t = trainType.toUpperCase();
  if (t.includes("DART")) return "D";
  if (t.includes("MAIN") || t === "M") return "M";
  if (t.includes("SUB") || t === "S") return "S";
  return "ALL";
}

/** Return a Unicode directional arrow for a direction string, or "" */
function getDirectionArrow(direction?: string | null): string {
  if (!direction) return "";
  const d = direction.toLowerCase();
  if (d.includes("north")) return "↑";
  if (d.includes("south")) return "↓";
  if (d.includes("east")) return "→";
  if (d.includes("west")) return "←";
  return "";
}

const isRunning = (status?: string | null): boolean => {
  if (!status) return false;
  const s = status.toUpperCase();
  return s === "R" || s === "RUNNING";
};

// ── Leaflet icon factories ───────────────────────────────────────────

function makeStationIcon(type: string | null): L.DivIcon {
  const cfg = TYPE_CONFIG[(type as TypeKey) ?? "ALL"] ?? TYPE_CONFIG.ALL;
  return L.divIcon({
    html: `<div class="stn-pin" style="width:26px;height:26px;background:${cfg.color};font-size:10px;">${cfg.short}</div>`,
    className: "",
    iconSize: [26, 26],
    iconAnchor: [13, 13],
    popupAnchor: [0, -16],
  });
}

/**
 * Live train marker:
 *  - Coloured by train type (matches station pin colours)
 *  - Shows directional arrow (↑↓←→) when direction is known, otherwise type letter
 *  - Pulses when running
 */
function makeLiveTrainIcon(
  trainType: string | null,
  direction: string | null,
  running: boolean,
): L.DivIcon {
  const typeKey = getTrainTypeKey(trainType);
  // Use type colour; fall back to green/orange for unknown type
  const color =
    typeKey === "ALL"
      ? running
        ? "#2ea043"
        : "#d29922"
      : TYPE_CONFIG[typeKey].color;
  const dirArrow = getDirectionArrow(direction);
  // If we know direction show that; otherwise show type letter so user knows the service
  const inner = dirArrow || TYPE_CONFIG[typeKey].short;

  return L.divIcon({
    html: `<div class="trn-dot${running ? " running" : ""}" style="width:22px;height:22px;background:${color};">${inner}</div>`,
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

// ── KPI Card ─────────────────────────────────────────────────────────

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

// ── Map Legend ───────────────────────────────────────────────────────

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
        minWidth: 130,
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
        Station / Train Type
      </Typography>
      {(["D", "S", "M"] as TypeKey[]).map((t) => {
        const cfg = TYPE_CONFIG[t];
        return (
          <Box
            key={t}
            sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.5 }}
          >
            <Box
              sx={{
                width: 14,
                height: 14,
                borderRadius: "50%",
                bgcolor: cfg.color,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                flexShrink: 0,
              }}
            >
              <Typography sx={{ fontSize: 7, fontWeight: 800, color: "#fff" }}>
                {cfg.short}
              </Typography>
            </Box>
            <Typography
              variant="caption"
              sx={{ color: "#c9d1d9", fontSize: "0.7rem" }}
            >
              {cfg.label}
            </Typography>
          </Box>
        );
      })}
      <Divider sx={{ my: 0.75, borderColor: "rgba(255,255,255,0.07)" }} />
      <Typography
        variant="caption"
        sx={{
          color: "#484f58",
          textTransform: "uppercase",
          letterSpacing: 0.8,
          fontSize: "0.6rem",
          display: "block",
          mb: 0.5,
        }}
      >
        Train Status
      </Typography>
      {[
        { color: "#2ea043", label: "Running  (pulses)" },
        { color: "#d29922", label: "Scheduled" },
      ].map(({ color, label }) => (
        <Box
          key={label}
          sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.4 }}
        >
          <Box
            sx={{
              width: 11,
              height: 11,
              borderRadius: "50%",
              bgcolor: color,
              flexShrink: 0,
            }}
          />
          <Typography
            variant="caption"
            sx={{ color: "#c9d1d9", fontSize: "0.7rem" }}
          >
            {label}
          </Typography>
        </Box>
      ))}
      <Box sx={{ mt: 0.75 }}>
        <Typography
          variant="caption"
          sx={{ color: "#484f58", fontSize: "0.6rem" }}
        >
          ↑↓←→ = direction of travel
        </Typography>
      </Box>
    </Box>
  );
}

// ── Main Component ───────────────────────────────────────────────────

export const TrainDashboard = () => {
  const [panelOpen, setPanelOpen] = useState(true);
  const [activeTab, setActiveTab] = useState<PanelTab>("stations");
  const [typeFilter, setTypeFilter] = useState<TypeKey>("ALL");
  const [search, setSearch] = useState("");
  const [flyTarget, setFlyTarget] = useState<{
    center: [number, number];
    id: number;
  } | null>(null);
  const [selectedStationCode, setSelectedStationCode] = useState<string | null>(
    null,
  );
  const [selectedTrainCode, setSelectedTrainCode] = useState<string | null>(
    null,
  );

  const theme = useAppSelector((state) => state.ui.theme);
  const { data: trainData, isLoading: dataLoading, error } = useTrainData(500);
  const { data: kpiData } = useTrainKpis();
  const { data: liveTrains = [] } = useTrainLiveTrains();

  // Inject CSS once
  useEffect(() => {
    const el = document.createElement("style");
    el.dataset["trainUi"] = "1";
    el.innerHTML = INJECTED_CSS;
    document.head.append(el);
    return () => {
      el.remove();
    };
  }, []);

  // Same tile choice as every other dashboard: OSM light, Carto dark
  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const tileAttr =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

  // Filtered stations (for panel list + map markers)
  const filteredStations = useMemo(() => {
    let list = trainData?.data ?? [];
    if (typeFilter !== "ALL")
      list = list.filter((s) => s.stationType === typeFilter);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (s) =>
          s.stationDesc?.toLowerCase().includes(q) ||
          s.stationCode?.toLowerCase().includes(q),
      );
    }
    return list;
  }, [trainData, typeFilter, search]);

  // Filtered live trains (for panel list + map markers — type filter applies to both)
  const filteredLiveTrains = useMemo(() => {
    if (typeFilter === "ALL") return liveTrains;
    return liveTrains.filter(
      (t) => getTrainTypeKey(t.trainType) === typeFilter,
    );
  }, [liveTrains, typeFilter]);

  const runningCount = filteredLiveTrains.filter((t) =>
    isRunning(t.status),
  ).length;
  const scheduledCount = filteredLiveTrains.length - runningCount;

  const handleStationClick = useCallback(
    (lat: number, lon: number, code: string) => {
      setSelectedStationCode(code);
      setSelectedTrainCode(null);
      setFlyTarget({ center: [lat, lon], id: Date.now() });
    },
    [],
  );

  const handleTrainClick = useCallback(
    (lat: number, lon: number, code: string) => {
      setSelectedTrainCode(code);
      setSelectedStationCode(null);
      setFlyTarget({ center: [lat, lon], id: Date.now() });
    },
    [],
  );

  // When switching to Trains tab, clear station selection (and vice-versa)
  const switchTab = (tab: PanelTab) => {
    setActiveTab(tab);
    if (tab === "trains") setSelectedStationCode(null);
    else setSelectedTrainCode(null);
  };

  const PANEL_W = 380;

  // Sort trains: running first
  const sortedTrains = useMemo(
    () =>
      filteredLiveTrains.toSorted((a, b) => {
        const aR = isRunning(a.status) ? 0 : 1;
        const bR = isRunning(b.status) ? 0 : 1;
        return aR - bR || (a.trainCode ?? "").localeCompare(b.trainCode ?? "");
      }),
    [filteredLiveTrains],
  );

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
          zoom={11}
          style={{ height: "100%", width: "100%" }}
          zoomControl={false}
        >
          <TileLayer attribution={tileAttr} url={tileUrl} />
          <MapController target={flyTarget} />

          {/* Station markers — always visible, filtered by type */}
          {filteredStations.map((station) => (
            <Marker
              key={station.id}
              position={[station.lat, station.lon]}
              icon={makeStationIcon(station.stationType ?? null)}
              eventHandlers={{
                click: () =>
                  handleStationClick(
                    station.lat,
                    station.lon,
                    station.stationCode,
                  ),
              }}
            >
              <Popup>
                <Box sx={{ minWidth: 180 }}>
                  <Typography
                    fontWeight={700}
                    sx={{ fontSize: "0.95rem", color: "#e6edf3", mb: 0.75 }}
                  >
                    {station.stationDesc}
                  </Typography>
                  <Box sx={{ display: "flex", gap: 0.75, flexWrap: "wrap" }}>
                    <Chip
                      size="small"
                      label={station.stationCode}
                      sx={{
                        fontSize: "0.65rem",
                        height: 18,
                        bgcolor: "rgba(255,255,255,0.07)",
                        color: "#8b949e",
                      }}
                    />
                    {station.stationType &&
                      (() => {
                        const cfg = TYPE_CONFIG[station.stationType as TypeKey];
                        return cfg ? (
                          <Chip
                            size="small"
                            label={cfg.label}
                            sx={{
                              fontSize: "0.65rem",
                              height: 18,
                              bgcolor: cfg.color + "22",
                              color: cfg.color,
                              border: `1px solid ${cfg.color}44`,
                            }}
                          />
                        ) : null;
                      })()}
                  </Box>
                  {station.stationAlias && (
                    <Typography
                      sx={{ fontSize: "0.72rem", color: "#8b949e", mt: 0.5 }}
                    >
                      Also: {station.stationAlias}
                    </Typography>
                  )}
                </Box>
              </Popup>
            </Marker>
          ))}

          {/* Live train markers — filtered by type, coloured by type, directional */}
          {filteredLiveTrains
            .filter((t) => t.lat && t.lon)
            .map((t) => {
              const running = isRunning(t.status);
              const dirArrow = getDirectionArrow(t.direction);
              const typeKey = getTrainTypeKey(t.trainType);
              const typCfg = TYPE_CONFIG[typeKey];
              return (
                <Marker
                  key={t.trainCode}
                  position={[t.lat, t.lon]}
                  icon={makeLiveTrainIcon(
                    t.trainType ?? null,
                    t.direction ?? null,
                    running,
                  )}
                  eventHandlers={{
                    click: () => handleTrainClick(t.lat, t.lon, t.trainCode),
                  }}
                >
                  <Popup>
                    <Box sx={{ minWidth: 190 }}>
                      <Typography
                        fontWeight={700}
                        sx={{ fontSize: "0.9rem", color: "#e6edf3", mb: 0.5 }}
                      >
                        🚂 {t.trainCode}
                      </Typography>
                      <Box
                        sx={{
                          display: "flex",
                          gap: 0.5,
                          mb: 0.5,
                          flexWrap: "wrap",
                        }}
                      >
                        <Chip
                          size="small"
                          label={running ? "● Running" : "○ Scheduled"}
                          sx={{
                            fontSize: "0.65rem",
                            height: 18,
                            bgcolor: (running ? "#2ea043" : "#d29922") + "22",
                            color: running ? "#2ea043" : "#d29922",
                          }}
                        />
                        {t.trainType && (
                          <Chip
                            size="small"
                            label={
                              typCfg.label === "All"
                                ? t.trainType
                                : typCfg.label
                            }
                            sx={{
                              fontSize: "0.65rem",
                              height: 18,
                              bgcolor: typCfg.color + "22",
                              color: typCfg.color,
                              border: `1px solid ${typCfg.color}44`,
                            }}
                          />
                        )}
                      </Box>
                      {t.direction && (
                        <Typography
                          sx={{ fontSize: "0.72rem", color: "#c9d1d9" }}
                        >
                          {dirArrow} {t.direction}
                        </Typography>
                      )}
                      {t.publicMessage && (
                        <Typography
                          sx={{
                            fontSize: "0.72rem",
                            color: "#8b949e",
                            mt: 0.4,
                          }}
                        >
                          {t.publicMessage}
                        </Typography>
                      )}
                    </Box>
                  </Popup>
                </Marker>
              );
            })}
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
          Failed to load train data
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
                    "linear-gradient(135deg, #1565C0 0%, #0D47A1 100%)",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  boxShadow: "0 2px 8px rgba(21,101,192,0.4)",
                }}
              >
                <TrainIcon sx={{ fontSize: 18, color: "#fff" }} />
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
                  Dublin Rail Network
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
                    LIVE · Greater Dublin Area
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
          {kpiData && (
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
                label="Stations"
                value={kpiData.totalStations}
                accent="#00ACC1"
              />
              <KpiCard
                icon={<TrainIcon fontSize="inherit" />}
                label="Live Trains"
                value={kpiData.liveTrainsRunning}
                accent="#2ea043"
              />
              <KpiCard
                icon={<AccessTimeIcon fontSize="inherit" />}
                label="On Time"
                value={`${kpiData.onTimePct.toFixed(1)}%`}
                accent={kpiData.onTimePct >= 80 ? "#2ea043" : "#d29922"}
              />
              <KpiCard
                icon={<SpeedIcon fontSize="inherit" />}
                label="Avg Delay"
                value={`${kpiData.avgDelayMinutes.toFixed(1)} min`}
                accent={kpiData.avgDelayMinutes <= 2 ? "#2ea043" : "#d29922"}
              />
            </Box>
          )}

          {/* Running / Scheduled summary */}
          {liveTrains.length > 0 && (
            <Box sx={{ px: 1.75, pb: 1.25, display: "flex", gap: 0.75 }}>
              <Chip
                size="small"
                label={`${runningCount} Running`}
                sx={{
                  fontSize: "0.68rem",
                  bgcolor: "#2ea04320",
                  color: "#2ea043",
                  border: "1px solid #2ea04340",
                }}
              />
              <Chip
                size="small"
                label={`${scheduledCount} Scheduled`}
                sx={{
                  fontSize: "0.68rem",
                  bgcolor: "#d2992220",
                  color: "#d29922",
                  border: "1px solid #d2992240",
                }}
              />
            </Box>
          )}

          <Divider sx={{ borderColor: "rgba(48,54,61,0.5)" }} />

          {/* Tab toggle: Stations | Trains */}
          <Box
            sx={{
              px: 1.75,
              pt: 1.25,
              pb: 1,
              display: "flex",
              gap: 0.5,
            }}
          >
            {(["stations", "trains"] as PanelTab[]).map((tab) => {
              const active = activeTab === tab;
              const label = tab === "stations" ? "Stations" : "Live Trains";
              const count =
                tab === "stations"
                  ? filteredStations.length
                  : sortedTrains.filter((t) => t.lat && t.lon).length;
              return (
                <Box
                  key={tab}
                  onClick={() => switchTab(tab)}
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
                </Box>
              );
            })}
          </Box>

          {/* Type filter (applies to both tabs — filters map AND list) */}
          <Box sx={{ px: 1.75, pb: 1 }}>
            <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
              {(["ALL", "D", "S", "M"] as TypeKey[]).map((key) => {
                const active = typeFilter === key;
                const accent =
                  key === "ALL" ? "#1565C0" : TYPE_CONFIG[key].color;
                return (
                  <Chip
                    key={key}
                    size="small"
                    label={TYPE_CONFIG[key].label}
                    onClick={() => setTypeFilter(key)}
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

          {/* Search — only shown on Stations tab */}
          {activeTab === "stations" && (
            <Box sx={{ px: 1.75, pb: 1.25 }}>
              <TextField
                size="small"
                fullWidth
                placeholder="Search stations…"
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
          )}

          {/* Count caption */}
          <Box sx={{ px: 2, pb: 0.5 }}>
            <Typography
              variant="caption"
              sx={{ color: "#484f58", fontSize: "0.68rem" }}
            >
              {activeTab === "stations"
                ? `${filteredStations.length} station${filteredStations.length === 1 ? "" : "s"}${typeFilter === "ALL" ? "" : ` · ${TYPE_CONFIG[typeFilter].label}`}${search.trim() ? ` · "${search}"` : ""}`
                : `${sortedTrains.length} train${sortedTrains.length === 1 ? "" : "s"} on map${typeFilter === "ALL" ? "" : ` · ${TYPE_CONFIG[typeFilter].label}`}`}
            </Typography>
          </Box>

          {/* ── Scrollable list ── */}
          {dataLoading && activeTab === "stations" ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
              <CircularProgress size={24} sx={{ color: "#1565C0" }} />
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {/* ── STATIONS LIST ── */}
              {activeTab === "stations" &&
                filteredStations.map((station) => {
                  const typCfg =
                    TYPE_CONFIG[(station.stationType as TypeKey) ?? "ALL"] ??
                    TYPE_CONFIG.ALL;
                  const selected = selectedStationCode === station.stationCode;
                  return (
                    <ListItemButton
                      key={station.id}
                      onClick={() =>
                        handleStationClick(
                          station.lat,
                          station.lon,
                          station.stationCode,
                        )
                      }
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
                          bgcolor: typCfg.color,
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
                          {typCfg.short}
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
                          {station.stationDesc}
                        </Typography>
                        <Typography
                          sx={{
                            fontSize: "0.68rem",
                            color: "#484f58",
                            lineHeight: 1.2,
                          }}
                        >
                          {station.stationCode} · {typCfg.label}
                        </Typography>
                      </Box>
                      {selected && (
                        <Box
                          sx={{
                            width: 6,
                            height: 6,
                            borderRadius: "50%",
                            bgcolor: "#1565C0",
                            flexShrink: 0,
                            ml: 1,
                          }}
                        />
                      )}
                    </ListItemButton>
                  );
                })}

              {/* ── TRAINS LIST ── */}
              {activeTab === "trains" &&
                sortedTrains.map((t) => {
                  const running = isRunning(t.status);
                  const typeKey = getTrainTypeKey(t.trainType);
                  const typCfg = TYPE_CONFIG[typeKey];
                  const dirArrow = getDirectionArrow(t.direction);
                  const hasCoors = t.lat && t.lon;
                  const selected = selectedTrainCode === t.trainCode;

                  return (
                    <ListItemButton
                      key={t.trainCode}
                      onClick={() =>
                        hasCoors && handleTrainClick(t.lat, t.lon, t.trainCode)
                      }
                      disabled={!hasCoors}
                      sx={{
                        py: 0.875,
                        px: 2,
                        bgcolor: selected
                          ? "rgba(21,101,192,0.12)"
                          : "transparent",
                        borderLeft: selected
                          ? "3px solid #1565C0"
                          : "3px solid transparent",
                        opacity: hasCoors ? 1 : 0.45,
                        "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
                        transition: "all 0.12s",
                      }}
                    >
                      {/* Type + status dot */}
                      <Box
                        sx={{
                          width: 22,
                          height: 22,
                          borderRadius: "50%",
                          bgcolor: typCfg.color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          mr: 1.5,
                          boxShadow: running
                            ? `0 0 0 3px ${typCfg.color}44`
                            : "none",
                          transition: "box-shadow 0.3s",
                        }}
                      >
                        <Typography
                          sx={{ fontSize: 10, fontWeight: 900, color: "#fff" }}
                        >
                          {dirArrow || typCfg.short}
                        </Typography>
                      </Box>

                      {/* Code + type + direction */}
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 0.75,
                          }}
                        >
                          <Typography
                            sx={{
                              fontSize: "0.855rem",
                              fontWeight: 600,
                              color: selected ? "#e6edf3" : "#c9d1d9",
                              lineHeight: 1.25,
                            }}
                          >
                            {t.trainCode}
                          </Typography>
                          {!hasCoors && (
                            <Typography
                              sx={{ fontSize: "0.62rem", color: "#484f58" }}
                            >
                              (no GPS)
                            </Typography>
                          )}
                        </Box>
                        <Typography
                          sx={{
                            fontSize: "0.68rem",
                            color: "#484f58",
                            lineHeight: 1.2,
                          }}
                        >
                          {typCfg.label === "All"
                            ? (t.trainType ?? "Unknown")
                            : typCfg.label}
                          {t.direction ? ` · ${dirArrow} ${t.direction}` : ""}
                        </Typography>
                      </Box>

                      {/* Status badge */}
                      <Chip
                        size="small"
                        label={running ? "Running" : "Sched."}
                        sx={{
                          fontSize: "0.62rem",
                          height: 18,
                          ml: 0.5,
                          flexShrink: 0,
                          bgcolor: (running ? "#2ea043" : "#d29922") + "20",
                          color: running ? "#2ea043" : "#d29922",
                          border: `1px solid ${running ? "#2ea043" : "#d29922"}40`,
                        }}
                      />
                    </ListItemButton>
                  );
                })}

              {/* Empty state for trains */}
              {activeTab === "trains" && sortedTrains.length === 0 && (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <TrainIcon sx={{ fontSize: 32, color: "#30363d", mb: 1 }} />
                  <Typography sx={{ fontSize: "0.8rem", color: "#484f58" }}>
                    No live trains at the moment
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
              Irish Rail · Greater Dublin Area · Real-time data
            </Typography>
          </Box>
        </Paper>
      )}
    </Box>
  );
};
