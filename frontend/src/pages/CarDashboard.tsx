/**
 * Car dashboard — 3 tabs: Car (traffic/pollution map), EV Charging, Pedestrians (map + analysis).
 */

import { useState } from "react";
import {
  Box,
  Chip,
  Divider,
  IconButton,
  Paper,
  Tab,
  Tabs,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  CircularProgress,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import DirectionsCarIcon from "@mui/icons-material/DirectionsCar";
import EvStationIcon from "@mui/icons-material/EvStation";
import DirectionsWalkIcon from "@mui/icons-material/DirectionsWalk";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import PeopleIcon from "@mui/icons-material/People";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Circle,
  Popup,
} from "react-leaflet";
import {
  useCarFuelTypeStatistics,
  useCarHighTrafficPoints,
  useCarJunctionEmissions,
  useEvChargingStations,
  useEvChargingDemand,
  useEvAreasGeoJson,
  usePedestriansLive,
} from "@/hooks";
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
type ActiveMode = "traffic" | "ev" | "pedestrian";
type ColorBand = "low" | "medium" | "high";

const LEGEND_ITEMS: { band: ColorBand; color: string; label: string }[] = [
  { band: "low", color: "#16a34a", label: "Low" },
  { band: "medium", color: "#f97316", label: "Medium" },
  { band: "high", color: "#dc2626", label: "High" },
];

const SIDE_PANEL_WIDTH = 420;
const GAP = 16;
const PEDESTRIAN_PANEL_WIDTH = 340;

// ── Pedestrian map + analysis tab ─────────────────────────────────────────────

