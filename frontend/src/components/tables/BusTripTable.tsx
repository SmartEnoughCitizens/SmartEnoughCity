/**
 * Bus trip updates table component
 */

import {
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
} from '@mui/material';
import type { BusTripUpdate } from '@/types';

interface BusTripTableProps {
  trips: BusTripUpdate[];
  maxRows?: number;
}

export const BusTripTable = ({ trips, maxRows = 10 }: BusTripTableProps) => {
  const displayTrips = trips.slice(0, maxRows);

  const getDelayColor = (delay: number | null) => {
    if (delay === null) return 'default';
    if (delay <= 60) return 'success';
    if (delay <= 180) return 'warning';
    return 'error';
  };

  const formatDelay = (delay: number | null) => {
    if (delay === null) return 'N/A';
    const minutes = Math.floor(Math.abs(delay) / 60);
    const seconds = Math.abs(delay) % 60;
    return `${delay < 0 ? '-' : ''}${minutes}m ${seconds}s`;
  };

  return (
    <Paper>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Route ID</TableCell>
              <TableCell>Trip ID</TableCell>
              <TableCell>Stop ID</TableCell>
              <TableCell>Start Time</TableCell>
              <TableCell align="center">Arrival Delay</TableCell>
              <TableCell align="center">Departure Delay</TableCell>
              <TableCell>Schedule</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {displayTrips.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                    No trip data available
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              displayTrips.map((trip) => (
                <TableRow key={trip.id} hover>
                  <TableCell>
                    <Chip label={trip.routeId} size="small" color="primary" />
                  </TableCell>
                  <TableCell>{trip.tripId}</TableCell>
                  <TableCell>{trip.stopId}</TableCell>
                  <TableCell>{trip.startTime}</TableCell>
                  <TableCell align="center">
                    <Chip
                      label={formatDelay(trip.arrivalDelay)}
                      size="small"
                      color={getDelayColor(trip.arrivalDelay)}
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={formatDelay(trip.departureDelay)}
                      size="small"
                      color={getDelayColor(trip.departureDelay)}
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption">{trip.scheduleRelationship}</Typography>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {trips.length > maxRows && (
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ p: 1, display: 'block', textAlign: 'center' }}
        >
          Showing {maxRows} of {trips.length} trips
        </Typography>
      )}
    </Paper>
  );
};
