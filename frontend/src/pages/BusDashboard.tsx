/**
 * Bus data dashboard — full-viewport map with floating control panel
 * Shows Dublin map with bus trip data in a collapsible side panel
 */

import { useState } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  IconButton,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import { MapContainer, TileLayer } from "react-leaflet";
import { useBusData, useBusRoutes } from "@/hooks";
import { DelayChart } from "@/components/charts/DelayChart";
import { BusTripTable } from "@/components/tables/BusTripTable";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";

export const BusDashboard = () => {
  const [selectedRoute, setSelectedRoute] = useState<string>("");
  const [panelOpen, setPanelOpen] = useState(true);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: routes, isLoading: routesLoading } = useBusRoutes();
  const {
    data: busData,
    isLoading: dataLoading,
    error,
  } = useBusData(selectedRoute || undefined, 100);

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

  if (routesLoading) {
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

  const panelWidth = 400;

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
          Failed to load bus data
        </Alert>
      )}

      {/* Toggle button when panel is closed */}
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
            "&:hover": {
              bgcolor: (t) => t.palette.background.paper,
            },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* Floating side panel */}
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
          {/* Panel header */}
          <Box
            sx={{
              p: 2,
              pb: 1.5,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <Typography variant="h5">Bus Trips</Typography>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* Route selector */}
          <Box sx={{ px: 2, pb: 1.5 }}>
            <FormControl fullWidth size="small">
              <InputLabel>Route</InputLabel>
              <Select
                value={selectedRoute}
                label="Route"
                onChange={(e) => setSelectedRoute(e.target.value)}
              >
                <MenuItem value="">
                  <em>All Routes</em>
                </MenuItem>
                {routes?.map((route) => (
                  <MenuItem key={route} value={route}>
                    Route {route}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>

          {/* Content */}
          {dataLoading ? (
            <Box
              sx={{
                display: "flex",
                justifyContent: "center",
                py: 4,
              }}
            >
              <CircularProgress size={28} />
            </Box>
          ) : (
            <Box
              sx={{
                flex: 1,
                overflow: "auto",
                display: "flex",
                flexDirection: "column",
              }}
            >
              {/* Delay chart */}
              {busData?.statistics && selectedRoute && (
                <Box sx={{ px: 2, pb: 1 }}>
                  <DelayChart statistics={busData.statistics} compact />
                </Box>
              )}

              {/* Trip count header */}
              <Box sx={{ px: 2, pb: 0.5 }}>
                <Typography variant="caption" color="text.secondary">
                  {selectedRoute
                    ? `Route ${selectedRoute} — ${busData?.totalRecords || 0} trips`
                    : `All Routes — ${busData?.totalRecords || 0} trips`}
                </Typography>
              </Box>

              {/* Table */}
              <Box sx={{ flex: 1, overflow: "auto", px: 1 }}>
                <BusTripTable
                  trips={busData?.data || []}
                  maxRows={50}
                  compact
                />
              </Box>
            </Box>
          )}
        </Paper>
      )}
    </Box>
  );
};