function PedestrianTab({ theme }: { theme: string }) {
  const [panelOpen, setPanelOpen] = useState(true);
  const { data: pedestrians = [], isLoading } = usePedestriansLive(20);

  const dublinCenter: [number, number] = [53.3498, -6.2603];
  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const tileAttribution =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  const withCoords = pedestrians.filter(
    (s) => Number.isFinite(s.lat) && Number.isFinite(s.lon),
  );

  const maxCount =
    withCoords.length > 0
      ? Math.max(...withCoords.map((s) => s.totalCount))
      : 1;

  const getPedColor = (count: number) => {
    const r = count / maxCount;
    if (r > 0.66) return "#dc2626";
    if (r > 0.33) return "#f97316";
    return "#10B981";
  };

  const totalFootfall = pedestrians.reduce((s, p) => s + p.totalCount, 0);
  const sorted = pedestrians.toSorted((a, b) => b.totalCount - a.totalCount);
  const busiest = sorted[0];
  const quietest = sorted.findLast((p) => p.totalCount > 0);
  const highCount = pedestrians.filter(
    (p) => p.totalCount / maxCount > 0.66,
  ).length;
  const avgCount =
    pedestrians.length > 0 ? Math.round(totalFootfall / pedestrians.length) : 0;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Map */}
      {isLoading ? (
        <Box
          sx={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            height: "100%",
          }}
        >
          <CircularProgress sx={{ color: "#0891B2" }} />
        </Box>
      ) : (
        <MapContainer
          center={dublinCenter}
          zoom={13}
          style={{
            position: "absolute",
            inset: 0,
            width: "100%",
            height: "100%",
          }}
          zoomControl={false}
        >
          <TileLayer attribution={tileAttribution} url={tileUrl} />
          {withCoords.map((site) => (
            <CircleMarker
              key={site.siteId}
              center={[site.lat, site.lon]}
              radius={Math.max(
                8,
                Math.min(24, (site.totalCount / maxCount) * 24),
              )}
              pathOptions={{
                color: "#fff",
                weight: 1.5,
                fillColor: getPedColor(site.totalCount),
                fillOpacity: 0.82,
              }}
            >
              <Popup>
                <strong>{site.siteName}</strong>
                <br />
                Count: {site.totalCount.toLocaleString()} / 15 min
                <br />
                {site.lastUpdated && (
                  <>
                    Updated:{" "}
                    {new Date(site.lastUpdated).toLocaleTimeString("en-IE")}
                  </>
                )}
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
      )}

      {/* Hamburger when panel closed */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* Analysis panel — matches Car tab style */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            bottom: GAP,
            width: PEDESTRIAN_PANEL_WIDTH,
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
              <DirectionsWalkIcon fontSize="small" color="primary" />
              <Typography variant="h5">Pedestrian Activity</Typography>
            </Box>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          <Divider />

          <Box sx={{ flex: 1, overflow: "auto", px: 2, py: 1.5 }}>
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ display: "block", mb: 0.5, fontWeight: 600 }}
            >
              Eco Counter · latest batch · {pedestrians.length} sites
            </Typography>

            {/* KPI row */}
            <Box
              sx={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 1,
                mb: 2,
              }}
            >
              {[
                {
                  icon: <PeopleIcon sx={{ fontSize: 15 }} />,
                  label: "Total footfall",
                  value: totalFootfall.toLocaleString(),
                  color: "#0891B2",
                },
                {
                  icon: <TrendingUpIcon sx={{ fontSize: 15 }} />,
                  label: "Avg per site",
                  value: avgCount.toLocaleString(),
                  color: "#7C3AED",
                },
              ].map(({ icon, label, value, color }) => (
                <Box
                  key={label}
                  sx={{
                    px: 1.5,
                    py: 1,
                    borderRadius: 1.5,
                    bgcolor: (t) => t.palette.action.hover,
                  }}
                >
                  <Box
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      gap: 0.5,
                      color,
                      mb: 0.25,
                    }}
                  >
                    {icon}
                    <Typography
                      sx={{ fontSize: "0.62rem", color: "text.disabled" }}
                    >
                      {label}
                    </Typography>
                  </Box>
                  <Typography
                    fontWeight={700}
                    sx={{ fontSize: "1.05rem", color }}
                  >
                    {value}
                  </Typography>
                </Box>
              ))}
            </Box>

            <Divider sx={{ mb: 1.5 }} />

            {/* Quick stats */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontWeight: 600, display: "block", mb: 1 }}
            >
              Quick insights
            </Typography>
            <Box
              sx={{
                display: "flex",
                flexDirection: "column",
                gap: 0.75,
                mb: 2,
              }}
            >
              {[
                {
                  label: "Busiest site",
                  value: busiest
                    ? `${busiest.siteName} (${busiest.totalCount.toLocaleString()})`
                    : "—",
                  color: "#dc2626",
                },
                {
                  label: "Quietest site",
                  value: quietest
                    ? `${quietest.siteName} (${quietest.totalCount.toLocaleString()})`
                    : "—",
                  color: "#16a34a",
                },
                {
                  label: "High-activity",
                  value: `${highCount} of ${pedestrians.length} sites`,
                  color: "#f97316",
                },
              ].map(({ label, value, color }) => (
                <Box
                  key={label}
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    px: 1.25,
                    py: 0.75,
                    borderRadius: 1.5,
                    bgcolor: (t) => t.palette.action.hover,
                  }}
                >
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ flexShrink: 0, mr: 1 }}
                  >
                    {label}
                  </Typography>
                  <Typography
                    variant="body2"
                    fontWeight={700}
                    sx={{ color, textAlign: "right" }}
                    noWrap
                  >
                    {value}
                  </Typography>
                </Box>
              ))}
            </Box>

            <Divider sx={{ mb: 1.5 }} />

            {/* Site list */}
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontWeight: 600, display: "block", mb: 1 }}
            >
              All sites
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
              {sorted.map((site, idx) => {
                const intensity = maxCount > 0 ? site.totalCount / maxCount : 0;
                const color = getPedColor(site.totalCount);
                return (
                  <Box
                    key={site.siteId}
                    sx={{
                      px: 1.25,
                      py: 0.75,
                      borderRadius: 1.5,
                      bgcolor: (t) => t.palette.action.hover,
                    }}
                  >
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                        mb: 0.3,
                      }}
                    >
                      <Typography
                        sx={{
                          fontSize: "0.65rem",
                          fontWeight: 700,
                          color: "text.disabled",
                          width: 16,
                          textAlign: "right",
                          flexShrink: 0,
                        }}
                      >
                        {idx + 1}
                      </Typography>
                      <Typography
                        noWrap
                        variant="body2"
                        fontWeight={500}
                        sx={{ flex: 1 }}
                      >
                        {site.siteName}
                      </Typography>
                      <Typography
                        variant="body2"
                        fontWeight={700}
                        sx={{ color, flexShrink: 0 }}
                      >
                        {site.totalCount.toLocaleString()}
                      </Typography>
                    </Box>
                    <Box
                      sx={{
                        ml: "24px",
                        height: 3,
                        borderRadius: 2,
                        bgcolor: "rgba(0,0,0,0.06)",
                        overflow: "hidden",
                      }}
                    >
                      <Box
                        sx={{
                          height: "100%",
                          width: `${Math.max(intensity * 100, 2)}%`,
                          bgcolor: color,
                          borderRadius: 2,
                          transition: "width 0.4s ease",
                        }}
                      />
                    </Box>
                    {site.lastUpdated && (
                      <Typography
                        sx={{
                          ml: "24px",
                          fontSize: "0.6rem",
                          color: "text.disabled",
                          mt: 0.2,
                        }}
                      >
                        <AccessTimeIcon
                          sx={{ fontSize: 8, mr: 0.3, verticalAlign: "middle" }}
                        />
                        {new Date(site.lastUpdated).toLocaleTimeString(
                          "en-IE",
                          { hour: "2-digit", minute: "2-digit" },
                        )}
                      </Typography>
                    )}
                  </Box>
                );
              })}
            </Box>

            {/* Legend */}
            <Box sx={{ display: "flex", gap: 1.5, mt: 2 }}>
              {[
                { color: "#10B981", label: "Low" },
                { color: "#f97316", label: "Medium" },
                { color: "#dc2626", label: "High" },
              ].map(({ color, label }) => (
                <Box
                  key={label}
                  sx={{ display: "flex", alignItems: "center", gap: 0.4 }}
                >
                  <Box
                    sx={{
                      width: 8,
                      height: 8,
                      borderRadius: "50%",
                      bgcolor: color,
                    }}
                  />
                  <Typography
                    sx={{ fontSize: "0.62rem", color: "text.secondary" }}
                  >
                    {label}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Box>
        </Paper>
      )}
    </Box>
  );
}

