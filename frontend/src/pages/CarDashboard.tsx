/**
 * Car dashboard — displays fuel type statistics as tiles and high traffic points on a map
 */

import { useState } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  ToggleButton,
  ToggleButtonGroup,
} from "@mui/material";
import DirectionsCarIcon from "@mui/icons-material/DirectionsCar";
import { MapContainer, TileLayer, CircleMarker, Circle, Popup } from "react-leaflet";
import {
  useCarFuelTypeStatistics,
  useCarHighTrafficPoints,
  useCarJunctionEmissions,
} from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";

type DayTypeFilter = "weekday" | "weekend";
type TimeSlotFilter =
  | "morning_peak"
  | "inter_peak"
  | "evening_peak"
  | "off_peak";
type MapMode = "traffic" | "pollution";

const FuelTypeTile = ({
  fuelType,
  count,
}: {
  fuelType: string;
  count: number;
}) => (
  <Paper
    elevation={0}
    sx={{
      p: 2.5,
      borderRadius: 2,
      display: "flex",
      flexDirection: "column",
      gap: 0.5,
      minWidth: 160,
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
        {fuelType}
      </Typography>
      <DirectionsCarIcon fontSize="small" color="primary" />
    </Box>
    <Typography variant="h5" fontWeight="bold">
      {count.toLocaleString()}
    </Typography>
    <Typography variant="caption" color="text.secondary">
      vehicles registered
    </Typography>
  </Paper>
);

export const CarDashboard = () => {
  const { data: stats, isLoading: statsLoading } = useCarFuelTypeStatistics();
  const { data: trafficPoints, isLoading: trafficLoading } =
    useCarHighTrafficPoints();
  const theme = useAppSelector((state) => state.ui.theme);

  const [dayTypeFilter, setDayTypeFilter] = useState<DayTypeFilter>("weekday");
  const [timeSlotFilter, setTimeSlotFilter] =
    useState<TimeSlotFilter>("morning_peak");
  const [mapMode, setMapMode] = useState<MapMode>("traffic");

  const { data: emissionPoints, isLoading: emissionsLoading } =
    useCarJunctionEmissions(mapMode === "pollution");

  // --- Traffic mode ---
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

  const getMarkerColor = (volume: number): string => {
    const ratio = volume / maxVolume;
    if (ratio > 0.66) return "#dc2626";
    if (ratio > 0.33) return "#f97316";
    return "#16a34a";
  };

  // --- Pollution mode ---
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

  const getEmissionColor = (emission: number): string => {
    const ratio =
      maxEmission > minEmission
        ? (emission - minEmission) / (maxEmission - minEmission)
        : 0;
    if (ratio > 0.66) return "#dc2626";
    if (ratio > 0.33) return "#f97316";
    return "#16a34a";
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

  if (statsLoading || trafficLoading || (mapMode === "pollution" && emissionsLoading)) {
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
        p: 3,
        display: "flex",
        flexDirection: "column",
        gap: 3,
        height: "100%",
      }}
    >
      {/* Fuel Type Tiles */}
      <Box sx={{ flexShrink: 0 }}>
        <Typography variant="h6" fontWeight="bold" sx={{ mb: 2.5 }}>
          Vehicle Statistics by Fuel Type
        </Typography>
        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2 }}>
          {stats?.map((stat) => (
            <FuelTypeTile
              key={stat.fuelType}
              fuelType={stat.fuelType}
              count={stat.count}
            />
          ))}
        </Box>
      </Box>

      {/* Map Section */}
      <Box
        sx={{ flex: 1, display: "flex", flexDirection: "column", minHeight: 0 }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            mb: 2.5,
            flexWrap: "wrap",
            gap: 1,
          }}
        >
          <Typography variant="h6" fontWeight="bold">
            {mapMode === "traffic"
              ? "High Traffic Points — Dublin"
              : "Pollution Estimation — Dublin"}
          </Typography>
          <ToggleButtonGroup
            value={mapMode}
            exclusive
            onChange={(_, value) => value && setMapMode(value)}
            size="small"
          >
            <ToggleButton value="traffic">Traffic</ToggleButton>
            <ToggleButton value="pollution">Pollution</ToggleButton>
          </ToggleButtonGroup>
        </Box>

        {/* Filters */}
        <Box sx={{ display: "flex", gap: 2, mb: 2, flexWrap: "wrap" }}>
          <ToggleButtonGroup
            value={dayTypeFilter}
            exclusive
            onChange={(_, value) => value && setDayTypeFilter(value)}
            size="small"
          >
            <ToggleButton value="weekday">Weekday</ToggleButton>
            <ToggleButton value="weekend">Weekend</ToggleButton>
          </ToggleButtonGroup>

          <ToggleButtonGroup
            value={timeSlotFilter}
            exclusive
            onChange={(_, value) => value && setTimeSlotFilter(value)}
            size="small"
          >
            <ToggleButton value="morning_peak">Morning Peak</ToggleButton>
            <ToggleButton value="inter_peak">Inter Peak</ToggleButton>
            <ToggleButton value="evening_peak">Evening Peak</ToggleButton>
            <ToggleButton value="off_peak">Off Peak</ToggleButton>
          </ToggleButtonGroup>
        </Box>

        <Paper
          elevation={0}
          sx={{
            borderRadius: 2,
            overflow: "hidden",
            flex: 1,
            display: "flex",
            flexDirection: "column",
          }}
        >
          <MapContainer
            center={dublinCenter}
            zoom={12}
            style={{ flex: 1, width: "100%", minHeight: 0 }}
            zoomControl={true}
          >
            <TileLayer attribution={tileAttribution} url={tileUrl} />

            {mapMode === "traffic" &&
              filteredPoints?.map((point, idx) => (
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
              filteredEmissions?.map((point, idx) => (
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
                    Total Emission: {(point.totalEmissionG / 1000).toFixed(2)} kg CO₂
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
        </Paper>
      </Box>
    </Box>
  );
};
