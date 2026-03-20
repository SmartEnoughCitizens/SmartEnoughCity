/**
 * Misc dashboard — upcoming events + live pedestrian counts
 *
 * Layout:
 *  - Two glass-panel columns side-by-side (events left, pedestrians right)
 *  - Events: 5 visible rows, scrollable to all 10
 *  - Pedestrians: live count per site, refreshed every 30 s
 */

import { useState } from "react";
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Divider,
  ListItemButton,
  Paper,
  Tooltip,
  Typography,
} from "@mui/material";
import EventIcon from "@mui/icons-material/Event";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import PeopleIcon from "@mui/icons-material/People";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import DirectionsWalkIcon from "@mui/icons-material/DirectionsWalk";
import { useEvents, usePedestriansLive } from "@/hooks";
import type { EventItem, PedestrianLive } from "@/types";

// ── Constants ────────────────────────────────────────────────────────

const EVENT_VISIBLE = 5;
const EVENT_LIMIT = 10;

const EVENT_TYPE_COLORS: Record<string, string> = {
  Music: "#7C3AED",
  Sports: "#059669",
  "Arts & Theatre": "#0891B2",
  Film: "#DC2626",
  Miscellaneous: "#D97706",
};

function eventColor(type: string): string {
  return EVENT_TYPE_COLORS[type] ?? "#6B7280";
}

function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString("en-IE", {
      weekday: "short",
      day: "numeric",
      month: "short",
    });
  } catch {
    return dateStr;
  }
}

