import React from 'react';
import {
  Grid,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  LinearProgress,
  Alert,
  CircularProgress,
} from '@mui/material';
import { Settings, Refresh, CheckCircle, Error, Warning } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import ChartCard from '../../components/common/ChartCard';
import { useSystemStatus } from '../../hooks/useSystem';
import { SystemStatus as SystemStatusType } from '../../types';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

type ServiceStatusType = 'up' | 'down' | 'maintenance' | 'Running' | 'Warning' | 'Error';

const SystemStatus: React.FC = () => {
  const { data: status, isLoading, error, refetch } = useSystemStatus(30000);

  const performanceData = [
    { time: '00:00', cpu: 35, memory: 55, requests: 120 },
    { time: '04:00', cpu: 25, memory: 52, requests: 80 },
    { time: '08:00', cpu: 55, memory: 65, requests: 280 },
    { time: '12:00', cpu: 65, memory: 72, requests: 350 },
    { time: '16:00', cpu: 58, memory: 68, requests: 320 },
    { time: '20:00', cpu: 42, memory: 60, requests: 200 },
  ];

  const getStatusIcon = (status: ServiceStatusType) => {
    switch (status) {
      case 'up':
      case 'Running':
        return <CheckCircle sx={{ color: 'success.main' }} />;
      case 'maintenance':
      case 'Warning':
        return <Warning sx={{ color: 'warning.main' }} />;
      case 'down':
      case 'Error':
        return <Error sx={{ color: 'error.main' }} />;
      default:
        return null;
    }
  };

  const getStatusColor = (status: ServiceStatusType): 'success' | 'warning' | 'error' | 'default' => {
    switch (status) {
      case 'up':
      case 'Running':
        return 'success';
      case 'maintenance':
      case 'Warning':
        return 'warning';
      case 'down':
      case 'Error':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: ServiceStatusType): string => {
    switch (status) {
      case 'up':
        return 'Running';
      case 'down':
        return 'Error';
      case 'maintenance':
        return 'Warning';
      default:
        return status;
    }
  };

  const formatUptime = (uptimeSeconds?: number, uptimeString?: string): string => {
    if (uptimeString) return uptimeString;
    if (!uptimeSeconds) return 'N/A';

    const days = Math.floor(uptimeSeconds / 86400);
    const hours = Math.floor((uptimeSeconds % 86400) / 3600);
    const minutes = Math.floor((uptimeSeconds % 3600) / 60);

    return `${days}d ${hours}h ${minutes}m`;
  };

  const getOverallStatus = () => {
    if (!status) return { color: 'default' as const, text: 'Loading...' };

    const services = status.services || [];
    const errorCount = services.filter((s) => s.status === 'down' || s.status === 'Error').length;
    const warningCount = services.filter((s) => s.status === 'maintenance' || s.status === 'Warning').length;

    if (errorCount > 0) return { color: 'error' as const, text: 'Critical' };
    if (warningCount > 0) return { color: 'warning' as const, text: 'Warning' };
    return { color: 'success' as const, text: 'Healthy' };
  };

  const overallStatus = getOverallStatus();

  // Get metrics from status
  const cpuUsage = status?.metrics?.cpuUsage ?? (status as any)?.cpuUsage ?? 0;
  const memoryUsage = status?.metrics?.memoryUsage ?? (status as any)?.memoryUsage ?? 0;
  const diskUsage = status?.metrics?.diskUsage ?? (status as any)?.diskUsage ?? 0;
  const activeConnections = status?.metrics?.activeConnections ?? (status as any)?.activeConnections ?? 0;
  const uptime = formatUptime(status?.uptime, (status as any)?.uptime);

  if (isLoading && !status) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Settings sx={{ fontSize: 40 }} />
            System Status
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Monitor system health and performance metrics
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<Refresh />}
          onClick={() => refetch()}
          disabled={isLoading}
        >
          Refresh
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Error loading system status: {error.message}
        </Alert>
      )}

      {/* System Overview */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card sx={{ height: '100%', bgcolor: `${overallStatus.color}.light`, color: 'white' }}>
            <CardContent>
              <Typography variant="body2" gutterBottom>
                System Status
              </Typography>
              <Typography variant="h4">{overallStatus.text}</Typography>
              <Typography variant="body2" sx={{ mt: 1 }}>
                Uptime: {uptime}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="CPU Usage"
            value={cpuUsage}
            unit="%"
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Memory Usage"
            value={memoryUsage}
            unit="%"
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Active Connections"
            value={activeConnections}
            color="info"
          />
        </Grid>
      </Grid>

      {/* Performance Charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12}>
          <ChartCard title="System Performance" subtitle="Last 24 hours">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={performanceData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis yAxisId="left" />
                <YAxis yAxisId="right" orientation="right" />
                <Tooltip />
                <Legend />
                <Line yAxisId="left" type="monotone" dataKey="cpu" stroke="#1976d2" strokeWidth={2} name="CPU %" />
                <Line yAxisId="left" type="monotone" dataKey="memory" stroke="#dc004e" strokeWidth={2} name="Memory %" />
                <Line yAxisId="right" type="monotone" dataKey="requests" stroke="#4caf50" strokeWidth={2} name="Requests/min" />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
      </Grid>

      {/* Resource Usage */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Resource Usage
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ mb: 3 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">CPU</Typography>
                    <Typography variant="body2">{cpuUsage}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={cpuUsage}
                    color={cpuUsage > 80 ? 'error' : cpuUsage > 60 ? 'warning' : 'primary'}
                  />
                </Box>
                <Box sx={{ mb: 3 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Memory</Typography>
                    <Typography variant="body2">{memoryUsage}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={memoryUsage}
                    color={memoryUsage > 80 ? 'error' : memoryUsage > 60 ? 'warning' : 'primary'}
                  />
                </Box>
                <Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Disk</Typography>
                    <Typography variant="body2">{diskUsage}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={diskUsage}
                    color={diskUsage > 80 ? 'error' : diskUsage > 60 ? 'warning' : 'primary'}
                  />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Services Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Service Health
          </Typography>
          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Service Name</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Uptime</TableCell>
                  <TableCell>Response Time</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {status?.services && status.services.length > 0 ? (
                  status.services.map((service, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          {getStatusIcon(service.status as ServiceStatusType)}
                          {service.name}
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={getStatusLabel(service.status as ServiceStatusType)}
                          color={getStatusColor(service.status as ServiceStatusType)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        {(service as any).uptime || 'N/A'}
                      </TableCell>
                      <TableCell>
                        {service.responseTime ? `${service.responseTime}ms` : (service as any).responseTime || 'N/A'}
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={4} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No service data available
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SystemStatus;
