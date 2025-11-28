import React from 'react';
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
import { useConstructionIndicators } from '@/hooks';
import { formatDate } from '../../utils/helpers';
import { ConstructionIndicator as ConstructionIndicatorType } from '@/types';

const ConstructionIndicator: React.FC = () => {
  const { data: projects = [], error, refetch } = useConstructionIndicators();

  const mockProjects: ConstructionIndicatorType[] = [
    {
      id: '1',
      projectName: 'Bridge Repair - North Side',
      location: 'North Bridge',
      startDate: '2024-01-01T00:00:00Z',
      estimatedEndDate: '2024-03-01T00:00:00Z',
      status: 'in-progress',
      progress: 65,
      impact: 'high',
      affectedAreas: ['North Bridge', 'River Road'],
    },
    {
      id: '2',
      projectName: 'Road Resurfacing - Main St',
      location: 'Main Street',
      startDate: '2024-02-01T00:00:00Z',
      estimatedEndDate: '2024-02-15T00:00:00Z',
      status: 'planned',
      progress: 0,
      impact: 'medium',
      affectedAreas: ['Main Street'],
    },
    {
      id: '3',
      projectName: 'Subway Extension',
      location: 'City Center',
      startDate: '2023-06-01T00:00:00Z',
      estimatedEndDate: '2024-06-01T00:00:00Z',
      status: 'in-progress',
      progress: 80,
      impact: 'high',
      affectedAreas: ['City Center', 'Station Plaza', 'Underground Tunnel', 'Access Roads'],
    },
  ];

  const displayProjects = error ? mockProjects : (projects.length > 0 ? projects : mockProjects);

  const getImpactColor = (impact: string): 'error' | 'warning' | 'info' | 'success' => {
    switch (impact) {
      case 'high':
        return 'error';
      case 'medium':
        return 'warning';
      case 'low':
        return 'success';
      default:
        return 'info';
    }
  };

  const getStatusColor = (status: string): 'primary' | 'warning' | 'success' | 'default' => {
    switch (status) {
      case 'in-progress':
        return 'primary';
      case 'planned':
        return 'warning';
      case 'completed':
        return 'success';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: string): string => {
    switch (status) {
      case 'in-progress':
        return 'In Progress';
      case 'planned':
        return 'Scheduled';
      case 'completed':
        return 'Completed';
      default:
        return status;
    }
  };

  const getImpactLabel = (impact: string): string => {
    switch (impact) {
      case 'high':
        return 'High';
      case 'medium':
        return 'Medium';
      case 'low':
        return 'Low';
      default:
        return impact;
    }
  };

  const mapMarkers = displayProjects.map((project, index) => ({
    position: [51.505 + index * 0.01, -0.09 + index * 0.01] as [number, number],
    popup: `${project.projectName} - ${project.progress}% Complete`,
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
          <Button variant="outlined" startIcon={<Refresh />} onClick={() => refetch()}>
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Projects" value={displayProjects.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="In Progress"
            value={displayProjects.filter((p) => p.status === 'in-progress').length}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="High Impact"
            value={displayProjects.filter((p) => p.impact === 'high').length}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Areas Affected"
            value={displayProjects.reduce((sum, p) => sum + p.affectedAreas.length, 0)}
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
                    {displayProjects.map((project) => (
                      <TableRow key={project.id}>
                        <TableCell>{project.projectName}</TableCell>
                        <TableCell>{project.location}</TableCell>
                        <TableCell>
                          <Chip label={getStatusLabel(project.status)} color={getStatusColor(project.status)} size="small" />
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
                          <Chip label={getImpactLabel(project.impact)} color={getImpactColor(project.impact)} size="small" />
                        </TableCell>
                        <TableCell>{formatDate(project.estimatedEndDate)}</TableCell>
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
