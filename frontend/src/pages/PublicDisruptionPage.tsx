/**
 * PublicDisruptionPage — No-auth public page, accessible via QR code.
 * Displays disruption details in a large-format, screen-displayable layout.
 */

import { useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Box, Chip, CircularProgress, Typography } from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import PedalBikeIcon from "@mui/icons-material/PedalBike";
import EventIcon from "@mui/icons-material/Event";
import TrafficIcon from "@mui/icons-material/Traffic";
import CompareArrowsIcon from "@mui/icons-material/CompareArrows";
import { dashboardApi } from "@/api";
import type { DisruptionAlternative, DisruptionCause, DisruptionSeverity } from "@/types";

const SEVERITY_COLORS: Record<DisruptionSeverity, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

const CAUSE_ICONS: Record<string, React.ReactNode> = {
  EVENT: <EventIcon sx={{ fontSize: 20 }} />,
  CONGESTION: <TrafficIcon sx={{ fontSize: 20 }} />,
  CROSS_MODE: <CompareArrowsIcon sx={{ fontSize: 20 }} />,
};

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: "#EF4444",
  MEDIUM: "#F59E0B",
  LOW: "#10B981",
};

function altIcon(mode: string): React.ReactNode {
  const m = mode.toUpperCase();
  if (m.includes("BUS")) return <DirectionsBusIcon sx={{ fontSize: 20 }} />;
  if (m.includes("BIKE") || m.includes("CYCLE")) return <PedalBikeIcon sx={{ fontSize: 20 }} />;
  return <TrainIcon sx={{ fontSize: 20 }} />;
}

function formatTime(iso: string | null): string {
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

function CauseRow({ cause }: { cause: DisruptionCause }) {
  return (
    <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1.5, py: 0.75 }}>
      <Box sx={{ color: CONFIDENCE_COLORS[cause.confidence], mt: 0.1 }}>
        {CAUSE_ICONS[cause.causeType] ?? <WarningAmberIcon sx={{ fontSize: 20 }} />}
      </Box>
      <Box sx={{ flex: 1 }}>
        <Typography sx={{ fontSize: "1rem", color: "#1F2937" }}>
          {cause.causeDescription}
        </Typography>
      </Box>
      <Chip
        label={cause.confidence}
        size="small"
        sx={{
          fontSize: "0.7rem",
          bgcolor: CONFIDENCE_COLORS[cause.confidence] + "18",
          color: CONFIDENCE_COLORS[cause.confidence],
          border: `1px solid ${CONFIDENCE_COLORS[cause.confidence]}44`,
          fontWeight: 600,
        }}
      />
    </Box>
  );
}

function AlternativeRow({ alt }: { alt: DisruptionAlternative }) {
  return (
    <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1.5, py: 0.75 }}>
      <Box sx={{ color: "#6B7280", mt: 0.1 }}>{altIcon(alt.mode)}</Box>
      <Box sx={{ flex: 1 }}>
        <Typography sx={{ fontSize: "1rem", color: "#1F2937" }}>
          {alt.description}
        </Typography>
        {alt.etaMinutes != null && (
          <Typography sx={{ fontSize: "0.82rem", color: "#6B7280" }}>
            ~{alt.etaMinutes} min away
          </Typography>
        )}
      </Box>
      {alt.availabilityCount != null && (
        <Chip
          label={`${alt.availabilityCount} avail.`}
          size="small"
          sx={{ fontSize: "0.7rem" }}
        />
      )}
    </Box>
  );
}

