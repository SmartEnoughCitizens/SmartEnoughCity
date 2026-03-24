/**
 * Live tram forecast table — sortable by line, stop, direction
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
} from "@mui/material";
import type { TramLiveForecast } from "@/types";

interface TramForecastTableProps {
  forecasts: TramLiveForecast[];
  maxRows?: number;
  compact?: boolean;
}

const lineColor = (line: string) =>
  line === "red" ? "error" : line === "green" ? "success" : "default";

export const TramForecastTable = ({
  forecasts,
  maxRows = 50,
  compact = false,
}: TramForecastTableProps) => {
  const displayed = forecasts.slice(0, maxRows);

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
            {!compact && <TableCell>Message</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {displayed.map((f, idx) => (
            <TableRow key={`${f.stopId}-${f.direction}-${f.destination}-${idx}`}>
              <TableCell>
                <Chip
                  label={f.line}
                  size="small"
                  color={lineColor(f.line)}
                  sx={{ textTransform: "capitalize", fontWeight: 600 }}
                />
              </TableCell>
              <TableCell>
                <Typography variant="body2" noWrap sx={{ maxWidth: 140 }}>
                  {f.stopName}
                </Typography>
              </TableCell>
              <TableCell>{f.direction}</TableCell>
              <TableCell>{f.destination}</TableCell>
              <TableCell align="right">
                <Typography
                  variant="body2"
                  fontWeight="bold"
                  color={
                    f.dueMins === null
                      ? "text.primary"
                      : f.dueMins <= 2
                        ? "success.main"
                        : f.dueMins > 10
                          ? "warning.main"
                          : "text.primary"
                  }
                >
                  {f.dueMins === null ? "—" : f.dueMins}
                </Typography>
              </TableCell>
              {!compact && (
                <TableCell>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    noWrap
                    sx={{ maxWidth: 180 }}
                  >
                    {f.message || "—"}
                  </Typography>
                </TableCell>
              )}
            </TableRow>
          ))}
          {displayed.length === 0 && (
            <TableRow>
              <TableCell colSpan={compact ? 5 : 6} align="center">
                <Typography variant="body2" color="text.secondary">
                  No live forecasts available
                </Typography>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
