/**
 * DisruptionDashboard — full-viewport map with floating panels.
 * Two modes toggled from the panel header:
 *   Disruptions — live active disruptions list, detail with causes + alternatives
 *   Events      — upcoming events by day, detail with venue info + transport
 */

import { useState, useMemo } from "react";
import {
  Alert,
  Box,
  CircularProgress,
  Divider,
  IconButton,
  Link,
  Paper,
  Tab,
  Tabs,
  Tooltip,
  Typography,
} from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import TramIcon from "@mui/icons-material/Tram";
import PedalBikeIcon from "@mui/icons-material/PedalBike";
import EventIcon from "@mui/icons-material/Event";
import TrafficIcon from "@mui/icons-material/Traffic";
import CompareArrowsIcon from "@mui/icons-material/CompareArrows";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import DirectionsWalkIcon from "@mui/icons-material/DirectionsWalk";
import NotificationsActiveIcon from "@mui/icons-material/NotificationsActive";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import PeopleIcon from "@mui/icons-material/People";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import { useQuery } from "@tanstack/react-query";
import { useActiveDisruptions, useEvents } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import type {
  ActiveDisruption,
  DisruptionAlternative,
  DisruptionCause,
  DisruptionSeverity,
  DisruptionType,
  EventItem,
} from "@/types";
import { dashboardApi } from "@/api";
import { NetworkImpactMap } from "@/components/disruption/NetworkImpactMap";
import { RippleEffectVisualization } from "@/components/disruption/RippleEffectVisualization";
import { EventMap } from "@/components/map/EventMap";
import type { SelectedMapItem } from "@/components/map/EventMap";

// ── Layout constants ────────────────────────────────────────────────────
const PANEL_WIDTH = 380;
const DETAIL_HEIGHT = 360;
const GAP = 16;

// ── Disruption colour / label maps ──────────────────────────────────────
const SEVERITY_COLORS: Record<DisruptionSeverity, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

const SEVERITY_ORDER: Record<DisruptionSeverity, number> = {
  CRITICAL: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
};

const TYPE_LABELS: Record<DisruptionType, string> = {
  DELAY: "Delay",
  CANCELLATION: "Cancellation",
  CONGESTION: "Congestion",
  CONSTRUCTION: "Construction",
  EVENT: "Service Pressure",
  ACCIDENT: "Accident",
  TRAM_DISRUPTION: "Tram Disruption",
};

const CAUSE_ICONS: Record<string, React.ReactNode> = {
  EVENT: <EventIcon sx={{ fontSize: 15 }} />,
  CONGESTION: <TrafficIcon sx={{ fontSize: 15 }} />,
  CROSS_MODE: <CompareArrowsIcon sx={{ fontSize: 15 }} />,
};

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: "#EF4444",
  MEDIUM: "#F59E0B",
  LOW: "#10B981",
};

const ALT_MODE_COLORS: Record<string, string> = {
  bus: "#3B82F6",
  rail: "#8B5CF6",
  tram: "#10B981",
  bike: "#F59E0B",
};

// ── Event colour map ────────────────────────────────────────────────────
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

function eventAttendanceLevel(att: number | null): "high" | "medium" | "low" {
  if (att == null) return "low";
  if (att > 5000) return "high";
  if (att > 1000) return "medium";
  return "low";
}

const ATTENDANCE_COLORS = {
  high: "#EF4444",
  medium: "#F59E0B",
  low: "#10B981",
};

// ── Mode filter (disruptions tab) ───────────────────────────────────────
type ModeFilter = "ALL" | "BUS" | "TRAM" | "TRAIN" | "CONGESTION" | "EVENT";

const MODE_TABS: { key: ModeFilter; label: string }[] = [
  { key: "ALL", label: "All" },
  { key: "BUS", label: "Bus" },
  { key: "TRAM", label: "Tram" },
  { key: "TRAIN", label: "Train" },
  { key: "CONGESTION", label: "Traffic" },
  { key: "EVENT", label: "Events" },
];

// ── Helpers ─────────────────────────────────────────────────────────────

function modeIcon(modes: string[] | null, size = 18): React.ReactNode {
  const first = (modes?.[0] ?? "").toUpperCase();
  if (first === "BUS") return <DirectionsBusIcon sx={{ fontSize: size }} />;
  if (first === "TRAM") return <TramIcon sx={{ fontSize: size }} />;
  if (first === "TRAIN") return <TrainIcon sx={{ fontSize: size }} />;
  if (first.includes("CYCLE") || first.includes("BIKE"))
    return <PedalBikeIcon sx={{ fontSize: size }} />;
  return <WarningAmberIcon sx={{ fontSize: size }} />;
}

function altModeIcon(mode: string, size = 16): React.ReactNode {
  const m = mode.toLowerCase();
  if (m === "bus") return <DirectionsBusIcon sx={{ fontSize: size }} />;
  if (m === "tram") return <TramIcon sx={{ fontSize: size }} />;
  if (m === "rail") return <TrainIcon sx={{ fontSize: size }} />;
  if (m === "bike") return <PedalBikeIcon sx={{ fontSize: size }} />;
  return <DirectionsBusIcon sx={{ fontSize: size }} />;
}

