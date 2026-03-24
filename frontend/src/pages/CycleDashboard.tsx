/**
 * Cycle stations dashboard — full-viewport map with floating side panel
 * and a separate bottom demand-analysis panel.
 */

import { useState } from "react";
import {
  Box,
  Chip,
  Divider,
  IconButton,
  Paper,
  Tab,
  Tabs,
  Typography,
  CircularProgress,
  Alert,
} from "@mui/material";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import QueryStatsIcon from "@mui/icons-material/QueryStats";
import {
  useCycleStationsLive,
  useCycleNetworkSummary,
  useCycleBusiestStations,
  useCycleUnderusedStations,
  useCycleRebalancing,
  useCycleODPairs,
  useODRoutes,
} from "@/hooks";
import { NetworkSummaryChart } from "@/components/charts/NetworkSummaryChart";
import { LiveCycleStationTable } from "@/components/tables/LiveCycleStationTable";
import { CycleRankingTable } from "@/components/tables/CycleRankingTable";
import { RebalancingTable } from "@/components/tables/RebalancingTable";
import { ODFlowTable, type IntensityFilter } from "@/components/tables/ODFlowTable";
import { LiveCycleStationMap } from "@/components/map/LiveCycleStationMap";
import { ODFlowMap } from "@/components/map/ODFlowMap";
import { StationDemandPanel } from "@/components/cycle/StationDemandPanel";
import { useAppSelector } from "@/store/hooks";

const SIDE_PANEL_WIDTH = 420;
const DEMAND_PANEL_HEIGHT = 280;
const GAP = 16;

