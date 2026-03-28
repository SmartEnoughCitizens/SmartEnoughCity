/**
 * Event map — displays events and disruptions on an interactive Leaflet map
 * Icons: 🚧 construction · 🎉 public · 🚨 emergency
 */

import { useEffect } from "react";
import { MapContainer, TileLayer, Marker, useMap } from "react-leaflet";
import L from "leaflet";
import { Box } from "@mui/material";
import "leaflet/dist/leaflet.css";
import type { DisruptionItem, EventItem } from "@/types";
import {
  type EventCategory,
  type EventSeverity,
  type SelectedMapItem,
  CATEGORY_EMOJI,
  getDisruptionCategory,
  getDisruptionSeverity,
  getEventCategory,
  getEventSeverity,
} from "./eventMapUtils";

export type {
  EventCategory,
  EventSeverity,
  SelectedMapItem,
} from "./eventMapUtils";

const DUBLIN_CENTER: [number, number] = [53.3498, -6.2603];

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

// Fit bounds across all visible points (events + disruptions combined)
const FitBounds = ({ points }: { points: Array<[number, number]> }) => {
  const map = useMap();
  useEffect(() => {
    if (points.length > 1) {
      map.fitBounds(L.latLngBounds(points), { padding: [40, 40], maxZoom: 14 });
    } else if (points.length === 1) {
      map.setView(points[0], 14);
    }
  }, [points, map]);
  return null;
};

interface EventMapProps {
  events: EventItem[];
  disruptions: DisruptionItem[];
  selectedTypes: Set<EventCategory>;
  selectedSeverities: Set<EventSeverity>;
  selectedItem: SelectedMapItem | null;
  onEventClick: (event: EventItem) => void;
  onDisruptionClick: (disruption: DisruptionItem) => void;
}

export const EventMap = ({
  events,
  disruptions,
  selectedTypes,
  selectedSeverities,
  selectedItem,
  onEventClick,
  onDisruptionClick,
}: EventMapProps) => {
  const filteredEvents = events.filter((e) => {
    if (!e.latitude || !e.longitude) return false;
    return (
      selectedTypes.has(getEventCategory(e.eventType)) &&
      selectedSeverities.has(getEventSeverity(e))
    );
  });

  const filteredDisruptions = disruptions.filter((d) => {
    if (!d.latitude || !d.longitude) return false;
    return (
      selectedTypes.has(getDisruptionCategory(d.disruptionType)) &&
      selectedSeverities.has(getDisruptionSeverity(d.severity))
    );
  });

  const allPoints: Array<[number, number]> = [
    ...filteredEvents.map((e) => [e.latitude, e.longitude] as [number, number]),
    ...filteredDisruptions.map(
      (d) => [d.latitude!, d.longitude!] as [number, number],
    ),
  ];

  const selectedId =
    selectedItem?.kind === "event"
      ? `event-${selectedItem.item.id}`
      : selectedItem?.kind === "disruption"
        ? `disruption-${selectedItem.item.id}`
        : null;

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
        <FitBounds points={allPoints} />

        {filteredEvents.map((event) => {
          const key = `event-${event.id}`;
          return (
            <Marker
              key={key}
              position={[event.latitude, event.longitude]}
              icon={createEventIcon(
                getEventCategory(event.eventType),
                selectedId === key,
              )}
              opacity={selectedId !== null && selectedId !== key ? 0.45 : 1}
              eventHandlers={{ click: () => onEventClick(event) }}
            />
          );
        })}

        {filteredDisruptions.map((disruption) => {
          const key = `disruption-${disruption.id}`;
          return (
            <Marker
              key={key}
              position={[disruption.latitude!, disruption.longitude!]}
              icon={createEventIcon(
                getDisruptionCategory(disruption.disruptionType),
                selectedId === key,
              )}
              opacity={selectedId !== null && selectedId !== key ? 0.45 : 1}
              eventHandlers={{ click: () => onDisruptionClick(disruption) }}
            />
          );
        })}
      </MapContainer>
    </Box>
  );
};
