/**
 * Live cycle stations table — uses StationLiveDTO from CycleMetricsController
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
import type { StationLiveDTO } from "@/types";

interface LiveCycleStationTableProps {
  stations: StationLiveDTO[];
  maxRows?: number;
  compact?: boolean;
}

const StatusIcon = ({ status }: { status: boolean }) =>
  status ? (
    <CheckCircleIcon color="success" fontSize="small" />
  ) : (
    <CancelIcon color="error" fontSize="small" />
  );

const STATUS_CHIP_COLOR: Record<string, "success" | "warning" | "error"> = {
  GREEN: "success",
  YELLOW: "warning",
  RED: "error",
};

export const LiveCycleStationTable = ({
  stations,
  maxRows = 10,
  compact = false,
}: LiveCycleStationTableProps) => {
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
              <TableCell align="center">Bikes %</TableCell>
              <TableCell align="center">Docks %</TableCell>
              {!compact && <TableCell align="center">Status</TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {displayStations.length === 0 ? (
              <TableRow>
                <TableCell colSpan={compact ? 5 : 7} align="center">
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
                <TableRow key={station.stationId} hover>
                  <TableCell>
                    <Box
                      sx={{ display: "flex", alignItems: "center", gap: 0.75 }}
                    >
                      <Box
                        sx={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          bgcolor:
                            station.statusColor === "GREEN"
                              ? "success.main"
                              : station.statusColor === "YELLOW"
                                ? "warning.main"
                                : "error.main",
                          flexShrink: 0,
                        }}
                      />
                      <Typography variant="body2" fontWeight={500}>
                        {station.name}
                      </Typography>
                    </Box>
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
                      label={station.availableBikes}
                      size="small"
                      color="primary"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={station.availableDocks}
                      size="small"
                      color="secondary"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={`${station.bikeAvailabilityPct.toFixed(0)}%`}
                      size="small"
                      color={
                        STATUS_CHIP_COLOR[station.statusColor] ?? "default"
                      }
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={`${station.dockAvailabilityPct.toFixed(0)}%`}
                      size="small"
                      color={
                        station.dockAvailabilityPct < 20
                          ? "error"
                          : station.dockAvailabilityPct < 40
                            ? "warning"
                            : "default"
                      }
                    />
                  </TableCell>
                  {!compact && (
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
                  )}
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
