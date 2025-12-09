/**
 * Main dashboard page
 */

import { Box, Grid, Paper, Typography, CircularProgress } from '@mui/material';
import { useBusData, useCycleData } from '@/hooks';
import { DelayChart } from '@/components/charts/DelayChart';
import { CycleStatsChart } from '@/components/charts/CycleStatsChart';
import { BusTripTable } from '@/components/tables/BusTripTable';
import { CycleStationTable } from '@/components/tables/CycleStationTable';

export const Dashboard = () => {
  const { data: busData, isLoading: busLoading } = useBusData(undefined, 50);
  const { data: cycleData, isLoading: cycleLoading } = useCycleData(50);

  if (busLoading || cycleLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard Overview
      </Typography>

      <Grid container spacing={3}>
        {/* Bus Statistics */}
        {busData?.statistics && (
          <Grid item xs={12} lg={6}>
            <DelayChart statistics={busData.statistics} />
          </Grid>
        )}

        {/* Cycle Statistics */}
        {cycleData?.statistics && (
          <Grid item xs={12} lg={6}>
            <CycleStatsChart statistics={cycleData.statistics} />
          </Grid>
        )}

        {/* Recent Bus Updates */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Bus Trip Updates
            </Typography>
            <BusTripTable trips={busData?.data || []} maxRows={10} />
          </Paper>
        </Grid>

        {/* Cycle Stations */}
        <Grid item xs={12}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Cycle Stations
            </Typography>
            <CycleStationTable stations={cycleData?.data || []} maxRows={10} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};
