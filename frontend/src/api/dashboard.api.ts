/**
 * Dashboard API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  BusDashboardResponse,
  CycleDashboardResponse,
  CycleStation,
  IndicatorType,
  StationLiveDTO,
  NetworkSummaryDTO,
  NetworkKpiDTO,
  RebalanceSuggestionDTO,
  StationRankingDTO,
  StationODPairDTO,
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

  // ── Cycle Metrics ──────────────────────────────────────────────────────────

  getCycleStationsLive: async (): Promise<StationLiveDTO[]> => {
    const { data } = await axiosInstance.get<StationLiveDTO[]>(
      API_ENDPOINTS.CYCLE_STATIONS_LIVE,
    );
    return data;
  },

  getCycleNetworkSummary: async (): Promise<NetworkSummaryDTO> => {
    const { data } = await axiosInstance.get<NetworkSummaryDTO>(
      API_ENDPOINTS.CYCLE_NETWORK_SUMMARY,
    );
    return data;
  },

  getCycleNetworkKpi: async (): Promise<NetworkKpiDTO> => {
    const { data } = await axiosInstance.get<NetworkKpiDTO>(
      API_ENDPOINTS.CYCLE_NETWORK_KPI,
    );
    return data;
  },

  getCycleBusiestStations: async (params?: {
    limit?: number;
  }): Promise<StationRankingDTO[]> => {
    const { data } = await axiosInstance.get<StationRankingDTO[]>(
      API_ENDPOINTS.CYCLE_RANKINGS_BUSIEST,
      { params },
    );
    return data;
  },

  getCycleUnderusedStations: async (params?: {
    limit?: number;
  }): Promise<StationRankingDTO[]> => {
    const { data } = await axiosInstance.get<StationRankingDTO[]>(
      API_ENDPOINTS.CYCLE_RANKINGS_UNDERUSED,
      { params },
    );
    return data;
  },

  getCycleRebalancingSuggestions: async (params?: {
    limit?: number;
  }): Promise<RebalanceSuggestionDTO[]> => {
    const { data } = await axiosInstance.get<RebalanceSuggestionDTO[]>(
      API_ENDPOINTS.CYCLE_NETWORK_REBALANCING,
      { params },
    );
    return data;
  },

  getCycleODHeatmap: async (params?: {
    limit?: number;
  }): Promise<StationODPairDTO[]> => {
    const { data } = await axiosInstance.get<StationODPairDTO[]>(
      API_ENDPOINTS.CYCLE_OD_HEATMAP,
      { params },
    );
    return data;
  },
};
