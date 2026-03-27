/**
 * Event details panel — shown when a map marker is clicked
 */

import {
  Box,
  Chip,
  Divider,
  IconButton,
  Paper,
  Typography,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import EventIcon from "@mui/icons-material/Event";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import PeopleIcon from "@mui/icons-material/People";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import type { EventItem } from "@/types";
import {
  CATEGORY_EMOJI,
  CATEGORY_LABEL,
  SEVERITY_COLORS,
  getEventCategory,
  getEventSeverity,
} from "./EventMap";

function formatDate(dateStr: string): string {
  try {
    return new Date(dateStr).toLocaleDateString("en-IE", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
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

interface EventDetailsPanelProps {
  event: EventItem | null;
  onClose: () => void;
}

export const EventDetailsPanel = ({
  event,
  onClose,
}: EventDetailsPanelProps) => {
  if (!event) return null;

  const category = getEventCategory(event.eventType);
  const severity = getEventSeverity(event);
  const severityColor = SEVERITY_COLORS[severity];

  return (
    <Paper
      elevation={0}
      sx={{
        width: 260,
        flexShrink: 0,
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
        bgcolor: "background.paper",
        border: "1px solid rgba(0,0,0,0.08)",
        borderRadius: 2,
      }}
    >
      {/* Header */}
      <Box
        sx={{
          px: 1.5,
          py: 1.25,
          display: "flex",
          alignItems: "flex-start",
          gap: 1,
          borderBottom: "1px solid rgba(0,0,0,0.08)",
        }}
      >
        <Typography sx={{ fontSize: 22, lineHeight: 1, flexShrink: 0, mt: 0.25 }}>
          {CATEGORY_EMOJI[category]}
        </Typography>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            sx={{
              fontSize: "0.82rem",
              fontWeight: 700,
              color: "text.primary",
              lineHeight: 1.3,
            }}
          >
            {event.eventName}
          </Typography>
          <Box sx={{ display: "flex", gap: 0.5, mt: 0.4, flexWrap: "wrap" }}>
            <Chip
              size="small"
              label={CATEGORY_LABEL[category]}
              sx={{
                fontSize: "0.6rem",
                height: 16,
                bgcolor: "rgba(0,0,0,0.06)",
                color: "text.secondary",
              }}
            />
            <Chip
              size="small"
              label={severity.charAt(0).toUpperCase() + severity.slice(1)}
              sx={{
                fontSize: "0.6rem",
                height: 16,
                bgcolor: severityColor + "22",
                color: severityColor,
                border: `1px solid ${severityColor}44`,
              }}
            />
          </Box>
        </Box>
        <IconButton size="small" onClick={onClose} sx={{ flexShrink: 0 }}>
          <CloseIcon sx={{ fontSize: 16 }} />
        </IconButton>
      </Box>

      {/* Details */}
      <Box sx={{ flex: 1, overflow: "auto", px: 1.5, py: 1.25 }}>
        {/* Event type */}
        <Box sx={{ display: "flex", gap: 0.75, alignItems: "center", mb: 1 }}>
          <EventIcon sx={{ fontSize: 14, color: "text.disabled", flexShrink: 0 }} />
          <Typography sx={{ fontSize: "0.78rem", color: "text.secondary" }}>
            {event.eventType}
          </Typography>
        </Box>

        <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mb: 1 }} />

        {/* Venue */}
        <Box sx={{ display: "flex", gap: 0.75, alignItems: "flex-start", mb: 0.75 }}>
          <LocationOnIcon
            sx={{ fontSize: 14, color: "text.disabled", flexShrink: 0, mt: 0.1 }}
          />
          <Box>
            <Typography sx={{ fontSize: "0.72rem", color: "text.disabled", lineHeight: 1.1 }}>
              Venue
            </Typography>
            <Typography sx={{ fontSize: "0.8rem", color: "text.primary", lineHeight: 1.3 }}>
              {event.venueName}
            </Typography>
          </Box>
        </Box>

        {/* Date */}
        <Box sx={{ display: "flex", gap: 0.75, alignItems: "flex-start", mb: 0.75 }}>
          <EventIcon
            sx={{ fontSize: 14, color: "text.disabled", flexShrink: 0, mt: 0.1 }}
          />
          <Box>
            <Typography sx={{ fontSize: "0.72rem", color: "text.disabled", lineHeight: 1.1 }}>
              Date
            </Typography>
            <Typography sx={{ fontSize: "0.8rem", color: "text.primary", lineHeight: 1.3 }}>
              {formatDate(event.eventDate)}
            </Typography>
          </Box>
        </Box>

        {/* Time */}
        <Box sx={{ display: "flex", gap: 0.75, alignItems: "flex-start", mb: 0.75 }}>
          <AccessTimeIcon
            sx={{ fontSize: 14, color: "text.disabled", flexShrink: 0, mt: 0.1 }}
          />
          <Box>
            <Typography sx={{ fontSize: "0.72rem", color: "text.disabled", lineHeight: 1.1 }}>
              Time
            </Typography>
            <Typography sx={{ fontSize: "0.8rem", color: "text.primary", lineHeight: 1.3 }}>
              {formatTime(event.startTime)}
              {event.endTime ? ` – ${formatTime(event.endTime)}` : ""}
            </Typography>
          </Box>
        </Box>

        {/* Attendance */}
        {event.estimatedAttendance != null && (
          <Box sx={{ display: "flex", gap: 0.75, alignItems: "flex-start", mb: 0.75 }}>
            <PeopleIcon
              sx={{ fontSize: 14, color: "text.disabled", flexShrink: 0, mt: 0.1 }}
            />
            <Box>
              <Typography sx={{ fontSize: "0.72rem", color: "text.disabled", lineHeight: 1.1 }}>
                Est. Attendance
              </Typography>
              <Typography
                sx={{
                  fontSize: "0.8rem",
                  fontWeight: 600,
                  color: severityColor,
                  lineHeight: 1.3,
                }}
              >
                {event.estimatedAttendance.toLocaleString()}
              </Typography>
            </Box>
          </Box>
        )}

        {/* Coords */}
        <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mt: 0.5, mb: 0.75 }} />
        <Typography sx={{ fontSize: "0.65rem", color: "text.disabled" }}>
          {event.latitude.toFixed(5)}, {event.longitude.toFixed(5)}
        </Typography>
      </Box>
    </Paper>
  );
};
