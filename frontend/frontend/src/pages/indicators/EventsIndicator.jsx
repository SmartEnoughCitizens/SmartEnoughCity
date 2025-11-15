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
} from '@mui/material';
import { Event, Add, Refresh } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import CityMap from '../../components/common/CityMap';
import { indicatorService } from '../../services/indicatorService';
import { formatDate } from '../../utils/helpers';

const EventsIndicator = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchEvents();
  }, []);

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const data = await indicatorService.getEventsIndicators();
      setEvents(data);
    } catch (error) {
      console.error('Error fetching events:', error);
      // Mock data
      setEvents(mockEvents);
    } finally {
      setLoading(false);
    }
  };

  const mockEvents = [
    {
      id: 1,
      name: 'Music Festival',
      location: 'Central Park',
      date: new Date('2024-01-15'),
      status: 'Upcoming',
      impact: 'High',
      attendees: 5000,
    },
    {
      id: 2,
      name: 'Marathon Race',
      location: 'City Center',
      date: new Date('2024-01-20'),
      status: 'Upcoming',
      impact: 'Very High',
      attendees: 10000,
    },
    {
      id: 3,
      name: 'Street Market',
      location: 'Main Street',
      date: new Date('2024-01-10'),
      status: 'Completed',
      impact: 'Medium',
      attendees: 2000,
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
      case 'Upcoming':
        return 'primary';
      case 'Active':
        return 'success';
      case 'Completed':
        return 'default';
      default:
        return 'default';
    }
  };

  const mapMarkers = events.map((event, index) => ({
    position: [51.505 + index * 0.01, -0.09 + index * 0.01],
    popup: `${event.name} - ${event.location}`,
  }));

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
          <Button variant="outlined" startIcon={<Refresh />} onClick={fetchEvents}>
            Refresh
          </Button>
        </Box>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Events" value={events.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Upcoming Events"
            value={events.filter((e) => e.status === 'Upcoming').length}
            color="warning"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="High Impact"
            value={events.filter((e) => e.impact === 'High' || e.impact === 'Very High').length}
            color="error"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Avg Attendees"
            value={Math.round(events.reduce((sum, e) => sum + e.attendees, 0) / events.length || 0)}
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
                      <TableCell align="right">Attendees</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {events.map((event) => (
                      <TableRow key={event.id}>
                        <TableCell>{event.name}</TableCell>
                        <TableCell>{event.location}</TableCell>
                        <TableCell>{formatDate(event.date)}</TableCell>
                        <TableCell>
                          <Chip label={event.status} color={getStatusColor(event.status)} size="small" />
                        </TableCell>
                        <TableCell>
                          <Chip label={event.impact} color={getImpactColor(event.impact)} size="small" />
                        </TableCell>
                        <TableCell align="right">{event.attendees.toLocaleString()}</TableCell>
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
