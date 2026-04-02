/**
 * DisruptionDashboard — Live disruption detection overview.
 *
 * Layout:
 *  - Top: summary KPI strip (total / by severity)
 *  - Left column: disruption list with severity badges
 *  - Right top: NetworkImpactMap
 *  - Right bottom: RippleEffectVisualization
 */

import { useState } from "react";
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Divider,
  Paper,
  Tooltip,
  Typography,
} from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import PedalBikeIcon from "@mui/icons-material/PedalBike";
import EventIcon from "@mui/icons-material/Event";
import TrafficIcon from "@mui/icons-material/Traffic";
import CompareArrowsIcon from "@mui/icons-material/CompareArrows";
import CloseIcon from "@mui/icons-material/Close";
import { useQuery } from "@tanstack/react-query";
import { useActiveDisruptions } from "@/hooks";
import type {
  ActiveDisruption,
  DisruptionAlternative,
  DisruptionCause,
  DisruptionSeverity,
  DisruptionType,
} from "@/types";
import { dashboardApi } from "@/api";
import { NetworkImpactMap } from "@/components/disruption/NetworkImpactMap";
import { RippleEffectVisualization } from "@/components/disruption/RippleEffectVisualization";

// ── Constants ──────────────────────────────────────────────────────────

const SEVERITY_COLORS: Record<DisruptionSeverity, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

const TYPE_LABELS: Record<DisruptionType, string> = {
  DELAY: "Delay",
  CANCELLATION: "Cancellation",
  CONGESTION: "Congestion",
  CONSTRUCTION: "Construction",
  EVENT: "Event",
  ACCIDENT: "Accident",
  TRAM_DISRUPTION: "Tram Disruption",
};

const CAUSE_ICONS: Record<string, React.ReactNode> = {
  EVENT: <EventIcon sx={{ fontSize: 16 }} />,
  CONGESTION: <TrafficIcon sx={{ fontSize: 16 }} />,
  CROSS_MODE: <CompareArrowsIcon sx={{ fontSize: 16 }} />,
};

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: "#EF4444",
  MEDIUM: "#F59E0B",
  LOW: "#10B981",
};

function altIcon(mode: string): React.ReactNode {
  const m = mode.toUpperCase();
  if (m.includes("BUS")) return <DirectionsBusIcon sx={{ fontSize: 15 }} />;
  if (m.includes("BIKE") || m.includes("CYCLE")) return <PedalBikeIcon sx={{ fontSize: 15 }} />;
  return <TrainIcon sx={{ fontSize: 15 }} />;
}

// ── Helpers ────────────────────────────────────────────────────────────

