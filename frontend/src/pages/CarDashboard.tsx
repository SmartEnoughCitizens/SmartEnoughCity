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
import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";
import { useCarFuelTypeStatistics, useCarHighTrafficPoints } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";

type DayTypeFilter = "weekday" | "weekend";
type TimeSlotFilter =
  | "morning_peak"
  | "inter_peak"
  | "evening_peak"
  | "off_peak";

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
    if (ratio > 0.66) return "#dc2626"; // high   — red
    if (ratio > 0.33) return "#f97316"; // medium — orange
    return "#16a34a"; // low    — green
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

  if (statsLoading || trafficLoading) {
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

      {/* High Traffic Points Map */}
      <Box
        sx={{ flex: 1, display: "flex", flexDirection: "column", minHeight: 0 }}
      >
        <Typography variant="h6" fontWeight="bold" sx={{ mb: 2.5 }}>
          High Traffic Points — Dublin
        </Typography>

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
            {filteredPoints?.map((point, idx) => (
              <CircleMarker
                key={`${point.siteId}-${idx}`}
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
          </MapContainer>
        </Paper>
      </Box>
    </Box>
  );
};
