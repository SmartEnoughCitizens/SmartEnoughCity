/**
 * React Query hooks for dashboard data
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { dashboardApi } from "@/api";

export const MISC_KEYS = {
  events: (limit?: number) => ["misc", "events", { limit }] as const,
  pedestriansLive: (limit?: number) =>
    ["misc", "pedestrians", "live", { limit }] as const,
  disruptionsActive: ["misc", "disruptions", "active"] as const,
};

export const DASHBOARD_KEYS = {
  bus: (routeId?: string, limit?: number) =>
    ["dashboard", "bus", { routeId, limit }] as const,
  cycle: (limit?: number) => ["dashboard", "cycle", { limit }] as const,
  train: (limit?: number) => ["dashboard", "train", { limit }] as const,
  tram: (limit?: number) => ["dashboard", "tram", { limit }] as const,
  availableBikes: ["dashboard", "cycle", "available-bikes"] as const,
  availableDocks: ["dashboard", "cycle", "available-docks"] as const,
  busRoutes: ["dashboard", "bus", "routes"] as const,
  indicatorTypes: ["dashboard", "indicator-types"] as const,
  cycleStationsLive: ["cycle", "stations", "live"] as const,
  cycleNetworkSummary: ["cycle", "network", "summary"] as const,
  cycleBusiestStations: (limit?: number) =>
    ["cycle", "rankings", "busiest", { limit }] as const,
  cycleUnderusedStations: (limit?: number) =>
    ["cycle", "rankings", "underused", { limit }] as const,
  cycleRebalancing: (limit?: number) =>
    ["cycle", "network", "rebalancing", { limit }] as const,
  cycleNetworkHourlyProfile: (days?: number) =>
    ["cycle", "demand", "network-hourly", { days }] as const,
  cycleStationClassification: (days?: number) =>
    ["cycle", "demand", "classification", { days }] as const,
  cycleODPairs: (days?: number, limit?: number) =>
    ["cycle", "demand", "od-pairs", { days, limit }] as const,
  cycleStationHourlyUsage: (days?: number, limit?: number) =>
    ["cycle", "demand", "station-hourly", { days, limit }] as const,
  cycleRiskScores: ["cycle", "risk-scores"] as const,
  cycleCoverageGaps: ["cycle", "coverage-gaps"] as const,
  busKpis: ["bus", "kpis"] as const,
  busLiveVehicles: ["bus", "live-vehicles"] as const,
  busRouteUtilization: ["bus", "route-utilization"] as const,
  busSystemPerformance: ["bus", "system-performance"] as const,
  carFuelTypeStatistics: ["car", "fuel-type-statistics"] as const,
  carHighTrafficPoints: ["car", "high-traffic-points"] as const,
  carJunctionEmissions: ["car", "junction-emissions"] as const,
  trainKpis: ["train", "kpis"] as const,
  trainLiveTrains: ["train", "live-trains"] as const,
  trainServiceStats: ["train", "service-stats"] as const,
  trainFrequentDelays: ["train", "frequent-delays"] as const,
  tramKpis: ["tram", "kpis"] as const,
  tramLiveForecasts: ["tram", "live-forecasts"] as const,
  tramDelays: ["tram", "delays"] as const,
  tramHourlyDistribution: ["tram", "hourly-distribution"] as const,
};

/**
 * Get bus data with optional route filter
 */
export const useBusData = (routeId?: string, limit: number = 100) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.bus(routeId, limit),
    queryFn: () => dashboardApi.getBusData({ routeId, limit }),
    staleTime: 30_000, // 30 seconds
  });
};

/**
 * Get cycle station data
 */
export const useCycleData = (limit: number = 100) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycle(limit),
    queryFn: () => dashboardApi.getCycleData({ limit }),
    staleTime: 30_000, // 30 seconds
  });
};

/**
 * Get train station data
 */
export const useTrainData = (limit: number = 200) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.train(limit),
    queryFn: () => dashboardApi.getTrainData({ limit }),
    staleTime: 30_000, // 30 seconds
  });
};

