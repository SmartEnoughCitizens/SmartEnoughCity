/**
 * Tram delay visualization — bar chart of delays by stop
 */

import { Box, Typography } from "@mui/material";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import type { TramDelay } from "@/types";

interface TramDelayChartProps {
  delays: TramDelay[];
  height?: number;
}

export const TramDelayChart = ({
  delays,
  height = 260,
}: TramDelayChartProps) => {
  // Aggregate delays by stop, pick the worst delay per stop
  const byStop = delays.reduce(
    (acc, d) => {
      const existing = acc[d.stopName];
      if (!existing || d.delayMins > existing.delayMins) {
        acc[d.stopName] = d;
      }
      return acc;
    },
    {} as Record<string, TramDelay>,
  );

  const chartData = Object.values(byStop)
    .sort((a, b) => b.delayMins - a.delayMins)
    .slice(0, 15)
    .map((d) => ({
      name: d.stopName,
      delay: d.delayMins,
      line: d.line,
      affected: d.estimatedAffectedPassengers,
    }));

  if (chartData.length === 0) {
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
          No delays detected
        </Typography>
      </Box>
    );
  }

  return (
    <ResponsiveContainer width="100%" height={height}>
      <BarChart data={chartData} layout="vertical" margin={{ left: 80 }}>
        <CartesianGrid strokeDasharray="3 3" horizontal={false} />
        <XAxis type="number" unit=" min" />
        <YAxis
          type="category"
          dataKey="name"
          width={75}
          tick={{ fontSize: 11 }}
        />
        <Tooltip
          formatter={(value: number, _name: string, props: { payload: { line: string; affected: number } }) => [
            `${value} min (est. ${props.payload.affected} affected)`,
            `${props.payload.line} line delay`,
          ]}
        />
        <Bar dataKey="delay" radius={[0, 4, 4, 0]}>
          {chartData.map((entry, idx) => (
            <Cell
              key={idx}
              fill={entry.line === "red" ? "#ef4444" : "#22c55e"}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
};
