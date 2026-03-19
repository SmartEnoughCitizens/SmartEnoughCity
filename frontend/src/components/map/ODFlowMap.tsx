/**
 * Origin-Destination flow map — draws actual OSRM cycle routes between station pairs.
 * Falls back to a bezier arc only while the route is loading.
 * Failures are NOT cached so OSRM will be retried on next render.
 */

import { useEffect, useCallback, useState, useRef, useMemo } from "react";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Polyline,
  Popup,
  useMap,
} from "react-leaflet";
import { Box, Typography, Chip } from "@mui/material";
import L from "leaflet";
import type { StationLiveDTO, StationODPairDTO } from "@/types";
import "leaflet/dist/leaflet.css";
import "leaflet/dist/images/marker-icon.png";
import "leaflet/dist/images/marker-shadow.png";

// ── Colour / style helpers ────────────────────────────────────────────────────

const heatColor = (t: number): string => {
  if (t >= 0.66) return "#dc2626";
  if (t >= 0.33) return "#f97316";
  return "#4ade80";
};

const lineWeight = (trips: number, maxTrips: number): number =>
  maxTrips === 0 ? 2 : 2 + Math.round((trips / maxTrips) * 5);

const lineOpacity = (trips: number, maxTrips: number): number =>
  maxTrips === 0 ? 0.5 : 0.45 + (trips / maxTrips) * 0.45;

/** Temporary bezier shown only while OSRM route is loading */
const bezierArc = (
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number,
  steps = 20,
): [number, number][] => {
  const dLat = lat2 - lat1;
  const dLon = lon2 - lon1;
  const len = Math.hypot(dLat, dLon) || 1;
  const offset = len * 0.3;
  const ctrlLat = (lat1 + lat2) / 2 + (-dLon / len) * offset;
  const ctrlLon = (lon1 + lon2) / 2 + (dLat / len) * offset;
  const pts: [number, number][] = [];
  for (let i = 0; i <= steps; i++) {
    const t = i / steps;
    const u = 1 - t;
    pts.push([
      u * u * lat1 + 2 * u * t * ctrlLat + t * t * lat2,
      u * u * lon1 + 2 * u * t * ctrlLon + t * t * lon2,
    ]);
  }
  return pts;
};

// ── OSRM fetch ────────────────────────────────────────────────────────────────

const fetchCycleRoute = async (
  originLat: number,
  originLon: number,
  destLat: number,
  destLon: number,
): Promise<[number, number][]> => {
  // routing.openstreetmap.de/routed-bike is a public OSRM instance with bicycle profile
  const url =
    `https://routing.openstreetmap.de/routed-bike/route/v1/driving/` +
    `${originLon},${originLat};${destLon},${destLat}` +
    `?overview=full&geometries=geojson`;

  const res = await fetch(url);
  if (!res.ok) throw new Error(`OSRM ${res.status}`);
  const json = (await res.json()) as {
    routes?: { geometry: { coordinates: [number, number][] } }[];
  };
  if (!json.routes?.length) throw new Error("No route returned");
  // OSRM returns [lon, lat]; Leaflet needs [lat, lon]
  return json.routes[0].geometry.coordinates.map(
    ([lon, lat]) => [lat, lon] as [number, number],
  );
};

// ── Persistent module-level cache (only successful routes stored) ─────────────
// Failures are NOT stored here so OSRM is retried next time.

const routeCache = new Map<string, [number, number][]>();

const cacheKey = (p: StationODPairDTO) =>
  `${p.originStationId}-${p.destStationId}`;

// ── Sub-components ────────────────────────────────────────────────────────────

const FitBounds = ({ stations }: { stations: StationLiveDTO[] }) => {
  const map = useMap();
  const fitBounds = useCallback(() => {
    if (stations.length > 0) {
      map.fitBounds(
        L.latLngBounds(stations.map((s) => [s.latitude, s.longitude])),
        { padding: [50, 50] },
      );
    }
  }, [stations, map]);
  useEffect(() => {
    fitBounds();
  }, [fitBounds]);
  return <></>;
};

const Legend = () => (
  <Box
    sx={{
      position: "absolute",
      bottom: 24,
      left: 16,
      zIndex: 1000,
      bgcolor: "rgba(0,0,0,0.6)",
      color: "#fff",
      borderRadius: 2,
      px: 1.5,
      py: 1,
      backdropFilter: "blur(6px)",
    }}
  >
    <Typography
      variant="caption"
      sx={{ display: "block", mb: 0.75, fontWeight: 600 }}
    >
      Cycle path intensity
    </Typography>
    {[
      { color: "#dc2626", label: "Extreme" },
      { color: "#f97316", label: "Medium" },
      { color: "#4ade80", label: "Low" },
    ].map(({ color, label }) => (
      <Box
        key={label}
        sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.4 }}
      >
        <Box sx={{ width: 24, height: 4, borderRadius: 1, bgcolor: color }} />
        <Typography variant="caption">{label}</Typography>
      </Box>
    ))}
  </Box>
);

// ── Main component ────────────────────────────────────────────────────────────

interface ODFlowMapProps {
  stations: StationLiveDTO[];
  odPairs: StationODPairDTO[];
  globalMaxTrips: number;
  filterStationId?: number | null;
  selectedPairKey?: string | null;
  height?: number | string;
  darkTiles?: boolean;
}

const STATUS_FILL: Record<string, string> = {
  GREEN: "#4ade80",
  YELLOW: "#fbbf24",
  RED: "#f87171",
};

