/**
 * PublicEventPage — No-auth public page for event attendees (scanned via QR code).
 * Shows venue on a map with nearby transport options grouped by mode.
 */

import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import {
  Box,
  CircularProgress,
  Divider,
  IconButton,
  Link,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import TramIcon from "@mui/icons-material/Tram";
import PedalBikeIcon from "@mui/icons-material/PedalBike";
import DirectionsWalkIcon from "@mui/icons-material/DirectionsWalk";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import PeopleIcon from "@mui/icons-material/People";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { dashboardApi } from "@/api";
import type { DisruptionAlternative } from "@/types";

// Fix Leaflet default icon paths in bundled apps
delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)
  ._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

const STOP_MODE_COLORS: Record<string, string> = {
  bus: "#3B82F6",
  tram: "#10B981",
  rail: "#8B5CF6",
  bike: "#F59E0B",
};

function makeVenueIcon(): L.DivIcon {
  return L.divIcon({
    className: "",
    html: `<div style="width:16px;height:16px;border-radius:50%;background:#EF4444;border:3px solid white;box-shadow:0 0 0 2px #EF4444;"></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  });
}

function makeStopIcon(mode: string): L.DivIcon {
  const color = STOP_MODE_COLORS[mode?.toLowerCase()] ?? "#6B7280";
  return L.divIcon({
    className: "",
    html: `<div style="width:12px;height:12px;border-radius:50%;background:${color};border:2px solid white;box-shadow:0 1px 4px rgba(0,0,0,0.35);"></div>`,
    iconSize: [12, 12],
    iconAnchor: [6, 6],
  });
}

const EVENT_TYPE_COLORS: Record<string, string> = {
  Music: "#7C3AED",
  Sports: "#059669",
  "Arts & Theatre": "#0891B2",
  Film: "#DC2626",
  Miscellaneous: "#D97706",
};

function eventColor(type: string): string {
  return EVENT_TYPE_COLORS[type] ?? "#6366F1";
}

const MODE_COLORS: Record<string, string> = {
  bus: "#3B82F6",
  rail: "#8B5CF6",
  tram: "#10B981",
  bike: "#F59E0B",
};

function altModeIcon(mode: string, size = 18): React.ReactNode {
  const m = mode.toLowerCase();
  if (m === "bus") return <DirectionsBusIcon sx={{ fontSize: size }} />;
  if (m === "tram") return <TramIcon sx={{ fontSize: size }} />;
  if (m === "rail") return <TrainIcon sx={{ fontSize: size }} />;
  return <PedalBikeIcon sx={{ fontSize: size }} />;
}

function modeLabel(mode: string): string {
  const m = mode.toLowerCase();
  if (m === "bus") return "Bus";
  if (m === "tram") return "Luas";
  if (m === "rail") return "Irish Rail";
  if (m === "bike") return "DublinBikes";
  return mode;
}

function fmtEventTime(iso: string | null): string {
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

function fmtEventDate(dateStr: string | null): string {
  if (!dateStr) return "—";
  try {
    return new Date(dateStr + "T12:00:00").toLocaleDateString("en-IE", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
}

function groupByMode(
  transport: DisruptionAlternative[],
): Record<string, DisruptionAlternative[]> {
  const groups: Record<string, DisruptionAlternative[]> = {};
  for (const t of transport) {
    const m = t.mode?.toLowerCase() ?? "other";
    if (!groups[m]) groups[m] = [];
    groups[m].push(t);
  }
  return groups;
}

const MODE_ORDER = ["bus", "tram", "rail", "bike"];

export const PublicEventPage = () => {
  const { id } = useParams<{ id: string }>();
  const eventId = Number(id);

  const {
    data: event,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["public-event", eventId],
    queryFn: () => dashboardApi.getPublicEvent(eventId),
    staleTime: 60_000,
    enabled: !isNaN(eventId),
  });

  if (isLoading) {
    return (
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (isError || !event) {
    return (
      <Box
        sx={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexDirection: "column",
          gap: 1,
        }}
      >
        <Typography variant="h6" color="text.secondary">
          Event not found
        </Typography>
        <Typography variant="body2" color="text.disabled">
          This event may have ended or the link is invalid.
        </Typography>
      </Box>
    );
  }

  const color = eventColor(event.eventType);
  const grouped = groupByMode(event.nearbyTransport ?? []);
  const orderedModes = [
    ...MODE_ORDER.filter((m) => grouped[m]),
    ...Object.keys(grouped).filter((m) => !MODE_ORDER.includes(m)),
  ];

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
      {/* Event colour banner header */}
      <Box
        sx={{
          bgcolor: color,
          px: { xs: 2, md: 4 },
          py: 3,
          color: "#fff",
        }}
      >
        <Typography
          variant="overline"
          sx={{ opacity: 0.8, fontSize: "0.7rem", letterSpacing: 1.5 }}
        >
          {event.eventType}
        </Typography>
        <Typography
          variant="h4"
          fontWeight={800}
          sx={{ lineHeight: 1.2, mt: 0.25 }}
        >
          {event.eventName}
        </Typography>
        <Stack direction="row" spacing={2} sx={{ mt: 1.25, flexWrap: "wrap" }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            <LocationOnIcon sx={{ fontSize: 16, opacity: 0.85 }} />
            <Typography sx={{ fontSize: "0.875rem", opacity: 0.9 }}>
              {event.venueName}
            </Typography>
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            <AccessTimeIcon sx={{ fontSize: 16, opacity: 0.85 }} />
            <Typography sx={{ fontSize: "0.875rem", opacity: 0.9 }}>
              {fmtEventDate(event.eventDate)}
              {event.startTime ? ` · ${fmtEventTime(event.startTime)}` : ""}
              {event.endTime ? ` – ${fmtEventTime(event.endTime)}` : ""}
            </Typography>
          </Box>
          {event.estimatedAttendance != null && (
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
              <PeopleIcon sx={{ fontSize: 16, opacity: 0.85 }} />
              <Typography sx={{ fontSize: "0.875rem", opacity: 0.9 }}>
                {event.estimatedAttendance.toLocaleString()} expected
              </Typography>
            </Box>
          )}
        </Stack>
        <Typography
          sx={{
            mt: 1.5,
            fontSize: "0.8rem",
            opacity: 0.75,
            fontStyle: "italic",
          }}
        >
          Getting there · Public transport options near this venue
        </Typography>
      </Box>

      {/* Two-column layout */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
          minHeight: "calc(100vh - 160px)",
        }}
      >
        {/* Left: map */}
        {event.latitude != null && event.longitude != null && (
          <Box sx={{ height: { xs: 300, md: "auto" }, minHeight: 300 }}>
            <MapContainer
              center={[event.latitude, event.longitude]}
              zoom={15}
              style={{ height: "100%", width: "100%" }}
            >
              <TileLayer
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                attribution="&copy; OpenStreetMap contributors"
              />
              {/* Venue marker — red dot */}
              <Marker
                position={[event.latitude, event.longitude]}
                icon={makeVenueIcon()}
              >
                <Popup>
                  <strong>{event.eventName}</strong>
                  <br />
                  {event.venueName}
                </Popup>
              </Marker>
              {/* Transport stop markers — color-coded by mode */}
              {(event.nearbyTransport ?? [])
                .filter((t) => t.lat != null && t.lon != null)
                .map((t, i) => (
                  <Marker
                    key={i}
                    position={[t.lat!, t.lon!]}
                    icon={makeStopIcon(t.mode ?? "")}
                  >
                    <Popup>
                      <strong>
                        {modeLabel(t.mode ?? "")} · {t.stopName}
                      </strong>
                      {t.description && (
                        <>
                          <br />
                          {t.description}
                        </>
                      )}
                      {t.availabilityCount != null && (
                        <>
                          <br />
                          {t.availabilityCount} bikes available
                        </>
                      )}
                      {t.googleMapsWalkingUrl && (
                        <>
                          <br />
                          <a
                            href={t.googleMapsWalkingUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                          >
                            Walk here ↗
                          </a>
                        </>
                      )}
                    </Popup>
                  </Marker>
                ))}
            </MapContainer>
          </Box>
        )}

        {/* Right: info panel */}
        <Box sx={{ p: { xs: 2, md: 3 }, overflow: "auto" }}>
          {/* Event info summary */}
          <Typography
            variant="subtitle2"
            fontWeight={700}
            sx={{
              mb: 1.5,
              color: "text.secondary",
              textTransform: "uppercase",
              fontSize: "0.7rem",
              letterSpacing: 0.8,
            }}
          >
            Event info
          </Typography>
          <Stack spacing={1} sx={{ mb: 3 }}>
            <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
              <LocationOnIcon sx={{ fontSize: 16, color: "text.disabled" }} />
              <Typography variant="body2">{event.venueName}</Typography>
            </Box>
            <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
              <AccessTimeIcon sx={{ fontSize: 16, color: "text.disabled" }} />
              <Typography variant="body2">
                {fmtEventDate(event.eventDate)}
                {event.startTime ? ` · ${fmtEventTime(event.startTime)}` : ""}
                {event.endTime ? ` – ${fmtEventTime(event.endTime)}` : ""}
              </Typography>
            </Box>
            {event.estimatedAttendance != null && (
              <Box sx={{ display: "flex", gap: 1, alignItems: "center" }}>
                <PeopleIcon sx={{ fontSize: 16, color: "text.disabled" }} />
                <Typography variant="body2">
                  {event.estimatedAttendance.toLocaleString()} expected
                  attendees
                </Typography>
              </Box>
            )}
          </Stack>

          <Divider sx={{ mb: 2.5 }} />

          {/* Nearby transport grouped by mode */}
          <Typography
            variant="subtitle2"
            fontWeight={700}
            sx={{
              mb: 1.5,
              color: "text.secondary",
              textTransform: "uppercase",
              fontSize: "0.7rem",
              letterSpacing: 0.8,
            }}
          >
            Nearby transport
          </Typography>

          {orderedModes.length === 0 ? (
            <Typography variant="body2" color="text.disabled">
              No transport options found within 500m of this venue.
            </Typography>
          ) : (
            <Stack spacing={2.5}>
              {orderedModes.map((mode) => {
                const mc = MODE_COLORS[mode] ?? "#6B7280";
                const stops = grouped[mode];
                return (
                  <Box key={mode}>
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                        mb: 1,
                      }}
                    >
                      <Box sx={{ color: mc, display: "flex" }}>
                        {altModeIcon(mode, 18)}
                      </Box>
                      <Typography
                        sx={{ fontWeight: 700, fontSize: "0.85rem", color: mc }}
                      >
                        {modeLabel(mode)}
                      </Typography>
                    </Box>
                    <Stack spacing={0.75}>
                      {stops.map((t, i) => (
                        <Box
                          key={i}
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                            p: 1,
                            borderRadius: 1.5,
                            bgcolor: `${mc}08`,
                            border: `1px solid ${mc}20`,
                          }}
                        >
                          <Box sx={{ flex: 1, minWidth: 0 }}>
                            <Typography
                              noWrap
                              sx={{ fontSize: "0.82rem", fontWeight: 550 }}
                            >
                              {t.stopName ?? t.description}
                            </Typography>
                            <Typography
                              sx={{
                                fontSize: "0.7rem",
                                color: "text.disabled",
                              }}
                              noWrap
                            >
                              {t.description}
                              {t.availabilityCount != null
                                ? ` · ${t.availabilityCount} available`
                                : ""}
                            </Typography>
                          </Box>
                          {t.googleMapsWalkingUrl && (
                            <Tooltip title="Walking directions">
                              <IconButton
                                component={Link}
                                href={t.googleMapsWalkingUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                size="small"
                                sx={{ color: mc, flexShrink: 0 }}
                              >
                                <DirectionsWalkIcon sx={{ fontSize: 16 }} />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Box>
                      ))}
                    </Stack>
                  </Box>
                );
              })}
            </Stack>
          )}

          {/* Live footer */}
          <Box
            sx={{
              mt: 3,
              pt: 2,
              borderTop: "1px solid",
              borderColor: "divider",
              display: "flex",
              alignItems: "center",
              gap: 1,
            }}
          >
            <FiberManualRecordIcon
              sx={{
                fontSize: 8,
                color: "#10B981",
                animation: "pulse 1.5s ease-in-out infinite",
                "@keyframes pulse": {
                  "0%, 100%": { opacity: 1 },
                  "50%": { opacity: 0.3 },
                },
              }}
            />
            <Typography variant="caption" color="text.secondary">
              Transport information is live · SmartEnoughCity
            </Typography>
          </Box>
        </Box>
      </Box>
    </Box>
  );
};
