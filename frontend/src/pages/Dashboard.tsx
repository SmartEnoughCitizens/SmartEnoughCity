/**
 * City Overview — animated orb background with per-indicator summary cards
 */

import { Box, Paper, Typography } from "@mui/material";
import { useTheme, alpha } from "@mui/material/styles";
import { motion, useReducedMotion } from "framer-motion";
import { useNavigate } from "react-router-dom";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import DirectionsCarIcon from "@mui/icons-material/DirectionsCar";
import TrainIcon from "@mui/icons-material/Train";
import TramIcon from "@mui/icons-material/Tram";
import EvStationIcon from "@mui/icons-material/EvStation";
import EventNoteIcon from "@mui/icons-material/EventNote";
import ReportProblemIcon from "@mui/icons-material/ReportProblem";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import {
  useBusKpis,
  useTramKpis,
  useTrainKpis,
  useCarFuelTypeStatistics,
  useCarHighTrafficPoints,
  useCycleNetworkSummary,
  useEvChargingStations,
  useEvChargingDemand,
  useEvents,
  usePedestriansLive,
  useActiveDisruptions,
} from "@/hooks";
import { SkeletonCard } from "@/components/common/SkeletonCard";
import type { DashboardView } from "@/layouts/DashboardLayout";

const orbs = [
  { size: 500, top: "-15%", left: "-8%", color: "primary", duration: 22 },
  { size: 400, top: "60%", left: "70%", color: "secondary", duration: 28 },
  { size: 600, top: "25%", left: "50%", color: "primary", duration: 18 },
  { size: 300, top: "70%", left: "-5%", color: "secondary", duration: 25 },
  { size: 450, top: "-10%", left: "60%", color: "secondary", duration: 30 },
  { size: 350, top: "40%", left: "15%", color: "primary", duration: 20 },
];

const MetricRow = ({
  label,
  value,
  color,
}: {
  label: string;
  value: string | number;
  color?: string;
}) => (
  <Box
    sx={{
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      mb: 0.75,
    }}
  >
    <Typography variant="caption" color="text.secondary">
      {label}
    </Typography>
    <Typography
      variant="body2"
      fontWeight={600}
      sx={{ color: color ?? "text.primary" }}
    >
      {value}
    </Typography>
  </Box>
);

const CHART_HEIGHT = 100;

const MiniDonut = ({
  data,
  colors,
}: {
  data: { name: string; value: number }[];
  colors: string[];
}) => (
  <Box>
    <ResponsiveContainer width="100%" height={CHART_HEIGHT}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={28}
          outerRadius={42}
          paddingAngle={2}
          dataKey="value"
          strokeWidth={0}
          startAngle={90}
          endAngle={-270}
        >
          {data.map((_, i) => (
            <Cell key={i} fill={colors[i % colors.length]} />
          ))}
        </Pie>
        <Tooltip
          formatter={(v: number | undefined, name: string | undefined) => [
            `${(v ?? 0).toFixed(1)}`,
            name ?? "",
          ]}
          contentStyle={{ fontSize: 11, borderRadius: 6 }}
        />
      </PieChart>
    </ResponsiveContainer>
    <Box
      sx={{
        display: "flex",
        justifyContent: "center",
        flexWrap: "wrap",
        gap: 1.5,
        mt: 0.5,
      }}
    >
      {data
        .filter((d) => d.value > 0)
        .map((d, i) => (
          <Box key={i} sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                flexShrink: 0,
                bgcolor: colors[i % colors.length],
              }}
            />
            <Typography variant="caption" color="text.secondary">
              {d.name}
            </Typography>
          </Box>
        ))}
    </Box>
  </Box>
);

const IndicatorCard = ({
  title,
  icon,
  accent,
  loading,
  onClick,
  children,
}: {
  title: string;
  icon: React.ReactNode;
  accent: string;
  loading: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <Paper
    elevation={0}
    onClick={onClick}
    sx={{
      p: 2.5,
      borderRadius: 3,
      cursor: "pointer",
      border: "1px solid",
      borderColor: "divider",
      width: "100%",
      height: "100%",
      display: "flex",
      flexDirection: "column",
      transition: "transform 0.18s ease, box-shadow 0.18s ease",
      "&:hover": { transform: "translateY(-2px)", boxShadow: 4 },
    }}
  >
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        mb: 2,
        gap: 1,
        flexShrink: 0,
      }}
    >
      <Box sx={{ color: accent, display: "flex" }}>{icon}</Box>
      <Typography variant="h6" fontWeight={600} sx={{ flex: 1 }}>
        {title}
      </Typography>
      <ChevronRightIcon fontSize="small" sx={{ color: "text.disabled" }} />
    </Box>
    <Box sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
      {loading ? <SkeletonCard variant="stat" /> : children}
    </Box>
  </Paper>
);

