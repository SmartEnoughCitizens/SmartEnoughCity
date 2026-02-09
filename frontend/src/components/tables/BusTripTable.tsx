/**
 * Compact bus trip updates table component
 */

import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
} from "@mui/material";
import type { BusTripUpdate } from "@/types";

interface BusTripTableProps {
  trips: BusTripUpdate[];
  maxRows?: number;
  /** Compact mode hides some columns */
  compact?: boolean;
}

const getDelayColor = (delay: number | null) => {
  if (delay === null) return "default";
  if (delay <= 60) return "success";
  if (delay <= 180) return "warning";
  return "error";
};

const formatDelay = (delay: number | null) => {
  if (delay === null) return "N/A";
  const minutes = Math.floor(Math.abs(delay) / 60);
  const seconds = Math.abs(delay) % 60;
  return `${delay < 0 ? "-" : ""}${minutes}m ${seconds}s`;
};

export const BusTripTable = ({
  trips,
  maxRows = 10,
  compact = false,
}: BusTripTableProps) => {
  const displayTrips = trips.slice(0, maxRows);

  return (
    <>
      <TableContainer sx={{ maxHeight: compact ? 300 : undefined }}>
        <Table size="small" stickyHeader={compact}>
          <TableHead>
            <TableRow>
              <TableCell>Route</TableCell>
              {!compact && <TableCell>Trip ID</TableCell>}
              <TableCell>Stop</TableCell>
              {!compact && <TableCell>Start</TableCell>}
              <TableCell align="center">Arr. Delay</TableCell>
              <TableCell align="center">Dep. Delay</TableCell>
              {!compact && <TableCell>Schedule</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {displayTrips.length === 0 ? (
              <TableRow>
                <TableCell colSpan={compact ? 4 : 7} align="center">
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ py: 2 }}
                  >
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
                  {!compact && <TableCell>{trip.tripId}</TableCell>}
                  <TableCell>{trip.stopId}</TableCell>
                  {!compact && <TableCell>{trip.startTime}</TableCell>}
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
                  {!compact && (
                    <TableCell>
                      <Typography variant="caption">
                        {trip.scheduleRelationship}
                      </Typography>
                    </TableCell>
                  )}
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
          sx={{ p: 1, display: "block", textAlign: "center" }}
        >
          Showing {maxRows} of {trips.length} trips
        </Typography>
      )}
    </>
  );
};
