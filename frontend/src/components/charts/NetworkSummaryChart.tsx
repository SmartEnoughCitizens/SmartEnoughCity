/**
 * Compact network summary chart — uses NetworkSummaryDTO from CycleMetricsController
 */

import { Box, Typography } from "@mui/material";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";
import type { NetworkSummaryDTO } from "@/types";

interface NetworkSummaryChartProps {
  summary: NetworkSummaryDTO;
  compact?: boolean;
}

const COLORS = ["#60a5fa", "#f472b6"];

export const NetworkSummaryChart = ({
  summary,
  compact = false,
}: NetworkSummaryChartProps) => {
  const pieData = [
    { name: "Bikes", value: summary.totalBikesAvailable },
    { name: "Docks", value: summary.totalDocksAvailable },
  ];

  if (compact) {
    return (
      <Box>
        <Box sx={{ height: 120, mb: 1 }}>
          <ResponsiveContainer>
            <PieChart>
              <Pie
                data={pieData}
                cx="50%"
                cy="50%"
                innerRadius={30}
                outerRadius={50}
                dataKey="value"
                strokeWidth={0}
              >
                {pieData.map((_, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
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
        <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 0.75 }}>
          <StatItem
            label="Active stations"
            value={`${summary.activeStations}/${summary.totalStations}`}
          />
          <StatItem
            label="Avg bike availability"
            value={`${summary.avgNetworkFullnessPct.toFixed(0)}%`}
          />
          <StatItem
            label="No bikes (empty)"
            value={summary.emptyStations}
            highlight={summary.emptyStations > 0 ? "error" : undefined}
          />
          <StatItem
            label="No space (full)"
            value={summary.fullStations}
            highlight={summary.fullStations > 0 ? "warning" : undefined}
          />
        </Box>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Network Summary
      </Typography>
      <Box sx={{ height: 200, mb: 2 }}>
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
              dataKey="value"
              strokeWidth={0}
            >
              {pieData.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
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
      <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1.5 }}>
        <StatItem label="Total Stations" value={summary.totalStations} />
        <StatItem label="Active Stations" value={summary.activeStations} />
        <StatItem label="Bikes Available" value={summary.totalBikesAvailable} />
        <StatItem label="Docks Available" value={summary.totalDocksAvailable} />
        <StatItem label="No Bikes (empty stations)" value={summary.emptyStations} />
        <StatItem label="No Space (full stations)" value={summary.fullStations} />
        <StatItem
          label="Avg Bike Availability"
          value={`${summary.avgNetworkFullnessPct.toFixed(1)}%`}
        />
        <StatItem label="Rebalancing Need" value={summary.rebalancingNeedCount} />
      </Box>
    </Box>
  );
};

const StatItem = ({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string | number;
  highlight?: "error" | "warning";
}) => (
  <Box>
    <Typography
      variant="caption"
      color="text.secondary"
      sx={{ display: "block", lineHeight: 1.2 }}
    >
      {label}
    </Typography>
    <Typography
      variant="body2"
      sx={{ fontWeight: 600 }}
      color={highlight ? `${highlight}.main` : "text.primary"}
    >
      {value}
    </Typography>
  </Box>
);