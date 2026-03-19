/**
 * Cycle stations dashboard — full-viewport map with floating side panel
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
import {
  ODFlowTable,
  type IntensityFilter,
} from "@/components/tables/ODFlowTable";
import { LiveCycleStationMap } from "@/components/map/LiveCycleStationMap";
import { ODFlowMap } from "@/components/map/ODFlowMap";
import { useAppSelector } from "@/store/hooks";

export const CycleDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [rankingSubTab, setRankingSubTab] = useState(0);
  const [odFilterStationId, setOdFilterStationId] = useState<number | null>(
    null,
  );
  const [intensityFilter, setIntensityFilter] =
    useState<IntensityFilter>("all");
  const [selectedPairKey, setSelectedPairKey] = useState<string | null>(null);
  const [panelOpen, setPanelOpen] = useState(true);
  const theme = useAppSelector((state) => state.ui.theme);

  const {
    data: stations,
    isLoading: stationsLoading,
    error,
  } = useCycleStationsLive();
  const { data: summary } = useCycleNetworkSummary();
  const { data: busiest, isLoading: busiestLoading } =
    useCycleBusiestStations(10);
  const { data: underused, isLoading: underusedLoading } =
    useCycleUnderusedStations(10);
  const { data: rebalancing, isLoading: rebalancingLoading } =
    useCycleRebalancing(30);
  const { data: odHeatmap, isLoading: odLoading } = useCycleODHeatmap(50);

  const isLoading =
    stationsLoading ||
    busiestLoading ||
    underusedLoading ||
    rebalancingLoading ||
    odLoading;

  if (isLoading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100%",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  const allOdPairs = odHeatmap ?? [];
  const globalMaxTrips =
    allOdPairs.length > 0
      ? Math.max(...allOdPairs.map((p) => p.estimatedTrips))
      : 1;

  const filteredOdPairs = (() => {
    const byStation = odFilterStationId
      ? allOdPairs.filter(
          (p) =>
            p.originStationId === odFilterStationId ||
            p.destStationId === odFilterStationId,
        )
      : allOdPairs;

    if (intensityFilter === "all") return byStation;
    const threshold =
      intensityFilter === "extreme"
        ? 0.66
        : intensityFilter === "medium"
          ? 0.33
          : 0;
    return byStation.filter(
      (p) => p.estimatedTrips / globalMaxTrips >= threshold,
    );
  })();

  const panelWidth = 420;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map */}
      {tabValue === 3 ? (
        <ODFlowMap
          stations={stations ?? []}
          odPairs={filteredOdPairs}
          globalMaxTrips={globalMaxTrips}
          filterStationId={odFilterStationId}
          selectedPairKey={selectedPairKey}
          height="100%"
          darkTiles={theme === "dark"}
        />
      ) : (
        <LiveCycleStationMap
          stations={stations ?? []}
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
            "&:hover": {
              bgcolor: (t) => t.palette.background.paper,
            },
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
            <Typography variant="h5">Cycle Stations</Typography>
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
                fontSize: "0.7rem",
                textTransform: "none",
              },
            }}
          >
            <Tab label={`Stations (${stations?.length ?? 0})`} />
            <Tab label="Rankings" />
            <Tab label={`Rebalancing (${rebalancing?.length ?? 0})`} />
            <Tab label="OD Flow" />
          </Tabs>

          {/* Tab content */}
          <Box sx={{ flex: 1, overflow: "auto", px: 1, pt: 0.5 }}>
            {tabValue === 0 && (
              <LiveCycleStationTable stations={stations ?? []} compact />
            )}

            {tabValue === 1 && (
              <>
                <Tabs
                  value={rankingSubTab}
                  onChange={(_, v) => setRankingSubTab(v)}
                  variant="fullWidth"
                  sx={{
                    minHeight: 32,
                    mb: 0.5,
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
                {rankingSubTab === 0 && (
                  <CycleRankingTable stations={busiest ?? []} />
                )}
                {rankingSubTab === 1 && (
                  <CycleRankingTable stations={underused ?? []} />
                )}
              </>
            )}

            {tabValue === 2 && (
              <RebalancingTable suggestions={rebalancing ?? []} />
            )}

            {tabValue === 3 && (
              <ODFlowTable
                odPairs={filteredOdPairs}
                allPairs={allOdPairs}
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
