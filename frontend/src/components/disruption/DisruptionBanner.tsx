/**
 * DisruptionBanner — Slim sticky alert strip for Bus/Train/Tram dashboards.
 * Polls GET /disruptions/transport/{mode} every 30s.
 * Renders nothing when there are no active disruptions.
 */

import { useQuery } from "@tanstack/react-query";
import { Box, Typography } from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import { dashboardApi } from "@/api";
import type { TransportMode } from "@/types";

interface Props {
  mode: TransportMode;
  onViewDetails?: () => void;
}

export const DisruptionBanner = ({ mode, onViewDetails }: Props) => {
  const { data: disruptions = [] } = useQuery({
    queryKey: ["disruptions", "by-mode", mode],
    queryFn: () => dashboardApi.getDisruptionsByMode(mode),
    staleTime: 30_000,
    refetchInterval: 30_000,
  });

  if (disruptions.length === 0) return null;

  const hasCritical = disruptions.some((d) => d.severity === "CRITICAL");
  const hasHigh = disruptions.some((d) => d.severity === "HIGH");
  const bgColor = hasCritical ? "#7C3AED" : hasHigh ? "#EF4444" : "#F59E0B";

  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 1,
        px: 2,
        py: 0.75,
        bgcolor: bgColor,
        color: "#fff",
        flexShrink: 0,
        borderRadius: 1,
        mx: 2,
        mb: 1,
      }}
    >
      <Box
        sx={{
          width: 7,
          height: 7,
          borderRadius: "50%",
          bgcolor: "#fff",
          flexShrink: 0,
          animation: "bannerPulse 1.5s ease-in-out infinite",
          "@keyframes bannerPulse": {
            "0%, 100%": { opacity: 1 },
            "50%": { opacity: 0.35 },
          },
        }}
      />
      <WarningAmberIcon sx={{ fontSize: 15, flexShrink: 0 }} />
      <Typography sx={{ fontSize: "0.78rem", fontWeight: 600, flex: 1 }}>
        {disruptions.length} active disruption
        {disruptions.length === 1 ? "" : "s"} affecting {mode} services
      </Typography>
      {onViewDetails && (
        <Box
          onClick={onViewDetails}
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 0.4,
            cursor: "pointer",
            opacity: 0.9,
            "&:hover": { opacity: 1 },
            flexShrink: 0,
          }}
        >
          <Typography sx={{ fontSize: "0.75rem", fontWeight: 600 }}>
            View Details
          </Typography>
          <ArrowForwardIcon sx={{ fontSize: 13 }} />
        </Box>
      )}
    </Box>
  );
};