export const CycleDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [rankingSubTab, setRankingSubTab] = useState(0);
  const [odFilterStationId, setOdFilterStationId] = useState<number | null>(null);
  const [intensityFilter, setIntensityFilter] = useState<IntensityFilter>("all");
  const [selectedPairKey, setSelectedPairKey] = useState<string | null>(null);

  const handleIntensityFilterChange = (f: IntensityFilter) => {
    setIntensityFilter(f);
    setSelectedPairKey(null); // clear selection when filter changes
  };

  const handleStationFilterChange = (id: number | null) => {
    setOdFilterStationId(id);
    setSelectedPairKey(null); // clear selection when station filter changes
  };
  const [panelOpen, setPanelOpen] = useState(true);
  const [demandOpen, setDemandOpen] = useState(false);
  const theme = useAppSelector((state) => state.ui.theme);

  const {
    data: stations,
    isLoading: stationsLoading,
    error,
  } = useCycleStationsLive();
  const { data: summary } = useCycleNetworkSummary();
  const { data: busiest, isLoading: busiestLoading } = useCycleBusiestStations(10);
  const { data: underused, isLoading: underusedLoading } = useCycleUnderusedStations(10);
  const { data: rebalancing, isLoading: rebalancingLoading } = useCycleRebalancing(30);
  const { data: odPairs, isLoading: odLoading } = useCycleODPairs(30, 50);
  const { routeCache, progress: routeProgress } = useODRoutes(odPairs ?? []);

  const isLoading =
    stationsLoading || busiestLoading || underusedLoading || rebalancingLoading || odLoading;

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
        <CircularProgress />
      </Box>
    );
  }

  const allOdPairs = odPairs ?? [];

  // Compute intensity thresholds from the actual data distribution (percentile-based)
  const odThresholds = (() => {
    if (allOdPairs.length === 0) return { low: 1, high: 1 };
    const sorted = allOdPairs.toSorted((a, b) => a.estimatedTrips - b.estimatedTrips);
    const p33 = sorted[Math.floor(sorted.length * 0.33)]?.estimatedTrips ?? 1;
    const p66 = sorted[Math.floor(sorted.length * 0.66)]?.estimatedTrips ?? 1;
    return { low: p33, high: p66 };
  })();

  const filteredOdPairs = (() => {
    const byStation = odFilterStationId
      ? allOdPairs.filter(
          (p) => p.originStationId === odFilterStationId || p.destStationId === odFilterStationId,
        )
      : allOdPairs;
    if (intensityFilter === "all") return byStation;
    return byStation.filter((p) => {
      if (intensityFilter === "extreme") return p.estimatedTrips >= odThresholds.high;
      if (intensityFilter === "medium") return p.estimatedTrips >= odThresholds.low && p.estimatedTrips < odThresholds.high;
      if (intensityFilter === "low") return p.estimatedTrips < odThresholds.low;
      return true;
    });
  })();

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>

      {/* ── Full-viewport map ─────────────────────────────────────── */}
      {tabValue === 3 ? (
        <ODFlowMap
          stations={stations ?? []}
          odPairs={filteredOdPairs}
          thresholds={odThresholds}
          filterStationId={odFilterStationId}
          selectedPairKey={selectedPairKey}
          routeCache={routeCache}
          routeProgress={routeProgress}
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
            top: GAP,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1000,
            borderRadius: 2,
          }}
        >
          Failed to load cycle station data
        </Alert>
      )}

      {/* ── Side panel toggle (when closed) ───────────────────────── */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* ── Floating side panel (Stations / Rankings / Rebalancing / OD) ── */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            bottom: GAP,
            width: SIDE_PANEL_WIDTH,
            zIndex: 1000,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
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

          {summary && (
            <Box sx={{ px: 2, pb: 1 }}>
              <NetworkSummaryChart summary={summary} compact />
            </Box>
          )}

          <Tabs
            value={tabValue}
            onChange={(_, v) => setTabValue(v)}
            variant="fullWidth"
            sx={{
              minHeight: 36,
              px: 1,
              "& .MuiTab-root": { minHeight: 36, fontSize: "0.7rem", textTransform: "none" },
            }}
          >
            <Tab label={`Stations (${stations?.length ?? 0})`} />
            <Tab label="Rankings" />
            <Tab label={`Rebalancing (${rebalancing?.length ?? 0})`} />
            <Tab label="OD Flow" />
          </Tabs>

          <Box sx={{ flex: 1, overflow: "auto", px: 1, pt: 0.5 }}>
            {tabValue === 0 && <LiveCycleStationTable stations={stations ?? []} compact />}

            {tabValue === 1 && (
              <>
                <Tabs
                  value={rankingSubTab}
                  onChange={(_, v) => setRankingSubTab(v)}
                  variant="fullWidth"
                  sx={{
                    minHeight: 32,
                    mb: 0.5,
                    "& .MuiTab-root": { minHeight: 32, fontSize: "0.7rem", textTransform: "none" },
                  }}
                >
                  <Tab label="Busiest" />
                  <Tab label="Underused" />
                </Tabs>
                {rankingSubTab === 0 && <CycleRankingTable stations={busiest ?? []} />}
                {rankingSubTab === 1 && <CycleRankingTable stations={underused ?? []} />}
              </>
            )}

            {tabValue === 2 && <RebalancingTable suggestions={rebalancing ?? []} />}

            {tabValue === 3 && (
              <ODFlowTable
                odPairs={filteredOdPairs}
                allPairs={allOdPairs}
                thresholds={odThresholds}
                filterStationId={odFilterStationId}
                intensityFilter={intensityFilter}
                selectedPairKey={selectedPairKey}
                onFilterChange={handleStationFilterChange}
                onIntensityFilterChange={handleIntensityFilterChange}
                onPairSelect={setSelectedPairKey}
              />
            )}
          </Box>
        </Paper>
      )}

      {/* ── Demand Analysis — bottom floating panel ───────────────── */}
      {!demandOpen && (
        <Chip
          icon={<QueryStatsIcon sx={{ fontSize: "1rem !important" }} />}
          label="Demand Analysis"
          onClick={() => setDemandOpen(true)}
          sx={{
            position: "absolute",
            bottom: GAP,
            left: GAP,
            zIndex: 1000,
            backdropFilter: "blur(12px)",
            bgcolor: (t) => t.palette.background.paper,
            fontWeight: 600,
            fontSize: "0.72rem",
            cursor: "pointer",
            "& .MuiChip-icon": { color: "primary.main" },
          }}
        />
      )}

      {demandOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            bottom: GAP,
            left: GAP,
            right: panelOpen ? SIDE_PANEL_WIDTH + GAP * 2 : GAP,
            height: DEMAND_PANEL_HEIGHT,
            zIndex: 1000,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          <Box
            sx={{
              px: 2,
              py: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              flexShrink: 0,
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <QueryStatsIcon fontSize="small" color="primary" />
              <Typography variant="subtitle1" fontWeight={600}>
                Demand Analysis
              </Typography>
            </Box>
            <IconButton size="small" onClick={() => setDemandOpen(false)}>
              <KeyboardArrowDownIcon fontSize="small" />
            </IconButton>
          </Box>

          <Divider />

          <Box sx={{ flex: 1, overflow: "hidden" }}>
            <StationDemandPanel />
          </Box>
        </Paper>
      )}

    </Box>
  );
};
