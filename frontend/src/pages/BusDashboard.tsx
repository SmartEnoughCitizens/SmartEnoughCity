// /**
//  * Bus data dashboard page
//  */

// import { useState } from 'react';
// import {
//   Box,
//   Grid,
//   Paper,
//   Typography,
//   CircularProgress,
//   FormControl,
//   InputLabel,
//   Select,
//   MenuItem,
//   Alert,
// } from '@mui/material';
// import { useBusData, useBusRoutes } from '@/hooks';
// import { DelayChart } from '@/components/charts/DelayChart';
// import { BusTripTable } from '@/components/tables/BusTripTable';

// export const BusDashboard = () => {
//   const [selectedRoute, setSelectedRoute] = useState<string>('');

//   const { data: routes, isLoading: routesLoading } = useBusRoutes();
//   const { data: busData, isLoading: dataLoading, error } = useBusData(
//     selectedRoute || undefined,
//     100
//   );

//   if (routesLoading) {
//     return (
//       <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
//         <CircularProgress />
//       </Box>
//     );
//   }

//   return (
//     <Box>
//       <Typography variant="h4" gutterBottom>
//         Bus Trip Updates
//       </Typography>

//       <Grid container spacing={3}>
//         <Grid item xs={12}>
//           <Paper sx={{ p: 2 }}>
//             <FormControl fullWidth>
//               <InputLabel>Select Route</InputLabel>
//               <Select
//                 value={selectedRoute}
//                 label="Select Route"
//                 onChange={(e) => setSelectedRoute(e.target.value)}
//               >
//                 <MenuItem value="">
//                   <em>All Routes</em>
//                 </MenuItem>
//                 {routes?.map((route) => (
//                   <MenuItem key={route} value={route}>
//                     Route {route}
//                   </MenuItem>
//                 ))}
//               </Select>
//             </FormControl>
//           </Paper>
//         </Grid>

//         {error && (
//           <Grid item xs={12}>
//             <Alert severity="error">Failed to load bus data</Alert>
//           </Grid>
//         )}

//         {dataLoading ? (
//           <Grid item xs={12}>
//             <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
//               <CircularProgress />
//             </Box>
//           </Grid>
//         ) : (
//           <>
//             {busData?.statistics && selectedRoute && (
//               <Grid item xs={12}>
//                 <DelayChart statistics={busData.statistics} />
//               </Grid>
//             )}

//             <Grid item xs={12}>
//               <Paper sx={{ p: 2 }}>
//                 <Typography variant="h6" gutterBottom>
//                   {selectedRoute
//                     ? `Route ${selectedRoute} - ${busData?.totalRecords || 0} Trips`
//                     : `All Routes - ${busData?.totalRecords || 0} Trips`}
//                 </Typography>
//                 <BusTripTable trips={busData?.data || []} maxRows={50} />
//               </Paper>
//             </Grid>
//           </>
//         )}
//       </Grid>
//     </Box>
//   );
// };

/**
 * Bus data dashboard page
 */

import { useState } from 'react';
import {
  Box,
  Grid,
  Paper,
  Typography,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
} from '@mui/material';
import { useBusData, useBusRoutes } from '@/hooks';
import { DelayChart } from '@/components/charts/DelayChart';
import { BusTripTable } from '@/components/tables/BusTripTable';

export const BusDashboard = () => {
  const [selectedRoute, setSelectedRoute] = useState<string>('');

  const { data: routes, isLoading: routesLoading } = useBusRoutes();
  const { data: busData, isLoading: dataLoading, error } = useBusData(
    selectedRoute || undefined,
    100
  );

  if (routesLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Bus Trip Updates
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <Paper sx={{ p: 2 }}>
            <FormControl fullWidth>
              <InputLabel>Select Route</InputLabel>
              <Select
                value={selectedRoute}
                label="Select Route"
                onChange={(e) => setSelectedRoute(e.target.value)}
              >
                <MenuItem value="">
                  <em>All Routes</em>
                </MenuItem>
                {routes?.map((route) => (
                  <MenuItem key={route} value={route}>
                    Route {route}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Paper>
        </Grid>

        {error && (
          <Grid size={{ xs: 12 }}>
            <Alert severity="error">Failed to load bus data</Alert>
          </Grid>
        )}

        {dataLoading ? (
          <Grid size={{ xs: 12 }}>
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          </Grid>
        ) : (
          <>
            {busData?.statistics && selectedRoute && (
              <Grid size={{ xs: 12 }}>
                <DelayChart statistics={busData.statistics} />
              </Grid>
            )}

            <Grid size={{ xs: 12 }}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  {selectedRoute
                    ? `Route ${selectedRoute} - ${busData?.totalRecords || 0} Trips`
                    : `All Routes - ${busData?.totalRecords || 0} Trips`}
                </Typography>
                <BusTripTable trips={busData?.data || []} maxRows={50} />
              </Paper>
            </Grid>
          </>
        )}
      </Grid>
    </Box>
  );
};