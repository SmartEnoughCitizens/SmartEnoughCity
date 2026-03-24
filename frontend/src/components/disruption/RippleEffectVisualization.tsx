/**
 * RippleEffectVisualization — SVG-based animated ripple chart showing severity
 * impact across transport modes. Uses CSS keyframe animations for the pulse effect.
 */

import { Box, Typography, Chip } from "@mui/material";
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
      if (!counts[m]) counts[m] = { count: 0, max: 0 };
      counts[m].count += 1;
      counts[m].max = Math.max(counts[m].max, score);
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
  const totalActive = disruptions.length;
  const maxCount = Math.max(...impacts.map((i) => i.count), 1);

  return (
    <Box
      sx={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        p: 2,
        gap: 2,
      }}
    >
      {/* Central pulse node */}
      <Box sx={{ display: "flex", justifyContent: "center", pt: 1 }}>
        <Box sx={{ position: "relative", width: 80, height: 80 }}>
          {/* Animated ripple rings */}
          {[0, 1, 2].map((i) => (
            <Box
              key={i}
              sx={{
                position: "absolute",
                inset: 0,
                borderRadius: "50%",
                border: `2px solid ${totalActive > 0 ? "#EF4444" : "#9CA3AF"}`,
                opacity: 0,
                animation:
                  totalActive > 0
                    ? `ripple 2s ease-out ${i * 0.6}s infinite`
                    : "none",
                "@keyframes ripple": {
                  "0%": { transform: "scale(0.5)", opacity: 0.8 },
                  "100%": { transform: "scale(2.2)", opacity: 0 },
                },
              }}
            />
          ))}
          {/* Core node */}
          <Box
            sx={{
              position: "absolute",
              inset: 16,
              borderRadius: "50%",
              bgcolor: totalActive > 0 ? "#EF4444" : "#9CA3AF",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              boxShadow:
                totalActive > 0 ? "0 0 12px rgba(239,68,68,0.5)" : "none",
              transition: "all 0.3s",
            }}
          >
            <Typography
              sx={{ color: "#fff", fontWeight: 800, fontSize: "1rem" }}
            >
              {totalActive}
            </Typography>
          </Box>
        </Box>
      </Box>

      <Typography
        sx={{
          textAlign: "center",
          fontSize: "0.72rem",
          color: "text.secondary",
          mt: -1,
        }}
      >
        active disruption{totalActive === 1 ? "" : "s"}
      </Typography>

      {/* Mode impact bars */}
      <Box sx={{ display: "flex", flexDirection: "column", gap: 1, flex: 1 }}>
        {impacts.map(({ mode, count, maxSeverity }) => {
          const color = MODE_COLORS[mode];
          const barPct = maxCount > 0 ? (count / maxCount) * 100 : 0;
          const isAffected = count > 0;

          return (
            <Box
              key={mode}
              sx={{ display: "flex", alignItems: "center", gap: 1 }}
            >
              {/* Mode label */}
              <Typography
                sx={{
                  fontSize: "0.75rem",
                  fontWeight: 500,
                  color: isAffected ? "text.primary" : "text.disabled",
                  width: 40,
                  flexShrink: 0,
                }}
              >
                {MODE_LABELS[mode]}
              </Typography>

              {/* Bar */}
              <Box
                sx={{
                  flex: 1,
                  height: 10,
                  borderRadius: 5,
                  bgcolor: "rgba(0,0,0,0.06)",
                  overflow: "hidden",
                  position: "relative",
                }}
              >
                <Box
                  sx={{
                    height: "100%",
                    width: `${Math.max(barPct, isAffected ? 4 : 0)}%`,
                    bgcolor: color,
                    borderRadius: 5,
                    transition: "width 0.5s ease",
                    position: "relative",
                    "&::after": isAffected
                      ? {
                          content: '""',
                          position: "absolute",
                          right: 0,
                          top: 0,
                          bottom: 0,
                          width: "30%",
                          background: `linear-gradient(to right, transparent, ${color}99)`,
                          animation: "shimmer 1.5s ease-in-out infinite",
                        }
                      : {},
                    "@keyframes shimmer": {
                      "0%, 100%": { opacity: 0.3 },
                      "50%": { opacity: 1 },
                    },
                  }}
                />
              </Box>

              {/* Count + severity chip */}
              <Box
                sx={{
                  display: "flex",
                  alignItems: "center",
                  gap: 0.5,
                  width: 56,
                }}
              >
                {isAffected ? (
                  <>
                    <Typography
                      sx={{ fontSize: "0.7rem", fontWeight: 700, color }}
                    >
                      {count}
                    </Typography>
                    <Chip
                      size="small"
                      label={["", "LOW", "MED", "HIGH", "CRIT"][maxSeverity]}
                      sx={{
                        fontSize: "0.55rem",
                        height: 14,
                        bgcolor: color + "22",
                        color,
                        border: `1px solid ${color}44`,
                        px: 0.25,
                      }}
                    />
                  </>
                ) : (
                  <Typography
                    sx={{ fontSize: "0.68rem", color: "text.disabled" }}
                  >
                    —
                  </Typography>
                )}
              </Box>
            </Box>
          );
        })}
      </Box>

      <Typography
        sx={{
          fontSize: "0.6rem",
          color: "text.disabled",
          textAlign: "center",
          flexShrink: 0,
        }}
      >
        Auto-detected · refreshes every 30 s
      </Typography>
    </Box>
  );
};
