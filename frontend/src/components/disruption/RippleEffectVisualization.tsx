/**
 * ModeImpactPanel — compact mode-by-mode disruption breakdown.
 * Replaces the old ripple SVG which had sizing and overflow issues.
 */

import { Box, Typography } from "@mui/material";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import type { ActiveDisruption, TransportMode } from "@/types";

const MODE_COLORS: Record<TransportMode, string> = {
  BUS: "#3B82F6",
  TRAM: "#10B981",
  TRAIN: "#F59E0B",
  CAR: "#EF4444",
  CYCLE: "#8B5CF6",
};

const MODE_LABELS: Record<TransportMode, string> = {
  BUS: "Bus",
  TRAM: "Tram",
  TRAIN: "Train",
  CAR: "Car",
  CYCLE: "Cycle",
};

const SEVERITY_LABELS = ["", "Low", "Med", "High", "Crit"];
const SEVERITY_COLORS_ARR = [
  "",
  "#10B981",
  "#F59E0B",
  "#EF4444",
  "#7C3AED",
];

const ALL_MODES: TransportMode[] = ["BUS", "TRAM", "TRAIN", "CAR", "CYCLE"];

interface ModeImpact {
  mode: TransportMode;
  count: number;
  maxSeverity: number;
}

function severityScore(s: string): number {
  switch (s) {
    case "CRITICAL": {
      return 4;
    }
    case "HIGH": {
      return 3;
    }
    case "MEDIUM": {
      return 2;
    }
    default: {
      return 1;
    }
  }
}

function computeImpacts(disruptions: ActiveDisruption[]): ModeImpact[] {
  const counts: Partial<Record<TransportMode, { count: number; max: number }>> =
    {};

  for (const d of disruptions) {
    const modes = d.affectedTransportModes ?? [];
    const score = severityScore(d.severity);
    for (const m of modes) {
      if (!counts[m])
        counts[m] = { count: 0, max: 0 };
      counts[m].count += 1;
      counts[m].max = Math.max(
        counts[m].max,
        score,
      );
    }
  }

  return ALL_MODES.map((mode) => ({
    mode,
    count: counts[mode]?.count ?? 0,
    maxSeverity: counts[mode]?.max ?? 0,
  }));
}

interface Props {
  disruptions: ActiveDisruption[];
}

export const RippleEffectVisualization = ({ disruptions }: Props) => {
  const impacts = computeImpacts(disruptions);
  const maxCount = Math.max(...impacts.map((i) => i.count), 1);
  const affected = impacts.filter((i) => i.count > 0);
  const allClear = affected.length === 0;

  return (
    <Box
      sx={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        px: 2,
        py: 1.25,
        gap: 1.25,
        boxSizing: "border-box",
      }}
    >
      {/* Mode status cards — one per mode */}
      <Box sx={{ display: "flex", gap: 0.75, flexShrink: 0 }}>
        {impacts.map(({ mode, count, maxSeverity }) => {
          const color = MODE_COLORS[mode];
          const sevColor = SEVERITY_COLORS_ARR[maxSeverity] ?? color;
          const isAffected = count > 0;
          return (
            <Box
              key={mode}
              sx={{
                flex: 1,
                borderRadius: 2,
                border: `1px solid ${isAffected ? color + "55" : "rgba(0,0,0,0.07)"}`,
                bgcolor: isAffected ? color + "0e" : "rgba(0,0,0,0.02)",
                px: 0.75,
                py: 0.75,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                gap: 0.25,
                transition: "border-color 0.2s, background-color 0.2s",
              }}
            >
              <Box
                sx={{
                  width: 6,
                  height: 6,
                  borderRadius: "50%",
                  bgcolor: isAffected ? color : "#9CA3AF",
                  boxShadow: isAffected ? `0 0 4px ${color}99` : "none",
                  transition: "all 0.2s",
                }}
              />
              <Typography
                sx={{
                  fontSize: "0.58rem",
                  fontWeight: 700,
                  color: isAffected ? color : "text.disabled",
                  textTransform: "uppercase",
                  letterSpacing: 0.4,
                  lineHeight: 1,
                }}
              >
                {MODE_LABELS[mode]}
              </Typography>
              <Typography
                sx={{
                  fontSize: "1rem",
                  fontWeight: 800,
                  color: isAffected ? color : "text.disabled",
                  lineHeight: 1,
                }}
              >
                {count}
              </Typography>
              <Typography
                sx={{
                  fontSize: "0.55rem",
                  color: isAffected ? sevColor : "text.disabled",
                  lineHeight: 1,
                }}
              >
                {isAffected ? (SEVERITY_LABELS[maxSeverity] ?? "—") : "—"}
              </Typography>
            </Box>
          );
        })}
      </Box>

      {/* Proportional bars for affected modes only */}
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          gap: 0.55,
          flex: 1,
          justifyContent: allClear ? "center" : "flex-start",
        }}
      >
        {allClear ? (
          <Box
            sx={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 0.5,
            }}
          >
            <CheckCircleOutlineIcon
              sx={{ fontSize: 22, color: "#10B981" }}
            />
            <Typography
              sx={{ fontSize: "0.72rem", color: "text.secondary" }}
            >
              All modes operating normally
            </Typography>
          </Box>
        ) : (
          affected.map(({ mode, count }) => {
            const color = MODE_COLORS[mode];
            const pct = (count / maxCount) * 100;
            return (
              <Box
                key={mode}
                sx={{ display: "flex", alignItems: "center", gap: 1 }}
              >
                <Typography
                  sx={{
                    fontSize: "0.62rem",
                    color,
                    width: 34,
                    flexShrink: 0,
                    fontWeight: 600,
                  }}
                >
                  {MODE_LABELS[mode]}
                </Typography>
                <Box
                  sx={{
                    flex: 1,
                    height: 7,
                    borderRadius: 4,
                    bgcolor: "rgba(0,0,0,0.06)",
                    overflow: "hidden",
                  }}
                >
                  <Box
                    sx={{
                      height: "100%",
                      width: `${Math.max(pct, 6)}%`,
                      bgcolor: color,
                      borderRadius: 4,
                      transition: "width 0.45s ease",
                    }}
                  />
                </Box>
                <Typography
                  sx={{
                    fontSize: "0.62rem",
                    color,
                    fontWeight: 700,
                    width: 14,
                    textAlign: "right",
                    flexShrink: 0,
                  }}
                >
                  {count}
                </Typography>
              </Box>
            );
          })
        )}
      </Box>
    </Box>
  );
};
