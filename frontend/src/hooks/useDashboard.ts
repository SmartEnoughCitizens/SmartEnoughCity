/**
 * React Query hooks for dashboard data
 */

import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/api";

export const DASHBOARD_KEYS = {
  bus: (routeId?: string, limit?: number) =>
    ["dashboard", "bus", { routeId, limit }] as const,
  cycle: (limit?: number) => ["dashboard", "cycle", { limit }] as const,
  train: (limit?: number) => ["dashboard", "train", { limit }] as const,
  availableBikes: ["dashboard", "cycle", "available-bikes"] as const,
  availableDocks: ["dashboard", "cycle", "available-docks"] as const,
  busRoutes: ["dashboard", "bus", "routes"] as const,
  indicatorTypes: ["dashboard", "indicator-types"] as const,
  cycleStationsLive: ["cycle", "stations", "live"] as const,
  cycleNetworkSummary: ["cycle", "network", "summary"] as const,
  cycleNetworkKpi: ["cycle", "network", "kpi"] as const,
  cycleBusiestStations: (limit?: number) =>
    ["cycle", "rankings", "busiest", { limit }] as const,
  cycleUnderusedStations: (limit?: number) =>
    ["cycle", "rankings", "underused", { limit }] as const,
  cycleRebalancing: (limit?: number) =>
    ["cycle", "network", "rebalancing", { limit }] as const,
  cycleODHeatmap: (limit?: number) =>
    ["cycle", "od", "heatmap", { limit }] as const,
  busKpis: ["bus", "kpis"] as const,
  busLiveVehicles: ["bus", "live-vehicles"] as const,
  busRouteUtilization: ["bus", "route-utilization"] as const,
  busSystemPerformance: ["bus", "system-performance"] as const,
  carFuelTypeStatistics: ["car", "fuel-type-statistics"] as const,
  carHighTrafficPoints: ["car", "high-traffic-points"] as const,
  trainKpis: ["train", "kpis"] as const,
  trainLiveTrains: ["train", "live-trains"] as const,
  trainServiceStats: ["train", "service-stats"] as const,
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

export const useCycleNetworkKpi = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleNetworkKpi,
    queryFn: () => dashboardApi.getCycleNetworkKpi(),
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

export const useCycleODHeatmap = (limit = 50) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleODHeatmap(limit),
    queryFn: () => dashboardApi.getCycleODHeatmap({ limit }),
    staleTime: 300_000,
    refetchInterval: 300_000, // 5 min — historical data changes slowly
  });
};
