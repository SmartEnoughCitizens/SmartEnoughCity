/**
 * Compact cycle stations table component
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
  Box,
} from "@mui/material";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";
import type { CycleStation } from "@/types";

interface CycleStationTableProps {
  stations: CycleStation[];
  maxRows?: number;
  /** Compact mode hides some columns */
  compact?: boolean;
}

const StatusIcon = ({ status }: { status: boolean }) =>
  status ? (
    <CheckCircleIcon color="success" fontSize="small" />
  ) : (
    <CancelIcon color="error" fontSize="small" />
  );

export const CycleStationTable = ({
  stations,
  maxRows = 10,
  compact = false,
}: CycleStationTableProps) => {
  const displayStations = stations.slice(0, maxRows);

  return (
    <>
      <TableContainer sx={{ maxHeight: compact ? 300 : undefined }}>
        <Table size="small" stickyHeader={compact}>
          <TableHead>
            <TableRow>
              <TableCell>Station</TableCell>
              {!compact && <TableCell>Address</TableCell>}
              <TableCell align="center">Bikes</TableCell>
              <TableCell align="center">Docks</TableCell>
              {!compact && <TableCell align="center">Occupancy</TableCell>}
              <TableCell align="center">Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {displayStations.length === 0 ? (
              <TableRow>
                <TableCell colSpan={compact ? 4 : 6} align="center">
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ py: 2 }}
                  >
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
                  {!compact && (
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {station.address}
                      </Typography>
                    </TableCell>
                  )}
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
                  {!compact && (
                    <TableCell align="center">
                      <Typography variant="body2">
                        {station.occupancyRate.toFixed(1)}%
                      </Typography>
                    </TableCell>
                  )}
                  <TableCell align="center">
                    <Box
                      sx={{
                        display: "flex",
                        gap: 0.5,
                        justifyContent: "center",
                      }}
                    >
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
          sx={{ p: 1, display: "block", textAlign: "center" }}
        >
          Showing {maxRows} of {stations.length} stations
        </Typography>
      )}
    </>
  );
};
