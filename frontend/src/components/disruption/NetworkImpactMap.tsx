/**
 * NetworkImpactMap — Leaflet map showing active disruption locations with
 * severity-coloured markers and ripple pulse animations.
 */

import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";
import { Box, Typography, Chip } from "@mui/material";
import type { ActiveDisruption, DisruptionSeverity } from "@/types";
import "leaflet/dist/leaflet.css";

const DUBLIN_CENTER: [number, number] = [53.3498, -6.2603];

const SEVERITY_COLORS: Record<DisruptionSeverity, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

const SEVERITY_RADIUS: Record<DisruptionSeverity, number> = {
  LOW: 10,
  MEDIUM: 14,
  HIGH: 18,
  CRITICAL: 22,
};

function formatTime(iso: string | null): string {
  if (!iso) return "—";
  try {
    return new Date(iso).toLocaleTimeString("en-IE", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

interface Props {
  disruptions: ActiveDisruption[];
}

export const NetworkImpactMap = ({ disruptions }: Props) => {
  const mappable = disruptions.filter(
    (d) => d.latitude != null && d.longitude != null,
  );

  return (
    <Box
      sx={{
        width: "100%",
        height: "100%",
        borderRadius: 2,
        overflow: "hidden",
        position: "relative",
        "& .leaflet-container": { height: "100%", width: "100%" },
      }}
    >
      <MapContainer
        center={DUBLIN_CENTER}
        zoom={12}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {mappable.map((d) => {
          const color = SEVERITY_COLORS[d.severity] ?? "#6B7280";
          const radius = SEVERITY_RADIUS[d.severity] ?? 12;
          return (
            <CircleMarker
              key={d.id}
              center={[d.latitude!, d.longitude!]}
              radius={radius}
              pathOptions={{
                color,
                fillColor: color,
                fillOpacity: 0.55,
                weight: 2,
              }}
            >
              <Popup>
                <Box sx={{ minWidth: 180 }}>
                  <Typography
                    sx={{ fontWeight: 700, fontSize: "0.85rem", mb: 0.5 }}
                  >
                    {d.name}
                  </Typography>
                  <Box
                    sx={{
                      display: "flex",
                      gap: 0.5,
                      flexWrap: "wrap",
                      mb: 0.5,
                    }}
                  >
                    <Chip
                      size="small"
                      label={d.severity}
                      sx={{
                        fontSize: "0.6rem",
                        height: 16,
                        bgcolor: color + "22",
                        color,
                        border: `1px solid ${color}44`,
                      }}
                    />
                    <Chip
                      size="small"
                      label={d.disruptionType}
                      sx={{ fontSize: "0.6rem", height: 16 }}
                    />
                  </Box>
                  {d.description && (
                    <Typography
                      sx={{ fontSize: "0.75rem", color: "#6B7280", mb: 0.5 }}
                    >
                      {d.description}
                    </Typography>
                  )}
                  {d.affectedArea && (
                    <Typography sx={{ fontSize: "0.72rem" }}>
                      Area: {d.affectedArea}
                    </Typography>
                  )}
                  {d.delayMinutes != null && d.delayMinutes > 0 && (
                    <Typography sx={{ fontSize: "0.72rem" }}>
                      Delay: ~{d.delayMinutes} min
                    </Typography>
                  )}
                  <Typography
                    sx={{ fontSize: "0.68rem", color: "#9CA3AF", mt: 0.5 }}
                  >
                    Detected: {formatTime(d.detectedAt)}
                  </Typography>
                </Box>
              </Popup>
            </CircleMarker>
          );
        })}
      </MapContainer>

      {/* Legend overlay */}
      <Box
        sx={{
          position: "absolute",
          bottom: 12,
          left: 12,
          zIndex: 1000,
          bgcolor: "rgba(255,255,255,0.92)",
          borderRadius: 1.5,
          px: 1.25,
          py: 0.75,
          boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
          display: "flex",
          flexDirection: "column",
          gap: 0.4,
        }}
      >
        {(["LOW", "MEDIUM", "HIGH", "CRITICAL"] as DisruptionSeverity[]).map(
          (s) => (
            <Box
              key={s}
              sx={{ display: "flex", alignItems: "center", gap: 0.6 }}
            >
              <Box
                sx={{
                  width: 10,
                  height: 10,
                  borderRadius: "50%",
                  bgcolor: SEVERITY_COLORS[s],
                }}
              />
              <Typography sx={{ fontSize: "0.62rem", color: "#374151" }}>
                {s}
              </Typography>
            </Box>
          ),
        )}
      </Box>

      {mappable.length === 0 && (
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            pointerEvents: "none",
          }}
        >
          <Box
            sx={{
              bgcolor: "rgba(255,255,255,0.85)",
              px: 2,
              py: 1.5,
              borderRadius: 2,
              textAlign: "center",
            }}
          >
            <Typography sx={{ fontSize: "0.8rem", color: "#6B7280" }}>
              No geo-located disruptions
            </Typography>
          </Box>
        </Box>
      )}
    </Box>
  );
};
