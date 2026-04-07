/**
 * DisruptionsTabContent — reusable disruption list for transport dashboard side panels.
 * Fetches by transport mode; calls onSelect(disruption) when a row is clicked.
 */

import { useQuery } from "@tanstack/react-query";
import { Box, Chip, CircularProgress, Typography } from "@mui/material";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import { dashboardApi } from "@/api";
import type { DisruptionItem, TransportMode } from "@/types";

const SEVERITY_COLORS: Record<string, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

interface Props {
  mode: TransportMode;
  selectedId?: number | null;
  onSelect: (d: DisruptionItem) => void;
}

export const DisruptionsTabContent = ({
  mode,
  selectedId,
  onSelect,
}: Props) => {
  const { data: disruptions = [], isLoading } = useQuery({
    queryKey: ["disruptions", "by-mode", mode],
    queryFn: () => dashboardApi.getDisruptionsByMode(mode),
    staleTime: 30_000,
    refetchInterval: 30_000,
  });

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
        <CircularProgress size={20} sx={{ color: "#EF4444" }} />
      </Box>
    );
  }

  if (disruptions.length === 0) {
    return (
      <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
        <CheckCircleOutlineIcon
          sx={{ fontSize: 28, color: "#10B981", mb: 0.75 }}
        />
        <Typography sx={{ fontSize: "0.8rem", color: "#8b949e" }}>
          No active disruptions
        </Typography>
        <Typography sx={{ fontSize: "0.7rem", color: "#6e7681", mt: 0.3 }}>
          {mode} services operating normally
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      {disruptions.map((d) => {
        const color = SEVERITY_COLORS[d.severity] ?? "#6B7280";
        const selected = selectedId === d.id;
        return (
          <Box
            key={d.id}
            onClick={() => onSelect(d)}
            sx={{
              px: 2,
              py: 1.25,
              cursor: "pointer",
              borderLeft: `3px solid ${selected ? color : "transparent"}`,
              bgcolor: selected ? `${color}18` : "transparent",
              "&:hover": { bgcolor: "rgba(255,255,255,0.04)" },
              transition: "all 0.12s",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1 }}>
              <Box
                sx={{
                  mt: 0.5,
                  width: 8,
                  height: 8,
                  borderRadius: "50%",
                  bgcolor: color,
                  flexShrink: 0,
                  boxShadow: `0 0 5px ${color}88`,
                }}
              />
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography
                  noWrap
                  sx={{
                    fontSize: "0.82rem",
                    fontWeight: selected ? 600 : 400,
                    color: "#e6edf3",
                  }}
                >
                  {d.affectedArea ?? d.name}
                </Typography>
                <Box
                  sx={{ display: "flex", gap: 0.5, mt: 0.3, flexWrap: "wrap" }}
                >
                  <Chip
                    size="small"
                    label={d.severity}
                    sx={{
                      fontSize: "0.58rem",
                      height: 15,
                      bgcolor: `${color}22`,
                      color,
                      border: `1px solid ${color}44`,
                    }}
                  />
                  <Chip
                    size="small"
                    label={
                      d.disruptionType === "EVENT"
                        ? "Service Pressure"
                        : d.disruptionType.replaceAll("_", " ")
                    }
                    sx={{
                      fontSize: "0.58rem",
                      height: 15,
                      bgcolor: "rgba(48,54,61,0.6)",
                      color: "#8b949e",
                    }}
                  />
                  {(d.disruptionType === "CONGESTION" ||
                    d.disruptionType === "EVENT") &&
                    (d.affectedRoutes ?? []).slice(0, 3).map((r) => (
                      <Chip
                        key={r}
                        size="small"
                        label={r}
                        sx={{
                          fontSize: "0.55rem",
                          height: 15,
                          bgcolor: "rgba(48,54,61,0.4)",
                          color: "#6e7681",
                        }}
                      />
                    ))}
                  {(d.disruptionType === "CONGESTION" ||
                    d.disruptionType === "EVENT") &&
                    (d.affectedRoutes ?? []).length > 3 && (
                      <Typography
                        sx={{
                          fontSize: "0.55rem",
                          color: "#6e7681",
                          alignSelf: "center",
                        }}
                      >
                        +{(d.affectedRoutes ?? []).length - 3} more
                      </Typography>
                    )}
                </Box>
                {d.delayMinutes != null && d.delayMinutes > 0 && (
                  <Typography sx={{ fontSize: "0.68rem", color, mt: 0.2 }}>
                    +{d.delayMinutes} min delay
                  </Typography>
                )}
              </Box>
            </Box>
          </Box>
        );
      })}
    </Box>
  );
};
