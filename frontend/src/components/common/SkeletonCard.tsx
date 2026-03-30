/**
 * Skeleton placeholder cards for dashboard loading states
 */

import { Box, Skeleton } from "@mui/material";

interface SkeletonCardProps {
  variant: "stat" | "table" | "chart";
}

export const SkeletonCard = ({ variant }: SkeletonCardProps) => {
  if (variant === "stat") {
    return (
      <Box sx={{ px: 1, py: 0.5 }}>
        {/* Header row: small icon + title text + icon button */}
        <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
          <Skeleton variant="rectangular" width={16} height={16} />
          <Skeleton variant="text" width="60%" />
          <Box sx={{ ml: "auto" }}>
            <Skeleton variant="circular" width={24} height={24} />
          </Box>
        </Box>
        {/* Footer caption */}
        <Skeleton variant="text" width="80%" />
      </Box>
    );
  }

  if (variant === "table") {
    return (
      <Box
        sx={{
          px: 1,
          py: 0.5,
          display: "flex",
          flexDirection: "column",
          gap: 1,
        }}
      >
        <Skeleton variant="text" width="100%" />
        <Skeleton variant="text" width="90%" />
        <Skeleton variant="text" width="95%" />
        <Skeleton variant="text" width="85%" />
      </Box>
    );
  }

  // chart
  return (
    <Box sx={{ px: 1, py: 0.5 }}>
      <Skeleton variant="rectangular" height={180} sx={{ borderRadius: 1 }} />
    </Box>
  );
};
