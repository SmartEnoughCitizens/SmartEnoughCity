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
import { useCycleData, useAvailableBikes, useAvailableDocks } from "@/hooks";
import { CycleStatsChart } from "@/components/charts/CycleStatsChart";
import { CycleStationTable } from "@/components/tables/CycleStationTable";
import { CycleStationMap } from "@/components/map/CycleStationMap";
import { useAppSelector } from "@/store/hooks";

export const CycleDashboard = () => {
  const [tabValue, setTabValue] = useState(0);
  const [panelOpen, setPanelOpen] = useState(true);
  const theme = useAppSelector((state) => state.ui.theme);

  const { data: allStations, isLoading: allLoading, error } = useCycleData(200);
  const { data: bikesAvailable, isLoading: bikesLoading } = useAvailableBikes();
  const { data: docksAvailable, isLoading: docksLoading } = useAvailableDocks();

  const getCurrentData = () => {
    switch (tabValue) {
      case 0: {
        return allStations?.data || [];
      }
      case 1: {
        return bikesAvailable || [];
      }
      case 2: {
        return docksAvailable || [];
      }
      default: {
        return [];
      }
    }
  };

  const isLoading = allLoading || bikesLoading || docksLoading;

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

  const panelWidth = 380;

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* Full-viewport map */}
      <CycleStationMap
        stations={getCurrentData()}
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

          {/* Stats chart */}
          {allStations?.statistics && (
            <Box sx={{ px: 2, pb: 1 }}>
              <CycleStatsChart statistics={allStations.statistics} compact />
            </Box>
          )}

          {/* Tabs */}
          <Tabs
            value={tabValue}
            onChange={(_, newValue) => setTabValue(newValue)}
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
            <Tab label={`All (${allStations?.data.length || 0})`} />
            <Tab label={`Bikes (${bikesAvailable?.length || 0})`} />
            <Tab label={`Docks (${docksAvailable?.length || 0})`} />
          </Tabs>

          {/* Table */}
          <Box sx={{ flex: 1, overflow: "auto", px: 1, pt: 0.5 }}>
            <CycleStationTable
              stations={getCurrentData()}
              maxRows={50}
              compact
            />
          </Box>
        </Paper>
      )}
    </Box>
  );
};
