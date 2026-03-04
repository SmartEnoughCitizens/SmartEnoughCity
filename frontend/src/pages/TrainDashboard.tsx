/**
 * Train data dashboard — full-viewport map with floating control panel
 * Shows Dublin map with train stations + live trains, and KPI cards
 */

import { useState } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Divider,
  Chip,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import TrainIcon from "@mui/icons-material/Train";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import SpeedIcon from "@mui/icons-material/Speed";
import { MapContainer, TileLayer, Marker, Popup, Circle } from "react-leaflet";
import { useTrainData, useTrainKpis, useTrainLiveTrains } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// true if the train is actively running
const isRunning = (status?: string) =>
  status?.toUpperCase().includes("RUNNING") ?? false;

const stationIcon = new L.Icon({
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
});

interface KpiCardProps {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  sub?: string;
  color?: string;
}

function KpiCard({
  icon,
  label,
  value,
  sub,
  color = "primary.main",
}: KpiCardProps) {
  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 1.5,
        p: 1.5,
        borderRadius: 2,
        bgcolor: (t) =>
          t.palette.mode === "dark"
            ? "rgba(255,255,255,0.05)"
            : "rgba(0,0,0,0.04)",
      }}
    >
      <Box sx={{ color, fontSize: 28, display: "flex" }}>{icon}</Box>
      <Box>
        <Typography variant="h6" fontWeight={700} lineHeight={1}>
          {value}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {label}
          {sub && (
            <Box component="span" sx={{ ml: 0.5, opacity: 0.7 }}>
              {sub}
            </Box>
          )}
        </Typography>
      </Box>
    </Box>
  );
}

export const TrainDashboard = () => {
  const [panelOpen, setPanelOpen] = useState(true);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: trainData, isLoading: dataLoading, error } = useTrainData(200);
  const { data: kpiData } = useTrainKpis();
  const { data: liveTrains = [] } = useTrainLiveTrains();

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

  const panelWidth = 400;

  const liveColor = (status?: string) =>
    isRunning(status) ? "#4caf50" : "#ff9800";

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map background */}
      <Box sx={{ height: "100%", width: "100%" }}>
        <MapContainer
          center={defaultCenter}
          zoom={12}
          style={{ height: "100%", width: "100%" }}
          zoomControl={false}
        >
          <TileLayer attribution={tileAttribution} url={tileUrl} />

          {/* Plot train stations on the map */}
          {trainData?.data?.map((station) => (
            <Marker
              key={station.id}
              position={[station.lat, station.lon]}
              icon={stationIcon}
            >
              <Popup>
                <Typography variant="subtitle2" fontWeight={700}>
                  {station.stationDesc}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Code: {station.stationCode}
                </Typography>
                {station.stationType && (
                  <Typography variant="body2" color="text.secondary">
                    Type: {station.stationType}
                  </Typography>
                )}
              </Popup>
            </Marker>
          ))}

          {/* Plot live trains as coloured dots */}
          {liveTrains
            .filter((t) => t.lat && t.lon)
            .map((t) => (
              <Circle
                key={t.trainCode}
                center={[t.lat, t.lon]}
                radius={300}
                pathOptions={{
                  color: liveColor(t.status),
                  fillColor: liveColor(t.status),
                  fillOpacity: 0.85,
                }}
              >
                <Popup>
                  <Typography variant="subtitle2" fontWeight={700}>
                    🚂 {t.trainCode}
                  </Typography>
                  {t.direction && (
                    <Typography variant="body2" color="text.secondary">
                      Direction: {t.direction}
                    </Typography>
                  )}
                  {t.trainType && (
                    <Typography variant="body2" color="text.secondary">
                      Type: {t.trainType}
                    </Typography>
                  )}
                  {t.publicMessage && (
                    <Typography variant="body2" sx={{ mt: 0.5 }}>
                      {t.publicMessage}
                    </Typography>
                  )}
                </Popup>
              </Circle>
            ))}
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
          Failed to load train data
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
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
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
            <Typography variant="h5">🚂 Train Network</Typography>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* KPI cards */}
          {kpiData && (
            <Box
              sx={{
                px: 2,
                pb: 1.5,
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 1,
              }}
            >
              <KpiCard
                icon={<LocationOnIcon fontSize="inherit" />}
                label="Stations"
                value={kpiData.totalStations}
                color="info.main"
              />
              <KpiCard
                icon={<TrainIcon fontSize="inherit" />}
                label="Live Trains"
                value={kpiData.liveTrainsRunning}
                color="success.main"
              />
              <KpiCard
                icon={<AccessTimeIcon fontSize="inherit" />}
                label="On Time"
                value={`${kpiData.onTimePct.toFixed(1)}%`}
                color={
                  kpiData.onTimePct >= 80 ? "success.main" : "warning.main"
                }
              />
              <KpiCard
                icon={<SpeedIcon fontSize="inherit" />}
                label="Avg Delay"
                value={`${kpiData.avgDelayMinutes.toFixed(1)} min`}
                color={
                  kpiData.avgDelayMinutes <= 2 ? "success.main" : "warning.main"
                }
              />
            </Box>
          )}

          <Divider />

          {/* Live train legend */}
          {liveTrains.length > 0 && (
            <Box
              sx={{ px: 2, py: 1, display: "flex", gap: 1, flexWrap: "wrap" }}
            >
              <Chip
                size="small"
                label={`🟢 Running (${liveTrains.filter((t) => isRunning(t.status)).length})`}
                sx={{ fontSize: "0.7rem" }}
              />
              <Chip
                size="small"
                label={`🟠 Not yet running (${liveTrains.filter((t) => !isRunning(t.status)).length})`}
                sx={{ fontSize: "0.7rem" }}
              />
            </Box>
          )}

          {/* Content */}
          {dataLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
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
              {/* Station count */}
              <Box sx={{ px: 2, pb: 0.5 }}>
                <Typography variant="caption" color="text.secondary">
                  Showing {trainData?.totalRecords || 0} stations
                </Typography>
              </Box>

              {/* Station List */}
              <List sx={{ flex: 1, overflow: "auto", px: 1 }}>
                {trainData?.data?.map((station) => (
                  <ListItem key={station.id} divider>
                    <ListItemText
                      primary={station.stationDesc}
                      secondary={
                        <>
                          <Typography
                            component="span"
                            variant="body2"
                            color="text.secondary"
                          >
                            Code: {station.stationCode}
                          </Typography>
                          {station.stationType && (
                            <Typography
                              component="span"
                              variant="body2"
                              color="text.secondary"
                              sx={{ ml: 1 }}
                            >
                              • Type: {station.stationType}
                            </Typography>
                          )}
                        </>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            </Box>
          )}
        </Paper>
      )}
    </Box>
  );
};