// ── Main component ─────────────────────────────────────────────────────────────

export const CarDashboard = () => {
  const [activeMode, setActiveMode] = useState<ActiveMode>(() => {
    const saved = localStorage.getItem("carDashboardActiveMode");
    if (saved === "traffic" || saved === "ev" || saved === "pedestrian")
      return saved;
    return "traffic";
  });
  const [panelOpen, setPanelOpen] = useState(true);
  const [dayTypeFilter, setDayTypeFilter] = useState<DayTypeFilter>("weekday");
  const [timeSlotFilter, setTimeSlotFilter] =
    useState<TimeSlotFilter>("morning_peak");
  const [mapMode, setMapMode] = useState<MapMode>("traffic");
  const [activeColors, setActiveColors] = useState<Set<ColorBand>>(
    () => new Set(["low", "medium", "high"]),
  );

  const theme = useAppSelector((state) => state.ui.theme);

  const { data: stats, isLoading: statsLoading } = useCarFuelTypeStatistics();
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

  const handleModeChange = (
    _: React.SyntheticEvent,
    mode: ActiveMode | null,
  ) => {
    if (!mode) return;
    setActiveMode(mode);
    localStorage.setItem("carDashboardActiveMode", mode);
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

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* ── 3 Tab bar (top-left, floats over map) ─────────────────────── */}
      <Box
        sx={{
          position: "absolute",
          top: GAP,
          left: GAP,
          zIndex: 1100,
          bgcolor: (t) => t.palette.background.paper,
          borderRadius: 2,
          boxShadow: 1,
          backdropFilter: "blur(12px)",
          overflow: "hidden",
        }}
      >
        <Tabs
          value={activeMode}
          onChange={handleModeChange}
          sx={{
            minHeight: 36,
            "& .MuiTab-root": {
              minHeight: 36,
              py: 0,
              px: 1.5,
              fontSize: "0.78rem",
              textTransform: "none",
              fontWeight: 500,
            },
            "& .MuiTabs-indicator": { height: 2 },
          }}
        >
          <Tab
            value="traffic"
            label="Car"
            icon={<DirectionsCarIcon sx={{ fontSize: 15 }} />}
            iconPosition="start"
          />
          <Tab
            value="ev"
            label="EV"
            icon={<EvStationIcon sx={{ fontSize: 15 }} />}
            iconPosition="start"
          />
          <Tab
            value="pedestrian"
            label="Pedestrian"
            icon={<DirectionsWalkIcon sx={{ fontSize: 15 }} />}
            iconPosition="start"
          />
        </Tabs>
      </Box>

      {/* ── Traffic/Pollution map ──────────────────────────────────────── */}
      {activeMode === "traffic" && (
        <>
          {isLoading ? (
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
          ) : (
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
                        Total Emission:{" "}
                        {(point.totalEmissionG / 1000).toFixed(2)} kg CO₂
                        <br />
                        Car Volume: {point.carVolume.toFixed(0)}
                        <br />
                        Day Type: {point.dayType}
                        <br />
                        Time Slot: {point.timeSlot.replaceAll("_", " ")}
                      </Popup>
                    </Circle>
                  ))}
            </MapContainer>
          )}

          {/* Hamburger */}
          {!panelOpen && (
            <Box
              sx={{ position: "absolute", top: GAP, right: GAP, zIndex: 1000 }}
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

          {/* Floating side panel */}
          {panelOpen && (
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

              <Box sx={{ flex: 1, overflow: "auto", px: 2, py: 1.5 }}>
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
                          timeSlotFilter === value
                            ? "primary.main"
                            : "transparent",
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

                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: "block", mb: 1, fontWeight: 600 }}
                >
                  Vehicles by fuel type
                </Typography>
                <Box
                  sx={{ display: "flex", flexDirection: "column", gap: 0.75 }}
                >
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
              </Box>
            </Paper>
          )}
        </>
      )}

      {/* ── EV tab ────────────────────────────────────────────────────── */}
      <Box
        sx={{
          position: "absolute",
          inset: 0,
          visibility: activeMode === "ev" ? "visible" : "hidden",
          opacity: activeMode === "ev" ? 1 : 0,
          pointerEvents: activeMode === "ev" ? "auto" : "none",
          transition: "opacity 0.15s ease-in-out",
        }}
      >
        <EVDashboard />
      </Box>

      {/* ── Pedestrian tab ────────────────────────────────────────────── */}
      <Box
        sx={{
          position: "absolute",
          inset: 0,
          visibility: activeMode === "pedestrian" ? "visible" : "hidden",
          opacity: activeMode === "pedestrian" ? 1 : 0,
          pointerEvents: activeMode === "pedestrian" ? "auto" : "none",
          transition: "opacity 0.15s ease-in-out",
        }}
      >
        <PedestrianTab theme={theme} />
      </Box>
    </Box>
  );
};
