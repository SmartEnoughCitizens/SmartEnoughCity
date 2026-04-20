/**
 * Bus data dashboard — full-viewport map with unified right-side panel,
 * matching the Train and Cycle dashboard layout pattern.
 */

import { useState, useEffect } from "react";
import {
  Box,
  Paper,
  Typography,
  LinearProgress,
  Autocomplete,
  TextField,
  Alert,
  IconButton,
  Tabs,
  Tab,
} from "@mui/material";

import CommuteIcon from "@mui/icons-material/Commute";
import WarningIcon from "@mui/icons-material/Warning";
import EqualizerIcon from "@mui/icons-material/Equalizer";
import EcoIcon from "@mui/icons-material/EnergySavingsLeaf";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Popup,
  useMap,
} from "react-leaflet";

function MapController({
  target,
}: {
  target: { center: [number, number]; id: number } | null;
}) {
  const map = useMap();
  useEffect(() => {
    if (target)
      map.flyTo(target.center, 15, { duration: 1.2, easeLinearity: 0.25 });
  }, [map, target]);
  return null;
}
import {
  useBusKpis,
  useBusLiveVehicles,
  useBusRouteDetail,
  useBusRouteUtilization,
  useBusSystemPerformance,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import {
  BusRecommendationCandidate,
  BusRecommendationFitBounds,
  BusRecommendationPolylineAndStops,
} from "@/components/bus/BusRecommendationMapLayers";
import { DelayLeaderboard } from "@/components/bus/DelayLeaderboard";
import {
  NewStopRecommendationsList,
  RecommendationScoreCard,
  SelectedRecommendationChip,
} from "@/components/bus/NewStopRecommendationsList";
import type { BusNewStopRecommendation } from "@/types";
import "leaflet/dist/leaflet.css";

const PANEL_W = 420;
const GAP = 16;

const KpiCard = ({
  label,
  value,
  icon,
  subtitle,
}: {
  label: string;
  value: string;
  icon: React.ReactNode;
  subtitle?: string;
}) => (
  <Paper
    elevation={0}
    sx={{
      p: 2,
      borderRadius: 2,
      display: "flex",
      flexDirection: "column",
      gap: 0.5,
    }}
  >
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
      }}
    >
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      {icon}
    </Box>
    <Typography variant="h5" fontWeight="bold">
      {value}
    </Typography>
    {subtitle && (
      <Typography variant="caption" color="text.secondary">
        {subtitle}
      </Typography>
    )}
  </Paper>
);

const getUtilizationColor = (pct: number) => {
  if (pct >= 80) return "success";
  if (pct >= 50) return "warning";
  return "info";
};

const getGrade = (score: number) => {
  if (score >= 90) return "Grade A";
  if (score >= 80) return "Grade B";
  if (score >= 70) return "Grade C";
  return "Grade D";
};

const PerformanceGauge = ({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: string;
}) => {
  return (
    <Box
      sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}
    >
      <Box sx={{ position: "relative", width: 80, height: 80, mb: 0.5 }}>
        <svg width="80" height="80" style={{ transform: "rotate(-90deg)" }}>
          <circle
            cx="40"
            cy="40"
            r="33"
            fill="transparent"
            stroke="currentColor"
            strokeWidth="7"
            opacity={0.15}
          />
          <circle
            cx="40"
            cy="40"
            r="33"
            fill="transparent"
            stroke={color}
            strokeWidth="7"
            strokeDasharray={2 * Math.PI * 33}
            strokeDashoffset={
              2 * Math.PI * 33 - (value / 100) * 2 * Math.PI * 33
            }
            strokeLinecap="round"
          />
        </svg>
        <Typography
          sx={{
            position: "absolute",
            inset: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontWeight: "bold",
            fontSize: 14,
          }}
        >
          {value.toFixed(0)}%
        </Typography>
      </Box>
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{
          textTransform: "uppercase",
          fontWeight: 600,
          fontSize: 9,
          letterSpacing: 1,
        }}
      >
        {label}
      </Typography>
    </Box>
  );
};

