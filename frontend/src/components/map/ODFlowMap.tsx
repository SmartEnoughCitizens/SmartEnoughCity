/**
 * OD Flow map — renders pre-fetched OSRM cycling routes.
 * Route fetching happens at dashboard level (useODRoutes hook) so routes
 * are ready before the user even opens this tab.
 */

import { useEffect } from "react";
import {
  CircleMarker,
  MapContainer,
  Marker,
  Polyline,
  Popup,
  TileLayer,
  useMap,
} from "react-leaflet";
import { Box, LinearProgress, Typography } from "@mui/material";
import L from "leaflet";
import type { StationLiveDTO, StationODPairDTO } from "@/types";
import type { ODRouteCache } from "@/hooks";
import "leaflet/dist/leaflet.css";

interface ODFlowMapProps {
  stations: StationLiveDTO[];
  odPairs: StationODPairDTO[];
  thresholds: { low: number; high: number };
  filterStationId: number | null;
  selectedPairKey: string | null;
  routeCache: ODRouteCache;
  routeProgress: { done: number; total: number } | null;
  height?: number | string;
  darkTiles?: boolean;
}

type LatLon = [number, number];

const FitBounds = ({ pairs }: { pairs: StationODPairDTO[] }) => {
  const map = useMap();
  const key = pairs
    .map((p) => p.originStationId + "-" + p.destStationId)
    .join("|");
  useEffect(() => {
    if (pairs.length === 0) return;
    const pts: LatLon[] = pairs.flatMap((p) => [
      [p.originLat, p.originLon],
      [p.destLat, p.destLon],
    ]);
    map.fitBounds(L.latLngBounds(pts), { padding: [60, 60] });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);
  return null;
};

const FitToRoute = ({
  routeCoords,
  pair,
}: {
  routeCoords: LatLon[] | undefined;
  pair: StationODPairDTO;
}) => {
  const map = useMap();
  useEffect(() => {
    const pts: LatLon[] =
      routeCoords && routeCoords.length > 0
        ? routeCoords
        : [
            [pair.originLat, pair.originLon],
            [pair.destLat, pair.destLon],
          ];
    map.fitBounds(L.latLngBounds(pts), { padding: [80, 80] });
  }, [map, routeCoords, pair]);
  return null;
};

const makeIcon = (color: string, label: string) =>
  L.divIcon({
    className: "",
    html: `<div style="
      width:28px;height:28px;border-radius:50%;
      background:${color};border:3px solid #fff;
      box-shadow:0 2px 6px rgba(0,0,0,0.45);
      display:flex;align-items:center;justify-content:center;
      font-size:11px;font-weight:700;color:#fff;
    ">${label}</div>`,
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  });

const defaultCenter: [number, number] = [53.3498, -6.2603];

export const ODFlowMap = ({
  stations,
  odPairs,
  thresholds,
  selectedPairKey,
  routeCache,
  routeProgress,
  height = "100%",
  darkTiles = false,
}: ODFlowMapProps) => {
  const tileUrl = darkTiles
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution = darkTiles
    ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
    : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  const selectedPair = selectedPairKey
    ? (odPairs.find(
        (p) => `${p.originStationId}-${p.destStationId}` === selectedPairKey,
      ) ?? null)
    : null;

  const stationFlow = new Map<number, number>();
  for (const p of odPairs) {
    stationFlow.set(
      p.originStationId,
      (stationFlow.get(p.originStationId) ?? 0) + p.estimatedTrips,
    );
    stationFlow.set(
      p.destStationId,
      (stationFlow.get(p.destStationId) ?? 0) + p.estimatedTrips,
    );
  }
  const maxFlow = Math.max(...stationFlow.values(), 1);

  return (
    <Box sx={{ height, width: "100%", position: "relative" }}>
      {/* Route loading progress bar */}
      {routeProgress && (
        <Box
          sx={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            zIndex: 1100,
            bgcolor: "background.paper",
            px: 2,
            pt: 0.75,
            pb: 0.5,
          }}
        >
          <Box
            sx={{ display: "flex", justifyContent: "space-between", mb: 0.25 }}
          >
            <Typography variant="caption" color="text.secondary">
              Loading cycle routes…
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {routeProgress.done} / {routeProgress.total}
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={(routeProgress.done / routeProgress.total) * 100}
            sx={{ height: 3, borderRadius: 1 }}
          />
        </Box>
      )}

      {/* Legend */}
      <Box
        sx={{
          position: "absolute",
          bottom: 24,
          right: 12,
          zIndex: 1000,
          bgcolor: "background.paper",
          borderRadius: 2,
          px: 1.5,
          py: 1,
          boxShadow: 3,
          display: "flex",
          flexDirection: "column",
          gap: 0.5,
        }}
      >
        {[
          { color: "#ef4444", label: "High flow" },
          { color: "#f97316", label: "Medium flow" },
          { color: "#22c55e", label: "Low flow" },
          { color: "#f59e0b", label: "Selected" },
        ].map(({ color, label }) => (
          <Box
            key={label}
            sx={{ display: "flex", alignItems: "center", gap: 1 }}
          >
            <Box
              sx={{
                width: 24,
                height: 4,
                borderRadius: 1,
                bgcolor: color,
                flexShrink: 0,
              }}
            />
            <Typography
              variant="caption"
              sx={{ fontSize: "0.65rem", color: "text.secondary" }}
            >
              {label}
            </Typography>
          </Box>
        ))}
      </Box>

      <MapContainer
        center={defaultCenter}
        zoom={13}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
      >
        <TileLayer attribution={tileAttribution} url={tileUrl} />
        {selectedPair ? (
          <FitToRoute
            routeCoords={routeCache.get(selectedPairKey!)}
            pair={selectedPair}
          />
        ) : (
          <FitBounds pairs={odPairs} />
        )}

        {/* OD route polylines */}
        {odPairs.map((pair) => {
          const pairKey = `${pair.originStationId}-${pair.destStationId}`;
          const isSelected = pairKey === selectedPairKey;
          const positions = routeCache.get(pairKey);
          if (!positions) return null;

          // Hide other routes when one is selected
          if (selectedPairKey && !isSelected) return null;

          // Colour based on absolute trip count vs data-driven thresholds
          const trips = pair.estimatedTrips;
          const intensityColor =
            trips >= thresholds.high
              ? "#ef4444"
              : trips >= thresholds.low
                ? "#f97316"
                : "#22c55e";

          const maxTrips = Math.max(...odPairs.map((p) => p.estimatedTrips), 1);
          const ratio = trips / maxTrips;

          return (
            <Polyline
              key={pairKey}
              positions={positions}
              pathOptions={{
                color: isSelected ? "#f59e0b" : intensityColor,
                weight: isSelected ? 6 : Math.max(2, ratio * 5),
                opacity: isSelected ? 1 : Math.max(0.3, ratio * 0.6 + 0.2),
                lineCap: "round",
                lineJoin: "round",
              }}
            >
              <Popup>
                <Typography variant="caption" fontWeight={600} display="block">
                  {pair.originName}
                </Typography>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  display="block"
                >
                  → {pair.destName}
                </Typography>
                <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>
                  ~{pair.estimatedTrips} estimated trips ·{" "}
                  {pair.distanceKm.toFixed(2)} km
                </Typography>
              </Popup>
            </Polyline>
          );
        })}

        {/* Origin / destination markers for selected pair */}
        {selectedPair && (
          <>
            <Marker
              position={[selectedPair.originLat, selectedPair.originLon]}
              icon={makeIcon("#22c55e", "A")}
            >
              <Popup>
                <Typography variant="caption" fontWeight={600}>
                  Origin: {selectedPair.originName}
                </Typography>
              </Popup>
            </Marker>
            <Marker
              position={[selectedPair.destLat, selectedPair.destLon]}
              icon={makeIcon("#ef4444", "B")}
            >
              <Popup>
                <Typography variant="caption" fontWeight={600}>
                  Destination: {selectedPair.destName}
                </Typography>
              </Popup>
            </Marker>
          </>
        )}

        {/* Station circles sized by total flow */}
        {stations.map((s) => {
          const isOrigin = s.stationId === selectedPair?.originStationId;
          const isDest = s.stationId === selectedPair?.destStationId;
          if (isOrigin || isDest) return null;

          const flow = stationFlow.get(s.stationId) ?? 0;
          const radius = 3 + (flow / maxFlow) * 5;

          return (
            <CircleMarker
              key={s.stationId}
              center={[s.latitude, s.longitude]}
              radius={radius}
              pathOptions={{
                color: "#fff",
                weight: 1,
                fillColor: "#3b82f6",
                fillOpacity: selectedPair ? 0.15 : 0.6,
              }}
            >
              <Popup>
                <Typography variant="caption" fontWeight={600} display="block">
                  {s.name}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {s.availableBikes} bikes · {s.availableDocks} docks
                </Typography>
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>
    </Box>
  );
};
