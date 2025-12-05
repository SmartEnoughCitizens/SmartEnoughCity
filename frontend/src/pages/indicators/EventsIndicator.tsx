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
} from '@mui/material';
import { Event, Add, Refresh } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import CityMap from '../../components/common/CityMap';
import { useEventsIndicators } from '@/hooks';
import { formatDate } from '../../utils/helpers';
import type { EventIndicator } from '@/types';

const EventsIndicator: React.FC = () => {
  const { data: events = [], error, refetch } = useEventsIndicators();

  const mockEvents: EventIndicator[] = [
    {
      id: '1',
      name: 'Music Festival',
      type: 'Cultural',
      location: 'Central Park',
      startDate: '2024-01-15T00:00:00Z',
      endDate: '2024-01-15T23:59:59Z',
      status: 'planned',
      expectedImpact: 'high',
      affectedRoutes: ['Route 1', 'Route 2'],
    },
    {
      id: '2',
      name: 'Marathon Race',
      type: 'Sports',
      location: 'City Center',
      startDate: '2024-01-20T00:00:00Z',
      endDate: '2024-01-20T23:59:59Z',
      status: 'planned',
      expectedImpact: 'high',
      affectedRoutes: ['Route 3', 'Route 4', 'Route 5'],
    },
    {
      id: '3',
      name: 'Street Market',
      type: 'Commercial',
      location: 'Main Street',
      startDate: '2024-01-10T00:00:00Z',
      endDate: '2024-01-10T23:59:59Z',
      status: 'completed',
      expectedImpact: 'medium',
      affectedRoutes: ['Route 6'],
    },
  ];

  const displayEvents = error ? mockEvents : (events.length > 0 ? events : mockEvents);

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

  const getStatusColor = (status: string): 'primary' | 'success' | 'default' => {
    switch (status) {
      case 'planned':
        return 'primary';
      case 'active':
        return 'success';
      case 'completed':
        return 'default';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: string): string => {
    switch (status) {
      case 'planned':
        return 'Upcoming';
      case 'active':
        return 'Active';
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

  const mapMarkers = displayEvents.map((event, index) => ({
    position: [51.505 + index * 0.01, -0.09 + index * 0.01] as [number, number],
    popup: `${event.name} - ${event.location}`,
  }));

  // Calculate attendees estimate based on impact
  const getAttendees = (impact: string): number => {
    switch (impact) {
      case 'high':
        return Math.floor(Math.random() * 5000) + 5000;
      case 'medium':
        return Math.floor(Math.random() * 2000) + 2000;
      case 'low':
        return Math.floor(Math.random() * 1000) + 500;
      default:
        return 1000;
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Event sx={{ fontSize: 40 }} />
            Events Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Monitor and manage city events and their traffic impact
          </Typography>
        </Box>
        <Box>
          <Button variant="contained" startIcon={<Add />} sx={{ mr: 2 }}>
            Add Event
          </Button>
          <Button variant="outlined" startIcon={<Refresh />} onClick={() => refetch()}>
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Events" value={displayEvents.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Upcoming Events"
            value={displayEvents.filter((e) => e.status === 'planned').length}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="High Impact"
            value={displayEvents.filter((e) => e.expectedImpact === 'high').length}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Avg Attendees"
            value={Math.round(
              displayEvents.reduce((sum, e) => sum + getAttendees(e.expectedImpact), 0) / displayEvents.length || 0
            )}
            color="info"
          />
        </Grid>
      </Grid>

      {/* Events Table */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={7}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Event Schedule
              </Typography>
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Event Name</TableCell>
                      <TableCell>Location</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Impact</TableCell>
                      <TableCell align="right">Est. Attendees</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {displayEvents.map((event) => (
                      <TableRow key={event.id}>
                        <TableCell>{event.name}</TableCell>
                        <TableCell>{event.location}</TableCell>
                        <TableCell>{formatDate(event.startDate)}</TableCell>
                        <TableCell>
                          <Chip label={getStatusLabel(event.status)} color={getStatusColor(event.status)} size="small" />
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={getImpactLabel(event.expectedImpact)}
                            color={getImpactColor(event.expectedImpact)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell align="right">{getAttendees(event.expectedImpact).toLocaleString()}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={5}>
          <CityMap title="Event Locations" markers={mapMarkers} height={400} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default EventsIndicator;
