/**
 * Live cycle station map — uses StationLiveDTO with status-coloured circle markers
 */

import { useEffect, useCallback } from "react";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Popup,
  useMap,
} from "react-leaflet";
import { Box, Typography, Chip } from "@mui/material";
import L from "leaflet";
import type { StationLiveDTO } from "@/types";
import "leaflet/dist/leaflet.css";

// Required to avoid broken icon references in bundled output
import "leaflet/dist/images/marker-icon.png";
import "leaflet/dist/images/marker-shadow.png";

const STATUS_FILL: Record<string, string> = {
  GREEN: "#4ade80",
  YELLOW: "#fbbf24",
  RED: "#f87171",
};

interface LiveCycleStationMapProps {
  stations: StationLiveDTO[];
  height?: number | string;
  darkTiles?: boolean;
  onStationClick?: (station: StationLiveDTO) => void;
}

const FitBounds = ({ stations }: { stations: StationLiveDTO[] }) => {
  const map = useMap();

  const fitBounds = useCallback(() => {
    if (stations.length > 0) {
      const bounds = L.latLngBounds(
        stations.map((s) => [s.latitude, s.longitude]),
      );
      map.fitBounds(bounds, { padding: [50, 50] });
    }
  }, [stations, map]);

  useEffect(() => {
    fitBounds();
  }, [fitBounds]);

  return <></>;
};

export const LiveCycleStationMap = ({
  stations,
  height = "100%",
  darkTiles = false,
  onStationClick,
}: LiveCycleStationMapProps) => {
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
          <CircleMarker
            key={station.stationId}
            center={[station.latitude, station.longitude]}
            radius={8}
            pathOptions={{
              color: "#fff",
              weight: 1.5,
              fillColor: STATUS_FILL[station.statusColor] ?? "#94a3b8",
              fillOpacity: 0.9,
            }}
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
                    label={`${station.availableBikes} bikes`}
                    size="small"
                    color="primary"
                  />
                  <Chip
                    label={`${station.availableDocks} docks`}
                    size="small"
                    color="secondary"
                  />
                </Box>
                <Typography
                  variant="caption"
                  display="block"
                  sx={{ mt: 0.75, opacity: 0.7 }}
                >
                  Capacity {station.capacity} &middot; Bikes:{" "}
                  {station.bikeAvailabilityPct.toFixed(0)}% &middot; Docks:{" "}
                  {station.dockAvailabilityPct.toFixed(0)}%
                </Typography>
              </Box>
            </Popup>
          </CircleMarker>
        ))}
      </MapContainer>
    </Box>
  );
};