function formatDetected(iso: string | null): string {
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

function formatEta(iso: string | null): string {
  if (!iso) return "—";
  try {
    const d = new Date(iso);
    const now = new Date();
    const diffMin = Math.round((d.getTime() - now.getTime()) / 60_000);
    if (diffMin < 0) return "overdue";
    if (diffMin < 60) return `~${diffMin} min`;
    return `~${Math.round(diffMin / 60)}h`;
  } catch {
    return iso;
  }
}

function countBySeverity(
  disruptions: ActiveDisruption[],
  severity: DisruptionSeverity,
): number {
  return disruptions.filter((d) => d.severity === severity).length;
}

// ── Sub-components ─────────────────────────────────────────────────────

function KpiCard({
  label,
  value,
  color,
}: {
  label: string;
  value: number;
  color: string;
}) {
  return (
    <Box
      sx={{
        flex: "1 1 0",
        minWidth: 80,
        px: 2,
        py: 1.25,
        borderRadius: 2,
        bgcolor: color + "14",
        border: `1px solid ${color}33`,
        textAlign: "center",
      }}
    >
      <Typography
        sx={{ fontSize: "1.5rem", fontWeight: 800, color, lineHeight: 1.1 }}
      >
        {value}
      </Typography>
      <Typography
        sx={{ fontSize: "0.65rem", color: "text.secondary", mt: 0.2 }}
      >
        {label}
      </Typography>
    </Box>
  );
}

function DisruptionRow({
  disruption,
  selected,
  onClick,
}: {
  disruption: ActiveDisruption;
  selected: boolean;
  onClick: () => void;
}) {
  const color = SEVERITY_COLORS[disruption.severity] ?? "#6B7280";

  return (
    <Box
      onClick={onClick}
      sx={{
        py: 1.1,
        px: 2,
        cursor: "pointer",
        bgcolor: selected ? color + "10" : "transparent",
        borderLeft: `3px solid ${selected ? color : "transparent"}`,
        "&:hover": { bgcolor: "rgba(0,0,0,0.03)" },
        transition: "all 0.12s",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1 }}>
        {/* Severity dot */}
        <Box
          sx={{
            mt: 0.4,
            width: 10,
            height: 10,
            borderRadius: "50%",
            bgcolor: color,
            flexShrink: 0,
            boxShadow: `0 0 6px ${color}88`,
          }}
        />

        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            noWrap
            sx={{
              fontSize: "0.845rem",
              fontWeight: selected ? 600 : 400,
              color: selected ? "text.primary" : "text.secondary",
              lineHeight: 1.25,
            }}
          >
            {disruption.name}
          </Typography>

          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 0.5,
              mt: 0.3,
              flexWrap: "wrap",
            }}
          >
            <Chip
              size="small"
              label={disruption.severity}
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
              label={
                TYPE_LABELS[disruption.disruptionType] ??
                disruption.disruptionType
              }
              sx={{ fontSize: "0.6rem", height: 16 }}
            />
            {(disruption.affectedTransportModes ?? []).slice(0, 2).map((m) => (
              <Chip
                key={m}
                size="small"
                label={m}
                sx={{ fontSize: "0.58rem", height: 14 }}
              />
            ))}
          </Box>

          {disruption.affectedArea && (
            <Typography
              sx={{ fontSize: "0.68rem", color: "text.disabled", mt: 0.2 }}
              noWrap
            >
              {disruption.affectedArea}
            </Typography>
          )}

          <Box
            sx={{ display: "flex", alignItems: "center", gap: 1.5, mt: 0.3 }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 0.3 }}>
              <AccessTimeIcon sx={{ fontSize: 10, color: "text.disabled" }} />
              <Typography sx={{ fontSize: "0.65rem", color: "text.disabled" }}>
                {formatDetected(disruption.detectedAt)}
              </Typography>
            </Box>
            {disruption.estimatedEndTime && (
              <Typography sx={{ fontSize: "0.65rem", color: "text.disabled" }}>
                ETA: {formatEta(disruption.estimatedEndTime)}
              </Typography>
            )}
            {disruption.delayMinutes != null && disruption.delayMinutes > 0 && (
              <Typography sx={{ fontSize: "0.65rem", color }}>
                +{disruption.delayMinutes} min
              </Typography>
            )}
          </Box>
        </Box>

        {disruption.notificationSent && (
          <Tooltip title="Notification sent" placement="left" arrow>
            <CheckCircleOutlineIcon
              sx={{ fontSize: 14, color: "#10B981", mt: 0.3 }}
            />
          </Tooltip>
        )}
      </Box>
    </Box>
  );
}

// ── Detail Panel ───────────────────────────────────────────────────────

