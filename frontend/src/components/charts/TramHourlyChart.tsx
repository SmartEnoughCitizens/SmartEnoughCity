/**
 * Tram hourly passenger distribution chart — line chart per Luas line
 */

import { Box, Typography } from "@mui/material";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import type { TramHourlyDistribution } from "@/types";

interface TramHourlyChartProps {
  data: TramHourlyDistribution[];
  height?: number;
}

export const TramHourlyChart = ({
  data,
  height = 260,
}: TramHourlyChartProps) => {
  if (data.length === 0) {
    return (
      <Box
        sx={{
          height,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <Typography variant="body2" color="text.secondary">
          No hourly distribution data available
        </Typography>
      </Box>
    );
  }

  // Pivot data: { timeLabel, Red: %, Green: % }
  const pivoted: Record<string, { timeLabel: string; Red?: number; Green?: number }> = {};
  data.forEach((d) => {
    if (!pivoted[d.timeLabel]) {
      pivoted[d.timeLabel] = { timeLabel: d.timeLabel };
    }
    const lineKey = d.line.includes("Red") ? "Red" : "Green";
    pivoted[d.timeLabel][lineKey] = d.percentage ?? 0;
  });

  const chartData = Object.values(pivoted).sort((a, b) =>
    a.timeLabel.localeCompare(b.timeLabel),
  );

  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={chartData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="timeLabel"
          tick={{ fontSize: 10 }}
          interval="preserveStartEnd"
        />
        <YAxis unit="%" tick={{ fontSize: 11 }} />
        <Tooltip formatter={(value: number | undefined) => [`${(value ?? 0).toFixed(1)}%`]} />
        <Legend />
        <Line
          type="monotone"
          dataKey="Red"
          stroke="#ef4444"
          strokeWidth={2}
          dot={{ r: 3 }}
          name="Red Line"
        />
        <Line
          type="monotone"
          dataKey="Green"
          stroke="#22c55e"
          strokeWidth={2}
          dot={{ r: 3 }}
          name="Green Line"
        />
      </LineChart>
    </ResponsiveContainer>
  );
};
