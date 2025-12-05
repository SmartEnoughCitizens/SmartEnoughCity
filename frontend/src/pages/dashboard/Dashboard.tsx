import React from 'react';
import { Grid, Typography, Box, Alert } from '@mui/material';
import {
  DirectionsCar,
  // DirectionsBike,
  DirectionsBus,
  // Train,
  People,
  TrendingUp,
} from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import ChartCard from '../../components/common/ChartCard';
import CityMap from '../../components/common/CityMap';
import RecommendationCard from '../../components/common/RecommendationCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { useDashboardStats } from '@/hooks';

// Chart data interfaces
interface TrafficDataPoint {
  time: string;
  cars: number;
  bikes: number;
  buses: number;
}

interface TransportModeData {
  name: string;
  value: number;
  color: string;
}

interface WeeklyTrendData {
  day: string;
  traffic: number;
}

interface MapMarker {
  position: [number, number];
  popup: string;
}

const Dashboard: React.FC = () => {
  const { data: stats, isLoading, error } = useDashboardStats();

  // Mock data for demonstration
  const mockStats = {
    totalVehicles: 15420,
    activeRoutes: 87,
    avgSpeed: 45.3,
    congestionLevel: '67',
  };

  const trafficData: TrafficDataPoint[] = [
    { time: '00:00', cars: 120, bikes: 30, buses: 15 },
    { time: '06:00', cars: 450, bikes: 120, buses: 45 },
    { time: '12:00', cars: 680, bikes: 200, buses: 60 },
    { time: '18:00', cars: 820, bikes: 150, buses: 55 },
    { time: '23:59', cars: 200, bikes: 40, buses: 20 },
  ];

  const transportModeData: TransportModeData[] = [
    { name: 'Car', value: 45, color: '#1976d2' },
    { name: 'Bus', value: 25, color: '#dc004e' },
    { name: 'Cycle', value: 15, color: '#4caf50' },
    { name: 'Train', value: 10, color: '#ff9800' },
    { name: 'Walk', value: 5, color: '#9c27b0' },
  ];

  const weeklyTrend: WeeklyTrendData[] = [
    { day: 'Mon', traffic: 7200 },
    { day: 'Tue', traffic: 7800 },
    { day: 'Wed', traffic: 7500 },
    { day: 'Thu', traffic: 8100 },
    { day: 'Fri', traffic: 8900 },
    { day: 'Sat', traffic: 6200 },
    { day: 'Sun', traffic: 5500 },
  ];

  const mapMarkers: MapMarker[] = [
    { position: [51.505, -0.09], popup: 'City Center - High Traffic' },
    { position: [51.51, -0.1], popup: 'Main Station' },
    { position: [51.5, -0.08], popup: 'Shopping District' },
  ];

  if (isLoading) {
    return <LoadingSpinner message="Loading dashboard..." />;
  }

  // Use stats from API or fallback to mock data
  const displayStats = stats || mockStats;
  const congestionValue = typeof displayStats.congestionLevel === 'string'
    ? parseFloat(displayStats.congestionLevel)
    : 67;

  return (
    <Box>
      {error && (
        <Alert severity="warning" sx={{ mb: 3 }}>
          Unable to load live data. Showing mock data for demonstration.
        </Alert>
      )}
      <Typography variant="h4" gutterBottom>
        Dashboard Overview
      </Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom sx={{ mb: 3 }}>
        Real-time insights into city transportation and sustainability metrics
      </Typography>
      {/* Metrics Row */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title="Total Vehicles"
            value={displayStats.totalVehicles}
            trend={5.2}
            icon={DirectionsCar}
            color="primary"
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title="Active Routes"
            value={displayStats.activeRoutes}
            trend={-2.1}
            icon={DirectionsBus}
            color="secondary"
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            sm: 6,
            md: 3
          }}>
          <MetricCard
            title="Avg Speed"
            value={displayStats.avgSpeed}
            unit="km/h"
            trend={3.4}
            icon={TrendingUp}
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
            title="Congestion Level"
            value={congestionValue}
            unit="%"
            trend={-1.8}
            icon={People}
            color="warning"
          />
        </Grid>
      </Grid>
      {/* Charts Row */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid
          size={{
            xs: 12,
            md: 8
          }}>
          <ChartCard title="Traffic Trends" subtitle="Vehicles by time of day">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={trafficData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="cars" stroke="#1976d2" strokeWidth={2} />
                <Line type="monotone" dataKey="bikes" stroke="#4caf50" strokeWidth={2} />
                <Line type="monotone" dataKey="buses" stroke="#dc004e" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
        <Grid
          size={{
            xs: 12,
            md: 4
          }}>
          <ChartCard title="Transport Mode Split" subtitle="Usage distribution">
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={transportModeData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={(entry) => `${entry.name}: ${entry.value}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                >
                  {transportModeData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
      </Grid>
      {/* Weekly Trend & Map */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid
          size={{
            xs: 12,
            md: 6
          }}>
          <ChartCard title="Weekly Traffic Volume" subtitle="Last 7 days">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={weeklyTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="traffic" fill="#1976d2" />
              </BarChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
        <Grid
          size={{
            xs: 12,
            md: 6
          }}>
          <CityMap title="City Traffic Hotspots" markers={mapMarkers} height={350} />
        </Grid>
      </Grid>
      {/* Recommendations */}
      <Grid container spacing={3}>
        <Grid
          size={{
            xs: 12,
            md: 4
          }}>
          <RecommendationCard
            title="Peak Hour Optimization"
            description="Consider implementing dynamic traffic signals during peak hours (6-9 AM and 5-8 PM) to reduce congestion by an estimated 15%."
            priority="high"
            type="warning"
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            md: 4
          }}>
          <RecommendationCard
            title="Promote Cycling"
            description="Expand bike lane network in downtown area. Current usage shows 15% adoption with room for growth."
            priority="medium"
            type="info"
          />
        </Grid>
        <Grid
          size={{
            xs: 12,
            md: 4
          }}>
          <RecommendationCard
            title="Public Transport Schedule"
            description="Weekend bus frequency could be increased based on demand patterns showing 23% capacity utilization."
            priority="low"
            type="success"
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
