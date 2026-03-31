import { useState } from "react";
import {
  Box,
  Chip,
  CircularProgress,
  Drawer,
  Tab,
  Tabs,
  Typography,
} from "@mui/material";
import { useCommonDelays, useBusRouteBreakdown } from "@/hooks/useDashboard";
import type { BusCommonDelay } from "@/types";

type FilterType = "today" | "week" | "month";

const delayColor = (minutes: number): "success" | "warning" | "error" =>
  minutes > 10 ? "error" : minutes > 5 ? "warning" : "success";

export const DelayLeaderboard = () => {
  const [filter, setFilter] = useState<FilterType>("today");
  const [selectedRoute, setSelectedRoute] = useState<BusCommonDelay | null>(
    null,
  );

  const { data: delays = [], isLoading } = useCommonDelays(filter);
  const { data: breakdown = [] } = useBusRouteBreakdown(
    selectedRoute?.routeId ?? null,
    filter,
  );

  return (
    <Box>
      <Tabs
        value={filter}
        onChange={(_, v) => setFilter(v as FilterType)}
        sx={{ mb: 1 }}
        textColor="inherit"
      >
        <Tab
          label="Today"
          value="today"
          sx={{ fontSize: "0.75rem", minHeight: 36 }}
        />
        <Tab
          label="This Week"
          value="week"
          sx={{ fontSize: "0.75rem", minHeight: 36 }}
        />
        <Tab
          label="This Month"
          value="month"
          sx={{ fontSize: "0.75rem", minHeight: 36 }}
        />
      </Tabs>

      {isLoading ? (
        <Box sx={{ display: "flex", justifyContent: "center", py: 2 }}>
          <CircularProgress size={24} />
        </Box>
      ) : delays.length === 0 ? (
        <Typography variant="body2" sx={{ opacity: 0.6, py: 1 }}>
          No delay data for this period.
        </Typography>
      ) : (
        <Box
          component="table"
          sx={{ width: "100%", borderCollapse: "collapse", fontSize: "0.8rem" }}
        >
          <Box component="thead">
            <Box component="tr">
              <Box
                component="th"
                sx={{ textAlign: "left", p: "4px 6px", opacity: 0.7 }}
              >
                #
              </Box>
              <Box
                component="th"
                sx={{ textAlign: "left", p: "4px 6px", opacity: 0.7 }}
              >
                Route
              </Box>
              <Box
                component="th"
                sx={{ textAlign: "left", p: "4px 6px", opacity: 0.7 }}
              >
                Avg Delay
              </Box>
            </Box>
          </Box>
          <Box component="tbody">
            {delays.map((row, idx) => (
              <Box
                component="tr"
                key={row.routeId}
                onClick={() => setSelectedRoute(row)}
                sx={{
                  cursor: "pointer",
                  "&:hover": { bgcolor: "rgba(255,255,255,0.05)" },
                  borderRadius: 1,
                }}
              >
                <Box component="td" sx={{ p: "4px 6px", opacity: 0.6 }}>
                  {idx + 1}
                </Box>
                <Box component="td" sx={{ p: "4px 6px" }}>
                  <strong>{row.routeShortName}</strong>{" "}
                  <span style={{ fontSize: "0.75em", opacity: 0.65 }}>
                    {row.routeLongName}
                  </span>
                </Box>
                <Box component="td" sx={{ p: "4px 6px" }}>
                  <Chip
                    label={`${row.avgDelayMinutes} min`}
                    color={delayColor(row.avgDelayMinutes)}
                    size="small"
                  />
                </Box>
              </Box>
            ))}
          </Box>
        </Box>
      )}

      {/* Per-stop breakdown drawer */}
      <Drawer
        anchor="right"
        open={!!selectedRoute}
        onClose={() => setSelectedRoute(null)}
      >
        <Box sx={{ width: 380, p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Route {selectedRoute?.routeShortName}
          </Typography>
          <Typography variant="body2" sx={{ mb: 2, opacity: 0.65 }}>
            {selectedRoute?.routeLongName}
          </Typography>
          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            Per-Stop Delay Breakdown
          </Typography>

          {breakdown.length === 0 ? (
            <Typography variant="body2" sx={{ opacity: 0.6 }}>
              No stop-level data available.
            </Typography>
          ) : (
            <Box
              component="table"
              sx={{
                width: "100%",
                borderCollapse: "collapse",
                fontSize: "0.8rem",
              }}
            >
              <Box component="thead">
                <Box component="tr">
                  <Box
                    component="th"
                    sx={{ textAlign: "left", p: "4px", opacity: 0.7 }}
                  >
                    Stop ID
                  </Box>
                  <Box
                    component="th"
                    sx={{ textAlign: "left", p: "4px", opacity: 0.7 }}
                  >
                    Avg
                  </Box>
                  <Box
                    component="th"
                    sx={{ textAlign: "left", p: "4px", opacity: 0.7 }}
                  >
                    Max
                  </Box>
                  <Box
                    component="th"
                    sx={{ textAlign: "left", p: "4px", opacity: 0.7 }}
                  >
                    Trips
                  </Box>
                </Box>
              </Box>
              <Box component="tbody">
                {breakdown.map((b) => (
                  <Box component="tr" key={b.stopId}>
                    <Box component="td" sx={{ p: "4px" }}>
                      {b.stopId}
                    </Box>
                    <Box component="td" sx={{ p: "4px" }}>
                      <Chip
                        label={`${b.avgDelayMinutes}m`}
                        color={delayColor(b.avgDelayMinutes)}
                        size="small"
                      />
                    </Box>
                    <Box component="td" sx={{ p: "4px" }}>
                      {b.maxDelayMinutes}m
                    </Box>
                    <Box component="td" sx={{ p: "4px" }}>
                      {b.tripCount}
                    </Box>
                  </Box>
                ))}
              </Box>
            </Box>
          )}
        </Box>
      </Drawer>
    </Box>
  );
};