function formatTime(dtStr: string | null): string {
  if (!dtStr) return "—";
  try {
    return new Date(dtStr).toLocaleTimeString("en-IE", {
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return dtStr;
  }
}

function formatLastUpdated(dtStr: string | null): string {
  if (!dtStr) return "—";
  try {
    return new Date(dtStr).toLocaleTimeString("en-IE", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  } catch {
    return dtStr;
  }
}

// ── Sub-components ────────────────────────────────────────────────────

function SectionHeader({
  icon,
  title,
  subtitle,
  accent,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  accent: string;
}) {
  return (
    <Box
      sx={{
        px: 2,
        py: 1.75,
        display: "flex",
        alignItems: "center",
        gap: 1.25,
        borderBottom: "1px solid rgba(0,0,0,0.08)",
        flexShrink: 0,
      }}
    >
      <Box
        sx={{
          width: 34,
          height: 34,
          borderRadius: "50%",
          background: `linear-gradient(135deg, ${accent} 0%, ${accent}bb 100%)`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          boxShadow: `0 2px 8px ${accent}44`,
          flexShrink: 0,
        }}
      >
        {icon}
      </Box>
      <Box>
        <Typography
          variant="subtitle1"
          fontWeight={700}
          sx={{ color: "text.primary", lineHeight: 1.2, letterSpacing: -0.2 }}
        >
          {title}
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
          <FiberManualRecordIcon sx={{ fontSize: 7, color: "#2ea043" }} />
          <Typography
            variant="caption"
            sx={{ color: "#2ea043", fontSize: "0.62rem", letterSpacing: 0.4 }}
          >
            {subtitle}
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}

function EventRow({ event, selected, onClick }: { event: EventItem; selected: boolean; onClick: () => void }) {
  const color = eventColor(event.eventType);
  return (
    <ListItemButton
      onClick={onClick}
      sx={{
        py: 1,
        px: 2,
        bgcolor: selected ? "rgba(124,58,237,0.10)" : "transparent",
        borderLeft: selected ? "3px solid #7C3AED" : "3px solid transparent",
        "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
        transition: "all 0.12s",
        alignItems: "flex-start",
      }}
    >
      {/* Type dot */}
      <Box
        sx={{
          width: 22,
          height: 22,
          borderRadius: "50%",
          bgcolor: color,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          flexShrink: 0,
          mr: 1.5,
          mt: 0.2,
        }}
      >
        <EventIcon sx={{ fontSize: 12, color: "#fff" }} />
      </Box>

      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography
          noWrap
          sx={{
            fontSize: "0.855rem",
            fontWeight: selected ? 600 : 400,
            color: selected ? "#e6edf3" : "#c9d1d9",
            lineHeight: 1.25,
          }}
        >
          {event.eventName}
        </Typography>

        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 0.3, flexWrap: "wrap" }}>
          <Chip
            size="small"
            label={event.eventType}
            sx={{
              fontSize: "0.6rem",
              height: 16,
              bgcolor: color + "22",
              color,
              border: `1px solid ${color}44`,
            }}
          />
          <Typography sx={{ fontSize: "0.68rem", color: "#8b949e" }}>
            {formatDate(event.eventDate)} · {formatTime(event.startTime)}
          </Typography>
        </Box>

        <Box sx={{ display: "flex", alignItems: "center", gap: 0.4, mt: 0.2 }}>
          <LocationOnIcon sx={{ fontSize: 11, color: "#484f58" }} />
          <Typography noWrap sx={{ fontSize: "0.68rem", color: "#484f58" }}>
            {event.venueName}
          </Typography>
          {event.estimatedAttendance != null && (
            <>
              <PeopleIcon sx={{ fontSize: 11, color: "#484f58", ml: 0.5 }} />
              <Typography sx={{ fontSize: "0.68rem", color: "#484f58" }}>
                {event.estimatedAttendance.toLocaleString()}
              </Typography>
            </>
          )}
        </Box>
      </Box>
    </ListItemButton>
  );
}

function PedestrianRow({ site, rank }: { site: PedestrianLive; rank: number }) {
  const intensity = Math.min(site.totalCount / 200, 1); // normalise for colour
  const color = intensity > 0.7 ? "#EF4444" : intensity > 0.4 ? "#F59E0B" : "#10B981";

  return (
    <Box
      sx={{
        py: 0.875,
        px: 2,
        display: "flex",
        alignItems: "center",
        gap: 1.5,
        borderBottom: "1px solid rgba(48,54,61,0.25)",
      }}
    >
      {/* Rank */}
      <Typography
        sx={{
          fontSize: "0.7rem",
          fontWeight: 700,
          color: "#484f58",
          width: 18,
          flexShrink: 0,
          textAlign: "right",
        }}
      >
        {rank}
      </Typography>

      {/* Count bar */}
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Box sx={{ display: "flex", alignItems: "center", justifyContent: "space-between", mb: 0.3 }}>
          <Typography
            noWrap
            sx={{ fontSize: "0.835rem", color: "#c9d1d9", fontWeight: 500, lineHeight: 1.2 }}
          >
            {site.siteName}
          </Typography>
          <Typography sx={{ fontSize: "0.8rem", fontWeight: 700, color, ml: 1, flexShrink: 0 }}>
            {site.totalCount.toLocaleString()}
          </Typography>
        </Box>
        {/* Mini progress bar */}
        <Box
          sx={{
            height: 3,
            borderRadius: 2,
            bgcolor: "rgba(255,255,255,0.06)",
            overflow: "hidden",
          }}
        >
          <Box
            sx={{
              height: "100%",
              width: `${Math.max(intensity * 100, 2)}%`,
              bgcolor: color,
              borderRadius: 2,
              transition: "width 0.4s ease",
            }}
          />
        </Box>
        {site.lastUpdated && (
          <Typography sx={{ fontSize: "0.62rem", color: "#484f58", mt: 0.2 }}>
            <AccessTimeIcon sx={{ fontSize: 9, mr: 0.3, verticalAlign: "middle" }} />
            {formatLastUpdated(site.lastUpdated)}
          </Typography>
        )}
      </Box>
    </Box>
  );
}

// ── Main Component ────────────────────────────────────────────────────

export const MiscDashboard = () => {
  const [showAllEvents, setShowAllEvents] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);

  const { data: events = [], isLoading: eventsLoading, error: eventsError } = useEvents(EVENT_LIMIT);
  const {
    data: pedestrians = [],
    isLoading: pedLoading,
    error: pedError,
  } = usePedestriansLive(20);

  const visibleEvents = showAllEvents ? events : events.slice(0, EVENT_VISIBLE);

  return (
    <Box
      sx={{
        height: "100%",
        width: "100%",
        bgcolor: "background.default",
        display: "flex",
        flexDirection: "column",
        p: 2,
        gap: 2,
        overflow: "hidden",
      }}
    >
      {/* Page title */}
      <Box sx={{ flexShrink: 0 }}>
        <Typography
          variant="h6"
          fontWeight={700}
          sx={{ color: "text.primary", letterSpacing: -0.3 }}
        >
          Misc
        </Typography>
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          Upcoming events · Live pedestrian counters
        </Typography>
      </Box>

      {/* Two-column layout */}
      <Box
        sx={{
          flex: 1,
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
          gap: 2,
          overflow: "hidden",
        }}
      >
        {/* ── Events panel ── */}
        <Paper
          elevation={0}
          sx={{
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            background: "background.paper",
            border: "1px solid rgba(0,0,0,0.08)",
            borderRadius: 2,
          }}
        >
          <SectionHeader
            icon={<EventIcon sx={{ fontSize: 18, color: "#fff" }} />}
            title="Upcoming Events"
            subtitle={`${events.length} event${events.length !== 1 ? "s" : ""} · Dublin`}
            accent="#7C3AED"
          />

          {eventsError && (
            <Alert severity="error" sx={{ m: 1.5 }}>
              Failed to load events
            </Alert>
          )}

          {eventsLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
              <CircularProgress size={24} sx={{ color: "#7C3AED" }} />
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {visibleEvents.length === 0 ? (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <EventIcon sx={{ fontSize: 32, color: "#30363d", mb: 1 }} />
                  <Typography sx={{ fontSize: "0.8rem", color: "#484f58" }}>
                    No upcoming events
                  </Typography>
                </Box>
              ) : (
                visibleEvents.map((ev) => (
                  <EventRow
                    key={ev.id}
                    event={ev}
                    selected={selectedEventId === ev.id}
                    onClick={() =>
                      setSelectedEventId(selectedEventId === ev.id ? null : ev.id)
                    }
                  />
                ))
              )}

              {/* Show more / less toggle */}
              {events.length > EVENT_VISIBLE && (
                <Box
                  onClick={() => setShowAllEvents((v) => !v)}
                  sx={{
                    px: 2,
                    py: 1,
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    gap: 0.5,
                    borderTop: "1px solid rgba(48,54,61,0.4)",
                    "&:hover": { bgcolor: "rgba(255,255,255,0.03)" },
                  }}
                >
                  <Typography sx={{ fontSize: "0.75rem", color: "#7C3AED" }}>
                    {showAllEvents
                      ? "Show fewer"
                      : `Show all ${events.length} events`}
                  </Typography>
                </Box>
              )}
            </Box>
          )}

          <Box sx={{ px: 2, py: 1, borderTop: "1px solid rgba(48,54,61,0.5)", flexShrink: 0 }}>
            <Typography variant="caption" sx={{ color: "#30363d", fontSize: "0.62rem" }}>
              Source: Ticketmaster · Refreshed every 5 min
            </Typography>
          </Box>
        </Paper>

        {/* ── Pedestrians panel ── */}
        <Paper
          elevation={0}
          sx={{
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            background: "background.paper",
            border: "1px solid rgba(0,0,0,0.08)",
            borderRadius: 2,
          }}
        >
          <SectionHeader
            icon={<DirectionsWalkIcon sx={{ fontSize: 18, color: "#fff" }} />}
            title="Live Pedestrian Counts"
            subtitle="Per-site · Latest measurement"
            accent="#0891B2"
          />

          {/* Legend */}
          <Box sx={{ px: 2, py: 0.75, display: "flex", gap: 1.5, flexShrink: 0 }}>
            {[
              { color: "#10B981", label: "Low" },
              { color: "#F59E0B", label: "Medium" },
              { color: "#EF4444", label: "High" },
            ].map(({ color, label }) => (
              <Box key={label} sx={{ display: "flex", alignItems: "center", gap: 0.4 }}>
                <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: color }} />
                <Typography sx={{ fontSize: "0.62rem", color: "#8b949e" }}>{label}</Typography>
              </Box>
            ))}
            <Typography sx={{ fontSize: "0.62rem", color: "#484f58", ml: "auto" }}>
              count / 15 min window
            </Typography>
          </Box>

          <Divider sx={{ borderColor: "rgba(48,54,61,0.4)" }} />

          {pedError && (
            <Alert severity="error" sx={{ m: 1.5 }}>
              Failed to load pedestrian data
            </Alert>
          )}

          {pedLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
              <CircularProgress size={24} sx={{ color: "#0891B2" }} />
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {pedestrians.length === 0 ? (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <DirectionsWalkIcon sx={{ fontSize: 32, color: "#30363d", mb: 1 }} />
                  <Typography sx={{ fontSize: "0.8rem", color: "#484f58" }}>
                    No pedestrian data available
                  </Typography>
                </Box>
              ) : (
                pedestrians.map((site, idx) => (
                  <Tooltip
                    key={site.siteId}
                    title={`Lat: ${site.lat?.toFixed(4)}, Lon: ${site.lon?.toFixed(4)}`}
                    placement="left"
                    arrow
                  >
                    <Box>
                      <PedestrianRow site={site} rank={idx + 1} />
                    </Box>
                  </Tooltip>
                ))
              )}
            </Box>
          )}

          <Box sx={{ px: 2, py: 1, borderTop: "1px solid rgba(48,54,61,0.5)", flexShrink: 0 }}>
            <Typography variant="caption" sx={{ color: "#30363d", fontSize: "0.62rem" }}>
              Source: Eco Counter · Refreshed every 30 s
            </Typography>
          </Box>
        </Paper>
      </Box>
    </Box>
  );
};