function fmtTime(iso: string | null): string {
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

function fmtEtaRemaining(iso: string | null): string {
  if (!iso) return "";
  try {
    const diff = Math.round((new Date(iso).getTime() - Date.now()) / 60_000);
    if (diff <= 0) return "ending";
    if (diff < 60) return `${diff}m left`;
    return `${Math.round(diff / 60)}h left`;
  } catch {
    return "";
  }
}

function matchesMode(d: ActiveDisruption, f: ModeFilter): boolean {
  if (f === "ALL") return true;
  if (f === "EVENT") return d.disruptionType === "EVENT";
  if (f === "CONGESTION") return d.disruptionType === "CONGESTION";
  return d.affectedTransportModes?.includes(f) ?? false;
}

function isoDateStr(offset = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offset);
  return d.toISOString().slice(0, 10);
}

function fmtDayLabel(dateStr: string): string {
  const today = isoDateStr(0);
  const tomorrow = isoDateStr(1);
  if (dateStr === today) return "Today";
  if (dateStr === tomorrow) return "Tomorrow";
  try {
    return new Date(dateStr + "T12:00:00").toLocaleDateString("en-IE", {
      weekday: "short",
      day: "numeric",
      month: "short",
    });
  } catch {
    return dateStr;
  }
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

// ── Shared section label ────────────────────────────────────────────────

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <Typography
      sx={{
        fontSize: "0.65rem",
        fontWeight: 700,
        color: "text.disabled",
        textTransform: "uppercase",
        letterSpacing: 0.8,
        mb: 0.75,
      }}
    >
      {children}
    </Typography>
  );
}

// ── Disruption card ─────────────────────────────────────────────────────

function DisruptionCard({
  d,
  selected,
  onClick,
}: {
  d: ActiveDisruption;
  selected: boolean;
  onClick: () => void;
}) {
  const color = SEVERITY_COLORS[d.severity] ?? "#6B7280";
  const eta = fmtEtaRemaining(d.estimatedEndTime);

  return (
    <Box
      onClick={onClick}
      sx={{
        display: "flex",
        alignItems: "stretch",
        cursor: "pointer",
        bgcolor: selected ? `${color}0e` : "transparent",
        "&:hover": {
          bgcolor: selected ? `${color}14` : "rgba(0,0,0,0.03)",
        },
        transition: "background 0.12s",
      }}
    >
      <Box
        sx={{
          width: 3,
          flexShrink: 0,
          bgcolor: selected ? color : `${color}55`,
          borderRadius: "0 2px 2px 0",
          transition: "background 0.12s",
        }}
      />
      <Box
        sx={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 1.5,
          py: 1.25,
          minWidth: 0,
        }}
      >
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: 2,
            bgcolor: `${color}18`,
            color,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          {modeIcon(d.affectedTransportModes, 18)}
        </Box>

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Box
            sx={{ display: "flex", alignItems: "baseline", gap: 0.75, mb: 0.2 }}
          >
            <Typography
              noWrap
              sx={{ fontSize: "0.875rem", fontWeight: 650, flex: 1 }}
            >
              {d.affectedArea ?? d.name}
            </Typography>
            {d.delayMinutes != null && d.delayMinutes > 0 && (
              <Typography
                sx={{
                  fontSize: "0.72rem",
                  fontWeight: 700,
                  color,
                  flexShrink: 0,
                }}
              >
                +{d.delayMinutes} min
              </Typography>
            )}
          </Box>

          <Box
            sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.2 }}
          >
            <Typography sx={{ fontSize: "0.72rem", fontWeight: 600, color }}>
              {TYPE_LABELS[d.disruptionType] ?? d.disruptionType}
            </Typography>
            {(d.affectedTransportModes ?? []).length > 0 && (
              <>
                <Typography
                  sx={{ fontSize: "0.65rem", color: "text.disabled" }}
                >
                  ·
                </Typography>
                <Typography
                  sx={{ fontSize: "0.72rem", color: "text.secondary" }}
                >
                  {(d.affectedTransportModes ?? []).join(", ")}
                </Typography>
              </>
            )}
            {d.notificationSent && (
              <Tooltip title="Notification sent" arrow>
                <NotificationsActiveIcon
                  sx={{ fontSize: 11, color: "#10B981", ml: 0.25 }}
                />
              </Tooltip>
            )}
          </Box>

          <Typography
            sx={{ fontSize: "0.7rem", color: "text.disabled" }}
            noWrap
          >
            {fmtTime(d.detectedAt)}
            {eta ? ` · ${eta}` : ""}
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}

// ── Event card ──────────────────────────────────────────────────────────

