/**
 * Dashboard API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  BusDashboardResponse,
  BusKpis,
  BusLiveVehicle,
  BusRouteUtilization,
  BusSystemPerformance,
  CycleDashboardResponse,
  CycleStation,
  IndicatorType,
} from "@/types";

export const dashboardApi = {
  /**
   * Get bus data for dashboard
   */
  getBusData: async (params?: {
    routeId?: string;
    limit?: number;
  }): Promise<BusDashboardResponse> => {
    const { data } = await axiosInstance.get<BusDashboardResponse>(
      API_ENDPOINTS.DASHBOARD_BUS,
      { params },
    );
    return data;
  },

  /**
   * Get cycle station data for dashboard
   */
  getCycleData: async (params?: {
    limit?: number;
  }): Promise<CycleDashboardResponse> => {
    const { data } = await axiosInstance.get<CycleDashboardResponse>(
      API_ENDPOINTS.DASHBOARD_CYCLE,
      { params },
    );
    return data;
  },

  /**
   * Get cycle stations with available bikes
   */
  getAvailableBikes: async (): Promise<CycleStation[]> => {
    const { data } = await axiosInstance.get<CycleStation[]>(
      API_ENDPOINTS.DASHBOARD_CYCLE_AVAILABLE_BIKES,
    );
    return data;
  },

  /**
   * Get cycle stations with available docks
   */
  getAvailableDocks: async (): Promise<CycleStation[]> => {
    const { data } = await axiosInstance.get<CycleStation[]>(
      API_ENDPOINTS.DASHBOARD_CYCLE_AVAILABLE_DOCKS,
    );
    return data;
  },

  /**
   * Get all bus routes
   */
  getBusRoutes: async (): Promise<string[]> => {
    const { data } = await axiosInstance.get<string[]>(
      API_ENDPOINTS.DASHBOARD_BUS_ROUTES,
    );
    return data;
  },

  /**
   * Get available indicator types
   */
  getIndicatorTypes: async (): Promise<IndicatorType[]> => {
    const { data } = await axiosInstance.get<IndicatorType[]>(
      API_ENDPOINTS.DASHBOARD_INDICATOR_TYPES,
    );
    return data;
  },

  /**
   * Get bus dashboard KPIs
   */
  getBusKpis: async (): Promise<BusKpis> => {
    const { data } = await axiosInstance.get<BusKpis>(API_ENDPOINTS.BUS_KPIS);
    return data;
  },

  /**
   * Get live bus vehicle positions
   */
  getBusLiveVehicles: async (): Promise<BusLiveVehicle[]> => {
    const { data } = await axiosInstance.get<BusLiveVehicle[]>(
      API_ENDPOINTS.BUS_LIVE_VEHICLES,
    );
    return data;
  },

  /**
   * Get route utilization data
   */
  getBusRouteUtilization: async (): Promise<BusRouteUtilization[]> => {
    const { data } = await axiosInstance.get<BusRouteUtilization[]>(
      API_ENDPOINTS.BUS_ROUTE_UTILIZATION,
    );
    return data;
  },

  /**
   * Get system performance metrics
   */
  getBusSystemPerformance: async (): Promise<BusSystemPerformance> => {
    const { data } = await axiosInstance.get<BusSystemPerformance>(
      API_ENDPOINTS.BUS_SYSTEM_PERFORMANCE,
    );
    return data;
  },
};
