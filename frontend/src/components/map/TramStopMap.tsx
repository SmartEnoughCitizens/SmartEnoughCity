/**
 * Tram stop map — shows Red/Green line stops with live forecast popups
 */

import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";
import { Typography } from "@mui/material";
import type { TramLiveForecast } from "@/types";
import "leaflet/dist/leaflet.css";

interface TramStopMapProps {
  forecasts: TramLiveForecast[];
  height?: string;
  darkTiles?: boolean;
}

export const TramStopMap = ({
  forecasts,
  height = "100%",
  darkTiles = false,
}: TramStopMapProps) => {
  // Dublin center
  const defaultCenter: [number, number] = [53.3398, -6.2603];

  const tileUrl = darkTiles
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution = darkTiles
    ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
    : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  // Deduplicate by stopId, keeping the first forecast per stop for the marker
  const stopMap = new Map<
    string,
    { lat: number; lon: number; name: string; line: string; forecasts: TramLiveForecast[] }
  >();
  for (const f of forecasts) {
    if (f.lat == null || f.lon == null) continue;
    if (!stopMap.has(f.stopId)) {
      stopMap.set(f.stopId, {
        lat: f.lat,
        lon: f.lon,
        name: f.stopName,
        line: f.line,
        forecasts: [],
      });
    }
    stopMap.get(f.stopId)!.forecasts.push(f);
  }

  const stops = [...stopMap.values()];

  return (
    <MapContainer
      center={defaultCenter}
      zoom={13}
      style={{ height, width: "100%" }}
      zoomControl={false}
    >
      <TileLayer attribution={tileAttribution} url={tileUrl} />
      {stops.map((stop) => (
        <CircleMarker
          key={`${stop.lat}-${stop.lon}`}
          center={[stop.lat, stop.lon]}
          radius={7}
          pathOptions={{
            color: "#fff",
            weight: 2,
            fillColor: stop.line === "red" ? "#ef4444" : "#22c55e",
            fillOpacity: 0.9,
          }}
        >
          <Popup>
            <Typography variant="subtitle2" fontWeight="bold">
              {stop.name}
            </Typography>
            <Typography
              variant="caption"
              sx={{ textTransform: "capitalize", color: stop.line === "red" ? "#ef4444" : "#22c55e" }}
            >
              {stop.line} line
            </Typography>
            {stop.forecasts.slice(0, 4).map((f, i) => (
              <div key={i} style={{ fontSize: 12, marginTop: 2 }}>
                → {f.destination}: <strong>{f.dueMins ?? "—"} min</strong>{" "}
                <span style={{ color: "#888" }}>({f.direction})</span>
              </div>
            ))}
            {stop.forecasts[0]?.message && (
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: "block" }}>
                {stop.forecasts[0].message}
              </Typography>
            )}
          </Popup>
        </CircleMarker>
      ))}
    </MapContainer>
  );
};
