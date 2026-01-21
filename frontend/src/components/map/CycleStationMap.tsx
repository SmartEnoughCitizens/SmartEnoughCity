/**
 * Cycle station map component using React Leaflet
 */

import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import { Box, Paper, Typography, Chip } from "@mui/material";
import L from "leaflet";
import type { CycleStation } from "@/types";
import "leaflet/dist/leaflet.css";

// Fix for default marker icon issue with Webpack
import icon from "leaflet/dist/images/marker-icon.png";
import iconShadow from "leaflet/dist/images/marker-shadow.png";

const DefaultIcon = L.icon({
  iconUrl: icon,
  shadowUrl: iconShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

L.Marker.prototype.options.icon = DefaultIcon;

interface CycleStationMapProps {
  stations: CycleStation[];
  height?: number | string;
}

// Component to fit bounds when stations change
const FitBounds = ({ stations }: { stations: CycleStation[] }) => {
  const map = useMap();

  useEffect(() => {
    if (stations.length > 0) {
      const bounds = L.latLngBounds(
        stations.map((station) => [station.lat, station.lon]),
      );
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [stations, map]);

  return <></>;
};

export const CycleStationMap = ({
  stations,
  height = 400,
}: CycleStationMapProps) => {
  // Default center (Dublin, Ireland)
  const defaultCenter: [number, number] = [53.3498, -6.2603];

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Cycle Stations Map
      </Typography>
      <Box sx={{ height, width: "100%", borderRadius: 1, overflow: "hidden" }}>
        <MapContainer
          center={defaultCenter}
          zoom={13}
          style={{ height: "100%", width: "100%" }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <FitBounds stations={stations} />
          {stations.map((station) => (
            <Marker key={station.id} position={[station.lat, station.lon]}>
              <Popup>
                <Box sx={{ minWidth: 200 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    {station.name}
                  </Typography>
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    gutterBottom
                  >
                    {station.address}
                  </Typography>
                  <Box
                    sx={{ mt: 1, display: "flex", gap: 1, flexWrap: "wrap" }}
                  >
                    <Chip
                      label={`Bikes: ${station.numBikesAvailable}`}
                      size="small"
                      color="primary"
                    />
                    <Chip
                      label={`Docks: ${station.numDocksAvailable}`}
                      size="small"
                      color="secondary"
                    />
                  </Box>
                  <Typography variant="caption" display="block" sx={{ mt: 1 }}>
                    Capacity: {station.capacity} | Occupancy:{" "}
                    {station.occupancyRate.toFixed(1)}%
                  </Typography>
                </Box>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </Box>
    </Paper>
  );
};
