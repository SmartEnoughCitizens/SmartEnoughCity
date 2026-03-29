/**
 * Station event table — shows recent empty / full events
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
import type { StationEventDTO } from "@/types";

interface CycleEventTableProps {
  events: StationEventDTO[];
  maxRows?: number;
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString(undefined, {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

export const CycleEventTable = ({
  events,
  maxRows = 30,
}: CycleEventTableProps) => {
  const rows = events.slice(0, maxRows);

  return (
    <TableContainer>
      <Table size="small" stickyHeader>
        <TableHead>
          <TableRow>
            <TableCell>Station</TableCell>
            <TableCell>Time</TableCell>
            <TableCell align="center">Event</TableCell>
            <TableCell align="center">Bikes</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.length === 0 ? (
            <TableRow>
              <TableCell colSpan={4} align="center">
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ py: 2 }}
                >
                  No events found
                </Typography>
              </TableCell>
            </TableRow>
          ) : (
            rows.map((event, idx) => (
              <TableRow
                key={`${event.stationId}-${event.eventTime}-${idx}`}
                hover
              >
                <TableCell>
                  <Typography variant="body2" fontWeight={500}>
                    {event.stationName}
                  </Typography>
                </TableCell>
                <TableCell>
                  <Typography variant="caption" color="text.secondary">
                    {formatTime(event.eventTime)}
                  </Typography>
                </TableCell>
                <TableCell align="center">
                  <Chip
                    label={event.eventType}
                    size="small"
                    color={event.eventType === "EMPTY" ? "error" : "warning"}
                  />
                </TableCell>
                <TableCell align="center">
                  <Typography variant="body2">
                    {event.prevAvailableBikes} → {event.availableBikes}
                  </Typography>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
