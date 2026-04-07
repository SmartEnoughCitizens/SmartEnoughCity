import {
  Box,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import type { StationRiskScoreDTO } from "@/types";

// ── Risk level helpers ────────────────────────────────────────────────────────

type RiskLevel = "VERY_HIGH" | "HIGH" | "MODERATE" | "LOW";

function emptyRiskLevel(p: number): RiskLevel {
  if (p >= 0.995) return "VERY_HIGH";
  if (p >= 0.6) return "HIGH";
  if (p >= 0.3) return "MODERATE";
  return "LOW";
}

function fullRiskLevel(p: number): RiskLevel {
  if (p >= 0.995) return "VERY_HIGH";
  if (p >= 0.6) return "HIGH";
  if (p >= 0.3) return "MODERATE";
  return "LOW";
}

const RISK_META: Record<
  RiskLevel,
  { label: string; color: string; bg: string }
> = {
  VERY_HIGH: { label: "Very High Risk", color: "#ef4444", bg: "#fef2f2" },
  HIGH: { label: "High Risk", color: "#f97316", bg: "#fff7ed" },
  MODERATE: { label: "Moderate Risk", color: "#eab308", bg: "#fefce8" },
  LOW: { label: "Low Risk", color: "#22c55e", bg: "#f0fdf4" },
};

function emptyDescription(level: RiskLevel, name: string): string {
  switch (level) {
    case "VERY_HIGH": {
      return `${name} is very likely to run out of bikes within 2 hours.`;
    }
    case "HIGH": {
      return `${name} may run out of bikes within 2 hours.`;
    }
    case "MODERATE": {
      return `${name} has a moderate chance of running low on bikes.`;
    }
    default: {
      return `${name} has sufficient bikes available.`;
    }
  }
}

function fullDescription(level: RiskLevel, name: string): string {
  switch (level) {
    case "VERY_HIGH": {
      return `${name} is very likely to have no free docks within 2 hours.`;
    }
    case "HIGH": {
      return `${name} may fill up and have no free docks within 2 hours.`;
    }
    case "MODERATE": {
      return `${name} has a moderate chance of filling up.`;
    }
    default: {
      return `${name} has sufficient free docks available.`;
    }
  }
}

// ── Sub-components ────────────────────────────────────────────────────────────

function RiskBadge({ level }: { level: RiskLevel }) {
  const meta = RISK_META[level];
  return (
    <Chip
      size="small"
      label={meta.label}
      sx={{
        fontSize: "0.65rem",
        fontWeight: 700,
        color: meta.color,
        bgcolor: meta.bg,
        border: `1px solid ${meta.color}33`,
        height: 20,
      }}
    />
  );
}

function RiskBar({ value }: { value: number }) {
  const pct = Math.round(value * 100);
  const color =
    pct >= 100
      ? "#ef4444"
      : pct >= 60
        ? "#f97316"
        : pct >= 30
          ? "#eab308"
          : "#22c55e";
  return (
    <Tooltip title={`${pct}% probability`}>
      <Box
        sx={{ display: "flex", alignItems: "center", gap: 0.75, minWidth: 80 }}
      >
        <Box
          sx={{
            flex: 1,
            height: 6,
            borderRadius: 99,
            bgcolor: "#e5e7eb",
            overflow: "hidden",
          }}
        >
          <Box
            sx={{
              width: `${pct}%`,
              height: "100%",
              bgcolor: color,
              borderRadius: 99,
              transition: "width 0.3s",
            }}
          />
        </Box>
        <Typography
          variant="caption"
          sx={{ fontWeight: 600, color, minWidth: 30 }}
        >
          {pct}%
        </Typography>
      </Box>
    </Tooltip>
  );
}

function RiskCard({ score }: { score: StationRiskScoreDTO }) {
  const eLevel = emptyRiskLevel(score.emptyRisk2h);
  const fLevel = fullRiskLevel(score.fullRisk2h);
  const primaryLevel = score.emptyRisk2h >= score.fullRisk2h ? eLevel : fLevel;

  return (
    <Box
      sx={{
        px: 1.5,
        py: 1.25,
        borderLeft: `3px solid ${RISK_META[primaryLevel].color}`,
        bgcolor: "background.paper",
        "&:hover": { bgcolor: "action.hover" },
      }}
    >
      <Box
        sx={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
          mb: 0.5,
        }}
      >
        <Typography
          variant="body2"
          fontWeight={600}
          sx={{ lineHeight: 1.3, flex: 1, mr: 1 }}
        >
          {score.name}
        </Typography>
        <RiskBadge level={primaryLevel} />
      </Box>

      {/* Empty risk row */}
      {eLevel !== "LOW" && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.25 }}>
          <DirectionsBikeIcon
            sx={{ fontSize: "0.8rem", color: "text.secondary" }}
          />
          <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
            {emptyDescription(eLevel, score.name)}
          </Typography>
        </Box>
      )}
      {fLevel !== "LOW" && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mb: 0.25 }}>
          <WarningAmberIcon
            sx={{ fontSize: "0.8rem", color: "text.secondary" }}
          />
          <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
            {fullDescription(fLevel, score.name)}
          </Typography>
        </Box>
      )}
      {eLevel === "LOW" && fLevel === "LOW" && (
        <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
          <CheckCircleOutlineIcon
            sx={{ fontSize: "0.8rem", color: "#22c55e" }}
          />
          <Typography variant="caption" color="text.secondary">
            Station is operating normally.
          </Typography>
        </Box>
      )}

      {/* Probability bars */}
      <Box sx={{ mt: 0.75, display: "flex", gap: 2 }}>
        <Box>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ fontSize: "0.6rem" }}
          >
            EMPTY RISK
          </Typography>
          <RiskBar value={score.emptyRisk2h} />
        </Box>
        <Box>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ fontSize: "0.6rem" }}
          >
            FULL RISK
          </Typography>
          <RiskBar value={score.fullRisk2h} />
        </Box>
      </Box>
    </Box>
  );
}

