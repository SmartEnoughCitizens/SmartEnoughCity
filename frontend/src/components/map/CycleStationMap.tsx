/**
 * Cycle station map component using React Leaflet
 * Full-viewport ready with dark tile support
 */

import { useEffect, useCallback } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import { Box, Typography, Chip } from "@mui/material";
import L from "leaflet";
import type { CycleStation } from "@/types";
import "leaflet/dist/leaflet.css";

// Fix for default marker icon issue with Webpack
import markerIcon from "leaflet/dist/images/marker-icon.png";
import iconShadow from "leaflet/dist/images/marker-shadow.png";

const DefaultIcon = L.icon({
  iconUrl: markerIcon,
  shadowUrl: iconShadow,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

L.Marker.prototype.options.icon = DefaultIcon;

interface CycleStationMapProps {
  stations: CycleStation[];
  height?: number | string;
  /** Use a dark-toned tile layer */
  darkTiles?: boolean;
  /** Called when a station marker is clicked */
  onStationClick?: (station: CycleStation) => void;
}

// Component to fit bounds when stations change
const FitBounds = ({ stations }: { stations: CycleStation[] }) => {
  const map = useMap();

  const fitBounds = useCallback(() => {
    if (stations.length > 0) {
      const bounds = L.latLngBounds(
        stations.map((station) => [station.lat, station.lon]),
      );
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [stations, map]);

  useEffect(() => {
    fitBounds();
  }, [fitBounds]);

  return <></>;
};

export const CycleStationMap = ({
  stations,
  height = "100%",
  darkTiles = false,
  onStationClick,
}: CycleStationMapProps) => {
  const defaultCenter: [number, number] = [53.3498, -6.2603];

  const tileUrl = darkTiles
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution = darkTiles
    ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
    : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  return (
    <Box sx={{ height, width: "100%", position: "relative" }}>
      <MapContainer
        center={defaultCenter}
        zoom={13}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
      >
        <TileLayer attribution={tileAttribution} url={tileUrl} />
        <FitBounds stations={stations} />
        {stations.map((station) => (
          <Marker
            key={station.id}
            position={[station.lat, station.lon]}
            eventHandlers={{
              click: () => onStationClick?.(station),
            }}
          >
            <Popup>
              <Box sx={{ minWidth: 180, p: 0.5 }}>
                <Typography
                  variant="subtitle2"
                  sx={{ fontWeight: 600, mb: 0.5 }}
                >
                  {station.name}
                </Typography>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  display="block"
                  sx={{ mb: 1 }}
                >
                  {station.address}
                </Typography>
                <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
                  <Chip
                    label={`${station.numBikesAvailable} bikes`}
                    size="small"
                    color="primary"
                  />
                  <Chip
                    label={`${station.numDocksAvailable} docks`}
                    size="small"
                    color="secondary"
                  />
                </Box>
                <Typography
                  variant="caption"
                  display="block"
                  sx={{ mt: 0.75, opacity: 0.7 }}
                >
                  Capacity {station.capacity} &middot;{" "}
                  {station.occupancyRate.toFixed(0)}% full
                </Typography>
              </Box>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </Box>
  );
};
