/**
 * Demand analysis content — network usage by hour + station classification
 * + per-station heatmap + peak hours table.
 * Layout (open/close, Paper, Chip) is owned by CycleDashboard.
 */

import { useMemo, useState } from "react";
import {
  Box,
  CircularProgress,
  Tab,
  Tabs,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from "@mui/material";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  useCycleNetworkHourlyProfile,
  useCycleStationClassification,
  useCycleStationHourlyUsage,
} from "@/hooks";
import type { StationClassification, StationHourlyUsageDTO } from "@/types";

// ── Classification meta ───────────────────────────────────────────────────────

const CLASS_META: Record<StationClassification, { label: string; color: string; hours: string }> = {
  MORNING_PEAK:   { label: "Morning Peak",   color: "#f59e0b", hours: "07–09" },
  AFTERNOON_PEAK: { label: "Afternoon Peak", color: "#3b82f6", hours: "12–14" },
  EVENING_PEAK:   { label: "Evening Peak",   color: "#f97316", hours: "17–19" },
  OFF_PEAK:       { label: "Off-Peak",       color: "#6b7280", hours: "Other" },
};

const CLASS_ORDER: StationClassification[] = [
  "MORNING_PEAK",
  "AFTERNOON_PEAK",
  "EVENING_PEAK",
  "OFF_PEAK",
];

// ── Heatmap helpers ───────────────────────────────────────────────────────────

/** Interpolate between two hex colours based on 0–1 ratio */
function lerpColor(hex1: string, hex2: string, t: number): string {
  const r1 = Number.parseInt(hex1.slice(1, 3), 16);
  const g1 = Number.parseInt(hex1.slice(3, 5), 16);
  const b1 = Number.parseInt(hex1.slice(5, 7), 16);
  const r2 = Number.parseInt(hex2.slice(1, 3), 16);
  const g2 = Number.parseInt(hex2.slice(3, 5), 16);
  const b2 = Number.parseInt(hex2.slice(5, 7), 16);
  const r = Math.round(r1 + (r2 - r1) * t);
  const g = Math.round(g1 + (g2 - g1) * t);
  const b = Math.round(b1 + (b2 - b1) * t);
  return `rgb(${r},${g},${b})`;
}

function heatColor(value: number, max: number): string {
  if (max === 0) return "#1e3a5f";
  const t = Math.min(value / max, 1);
  if (t < 0.5) return lerpColor("#1e3a5f", "#f59e0b", t * 2);
  return lerpColor("#f59e0b", "#ef4444", (t - 0.5) * 2);
}

// ── Heatmap grid component ────────────────────────────────────────────────────

