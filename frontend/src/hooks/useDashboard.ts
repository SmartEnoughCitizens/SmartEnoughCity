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
  busKpis: ["bus", "kpis"] as const,
  busLiveVehicles: ["bus", "live-vehicles"] as const,
  busRouteUtilization: ["bus", "route-utilization"] as const,
  busSystemPerformance: ["bus", "system-performance"] as const,
  carFuelTypeStatistics: ["car", "fuel-type-statistics"] as const,
  carHighTrafficPoints: ["car", "high-traffic-points"] as const,
  trainKpis: ["train", "kpis"] as const,
  trainLiveTrains: ["train", "live-trains"] as const,
  trainServiceStats: ["train", "service-stats"] as const,
  trainUtilization: ["train", "utilization"] as const,
  trainDelayPatterns: (days: number) =>
    ["train", "delay-patterns", days] as const,
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

/**
 * Get per-station utilization (service count + delay + HIGH/MEDIUM/LOW band).
 * Refreshes every 60 s — utilization changes slowly.
 */
export const useTrainUtilization = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.trainUtilization,
    queryFn: () => dashboardApi.getTrainUtilization(),
    staleTime: 60_000,
    refetchInterval: 60_000,
    refetchIntervalInBackground: true,
  });
};

/**
 * Get recurring delay patterns aggregated by station, route, train type, and time-of-day.
 * @param days look-back window: 7 | 30 | 90 (default 30)
 */
export const useTrainDelayPatterns = (days: 7 | 30 | 90 = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.trainDelayPatterns(days),
    queryFn: () => dashboardApi.getTrainDelayPatterns(days),
    staleTime: 120_000,
    refetchInterval: 120_000,
    refetchIntervalInBackground: true,
  });
};