export const BusDashboard = () => {
  const [panelOpen, setPanelOpen] = useState(true);
  const [tabValue, setTabValue] = useState(0);
  const [selectedRoute, setSelectedRoute] = useState<string>("");

  const flyTarget: { center: [number, number]; id: number } | null = null;
  const [selectedRecommendation, setSelectedRecommendation] =
    useState<BusNewStopRecommendation | null>(null);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: kpis } = useBusKpis();
  const { data: liveVehicles } = useBusLiveVehicles();
  const { data: routeUtilization } = useBusRouteUtilization();
  const { data: systemPerformance } = useBusSystemPerformance();

  const { data: routeDetailForMap, isError: routeDetailError } =
    useBusRouteDetail(selectedRecommendation?.routeId ?? null);

  // Build unique route list sorted by short name for the dropdown
  const routes = routeUtilization
    ? routeUtilization.toSorted((a, b) =>
        a.routeShortName.localeCompare(b.routeShortName, undefined, {
          numeric: true,
        }),
      )
    : [];

  // Filter live vehicles by selected route
  const filteredVehicles = liveVehicles?.filter(
    (v) => !selectedRoute || v.routeShortName === selectedRoute,
  );

  // Dublin center
  const defaultCenter: [number, number] = [53.3498, -6.2603];

  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  // Sort route utilization ascending (worst-performing routes first)
  const sortedUtilization = routeUtilization
    ? routeUtilization
        .filter((r) => r.utilizationPct > 0)
        .toSorted((a, b) => a.utilizationPct - b.utilizationPct)
    : [];

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map background */}
      <Box sx={{ height: "100%", width: "100%" }}>
        <MapContainer
          center={defaultCenter}
          zoom={13}
          style={{ height: "100%", width: "100%" }}
          zoomControl={false}
        >
          <TileLayer attribution={tileAttribution} url={tileUrl} />
          <MapController target={flyTarget} />
          {selectedRecommendation && (
            <>
              <BusRecommendationFitBounds
                recommendation={selectedRecommendation}
                routeDetail={routeDetailForMap}
              />
              <BusRecommendationPolylineAndStops
                routeDetail={routeDetailForMap}
              />
            </>
          )}
          {filteredVehicles?.map((vehicle) => (
            <CircleMarker
              key={vehicle.vehicleId}
              center={[vehicle.latitude, vehicle.longitude]}
              radius={6}
              pathOptions={{
                color: "#fff",
                weight: 2,
                fillColor: "#1e3a8a",
                fillOpacity: 0.9,
              }}
            >
              <Popup>
                <strong>
                  Route {vehicle.routeShortName} — Bus #{vehicle.vehicleId}
                </strong>
                <br />
                Status: {vehicle.status}
                <br />
                Occupancy: {vehicle.occupancyPct.toFixed(0)}%
                {vehicle.delaySeconds > 0 && (
                  <>
                    <br />
                    Delay: {Math.round(vehicle.delaySeconds / 60)}m
                  </>
                )}
              </Popup>
            </CircleMarker>
          ))}
          {selectedRecommendation && (
            <BusRecommendationCandidate
              recommendation={selectedRecommendation}
            />
          )}
        </MapContainer>
      </Box>

      {/* ── Toggle button when panel closed ── */}
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

      {/* ── Single unified right panel ── */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            bottom: GAP,
            width: PANEL_W,
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
              <DirectionsBusIcon color="primary" />
              <Typography variant="h5">Dublin Bus Network</Typography>
            </Box>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* Persistent route filter — above tabs, filters map */}
          <Box sx={{ px: 2, pb: 1 }}>
            <Autocomplete
              size="small"
              options={routes}
              getOptionLabel={(r) => `${r.routeShortName} — ${r.routeLongName}`}
              filterOptions={(options, { inputValue }) =>
                options.filter((r) =>
                  r.routeShortName
                    .toLowerCase()
                    .includes(inputValue.toLowerCase()),
                )
              }
              value={
                routes.find((r) => r.routeShortName === selectedRoute) ?? null
              }
              onChange={(_, value) =>
                setSelectedRoute(value?.routeShortName ?? "")
              }
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Filter Route"
                  placeholder="e.g. H2"
                />
              )}
            />
          </Box>

          {/* Tab bar */}
          <Tabs
            value={tabValue}
            onChange={(_, v) => setTabValue(v)}
            variant="scrollable"
            scrollButtons="auto"
            sx={{
              minHeight: 36,
              "& .MuiTab-root": {
                minHeight: 36,
                fontSize: "0.7rem",
                textTransform: "none",
                minWidth: 0,
                px: 1.5,
              },
              "& .MuiTabScrollButton-root": { width: 24 },
            }}
          >
            <Tab label="Overview" />
            <Tab label="Utilization" />
            <Tab label="Delays" />
            <Tab label="Recommendations" />
          </Tabs>

          {/* Tab content */}
          <Box sx={{ flex: 1, overflow: "auto", px: 2, pt: 1 }}>
            {/* Tab 0: Overview — KPI cards + Service Reliability */}
            {tabValue === 0 && kpis && (
              <>
                <Box
                  sx={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: 1,
                    mb: 2,
                  }}
                >
                  <KpiCard
                    label="Total Buses Running"
                    value={kpis.totalBusesRunning.toLocaleString()}
                    icon={<CommuteIcon color="primary" fontSize="small" />}
                  />
                  <KpiCard
                    label="Active Delays"
                    value={kpis.activeDelays.toLocaleString()}
                    icon={<WarningIcon color="error" fontSize="small" />}
                  />
                  <KpiCard
                    label="Fleet Utilization"
                    value={`${kpis.fleetUtilizationPct.toFixed(1)}%`}
                    icon={<EqualizerIcon color="warning" fontSize="small" />}
                  />
                  <KpiCard
                    label="Sustainability"
                    value={kpis.sustainabilityScore.toFixed(0)}
                    icon={<EcoIcon color="success" fontSize="small" />}
                    subtitle={getGrade(kpis.sustainabilityScore)}
                  />
                </Box>
                {systemPerformance && (
                  <>
                    <Typography
                      variant="subtitle2"
                      fontWeight="bold"
                      sx={{ mb: 1.5 }}
                    >
                      Service Reliability
                    </Typography>
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "center",
                        gap: 4,
                        pb: 1,
                      }}
                    >
                      <PerformanceGauge
                        label="Reliability"
                        value={systemPerformance.reliabilityPct}
                        color="#2563eb"
                      />
                      <PerformanceGauge
                        label="Late Arrival"
                        value={systemPerformance.lateArrivalPct}
                        color="#ef4444"
                      />
                    </Box>
                  </>
                )}
              </>
            )}

            {/* Tab 1: Utilization — Route list with LinearProgress */}
            {tabValue === 1 && (
              <>
                <Typography
                  variant="subtitle2"
                  fontWeight="bold"
                  sx={{ mb: 1.5 }}
                >
                  Route Utilization
                </Typography>
                <Box
                  sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}
                >
                  {sortedUtilization.map((route) => (
                    <Box key={route.routeId}>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          mb: 0.25,
                        }}
                      >
                        <Typography
                          variant="caption"
                          fontWeight={500}
                          noWrap
                          sx={{ maxWidth: 280 }}
                        >
                          Route {route.routeShortName} ({route.routeLongName})
                        </Typography>
                        <Typography
                          variant="caption"
                          fontWeight="bold"
                          color={`${getUtilizationColor(route.utilizationPct)}.main`}
                        >
                          {route.utilizationPct.toFixed(1)}%
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(route.utilizationPct, 100)}
                        color={getUtilizationColor(route.utilizationPct)}
                        sx={{ height: 6, borderRadius: 3 }}
                      />
                    </Box>
                  ))}
                </Box>
              </>
            )}

            {/* Tab 2: Delays */}
            {tabValue === 2 && (
              <>
                <Typography
                  variant="subtitle2"
                  fontWeight="bold"
                  sx={{ mb: 1 }}
                >
                  Common Bus Delays
                </Typography>
                <DelayLeaderboard />
              </>
            )}

            {/* Tab 4: Recommendations */}
            {tabValue === 3 && (
              <>
                <Typography
                  variant="subtitle2"
                  fontWeight="bold"
                  sx={{ mb: 1 }}
                >
                  New Stop Recommendations
                </Typography>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  display="block"
                  sx={{ mb: 1 }}
                >
                  Click a row to show route shape, stops, and suggested location
                  on the map.
                </Typography>
                {selectedRecommendation && (
                  <Box sx={{ mb: 1 }}>
                    <SelectedRecommendationChip
                      recommendation={selectedRecommendation}
                      onClear={() => setSelectedRecommendation(null)}
                    />
                    <RecommendationScoreCard
                      recommendation={selectedRecommendation}
                    />
                  </Box>
                )}
                {selectedRecommendation && routeDetailError && (
                  <Alert
                    severity="warning"
                    sx={{ mb: 1, py: 0, fontSize: "0.75rem" }}
                  >
                    Could not load route geometry; only the suggested stop is
                    shown on the map.
                  </Alert>
                )}
                <NewStopRecommendationsList
                  selectedRecommendation={selectedRecommendation}
                  onSelectRecommendation={setSelectedRecommendation}
                />
              </>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};