function DetailPanel({
  disruptionId,
  onClose,
}: {
  disruptionId: number;
  onClose: () => void;
}) {
  const { data, isLoading } = useQuery({
    queryKey: ["disruption", "detail", disruptionId],
    queryFn: () => dashboardApi.getDisruptionById(disruptionId),
    staleTime: 30_000,
  });

  const color = data ? (SEVERITY_COLORS[data.severity] ?? "#6B7280") : "#6B7280";
  const causes: DisruptionCause[] = data?.causes ?? [];
  const alternatives: DisruptionAlternative[] = data?.alternatives ?? [];

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%", overflow: "auto" }}>
      {/* Panel header */}
      <Box
        sx={{
          px: 2,
          py: 1.25,
          borderBottom: "1px solid rgba(0,0,0,0.08)",
          display: "flex",
          alignItems: "center",
          gap: 1,
          flexShrink: 0,
        }}
      >
        <Box
          sx={{
            width: 8,
            height: 8,
            borderRadius: "50%",
            bgcolor: color,
            flexShrink: 0,
          }}
        />
        <Typography variant="subtitle2" fontWeight={700} sx={{ flex: 1 }} noWrap>
          {data?.name ?? "Disruption Detail"}
        </Typography>
        <Box
          onClick={onClose}
          sx={{ cursor: "pointer", color: "text.disabled", display: "flex", "&:hover": { color: "text.primary" } }}
        >
          <CloseIcon sx={{ fontSize: 16 }} />
        </Box>
      </Box>

      {isLoading ? (
        <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
          <CircularProgress size={20} sx={{ color }} />
        </Box>
      ) : (
        <Box sx={{ flex: 1, overflow: "auto", px: 2, py: 1.5 }}>
          {/* Causes */}
          {causes.length > 0 && (
            <Box sx={{ mb: 1.5 }}>
              <Typography sx={{ fontSize: "0.65rem", fontWeight: 700, color: "text.disabled", textTransform: "uppercase", letterSpacing: 0.6, mb: 0.75 }}>
                Possible Causes
              </Typography>
              {causes.map((c) => (
                <Box key={c.id} sx={{ display: "flex", alignItems: "flex-start", gap: 1, py: 0.5 }}>
                  <Box sx={{ color: CONFIDENCE_COLORS[c.confidence], mt: 0.1, flexShrink: 0 }}>
                    {CAUSE_ICONS[c.causeType] ?? <WarningAmberIcon sx={{ fontSize: 16 }} />}
                  </Box>
                  <Typography sx={{ fontSize: "0.78rem", flex: 1, color: "text.secondary", lineHeight: 1.4 }}>
                    {c.causeDescription}
                  </Typography>
                  <Chip
                    label={c.confidence}
                    size="small"
                    sx={{
                      fontSize: "0.55rem",
                      height: 14,
                      bgcolor: CONFIDENCE_COLORS[c.confidence] + "18",
                      color: CONFIDENCE_COLORS[c.confidence],
                      flexShrink: 0,
                    }}
                  />
                </Box>
              ))}
            </Box>
          )}

          {causes.length > 0 && alternatives.length > 0 && (
            <Divider sx={{ borderColor: "rgba(0,0,0,0.06)", mb: 1.5 }} />
          )}

          {/* Alternatives */}
          {alternatives.length > 0 && (
            <Box>
              <Typography sx={{ fontSize: "0.65rem", fontWeight: 700, color: "text.disabled", textTransform: "uppercase", letterSpacing: 0.6, mb: 0.75 }}>
                Alternative Transport
              </Typography>
              {alternatives.map((a) => (
                <Box key={a.id} sx={{ display: "flex", alignItems: "flex-start", gap: 1, py: 0.5 }}>
                  <Box sx={{ color: "text.disabled", mt: 0.1, flexShrink: 0 }}>
                    {altIcon(a.mode)}
                  </Box>
                  <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Typography sx={{ fontSize: "0.78rem", color: "text.secondary", lineHeight: 1.3 }} noWrap>
                      {a.description}
                    </Typography>
                    {a.etaMinutes != null && (
                      <Typography sx={{ fontSize: "0.68rem", color: "text.disabled" }}>
                        ~{a.etaMinutes} min
                      </Typography>
                    )}
                  </Box>
                  {a.availabilityCount != null && (
                    <Typography sx={{ fontSize: "0.68rem", color: "text.disabled", flexShrink: 0 }}>
                      {a.availabilityCount} avail.
                    </Typography>
                  )}
                </Box>
              ))}
            </Box>
          )}

          {causes.length === 0 && alternatives.length === 0 && (
            <Typography sx={{ fontSize: "0.78rem", color: "text.disabled", textAlign: "center", py: 2 }}>
              No additional details available
            </Typography>
          )}
        </Box>
      )}
    </Box>
  );
}

// ── Main Component ─────────────────────────────────────────────────────

