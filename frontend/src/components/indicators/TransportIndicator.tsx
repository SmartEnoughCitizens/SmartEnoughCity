import React, { useState } from 'react';
import { Grid, Typography, Box, Button, ButtonGroup } from '@mui/material';
import { Refresh } from '@mui/icons-material';
import type { SvgIconComponent } from '@mui/icons-material';
import type { UseQueryResult } from '@tanstack/react-query';
import MetricCard from '../common/MetricCard';
import ChartCard from '../common/ChartCard';
import CityMap from '../common/CityMap';
import LoadingSpinner from '../common/LoadingSpinner';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import type { TransportIndicatorData, TimeRange } from '@/types';

interface TransportIndicatorProps {
  mode: string;
  icon: SvgIconComponent;
  useIndicatorHook: (filters?: { timeRange?: TimeRange }) => UseQueryResult<TransportIndicatorData, Error>;
  color?: 'primary' | 'secondary' | 'success' | 'error' | 'info' | 'warning';
}

const TransportIndicator: React.FC<TransportIndicatorProps> = ({
  mode,
  icon: Icon,
  useIndicatorHook,
  color = 'primary'
}) => {
  const [timeRange, setTimeRange] = useState<TimeRange>('24h');

  const { data, isLoading, error, refetch } = useIndicatorHook({ timeRange });

  const getMockData = () => ({
    totalCount: Math.floor(Math.random() * 10000) + 1000,
    avgSpeed: Math.floor(Math.random() * 50) + 20,
    efficiency: Math.floor(Math.random() * 30) + 70,
    emissions: Math.floor(Math.random() * 100) + 50,
  });

  const hourlyData = Array.from({ length: 24 }, (_, i) => ({
    hour: `${i}:00`,
    count: Math.floor(Math.random() * 500) + 100,
    speed: Math.floor(Math.random() * 40) + 20,
  }));

  const dailyData = Array.from({ length: 7 }, (_, i) => {
    const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    return {
      day: days[i],
      count: Math.floor(Math.random() * 3000) + 1000,
    };
  });

  const mapMarkers = [
    { position: [51.505, -0.09] as [number, number], popup: `${mode} - High Activity` },
    { position: [51.51, -0.1] as [number, number], popup: `${mode} - Medium Activity` },
  ];

  if (isLoading && !data) {
    return <LoadingSpinner message={`Loading ${mode} indicators...`} />;
  }

  if (error) {
    console.error(`Error fetching ${mode} data:`, error);
  }

  const stats = data ? {
    totalCount: data.metrics.totalVehicles || 0,
    avgSpeed: data.metrics.avgSpeed || 0,
    efficiency: data.metrics.efficiency || 0,
    emissions: data.metrics.carbonEmissions || 0,
  } : getMockData();

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Icon sx={{ fontSize: 40 }} />
            {mode} Indicators
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Real-time monitoring and analytics for {mode.toLowerCase()} transportation
          </Typography>
        </Box>
        <Box>
          <ButtonGroup variant="outlined" size="small" sx={{ mr: 2 }}>
            <Button variant={timeRange === '24h' ? 'contained' : 'outlined'} onClick={() => setTimeRange('24h')}>
              24H
            </Button>
            <Button variant={timeRange === '7d' ? 'contained' : 'outlined'} onClick={() => setTimeRange('7d')}>
              7D
            </Button>
            <Button variant={timeRange === '30d' ? 'contained' : 'outlined'} onClick={() => setTimeRange('30d')}>
              30D
            </Button>
          </ButtonGroup>
          <Button variant="outlined" startIcon={<Refresh />} onClick={() => refetch()}>
            Refresh
          </Button>
        </Box>
      </Box>
      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title={`Total ${mode}s`}
            value={stats.totalCount?.toLocaleString() || '0'}
            trend={data?.changePercentage || (Math.random() > 0.5 ? 5.2 : -2.1)}
            color={color}
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title="Average Speed"
            value={stats.avgSpeed || 0}
            unit="km/h"
            trend={Math.random() > 0.5 ? 3.4 : -1.2}
            color="info"
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title="Efficiency"
            value={stats.efficiency || 0}
            unit="%"
            trend={Math.random() > 0.5 ? 2.1 : -0.8}
            color="success"
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title="CO₂ Emissions"
            value={stats.emissions || 0}
            unit="kg"
            trend={Math.random() > 0.5 ? -4.5 : 2.3}
            color="warning"
          />
        </Grid>
      </Grid>
      {/* Charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid
          size={{
            xs: 12,
            md: 6
          }}>
          <ChartCard title="Hourly Distribution" subtitle="Last 24 hours">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={hourlyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="hour" />
                <YAxis yAxisId="left" />
                <YAxis yAxisId="right" orientation="right" />
                <Tooltip />
                <Legend />
                <Line yAxisId="left" type="monotone" dataKey="count" stroke="#1976d2" strokeWidth={2} name="Count" />
                <Line yAxisId="right" type="monotone" dataKey="speed" stroke="#4caf50" strokeWidth={2} name="Speed (km/h)" />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
        <Grid
          size={{
            xs: 12,
            md: 6
          }}>
          <ChartCard title="Weekly Overview" subtitle="Last 7 days">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={dailyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="count" fill="#1976d2" name={`${mode} Count`} />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
      </Grid>
      {/* Map */}
      <Grid container spacing={3}>
        <Grid size={12}>
          <CityMap title={`${mode} Activity Heatmap`} markers={mapMarkers} height={400} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default TransportIndicator;
