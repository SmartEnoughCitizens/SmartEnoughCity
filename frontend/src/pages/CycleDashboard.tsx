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
  useCycleRebalancing,
  useCycleODHeatmap,
} from "@/hooks";
import { NetworkSummaryChart } from "@/components/charts/NetworkSummaryChart";
import { LiveCycleStationTable } from "@/components/tables/LiveCycleStationTable";
import { CycleRankingTable } from "@/components/tables/CycleRankingTable";
import { RebalancingTable } from "@/components/tables/RebalancingTable";
import { ODFlowTable, type IntensityFilter } from "@/components/tables/ODFlowTable";
import { LiveCycleStationMap } from "@/components/map/LiveCycleStationMap";
import { ODFlowMap } from "@/components/map/ODFlowMap";
import { useAppSelector } from "@/store/hooks";

export const CycleDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [rankingSubTab, setRankingSubTab] = useState(0);
  const [odFilterStationId, setOdFilterStationId] = useState<number | null>(null);
  const [intensityFilter, setIntensityFilter] = useState<IntensityFilter>("extreme");
  const [selectedPairKey, setSelectedPairKey] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(true);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: liveStations, isLoading: stationsLoading, error } = useCycleStationsLive();
  const { data: summary, isLoading: summaryLoading } = useCycleNetworkSummary();
  const { data: busiest, isLoading: busiestLoading } = useCycleBusiestStations();
  const { data: underused, isLoading: underusedLoading } = useCycleUnderusedStations();
  const { data: rebalancing, isLoading: rebalancingLoading } = useCycleRebalancing();
  const { data: odPairs, isLoading: odLoading } = useCycleODHeatmap(50);

  if (stationsLoading) {
    return (
      <Box
        sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}
      >
        <CircularProgress />
      </Box>
    );
  }

  const stations = liveStations ?? [];
  const allPairs = odPairs ?? [];

  const maxTrips = Math.max(...allPairs.map((p) => p.estimatedTrips), 1);
  const pairs = allPairs.filter((p) => {
    if (intensityFilter === "all") return true;
    const t = p.estimatedTrips / maxTrips;
    if (intensityFilter === "extreme") return t >= 0.66;
    if (intensityFilter === "medium")  return t >= 0.33 && t < 0.66;
    if (intensityFilter === "low")     return t < 0.33;
    return true;
  });

  const isODTab = tabValue === 3;
  const panelWidth = 400;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Map — switches between live view and OD flow view */}
      {isODTab ? (
        <ODFlowMap
          stations={stations}
          odPairs={pairs}
          globalMaxTrips={maxTrips}
          filterStationId={odFilterStationId}
          selectedPairKey={selectedPairKey}
          height="100%"
          darkTiles={theme === "dark"}
        />
      ) : (
        <LiveCycleStationMap
          stations={stations}
          height="100%"
          darkTiles={theme === "dark"}
        />
      )}

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
            onChange={(_, v) => {
              setTabValue(v);
              if (v !== 3) {
                setOdFilterStationId(null);
                setSelectedPairKey(null);
              }
            }}
            variant="fullWidth"
            sx={{
              minHeight: 36,
              "& .MuiTab-root": {
                minHeight: 36,
                fontSize: "0.7rem",
                textTransform: "none",
              },
            }}
          >
            <Tab label={`Stations (${stations.length})`} />
            <Tab label="Rankings" />
            <Tab label={`Rebalancing (${rebalancing?.length ?? 0})`} />
            <Tab label="OD Flow" />
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
                    "& .MuiTab-root": { minHeight: 32, fontSize: "0.7rem", textTransform: "none" },
                  }}
                >
                  <Tab label="Busiest" />
                  <Tab label="Underused" />
                </Tabs>
                <Box sx={{ px: 1, pt: 0.5 }}>
                  {rankingSubTab === 0 && (
                    busiestLoading
                      ? <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={24} /></Box>
                      : <CycleRankingTable stations={busiest ?? []} />
                  )}
                  {rankingSubTab === 1 && (
                    underusedLoading
                      ? <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={24} /></Box>
                      : <CycleRankingTable stations={underused ?? []} />
                  )}
                </Box>
              </Box>
            )}

            {/* Tab 2: Rebalancing */}
            {tabValue === 2 && (
              <Box sx={{ px: 1, pt: 0.5 }}>
                {rebalancingLoading
                  ? <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={24} /></Box>
                  : <RebalancingTable suggestions={rebalancing ?? []} />}
              </Box>
            )}

            {/* Tab 3: OD Flow */}
            {tabValue === 3 && (
              odLoading
                ? <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}><CircularProgress size={24} /></Box>
                : <ODFlowTable
                    odPairs={pairs}
                    allPairs={allPairs}
                    filterStationId={odFilterStationId}
                    intensityFilter={intensityFilter}
                    selectedPairKey={selectedPairKey}
                    onFilterChange={setOdFilterStationId}
                    onIntensityFilterChange={setIntensityFilter}
                    onPairSelect={setSelectedPairKey}
                  />
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};