/**
 * Train data dashboard — full-viewport map with floating control panel
 * Shows Dublin map with train stations in a collapsible side panel
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
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import { useTrainData } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// Create a custom icon for train stations
const trainIcon = new L.Icon({
    iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
    iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
    shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
});

export const TrainDashboard = () => {
    const [panelOpen, setPanelOpen] = useState(true);
    const theme = useAppSelector((state) => state.ui.theme);

    const {
        data: trainData,
        isLoading: dataLoading,
        error,
    } = useTrainData(200);

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
                        <Marker key={station.id} position={[station.lat, station.lon]} icon={trainIcon}>
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
                        <Typography variant="h5">Train Stations</Typography>
                        <IconButton size="small" onClick={() => setPanelOpen(false)}>
                            <CloseIcon fontSize="small" />
                        </IconButton>
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
                            {/* Trip count header */}
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
                                                    <Typography component="span" variant="body2" color="text.secondary">
                                                        Code: {station.stationCode}
                                                    </Typography>
                                                    {station.stationType && (
                                                        <Typography component="span" variant="body2" color="text.secondary" sx={{ ml: 1 }}>
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
