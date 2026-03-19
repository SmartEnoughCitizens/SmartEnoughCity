/**
 * Origin-Destination flow table with station filter
 */

import {
  Box,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
  Autocomplete,
  TextField,
  IconButton,
  Tooltip,
  ToggleButtonGroup,
  ToggleButton,
} from "@mui/material";
import ClearIcon from "@mui/icons-material/Clear";
import type { StationODPairDTO } from "@/types";

export type IntensityFilter = "extreme" | "medium" | "low" | "all";

interface ODFlowTableProps {
  odPairs: StationODPairDTO[];
  allPairs: StationODPairDTO[];
  filterStationId: number | null;
  intensityFilter: IntensityFilter;
  selectedPairKey: string | null;
  onFilterChange: (stationId: number | null) => void;
  onIntensityFilterChange: (level: IntensityFilter) => void;
  onPairSelect: (key: string | null) => void;
}

interface StationOption {
  id: number;
  label: string;
}

export const ODFlowTable = ({
  odPairs,
  allPairs,
  filterStationId,
  intensityFilter,
  selectedPairKey,
  onFilterChange,
  onIntensityFilterChange,
  onPairSelect,
}: ODFlowTableProps) => {
  // Build unique station list from all pairs (unfiltered) for the autocomplete
  const stationMap = new Map<number, string>();
  for (const pair of allPairs) {
    stationMap.set(pair.originStationId, pair.originName);
    stationMap.set(pair.destStationId, pair.destName);
  }
  const stationOptions: StationOption[] = [...stationMap.entries()]
    .map(([id, label]) => ({ id, label }))
    .toSorted((a, b) => a.label.localeCompare(b.label));

  const visiblePairs = filterStationId
    ? odPairs.filter(
        (p) =>
          p.originStationId === filterStationId ||
          p.destStationId === filterStationId,
      )
    : odPairs;

  const maxTrips = Math.max(...allPairs.map((p) => p.estimatedTrips), 1);

  const selectedOption =
    stationOptions.find((o) => o.id === filterStationId) ?? null;

  return (
    <Box>
      {/* Intensity filter */}
      <Box sx={{ px: 1, pt: 1, pb: 0.5 }}>
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ display: "block", mb: 0.5 }}
        >
          Show flows with intensity ≥
        </Typography>
        <ToggleButtonGroup
          value={intensityFilter}
          exclusive
          onChange={(_, val) => {
            if (val) onIntensityFilterChange(val as IntensityFilter);
          }}
          size="small"
          fullWidth
          sx={{
            "& .MuiToggleButton-root": {
              fontSize: "0.65rem",
              py: 0.4,
              textTransform: "none",
            },
          }}
        >
          <ToggleButton
            value="extreme"
            sx={{
              "&.Mui-selected": {
                bgcolor: "#dc2626",
                color: "#fff",
                "&:hover": { bgcolor: "#b91c1c" },
              },
            }}
          >
            Extreme
          </ToggleButton>
          <ToggleButton
            value="medium"
            sx={{
              "&.Mui-selected": {
                bgcolor: "#f97316",
                color: "#fff",
                "&:hover": { bgcolor: "#ea6c0a" },
              },
            }}
          >
            Medium
          </ToggleButton>
          <ToggleButton
            value="low"
            sx={{
              "&.Mui-selected": {
                bgcolor: "#4ade80",
                color: "#000",
                "&:hover": { bgcolor: "#22c55e" },
              },
            }}
          >
            Low
          </ToggleButton>
          <ToggleButton value="all">All</ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {/* Station filter */}
      <Box
        sx={{ px: 1, pb: 0.5, display: "flex", gap: 0.5, alignItems: "center" }}
      >
        <Autocomplete
          size="small"
          options={stationOptions}
          value={selectedOption}
          onChange={(_, opt) => onFilterChange(opt?.id ?? null)}
          isOptionEqualToValue={(a, b) => a.id === b.id}
          renderInput={(params) => (
            <TextField
              {...params}
              label="Filter by station"
              variant="outlined"
            />
          )}
          sx={{ flex: 1 }}
          clearOnEscape
        />
        {filterStationId && (
          <Tooltip title="Clear filter">
            <IconButton size="small" onClick={() => onFilterChange(null)}>
              <ClearIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      {/* Summary */}
      <Box sx={{ px: 1, pb: 0.5 }}>
        <Typography variant="caption" color="text.secondary">
          {filterStationId
            ? `${visiblePairs.length} flows involving this station`
            : `${odPairs.length} flows shown (last 30 days)`}
        </Typography>
      </Box>

      {/* Table */}
      <TableContainer sx={{ maxHeight: 420 }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell>Origin</TableCell>
              <TableCell>Destination</TableCell>
              <TableCell align="right">Trips</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {visiblePairs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center">
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    sx={{ py: 2 }}
                  >
                    No flows found
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              visiblePairs.map((pair, idx) => {
                const key = `${pair.originStationId}-${pair.destStationId}`;
                const intensity = pair.estimatedTrips / maxTrips;
                const pct = Math.round(intensity * 100);
                const chipColor =
                  intensity >= 0.66
                    ? "#dc2626"
                    : intensity >= 0.33
                      ? "#f97316"
                      : "#4ade80";
                const isSelected = selectedPairKey === key;
                const isOrigin = pair.originStationId === filterStationId;
                const isDest = pair.destStationId === filterStationId;
                return (
                  <TableRow
                    key={`${key}-${idx}`}
                    hover
                    onClick={() => onPairSelect(isSelected ? null : key)}
                    sx={{
                      cursor: "pointer",
                      ...(isSelected
                        ? {
                            bgcolor: "primary.main",
                            "& td": { color: "#fff" },
                            "&:hover": { bgcolor: "primary.dark" },
                          }
                        : filterStationId && (isOrigin || isDest)
                          ? { bgcolor: "action.selected" }
                          : undefined),
                    }}
                  >
                    <TableCell>
                      <Typography
                        variant="body2"
                        fontWeight={isOrigin || isSelected ? 700 : 400}
                      >
                        {pair.originName}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography
                        variant="body2"
                        fontWeight={isDest || isSelected ? 700 : 400}
                      >
                        {pair.destName}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "flex-end",
                          gap: 0.5,
                        }}
                      >
                        <Box
                          sx={{
                            width: `${Math.max(pct * 0.4, 4)}px`,
                            height: 6,
                            borderRadius: 1,
                            bgcolor: chipColor,
                            opacity: 0.7,
                          }}
                        />
                        <Chip
                          label={pair.estimatedTrips}
                          size="small"
                          variant="outlined"
                          sx={{
                            borderColor: chipColor,
                            color: chipColor,
                            fontWeight: 600,
                          }}
                        />
                      </Box>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
};
