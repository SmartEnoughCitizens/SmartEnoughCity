/**
 * Car dashboard — full-viewport map with collapsible floating side panel.
 * Panel contains map controls (mode, day type, time slot, legend) and fuel stats.
 * EV Charging tab shows EVDashboard as a full overlay.
 */

import { useState } from "react";
import {
  Box,
  Chip,
  Divider,
  IconButton,
  Paper,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  CircularProgress,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import DirectionsCarIcon from "@mui/icons-material/DirectionsCar";
import EvStationIcon from "@mui/icons-material/EvStation";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Circle,
  Popup,
  Polyline,
} from "react-leaflet";
import {
  useCarFuelTypeStatistics,
  useCarHighTrafficPoints,
  useCarJunctionEmissions,
  useEvChargingStations,
  useEvChargingDemand,
  useEvAreasGeoJson,
  useTrafficRecommendations,
} from "@/hooks";
import type { TrafficRecommendation } from "@/types";
import { useAppSelector } from "@/store/hooks";
import { EVDashboard } from "./EVDashboard";
import "leaflet/dist/leaflet.css";

type DayTypeFilter = "weekday" | "weekend";
type TimeSlotFilter =
  | "morning_peak"
  | "inter_peak"
  | "evening_peak"
  | "off_peak";
type MapMode = "traffic" | "pollution";
type ColorBand = "low" | "medium" | "high";

const LEGEND_ITEMS: { band: ColorBand; color: string; label: string }[] = [
  { band: "low", color: "#16a34a", label: "Low" },
  { band: "medium", color: "#f97316", label: "Medium" },
  { band: "high", color: "#dc2626", label: "High" },
];

const SIDE_PANEL_WIDTH = 420;
const GAP = 16;

