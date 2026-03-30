/**
 * Main dashboard — full-viewport map with floating stat overlays
 * Shows cycle station markers; bus + cycle stats as compact floating cards
 */

import { useState, useRef, useEffect } from "react";
import {
  Box,
  Paper,
  Typography,
  IconButton,
  Collapse,
} from "@mui/material";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import CountUp from "react-countup";
import { useBusData, useCycleData } from "@/hooks";
import { DelayChart } from "@/components/charts/DelayChart";
import { CycleStatsChart } from "@/components/charts/CycleStatsChart";
import { BusTripTable } from "@/components/tables/BusTripTable";
import { CycleStationTable } from "@/components/tables/CycleStationTable";
import { CycleStationMap } from "@/components/map/CycleStationMap";
import { SkeletonCard } from "@/components/common/SkeletonCard";
import { EmptyState } from "@/components/common/EmptyState";
import { useAppSelector } from "@/store/hooks";

/** Reusable floating panel positioned absolutely over the map */
const FloatingPanel = ({
  children,
  sx = {},
}: {
  children: React.ReactNode;
  sx?: object;
}) => (
  <Paper
    elevation={0}
    sx={{
      position: "absolute",
      zIndex: 1000,
      p: 2,
      maxWidth: 340,
      maxHeight: "calc(100vh - 32px)",
      overflow: "auto",
      borderRadius: 3,
      border: "1px solid",
      borderColor: "divider",
      ...sx,
    }}
  >
    {children}
  </Paper>
);

export const Dashboard = () => {
  const { data: busData, isLoading: busLoading, isSuccess: busSuccess } = useBusData(undefined, 50);
  const { data: cycleData, isLoading: cycleLoading, isSuccess: cycleSuccess } = useCycleData(50);
  const theme = useAppSelector((state) => state.ui.theme);

  const [busExpanded, setBusExpanded] = useState(false);
  const [cycleExpanded, setCycleExpanded] = useState(false);

  // Track first successful load so count-up doesn't re-trigger on background refetches
  const busHasAnimated = useRef(busSuccess);
  const cycleHasAnimated = useRef(cycleSuccess);
  useEffect(() => { if (busSuccess) busHasAnimated.current = true; }, [busSuccess]);
  useEffect(() => { if (cycleSuccess) cycleHasAnimated.current = true; }, [cycleSuccess]);

  const busEmpty = !busLoading && (busData?.totalRecords ?? 0) === 0;
  const cycleEmpty = !cycleLoading && (cycleData?.data?.length ?? 0) === 0;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map background */}
      <CycleStationMap
        stations={cycleData?.data || []}
        height="100%"
        darkTiles={theme === "dark"}
      />

      {/* Top-left: Bus stats card */}
      <FloatingPanel sx={{ top: 16, left: 16 }}>
        {busLoading ? (
          <SkeletonCard variant="stat" />
        ) : (
          <>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                mb: busExpanded ? 1.5 : 0,
              }}
            >
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <DirectionsBusIcon color="primary" fontSize="small" />
                <Typography variant="h5">Bus Trips</Typography>
              </Box>
              <IconButton size="small" onClick={() => setBusExpanded(!busExpanded)}>
                {busExpanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
              </IconButton>
            </Box>
            {!busExpanded && (
              <Typography variant="caption" color="text.secondary">
                <CountUp
                  end={busData?.totalRecords ?? 0}
                  duration={1.2}
                  separator=","
                  startOnMount={false}
                  start={busHasAnimated.current ? (busData?.totalRecords ?? 0) : 0}
                />{" "}
                trips &middot; Click to expand
              </Typography>
            )}
            <Collapse in={busExpanded}>
              {busEmpty ? (
                <EmptyState
                  icon="bus"
                  title="No bus trips"
                  description="No active trips found for this route."
                />
              ) : (
                <>
                  {busData?.statistics && (
                    <Box sx={{ mb: 1.5 }}>
                      <DelayChart statistics={busData.statistics} compact />
                    </Box>
                  )}
                  <BusTripTable trips={busData?.data || []} maxRows={8} compact />
                </>
              )}
            </Collapse>
          </>
        )}
      </FloatingPanel>

      {/* Bottom-left: Cycle stats card */}
      <FloatingPanel sx={{ bottom: 16, left: 16 }}>
        {cycleLoading ? (
          <SkeletonCard variant="stat" />
        ) : (
          <>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                mb: cycleExpanded ? 1.5 : 0,
              }}
            >
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <DirectionsBikeIcon color="secondary" fontSize="small" />
                <Typography variant="h5">Cycle Stations</Typography>
              </Box>
              <IconButton
                size="small"
                onClick={() => setCycleExpanded(!cycleExpanded)}
              >
                {cycleExpanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
              </IconButton>
            </Box>
            {!cycleExpanded && (
              <Typography variant="caption" color="text.secondary">
                <CountUp
                  end={cycleData?.data?.length ?? 0}
                  duration={1.2}
                  separator=","
                  startOnMount={false}
                  start={cycleHasAnimated.current ? (cycleData?.data?.length ?? 0) : 0}
                />{" "}
                stations on map &middot; Click to expand
              </Typography>
            )}
            <Collapse in={cycleExpanded}>
              {cycleEmpty ? (
                <EmptyState
                  icon="cycle"
                  title="No station data"
                  description="Cycle station data is currently unavailable."
                />
              ) : (
                <>
                  {cycleData?.statistics && (
                    <Box sx={{ mb: 1.5 }}>
                      <CycleStatsChart statistics={cycleData.statistics} compact />
                    </Box>
                  )}
                  <CycleStationTable
                    stations={cycleData?.data || []}
                    maxRows={8}
                    compact
                  />
                </>
              )}
            </Collapse>
          </>
        )}
      </FloatingPanel>
    </Box>
  );
};
