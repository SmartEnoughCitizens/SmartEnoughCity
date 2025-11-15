import React, { useState, useEffect } from 'react';
import {
  Grid,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { Science, Add, PlayArrow, Delete } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import ChartCard from '../../components/common/ChartCard';
import { simulationService } from '../../services/simulationService';
import { formatDateTime } from '../../utils/helpers';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const SimulationCenter = () => {
  const [simulations, setSimulations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [newSimulation, setNewSimulation] = useState({
    name: '',
    transportMode: 'CAR',
    duration: 60,
    scenario: 'NORMAL',
  });

  useEffect(() => {
    fetchSimulations();
  }, []);

  const fetchSimulations = async () => {
    setLoading(true);
    try {
      const data = await simulationService.getAllSimulations();
      setSimulations(data);
    } catch (error) {
      console.error('Error fetching simulations:', error);
      setSimulations(mockSimulations);
    } finally {
      setLoading(false);
    }
  };

  const mockSimulations = [
    {
      id: 1,
      name: 'Peak Hour Traffic - Car',
      transportMode: 'CAR',
      status: 'Completed',
      createdAt: new Date('2024-01-10'),
      duration: 60,
      result: { efficiency: 72, avgSpeed: 35, congestion: 68 },
    },
    {
      id: 2,
      name: 'Weekend Bus Routes',
      transportMode: 'BUS',
      status: 'Running',
      createdAt: new Date('2024-01-14'),
      duration: 120,
      result: null,
    },
    {
      id: 3,
      name: 'Bike Lane Expansion',
      transportMode: 'CYCLE',
      status: 'Completed',
      createdAt: new Date('2024-01-12'),
      duration: 90,
      result: { efficiency: 85, avgSpeed: 18, congestion: 15 },
    },
  ];

  const handleOpenDialog = () => {
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setNewSimulation({
      name: '',
      transportMode: 'CAR',
      duration: 60,
      scenario: 'NORMAL',
    });
  };

  const handleRunSimulation = async () => {
    try {
      await simulationService.runSimulation(newSimulation);
      fetchSimulations();
      handleCloseDialog();
    } catch (error) {
      console.error('Error running simulation:', error);
    }
  };

  const handleDeleteSimulation = async (id) => {
    try {
      await simulationService.deleteSimulation(id);
      fetchSimulations();
    } catch (error) {
      console.error('Error deleting simulation:', error);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'Running':
        return 'primary';
      case 'Completed':
        return 'success';
      case 'Failed':
        return 'error';
      default:
        return 'default';
    }
  };

  const simulationTrendData = [
    { time: '0m', efficiency: 100, congestion: 20 },
    { time: '15m', efficiency: 85, congestion: 45 },
    { time: '30m', efficiency: 75, congestion: 65 },
    { time: '45m', efficiency: 72, congestion: 68 },
    { time: '60m', efficiency: 78, congestion: 55 },
  ];

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Science sx={{ fontSize: 40 }} />
            Simulation Center
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Run traffic simulations to predict and optimize transport scenarios
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={handleOpenDialog}>
          New Simulation
        </Button>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Simulations" value={simulations.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Running"
            value={simulations.filter((s) => s.status === 'Running').length}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Completed"
            value={simulations.filter((s) => s.status === 'Completed').length}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Avg Efficiency"
            value={75}
            unit="%"
            color="info"
          />
        </Grid>
      </Grid>

      {/* Simulation Results Chart */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12}>
          <ChartCard title="Latest Simulation Results" subtitle="Efficiency vs Congestion over time">
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={simulationTrendData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="time" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="efficiency" stroke="#4caf50" strokeWidth={2} name="Efficiency %" />
                <Line type="monotone" dataKey="congestion" stroke="#f44336" strokeWidth={2} name="Congestion %" />
              </LineChart>
            </ResponsiveContainer>
          </ChartCard>
        </Grid>
      </Grid>

      {/* Simulations Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Simulation History
          </Typography>
          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Transport Mode</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Duration</TableCell>
                  <TableCell>Results</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {simulations.map((simulation) => (
                  <TableRow key={simulation.id}>
                    <TableCell>{simulation.name}</TableCell>
                    <TableCell>{simulation.transportMode}</TableCell>
                    <TableCell>
                      <Chip label={simulation.status} color={getStatusColor(simulation.status)} size="small" />
                    </TableCell>
                    <TableCell>{formatDateTime(simulation.createdAt)}</TableCell>
                    <TableCell>{simulation.duration} min</TableCell>
                    <TableCell>
                      {simulation.result ? (
                        <Box>
                          <Typography variant="caption" display="block">
                            Efficiency: {simulation.result.efficiency}%
                          </Typography>
                          <Typography variant="caption" display="block">
                            Congestion: {simulation.result.congestion}%
                          </Typography>
                        </Box>
                      ) : (
                        '-'
                      )}
                    </TableCell>
                    <TableCell>
                      <Button
                        size="small"
                        color="error"
                        startIcon={<Delete />}
                        onClick={() => handleDeleteSimulation(simulation.id)}
                      >
                        Delete
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* New Simulation Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Run New Simulation</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              fullWidth
              label="Simulation Name"
              value={newSimulation.name}
              onChange={(e) => setNewSimulation({ ...newSimulation, name: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Transport Mode</InputLabel>
              <Select
                value={newSimulation.transportMode}
                onChange={(e) => setNewSimulation({ ...newSimulation, transportMode: e.target.value })}
                label="Transport Mode"
              >
                <MenuItem value="CAR">Car</MenuItem>
                <MenuItem value="BUS">Bus</MenuItem>
                <MenuItem value="CYCLE">Cycle</MenuItem>
                <MenuItem value="TRAIN">Train</MenuItem>
                <MenuItem value="TRAM">Tram</MenuItem>
                <MenuItem value="PEDESTRIAN">Pedestrian</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Scenario</InputLabel>
              <Select
                value={newSimulation.scenario}
                onChange={(e) => setNewSimulation({ ...newSimulation, scenario: e.target.value })}
                label="Scenario"
              >
                <MenuItem value="NORMAL">Normal Traffic</MenuItem>
                <MenuItem value="PEAK_HOUR">Peak Hour</MenuItem>
                <MenuItem value="EVENT">Special Event</MenuItem>
                <MenuItem value="EMERGENCY">Emergency</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              type="number"
              label="Duration (minutes)"
              value={newSimulation.duration}
              onChange={(e) => setNewSimulation({ ...newSimulation, duration: parseInt(e.target.value) })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            variant="contained"
            startIcon={<PlayArrow />}
            onClick={handleRunSimulation}
            disabled={!newSimulation.name}
          >
            Run Simulation
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SimulationCenter;