export const CarDashboard = () => {
  const [activeTab, setActiveTab] = useState(() => {
    const saved = localStorage.getItem("carDashboardActiveTab");
    return saved ? Number.parseInt(saved, 10) : 0;
  });
  const [panelOpen, setPanelOpen] = useState(true);
  const [dayTypeFilter, setDayTypeFilter] = useState<DayTypeFilter>("weekday");
  const [timeSlotFilter, setTimeSlotFilter] =
    useState<TimeSlotFilter>("morning_peak");
  const [mapMode, setMapMode] = useState<MapMode>("traffic");
  const [activeColors, setActiveColors] = useState<Set<ColorBand>>(
    () => new Set(["low", "medium", "high"]),
  );

  const [selectedRecommendation, setSelectedRecommendation] =
    useState<TrafficRecommendation | null>(null);

  const theme = useAppSelector((state) => state.ui.theme);

  const { data: stats, isLoading: statsLoading } = useCarFuelTypeStatistics();
  const { data: recommendations } = useTrafficRecommendations();
  const { data: trafficPoints, isLoading: trafficLoading } =
    useCarHighTrafficPoints();
  const { data: emissionPoints, isLoading: emissionsLoading } =
    useCarJunctionEmissions();

  // Prefetch EV data so the EV tab opens instantly
  useEvChargingStations();
  useEvChargingDemand();
  useEvAreasGeoJson();

  const toggleColor = (band: ColorBand) => {
    setActiveColors((prev) => {
      const next = new Set(prev);
      if (next.has(band)) {
        next.delete(band);
        if (next.size === 0) return new Set(["low", "medium", "high"]);
      } else {
        next.add(band);
      }
      return next;
    });
  };

  const handleTabChange = (_: React.SyntheticEvent, v: number) => {
    setActiveTab(v);
    localStorage.setItem("carDashboardActiveTab", v.toString());
  };

  // Traffic mode
  const filteredPoints = trafficPoints?.filter(
    (p) =>
      p.lat != null &&
      p.lon != null &&
      p.dayType === dayTypeFilter &&
      p.timeSlot === timeSlotFilter,
  );
  const maxVolume = filteredPoints?.length
    ? Math.max(...filteredPoints.map((p) => p.avgVolume))
    : 1;
  const getMarkerColor = (volume: number) => {
    const r = volume / maxVolume;
    if (r > 0.66) return "#dc2626";
    if (r > 0.33) return "#f97316";
    return "#16a34a";
  };
  const getVolumeBand = (volume: number): ColorBand => {
    const r = volume / maxVolume;
    if (r > 0.66) return "high";
    if (r > 0.33) return "medium";
    return "low";
  };

  // Pollution mode
  const filteredEmissions = emissionPoints?.filter(
    (p) =>
      p.lat != null &&
      p.lon != null &&
      p.dayType === dayTypeFilter &&
      p.timeSlot === timeSlotFilter,
  );
  const maxEmission = filteredEmissions?.length
    ? Math.max(...filteredEmissions.map((p) => p.totalEmissionG))
    : 1;
  const minEmission = filteredEmissions?.length
    ? Math.min(...filteredEmissions.map((p) => p.totalEmissionG))
    : 0;
  const getEmissionColor = (emission: number) => {
    const r =
      maxEmission > minEmission
        ? (emission - minEmission) / (maxEmission - minEmission)
        : 0;
    if (r > 0.66) return "#dc2626";
    if (r > 0.33) return "#f97316";
    return "#16a34a";
  };
  const getEmissionBand = (emission: number): ColorBand => {
    const r =
      maxEmission > minEmission
        ? (emission - minEmission) / (maxEmission - minEmission)
        : 0;
    if (r > 0.66) return "high";
    if (r > 0.33) return "medium";
    return "low";
  };

  const dublinCenter: [number, number] = [53.3498, -6.2603];
  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const tileAttribution =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  const isLoading =
    statsLoading ||
    trafficLoading ||
    (mapMode === "pollution" && emissionsLoading);

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
      {/* ── Full-viewport map (Traffic & Fuel tab only) ────────────── */}
      {activeTab === 0 && (
        <MapContainer
          center={dublinCenter}
          zoom={12}
          style={{
            position: "absolute",
            inset: 0,
            width: "100%",
            height: "100%",
          }}
          zoomControl={false}
        >
          <TileLayer attribution={tileAttribution} url={tileUrl} />

          {mapMode === "traffic" &&
            filteredPoints
              ?.filter((p) => activeColors.has(getVolumeBand(p.avgVolume)))
              .map((point, idx) => (
                <CircleMarker
                  key={`traffic-${point.siteId}-${idx}`}
                  center={[point.lat, point.lon]}
                  radius={6}
                  pathOptions={{
                    color: "#fff",
                    weight: 1.5,
                    fillColor: getMarkerColor(point.avgVolume),
                    fillOpacity: 0.8,
                  }}
                >
                  <Popup>
                    <strong>Site {point.siteId}</strong>
                    <br />
                    Avg Volume: {point.avgVolume.toFixed(2)}
                    <br />
                    Day Type: {point.dayType}
                    <br />
                    Time Slot: {point.timeSlot.replaceAll("_", " ")}
                  </Popup>
                </CircleMarker>
              ))}

          {mapMode === "pollution" &&
            filteredEmissions
              ?.filter((p) =>
                activeColors.has(getEmissionBand(p.totalEmissionG)),
              )
              .map((point, idx) => (
                <Circle
                  key={`pollution-${point.siteId}-${idx}`}
                  center={[point.lat, point.lon]}
                  radius={250}
                  pathOptions={{
                    color: getEmissionColor(point.totalEmissionG),
                    weight: 1,
                    fillColor: getEmissionColor(point.totalEmissionG),
                    fillOpacity: 0.45,
                  }}
                >
                  <Popup>
                    <strong>Site {point.siteId}</strong>
                    <br />
                    Total Emission: {(point.totalEmissionG / 1000).toFixed(
                      2,
                    )}{" "}
                    kg CO₂
                    <br />
                    Car Volume: {point.carVolume.toFixed(0)}
                    <br />
                    Day Type: {point.dayType}
                    <br />
                    Time Slot: {point.timeSlot.replaceAll("_", " ")}
                  </Popup>
                </Circle>
              ))}
          {/* Alternative route overlays for selected recommendation */}
          {selectedRecommendation?.alternativeRoutes.map((route) => (
            <Polyline
              key={route.routeId}
              positions={route.path.map((wp) => [wp.lat, wp.lon])}
              pathOptions={{ color: route.color, weight: 4, opacity: 0.85 }}
            >
              <Popup>
                <strong>{route.label}</strong>
                <br />
                {route.summary}
                <br />
                Saves ~{route.estimatedTimeSavingsMinutes} min · {route.distanceKm.toFixed(1)} km
              </Popup>
            </Polyline>
          ))}
        </MapContainer>
      )}

      {/* ── EV Dashboard overlay ───────────────────────────────────── */}
      <Box
        sx={{
          position: "absolute",
          inset: 0,
          visibility: activeTab === 1 ? "visible" : "hidden",
          opacity: activeTab === 1 ? 1 : 0,
          pointerEvents: activeTab === 1 ? "auto" : "none",
          transition: "opacity 0.15s ease-in-out",
        }}
      >
        <EVDashboard />
      </Box>

      {/* ── Persistent top-left tab toggle ────────────────────────────── */}
      <Box
        sx={{
          position: "absolute",
          top: GAP,
          left: GAP,
          zIndex: 1100,
        }}
      >
        <IconButton
          onClick={() =>
            handleTabChange({} as React.SyntheticEvent, activeTab === 0 ? 1 : 0)
          }
          title={activeTab === 0 ? "EV Charging" : "Back to Traffic"}
          sx={{
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          {activeTab === 0 ? <EvStationIcon /> : <DirectionsCarIcon />}
        </IconButton>
      </Box>

      {/* ── Hamburger (top-right, only when traffic panel is closed) ─── */}
      {activeTab === 0 && !panelOpen && (
        <Box
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            zIndex: 1000,
          }}
        >
          <IconButton
            onClick={() => setPanelOpen(true)}
            sx={{
              bgcolor: (t) => t.palette.background.paper,
              backdropFilter: "blur(12px)",
              "&:hover": { bgcolor: (t) => t.palette.background.paper },
            }}
          >
            <MenuOpenIcon />
          </IconButton>
        </Box>
      )}

      {/* ── Floating side panel ────────────────────────────────────── */}
      {activeTab === 0 && panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            bottom: GAP,
            width: SIDE_PANEL_WIDTH,
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
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <DirectionsCarIcon fontSize="small" color="primary" />
              <Typography variant="h5">Traffic & Fuel</Typography>
            </Box>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          <Divider />

          {/* Scrollable content */}
          <Box sx={{ flex: 1, overflow: "auto", px: 2, py: 1.5 }}>
            {/* Map mode */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 0.75, fontWeight: 600 }}
            >
              Map mode
            </Typography>
            <ToggleButtonGroup
              value={mapMode}
              exclusive
              onChange={(_, v) => v && setMapMode(v)}
              size="small"
              fullWidth
              sx={{ mb: 2 }}
            >
              <ToggleButton value="traffic">Traffic</ToggleButton>
              <ToggleButton value="pollution">Pollution</ToggleButton>
            </ToggleButtonGroup>

            {/* Day type */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 0.75, fontWeight: 600 }}
            >
              Day type
            </Typography>
            <ToggleButtonGroup
              value={dayTypeFilter}
              exclusive
              onChange={(_, v) => v && setDayTypeFilter(v)}
              size="small"
              fullWidth
              sx={{ mb: 2 }}
            >
              <ToggleButton value="weekday">Weekday</ToggleButton>
              <ToggleButton value="weekend">Weekend</ToggleButton>
            </ToggleButtonGroup>

            {/* Time slot */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 0.75, fontWeight: 600 }}
            >
              Time slot
            </Typography>
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1, mb: 2 }}>
              {(
                [
                  ["morning_peak", "Morning peak"],
                  ["inter_peak", "Inter peak"],
                  ["evening_peak", "Evening peak"],
                  ["off_peak", "Off peak"],
                ] as const
              ).map(([value, label]) => (
                <Box
                  key={value}
                  onClick={() => setTimeSlotFilter(value)}
                  sx={{
                    flex: "1 1 45%",
                    py: 0.75,
                    px: 1,
                    borderRadius: 1,
                    border: "1px solid",
                    borderColor:
                      timeSlotFilter === value ? "primary.main" : "divider",
                    bgcolor:
                      timeSlotFilter === value ? "primary.main" : "transparent",
                    color:
                      timeSlotFilter === value
                        ? "primary.contrastText"
                        : "text.secondary",
                    fontSize: "0.8125rem",
                    fontWeight: timeSlotFilter === value ? 600 : 400,
                    textAlign: "center",
                    cursor: "pointer",
                    userSelect: "none",
                    transition: "all 0.15s",
                    "&:hover": {
                      borderColor: "primary.main",
                      color:
                        timeSlotFilter === value
                          ? "primary.contrastText"
                          : "primary.main",
                    },
                  }}
                >
                  {label}
                </Box>
              ))}
            </Box>

            {/* Intensity filter */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 0.75, fontWeight: 600 }}
            >
              Filter by intensity
            </Typography>
            <Box sx={{ display: "flex", gap: 1, mb: 2.5 }}>
              {LEGEND_ITEMS.map(({ band, color, label }) => (
                <Chip
                  key={band}
                  label={label}
                  size="small"
                  onClick={() => toggleColor(band)}
                  sx={{
                    bgcolor: activeColors.has(band) ? color : "transparent",
                    color: activeColors.has(band) ? "#fff" : color,
                    border: `2px solid ${color}`,
                    fontWeight: 600,
                    cursor: "pointer",
                    "&:hover": { opacity: 0.85 },
                  }}
                />
              ))}
            </Box>

            <Divider sx={{ mb: 2 }} />

            {/* Fuel type stats */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 1, fontWeight: 600 }}
            >
              Vehicles by fuel type
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 0.75 }}>
              {stats?.map((stat) => (
                <Box
                  key={stat.fuelType}
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    px: 1.25,
                    py: 0.75,
                    borderRadius: 1.5,
                    bgcolor: (t) => t.palette.action.hover,
                  }}
                >
                  <Typography variant="body2" color="text.secondary">
                    {stat.fuelType}
                  </Typography>
                  <Typography variant="body2" fontWeight={700}>
                    {stat.count.toLocaleString()}
                  </Typography>
                </Box>
              ))}
            </Box>

            <Divider sx={{ my: 2 }} />

            {/* Traffic diversion recommendations */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 1, fontWeight: 600 }}
            >
              Traffic diversion recommendations
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
              {recommendations?.map((rec) => {
                const isSelected =
                  selectedRecommendation?.recommendationId ===
                  rec.recommendationId;
                const levelColor =
                  rec.congestionLevel === "critical"
                    ? "#dc2626"
                    : rec.congestionLevel === "high"
                      ? "#f97316"
                      : "#ca8a04";
                return (
                  <Box
                    key={rec.recommendationId}
                    onClick={() =>
                      setSelectedRecommendation(isSelected ? null : rec)
                    }
                    sx={{
                      px: 1.5,
                      py: 1,
                      borderRadius: 1.5,
                      border: "1px solid",
                      borderColor: isSelected ? "primary.main" : "divider",
                      bgcolor: isSelected
                        ? "primary.main"
                        : (t) => t.palette.action.hover,
                      color: isSelected ? "primary.contrastText" : "inherit",
                      cursor: "pointer",
                      transition: "all 0.15s",
                      "&:hover": { borderColor: "primary.main" },
                    }}
                  >
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "flex-start",
                        mb: 0.5,
                      }}
                    >
                      <Typography
                        variant="body2"
                        fontWeight={600}
                        sx={{ lineHeight: 1.3, flex: 1, pr: 1 }}
                      >
                        {rec.title}
                      </Typography>
                      <Chip
                        label={rec.congestionLevel}
                        size="small"
                        sx={{
                          bgcolor: levelColor,
                          color: "#fff",
                          fontWeight: 700,
                          fontSize: "0.7rem",
                          height: 20,
                          flexShrink: 0,
                        }}
                      />
                    </Box>
                    <Typography
                      variant="caption"
                      sx={{
                        display: "block",
                        mb: 0.75,
                        color: isSelected
                          ? "primary.contrastText"
                          : "text.secondary",
                      }}
                    >
                      {rec.summary}
                    </Typography>
                    <Box sx={{ display: "flex", gap: 1.5, flexWrap: "wrap" }}>
                      <Typography
                        variant="caption"
                        fontWeight={600}
                        sx={{
                          color: isSelected
                            ? "primary.contrastText"
                            : "text.secondary",
                        }}
                      >
                        Confidence: {Math.round(rec.confidenceScore * 100)}%
                      </Typography>
                      {rec.alternativeRoutes.map((r) => (
                        <Typography
                          key={r.routeId}
                          variant="caption"
                          sx={{
                            color: isSelected ? "primary.contrastText" : r.color,
                            fontWeight: 600,
                          }}
                        >
                          -{r.estimatedTimeSavingsMinutes} min
                        </Typography>
                      ))}
                    </Box>
                  </Box>
                );
              })}
              {!recommendations?.length && (
                <Typography variant="caption" color="text.secondary">
                  No recommendations available.
                </Typography>
              )}
            </Box>
          </Box>
        </Paper>
      )}
    </Box>
  );
};
