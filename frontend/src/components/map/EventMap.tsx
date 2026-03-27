/**
 * Event map — displays events on an interactive Leaflet map
 * Icons: 🚧 construction · 🎉 public · 🚨 emergency
 */

import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import { Box } from "@mui/material";
import "leaflet/dist/leaflet.css";
import type { EventItem } from "@/types";

export type EventCategory = "construction" | "public" | "emergency";
export type EventSeverity = "high" | "medium" | "low";

const DUBLIN_CENTER: [number, number] = [53.3498, -6.2603];

export const CATEGORY_EMOJI: Record<EventCategory, string> = {
  construction: "🚧",
  public: "🎉",
  emergency: "🚨",
};

export const CATEGORY_LABEL: Record<EventCategory, string> = {
  construction: "Construction",
  public: "Public",
  emergency: "Emergency",
};

export const SEVERITY_COLORS: Record<EventSeverity, string> = {
  high: "#EF4444",
  medium: "#F59E0B",
  low: "#10B981",
};

export function getEventCategory(eventType: string): EventCategory {
  const t = eventType.toLowerCase();
  if (t.includes("construction") || t.includes("roadwork")) return "construction";
  if (
    t.includes("emergency") ||
    t.includes("incident") ||
    t.includes("alert")
  )
    return "emergency";
  return "public";
}

export function getEventSeverity(event: EventItem): EventSeverity {
  const att = event.estimatedAttendance ?? 0;
  if (att > 5000) return "high";
  if (att > 1000) return "medium";
  return "low";
}

function createEventIcon(
  category: EventCategory,
  selected: boolean,
): L.DivIcon {
  const emoji = CATEGORY_EMOJI[category];
  const scale = selected ? "1.4" : "1";
  return L.divIcon({
    html: `<div style="font-size:24px;line-height:1;transform:scale(${scale});transform-origin:center;transition:transform 0.15s;filter:drop-shadow(0 1px 3px rgba(0,0,0,0.45));">${emoji}</div>`,
    className: "",
    iconSize: [28, 28],
    iconAnchor: [14, 14],
  });
}

const FitBounds = ({ events }: { events: EventItem[] }) => {
  const map = useMap();
  useEffect(() => {
    const valid = events.filter((e) => e.latitude && e.longitude);
    if (valid.length > 1) {
      const bounds = L.latLngBounds(
        valid.map((e) => [e.latitude, e.longitude]),
      );
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 14 });
    } else if (valid.length === 1) {
      map.setView([valid[0].latitude, valid[0].longitude], 14);
    }
  }, [events, map]);
  return null;
};

interface EventMapProps {
  events: EventItem[];
  selectedTypes: Set<EventCategory>;
  selectedSeverities: Set<EventSeverity>;
  selectedEventId: number | null;
  onEventClick: (event: EventItem) => void;
}

export const EventMap = ({
  events,
  selectedTypes,
  selectedSeverities,
  selectedEventId,
  onEventClick,
}: EventMapProps) => {
  const filtered = events.filter((e) => {
    if (!e.latitude || !e.longitude) return false;
    return (
      selectedTypes.has(getEventCategory(e.eventType)) &&
      selectedSeverities.has(getEventSeverity(e))
    );
  });

  return (
    <Box sx={{ height: "100%", width: "100%", position: "relative" }}>
      <MapContainer
        center={DUBLIN_CENTER}
        zoom={13}
        style={{ height: "100%", width: "100%" }}
        zoomControl={true}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitBounds events={filtered} />
        {filtered.map((event) => (
          <Marker
            key={event.id}
            position={[event.latitude, event.longitude]}
            icon={createEventIcon(
              getEventCategory(event.eventType),
              selectedEventId === event.id,
            )}
            opacity={
              selectedEventId !== null && selectedEventId !== event.id
                ? 0.45
                : 1
            }
            eventHandlers={{ click: () => onEventClick(event) }}
          />
        ))}
      </MapContainer>
    </Box>
  );
};
