/**
 * OD Flow table — lists origin→destination pairs with station filter + intensity filter.
 */

import {
  Box,
  Chip,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import SwapHorizIcon from "@mui/icons-material/SwapHoriz";
import type { StationODPairDTO } from "@/types";

export type IntensityFilter = "all" | "low" | "medium" | "extreme";

interface ODFlowTableProps {
  odPairs: StationODPairDTO[];
  allPairs: StationODPairDTO[];
  thresholds: { low: number; high: number };
  filterStationId: number | null;
  intensityFilter: IntensityFilter;
  selectedPairKey: string | null;
  onFilterChange: (id: number | null) => void;
  onIntensityFilterChange: (f: IntensityFilter) => void;
  onPairSelect: (key: string | null) => void;
}

export const ODFlowTable = ({
  odPairs,
  allPairs,
  thresholds,
  filterStationId,
  intensityFilter,
  selectedPairKey,
  onFilterChange,
  onIntensityFilterChange,
  onPairSelect,
}: ODFlowTableProps) => {
  // Unique stations for the dropdown filter
  const stationOptions = [
    ...new Map(
      allPairs.flatMap((p) => [
        [p.originStationId, p.originName],
        [p.destStationId, p.destName],
      ]),
    ).entries(),
  ].toSorted((a, b) => a[1].localeCompare(b[1]));

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 1, pt: 0.5 }}>
      {/* Filters */}
      <Box
        sx={{ display: "flex", gap: 1, flexWrap: "wrap", alignItems: "center" }}
      >
        <Select
          size="small"
          displayEmpty
          value={filterStationId === null ? "" : String(filterStationId)}
          onChange={(e) =>
            onFilterChange(
              e.target.value === "" ? null : Number(e.target.value),
            )
          }
          sx={{ fontSize: "0.7rem", minWidth: 140, flex: 1 }}
        >
          <MenuItem value="">All stations</MenuItem>
          {stationOptions.map(([id, name]) => (
            <MenuItem key={id} value={String(id)} sx={{ fontSize: "0.7rem" }}>
              {name}
            </MenuItem>
          ))}
        </Select>

        <ToggleButtonGroup
          value={intensityFilter}
          exclusive
          onChange={(_, v) => v && onIntensityFilterChange(v)}
          size="small"
          sx={{
            "& .MuiToggleButton-root": {
              px: 0.75,
              py: 0.25,
              fontSize: "0.6rem",
            },
          }}
        >
          <ToggleButton value="all">All</ToggleButton>
          <ToggleButton
            value="low"
            sx={{
              color: "#22c55e",
              "&.Mui-selected": { bgcolor: "#22c55e22" },
            }}
          >
            Low
          </ToggleButton>
          <ToggleButton
            value="medium"
            sx={{
              color: "#f97316",
              "&.Mui-selected": { bgcolor: "#f9731622" },
            }}
          >
            Med
          </ToggleButton>
          <ToggleButton
            value="extreme"
            sx={{
              color: "#ef4444",
              "&.Mui-selected": { bgcolor: "#ef444422" },
            }}
          >
            High
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {filterStationId !== null && (
        <Chip
          label={`Filtered: ${stationOptions.find(([id]) => id === filterStationId)?.[1] ?? filterStationId}`}
          size="small"
          onDelete={() => onFilterChange(null)}
          sx={{ fontSize: "0.65rem", alignSelf: "flex-start" }}
        />
      )}

      {odPairs.length === 0 ? (
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ py: 2, textAlign: "center", display: "block" }}
        >
          No OD pairs match the current filters.
        </Typography>
      ) : (
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontSize: "0.65rem", py: 0.5 }}>
                Origin → Destination
              </TableCell>
              <TableCell align="right" sx={{ fontSize: "0.65rem", py: 0.5 }}>
                Trips
              </TableCell>
              <TableCell align="right" sx={{ fontSize: "0.65rem", py: 0.5 }}>
                Dist
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {odPairs.slice(0, 50).map((pair) => {
              const pairKey = `${pair.originStationId}-${pair.destStationId}`;
              const isSelected = pairKey === selectedPairKey;
              const trips = pair.estimatedTrips;
              const dotColor =
                trips >= thresholds.high
                  ? "#ef4444"
                  : trips >= thresholds.low
                    ? "#f97316"
                    : "#22c55e";
              return (
                <TableRow
                  key={pairKey}
                  hover
                  selected={isSelected}
                  onClick={() => onPairSelect(isSelected ? null : pairKey)}
                  sx={{ cursor: "pointer" }}
                >
                  <TableCell sx={{ fontSize: "0.65rem", py: 0.25 }}>
                    <Box
                      sx={{ display: "flex", alignItems: "center", gap: 0.5 }}
                    >
                      <Box
                        sx={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          bgcolor: dotColor,
                          flexShrink: 0,
                        }}
                      />
                      <Typography
                        variant="inherit"
                        noWrap
                        sx={{ maxWidth: 85 }}
                      >
                        {pair.originName}
                      </Typography>
                      <SwapHorizIcon
                        sx={{
                          fontSize: "0.75rem",
                          color: "text.disabled",
                          flexShrink: 0,
                        }}
                      />
                      <Typography
                        variant="inherit"
                        noWrap
                        sx={{ maxWidth: 85 }}
                      >
                        {pair.destName}
                      </Typography>
                    </Box>
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{ fontSize: "0.65rem", py: 0.25 }}
                  >
                    {pair.estimatedTrips}
                  </TableCell>
                  <TableCell
                    align="right"
                    sx={{ fontSize: "0.65rem", py: 0.25 }}
                  >
                    {pair.distanceKm.toFixed(1)} km
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </Box>
  );
};