function EventCard({
  event,
  selected,
  onClick,
}: {
  event: EventItem;
  selected: boolean;
  onClick: () => void;
}) {
  const color = eventColor(event.eventType);
  const attLevel = eventAttendanceLevel(event.estimatedAttendance);
  const attColor = ATTENDANCE_COLORS[attLevel];

  return (
    <Box
      onClick={onClick}
      sx={{
        display: "flex",
        alignItems: "stretch",
        cursor: "pointer",
        bgcolor: selected ? `${color}0e` : "transparent",
        "&:hover": { bgcolor: selected ? `${color}14` : "rgba(0,0,0,0.03)" },
        transition: "background 0.12s",
      }}
    >
      <Box
        sx={{
          width: 3,
          flexShrink: 0,
          bgcolor: selected ? color : `${color}55`,
          borderRadius: "0 2px 2px 0",
          transition: "background 0.12s",
        }}
      />
      <Box
        sx={{
          flex: 1,
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 1.5,
          py: 1.25,
          minWidth: 0,
        }}
      >
        {/* Attendance badge */}
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: 2,
            bgcolor: `${attColor}18`,
            color: attColor,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
            gap: 0,
          }}
        >
          <PeopleIcon sx={{ fontSize: 16 }} />
          {event.estimatedAttendance != null && (
            <Typography
              sx={{
                fontSize: "0.52rem",
                fontWeight: 700,
                lineHeight: 1,
                color: attColor,
              }}
            >
              {event.estimatedAttendance >= 1000
                ? `${Math.round(event.estimatedAttendance / 1000)}k`
                : event.estimatedAttendance}
            </Typography>
          )}
        </Box>

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            noWrap
            sx={{ fontSize: "0.875rem", fontWeight: 650, mb: 0.2 }}
          >
            {event.eventName}
          </Typography>
          <Box
            sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.2 }}
          >
            <Typography sx={{ fontSize: "0.72rem", fontWeight: 600, color }}>
              {event.eventType}
            </Typography>
            <Typography sx={{ fontSize: "0.65rem", color: "text.disabled" }}>
              ·
            </Typography>
            <LocationOnIcon sx={{ fontSize: 11, color: "text.disabled" }} />
            <Typography
              sx={{ fontSize: "0.72rem", color: "text.secondary" }}
              noWrap
            >
              {event.venueName}
            </Typography>
          </Box>
          <Typography sx={{ fontSize: "0.7rem", color: "text.disabled" }}>
            {fmtEventTime(event.startTime)}
            {event.endTime ? ` – ${fmtEventTime(event.endTime)}` : ""}
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}

// ── Day selector ────────────────────────────────────────────────────────

function DaySelector({
  days,
  selected,
  counts,
  onChange,
}: {
  days: string[];
  selected: string;
  counts: Record<string, number>;
  onChange: (day: string) => void;
}) {
  return (
    <Box
      sx={{
        display: "flex",
        gap: 0.75,
        overflowX: "auto",
        pb: 0.5,
        "&::-webkit-scrollbar": { display: "none" },
      }}
    >
      {days.map((day) => {
        const active = day === selected;
        return (
          <Box
            key={day}
            onClick={() => onChange(day)}
            sx={{
              flexShrink: 0,
              px: 1.25,
              py: 0.6,
              borderRadius: 2,
              cursor: "pointer",
              bgcolor: active ? "#3B82F6" : "rgba(0,0,0,0.05)",
              border: `1px solid ${active ? "#3B82F6" : "transparent"}`,
              transition: "all 0.12s",
              "&:hover": { bgcolor: active ? "#3B82F6" : "rgba(0,0,0,0.08)" },
              textAlign: "center",
            }}
          >
            <Typography
              sx={{
                fontSize: "0.7rem",
                fontWeight: 600,
                color: active ? "#fff" : "text.primary",
                lineHeight: 1.2,
              }}
            >
              {fmtDayLabel(day)}
            </Typography>
            <Typography
              sx={{
                fontSize: "0.6rem",
                color: active ? "rgba(255,255,255,0.75)" : "text.disabled",
                lineHeight: 1.2,
              }}
            >
              {counts[day] ?? 0}
            </Typography>
          </Box>
        );
      })}
    </Box>
  );
}

// ── Disruption detail panel ─────────────────────────────────────────────

