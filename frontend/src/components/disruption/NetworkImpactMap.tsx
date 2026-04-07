/**
 * NetworkImpactMap — Leaflet map showing active disruption locations with
 * severity-coloured pulsating DivIcon markers. Flies to the selected marker.
 */

import { useEffect, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
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

const SEVERITY_SIZE: Record<DisruptionSeverity, number> = {
  LOW: 18,
  MEDIUM: 22,
  HIGH: 26,
  CRITICAL: 30,
};

// Inject pulse keyframes once
const STYLE_ID = "disruption-pulse-styles";
function ensurePulseStyles() {
  if (document.querySelector(`#${STYLE_ID}`)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = `
    @keyframes disruption-ripple {
      0%   { transform: scale(1);   opacity: 0.9; }
      70%  { transform: scale(2.4); opacity: 0;   }
      100% { transform: scale(2.4); opacity: 0;   }
    }
    .disruption-marker-ring {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      animation: disruption-ripple 1.8s ease-out infinite;
    }
  `;
  document.head.append(style);
}

function makeDivIcon(severity: DisruptionSeverity, selected: boolean): L.DivIcon {
  ensurePulseStyles();
  const color = SEVERITY_COLORS[severity] ?? "#6B7280";
  const size = SEVERITY_SIZE[severity] ?? 22;
  const ring = selected
    ? `<span class="disruption-marker-ring" style="border: 3px solid ${color};"></span>`
    : `<span class="disruption-marker-ring" style="border: 2px solid ${color}88;"></span>`;

  return L.divIcon({
    className: "",
    html: `
      <div style="
        position: relative;
        width: ${size}px;
        height: ${size}px;
      ">
        ${ring}
        <div style="
          position: absolute;
          inset: 0;
          border-radius: 50%;
          background: ${color};
          opacity: ${selected ? 1 : 0.75};
          border: 2px solid ${selected ? "#fff" : color + "cc"};
          box-shadow: 0 0 ${selected ? 10 : 6}px ${color}99;
        "></div>
      </div>
    `,
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -(size / 2) - 4],
  });
}

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

// Flies to the selected disruption
function FlyToSelected({
  disruptions,
  selectedId,
}: {
  disruptions: ActiveDisruption[];
  selectedId: number | null;
}) {
  const map = useMap();
  const prevId = useRef<number | null>(null);

  useEffect(() => {
    if (selectedId === null || selectedId === prevId.current) return;
    const d = disruptions.find((x) => x.id === selectedId);
    if (d?.latitude != null && d?.longitude != null) {
      map.flyTo([d.latitude, d.longitude], 15, { duration: 1.2 });
    }
    prevId.current = selectedId;
  }, [selectedId, disruptions, map]);

  return null;
}

interface Props {
  disruptions: ActiveDisruption[];
  selectedId?: number | null;
  onMarkerClick?: (id: number) => void;
  darkTiles?: boolean;
}

export const NetworkImpactMap = ({ disruptions, selectedId = null, onMarkerClick, darkTiles = false }: Props) => {
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
          attribution={
            darkTiles
              ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
              : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          }
          url={
            darkTiles
              ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
              : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          }
        />

        <FlyToSelected disruptions={mappable} selectedId={selectedId} />

        {mappable.map((d) => {
          const color = SEVERITY_COLORS[d.severity] ?? "#6B7280";
          const isSelected = d.id === selectedId;
          return (
            <Marker
              key={d.id}
              position={[d.latitude!, d.longitude!]}
              icon={makeDivIcon(d.severity, isSelected)}
              eventHandlers={onMarkerClick ? { click: () => onMarkerClick(d.id) } : undefined}
            >
              <Popup>
                <Box sx={{ minWidth: 180 }}>
                  <Typography sx={{ fontWeight: 700, fontSize: "0.85rem", mb: 0.5 }}>
                    {d.name}
                  </Typography>
                  <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap", mb: 0.5 }}>
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
                    <Typography sx={{ fontSize: "0.75rem", color: "#6B7280", mb: 0.5 }}>
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
                  <Typography sx={{ fontSize: "0.68rem", color: "#9CA3AF", mt: 0.5 }}>
                    Detected: {formatTime(d.detectedAt)}
                  </Typography>
                </Box>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>

      {/* Legend */}
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
        {(["LOW", "MEDIUM", "HIGH", "CRITICAL"] as DisruptionSeverity[]).map((s) => (
          <Box key={s} sx={{ display: "flex", alignItems: "center", gap: 0.6 }}>
            <Box
              sx={{
                width: 10,
                height: 10,
                borderRadius: "50%",
                bgcolor: SEVERITY_COLORS[s],
              }}
            />
            <Typography sx={{ fontSize: "0.62rem", color: "#374151" }}>{s}</Typography>
          </Box>
        ))}
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
