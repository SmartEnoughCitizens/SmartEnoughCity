import React, { useState, useEffect } from 'react';
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
} from '@mui/material';
import { Settings, Refresh, CheckCircle, Error, Warning } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import ChartCard from '../../components/common/ChartCard';
import { systemService } from '../../services/systemService';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const SystemStatus = () => {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchSystemStatus();
    // Poll every 30 seconds
    const interval = setInterval(fetchSystemStatus, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchSystemStatus = async () => {
    setLoading(true);
    try {
      const data = await systemService.getStatus();
      setStatus(data);
    } catch (error) {
      console.error('Error fetching system status:', error);
      setStatus(mockStatus);
    } finally {
      setLoading(false);
    }
  };

  const mockStatus = {
    overall: 'Healthy',
    uptime: '15d 7h 32m',
    cpuUsage: 45,
    memoryUsage: 68,
    diskUsage: 52,
    activeConnections: 247,
    services: [
      { name: 'API Gateway', status: 'Running', uptime: '99.9%', responseTime: '45ms' },
      { name: 'Database', status: 'Running', uptime: '100%', responseTime: '12ms' },
      { name: 'Cache Server', status: 'Running', uptime: '99.8%', responseTime: '5ms' },
      { name: 'Message Queue', status: 'Running', uptime: '99.9%', responseTime: '8ms' },
      { name: 'Data Analytics', status: 'Warning', uptime: '98.5%', responseTime: '120ms' },
      { name: 'Notification Service', status: 'Running', uptime: '99.7%', responseTime: '35ms' },
    ],
  };

  const performanceData = [
    { time: '00:00', cpu: 35, memory: 55, requests: 120 },
    { time: '04:00', cpu: 25, memory: 52, requests: 80 },
    { time: '08:00', cpu: 55, memory: 65, requests: 280 },
    { time: '12:00', cpu: 65, memory: 72, requests: 350 },
    { time: '16:00', cpu: 58, memory: 68, requests: 320 },
    { time: '20:00', cpu: 42, memory: 60, requests: 200 },
  ];

  const getStatusIcon = (status) => {
    switch (status) {
      case 'Running':
        return <CheckCircle sx={{ color: 'success.main' }} />;
      case 'Warning':
        return <Warning sx={{ color: 'warning.main' }} />;
      case 'Error':
        return <Error sx={{ color: 'error.main' }} />;
      default:
        return null;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'Running':
        return 'success';
      case 'Warning':
        return 'warning';
      case 'Error':
        return 'error';
      default:
        return 'default';
    }
  };

  const getOverallStatus = () => {
    if (!status) return { color: 'default', text: 'Loading...' };
    const errorCount = status.services.filter((s) => s.status === 'Error').length;
    const warningCount = status.services.filter((s) => s.status === 'Warning').length;

    if (errorCount > 0) return { color: 'error', text: 'Critical' };
    if (warningCount > 0) return { color: 'warning', text: 'Warning' };
    return { color: 'success', text: 'Healthy' };
  };

  const overallStatus = getOverallStatus();

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
        <Button variant="outlined" startIcon={<Refresh />} onClick={fetchSystemStatus}>
          Refresh
        </Button>
      </Box>

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
                Uptime: {status?.uptime || 'N/A'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="CPU Usage"
            value={status?.cpuUsage || 0}
            unit="%"
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Memory Usage"
            value={status?.memoryUsage || 0}
            unit="%"
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Active Connections"
            value={status?.activeConnections || 0}
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
                    <Typography variant="body2">{status?.cpuUsage || 0}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={status?.cpuUsage || 0}
                    color={status?.cpuUsage > 80 ? 'error' : status?.cpuUsage > 60 ? 'warning' : 'primary'}
                  />
                </Box>
                <Box sx={{ mb: 3 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Memory</Typography>
                    <Typography variant="body2">{status?.memoryUsage || 0}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={status?.memoryUsage || 0}
                    color={status?.memoryUsage > 80 ? 'error' : status?.memoryUsage > 60 ? 'warning' : 'primary'}
                  />
                </Box>
                <Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Disk</Typography>
                    <Typography variant="body2">{status?.diskUsage || 0}%</Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={status?.diskUsage || 0}
                    color={status?.diskUsage > 80 ? 'error' : status?.diskUsage > 60 ? 'warning' : 'primary'}
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
                {status?.services.map((service, index) => (
                  <TableRow key={index}>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        {getStatusIcon(service.status)}
                        {service.name}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip label={service.status} color={getStatusColor(service.status)} size="small" />
                    </TableCell>
                    <TableCell>{service.uptime}</TableCell>
                    <TableCell>{service.responseTime}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  );
};

export default SystemStatus;