export const ODFlowMap = ({
  stations,
  odPairs,
  globalMaxTrips,
  filterStationId,
  selectedPairKey,
  height = "100%",
  darkTiles = false,
}: ODFlowMapProps) => {
  const [routes, setRoutes] = useState<Map<string, [number, number][]>>(
    () => new Map(routeCache),
  );
  const inFlight = useRef(new Set<string>());

  const tileUrl = darkTiles
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png";

  const tileAttribution =
    '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>';

  const visiblePairs = useMemo(
    () =>
      filterStationId
        ? odPairs.filter(
            (p) =>
              p.originStationId === filterStationId ||
              p.destStationId === filterStationId,
          )
        : odPairs,
    [odPairs, filterStationId],
  );

  // Stable key string so the effect only fires when the actual pair set changes
  const pairKeys = useMemo(
    () => visiblePairs.map((p) => cacheKey(p)).join(","),
    [visiblePairs],
  );

  const maxTrips = globalMaxTrips > 0 ? globalMaxTrips : 1;

  const activeIds = useMemo(
    () =>
      new Set<number>(
        visiblePairs.flatMap((p) => [p.originStationId, p.destStationId]),
      ),
    [visiblePairs],
  );

  // Station IDs for the selected pair (highlighted with a ring)
  const selectedStationIds = useMemo(() => {
    if (!selectedPairKey) return new Set<number>();
    const pair = visiblePairs.find((p) => cacheKey(p) === selectedPairKey);
    if (!pair) return new Set<number>();
    return new Set([pair.originStationId, pair.destStationId]);
  }, [selectedPairKey, visiblePairs]);

  // Fetch OSRM routes for visible pairs not yet successfully cached
  useEffect(() => {
    const missing = visiblePairs.filter(
      (p) => !routeCache.has(cacheKey(p)) && !inFlight.current.has(cacheKey(p)),
    );
    if (missing.length === 0) return;

    for (const pair of missing) {
      const key = cacheKey(pair);
      inFlight.current.add(key);

      fetchCycleRoute(
        pair.originLat,
        pair.originLon,
        pair.destLat,
        pair.destLon,
      )
        .then((path) => {
          console.log(`Cycle route loaded: ${key} (${path.length} points)`);
          routeCache.set(key, path);
          setRoutes(new Map(routeCache));
        })
        .catch((error) => {
          console.warn(`OSRM route failed for ${key}:`, error);
        })
        .finally(() => {
          inFlight.current.delete(key);
        });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pairKeys]);

  return (
    <Box sx={{ height, width: "100%", position: "relative" }}>
      <MapContainer
        center={[53.3498, -6.2603]}
        zoom={13}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
      >
        <TileLayer attribution={tileAttribution} url={tileUrl} />
        <FitBounds stations={stations} />

        {/* Cycle paths — real road geometry or temporary bezier while loading */}
        {visiblePairs.map((pair, idx) => {
          const key = cacheKey(pair);
          const intensity = pair.estimatedTrips / maxTrips;
          const isSelected = selectedPairKey === key;
          const isHighlighted =
            filterStationId != null &&
            (filterStationId === pair.originStationId ||
              filterStationId === pair.destStationId);

          const path =
            routes.get(key) ??
            bezierArc(
              pair.originLat,
              pair.originLon,
              pair.destLat,
              pair.destLon,
            );

          const isRealRoute = routes.has(key);

          return (
            <Polyline
              key={`od-${key}-${idx}`}
              positions={path}
              pathOptions={{
                color: isSelected
                  ? "#facc15"
                  : isHighlighted
                    ? "#ffffff"
                    : heatColor(intensity),
                weight: isSelected
                  ? lineWeight(pair.estimatedTrips, maxTrips) + 3
                  : lineWeight(pair.estimatedTrips, maxTrips),
                opacity: isSelected
                  ? 1
                  : isHighlighted
                    ? 1
                    : isRealRoute
                      ? lineOpacity(pair.estimatedTrips, maxTrips)
                      : 0.3,
                dashArray: isRealRoute ? undefined : "6 4",
              }}
            >
              <Popup>
                <Box sx={{ minWidth: 180, p: 0.5 }}>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    display="block"
                  >
                    Origin
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {pair.originName}
                  </Typography>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    display="block"
                    sx={{ mt: 0.5 }}
                  >
                    Destination
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {pair.destName}
                  </Typography>
                  <Chip
                    label={`~${pair.estimatedTrips} trips / month`}
                    size="small"
                    color="warning"
                    sx={{ mt: 0.75 }}
                  />
                  {!isRealRoute && (
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      display="block"
                      sx={{ mt: 0.5 }}
                    >
                      Loading cycle route…
                    </Typography>
                  )}
                </Box>
              </Popup>
            </Polyline>
          );
        })}

        {/* Station dots */}
        {stations.map((station) => {
          const isActive = activeIds.has(station.stationId);
          const isEndpoint = selectedStationIds.has(station.stationId);
          return (
            <CircleMarker
              key={station.stationId}
              center={[station.latitude, station.longitude]}
              radius={isEndpoint ? 12 : isActive ? 9 : 5}
              pathOptions={{
                color: isEndpoint ? "#facc15" : "#fff",
                weight: isEndpoint ? 3 : isActive ? 2 : 1,
                fillColor: isEndpoint
                  ? (STATUS_FILL[station.statusColor] ?? "#94a3b8")
                  : isActive
                    ? (STATUS_FILL[station.statusColor] ?? "#94a3b8")
                    : "#94a3b8",
                fillOpacity: isEndpoint ? 1 : isActive ? 1 : 0.75,
              }}
            >
              <Popup>
                <Box sx={{ minWidth: 160, p: 0.5 }}>
                  <Typography
                    variant="subtitle2"
                    sx={{ fontWeight: 600, mb: 0.5 }}
                  >
                    {station.name}
                  </Typography>
                  <Box sx={{ display: "flex", gap: 0.5 }}>
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
                </Box>
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>

      <Legend />
    </Box>
  );
};
