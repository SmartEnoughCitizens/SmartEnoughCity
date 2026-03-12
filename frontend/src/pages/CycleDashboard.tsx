/**
 * Cycle stations dashboard — full-viewport map with floating side panel
 * Uses CycleMetricsController endpoints (/api/v1/cycle/...)
 */

import { useState } from "react";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  IconButton,
  Divider,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import {
  useCycleStationsLive,
  useCycleNetworkSummary,
  useCycleBusiestStations,
  useCycleUnderusedStations,
  useCycleEmptyEvents,
  useCycleFullEvents,
} from "@/hooks";
import { NetworkSummaryChart } from "@/components/charts/NetworkSummaryChart";
import { LiveCycleStationTable } from "@/components/tables/LiveCycleStationTable";
import { CycleRankingTable } from "@/components/tables/CycleRankingTable";
import { CycleEventTable } from "@/components/tables/CycleEventTable";
import { LiveCycleStationMap } from "@/components/map/LiveCycleStationMap";
import { useAppSelector } from "@/store/hooks";

export const CycleDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [rankingSubTab, setRankingSubTab] = useState(0); // 0=busiest, 1=underused
  const [eventSubTab, setEventSubTab] = useState(0); // 0=empty, 1=full
  const [panelOpen, setPanelOpen] = useState(true);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: liveStations, isLoading: stationsLoading, error } = useCycleStationsLive();
  const { data: summary, isLoading: summaryLoading } = useCycleNetworkSummary();
  const { data: busiest, isLoading: busiestLoading } = useCycleBusiestStations();
  const { data: underused, isLoading: underusedLoading } = useCycleUnderusedStations();
  const { data: emptyEvents, isLoading: emptyLoading } = useCycleEmptyEvents();
  const { data: fullEvents, isLoading: fullLoading } = useCycleFullEvents();

  const isLoading =
    stationsLoading ||
    summaryLoading ||
    busiestLoading ||
    underusedLoading ||
    emptyLoading ||
    fullLoading;

  if (isLoading) {
    return (
      <Box
        sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}
      >
        <CircularProgress />
      </Box>
    );
  }

  const stations = liveStations ?? [];
  const panelWidth = 400;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map with status-coloured circle markers */}
      <LiveCycleStationMap
        stations={stations}
        height="100%"
        darkTiles={theme === "dark"}
      />

      {error && (
        <Alert
          severity="error"
          sx={{
            position: "absolute",
            top: 16,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1000,
            borderRadius: 2,
          }}
        >
          Failed to load cycle station data
        </Alert>
      )}

      {/* Toggle button when panel is closed */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
            zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* Floating side panel */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: 16,
            right: 16,
            bottom: 16,
            width: panelWidth,
            zIndex: 1000,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          {/* Panel header */}
          <Box
            sx={{
              p: 2,
              pb: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <Box>
              <Typography variant="h5">Cycle Stations</Typography>
              {summary && (
                <Typography variant="caption" color="text.secondary">
                  {summary.activeStations} active &middot;{" "}
                  {summary.totalBikesAvailable} bikes &middot;{" "}
                  {summary.rebalancingNeedCount} need rebalancing
                </Typography>
              )}
            </Box>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* Network summary chart */}
          {summary && (
            <Box sx={{ px: 2, pb: 1 }}>
              <NetworkSummaryChart summary={summary} compact />
            </Box>
          )}

          <Divider />

          {/* Main tabs */}
          <Tabs
            value={tabValue}
            onChange={(_, v) => setTabValue(v)}
            variant="fullWidth"
            sx={{
              minHeight: 36,
              px: 1,
              "& .MuiTab-root": {
                minHeight: 36,
                fontSize: "0.75rem",
                textTransform: "none",
              },
            }}
          >
            <Tab label={`Stations (${stations.length})`} />
            <Tab label="Rankings" />
            <Tab label="Events" />
          </Tabs>

          {/* Tab content */}
          <Box sx={{ flex: 1, overflow: "auto", pt: 0.5 }}>
            {/* Tab 0: Live stations */}
            {tabValue === 0 && (
              <Box sx={{ px: 1 }}>
                <LiveCycleStationTable stations={stations} maxRows={100} compact />
              </Box>
            )}

            {/* Tab 1: Rankings */}
            {tabValue === 1 && (
              <Box>
                <Tabs
                  value={rankingSubTab}
                  onChange={(_, v) => setRankingSubTab(v)}
                  variant="fullWidth"
                  sx={{
                    minHeight: 32,
                    "& .MuiTab-root": {
                      minHeight: 32,
                      fontSize: "0.7rem",
                      textTransform: "none",
                    },
                  }}
                >
                  <Tab label="Busiest" />
                  <Tab label="Underused" />
                </Tabs>
                <Box sx={{ px: 1, pt: 0.5 }}>
                  {rankingSubTab === 0 && (
                    <CycleRankingTable stations={busiest ?? []} />
                  )}
                  {rankingSubTab === 1 && (
                    <CycleRankingTable stations={underused ?? []} />
                  )}
                </Box>
              </Box>
            )}

            {/* Tab 2: Events */}
            {tabValue === 2 && (
              <Box>
                <Tabs
                  value={eventSubTab}
                  onChange={(_, v) => setEventSubTab(v)}
                  variant="fullWidth"
                  sx={{
                    minHeight: 32,
                    "& .MuiTab-root": {
                      minHeight: 32,
                      fontSize: "0.7rem",
                      textTransform: "none",
                    },
                  }}
                >
                  <Tab label={`Empty (${emptyEvents?.length ?? 0})`} />
                  <Tab label={`Full (${fullEvents?.length ?? 0})`} />
                </Tabs>
                <Box sx={{ px: 1, pt: 0.5 }}>
                  {eventSubTab === 0 && (
                    <CycleEventTable events={emptyEvents ?? []} />
                  )}
                  {eventSubTab === 1 && (
                    <CycleEventTable events={fullEvents ?? []} />
                  )}
                </Box>
              </Box>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};