interface DashboardProps {
  onNavigate: (view: DashboardView) => void;
}

export const Dashboard = ({ onNavigate }: DashboardProps) => {
  const muiTheme = useTheme();
  const shouldReduceMotion = useReducedMotion();
  const navigate = useNavigate();
  const orbOpacity = muiTheme.palette.mode === "dark" ? 0.25 : 0.15;

  const { data: busKpis, isLoading: busLoading } = useBusKpis();
  const { data: tramKpis, isLoading: tramLoading } = useTramKpis();
  const { data: trainKpis, isLoading: trainLoading } = useTrainKpis();
  const { data: fuelStats, isLoading: fuelLoading } =
    useCarFuelTypeStatistics();
  const { data: trafficPoints, isLoading: trafficLoading } =
    useCarHighTrafficPoints();
  const { data: cycleNetwork, isLoading: cycleLoading } =
    useCycleNetworkSummary();
  const { data: evStations, isLoading: evStationsLoading } =
    useEvChargingStations();
  const { data: evDemand, isLoading: evDemandLoading } = useEvChargingDemand();
  const { data: events, isLoading: eventsLoading } = useEvents(10);
  const { data: pedestrians, isLoading: pedestriansLoading } =
    usePedestriansLive(20);
  const { data: disruptions, isLoading: disruptionsLoading } =
    useActiveDisruptions();

  // ── Derived values ────────────────────────────────────────────────────────

  const filteredFuelStats = (fuelStats ?? []).filter(
    (s) => !s.fuelType.toLowerCase().includes("all"),
  );
  const topFuelType =
    filteredFuelStats.toSorted((a, b) => b.count - a.count)[0]?.fuelType ?? "—";
  const monitoringSites = new Set(trafficPoints?.map((p) => p.siteId)).size;
  const criticalCount =
    disruptions?.filter((d) => d.severity === "CRITICAL").length ?? 0;
  const highCount =
    disruptions?.filter((d) => d.severity === "HIGH").length ?? 0;
  const disruptionAccent =
    (disruptions?.length ?? 0) > 0
      ? muiTheme.palette.error.main
      : muiTheme.palette.success.main;

  // ── Chart data ────────────────────────────────────────────────────────────

  const faded = alpha(muiTheme.palette.divider, 0.5);

  const busDonutData = busKpis
    ? [
        { name: "Utilized", value: busKpis.fleetUtilizationPct },
        { name: "Idle", value: Math.max(0, 100 - busKpis.fleetUtilizationPct) },
      ]
    : [];

  const tramDonutData = tramKpis
    ? [
        { name: "Operating", value: tramKpis.linesOperating },
        { name: "Offline", value: Math.max(0, 2 - tramKpis.linesOperating) },
      ]
    : [];

  const trainDonutData = trainKpis
    ? [
        { name: "On Time", value: trainKpis.onTimePct },
        { name: "Delayed", value: Math.max(0, 100 - trainKpis.onTimePct) },
      ]
    : [];

  const topFuelData = filteredFuelStats
    .toSorted((a, b) => b.count - a.count)
    .slice(0, 5);

  const bikeDonutData = cycleNetwork
    ? [
        {
          name: "Available",
          value: Math.max(
            0,
            cycleNetwork.activeStations -
              cycleNetwork.emptyStations -
              cycleNetwork.fullStations,
          ),
        },
        { name: "Empty", value: cycleNetwork.emptyStations },
        { name: "Full", value: cycleNetwork.fullStations },
        {
          name: "Inactive",
          value: Math.max(
            0,
            cycleNetwork.totalStations - cycleNetwork.activeStations,
          ),
        },
      ]
    : [];

  const barColors = [
    muiTheme.palette.primary.main,
    muiTheme.palette.info.main,
    muiTheme.palette.warning.main,
    muiTheme.palette.success.main,
    muiTheme.palette.secondary.main,
  ];

  return (
    <Box sx={{ position: "relative", height: "100%", overflow: "hidden" }}>
      {/* Orb background */}
      {orbs.map((orb, i) => (
        <motion.div
          key={i}
          style={{
            position: "absolute",
            width: orb.size,
            height: orb.size,
            borderRadius: "50%",
            top: orb.top,
            left: orb.left,
            filter: "blur(60px)",
            willChange: "transform",
            backgroundColor:
              orb.color === "primary"
                ? alpha(muiTheme.palette.primary.main, orbOpacity)
                : alpha(muiTheme.palette.secondary.main, orbOpacity),
            zIndex: 0,
          }}
          {...(!shouldReduceMotion && {
            animate: { x: [0, 25, -15, 8, 0], y: [0, -20, 12, -8, 0] },
            transition: {
              duration: orb.duration,
              repeat: Infinity,
              ease: "easeInOut",
            },
          })}
        />
      ))}

      {/* Content layer */}
      <Box
        sx={{
          position: "relative",
          zIndex: 1,
          height: "100%",
          display: "flex",
          flexDirection: "column",
          p: 3,
          overflowY: { xs: "auto", md: "hidden" },
        }}
      >
        {/* Page header */}
        <Box sx={{ mb: 2, flexShrink: 0 }}>
          <Typography variant="h5" fontWeight={700}>
            City Overview
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Live snapshot across all transport indicators
          </Typography>
        </Box>

        {/* Card grid — fills remaining height, 4 columns × 2 rows on desktop */}
        <Box
          sx={{
            flex: 1,
            minHeight: 0,
            display: "grid",
            gridTemplateColumns: { xs: "repeat(2, 1fr)", md: "repeat(4, 1fr)" },
            gridAutoRows: "1fr",
            gap: 2,
            overflow: "hidden",
          }}
        >
          {/* Bus */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Bus"
              icon={<DirectionsBusIcon />}
              accent={muiTheme.palette.primary.main}
              loading={busLoading}
              onClick={() => onNavigate("bus")}
            >
              <MetricRow
                label="Buses Running"
                value={busKpis?.totalBusesRunning ?? "—"}
              />
              <MetricRow
                label="Active Delays"
                value={busKpis?.activeDelays ?? "—"}
                color={
                  (busKpis?.activeDelays ?? 0) > 0
                    ? muiTheme.palette.error.main
                    : muiTheme.palette.success.main
                }
              />
              <MetricRow
                label="Fleet Utilization"
                value={
                  busKpis ? `${busKpis.fleetUtilizationPct.toFixed(1)}%` : "—"
                }
              />
              {busDonutData.length > 0 && (
                <Box sx={{ mt: 1.5 }}>
                  <MiniDonut
                    data={busDonutData}
                    colors={[muiTheme.palette.primary.main, faded]}
                  />
                </Box>
              )}
            </IndicatorCard>
          </Box>

          {/* Luas */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Luas"
              icon={<TramIcon />}
              accent={muiTheme.palette.info.main}
              loading={tramLoading}
              onClick={() => onNavigate("tram")}
            >
              <MetricRow
                label="Lines Operating"
                value={tramKpis?.linesOperating ?? "—"}
              />
              <MetricRow
                label="Avg Due"
                value={tramKpis ? `${tramKpis.avgDueMins.toFixed(1)} min` : "—"}
              />
              <MetricRow
                label="Active Forecasts"
                value={tramKpis?.activeForecastCount ?? "—"}
              />
              {tramDonutData.length > 0 && (
                <Box sx={{ mt: 1.5 }}>
                  <MiniDonut
                    data={tramDonutData}
                    colors={[muiTheme.palette.info.main, faded]}
                  />
                </Box>
              )}
            </IndicatorCard>
          </Box>

          {/* Train / DART */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Train / DART"
              icon={<TrainIcon />}
              accent={muiTheme.palette.secondary.main}
              loading={trainLoading}
              onClick={() => onNavigate("train")}
            >
              <MetricRow
                label="Live Trains"
                value={trainKpis?.liveTrainsRunning ?? "—"}
              />
              <MetricRow
                label="On Time"
                value={trainKpis ? `${trainKpis.onTimePct.toFixed(1)}%` : "—"}
                color={
                  (trainKpis?.onTimePct ?? 0) >= 80
                    ? muiTheme.palette.success.main
                    : muiTheme.palette.warning.main
                }
              />
              <MetricRow
                label="Avg Delay"
                value={
                  trainKpis
                    ? `${trainKpis.avgDelayMinutes.toFixed(1)} min`
                    : "—"
                }
              />
              {trainDonutData.length > 0 && (
                <Box sx={{ mt: 1.5 }}>
                  <MiniDonut
                    data={trainDonutData}
                    colors={[
                      (trainKpis?.onTimePct ?? 0) >= 80
                        ? muiTheme.palette.success.main
                        : muiTheme.palette.warning.main,
                      faded,
                    ]}
                  />
                </Box>
              )}
            </IndicatorCard>
          </Box>

          {/* Car / Traffic */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Car / Traffic"
              icon={<DirectionsCarIcon />}
              accent={muiTheme.palette.warning.main}
              loading={fuelLoading || trafficLoading}
              onClick={() => onNavigate("car")}
            >
              <MetricRow
                label="Monitoring Sites"
                value={monitoringSites > 0 ? monitoringSites : "—"}
              />
              <MetricRow label="Top Fuel Type" value={topFuelType} />
              {topFuelData.length > 0 && (
                <Box sx={{ mt: 1.5 }}>
                  <ResponsiveContainer width="100%" height={CHART_HEIGHT}>
                    <BarChart
                      layout="vertical"
                      data={topFuelData}
                      margin={{ top: 0, right: 8, bottom: 0, left: 64 }}
                    >
                      <XAxis type="number" hide />
                      <YAxis
                        type="category"
                        dataKey="fuelType"
                        tick={{ fontSize: 10 }}
                        width={64}
                      />
                      <Bar dataKey="count" radius={[0, 3, 3, 0]}>
                        {topFuelData.map((_, i) => (
                          <Cell
                            key={i}
                            fill={barColors[i % barColors.length]}
                          />
                        ))}
                      </Bar>
                      <Tooltip
                        formatter={(v: number | undefined) =>
                          (v ?? 0).toLocaleString()
                        }
                        contentStyle={{ fontSize: 11, borderRadius: 6 }}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    sx={{ display: "block", textAlign: "center", mt: 0.5 }}
                  >
                    Vehicles Registered
                  </Typography>
                </Box>
              )}
            </IndicatorCard>
          </Box>

          {/* Dublin Bikes */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Dublin Bikes"
              icon={<DirectionsBikeIcon />}
              accent={muiTheme.palette.success.main}
              loading={cycleLoading}
              onClick={() => onNavigate("cycle")}
            >
              <MetricRow
                label="Active Stations"
                value={cycleNetwork?.activeStations ?? "—"}
              />
              <MetricRow
                label="Bikes Available"
                value={cycleNetwork?.totalBikesAvailable ?? "—"}
              />
              <MetricRow
                label="Network Fullness"
                value={
                  cycleNetwork
                    ? `${cycleNetwork.avgNetworkFullnessPct.toFixed(1)}%`
                    : "—"
                }
              />
              {bikeDonutData.length > 0 && (
                <Box sx={{ mt: 1.5 }}>
                  <MiniDonut
                    data={bikeDonutData}
                    colors={[
                      muiTheme.palette.success.main,
                      muiTheme.palette.error.light,
                      muiTheme.palette.warning.main,
                      faded,
                    ]}
                  />
                </Box>
              )}
            </IndicatorCard>
          </Box>

          {/* EV Charging */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="EV Charging"
              icon={<EvStationIcon />}
              accent={muiTheme.palette.success.dark}
              loading={evStationsLoading || evDemandLoading}
              onClick={() => onNavigate("car")}
            >
              <MetricRow
                label="Total Stations"
                value={evStations?.stations?.length ?? "—"}
              />
              <MetricRow
                label="High Demand Areas"
                value={evDemand?.high_priority_areas?.length ?? "—"}
              />
            </IndicatorCard>
          </Box>

          {/* Events & Pedestrians */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Events & Pedestrians"
              icon={<EventNoteIcon />}
              accent={muiTheme.palette.secondary.light}
              loading={eventsLoading || pedestriansLoading}
              onClick={() => onNavigate("misc")}
            >
              <MetricRow
                label="Upcoming Events"
                value={events?.length ?? "—"}
              />
              <MetricRow
                label="Monitoring Sites"
                value={pedestrians?.length ?? "—"}
              />
            </IndicatorCard>
          </Box>

          {/* Disruptions */}
          <Box sx={{ display: "flex" }}>
            <IndicatorCard
              title="Disruptions"
              icon={<ReportProblemIcon />}
              accent={disruptionAccent}
              loading={disruptionsLoading}
              onClick={() => navigate("/dashboard/disruptions")}
            >
              <MetricRow
                label="Total Active"
                value={disruptions?.length ?? "—"}
              />
              <MetricRow
                label="Critical"
                value={criticalCount}
                color={
                  criticalCount > 0 ? muiTheme.palette.error.main : undefined
                }
              />
              <MetricRow
                label="High"
                value={highCount}
                color={
                  highCount > 0 ? muiTheme.palette.warning.main : undefined
                }
              />
            </IndicatorCard>
          </Box>
        </Box>
      </Box>
    </Box>
  );
};