const StationHeatmap = ({ rows }: { rows: StationHourlyUsageDTO[] }) => {
  const stations = useMemo(() => {
    const map = new Map<number, { name: string; byHour: Record<number, number> }>();
    for (const r of rows) {
      if (!map.has(r.stationId)) map.set(r.stationId, { name: r.name, byHour: {} });
      map.get(r.stationId)!.byHour[r.hourOfDay] = r.avgUsageRate;
    }
    return [...map.entries()].map(([id, v]) => ({ id, ...v }));
  }, [rows]);

  const globalMax = useMemo(
    () => Math.max(...rows.map((r) => r.avgUsageRate), 1),
    [rows],
  );

  if (stations.length === 0) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
        <Typography variant="caption" color="text.secondary">No data available</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ height: "100%", overflow: "auto" }}>
      {/* Hour axis header */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "110px repeat(24, 1fr)",
          gap: "1px",
          mb: "1px",
          position: "sticky",
          top: 0,
          bgcolor: "background.paper",
          zIndex: 1,
        }}
      >
        <Box />
        {Array.from({ length: 24 }, (_, h) => (
          <Typography
            key={h}
            variant="caption"
            sx={{ fontSize: "0.55rem", textAlign: "center", color: "text.secondary" }}
          >
            {h}h
          </Typography>
        ))}
      </Box>

      {/* Station rows */}
      {stations.map((st) => (
        <Box
          key={st.id}
          sx={{
            display: "grid",
            gridTemplateColumns: "110px repeat(24, 1fr)",
            gap: "1px",
            mb: "1px",
          }}
        >
          <Typography
            variant="caption"
            sx={{
              fontSize: "0.58rem",
              color: "text.secondary",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              pr: 0.5,
              lineHeight: "14px",
            }}
            title={st.name}
          >
            {st.name}
          </Typography>
          {Array.from({ length: 24 }, (_, h) => {
            const val = st.byHour[h] ?? 0;
            return (
              <Box
                key={h}
                title={`${st.name} ${h}:00 — ${val.toFixed(1)}%`}
                sx={{
                  height: 14,
                  borderRadius: "2px",
                  bgcolor: heatColor(val, globalMax),
                  opacity: val === 0 ? 0.15 : 1,
                }}
              />
            );
          })}
        </Box>
      ))}

      {/* Legend */}
      <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, mt: 1, px: 0.5 }}>
        <Typography variant="caption" sx={{ fontSize: "0.55rem", color: "text.secondary" }}>Low</Typography>
        <Box
          sx={{
            flex: 1,
            height: 6,
            borderRadius: 1,
            background: "linear-gradient(to right, #1e3a5f, #f59e0b, #ef4444)",
          }}
        />
        <Typography variant="caption" sx={{ fontSize: "0.55rem", color: "text.secondary" }}>High</Typography>
      </Box>
    </Box>
  );
};

// ── Peak Hours table component ────────────────────────────────────────────────

function classifyHour(h: number): StationClassification {
  if (h >= 7 && h <= 9) return "MORNING_PEAK";
  if (h >= 12 && h <= 14) return "AFTERNOON_PEAK";
  if (h >= 17 && h <= 19) return "EVENING_PEAK";
  return "OFF_PEAK";
}

const PeakHoursTable = ({ rows }: { rows: StationHourlyUsageDTO[] }) => {
  // Build per-station peak hour from the hourly data
  const peakByStation = useMemo(() => {
    const map = new Map<number, { name: string; peakHour: number; peakUsage: number }>();
    for (const r of rows) {
      const existing = map.get(r.stationId);
      if (!existing || r.avgUsageRate > existing.peakUsage) {
        map.set(r.stationId, { name: r.name, peakHour: r.hourOfDay, peakUsage: r.avgUsageRate });
      }
    }
    return [...map.values()].toSorted((a, b) => b.peakUsage - a.peakUsage);
  }, [rows]);

  if (peakByStation.length === 0) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100%" }}>
        <Typography variant="caption" color="text.secondary">No data available</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ height: "100%", overflow: "auto" }}>
      {/* Header */}
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: "1fr 60px 60px 80px",
          px: 1,
          py: 0.5,
          position: "sticky",
          top: 0,
          bgcolor: "background.paper",
          borderBottom: "1px solid",
          borderColor: "divider",
        }}
      >
        {["Station", "Peak Hr", "Usage", "Type"].map((h) => (
          <Typography key={h} variant="caption" sx={{ fontSize: "0.6rem", color: "text.secondary", fontWeight: 700 }}>
            {h}
          </Typography>
        ))}
      </Box>

      {peakByStation.map((st) => {
        const cls = classifyHour(st.peakHour);
        const meta = CLASS_META[cls];
        return (
          <Box
            key={st.name}
            sx={{
              display: "grid",
              gridTemplateColumns: "1fr 60px 60px 80px",
              px: 1,
              py: 0.4,
              borderBottom: "1px solid",
              borderColor: "divider",
              "&:hover": { bgcolor: "action.hover" },
            }}
          >
            <Typography
              variant="caption"
              sx={{ fontSize: "0.62rem", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}
              title={st.name}
            >
              {st.name}
            </Typography>
            <Typography variant="caption" sx={{ fontSize: "0.62rem" }}>
              {st.peakHour}:00
            </Typography>
            <Typography variant="caption" sx={{ fontSize: "0.62rem" }}>
              {st.peakUsage.toFixed(1)}%
            </Typography>
            <Box
              sx={{
                display: "inline-flex",
                alignItems: "center",
                gap: 0.3,
              }}
            >
              <Box
                sx={{
                  width: 6,
                  height: 6,
                  borderRadius: "50%",
                  bgcolor: meta.color,
                  flexShrink: 0,
                }}
              />
              <Typography variant="caption" sx={{ fontSize: "0.58rem", color: meta.color }}>
                {meta.hours}
              </Typography>
            </Box>
          </Box>
        );
      })}
    </Box>
  );
};

