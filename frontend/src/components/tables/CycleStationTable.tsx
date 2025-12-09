/**
 * Cycle stations table component
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
  Box,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import type { CycleStation } from '@/types';

interface CycleStationTableProps {
  stations: CycleStation[];
  maxRows?: number;
}

export const CycleStationTable = ({
  stations,
  maxRows = 10,
}: CycleStationTableProps) => {
  const displayStations = stations.slice(0, maxRows);

  const StatusIcon = ({ status }: { status: boolean }) =>
    status ? (
      <CheckCircleIcon color="success" fontSize="small" />
    ) : (
      <CancelIcon color="error" fontSize="small" />
    );

  return (
    <Paper>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Station Name</TableCell>
              <TableCell>Address</TableCell>
              <TableCell align="center">Bikes</TableCell>
              <TableCell align="center">Docks</TableCell>
              <TableCell align="center">Occupancy</TableCell>
              <TableCell align="center">Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {displayStations.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                    No station data available
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              displayStations.map((station) => (
                <TableRow key={station.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight={500}>
                      {station.name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {station.address}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={station.numBikesAvailable}
                      size="small"
                      color="primary"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={station.numDocksAvailable}
                      size="small"
                      color="secondary"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Typography variant="body2">
                      {station.occupancyRate.toFixed(1)}%
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'center' }}>
                      <StatusIcon status={station.isRenting} />
                      <StatusIcon status={station.isReturning} />
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {stations.length > maxRows && (
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ p: 1, display: 'block', textAlign: 'center' }}
        >
          Showing {maxRows} of {stations.length} stations
        </Typography>
      )}
    </Paper>
  );
};