/**
 * Get tram stop data
 */
export const useTramData = (limit: number = 200) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.tram(limit),
    queryFn: () => dashboardApi.getTramData({ limit }),
    staleTime: 30_000,
  });
};

/**
 * Get cycle stations with available bikes
 */
export const useAvailableBikes = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.availableBikes,
    queryFn: () => dashboardApi.getAvailableBikes(),
    staleTime: 30_000,
  });
};

/**
 * Get cycle stations with available docks
 */
export const useAvailableDocks = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.availableDocks,
    queryFn: () => dashboardApi.getAvailableDocks(),
    staleTime: 30_000,
  });
};

/**
 * Get all bus routes
 */
export const useBusRoutes = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.busRoutes,
    queryFn: () => dashboardApi.getBusRoutes(),
    staleTime: 300_000, // 5 minutes - routes don't change often
  });
};

/**
 * Get available indicator types
 */
export const useIndicatorTypes = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.indicatorTypes,
    queryFn: () => dashboardApi.getIndicatorTypes(),
    staleTime: 300_000, // 5 minutes
  });
};

/**
 * Get bus dashboard KPIs
 */
export const useBusKpis = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.busKpis,
    queryFn: () => dashboardApi.getBusKpis(),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get live bus vehicle positions
 */
export const useBusLiveVehicles = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.busLiveVehicles,
    queryFn: () => dashboardApi.getBusLiveVehicles(),
    staleTime: 10_000,
    refetchInterval: 10_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get route utilization data
 */
export const useBusRouteUtilization = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.busRouteUtilization,
    queryFn: () => dashboardApi.getBusRouteUtilization(),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get system performance metrics
 */
export const useBusSystemPerformance = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.busSystemPerformance,
    queryFn: () => dashboardApi.getBusSystemPerformance(),
    staleTime: 60_000,
    refetchInterval: 60_000,
    refetchIntervalInBackground: true,
  });
};
/**
 * Get car fuel type statistics
 */
export const useCarFuelTypeStatistics = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.carFuelTypeStatistics,
    queryFn: () => dashboardApi.getCarFuelTypeStatistics(),
    staleTime: 300_000,
  });
};

/**
 * Get high traffic points with location and time slot data
 */
export const useCarHighTrafficPoints = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.carHighTrafficPoints,
    queryFn: () => dashboardApi.getCarHighTrafficPoints(),
    staleTime: 300_000,
  });
};

/**
 * Get junction-level CO2 emission estimates
 */
export const useCarJunctionEmissions = (enabled = true) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.carJunctionEmissions,
    queryFn: () => dashboardApi.getCarJunctionEmissions(),
    staleTime: 300_000,
    enabled,
  });
};

/**
 * Get train dashboard KPIs
 */
export const useTrainKpis = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.trainKpis,
    queryFn: () => dashboardApi.getTrainKpis(),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get live train positions
 */
export const useTrainLiveTrains = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.trainLiveTrains,
    queryFn: () => dashboardApi.getTrainLiveTrains(),
    staleTime: 10_000,
    refetchInterval: 10_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get train service reliability stats
 */
export const useTrainServiceStats = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.trainServiceStats,
    queryFn: () => dashboardApi.getTrainServiceStats(),
    staleTime: 60_000,
    refetchInterval: 60_000,
    refetchIntervalInBackground: true,
  });
};

// ── Cycle Metrics hooks ──────────────────────────────────────────────────────

export const useCycleStationsLive = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleStationsLive,
    queryFn: () => dashboardApi.getCycleStationsLive(),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
};

export const useCycleNetworkSummary = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleNetworkSummary,
    queryFn: () => dashboardApi.getCycleNetworkSummary(),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
};

export const useCycleBusiestStations = (limit = 10) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleBusiestStations(limit),
    queryFn: () => dashboardApi.getCycleBusiestStations({ limit }),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
};

export const useCycleUnderusedStations = (limit = 10) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleUnderusedStations(limit),
    queryFn: () => dashboardApi.getCycleUnderusedStations({ limit }),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
};

