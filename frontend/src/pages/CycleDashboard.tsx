// /**
//  * Cycle stations dashboard page
//  */

// import {
//   Box,
//   Grid,
//   Paper,
//   Typography,
//   CircularProgress,
//   Alert,
//   Tabs,
//   Tab,
// } from '@mui/material';
// import { useState } from 'react';
// import { useCycleData, useAvailableBikes, useAvailableDocks } from '@/hooks';
// import { CycleStatsChart } from '@/components/charts/CycleStatsChart';
// import { CycleStationTable } from '@/components/tables/CycleStationTable';
// import { CycleStationMap } from '@/components/map/CycleStationMap';

// export const CycleDashboard = () => {
//   const [tabValue, setTabValue] = useState(0);

//   const { data: allStations, isLoading: allLoading, error } = useCycleData(200);
//   const { data: bikesAvailable, isLoading: bikesLoading } = useAvailableBikes();
//   const { data: docksAvailable, isLoading: docksLoading } = useAvailableDocks();

//   const getCurrentData = () => {
//     switch (tabValue) {
//       case 0:
//         return allStations?.data || [];
//       case 1:
//         return bikesAvailable || [];
//       case 2:
//         return docksAvailable || [];
//       default:
//         return [];
//     }
//   };

//   const isLoading = allLoading || bikesLoading || docksLoading;

//   if (isLoading) {
//     return (
//       <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
//         <CircularProgress />
//       </Box>
//     );
//   }

//   return (
//     <Box>
//       <Typography variant="h4" gutterBottom>
//         Cycle Stations
//       </Typography>

//       {error && (
//         <Alert severity="error" sx={{ mb: 2 }}>
//           Failed to load cycle station data
//         </Alert>
//       )}

//       <Grid container spacing={3}>
//         {/* Statistics */}
//         {allStations?.statistics && (
//           <Grid item xs={12}>
//             <CycleStatsChart statistics={allStations.statistics} />
//           </Grid>
//         )}

//         {/* Map View */}
//         <Grid item xs={12}>
//           <CycleStationMap stations={getCurrentData()} height={500} />
//         </Grid>

//         {/* Tabs for filtering */}
//         <Grid item xs={12}>
//           <Paper sx={{ p: 2 }}>
//             <Tabs
//               value={tabValue}
//               onChange={(_, newValue) => setTabValue(newValue)}
//               sx={{ mb: 2 }}
//             >
//               <Tab label={`All Stations (${allStations?.data.length || 0})`} />
//               <Tab label={`Bikes Available (${bikesAvailable?.length || 0})`} />
//               <Tab label={`Docks Available (${docksAvailable?.length || 0})`} />
//             </Tabs>

//             <CycleStationTable stations={getCurrentData()} maxRows={20} />
//           </Paper>
//         </Grid>
//       </Grid>
//     </Box>
//   );
// };

/**
 * Cycle stations dashboard page
 */

import {
  Box,
  Grid,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
} from '@mui/material';
import { useState } from 'react';
import { useCycleData, useAvailableBikes, useAvailableDocks } from '@/hooks';
import { CycleStatsChart } from '@/components/charts/CycleStatsChart';
import { CycleStationTable } from '@/components/tables/CycleStationTable';
import { CycleStationMap } from '@/components/map/CycleStationMap';

export const CycleDashboard = () => {
  const [tabValue, setTabValue] = useState(0);

  const { data: allStations, isLoading: allLoading, error } = useCycleData(200);
  const { data: bikesAvailable, isLoading: bikesLoading } = useAvailableBikes();
  const { data: docksAvailable, isLoading: docksLoading } = useAvailableDocks();

  const getCurrentData = () => {
    switch (tabValue) {
      case 0:
        return allStations?.data || [];
      case 1:
        return bikesAvailable || [];
      case 2:
        return docksAvailable || [];
      default:
        return [];
    }
  };

  const isLoading = allLoading || bikesLoading || docksLoading;

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Cycle Stations
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load cycle station data
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Statistics */}
        {allStations?.statistics && (
          <Grid size={{ xs: 12 }}>
            <CycleStatsChart statistics={allStations.statistics} />
          </Grid>
        )}

        {/* Map View */}
        <Grid size={{ xs: 12 }}>
          <CycleStationMap stations={getCurrentData()} height={500} />
        </Grid>

        {/* Tabs for filtering */}
        <Grid size={{ xs: 12 }}>
          <Paper sx={{ p: 2 }}>
            <Tabs
              value={tabValue}
              onChange={(_, newValue) => setTabValue(newValue)}
              sx={{ mb: 2 }}
            >
              <Tab label={`All Stations (${allStations?.data.length || 0})`} />
              <Tab label={`Bikes Available (${bikesAvailable?.length || 0})`} />
              <Tab label={`Docks Available (${docksAvailable?.length || 0})`} />
            </Tabs>

            <CycleStationTable stations={getCurrentData()} maxRows={20} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};