/**
 * Compact cycle station statistics chart
 * Designed for overlay panels
 */

import { Box, Typography, Grid } from "@mui/material";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";
import type { CycleStatistics } from "@/types";

interface CycleStatsChartProps {
  statistics: CycleStatistics;
  /** Compact mode for overlay panels */
  compact?: boolean;
}

const COLORS = ["#60a5fa", "#f472b6", "#4ade80", "#fbbf24"];

export const CycleStatsChart = ({
  statistics,
  compact = false,
}: CycleStatsChartProps) => {
  const pieData = [
    { name: "Bikes", value: statistics.totalBikesAvailable || 0 },
    { name: "Docks", value: statistics.totalDocksAvailable || 0 },
  ];

  if (compact) {
    return (
      <Box>
        <Box sx={{ height: 140, mb: 1 }}>
          <ResponsiveContainer>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                innerRadius={35}
                outerRadius={55}
                fill="#8884d8"
                dataKey="value"
                strokeWidth={0}
              >
                {pieData.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  borderRadius: 8,
                  fontSize: 12,
                  border: "none",
                  boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </Box>
        <Box
          sx={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 0.75,
            fontSize: "0.75rem",
          }}
        >
          <StatItem label="Stations" value={statistics.totalStations} />
          <StatItem
            label="Avg Bikes"
            value={statistics.averageBikesAvailable?.toFixed(1) || "0"}
          />
          <StatItem
            label="Avg Docks"
            value={statistics.averageDocksAvailable?.toFixed(1) || "0"}
          />
          <StatItem
            label="Occupancy"
            value={`${statistics.averageOccupancyRate?.toFixed(0) || 0}%`}
          />
        </Box>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Cycle Station Statistics
      </Typography>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Box sx={{ height: 260 }}>
            <ResponsiveContainer>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ name, percent }) =>
                    `${name}: ${((percent ?? 0) * 100).toFixed(0)}%`
                  }
                  innerRadius={40}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="value"
                  strokeWidth={0}
                >
                  {pieData.map((_, index) => (
                    <Cell
                      key={`cell-${index}`}
                      fill={COLORS[index % COLORS.length]}
                    />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    borderRadius: 8,
                    fontSize: 12,
                    border: "none",
                    boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </Box>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: 1.5,
              p: 2,
            }}
          >
            <StatItem label="Total Stations" value={statistics.totalStations} />
            <StatItem
              label="Avg Bikes"
              value={statistics.averageBikesAvailable?.toFixed(1) || "0"}
            />
            <StatItem
              label="Avg Docks"
              value={statistics.averageDocksAvailable?.toFixed(1) || "0"}
            />
            <StatItem
              label="Avg Occupancy"
              value={`${statistics.averageOccupancyRate?.toFixed(1) || 0}%`}
            />
            <StatItem label="Renting" value={statistics.stationsRenting || 0} />
            <StatItem
              label="Returning"
              value={statistics.stationsReturning || 0}
            />
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

const StatItem = ({
  label,
  value,
}: {
  label: string;
  value: string | number;
}) => (
  <Box>
    <Typography
      variant="caption"
      color="text.secondary"
      sx={{ display: "block", lineHeight: 1.2 }}
    >
      {label}
    </Typography>
    <Typography variant="body2" sx={{ fontWeight: 600 }}>
      {value}
    </Typography>
  </Box>
);