export const useCycleRebalancing = (limit = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleRebalancing(limit),
    queryFn: () => dashboardApi.getCycleRebalancingSuggestions({ limit }),
    staleTime: 60_000,
    refetchInterval: 60_000,
  });
};

export const useCycleNetworkHourlyProfile = (days = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleNetworkHourlyProfile(days),
    queryFn: () => dashboardApi.getCycleNetworkHourlyProfile({ days }),
    staleTime: 300_000,
    refetchInterval: 300_000,
  });
};

export const useCycleStationClassification = (days = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleStationClassification(days),
    queryFn: () => dashboardApi.getCycleStationClassification({ days }),
    staleTime: 300_000,
    refetchInterval: 300_000,
  });
};

export const useCycleODPairs = (days = 30, limit = 50) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleODPairs(days, limit),
    queryFn: () => dashboardApi.getCycleODPairs({ days, limit }),
    staleTime: 300_000,
    refetchInterval: 300_000,
  });
};

export const useCycleStationHourlyUsage = (days = 30, limit = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleStationHourlyUsage(days, limit),
    queryFn: () => dashboardApi.getCycleStationHourlyUsage({ days, limit }),
    staleTime: 300_000,
    refetchInterval: 300_000,
  });
};

export const useCycleRiskScores = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleRiskScores,
    queryFn: () => dashboardApi.getCycleRiskScores(),
    staleTime: 300_000,
    refetchInterval: 300_000,
  });
};

/**
 * Get frequently delayed trains
 */
export const useTrainFrequentDelays = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.trainFrequentDelays,
    queryFn: () => dashboardApi.getTrainFrequentDelays(),
    staleTime: 60_000,
    refetchInterval: 60_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get tram dashboard KPIs
 */
export const useTramKpis = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.tramKpis,
    queryFn: () => dashboardApi.getTramKpis(),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get live tram forecasts
 */
export const useTramLiveForecasts = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.tramLiveForecasts,
    queryFn: () => dashboardApi.getTramLiveForecasts(),
    staleTime: 20_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get tram delays
 */
export const useTramDelays = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.tramDelays,
    queryFn: () => dashboardApi.getTramDelays(),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get tram hourly passenger distribution
 */
export const useTramHourlyDistribution = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.tramHourlyDistribution,
    queryFn: () => dashboardApi.getTramHourlyDistribution(),
    staleTime: 300_000, // 5 minutes - CSO data is static
  });
};

/**
 * Get upcoming events (default 10, show 5 initially)
 */
export const useEvents = (limit = 10) => {
  return useQuery({
    queryKey: MISC_KEYS.events(limit),
    queryFn: () => dashboardApi.getEvents(limit),
    staleTime: 300_000, // 5 minutes — events don't change often
    refetchInterval: 300_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get live pedestrian counts per site
 */
export const usePedestriansLive = (limit = 20) => {
  return useQuery({
    queryKey: MISC_KEYS.pedestriansLive(limit),
    queryFn: () => dashboardApi.getPedestriansLive(limit),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get active disruptions (construction, congestion, incidents)
 */
export const useActiveDisruptions = () => {
  return useQuery({
    queryKey: MISC_KEYS.disruptionsActive,
    queryFn: () => dashboardApi.getActiveDisruptions(),
    staleTime: 30_000,
    refetchInterval: 30_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get cycle coverage gaps (electoral divisions with low station density)
 */
export const useCycleCoverageGaps = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleCoverageGaps,
    queryFn: () => dashboardApi.getCycleCoverageGaps(),
    staleTime: 3_600_000, // 1 hour — recomputed nightly
  });
};

/**
 * Mark a coverage gap electoral division as planned for implementation
 */
export const useMarkCoverageGapProcessed = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (electoralDivision: string) =>
      dashboardApi.markCoverageGapProcessed(electoralDivision),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: DASHBOARD_KEYS.cycleCoverageGaps,
      });
    },
  });
};
