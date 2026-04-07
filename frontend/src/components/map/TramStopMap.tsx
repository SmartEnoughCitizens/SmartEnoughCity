/**
 * Tram stop map — shows Red/Green line stops with live forecast popups
 */
import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";
import { Typography, Button, CircularProgress, Chip } from "@mui/material";
import type { TramLiveForecast, TramAlternativeRoute } from "@/types";
import { dashboardApi } from "@/api";
import { useState } from "react";
import "leaflet/dist/leaflet.css";

const DISRUPTION_KEYWORDS = [
  "not in service",
  "disruption",
  "suspended",
  "suspension",   // ← hinzufügen
  "delay",
  "fault",
  "no service",
  "terminated",
  "partial",      // ← optional, fängt noch mehr
];



const isDisrupted = (forecasts: TramLiveForecast[]) =>
  forecasts.some((f) =>
    DISRUPTION_KEYWORDS.some((kw) => f.message?.toLowerCase().includes(kw))
  );

const iconFor = (type: string) =>
  type === "bus" ? "🚌" : type === "rail" ? "🚂" : "🚲";

const AlternativesSection = ({ stopId }: { stopId: string }) => {
  const [alternatives, setAlternatives] = useState<TramAlternativeRoute[] | null>(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    const data = await dashboardApi.getTramAlternativeRoutes(stopId);
    setAlternatives(data);
    setLoading(false);
  };

  return (
    <div style={{ marginTop: 8 }}>
      {!alternatives && (
        <Button
          size="small"
          variant="outlined"
          color="warning"
          onClick={load}
          disabled={loading}
        >
          {loading ? <CircularProgress size={12} /> : "Show alternatives"}
        </Button>
      )}
      {alternatives?.length === 0 && (
        <Typography variant="caption" color="text.secondary">
          No alternatives nearby
        </Typography>
      )}
      {alternatives && alternatives.length > 0 && (
        <>
          <Typography
            variant="caption"
            fontWeight="bold"
            display="block"
            sx={{ mt: 0.5 }}
          >
            Nearby alternatives:
          </Typography>
          {alternatives.slice(0, 5).map((a, i) => (
            <div key={i} style={{ fontSize: 12, marginTop: 2 }}>
              {iconFor(a.transportType)} {a.stopName}
              <span style={{ color: "#888" }}> — {a.distanceM}m</span>
              {a.availableBikes != null && (
                <Chip
                  label={`${a.availableBikes} bikes`}
                  size="small"
                  sx={{ ml: 0.5, height: 16, fontSize: 10 }}
                />
              )}
            </div>
          ))}
        </>
      )}
    </div>
  );
};

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
  console.log("TramStopMap received forecasts:", forecasts); // ← hier

  const defaultCenter: [number, number] = [53.3398, -6.2603];

  const tileUrl = darkTiles
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution = darkTiles
    ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
    : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

 const stopMap = new Map<
  string,
  {
    lat: number;
    lon: number;
    name: string;
    line: string;
    forecasts: TramLiveForecast[];
  }
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


  console.log("stops:", stops);
  console.log("disrupted stops:", stops.filter(s => isDisrupted(s.forecasts)));
  
console.log("stops with messages:", stops.map(s => ({ 
  name: s.name, 
  messages: s.forecasts.map(f => f.message),
  disrupted: isDisrupted(s.forecasts)
})));
  
  
  return (
    <MapContainer
      center={defaultCenter}
      zoom={13}
      style={{ height, width: "100%" }}
      zoomControl={false}
    >
      <TileLayer attribution={tileAttribution} url={tileUrl} />
      {stops.map((stop) => {
        const disrupted = isDisrupted(stop.forecasts);
        return (
          <CircleMarker
            key={`${stop.lat}-${stop.lon}`}
            center={[stop.lat, stop.lon]}
            radius={7}
            pathOptions={{
              color: disrupted ? "#f97316" : "#fff",
              weight: disrupted ? 3 : 2,
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
                sx={{
                  textTransform: "capitalize",
                  color: stop.line === "red" ? "#ef4444" : "#22c55e",
                }}
              >
                {stop.line} line
              </Typography>

              {stop.forecasts.slice(0, 4).map((f, i) => (
                <div key={i} style={{ fontSize: 12, marginTop: 2 }}>
                  → {f.destination}:{" "}
                  <strong>{f.dueMins ?? "—"} min</strong>{" "}
                  <span style={{ color: "#888" }}>({f.direction})</span>
                </div>
              ))}

              {stop.forecasts[0]?.message && (
                <Typography
                  variant="caption"
                  color={disrupted ? "error" : "text.secondary"}
                  sx={{ mt: 0.5, display: "block" }}
                >
                  {disrupted ? "⚠ " : ""}
                  {stop.forecasts[0].message}
                </Typography>
              )}

              {disrupted && (
                <AlternativesSection stopId={stop.forecasts[0].stopId} />
              )}
            </Popup>
          </CircleMarker>
        );
      })}
    </MapContainer>
  );
};