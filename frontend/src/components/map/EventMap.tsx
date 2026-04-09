/**
 * EventMap — Leaflet map for the Events tab on the disruption dashboard.
 * Shows upcoming city events as venue markers coloured by risk level (derived
 * from venue capacity).  Clicking a marker fires onEventClick so the panel
 * can show detail + nearby transport alternatives.
 */

import { useEffect, useRef } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import { Box, Typography, Chip } from "@mui/material";
import type { EventItem } from "@/types";
import "leaflet/dist/leaflet.css";

const DUBLIN_CENTER: [number, number] = [53.3498, -6.2603];

export interface SelectedMapItem {
  kind: "event" | "disruption" | "pedestrian";
  item: EventItem;
}

const RISK_COLORS: Record<string, string> = {
  CRITICAL: "#7C3AED",
  HIGH: "#EF4444",
  MEDIUM: "#F59E0B",
  LOW: "#10B981",
};

const RISK_SIZE: Record<string, number> = {
  CRITICAL: 30,
  HIGH: 26,
  MEDIUM: 22,
  LOW: 18,
};

const STYLE_ID = "event-marker-styles";
function ensureStyles() {
  if (document.querySelector(`#${STYLE_ID}`)) return;
  const style = document.createElement("style");
  style.id = STYLE_ID;
  style.textContent = `
    @keyframes event-pulse {
      0%   { transform: scale(1);   opacity: 0.8; }
      70%  { transform: scale(2.2); opacity: 0;   }
      100% { transform: scale(2.2); opacity: 0;   }
    }
    .event-marker-ring {
      position: absolute;
      inset: 0;
      border-radius: 50%;
      animation: event-pulse 2.4s ease-out infinite;
    }
  `;
  document.head.append(style);
}

function makeEventIcon(riskLevel: string, selected: boolean): L.DivIcon {
  ensureStyles();
  const color = RISK_COLORS[riskLevel] ?? "#6366F1";
  const size = RISK_SIZE[riskLevel] ?? 22;

  // Star-shaped inner icon to distinguish from disruption dots
  const starPath =
    "M12 2l2.9 6.26L22 9.27l-5 5.14 1.18 7.09L12 18.77l-6.18 2.73L7 14.41 2 9.27l7.1-1.01L12 2z";

  const ring = selected
    ? `<span class="event-marker-ring" style="border: 3px solid ${color};"></span>`
    : `<span class="event-marker-ring" style="border: 2px solid ${color}88;"></span>`;

  return L.divIcon({
    className: "",
    html: `
      <div style="position:relative;width:${size}px;height:${size}px;">
        ${ring}
        <div style="
          position:absolute;inset:0;border-radius:50%;
          background:${color};opacity:${selected ? 1 : 0.8};
          border:2px solid ${selected ? "#fff" : color + "cc"};
          box-shadow:0 0 ${selected ? 10 : 6}px ${color}99;
          display:flex;align-items:center;justify-content:center;
        ">
          <svg width="${size * 0.55}" height="${size * 0.55}" viewBox="0 0 24 24" fill="white">
            <path d="${starPath}"/>
          </svg>
        </div>
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

function FlyToSelected({
  events,
  selectedItem,
}: {
  events: EventItem[];
  selectedItem: SelectedMapItem | null;
}) {
  const map = useMap();
  const prevId = useRef<number | null>(null);

  useEffect(() => {
    if (!selectedItem || selectedItem.kind !== "event") return;
    const e = selectedItem.item;
    if (e.id === prevId.current) return;
    if (e.latitude != null && e.longitude != null) {
      map.flyTo([e.latitude, e.longitude], 15, { duration: 1.1 });
    }
    prevId.current = e.id;
  }, [selectedItem, events, map]);

  return null;
}

interface EventMapProps {
  events: EventItem[];
  disruptions: unknown[];
  pedestrians: unknown[];
  selectedTypes: Set<string>;
  selectedSeverities: Set<string>;
  selectedItem: SelectedMapItem | null;
  onEventClick: (event: EventItem) => void;
  onDisruptionClick: () => void;
  onPedestrianClick: () => void;
}

export const EventMap = ({
  events,
  selectedItem,
  onEventClick,
}: EventMapProps) => {
  const mappable = events.filter(
    (e) => e.latitude != null && e.longitude != null,
  );

  const selectedEventId =
    selectedItem?.kind === "event" ? selectedItem.item.id : null;

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

        <FlyToSelected events={mappable} selectedItem={selectedItem} />

        {mappable.map((e) => {
          const color = RISK_COLORS[e.riskLevel] ?? "#6366F1";
          const isSelected = e.id === selectedEventId;
          return (
            <Marker
              key={e.id}
              position={[e.latitude!, e.longitude!]}
              icon={makeEventIcon(e.riskLevel, isSelected)}
              eventHandlers={{ click: () => onEventClick(e) }}
            >
              <Popup>
                <Box sx={{ minWidth: 190 }}>
                  <Typography
                    sx={{ fontWeight: 700, fontSize: "0.85rem", mb: 0.5 }}
                  >
                    {e.eventName}
                  </Typography>
                  <Box sx={{ display: "flex", gap: 0.5, mb: 0.5 }}>
                    <Chip
                      size="small"
                      label={e.riskLevel}
                      sx={{
                        fontSize: "0.6rem",
                        height: 16,
                        bgcolor: color + "22",
                        color,
                        border: `1px solid ${color}44`,
                      }}
                    />
                    {e.eventType && (
                      <Chip
                        size="small"
                        label={e.eventType}
                        sx={{ fontSize: "0.6rem", height: 16 }}
                      />
                    )}
                  </Box>
                  <Typography sx={{ fontSize: "0.75rem", color: "#6B7280" }}>
                    {e.venueName}
                  </Typography>
                  {e.venueCapacity != null && (
                    <Typography sx={{ fontSize: "0.72rem" }}>
                      Capacity: {e.venueCapacity.toLocaleString()}
                    </Typography>
                  )}
                  <Typography sx={{ fontSize: "0.72rem" }}>
                    {formatTime(e.startTime)}
                    {e.endTime ? ` – ${formatTime(e.endTime)}` : ""}
                  </Typography>
                </Box>
              </Popup>
            </Marker>
          );
        })}
      </MapContainer>

      {/* Risk legend */}
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
        <Typography
          sx={{ fontSize: "0.58rem", fontWeight: 700, color: "#374151", mb: 0.2 }}
        >
          VENUE RISK
        </Typography>
        {(["CRITICAL", "HIGH", "MEDIUM", "LOW"] as const).map((r) => (
          <Box key={r} sx={{ display: "flex", alignItems: "center", gap: 0.6 }}>
            <Box
              sx={{
                width: 10,
                height: 10,
                borderRadius: "50%",
                bgcolor: RISK_COLORS[r],
              }}
            />
            <Typography sx={{ fontSize: "0.62rem", color: "#374151" }}>
              {r}
            </Typography>
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
              No geo-located events today
            </Typography>
          </Box>
        </Box>
      )}
    </Box>
  );
};
