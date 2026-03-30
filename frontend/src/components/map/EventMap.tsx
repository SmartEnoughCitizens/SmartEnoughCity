/**
 * Event map — displays events and disruptions on an interactive Leaflet map
 * Icons: 🚧 construction · 🎉 public · 🚨 emergency
 */

import { useEffect, useMemo, useRef, useState } from "react";
import {
  MapContainer,
  TileLayer,
  Marker,
  useMap,
  useMapEvents,
} from "react-leaflet";
import L from "leaflet";
import { Box } from "@mui/material";
import "leaflet/dist/leaflet.css";
import type { DisruptionItem, EventItem, PedestrianLive } from "@/types";
import {
  type EventCategory,
  type EventSeverity,
  type SelectedMapItem,
  CATEGORY_EMOJI,
  getDisruptionCategory,
  getDisruptionSeverity,
  getEventCategory,
  getEventSeverity,
  getPedestrianSeverity,
  SEVERITY_COLORS,
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

function createPedestrianIcon(
  pedestrian: PedestrianLive,
  selected: boolean,
): L.DivIcon {
  const severity = getPedestrianSeverity(pedestrian);
  const color = SEVERITY_COLORS[severity];
  const scale = selected ? 1.2 : 1;
  const ringOpacity = selected ? 0.68 : 0.54;
  const fillOpacity = selected ? 0.98 : 0.94;
  const label = pedestrian.totalCount > 999 ? "999+" : pedestrian.totalCount;

  return L.divIcon({
    html: `
      <div style="position:relative;width:34px;height:34px;transform:scale(${scale});transform-origin:center;transition:transform 0.15s ease;">
        <div style="position:absolute;inset:0;border-radius:999px;background:${color};opacity:${ringOpacity};"></div>
        <div style="position:absolute;left:4px;top:4px;width:26px;height:26px;border-radius:999px;background:${color};opacity:${fillOpacity};border:1.5px solid rgba(255,255,255,0.94);box-shadow:0 8px 18px rgba(8,145,178,0.22);display:flex;align-items:center;justify-content:center;color:#ffffff;font-size:10px;font-weight:800;line-height:1;text-shadow:0 1px 2px rgba(0,0,0,0.28);">
          ${label}
        </div>
      </div>
    `,
    className: "",
    iconSize: [34, 34],
    iconAnchor: [17, 17],
  });
}

function createPedestrianClusterIcon(
  count: number,
  maxCount: number,
  selected: boolean,
): L.DivIcon {
  const size = count >= 10 ? 40 : count >= 5 ? 34 : 30;
  const severityColor =
    SEVERITY_COLORS[
      maxCount >= 140 ? "high" : maxCount >= 70 ? "medium" : "low"
    ];

  return L.divIcon({
    html: `
      <div style="width:${size}px;height:${size}px;border-radius:999px;background:${severityColor};opacity:${selected ? 0.66 : 0.54};border:1px solid rgba(255,255,255,0.76);backdrop-filter:blur(6px);display:flex;align-items:center;justify-content:center;box-shadow:0 10px 24px rgba(15,23,42,0.16);">
        <div style="width:${size - 10}px;height:${size - 10}px;border-radius:999px;background:rgba(255,255,255,0.97);display:flex;flex-direction:column;align-items:center;justify-content:center;color:#0f172a;line-height:1;">
          <span style="font-size:12px;font-weight:800;">${count}</span>
          <span style="font-size:8px;font-weight:700;opacity:0.66;margin-top:2px;">max ${maxCount}</span>
        </div>
      </div>
    `,
    className: "",
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
  });
}

// Fit bounds across all visible points (events + disruptions combined)
const FitBounds = ({ points }: { points: Array<[number, number]> }) => {
  const map = useMap();
  const lastSignatureRef = useRef<string | null>(null);

  useEffect(() => {
    const signature = JSON.stringify(points);
    if (lastSignatureRef.current === signature) return;
    lastSignatureRef.current = signature;

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
  pedestrians: PedestrianLive[];
  selectedTypes: Set<EventCategory>;
  selectedSeverities: Set<EventSeverity>;
  selectedItem: SelectedMapItem | null;
  onEventClick: (event: EventItem) => void;
  onDisruptionClick: (disruption: DisruptionItem) => void;
  onPedestrianClick: (pedestrian: PedestrianLive) => void;
}

type PedestrianCluster =
  | {
      kind: "single";
      site: PedestrianLive;
      position: [number, number];
    }
  | {
      kind: "cluster";
      sites: PedestrianLive[];
      position: [number, number];
      maxCount: number;
    };

function PedestrianMarkers({
  pedestrians,
  selectedId,
  onPedestrianClick,
}: {
  pedestrians: PedestrianLive[];
  selectedId: string | null;
  onPedestrianClick: (pedestrian: PedestrianLive) => void;
}) {
  const map = useMap();
  const [zoom, setZoom] = useState(() => map.getZoom());

  useMapEvents({
    zoomend: () => setZoom(map.getZoom()),
  });

  const clusters = useMemo<PedestrianCluster[]>(() => {
    const gridSize =
      zoom >= 16 ? 1 : zoom >= 15 ? 28 : zoom >= 14 ? 36 : zoom >= 13 ? 46 : 58;
    if (gridSize === 1) {
      return pedestrians.map((site) => ({
        kind: "single",
        site,
        position: [site.lat, site.lon],
      }));
    }

    const buckets = new Map<string, PedestrianLive[]>();
    for (const site of pedestrians) {
      const point = map.project(L.latLng(site.lat, site.lon), zoom);
      const key = `${Math.floor(point.x / gridSize)}:${Math.floor(point.y / gridSize)}`;
      const sites = buckets.get(key);
      if (sites) sites.push(site);
      else buckets.set(key, [site]);
    }

    return [...buckets.values()].map((sites) => {
      if (sites.length === 1) {
        return {
          kind: "single",
          site: sites[0],
          position: [sites[0].lat, sites[0].lon] as [number, number],
        };
      }

      const lat =
        sites.reduce((sum, site) => sum + site.lat, 0) /
        Math.max(sites.length, 1);
      const lon =
        sites.reduce((sum, site) => sum + site.lon, 0) /
        Math.max(sites.length, 1);

      return {
        kind: "cluster",
        sites,
        position: [lat, lon] as [number, number],
        maxCount: Math.max(...sites.map((site) => site.totalCount)),
      };
    });
  }, [map, pedestrians, zoom]);

  return (
    <>
      {clusters.map((cluster, index) => {
        if (cluster.kind === "single") {
          const key = `pedestrian-${cluster.site.siteId}`;
          return (
            <Marker
              key={key}
              position={cluster.position}
              icon={createPedestrianIcon(cluster.site, selectedId === key)}
              opacity={selectedId !== null && selectedId !== key ? 0.78 : 0.99}
              zIndexOffset={selectedId === key ? 450 : 260}
              eventHandlers={{
                click: () => onPedestrianClick(cluster.site),
              }}
            />
          );
        }

        const key = `pedestrian-cluster-${index}`;
        return (
          <Marker
            key={key}
            position={cluster.position}
            icon={createPedestrianClusterIcon(
              cluster.sites.length,
              cluster.maxCount,
              false,
            )}
            opacity={selectedId?.startsWith("pedestrian-") ? 0.9 : 0.99}
            zIndexOffset={180}
            eventHandlers={{
              click: () => {
                const bounds = L.latLngBounds(
                  cluster.sites.map(
                    (site) => [site.lat, site.lon] as [number, number],
                  ),
                );
                map.fitBounds(bounds, {
                  padding: [48, 48],
                  maxZoom: Math.max(zoom + 2, 16),
                });
              },
            }}
          />
        );
      })}
    </>
  );
}

export const EventMap = ({
  events,
  disruptions,
  pedestrians,
  selectedTypes,
  selectedSeverities,
  selectedItem,
  onEventClick,
  onDisruptionClick,
  onPedestrianClick,
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

  const allPoints = useMemo<Array<[number, number]>>(
    () => [
      ...filteredEvents.map(
        (e) => [e.latitude, e.longitude] as [number, number],
      ),
      ...filteredDisruptions.map(
        (d) => [d.latitude!, d.longitude!] as [number, number],
      ),
      ...pedestrians
        .filter((p) => Number.isFinite(p.lat) && Number.isFinite(p.lon))
        .map((p) => [p.lat, p.lon] as [number, number]),
    ],
    [filteredDisruptions, filteredEvents, pedestrians],
  );

  const selectedId =
    selectedItem?.kind === "event"
      ? `event-${selectedItem.item.id}`
      : selectedItem?.kind === "disruption"
        ? `disruption-${selectedItem.item.id}`
        : selectedItem?.kind === "pedestrian"
          ? `pedestrian-${selectedItem.item.siteId}`
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
              opacity={selectedId !== null && selectedId !== key ? 0.78 : 1}
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
              opacity={selectedId !== null && selectedId !== key ? 0.78 : 1}
              eventHandlers={{ click: () => onDisruptionClick(disruption) }}
            />
          );
        })}

        <PedestrianMarkers
          pedestrians={pedestrians.filter(
            (site) => Number.isFinite(site.lat) && Number.isFinite(site.lon),
          )}
          selectedId={selectedId}
          onPedestrianClick={onPedestrianClick}
        />
      </MapContainer>
    </Box>
  );
};