export const PublicDisruptionPage = () => {
  const { id } = useParams<{ id: string }>();
  const numericId = id ? parseInt(id, 10) : null;

  const { data: disruption, isLoading, error } = useQuery({
    queryKey: ["public", "disruption", numericId],
    queryFn: () => dashboardApi.getPublicDisruption(numericId!),
    enabled: numericId != null && !isNaN(numericId),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh" }}>
        <CircularProgress sx={{ color: "#EF4444" }} />
      </Box>
    );
  }

  if (error || !disruption) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh", bgcolor: "#F9FAFB" }}>
        <Box sx={{ textAlign: "center" }}>
          <WarningAmberIcon sx={{ fontSize: 48, color: "#9CA3AF", mb: 2 }} />
          <Typography sx={{ fontSize: "1.1rem", color: "#6B7280" }}>
            Disruption not found or no longer active.
          </Typography>
        </Box>
      </Box>
    );
  }

  const severityColor = SEVERITY_COLORS[disruption.severity] ?? "#6B7280";
  const causes = disruption.causes ?? [];
  const alternatives = disruption.alternatives ?? [];

  return (
    <Box
      sx={{
        minHeight: "100vh",
        bgcolor: "#F9FAFB",
        display: "flex",
        alignItems: "flex-start",
        justifyContent: "center",
        p: { xs: 2, md: 4 },
      }}
    >
      <Box
        sx={{
          width: "100%",
          maxWidth: 680,
          bgcolor: "#fff",
          borderRadius: 3,
          boxShadow: "0 4px 24px rgba(0,0,0,0.10)",
          overflow: "hidden",
        }}
      >
        {/* Header bar */}
        <Box
          sx={{
            bgcolor: severityColor,
            px: 3,
            py: 2.5,
            display: "flex",
            alignItems: "center",
            gap: 1.5,
          }}
        >
          <WarningAmberIcon sx={{ color: "#fff", fontSize: 28 }} />
          <Typography sx={{ color: "#fff", fontWeight: 800, fontSize: "1.15rem", letterSpacing: -0.3 }}>
            SERVICE DISRUPTION
          </Typography>
          <Box sx={{ ml: "auto" }}>
            <Chip
              label={disruption.severity}
              sx={{
                bgcolor: "rgba(255,255,255,0.25)",
                color: "#fff",
                fontWeight: 700,
                fontSize: "0.75rem",
                border: "1px solid rgba(255,255,255,0.4)",
              }}
            />
          </Box>
        </Box>

        <Box sx={{ px: 3, pt: 2.5, pb: 1 }}>
          {/* Title */}
          <Typography sx={{ fontSize: "1.4rem", fontWeight: 800, color: "#111827", lineHeight: 1.2 }}>
            {disruption.name ?? disruption.disruptionType}
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mt: 0.75, flexWrap: "wrap" }}>
            {disruption.affectedArea && (
              <Typography sx={{ fontSize: "0.9rem", color: "#6B7280" }}>
                {disruption.affectedArea}
              </Typography>
            )}
            {disruption.detectedAt && (
              <Typography sx={{ fontSize: "0.9rem", color: "#9CA3AF" }}>
                · Detected {formatTime(disruption.detectedAt)}
              </Typography>
            )}
          </Box>
          {disruption.description && (
            <Typography sx={{ mt: 1.25, fontSize: "0.95rem", color: "#374151", lineHeight: 1.5 }}>
              {disruption.description}
            </Typography>
          )}
        </Box>

        {/* Divider */}
        <Box sx={{ mx: 3, borderTop: "1px solid #E5E7EB", my: 1.5 }} />

        {/* Causes */}
        {causes.length > 0 && (
          <Box sx={{ px: 3, pb: 1 }}>
            <Typography sx={{ fontSize: "0.8rem", fontWeight: 700, color: "#9CA3AF", textTransform: "uppercase", letterSpacing: 0.8, mb: 0.75 }}>
              Why is this happening?
            </Typography>
            {causes.map((c) => (
              <CauseRow key={c.id} cause={c} />
            ))}
          </Box>
        )}

        {causes.length > 0 && alternatives.length > 0 && (
          <Box sx={{ mx: 3, borderTop: "1px solid #E5E7EB", my: 1.5 }} />
        )}

        {/* Alternatives */}
        {alternatives.length > 0 && (
          <Box sx={{ px: 3, pb: 1 }}>
            <Typography sx={{ fontSize: "0.8rem", fontWeight: 700, color: "#9CA3AF", textTransform: "uppercase", letterSpacing: 0.8, mb: 0.75 }}>
              What to do
            </Typography>
            {alternatives.map((a) => (
              <AlternativeRow key={a.id} alt={a} />
            ))}
          </Box>
        )}

        {/* Footer */}
        <Box
          sx={{
            mx: 3,
            borderTop: "1px solid #E5E7EB",
            mt: 1.5,
            py: 2,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <Typography sx={{ fontSize: "0.78rem", color: "#9CA3AF" }}>
            This disruption is being actively managed
          </Typography>
          <Box
            sx={{
              width: 8,
              height: 8,
              borderRadius: "50%",
              bgcolor: severityColor,
              animation: "pulse 1.5s ease-in-out infinite",
              "@keyframes pulse": {
                "0%, 100%": { opacity: 1 },
                "50%": { opacity: 0.3 },
              },
            }}
          />
        </Box>
      </Box>
    </Box>
  );
};
