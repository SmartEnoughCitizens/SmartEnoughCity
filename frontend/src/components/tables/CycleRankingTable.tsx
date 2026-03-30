/**
 * Station ranking table (busiest / underused) — today's data only
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
import type { StationRankingDTO } from "@/types";

interface CycleRankingTableProps {
  stations: StationRankingDTO[];
  maxRows?: number;
}

export const CycleRankingTable = ({
  stations,
  maxRows = 10,
}: CycleRankingTableProps) => {
  const rows = stations.slice(0, maxRows);

  return (
    <>
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{ px: 1, pt: 0.5, display: "block" }}
      >
        Based on today's data
      </Typography>
      <TableContainer>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell>#</TableCell>
              <TableCell>Station</TableCell>
              <TableCell align="center">Bike usage %</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center">
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ py: 2 }}
                  >
                    No data available yet today
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((station, idx) => (
                <TableRow key={station.stationId} hover>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {idx + 1}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={500}>
                      {station.name}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={`${station.avgUsageRate.toFixed(0)}%`}
                      size="small"
                      color={
                        station.avgUsageRate >= 70
                          ? "error"
                          : station.avgUsageRate >= 40
                            ? "warning"
                            : "success"
                      }
                    />
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};
