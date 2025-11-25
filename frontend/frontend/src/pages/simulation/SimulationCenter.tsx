import React, { useState } from 'react';
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
  SelectChangeEvent,
} from '@mui/material';
import { Science, Add, PlayArrow, Delete } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import ChartCard from '../../components/common/ChartCard';
import { useSimulations, useRunSimulation, useDeleteSimulation } from '../../hooks/useSimulations';
import { formatDateTime } from '../../utils/helpers';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { CreateSimulationRequest, SimulationScenario, SimulationStatus } from '../../types';

interface NewSimulationForm {
  name: string;
  description: string;
  scenario: SimulationScenario;
  duration: number;
  trafficIncrease: number;
  weatherConditions: 'clear' | 'rain' | 'snow' | 'fog';
  timeOfDay: 'morning' | 'midday' | 'evening' | 'night';
}

const SimulationCenter: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false);
  const [newSimulation, setNewSimulation] = useState<NewSimulationForm>({
    name: '',
    description: '',
    scenario: 'traffic-increase',
    duration: 60,
    trafficIncrease: 20,
    weatherConditions: 'clear',
    timeOfDay: 'morning',
  });

  // TanStack Query hooks
  const { data: simulations = [], isLoading } = useSimulations();
  const runSimulation = useRunSimulation();
  const deleteSimulation = useDeleteSimulation();

  const handleOpenDialog = () => {
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setNewSimulation({
      name: '',
      description: '',
      scenario: 'traffic-increase',
      duration: 60,
      trafficIncrease: 20,
      weatherConditions: 'clear',
      timeOfDay: 'morning',
    });
  };

  const handleRunSimulation = () => {
    const simulationRequest: CreateSimulationRequest = {
      name: newSimulation.name,
      description: newSimulation.description,
      scenario: newSimulation.scenario,
      parameters: {
        duration: newSimulation.duration,
        trafficIncrease: newSimulation.trafficIncrease,
        weatherConditions: newSimulation.weatherConditions,
        timeOfDay: newSimulation.timeOfDay,
      },
    };

    runSimulation.mutate(simulationRequest, {
      onSuccess: () => {
        handleCloseDialog();
      },
      onError: (error) => {
        console.error('Error running simulation:', error);
      },
    });
  };

  const handleDeleteSimulation = (id: string) => {
    deleteSimulation.mutate(id, {
      onError: (error) => {
        console.error('Error deleting simulation:', error);
      },
    });
  };

  const getStatusColor = (status: SimulationStatus): 'primary' | 'success' | 'error' | 'warning' | 'default' => {
    switch (status) {
      case 'running':
        return 'primary';
      case 'completed':
        return 'success';
      case 'failed':
        return 'error';
      case 'pending':
        return 'warning';
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

  const calculateAverageEfficiency = (): number => {
    const completedSimulations = simulations.filter((s) => s.status === 'completed' && s.results);
    if (completedSimulations.length === 0) return 0;

    const totalEfficiency = completedSimulations.reduce((sum, sim) => {
      const avgSpeed = sim.results?.summary.avgSpeed || 0;
      const congestionLevel = sim.results?.summary.congestionLevel || 0;
      // Calculate efficiency as inverse of congestion relative to speed
      const efficiency = avgSpeed > 0 ? Math.max(0, 100 - congestionLevel) : 0;
      return sum + efficiency;
    }, 0);

    return Math.round(totalEfficiency / completedSimulations.length);
  };

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
            value={simulations.filter((s) => s.status === 'running').length}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Completed"
            value={simulations.filter((s) => s.status === 'completed').length}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Avg Efficiency"
            value={calculateAverageEfficiency()}
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
                  <TableCell>Scenario</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Duration</TableCell>
                  <TableCell>Results</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {isLoading ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      Loading...
                    </TableCell>
                  </TableRow>
                ) : simulations.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      No simulations found. Create your first simulation to get started.
                    </TableCell>
                  </TableRow>
                ) : (
                  simulations.map((simulation) => (
                    <TableRow key={simulation.id}>
                      <TableCell>{simulation.name}</TableCell>
                      <TableCell sx={{ textTransform: 'capitalize' }}>
                        {simulation.scenario.replace('-', ' ')}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={simulation.status}
                          color={getStatusColor(simulation.status)}
                          size="small"
                          sx={{ textTransform: 'capitalize' }}
                        />
                      </TableCell>
                      <TableCell>{formatDateTime(simulation.createdAt)}</TableCell>
                      <TableCell>
                        {simulation.parameters.duration ? `${simulation.parameters.duration} min` : '-'}
                      </TableCell>
                      <TableCell>
                        {simulation.results ? (
                          <Box>
                            <Typography variant="caption" display="block">
                              Avg Speed: {simulation.results.summary.avgSpeed} km/h
                            </Typography>
                            <Typography variant="caption" display="block">
                              Congestion: {simulation.results.summary.congestionLevel}%
                            </Typography>
                            <Typography variant="caption" display="block">
                              Total Delay: {simulation.results.summary.totalDelay} min
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
                          disabled={deleteSimulation.isPending}
                        >
                          Delete
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
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
            <TextField
              fullWidth
              label="Description"
              multiline
              rows={2}
              value={newSimulation.description}
              onChange={(e) => setNewSimulation({ ...newSimulation, description: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Scenario</InputLabel>
              <Select
                value={newSimulation.scenario}
                onChange={(e: SelectChangeEvent) =>
                  setNewSimulation({ ...newSimulation, scenario: e.target.value as SimulationScenario })
                }
                label="Scenario"
              >
                <MenuItem value="traffic-increase">Traffic Increase</MenuItem>
                <MenuItem value="event-impact">Event Impact</MenuItem>
                <MenuItem value="construction-impact">Construction Impact</MenuItem>
                <MenuItem value="transit-optimization">Transit Optimization</MenuItem>
                <MenuItem value="emergency-response">Emergency Response</MenuItem>
                <MenuItem value="custom">Custom</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              type="number"
              label="Duration (minutes)"
              value={newSimulation.duration}
              onChange={(e) => setNewSimulation({ ...newSimulation, duration: parseInt(e.target.value) || 0 })}
            />
            <TextField
              fullWidth
              type="number"
              label="Traffic Increase (%)"
              value={newSimulation.trafficIncrease}
              onChange={(e) => setNewSimulation({ ...newSimulation, trafficIncrease: parseInt(e.target.value) || 0 })}
            />
            <FormControl fullWidth>
              <InputLabel>Weather Conditions</InputLabel>
              <Select
                value={newSimulation.weatherConditions}
                onChange={(e: SelectChangeEvent) =>
                  setNewSimulation({ ...newSimulation, weatherConditions: e.target.value as 'clear' | 'rain' | 'snow' | 'fog' })
                }
                label="Weather Conditions"
              >
                <MenuItem value="clear">Clear</MenuItem>
                <MenuItem value="rain">Rain</MenuItem>
                <MenuItem value="snow">Snow</MenuItem>
                <MenuItem value="fog">Fog</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Time of Day</InputLabel>
              <Select
                value={newSimulation.timeOfDay}
                onChange={(e: SelectChangeEvent) =>
                  setNewSimulation({ ...newSimulation, timeOfDay: e.target.value as 'morning' | 'midday' | 'evening' | 'night' })
                }
                label="Time of Day"
              >
                <MenuItem value="morning">Morning</MenuItem>
                <MenuItem value="midday">Midday</MenuItem>
                <MenuItem value="evening">Evening</MenuItem>
                <MenuItem value="night">Night</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            variant="contained"
            startIcon={<PlayArrow />}
            onClick={handleRunSimulation}
            disabled={!newSimulation.name || !newSimulation.description || runSimulation.isPending}
          >
            {runSimulation.isPending ? 'Running...' : 'Run Simulation'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default SimulationCenter;
