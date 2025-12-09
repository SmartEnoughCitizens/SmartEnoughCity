// /**
//  * Cycle station statistics chart component
//  */

// import { Box, Paper, Typography, Grid } from '@mui/material';
// import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
// import type { CycleStatistics } from '@/types';

// interface CycleStatsChartProps {
//   statistics: CycleStatistics;
// }

// const COLORS = ['#1976d2', '#dc004e', '#2e7d32', '#ed6c02'];

// export const CycleStatsChart = ({ statistics }: CycleStatsChartProps) => {
//   const pieData = [
//     { name: 'Available Bikes', value: statistics.totalBikesAvailable || 0 },
//     { name: 'Available Docks', value: statistics.totalDocksAvailable || 0 },
//   ];

//   return (
//     <Paper sx={{ p: 2 }}>
//       <Typography variant="h6" gutterBottom>
//         Cycle Station Statistics
//       </Typography>

//       <Grid container spacing={2}>
//         <Grid item xs={12} md={6}>
//           <Box sx={{ height: 300 }}>
//             <ResponsiveContainer>
//               <PieChart>
//                 <Pie
//                   data={pieData}
//                   cx="50%"
//                   cy="50%"
//                   labelLine={false}
//                   label={({ name, percent }) =>
//                     `${name}: ${(percent * 100).toFixed(0)}%`
//                   }
//                   outerRadius={80}
//                   fill="#8884d8"
//                   dataKey="value"
//                 >
//                   {pieData.map((_, index) => (
//                     <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
//                   ))}
//                 </Pie>
//                 <Tooltip />
//                 <Legend />
//               </PieChart>
//             </ResponsiveContainer>
//           </Box>
//         </Grid>

//         <Grid item xs={12} md={6}>
//           <Box sx={{ p: 2 }}>
//             <Typography variant="body2" gutterBottom>
//               <strong>Total Stations:</strong> {statistics.totalStations}
//             </Typography>
//             <Typography variant="body2" gutterBottom>
//               <strong>Average Bikes Available:</strong>{' '}
//               {statistics.averageBikesAvailable?.toFixed(1) || 0}
//             </Typography>
//             <Typography variant="body2" gutterBottom>
//               <strong>Average Docks Available:</strong>{' '}
//               {statistics.averageDocksAvailable?.toFixed(1) || 0}
//             </Typography>
//             <Typography variant="body2" gutterBottom>
//               <strong>Average Occupancy Rate:</strong>{' '}
//               {statistics.averageOccupancyRate?.toFixed(1) || 0}%
//             </Typography>
//             <Typography variant="body2" gutterBottom>
//               <strong>Stations Renting:</strong> {statistics.stationsRenting || 0}
//             </Typography>
//             <Typography variant="body2" gutterBottom>
//               <strong>Stations Returning:</strong> {statistics.stationsReturning || 0}
//             </Typography>
//           </Box>
//         </Grid>
//       </Grid>
//     </Paper>
//   );
// };

/**
 * Cycle station statistics chart component
 */

import { Box, Paper, Typography, Grid } from '@mui/material';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import type { CycleStatistics } from '@/types';

interface CycleStatsChartProps {
  statistics: CycleStatistics;
}

const COLORS = ['#1976d2', '#dc004e', '#2e7d32', '#ed6c02'];

export const CycleStatsChart = ({ statistics }: CycleStatsChartProps) => {
  const pieData = [
    { name: 'Available Bikes', value: statistics.totalBikesAvailable || 0 },
    { name: 'Available Docks', value: statistics.totalDocksAvailable || 0 },
  ];

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Cycle Station Statistics
      </Typography>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Box sx={{ height: 300 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) =>
                    `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`
                  }
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {pieData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </Box>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Box sx={{ p: 2 }}>
            <Typography variant="body2" gutterBottom>
              <strong>Total Stations:</strong> {statistics.totalStations}
            </Typography>
            <Typography variant="body2" gutterBottom>
              <strong>Average Bikes Available:</strong>{' '}
              {statistics.averageBikesAvailable?.toFixed(1) || 0}
            </Typography>
            <Typography variant="body2" gutterBottom>
              <strong>Average Docks Available:</strong>{' '}
              {statistics.averageDocksAvailable?.toFixed(1) || 0}
            </Typography>
            <Typography variant="body2" gutterBottom>
              <strong>Average Occupancy Rate:</strong>{' '}
              {statistics.averageOccupancyRate?.toFixed(1) || 0}%
            </Typography>
            <Typography variant="body2" gutterBottom>
              <strong>Stations Renting:</strong> {statistics.stationsRenting || 0}
            </Typography>
            <Typography variant="body2" gutterBottom>
              <strong>Stations Returning:</strong> {statistics.stationsReturning || 0}
            </Typography>
          </Box>
        </Grid>
      </Grid>
    </Paper>
  );
};