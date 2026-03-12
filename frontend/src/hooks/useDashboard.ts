/**
 * React Query hooks for dashboard data
 */

import { useQuery } from "@tanstack/react-query";
import { dashboardApi } from "@/api";

export const DASHBOARD_KEYS = {
  bus: (routeId?: string, limit?: number) =>
    ["dashboard", "bus", { routeId, limit }] as const,
  cycle: (limit?: number) => ["dashboard", "cycle", { limit }] as const,
  availableBikes: ["dashboard", "cycle", "available-bikes"] as const,
  availableDocks: ["dashboard", "cycle", "available-docks"] as const,
  busRoutes: ["dashboard", "bus", "routes"] as const,
  indicatorTypes: ["dashboard", "indicator-types"] as const,
  cycleStationsLive: ["cycle", "stations", "live"] as const,
  cycleNetworkSummary: ["cycle", "network", "summary"] as const,
  cycleNetworkKpi: ["cycle", "network", "kpi"] as const,
  cycleBusiestStations: (days?: number, limit?: number) =>
    ["cycle", "rankings", "busiest", { days, limit }] as const,
  cycleUnderusedStations: (days?: number, limit?: number) =>
    ["cycle", "rankings", "underused", { days, limit }] as const,
  cycleEmptyEvents: (days?: number, limit?: number) =>
    ["cycle", "events", "empty", { days, limit }] as const,
  cycleFullEvents: (days?: number, limit?: number) =>
    ["cycle", "events", "full", { days, limit }] as const,
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

// ── Cycle Metrics hooks ──────────────────────────────────────────────────────

export const useCycleStationsLive = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleStationsLive,
    queryFn: () => dashboardApi.getCycleStationsLive(),
    staleTime: 30_000,
  });
};

export const useCycleNetworkSummary = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleNetworkSummary,
    queryFn: () => dashboardApi.getCycleNetworkSummary(),
    staleTime: 30_000,
  });
};

export const useCycleNetworkKpi = () => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleNetworkKpi,
    queryFn: () => dashboardApi.getCycleNetworkKpi(),
    staleTime: 60_000,
  });
};

export const useCycleBusiestStations = (days = 7, limit = 10) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleBusiestStations(days, limit),
    queryFn: () => dashboardApi.getCycleBusiestStations({ days, limit }),
    staleTime: 60_000,
  });
};

export const useCycleUnderusedStations = (days = 7, limit = 10) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleUnderusedStations(days, limit),
    queryFn: () => dashboardApi.getCycleUnderusedStations({ days, limit }),
    staleTime: 60_000,
  });
};

export const useCycleEmptyEvents = (days = 7, limit = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleEmptyEvents(days, limit),
    queryFn: () => dashboardApi.getCycleEmptyEvents({ days, limit }),
    staleTime: 60_000,
  });
};

export const useCycleFullEvents = (days = 7, limit = 30) => {
  return useQuery({
    queryKey: DASHBOARD_KEYS.cycleFullEvents(days, limit),
    queryFn: () => dashboardApi.getCycleFullEvents({ days, limit }),
    staleTime: 60_000,
  });
};
