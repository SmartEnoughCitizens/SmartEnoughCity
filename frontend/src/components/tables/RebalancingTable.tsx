/**
 * Rebalancing suggestions table — shows full stations that need bikes moved out
 * paired with the nearest empty station that needs bikes moved in.
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
  Tooltip,
} from "@mui/material";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import type { RebalanceSuggestionDTO } from "@/types";

interface RebalancingTableProps {
  suggestions: RebalanceSuggestionDTO[];
}

export const RebalancingTable = ({ suggestions }: RebalancingTableProps) => {
  if (suggestions.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: "center" }}>
        <Typography variant="body2" color="text.secondary">
          No rebalancing needed right now
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 0.5 }}>
          All stations have bikes and dock space available
        </Typography>
      </Box>
    );
  }

  return (
    <TableContainer sx={{ maxHeight: 400 }}>
      <Table size="small" stickyHeader>
        <TableHead>
          <TableRow>
            <TableCell>
              <Tooltip title="Nearest station with available bikes — can donate bikes">
                <span>From (has bikes)</span>
              </Tooltip>
            </TableCell>
            <TableCell />
            <TableCell>
              <Tooltip title="Station with no bikes — needs bikes moved in">
                <span>To (empty)</span>
              </Tooltip>
            </TableCell>
            <TableCell align="right">Dist (km)</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {suggestions.map((s, i) => (
            <TableRow key={`${s.sourceStationId}-${s.targetStationId}-${i}`} hover>
              <TableCell>
                <Typography variant="body2" fontWeight={500} noWrap sx={{ maxWidth: 130 }}>
                  {s.sourceName}
                </Typography>
                <Chip
                  label={`${s.sourceBikes} bikes`}
                  size="small"
                  color="error"
                  sx={{ mt: 0.25, height: 18, fontSize: "0.65rem" }}
                />
              </TableCell>
              <TableCell align="center" sx={{ px: 0 }}>
                <ArrowForwardIcon fontSize="small" color="action" />
              </TableCell>
              <TableCell>
                <Typography variant="body2" fontWeight={500} noWrap sx={{ maxWidth: 130 }}>
                  {s.targetName}
                </Typography>
                <Chip
                  label={`${s.targetCapacity} cap`}
                  size="small"
                  color="warning"
                  sx={{ mt: 0.25, height: 18, fontSize: "0.65rem" }}
                />
              </TableCell>
              <TableCell align="right">
                <Typography variant="caption" color="text.secondary">
                  {s.distanceKm.toFixed(1)}
                </Typography>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
