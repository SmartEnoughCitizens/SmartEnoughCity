/**
 * Illustrated empty state component for dashboard panels
 */

import type { ReactNode } from "react";
import { Box, Typography } from "@mui/material";

const BusIllustration = () => (
  <svg viewBox="0 0 120 120" width={80} height={80} fill="none" stroke="currentColor" strokeWidth={2}>
    <rect x="15" y="35" width="90" height="55" rx="8" />
    <rect x="25" y="45" width="20" height="16" rx="3" />
    <rect x="50" y="45" width="20" height="16" rx="3" />
    <rect x="75" y="45" width="20" height="16" rx="3" />
    <line x1="15" y1="72" x2="105" y2="72" />
    <circle cx="35" cy="92" r="8" />
    <circle cx="85" cy="92" r="8" />
    <rect x="45" y="28" width="30" height="10" rx="3" />
  </svg>
);

const CycleIllustration = () => (
  <svg viewBox="0 0 120 120" width={80} height={80} fill="none" stroke="currentColor" strokeWidth={2}>
    <circle cx="35" cy="75" r="22" />
    <circle cx="85" cy="75" r="22" />
    <circle cx="35" cy="75" r="5" fill="currentColor" />
    <circle cx="85" cy="75" r="5" fill="currentColor" />
    <polyline points="35,75 55,40 70,40 85,75" />
    <polyline points="55,40 35,75" />
    <polyline points="70,40 85,75" />
    <line x1="55" y1="40" x2="65" y2="55" />
  </svg>
);

const TrainIllustration = () => (
  <svg viewBox="0 0 120 120" width={80} height={80} fill="none" stroke="currentColor" strokeWidth={2}>
    <rect x="25" y="25" width="70" height="65" rx="10" />
    <rect x="35" y="35" width="20" height="18" rx="3" />
    <rect x="65" y="35" width="20" height="18" rx="3" />
    <line x1="25" y1="70" x2="95" y2="70" />
    <circle cx="40" cy="90" r="7" />
    <circle cx="80" cy="90" r="7" />
    <line x1="10" y1="100" x2="110" y2="100" />
    <line x1="40" y1="100" x2="40" y2="97" />
    <line x1="60" y1="100" x2="60" y2="97" />
    <line x1="80" y1="100" x2="80" y2="97" />
  </svg>
);

const GenericIllustration = () => (
  <svg viewBox="0 0 120 120" width={80} height={80} fill="none" stroke="currentColor" strokeWidth={2}>
    <circle cx="60" cy="50" r="28" />
    <line x1="60" y1="78" x2="60" y2="95" />
    <line x1="40" y1="95" x2="80" y2="95" />
    <line x1="60" y1="35" x2="60" y2="52" />
    <circle cx="60" cy="58" r="3" fill="currentColor" />
  </svg>
);

const illustrations = {
  bus: BusIllustration,
  cycle: CycleIllustration,
  train: TrainIllustration,
  generic: GenericIllustration,
};

interface EmptyStateProps {
  icon?: "bus" | "cycle" | "train" | "generic";
  title: string;
  description?: string;
  action?: ReactNode;
}

export const EmptyState = ({
  icon = "generic",
  title,
  description,
  action,
}: EmptyStateProps) => {
  const Illustration = illustrations[icon];

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        py: 4,
        px: 2,
        gap: 1,
        color: "text.secondary",
      }}
    >
      <Box sx={{ color: "text.secondary", opacity: 0.5 }}>
        <Illustration />
      </Box>
      <Typography variant="body2" fontWeight={600} color="text.secondary" align="center">
        {title}
      </Typography>
      {description && (
        <Typography variant="caption" color="text.disabled" align="center">
          {description}
        </Typography>
      )}
      {action}
    </Box>
  );
};