function DisruptionDetailPanel({
  id,
  onClose,
}: {
  id: number;
  onClose: () => void;
}) {
  const { data, isLoading } = useQuery({
    queryKey: ["disruption", "detail", id],
    queryFn: () => dashboardApi.getDisruptionById(id),
    staleTime: 30_000,
  });

  const color = SEVERITY_COLORS[data?.severity ?? "LOW"] ?? "#6B7280";
  const causes: DisruptionCause[] = data?.causes ?? [];
  const alternatives: DisruptionAlternative[] = data?.alternatives ?? [];

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Coloured header */}
      <Box
        sx={{
          bgcolor: color,
          px: 2,
          py: 1.25,
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          flexShrink: 0,
        }}
      >
        <Box
          sx={{
            width: 32,
            height: 32,
            borderRadius: 1.5,
            bgcolor: "rgba(255,255,255,0.2)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
            color: "#fff",
          }}
        >
          {modeIcon(data?.affectedTransportModes ?? null, 17)}
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            noWrap
            sx={{
              fontSize: "0.95rem",
              fontWeight: 750,
              color: "#fff",
              lineHeight: 1.2,
            }}
          >
            {data?.affectedArea ?? data?.name ?? "Loading…"}
          </Typography>
          {data && (
            <Typography
              sx={{ fontSize: "0.72rem", color: "rgba(255,255,255,0.8)" }}
            >
              {TYPE_LABELS[data.disruptionType] ?? data.disruptionType}
              {data.delayMinutes != null && data.delayMinutes > 0
                ? ` · +${data.delayMinutes} min`
                : ""}
              {" · "}
              {fmtTime(data.detectedAt)}
            </Typography>
          )}
        </Box>
        <IconButton
          size="small"
          onClick={onClose}
          sx={{ color: "#fff", opacity: 0.8 }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </Box>

      {isLoading ? (
        <Box
          sx={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            flex: 1,
          }}
        >
          <CircularProgress size={20} sx={{ color }} />
        </Box>
      ) : (
        <Box
          sx={{
            flex: 1,
            overflow: "hidden",
            display: "grid",
            gridTemplateColumns: alternatives.length > 0 ? "1fr 1fr" : "1fr",
          }}
        >
          {/* Left: description + causes */}
          <Box
            sx={{
              overflow: "auto",
              px: 2,
              py: 1.5,
              display: "flex",
              flexDirection: "column",
              gap: 1.5,
              borderRight:
                alternatives.length > 0 ? "1px solid rgba(0,0,0,0.07)" : "none",
            }}
          >
            {data?.description && (
              <Box>
                <SectionLabel>What happened</SectionLabel>
                <Typography
                  sx={{
                    fontSize: "0.8rem",
                    color: "text.secondary",
                    lineHeight: 1.6,
                  }}
                >
                  {data.description}
                </Typography>
              </Box>
            )}

            {causes.length > 0 && (
              <Box>
                <SectionLabel>Possible causes</SectionLabel>
                <Box
                  sx={{ display: "flex", flexDirection: "column", gap: 0.75 }}
                >
                  {causes.map((c) => {
                    const cc = CONFIDENCE_COLORS[c.confidence] ?? "#6B7280";
                    return (
                      <Box
                        key={c.id}
                        sx={{
                          display: "flex",
                          alignItems: "flex-start",
                          gap: 1,
                          p: 1,
                          borderRadius: 1.5,
                          bgcolor: `${cc}08`,
                          border: `1px solid ${cc}20`,
                        }}
                      >
                        <Box
                          sx={{
                            width: 24,
                            height: 24,
                            borderRadius: 1,
                            bgcolor: `${cc}18`,
                            color: cc,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            flexShrink: 0,
                            mt: 0.1,
                          }}
                        >
                          {CAUSE_ICONS[c.causeType] ?? (
                            <WarningAmberIcon sx={{ fontSize: 15 }} />
                          )}
                        </Box>
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography
                            sx={{
                              fontSize: "0.78rem",
                              color: "text.secondary",
                              lineHeight: 1.45,
                            }}
                          >
                            {c.causeDescription}
                          </Typography>
                          <Typography
                            sx={{
                              fontSize: "0.65rem",
                              fontWeight: 600,
                              color: cc,
                              mt: 0.25,
                            }}
                          >
                            {c.confidence} confidence
                          </Typography>
                        </Box>
                      </Box>
                    );
                  })}
                </Box>
              </Box>
            )}

            {!data?.description && causes.length === 0 && (
              <Typography sx={{ fontSize: "0.78rem", color: "text.disabled" }}>
                No additional details available.
              </Typography>
            )}
          </Box>

          {/* Right: alternatives */}
          {alternatives.length > 0 && (
            <Box
              sx={{
                overflow: "auto",
                px: 2,
                py: 1.5,
                display: "flex",
                flexDirection: "column",
                gap: 0.75,
              }}
            >
              <SectionLabel>Nearby alternatives</SectionLabel>
              {alternatives.map((a) => {
                const mc =
                  ALT_MODE_COLORS[a.mode?.toLowerCase() ?? ""] ?? "#6B7280";
                return (
                  <Box
                    key={a.id}
                    sx={{
                      display: "flex",
                      alignItems: "center",
                      gap: 1,
                      p: 1,
                      borderRadius: 1.5,
                      bgcolor: (t) =>
                        t.palette.mode === "dark"
                          ? "rgba(255,255,255,0.04)"
                          : "rgba(0,0,0,0.025)",
                      "&:hover": {
                        bgcolor: (t) =>
                          t.palette.mode === "dark"
                            ? "rgba(255,255,255,0.07)"
                            : "rgba(0,0,0,0.05)",
                      },
                    }}
                  >
                    <Box
                      sx={{
                        width: 30,
                        height: 30,
                        borderRadius: "50%",
                        bgcolor: `${mc}18`,
                        color: mc,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        flexShrink: 0,
                      }}
                    >
                      {altModeIcon(a.mode, 15)}
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 0 }}>
                      <Typography
                        noWrap
                        sx={{ fontSize: "0.8rem", fontWeight: 550 }}
                      >
                        {a.stopName ?? a.description}
                      </Typography>
                      <Typography
                        sx={{ fontSize: "0.68rem", color: "text.disabled" }}
                        noWrap
                      >
                        {a.description}
                        {a.availabilityCount == null
                          ? ""
                          : ` · ${a.availabilityCount} available`}
                      </Typography>
                    </Box>
                    {a.googleMapsWalkingUrl && (
                      <Tooltip title="Walking directions">
                        <IconButton
                          component={Link}
                          href={a.googleMapsWalkingUrl}
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
                );
              })}
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
}

// ── Event detail panel ──────────────────────────────────────────────────

function EventDetailPanel({
  event,
  onClose,
}: {
  event: EventItem;
  onClose: () => void;
}) {
  const color = eventColor(event.eventType);
  const attLevel = eventAttendanceLevel(event.estimatedAttendance);
  const attColor = ATTENDANCE_COLORS[attLevel];

  const { data: nearbyTransport = [], isLoading: transportLoading } = useQuery<
    DisruptionAlternative[]
  >({
    queryKey: ["event", "nearby-alternatives", event.latitude, event.longitude],
    queryFn: () =>
      dashboardApi.getNearbyAlternatives(event.latitude!, event.longitude!),
    staleTime: 120_000,
    enabled: event.latitude != null && event.longitude != null,
  });

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Header */}
      <Box
        sx={{
          bgcolor: color,
          px: 2,
          py: 1.25,
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          flexShrink: 0,
        }}
      >
        <Box
          sx={{
            width: 32,
            height: 32,
            borderRadius: 1.5,
            bgcolor: "rgba(255,255,255,0.2)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
            color: "#fff",
          }}
        >
          <EventIcon sx={{ fontSize: 17 }} />
        </Box>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            noWrap
            sx={{
              fontSize: "0.95rem",
              fontWeight: 750,
              color: "#fff",
              lineHeight: 1.2,
            }}
          >
            {event.eventName}
          </Typography>
          <Typography
            sx={{ fontSize: "0.72rem", color: "rgba(255,255,255,0.8)" }}
          >
            {event.eventType} · {event.venueName}
          </Typography>
        </Box>
        <IconButton
          size="small"
          onClick={onClose}
          sx={{ color: "#fff", opacity: 0.8 }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </Box>

      <Box
        sx={{
          flex: 1,
          overflow: "hidden",
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
        }}
      >
        {/* Left: event info */}
        <Box
          sx={{
            overflow: "auto",
            px: 2,
            py: 1.5,
            display: "flex",
            flexDirection: "column",
            gap: 1.25,
            borderRight: "1px solid rgba(0,0,0,0.07)",
          }}
        >
          <Box>
            <SectionLabel>Venue</SectionLabel>
            <Box sx={{ display: "flex", alignItems: "flex-start", gap: 0.75 }}>
              <LocationOnIcon
                sx={{
                  fontSize: 15,
                  color: "text.disabled",
                  mt: 0.15,
                  flexShrink: 0,
                }}
              />
              <Typography
                sx={{
                  fontSize: "0.82rem",
                  color: "text.primary",
                  lineHeight: 1.45,
                }}
              >
                {event.venueName}
              </Typography>
            </Box>
          </Box>

          <Box>
            <SectionLabel>Time</SectionLabel>
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
              <AccessTimeIcon
                sx={{ fontSize: 15, color: "text.disabled", flexShrink: 0 }}
              />
              <Typography sx={{ fontSize: "0.82rem", color: "text.primary" }}>
                {fmtEventTime(event.startTime)}
                {event.endTime ? ` – ${fmtEventTime(event.endTime)}` : ""}
              </Typography>
            </Box>
          </Box>

          {event.estimatedAttendance != null && (
            <Box>
              <SectionLabel>Expected attendance</SectionLabel>
              <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
                <PeopleIcon
                  sx={{ fontSize: 15, color: attColor, flexShrink: 0 }}
                />
                <Typography
                  sx={{ fontSize: "0.88rem", fontWeight: 700, color: attColor }}
                >
                  {event.estimatedAttendance.toLocaleString()}
                </Typography>
                <Typography sx={{ fontSize: "0.7rem", color: "text.disabled" }}>
                  · {attLevel} impact
                </Typography>
              </Box>
            </Box>
          )}
        </Box>

        {/* Right: transport section */}
        <Box
          sx={{
            overflow: "auto",
            px: 2,
            py: 1.5,
            display: "flex",
            flexDirection: "column",
            gap: 0.75,
          }}
        >
          <SectionLabel>Nearby transport</SectionLabel>
          {transportLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", pt: 2 }}>
              <CircularProgress size={18} sx={{ color }} />
            </Box>
          ) : nearbyTransport.length === 0 ? (
            <Typography sx={{ fontSize: "0.75rem", color: "text.disabled" }}>
              No transport options found nearby.
            </Typography>
          ) : (
            nearbyTransport.map((a, idx) => {
              const mc =
                ALT_MODE_COLORS[a.mode?.toLowerCase() ?? ""] ?? "#6B7280";
              return (
                <Box
                  key={idx}
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    gap: 1,
                    p: 1,
                    borderRadius: 1.5,
                    bgcolor: (t) =>
                      t.palette.mode === "dark"
                        ? "rgba(255,255,255,0.04)"
                        : "rgba(0,0,0,0.025)",
                    "&:hover": {
                      bgcolor: (t) =>
                        t.palette.mode === "dark"
                          ? "rgba(255,255,255,0.07)"
                          : "rgba(0,0,0,0.05)",
                    },
                  }}
                >
                  <Box
                    sx={{
                      width: 30,
                      height: 30,
                      borderRadius: "50%",
                      bgcolor: `${mc}18`,
                      color: mc,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                    }}
                  >
                    {altModeIcon(a.mode, 15)}
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography
                      noWrap
                      sx={{ fontSize: "0.8rem", fontWeight: 550 }}
                    >
                      {a.stopName ?? a.description}
                    </Typography>
                    <Typography
                      sx={{ fontSize: "0.68rem", color: "text.disabled" }}
                      noWrap
                    >
                      {a.description}
                      {a.availabilityCount == null
                        ? ""
                        : ` · ${a.availabilityCount} available`}
                    </Typography>
                  </Box>
                  {a.googleMapsWalkingUrl && (
                    <Tooltip title="Walking directions">
                      <IconButton
                        component={Link}
                        href={a.googleMapsWalkingUrl}
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
              );
            })
          )}
        </Box>
      </Box>
    </Box>
  );
}

// ── Main component ──────────────────────────────────────────────────────

export const DisruptionDashboard = () => {
  // Layout state
  const [panelOpen, setPanelOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(false);

  // Disruptions state
  const [selectedDisruptionId, setSelectedDisruptionId] = useState<
    number | null
  >(null);
  const [modeTab, setModeTab] = useState(0);

  // Events state
  const [tabMode, setTabMode] = useState<"disruptions" | "events">(
    "disruptions",
  );
  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);
  const [selectedDay, setSelectedDay] = useState<string>(() => isoDateStr(0));
  const [selectedMapItem, setSelectedMapItem] =
    useState<SelectedMapItem | null>(null);

  const theme = useAppSelector((s) => s.ui.theme);

  // Data
  const {
    data: disruptions = [],
    isLoading: disruptionsLoading,
    error: disruptionsError,
    dataUpdatedAt,
  } = useActiveDisruptions();

  const { data: allEvents = [], isLoading: eventsLoading } = useEvents(500);

  // ── Disruptions derived state ──
  const lastUpdated = dataUpdatedAt
    ? new Date(dataUpdatedAt).toLocaleTimeString("en-IE", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })
    : null;

  const modeFilter = MODE_TABS[modeTab]?.key ?? "ALL";

  const filteredDisruptions = useMemo(
    () =>
      disruptions
        .filter((d) => matchesMode(d, modeFilter))
        .toSorted(
          (a, b) =>
            (SEVERITY_ORDER[a.severity] ?? 4) -
            (SEVERITY_ORDER[b.severity] ?? 4),
        ),
    [disruptions, modeFilter],
  );

  const counts = useMemo(
    () => ({
      total: disruptions.length,
      critical: disruptions.filter((d) => d.severity === "CRITICAL").length,
      high: disruptions.filter((d) => d.severity === "HIGH").length,
      medium: disruptions.filter((d) => d.severity === "MEDIUM").length,
      low: disruptions.filter((d) => d.severity === "LOW").length,
    }),
    [disruptions],
  );

  // ── Events derived state ──
  const availableDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => isoDateStr(i)),
    [],
  );

  const eventCountByDay = useMemo(() => {
    const map: Record<string, number> = {};
    for (const e of allEvents) {
      const d = e.eventDate.slice(0, 10);
      map[d] = (map[d] ?? 0) + 1;
    }
    return map;
  }, [allEvents]);

  const dayEvents = useMemo(
    () => allEvents.filter((e) => e.eventDate.slice(0, 10) === selectedDay),
    [allEvents, selectedDay],
  );

  const selectedEvent = dayEvents.find((e) => e.id === selectedEventId) ?? null;

  // ── Handlers ──
  function handleDisruptionSelect(id: number) {
    const next = selectedDisruptionId === id ? null : id;
    setSelectedDisruptionId(next);
    setDetailOpen(next != null);
  }

  function handleEventSelect(id: number) {
    const next = selectedEventId === id ? null : id;
    setSelectedEventId(next);
    setDetailOpen(next != null);
  }

  function handleTabModeChange(mode: "disruptions" | "events") {
    setTabMode(mode);
    setDetailOpen(false);
    setSelectedDisruptionId(null);
    setSelectedEventId(null);
    setSelectedMapItem(null);
  }

  const isLoading =
    tabMode === "disruptions" ? disruptionsLoading : eventsLoading;
  const error = tabMode === "disruptions" ? disruptionsError : null;

  // All categories/severities for events map (show everything)
  const ALL_EVENT_CATEGORIES = useMemo(
    () => new Set(["construction", "public", "emergency"] as const),
    [],
  );
  const ALL_EVENT_SEVERITIES = useMemo(
    () => new Set(["high", "medium", "low"] as const),
    [],
  );

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Map background */}
      {tabMode === "disruptions" ? (
        <NetworkImpactMap
          disruptions={disruptions}
          selectedId={selectedDisruptionId}
          onMarkerClick={(id) => handleDisruptionSelect(id)}
          darkTiles={theme === "dark"}
        />
      ) : (
        <EventMap
          events={dayEvents}
          disruptions={[]}
          pedestrians={[]}
          selectedTypes={ALL_EVENT_CATEGORIES}
          selectedSeverities={ALL_EVENT_SEVERITIES}
          selectedItem={selectedMapItem}
          onEventClick={(e) => {
            setSelectedMapItem({ kind: "event", item: e });
            handleEventSelect(e.id);
          }}
          onDisruptionClick={() => {}}
          onPedestrianClick={() => {}}
        />
      )}

      {error && (
        <Alert
          severity="error"
          sx={{
            position: "absolute",
            top: GAP,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1200,
            borderRadius: 2,
          }}
        >
          Failed to load disruptions
        </Alert>
      )}

      {/* Top-left KPI strip — disruptions mode only */}
      {tabMode === "disruptions" && (
        <Box
          sx={{
            position: "absolute",
            top: GAP,
            left: GAP,
            zIndex: 1000,
            display: "flex",
            gap: 0.75,
            flexWrap: "wrap",
            alignItems: "center",
          }}
        >
          {[
            { label: "Total", value: counts.total, color: "#6B7280" },
            { label: "Critical", value: counts.critical, color: "#7C3AED" },
            { label: "High", value: counts.high, color: "#EF4444" },
            { label: "Medium", value: counts.medium, color: "#F59E0B" },
            { label: "Low", value: counts.low, color: "#10B981" },
          ].map(({ label, value, color }) => (
            <Box
              key={label}
              sx={{
                px: 1.5,
                py: 0.75,
                borderRadius: 2,
                bgcolor: (t) =>
                  t.palette.mode === "dark"
                    ? "rgba(30,30,30,0.88)"
                    : "rgba(255,255,255,0.9)",
                backdropFilter: "blur(10px)",
                border: `1px solid ${color}33`,
                textAlign: "center",
                minWidth: 52,
              }}
            >
              <Typography
                sx={{
                  fontSize: "1.1rem",
                  fontWeight: 800,
                  color,
                  lineHeight: 1,
                }}
              >
                {value}
              </Typography>
              <Typography
                sx={{ fontSize: "0.6rem", color: "text.secondary", mt: 0.1 }}
              >
                {label}
              </Typography>
            </Box>
          ))}

          {!disruptionsLoading && (
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 0.5,
                px: 1.25,
                py: 0.75,
                borderRadius: 2,
                bgcolor: (t) =>
                  t.palette.mode === "dark"
                    ? "rgba(30,30,30,0.88)"
                    : "rgba(255,255,255,0.9)",
                backdropFilter: "blur(10px)",
              }}
            >
              <FiberManualRecordIcon
                sx={{
                  fontSize: 8,
                  color: disruptions.length > 0 ? "#EF4444" : "#10B981",
                  animation:
                    disruptions.length > 0
                      ? "pulse 1.5s ease-in-out infinite"
                      : "none",
                  "@keyframes pulse": {
                    "0%, 100%": { opacity: 1 },
                    "50%": { opacity: 0.3 },
                  },
                }}
              />
              <Typography sx={{ fontSize: "0.65rem", color: "text.secondary" }}>
                {disruptions.length > 0
                  ? `${disruptions.length} active`
                  : "All clear"}
              </Typography>
              {lastUpdated && (
                <Typography
                  sx={{ fontSize: "0.6rem", color: "text.disabled", ml: 0.25 }}
                >
                  · {lastUpdated}
                </Typography>
              )}
            </Box>
          )}
        </Box>
      )}

      {/* Panel open button */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* Right side panel */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            bottom: detailOpen ? DETAIL_HEIGHT + GAP * 2 : GAP,
            width: PANEL_WIDTH,
            zIndex: 1000,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          {/* Panel header: mode toggle */}
          <Box sx={{ px: 2, pt: 1.5, pb: 1, flexShrink: 0 }}>
            <Box sx={{ display: "flex", alignItems: "center", mb: 1.25 }}>
              {/* Mode toggle */}
              <Box
                sx={{
                  display: "flex",
                  flex: 1,
                  bgcolor: (t) =>
                    t.palette.mode === "dark"
                      ? "rgba(255,255,255,0.07)"
                      : "rgba(0,0,0,0.05)",
                  borderRadius: 2,
                  p: 0.4,
                  gap: 0.4,
                }}
              >
                {(["disruptions", "events"] as const).map((mode) => {
                  const active = tabMode === mode;
                  return (
                    <Box
                      key={mode}
                      onClick={() => handleTabModeChange(mode)}
                      sx={{
                        flex: 1,
                        py: 0.55,
                        borderRadius: 1.5,
                        cursor: "pointer",
                        textAlign: "center",
                        bgcolor: active
                          ? (t) => t.palette.background.paper
                          : "transparent",
                        boxShadow: active
                          ? "0 1px 3px rgba(0,0,0,0.12)"
                          : "none",
                        transition: "all 0.15s",
                      }}
                    >
                      <Typography
                        sx={{
                          fontSize: "0.72rem",
                          fontWeight: active ? 700 : 500,
                          color: active ? "text.primary" : "text.secondary",
                        }}
                      >
                        {mode === "disruptions" ? "Disruptions" : "Events"}
                        {mode === "disruptions" && disruptions.length > 0 && (
                          <Box
                            component="span"
                            sx={{
                              ml: 0.5,
                              fontSize: "0.62rem",
                              color: "#EF4444",
                              fontWeight: 700,
                            }}
                          >
                            {disruptions.length}
                          </Box>
                        )}
                        {mode === "events" && dayEvents.length > 0 && (
                          <Box
                            component="span"
                            sx={{
                              ml: 0.5,
                              fontSize: "0.62rem",
                              color: "#6366F1",
                              fontWeight: 700,
                            }}
                          >
                            {dayEvents.length}
                          </Box>
                        )}
                      </Typography>
                    </Box>
                  );
                })}
              </Box>

              <IconButton
                size="small"
                onClick={() => setPanelOpen(false)}
                sx={{ ml: 0.75 }}
              >
                <CloseIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </Box>

            {/* Disruptions: mode filter tabs */}
            {tabMode === "disruptions" && (
              <Tabs
                value={modeTab}
                onChange={(_, v) => setModeTab(v)}
                variant="scrollable"
                scrollButtons={false}
                sx={{
                  minHeight: 30,
                  "& .MuiTab-root": {
                    minHeight: 30,
                    fontSize: "0.68rem",
                    textTransform: "none",
                    px: 1.25,
                    py: 0,
                    fontWeight: 500,
                  },
                  "& .MuiTabs-indicator": { height: 2 },
                }}
              >
                {MODE_TABS.map((t) => (
                  <Tab
                    key={t.key}
                    label={
                      t.key === "ALL"
                        ? `All (${disruptions.length})`
                        : `${t.label} (${disruptions.filter((d) => matchesMode(d, t.key)).length})`
                    }
                  />
                ))}
              </Tabs>
            )}

            {/* Events: day selector */}
            {tabMode === "events" && (
              <DaySelector
                days={availableDays}
                selected={selectedDay}
                counts={eventCountByDay}
                onChange={(day) => {
                  setSelectedDay(day);
                  setSelectedEventId(null);
                  setDetailOpen(false);
                }}
              />
            )}
          </Box>

          <Divider />

          {/* List content */}
          {isLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
              <CircularProgress
                size={22}
                sx={{
                  color: tabMode === "disruptions" ? "#EF4444" : "#6366F1",
                }}
              />
            </Box>
          ) : tabMode === "disruptions" ? (
            filteredDisruptions.length === 0 ? (
              <Box sx={{ px: 2, py: 5, textAlign: "center" }}>
                <CheckCircleOutlineIcon
                  sx={{ fontSize: 32, color: "#10B981", mb: 1 }}
                />
                <Typography
                  sx={{ fontSize: "0.82rem", color: "text.secondary" }}
                >
                  No disruptions in this category
                </Typography>
              </Box>
            ) : (
              <Box sx={{ flex: 1, overflow: "auto" }}>
                {filteredDisruptions.map((d, idx) => (
                  <Box key={d.id}>
                    <DisruptionCard
                      d={d}
                      selected={selectedDisruptionId === d.id}
                      onClick={() => handleDisruptionSelect(d.id)}
                    />
                    {idx < filteredDisruptions.length - 1 && (
                      <Divider
                        sx={{ borderColor: "rgba(0,0,0,0.05)", ml: 7 }}
                      />
                    )}
                  </Box>
                ))}
              </Box>
            )
          ) : dayEvents.length === 0 ? (
            <Box sx={{ px: 2, py: 5, textAlign: "center" }}>
              <EventIcon sx={{ fontSize: 32, color: "text.disabled", mb: 1 }} />
              <Typography sx={{ fontSize: "0.82rem", color: "text.secondary" }}>
                No events on this day
              </Typography>
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {dayEvents.map((e, idx) => (
                <Box key={e.id}>
                  <EventCard
                    event={e}
                    selected={selectedEventId === e.id}
                    onClick={() => handleEventSelect(e.id)}
                  />
                  {idx < dayEvents.length - 1 && (
                    <Divider sx={{ borderColor: "rgba(0,0,0,0.05)", ml: 7 }} />
                  )}
                </Box>
              ))}
            </Box>
          )}

          <Box
            sx={{
              px: 2,
              py: 0.75,
              borderTop: "1px solid rgba(0,0,0,0.06)",
              flexShrink: 0,
            }}
          >
            <Typography sx={{ fontSize: "0.62rem", color: "text.disabled" }}>
              {tabMode === "disruptions"
                ? "Auto-detected · refreshed every 5 min"
                : `${dayEvents.length} event${dayEvents.length === 1 ? "" : "s"} · ${fmtDayLabel(selectedDay)}`}
            </Typography>
          </Box>
        </Paper>
      )}

      {/* Bottom detail panel */}
      {detailOpen &&
        tabMode === "disruptions" &&
        selectedDisruptionId != null && (
          <Paper
            elevation={0}
            sx={{
              position: "absolute",
              bottom: GAP,
              left: GAP,
              right: panelOpen ? PANEL_WIDTH + GAP * 2 : GAP,
              height: DETAIL_HEIGHT,
              zIndex: 1000,
              borderRadius: 3,
              overflow: "hidden",
              transition: "right 0.2s ease",
            }}
          >
            <DisruptionDetailPanel
              id={selectedDisruptionId}
              onClose={() => setDetailOpen(false)}
            />
          </Paper>
        )}

      {detailOpen && tabMode === "events" && selectedEvent != null && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            bottom: GAP,
            left: GAP,
            right: panelOpen ? PANEL_WIDTH + GAP * 2 : GAP,
            height: DETAIL_HEIGHT,
            zIndex: 1000,
            borderRadius: 3,
            overflow: "hidden",
            transition: "right 0.2s ease",
          }}
        >
          <EventDetailPanel
            event={selectedEvent}
            onClose={() => setDetailOpen(false)}
          />
        </Paper>
      )}

      {/* Mode impact panel — disruptions mode, nothing selected */}
      {!detailOpen &&
        tabMode === "disruptions" &&
        !selectedDisruptionId &&
        disruptions.length > 0 && (
          <Paper
            elevation={0}
            sx={{
              position: "absolute",
              bottom: GAP,
              left: GAP,
              right: panelOpen ? PANEL_WIDTH + GAP * 2 : GAP,
              height: 200,
              zIndex: 1000,
              borderRadius: 3,
              overflow: "hidden",
              display: "flex",
              flexDirection: "column",
              transition: "right 0.2s ease",
            }}
          >
            <Box
              sx={{
                px: 2,
                py: 1,
                borderBottom: "1px solid rgba(0,0,0,0.07)",
                flexShrink: 0,
                display: "flex",
                alignItems: "center",
              }}
            >
              <Typography
                sx={{ fontSize: "0.875rem", fontWeight: 700, flex: 1 }}
              >
                Mode Impact
              </Typography>
              <Typography sx={{ fontSize: "0.65rem", color: "text.secondary" }}>
                Select a disruption for causes & alternatives
              </Typography>
            </Box>
            <Box sx={{ flex: 1, minHeight: 0, overflow: "hidden" }}>
              <RippleEffectVisualization disruptions={disruptions} />
            </Box>
          </Paper>
        )}
    </Box>
  );
};
