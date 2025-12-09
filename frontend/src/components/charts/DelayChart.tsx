/**
 * Delay statistics chart component using Recharts
 */

import { Box, Paper, Typography } from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { DelayStatistics } from '@/types';

interface DelayChartProps {
  statistics: DelayStatistics;
}

export const DelayChart = ({ statistics }: DelayChartProps) => {
  const data = [
    {
      name: 'Average',
      arrival: statistics.averageArrivalDelay || 0,
      departure: statistics.averageDepartureDelay || 0,
    },
    {
      name: 'Min',
      arrival: statistics.minArrivalDelay || 0,
      departure: statistics.minDepartureDelay || 0,
    },
    {
      name: 'Max',
      arrival: statistics.maxArrivalDelay || 0,
      departure: statistics.maxDepartureDelay || 0,
    },
  ];

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Delay Statistics - Route {statistics.routeId}
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Total Trips: {statistics.totalTrips}
      </Typography>
      <Box sx={{ width: '100%', height: 300, mt: 2 }}>
        <ResponsiveContainer>
          <BarChart data={data}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis label={{ value: 'Delay (seconds)', angle: -90, position: 'insideLeft' }} />
            <Tooltip />
            <Legend />
            <Bar dataKey="arrival" fill="#1976d2" name="Arrival Delay" />
            <Bar dataKey="departure" fill="#dc004e" name="Departure Delay" />
          </BarChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
};