export const DisruptionDashboard = () => {
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const {
    data: disruptions = [],
    isLoading,
    error,
    dataUpdatedAt,
  } = useActiveDisruptions();

  const lastUpdated = dataUpdatedAt
    ? new Date(dataUpdatedAt).toLocaleTimeString("en-IE", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })
    : null;

  const critical = countBySeverity(disruptions, "CRITICAL");
  const high = countBySeverity(disruptions, "HIGH");
  const medium = countBySeverity(disruptions, "MEDIUM");
  const low = countBySeverity(disruptions, "LOW");

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
      {/* Page header */}
      <Box sx={{ flexShrink: 0 }}>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <WarningAmberIcon sx={{ color: "#EF4444", fontSize: 20 }} />
          <Typography
            variant="h6"
            fontWeight={700}
            sx={{ color: "text.primary", letterSpacing: -0.3 }}
          >
            Live Disruptions
          </Typography>
          {!isLoading && (
            <Box
              sx={{ display: "flex", alignItems: "center", gap: 0.5, ml: 1 }}
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
              <Typography sx={{ fontSize: "0.62rem", color: "text.secondary" }}>
                {disruptions.length > 0
                  ? `${disruptions.length} active`
                  : "No disruptions"}
              </Typography>
            </Box>
          )}
        </Box>
        <Typography variant="caption" sx={{ color: "text.secondary" }}>
          Auto-detected every 5 min · Network-wide impact
          {lastUpdated ? ` · Updated ${lastUpdated}` : ""}
        </Typography>
      </Box>

      {/* KPI strip */}
      <Box sx={{ display: "flex", gap: 1.5, flexShrink: 0, flexWrap: "wrap" }}>
        <KpiCard label="Total" value={disruptions.length} color="#6B7280" />
        <KpiCard label="Critical" value={critical} color="#7C3AED" />
        <KpiCard label="High" value={high} color="#EF4444" />
        <KpiCard label="Medium" value={medium} color="#F59E0B" />
        <KpiCard label="Low" value={low} color="#10B981" />
      </Box>

      {error && (
        <Alert severity="error" sx={{ flexShrink: 0 }}>
          Failed to load disruptions
        </Alert>
      )}

      {/* Main grid */}
      <Box
        sx={{
          flex: 1,
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" },
          gridTemplateRows: { xs: "auto auto auto", md: "1fr 1fr" },
          gap: 2,
          overflow: "hidden",
        }}
      >
        {/* ── Disruption list (spans both rows on desktop) ── */}
        <Paper
          elevation={0}
          sx={{
            gridRow: { md: "1 / 3" },
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
                background:
                  "linear-gradient(135deg, #EF4444 0%, #EF4444bb 100%)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                boxShadow: "0 2px 8px #EF444444",
                flexShrink: 0,
              }}
            >
              <WarningAmberIcon sx={{ fontSize: 18, color: "#fff" }} />
            </Box>
            <Box>
              <Typography
                variant="subtitle1"
                fontWeight={700}
                sx={{ color: "text.primary", lineHeight: 1.2 }}
              >
                Active Disruptions
              </Typography>
              <Typography
                variant="caption"
                sx={{ color: "text.secondary", fontSize: "0.62rem" }}
              >
                {disruptions.length} disruption
                {disruptions.length === 1 ? "" : "s"} detected
              </Typography>
            </Box>
          </Box>

          {/* List */}
          {isLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
              <CircularProgress size={24} sx={{ color: "#EF4444" }} />
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {disruptions.length === 0 ? (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <CheckCircleOutlineIcon
                    sx={{ fontSize: 32, color: "#10B981", mb: 1 }}
                  />
                  <Typography
                    sx={{ fontSize: "0.8rem", color: "text.secondary" }}
                  >
                    No active disruptions
                  </Typography>
                  <Typography
                    sx={{ fontSize: "0.7rem", color: "text.disabled", mt: 0.5 }}
                  >
                    Network is operating normally
                  </Typography>
                </Box>
              ) : (
                disruptions.map((d, idx) => (
                  <Box key={d.id}>
                    <DisruptionRow
                      disruption={d}
                      selected={selectedId === d.id}
                      onClick={() =>
                        setSelectedId(selectedId === d.id ? null : d.id)
                      }
                    />
                    {idx < disruptions.length - 1 && (
                      <Divider sx={{ borderColor: "rgba(0,0,0,0.05)" }} />
                    )}
                  </Box>
                ))
              )}
            </Box>
          )}

          <Box
            sx={{
              px: 2,
              py: 1,
              borderTop: "1px solid rgba(0,0,0,0.08)",
              flexShrink: 0,
            }}
          >
            <Typography
              variant="caption"
              sx={{ color: "text.disabled", fontSize: "0.62rem" }}
            >
              Source: Scheduled Detection · Refreshed every 5 min
            </Typography>
          </Box>
        </Paper>

        {/* ── Network Impact Map ── */}
        <Paper
          elevation={0}
          sx={{
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            bgcolor: "background.paper",
            border: "1px solid rgba(0,0,0,0.08)",
            borderRadius: 2,
            minHeight: 280,
          }}
        >
          <Box
            sx={{
              px: 2,
              py: 1.25,
              borderBottom: "1px solid rgba(0,0,0,0.08)",
              flexShrink: 0,
            }}
          >
            <Typography variant="subtitle2" fontWeight={700}>
              Network Impact Map
            </Typography>
            <Typography sx={{ fontSize: "0.62rem", color: "text.secondary" }}>
              Disruption locations across Dublin
            </Typography>
          </Box>
          <Box sx={{ flex: 1, overflow: "hidden" }}>
            <NetworkImpactMap disruptions={disruptions} selectedId={selectedId} />
          </Box>
        </Paper>

        {/* ── Detail Panel / Ripple Visualization ── */}
        <Paper
          elevation={0}
          sx={{
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
            bgcolor: "background.paper",
            border: "1px solid rgba(0,0,0,0.08)",
            borderRadius: 2,
            minHeight: 200,
          }}
        >
          {selectedId != null ? (
            <DetailPanel
              disruptionId={selectedId}
              onClose={() => setSelectedId(null)}
            />
          ) : (
            <>
              <Box
                sx={{
                  px: 2,
                  py: 1.25,
                  borderBottom: "1px solid rgba(0,0,0,0.08)",
                  flexShrink: 0,
                }}
              >
                <Typography variant="subtitle2" fontWeight={700}>
                  Mode Impact
                </Typography>
                <Typography sx={{ fontSize: "0.62rem", color: "text.secondary" }}>
                  Click a disruption to see causes & alternatives
                </Typography>
              </Box>
              <Box sx={{ flex: 1, overflow: "hidden" }}>
                <RippleEffectVisualization disruptions={disruptions} />
              </Box>
            </>
          )}
        </Paper>
      </Box>
    </Box>
  );
};
