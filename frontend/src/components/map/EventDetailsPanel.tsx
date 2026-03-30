/**
 * Event/disruption details panel — shown when a map marker is clicked
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
import ConstructionIcon from "@mui/icons-material/Construction";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import type { DisruptionItem, EventItem } from "@/types";
import {
  CATEGORY_EMOJI,
  CATEGORY_LABEL,
  SEVERITY_COLORS,
  getDisruptionCategory,
  getDisruptionSeverity,
  getEventCategory,
  getEventSeverity,
  type SelectedMapItem,
} from "./eventMapUtils";

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

// ── Shared panel shell ───────────────────────────────────────────────

function PanelShell({
  emoji,
  title,
  categoryLabel,
  severityLabel,
  severityColor,
  onClose,
  children,
}: {
  emoji: string;
  title: string;
  categoryLabel: string;
  severityLabel: string;
  severityColor: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
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
        <Typography
          sx={{ fontSize: 22, lineHeight: 1, flexShrink: 0, mt: 0.25 }}
        >
          {emoji}
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
            {title}
          </Typography>
          <Box sx={{ display: "flex", gap: 0.5, mt: 0.4, flexWrap: "wrap" }}>
            <Chip
              size="small"
              label={categoryLabel}
              sx={{
                fontSize: "0.6rem",
                height: 16,
                bgcolor: "rgba(0,0,0,0.06)",
                color: "text.secondary",
              }}
            />
            <Chip
              size="small"
              label={severityLabel}
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
      <Box sx={{ flex: 1, overflow: "auto", px: 1.5, py: 1.25 }}>
        {children}
      </Box>
    </Paper>
  );
}

function DetailRow({
  icon,
  label,
  value,
  valueColor,
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
  valueColor?: string;
}) {
  return (
    <Box
      sx={{ display: "flex", gap: 0.75, alignItems: "flex-start", mb: 0.75 }}
    >
      <Box sx={{ flexShrink: 0, mt: 0.1 }}>{icon}</Box>
      <Box>
        <Typography
          sx={{ fontSize: "0.72rem", color: "text.disabled", lineHeight: 1.1 }}
        >
          {label}
        </Typography>
        <Typography
          sx={{
            fontSize: "0.8rem",
            color: valueColor ?? "text.primary",
            fontWeight: valueColor ? 600 : 400,
            lineHeight: 1.3,
          }}
        >
          {value}
        </Typography>
      </Box>
    </Box>
  );
}

// ── Event panel ──────────────────────────────────────────────────────

function EventPanel({
  event,
  onClose,
}: {
  event: EventItem;
  onClose: () => void;
}) {
  const category = getEventCategory(event.eventType);
  const severity = getEventSeverity(event);
  const severityColor = SEVERITY_COLORS[severity];

  return (
    <PanelShell
      emoji={CATEGORY_EMOJI[category]}
      title={event.eventName}
      categoryLabel={CATEGORY_LABEL[category]}
      severityLabel={severity.charAt(0).toUpperCase() + severity.slice(1)}
      severityColor={severityColor}
      onClose={onClose}
    >
      <DetailRow
        icon={<EventIcon sx={{ fontSize: 14, color: "text.disabled" }} />}
        label="Type"
        value={event.eventType}
      />
      <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mb: 1 }} />
      <DetailRow
        icon={<LocationOnIcon sx={{ fontSize: 14, color: "text.disabled" }} />}
        label="Venue"
        value={event.venueName}
      />
      <DetailRow
        icon={<EventIcon sx={{ fontSize: 14, color: "text.disabled" }} />}
        label="Date"
        value={formatDate(event.eventDate)}
      />
      <DetailRow
        icon={<AccessTimeIcon sx={{ fontSize: 14, color: "text.disabled" }} />}
        label="Time"
        value={`${formatTime(event.startTime)}${event.endTime ? ` – ${formatTime(event.endTime)}` : ""}`}
      />
      {event.estimatedAttendance != null && (
        <DetailRow
          icon={<PeopleIcon sx={{ fontSize: 14, color: "text.disabled" }} />}
          label="Est. Attendance"
          value={event.estimatedAttendance.toLocaleString()}
          valueColor={severityColor}
        />
      )}
      <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mt: 0.5, mb: 0.75 }} />
      <Typography sx={{ fontSize: "0.65rem", color: "text.disabled" }}>
        {event.latitude.toFixed(5)}, {event.longitude.toFixed(5)}
      </Typography>
    </PanelShell>
  );
}

// ── Disruption panel ─────────────────────────────────────────────────

function DisruptionPanel({
  disruption,
  onClose,
}: {
  disruption: DisruptionItem;
  onClose: () => void;
}) {
  const category = getDisruptionCategory(disruption.disruptionType);
  const severity = getDisruptionSeverity(disruption.severity);
  const severityColor = SEVERITY_COLORS[severity];

  return (
    <PanelShell
      emoji={CATEGORY_EMOJI[category]}
      title={disruption.name ?? disruption.disruptionType}
      categoryLabel={CATEGORY_LABEL[category]}
      severityLabel={disruption.severity}
      severityColor={severityColor}
      onClose={onClose}
    >
      {disruption.description && (
        <>
          <DetailRow
            icon={
              <WarningAmberIcon sx={{ fontSize: 14, color: "text.disabled" }} />
            }
            label="Description"
            value={disruption.description}
          />
          <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mb: 1 }} />
        </>
      )}
      {disruption.affectedArea && (
        <DetailRow
          icon={
            <LocationOnIcon sx={{ fontSize: 14, color: "text.disabled" }} />
          }
          label="Affected Area"
          value={disruption.affectedArea}
        />
      )}
      {disruption.startTime && (
        <DetailRow
          icon={
            <AccessTimeIcon sx={{ fontSize: 14, color: "text.disabled" }} />
          }
          label="Started"
          value={`${formatDate(disruption.startTime)} ${formatTime(disruption.startTime)}`}
        />
      )}
      {disruption.estimatedEndTime && (
        <DetailRow
          icon={
            <AccessTimeIcon sx={{ fontSize: 14, color: "text.disabled" }} />
          }
          label="Est. End"
          value={`${formatDate(disruption.estimatedEndTime)} ${formatTime(disruption.estimatedEndTime)}`}
        />
      )}
      {disruption.constructionDetails && (
        <DetailRow
          icon={
            <ConstructionIcon sx={{ fontSize: 14, color: "text.disabled" }} />
          }
          label="Construction Details"
          value={disruption.constructionDetails}
        />
      )}
      {disruption.delayMinutes != null && (
        <DetailRow
          icon={
            <AccessTimeIcon sx={{ fontSize: 14, color: "text.disabled" }} />
          }
          label="Delay"
          value={`${disruption.delayMinutes} min`}
          valueColor={severityColor}
        />
      )}
      {disruption.affectedTransportModes &&
        disruption.affectedTransportModes.length > 0 && (
          <DetailRow
            icon={
              <WarningAmberIcon sx={{ fontSize: 14, color: "text.disabled" }} />
            }
            label="Affects"
            value={disruption.affectedTransportModes.join(", ")}
          />
        )}
      <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mt: 0.5, mb: 0.75 }} />
      <Typography sx={{ fontSize: "0.65rem", color: "text.disabled" }}>
        {disruption.latitude?.toFixed(5)}, {disruption.longitude?.toFixed(5)}
      </Typography>
    </PanelShell>
  );
}

// ── Public component ─────────────────────────────────────────────────

interface EventDetailsPanelProps {
  selected: SelectedMapItem | null;
  onClose: () => void;
}

export const EventDetailsPanel = ({
  selected,
  onClose,
}: EventDetailsPanelProps) => {
  if (!selected) return null;
  if (selected.kind === "event")
    return <EventPanel event={selected.item} onClose={onClose} />;
  return <DisruptionPanel disruption={selected.item} onClose={onClose} />;
};
