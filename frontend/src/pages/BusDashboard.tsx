/**
 * Bus data dashboard — full-viewport map with floating KPI panel,
 * route utilization, system performance, and live vehicle markers
 */

import { useState } from "react";
import {
  Box,
  Paper,
  Typography,
  LinearProgress,
  Autocomplete,
  TextField,
} from "@mui/material";
import CommuteIcon from "@mui/icons-material/Commute";
import WarningIcon from "@mui/icons-material/Warning";
import EqualizerIcon from "@mui/icons-material/Equalizer";
import EcoIcon from "@mui/icons-material/EnergySavingsLeaf";
import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";
import {
  useBusKpis,
  useBusLiveVehicles,
  useBusRouteUtilization,
  useBusSystemPerformance,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";

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
    <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
      <Box sx={{ position: "relative", width: 80, height: 80, mb: 0.5 }}>
        <svg
          width="80"
          height="80"
          style={{ transform: "rotate(-90deg)" }}
        >
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
        sx={{ textTransform: "uppercase", fontWeight: 600, fontSize: 9, letterSpacing: 1 }}
      >
        {label}
      </Typography>
    </Box>
  );
};

export const BusDashboard = () => {
  const [selectedRoute, setSelectedRoute] = useState<string>("");
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: kpis } = useBusKpis();
  const { data: liveVehicles } = useBusLiveVehicles();
  const { data: routeUtilization } = useBusRouteUtilization();
  const { data: systemPerformance } = useBusSystemPerformance();

  // Build unique route list sorted by short name for the dropdown
  const routes = routeUtilization
    ? routeUtilization.toSorted((a, b) =>
        a.routeShortName.localeCompare(b.routeShortName, undefined, { numeric: true })
      )
    : [];

  // Filter live vehicles by selected route
  const filteredVehicles = liveVehicles?.filter(
    (v) => !selectedRoute || v.routeShortName === selectedRoute
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
        </MapContainer>
      </Box>



      {/* KPI cards — top left */}
      {kpis && (
        <Box
          sx={{
            position: "absolute",
            top: 16,
            left: 16,
            zIndex: 1000,
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 1,
            width: 340,
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
      )}

      {/* Route Utilization + System Performance — bottom left */}
      <Box
        sx={{
          position: "absolute",
          bottom: 16,
          left: 16,
          zIndex: 1000,
          display: "flex",
          gap: 1.5,
          maxWidth: "calc(100% - 450px)",
        }}
      >
        {/* Route Utilization */}
        {sortedUtilization.length > 0 && (
          <Paper
            elevation={0}
            sx={{
              p: 2,
              borderRadius: 2,
              width: 340,
              maxHeight: 260,
              overflow: "auto",
            }}
          >
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1.5 }}>
              Route Utilization
            </Typography>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
              {sortedUtilization.map((route) => (
                <Box key={route.routeId}>
                  <Box
                    sx={{
                      display: "flex",
                      justifyContent: "space-between",
                      mb: 0.25,
                    }}
                  >
                    <Typography variant="caption" fontWeight={500} noWrap sx={{ maxWidth: 220 }}>
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
          </Paper>
        )}

        {/* Service Reliability */}
        {systemPerformance && (
          <Paper
            elevation={0}
            sx={{
              p: 2,
              borderRadius: 2,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
            }}
          >
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1.5 }}>
              Service Reliability
            </Typography>
            <Box sx={{ display: "flex", gap: 2 }}>
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
          </Paper>
        )}
      </Box>

      {/* Compact route filter — top right */}
      <Paper
        elevation={0}
        sx={{
          position: "absolute",
          top: 16,
          right: 16,
          zIndex: 1000,
          borderRadius: 2,
          p: 1.5,
          width: 200,
        }}
      >
        <Autocomplete
          size="small"
          options={routes}
          getOptionLabel={(r) => `${r.routeShortName} — ${r.routeLongName}`}
          filterOptions={(options, { inputValue }) =>
            options.filter((r) =>
              r.routeShortName.toLowerCase().includes(inputValue.toLowerCase())
            )
          }
          value={routes.find((r) => r.routeShortName === selectedRoute) ?? null}
          onChange={(_, value) => setSelectedRoute(value?.routeShortName ?? "")}
          renderInput={(params) => (
            <TextField {...params} label="Filter Route" placeholder="e.g. H2" />
          )}
        />
      </Paper>
    </Box>
  );
};