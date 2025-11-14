import React, { useState, useEffect } from 'react';
import {
  Grid,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  LinearProgress,
} from '@mui/material';
import { Construction, Add, Refresh } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import CityMap from '../../components/common/CityMap';
import { indicatorService } from '../../services/indicatorService';
import { formatDate } from '../../utils/helpers';

const ConstructionIndicator = () => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchProjects();
  }, []);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const data = await indicatorService.getConstructionIndicators();
      setProjects(data);
    } catch (error) {
      console.error('Error fetching construction projects:', error);
      // Mock data
      setProjects(mockProjects);
    } finally {
      setLoading(false);
    }
  };

  const mockProjects = [
    {
      id: 1,
      name: 'Bridge Repair - North Side',
      location: 'North Bridge',
      startDate: new Date('2024-01-01'),
      endDate: new Date('2024-03-01'),
      status: 'In Progress',
      progress: 65,
      impact: 'High',
      lanesAffected: 2,
    },
    {
      id: 2,
      name: 'Road Resurfacing - Main St',
      location: 'Main Street',
      startDate: new Date('2024-02-01'),
      endDate: new Date('2024-02-15'),
      status: 'Scheduled',
      progress: 0,
      impact: 'Medium',
      lanesAffected: 1,
    },
    {
      id: 3,
      name: 'Subway Extension',
      location: 'City Center',
      startDate: new Date('2023-06-01'),
      endDate: new Date('2024-06-01'),
      status: 'In Progress',
      progress: 80,
      impact: 'Very High',
      lanesAffected: 4,
    },
  ];

  const getImpactColor = (impact) => {
    switch (impact) {
      case 'Very High':
        return 'error';
      case 'High':
        return 'warning';
      case 'Medium':
        return 'info';
      default:
        return 'success';
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'In Progress':
        return 'primary';
      case 'Scheduled':
        return 'warning';
      case 'Completed':
        return 'success';
      default:
        return 'default';
    }
  };

  const mapMarkers = projects.map((project, index) => ({
    position: [51.505 + index * 0.01, -0.09 + index * 0.01],
    popup: `${project.name} - ${project.progress}% Complete`,
  }));

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Construction sx={{ fontSize: 40 }} />
            Construction Projects
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Track construction projects and their impact on traffic flow
          </Typography>
        </Box>
        <Box>
          <Button variant="contained" startIcon={<Add />} sx={{ mr: 2 }}>
            Add Project
          </Button>
          <Button variant="outlined" startIcon={<Refresh />} onClick={fetchProjects}>
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Projects" value={projects.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="In Progress"
            value={projects.filter((p) => p.status === 'In Progress').length}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="High Impact"
            value={projects.filter((p) => p.impact === 'High' || p.impact === 'Very High').length}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Lanes Affected"
            value={projects.reduce((sum, p) => sum + p.lanesAffected, 0)}
            color="info"
          />
        </Grid>
      </Grid>

      {/* Projects Table */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Project Overview
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Project Name</TableCell>
                      <TableCell>Location</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Progress</TableCell>
                      <TableCell>Impact</TableCell>
                      <TableCell>End Date</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {projects.map((project) => (
                      <TableRow key={project.id}>
                        <TableCell>{project.name}</TableCell>
                        <TableCell>{project.location}</TableCell>
                        <TableCell>
                          <Chip label={project.status} color={getStatusColor(project.status)} size="small" />
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box sx={{ width: '100%', mr: 1 }}>
                              <LinearProgress variant="determinate" value={project.progress} />
                            </Box>
                            <Box sx={{ minWidth: 35 }}>
                              <Typography variant="body2" color="text.secondary">
                                {project.progress}%
                              </Typography>
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip label={project.impact} color={getImpactColor(project.impact)} size="small" />
                        </TableCell>
                        <TableCell>{formatDate(project.endDate)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={5}>
          <CityMap title="Construction Sites" markers={mapMarkers} height={400} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default ConstructionIndicator;