// ── Main panel ────────────────────────────────────────────────────────────────

export const StationDemandPanel = () => {
  const [days, setDays] = useState(30);
  const [innerTab, setInnerTab] = useState(0);

  const { data: hourlyProfile, isLoading: profileLoading } = useCycleNetworkHourlyProfile(days);
  const { data: classification, isLoading: classLoading } = useCycleStationClassification(days);
  const { data: stationHourly, isLoading: hourlyLoading } = useCycleStationHourlyUsage(days, 30);

  const isLoading = profileLoading || classLoading || hourlyLoading;

  const chartData = useMemo(
    () =>
      Array.from({ length: 24 }, (_, h) => {
        const found = hourlyProfile?.find((p) => p.hourOfDay === h);
        return {
          hourOfDay: h,
          avgUsageRate: found ? Math.round(found.avgUsageRate * 10) / 10 : 0,
        };
      }),
    [hourlyProfile],
  );

  const classCounts = useMemo(() => {
    const counts: Record<StationClassification, number> = {
      MORNING_PEAK: 0, AFTERNOON_PEAK: 0, EVENING_PEAK: 0, OFF_PEAK: 0,
    };
    for (const s of classification ?? []) { counts[s.classification]++; }
    return counts;
  }, [classification]);

  const totalClassified = classification?.length ?? 0;
  const currentHour = new Date().getHours();

  return (
    <Box sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      {/* Top bar: inner tabs + days selector */}
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          px: 2,
          pt: 0.5,
          flexShrink: 0,
        }}
      >
        <Tabs
          value={innerTab}
          onChange={(_, v) => setInnerTab(v)}
          sx={{
            minHeight: 28,
            "& .MuiTab-root": { minHeight: 28, fontSize: "0.65rem", textTransform: "none", py: 0.25 },
          }}
        >
          <Tab label="Overview" />
          <Tab label="Heatmap" />
          <Tab label="Peak Hours" />
        </Tabs>

        <ToggleButtonGroup
          value={days}
          exclusive
          onChange={(_, v) => v !== null && setDays(v)}
          size="small"
          sx={{ "& .MuiToggleButton-root": { px: 1, py: 0.25, fontSize: "0.65rem" } }}
        >
          <ToggleButton value={7}>7d</ToggleButton>
          <ToggleButton value={30}>30d</ToggleButton>
          <ToggleButton value={90}>90d</ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {isLoading ? (
        <Box sx={{ flex: 1, display: "flex", justifyContent: "center", alignItems: "center" }}>
          <CircularProgress size={24} />
        </Box>
      ) : (
        <Box sx={{ flex: 1, overflow: "hidden", px: 2, pb: 1.5 }}>

          {/* ── Overview: network chart + classification ── */}
          {innerTab === 0 && (
            <Box
              sx={{
                height: "100%",
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 2,
              }}
            >
              {/* Left: hourly bar chart */}
              <Box sx={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ mb: 0.5 }}>
                  NETWORK USAGE BY HOUR
                </Typography>
                <Box sx={{ flex: 1, minHeight: 0 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={chartData} margin={{ top: 4, right: 4, bottom: 0, left: -24 }} barCategoryGap="10%">
                      <CartesianGrid strokeDasharray="3 3" opacity={0.2} vertical={false} />
                      <XAxis
                        dataKey="hourOfDay"
                        tickFormatter={(h) => `${h}h`}
                        tick={{ fontSize: 8 }}
                        interval={2}
                        axisLine={false}
                        tickLine={false}
                      />
                      <YAxis
                        domain={[0, 100]}
                        tick={{ fontSize: 8 }}
                        tickFormatter={(v) => `${v}%`}
                        axisLine={false}
                        tickLine={false}
                        ticks={[0, 25, 50, 75, 100]}
                      />
                      <Tooltip
                        formatter={(value: number) => [`${value.toFixed(1)}%`, "Avg Usage"]}
                        labelFormatter={(h) => `${h}:00–${h}:59`}
                        contentStyle={{ fontSize: "0.72rem" }}
                      />
                      <Bar dataKey="avgUsageRate" radius={[3, 3, 0, 0]}>
                        {chartData.map((entry) => (
                          <Cell
                            key={entry.hourOfDay}
                            fill={entry.hourOfDay === currentHour ? "#f59e0b" : "#3b82f6"}
                            opacity={entry.hourOfDay === currentHour ? 1 : 0.7}
                          />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </Box>
              </Box>

              {/* Right: classification breakdown */}
              <Box sx={{ display: "flex", flexDirection: "column", minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ mb: 0.5 }}>
                  STATION CLASSIFICATION
                </Typography>
                <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 1, flex: 1 }}>
                  {CLASS_ORDER.map((cls) => {
                    const meta = CLASS_META[cls];
                    const count = classCounts[cls];
                    const pct = totalClassified > 0 ? Math.round((count / totalClassified) * 100) : 0;
                    return (
                      <Box
                        key={cls}
                        sx={{
                          p: 1,
                          borderRadius: 2,
                          border: "1px solid",
                          borderColor: `${meta.color}40`,
                          bgcolor: `${meta.color}10`,
                          display: "flex",
                          flexDirection: "column",
                          justifyContent: "center",
                        }}
                      >
                        <Typography variant="caption" color="text.secondary" sx={{ fontSize: "0.6rem" }}>
                          {meta.hours}
                        </Typography>
                        <Typography variant="body2" fontWeight={700} sx={{ color: meta.color, lineHeight: 1.2 }}>
                          {count}
                          <Typography component="span" sx={{ fontSize: "0.6rem", color: "text.secondary", ml: 0.5 }}>
                            ({pct}%)
                          </Typography>
                        </Typography>
                        <Typography variant="caption" sx={{ fontSize: "0.6rem", color: "text.secondary" }}>
                          {meta.label}
                        </Typography>
                      </Box>
                    );
                  })}
                </Box>
              </Box>
            </Box>
          )}

          {/* ── Heatmap ── */}
          {innerTab === 1 && (
            <Box sx={{ height: "100%" }}>
              <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: "block", mb: 0.5 }}>
                STATION USAGE HEATMAP (TOP 30 BUSIEST)
              </Typography>
              <Box sx={{ height: "calc(100% - 20px)" }}>
                <StationHeatmap rows={stationHourly ?? []} />
              </Box>
            </Box>
          )}

          {/* ── Peak Hours ── */}
          {innerTab === 2 && (
            <Box sx={{ height: "100%" }}>
              <Typography variant="caption" color="text.secondary" fontWeight={600} sx={{ display: "block", mb: 0.5 }}>
                PEAK HOUR PER STATION
              </Typography>
              <Box sx={{ height: "calc(100% - 20px)" }}>
                <PeakHoursTable rows={stationHourly ?? []} />
              </Box>
            </Box>
          )}

        </Box>
      )}
    </Box>
  );
};