// ── Summary row ───────────────────────────────────────────────────────────────

function RiskSummaryRow({ scores }: { scores: StationRiskScoreDTO[] }) {
  const veryHigh = scores.filter(
    (s) =>
      emptyRiskLevel(s.emptyRisk2h) === "VERY_HIGH" ||
      fullRiskLevel(s.fullRisk2h) === "VERY_HIGH",
  ).length;
  const high = scores.filter((s) => {
    const e = emptyRiskLevel(s.emptyRisk2h);
    const f = fullRiskLevel(s.fullRisk2h);
    return (
      (e === "HIGH" || f === "HIGH") && e !== "VERY_HIGH" && f !== "VERY_HIGH"
    );
  }).length;
  const ok = scores.length - veryHigh - high;

  return (
    <Box sx={{ px: 1.5, py: 1, display: "flex", gap: 1.5 }}>
      {[
        { count: veryHigh, label: "Very High", color: "#ef4444" },
        { count: high, label: "High", color: "#f97316" },
        { count: ok, label: "Normal", color: "#22c55e" },
      ].map(({ count, label, color }) => (
        <Box key={label} sx={{ textAlign: "center" }}>
          <Typography
            variant="h6"
            fontWeight={700}
            sx={{ color, lineHeight: 1 }}
          >
            {count}
          </Typography>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ fontSize: "0.6rem" }}
          >
            {label}
          </Typography>
        </Box>
      ))}
      <Box sx={{ flex: 1 }} />
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{ alignSelf: "flex-end", fontSize: "0.6rem" }}
      >
        Updated every 5 min · 2-hour horizon
      </Typography>
    </Box>
  );
}

// ── Main panel ────────────────────────────────────────────────────────────────

interface Props {
  scores: StationRiskScoreDTO[];
  isLoading: boolean;
}

export function StationRiskPanel({ scores, isLoading }: Props) {
  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (scores.length === 0) {
    return (
      <Stack alignItems="center" spacing={1} sx={{ py: 4 }}>
        <DirectionsBikeIcon sx={{ fontSize: 32, color: "text.disabled" }} />
        <Typography variant="body2" color="text.secondary">
          No risk scores yet. The inference engine trains on startup.
        </Typography>
      </Stack>
    );
  }

  // Sort: very high empty first, then high, then others
  const sorted = scores.toSorted(
    (a, b) =>
      Math.max(b.emptyRisk2h, b.fullRisk2h) -
      Math.max(a.emptyRisk2h, a.fullRisk2h),
  );

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <RiskSummaryRow scores={scores} />
      <Divider />
      <Box sx={{ flex: 1, overflow: "auto" }}>
        {sorted.map((score, i) => (
          <Box key={score.stationId}>
            {i > 0 && <Divider />}
            <RiskCard score={score} />
          </Box>
        ))}
      </Box>
    </Box>
  );
}
