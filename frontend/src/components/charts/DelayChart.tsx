/**
 * Compact delay statistics chart component
 * Designed to work inside floating overlay panels
 */

import { Box, Typography } from "@mui/material";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import type { DelayStatistics } from "@/types";

interface DelayChartProps {
  statistics: DelayStatistics;
  /** Compact mode reduces height and hides labels */
  compact?: boolean;
}

export const DelayChart = ({
  statistics,
  compact = false,
}: DelayChartProps) => {
  const data = [
    {
      name: "Avg",
      arrival: statistics.averageArrivalDelay || 0,
      departure: statistics.averageDepartureDelay || 0,
    },
    {
      name: "Min",
      arrival: statistics.minArrivalDelay || 0,
      departure: statistics.minDepartureDelay || 0,
    },
    {
      name: "Max",
      arrival: statistics.maxArrivalDelay || 0,
      departure: statistics.maxDepartureDelay || 0,
    },
  ];

  const chartHeight = compact ? 180 : 260;

  return (
    <Box>
      {!compact && (
        <>
          <Typography variant="h6" gutterBottom>
            Delay Statistics - Route {statistics.routeId}
          </Typography>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Total Trips: {statistics.totalTrips}
          </Typography>
        </>
      )}
      <Box sx={{ width: "100%", height: chartHeight }}>
        <ResponsiveContainer>
          <BarChart
            data={data}
            margin={{ top: 5, right: 5, left: 0, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" opacity={0.15} />
            <XAxis dataKey="name" tick={{ fontSize: 11 }} />
            <YAxis
              tick={{ fontSize: 11 }}
              label={
                compact
                  ? undefined
                  : {
                      value: "Delay (s)",
                      angle: -90,
                      position: "insideLeft",
                      style: { fontSize: 11 },
                    }
              }
            />
            <Tooltip
              contentStyle={{
                borderRadius: 8,
                fontSize: 12,
                border: "none",
                boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
              }}
            />
            {!compact && <Legend wrapperStyle={{ fontSize: 12 }} />}
            <Bar
              dataKey="arrival"
              fill="#60a5fa"
              name="Arrival"
              radius={[4, 4, 0, 0]}
            />
            <Bar
              dataKey="departure"
              fill="#f472b6"
              name="Departure"
              radius={[4, 4, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </Box>
    </Box>
  );
};
