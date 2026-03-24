/**
 * Tram delay table — sortable list of delays by stop, line, direction
 */

import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Typography,
  Box,
} from "@mui/material";
import type { TramDelay } from "@/types";

interface TramDelayTableProps {
  delays: TramDelay[];
  maxRows?: number;
  compact?: boolean;
}

const lineColor = (line: string) =>
  line === "red" ? "error" : line === "green" ? "success" : "default";

const severityColor = (delayMins: number) => {
  if (delayMins >= 20) return "error.main";
  if (delayMins >= 10) return "warning.main";
  return "text.secondary";
};

export const TramDelayTable = ({
  delays,
  maxRows = 50,
  compact = false,
}: TramDelayTableProps) => {
  const sorted = delays.toSorted((a, b) => b.delayMins - a.delayMins);
  const displayed = sorted.slice(0, maxRows);

  return (
    <TableContainer>
      <Table size={compact ? "small" : "medium"}>
        <TableHead>
          <TableRow>
            <TableCell>Line</TableCell>
            <TableCell>Stop</TableCell>
            <TableCell>Direction</TableCell>
            <TableCell>Destination</TableCell>
            <TableCell align="right">Due (min)</TableCell>
            <TableCell align="right">Delay (min)</TableCell>
            {!compact && <TableCell align="right">Est. Affected</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {displayed.map((d, idx) => (
            <TableRow
              key={`${d.stopId}-${d.direction}-${d.destination}-${idx}`}
            >
              <TableCell>
                <Chip
                  label={d.line}
                  size="small"
                  color={lineColor(d.line)}
                  sx={{ textTransform: "capitalize", fontWeight: 600 }}
                />
              </TableCell>
              <TableCell>
                <Typography variant="body2" noWrap sx={{ maxWidth: 120 }}>
                  {d.stopName}
                </Typography>
              </TableCell>
              <TableCell>{d.direction}</TableCell>
              <TableCell>{d.destination}</TableCell>
              <TableCell align="right">
                <Typography variant="body2" fontWeight="bold">
                  {d.dueMins}
                </Typography>
              </TableCell>
              <TableCell align="right">
                <Box
                  sx={{
                    display: "inline-flex",
                    alignItems: "center",
                    gap: 0.5,
                  }}
                >
                  <Typography
                    variant="body2"
                    fontWeight="bold"
                    color={severityColor(d.delayMins)}
                  >
                    +{d.delayMins}
                  </Typography>
                </Box>
              </TableCell>
              {!compact && (
                <TableCell align="right">
                  <Typography variant="caption" color="text.secondary">
                    {d.estimatedAffectedPassengers > 0
                      ? `~${d.estimatedAffectedPassengers}`
                      : "—"}
                  </Typography>
                </TableCell>
              )}
            </TableRow>
          ))}
          {displayed.length === 0 && (
            <TableRow>
              <TableCell colSpan={compact ? 6 : 7} align="center">
                <Typography variant="body2" color="text.secondary">
                  No delays detected
                </Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
