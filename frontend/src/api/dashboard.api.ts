/**
 * Dashboard API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  DisruptionItem,
  BusDashboardResponse,
  BusKpis,
  BusLiveVehicle,
  BusRouteUtilization,
  BusSystemPerformance,
  CarFuelTypeStat,
  CycleDashboardResponse,
  CycleStation,
  EventItem,
  HighTrafficPoint,
  HourlyNetworkProfileDTO,
  IndicatorType,
  StationClassificationDTO,
  StationHourlyUsageDTO,
  StationLiveDTO,
  NetworkSummaryDTO,
  RebalanceSuggestionDTO,
  StationODPairDTO,
  StationRankingDTO,
  JunctionEmission,
  PedestrianLive,
  TrainDashboardResponse,
  TrainDelay,
  TrainKpis,
  TrainLiveTrain,
  TrainServiceStats,
  TramDashboardResponse,
  TramKpis,
  TramLiveForecast,
  TramDelay,
  TramHourlyDistribution,
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
   * Get train station data for dashboard
   */
  getTrainData: async (params?: {
    limit?: number;
  }): Promise<TrainDashboardResponse> => {
    const { data } = await axiosInstance.get<TrainDashboardResponse>(
      API_ENDPOINTS.DASHBOARD_TRAIN,
      { params },
    );
    return data;
  },

  /**
   * Get tram stop data for dashboard
   */
  getTramData: async (params?: {
    limit?: number;
  }): Promise<TramDashboardResponse> => {
    const { data } = await axiosInstance.get<TramDashboardResponse>(
      API_ENDPOINTS.DASHBOARD_TRAM,
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
  getCarFuelTypeStatistics: async (): Promise<CarFuelTypeStat[]> => {
    const { data } = await axiosInstance.get<CarFuelTypeStat[]>(
      API_ENDPOINTS.CAR_FUEL_TYPE_STATISTICS,
    );
    return data;
  },

  /**
   * Get high traffic points with location and time slot data
   */
  getCarHighTrafficPoints: async (): Promise<HighTrafficPoint[]> => {
    const { data } = await axiosInstance.get<HighTrafficPoint[]>(
      API_ENDPOINTS.CAR_HIGH_TRAFFIC_POINTS,
    );
    return data;
  },

  /**
   * Get junction-level CO2 emission estimates
   */
  getCarJunctionEmissions: async (): Promise<JunctionEmission[]> => {
    const { data } = await axiosInstance.get<JunctionEmission[]>(
      API_ENDPOINTS.CAR_JUNCTION_EMISSIONS,
    );
    return data;
  },

  /**
   * Get train dashboard KPIs
   */
  getTrainKpis: async (): Promise<TrainKpis> => {
    const { data } = await axiosInstance.get<TrainKpis>(
      API_ENDPOINTS.TRAIN_KPIS,
    );
    return data;
  },

  /**
   * Get live train positions
   */
  getTrainLiveTrains: async (): Promise<TrainLiveTrain[]> => {
    const { data } = await axiosInstance.get<TrainLiveTrain[]>(
      API_ENDPOINTS.TRAIN_LIVE_TRAINS,
    );
    return data;
  },

  /**
   * Get train service reliability stats
   */
  getTrainServiceStats: async (): Promise<TrainServiceStats> => {
    const { data } = await axiosInstance.get<TrainServiceStats>(
      API_ENDPOINTS.TRAIN_SERVICE_STATS,
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

  getCycleNetworkHourlyProfile: async (params?: {
    days?: number;
  }): Promise<HourlyNetworkProfileDTO[]> => {
    const { data } = await axiosInstance.get<HourlyNetworkProfileDTO[]>(
      API_ENDPOINTS.CYCLE_DEMAND_NETWORK_HOURLY,
      { params },
    );
    return data;
  },

  getCycleStationClassification: async (params?: {
    days?: number;
  }): Promise<StationClassificationDTO[]> => {
    const { data } = await axiosInstance.get<StationClassificationDTO[]>(
      API_ENDPOINTS.CYCLE_DEMAND_CLASSIFICATION,
      { params },
    );
    return data;
  },

  getCycleODPairs: async (params?: {
    days?: number;
    limit?: number;
  }): Promise<StationODPairDTO[]> => {
    const { data } = await axiosInstance.get<StationODPairDTO[]>(
      API_ENDPOINTS.CYCLE_DEMAND_OD_PAIRS,
      { params },
    );
    return data;
  },

  getCycleStationHourlyUsage: async (params?: {
    days?: number;
    limit?: number;
  }): Promise<StationHourlyUsageDTO[]> => {
    const { data } = await axiosInstance.get<StationHourlyUsageDTO[]>(
      API_ENDPOINTS.CYCLE_DEMAND_STATION_HOURLY,
      { params },
    );
    return data;
  },

  /**
   * Get frequently delayed trains ordered by total average delay descending
   */
  getTrainFrequentDelays: async (): Promise<TrainDelay[]> => {
    const { data } = await axiosInstance.get<TrainDelay[]>(
      API_ENDPOINTS.TRAIN_FREQUENT_DELAYS,
    );
    return data;
  },

  /**
   * Get tram dashboard KPIs
   */
  getTramKpis: async (): Promise<TramKpis> => {
    const { data } = await axiosInstance.get<TramKpis>(API_ENDPOINTS.TRAM_KPIS);
    return data;
  },

  /**
   * Get live tram forecasts
   */
  getTramLiveForecasts: async (): Promise<TramLiveForecast[]> => {
    const { data } = await axiosInstance.get<TramLiveForecast[]>(
      API_ENDPOINTS.TRAM_LIVE_FORECASTS,
    );
    return data;
  },

  /**
   * Get tram delays
   */
  getTramDelays: async (): Promise<TramDelay[]> => {
    const { data } = await axiosInstance.get<TramDelay[]>(
      API_ENDPOINTS.TRAM_DELAYS,
    );
    return data;
  },

  /**
   * Get tram hourly passenger distribution
   */
  getTramHourlyDistribution: async (): Promise<TramHourlyDistribution[]> => {
    const { data } = await axiosInstance.get<TramHourlyDistribution[]>(
      API_ENDPOINTS.TRAM_HOURLY_DISTRIBUTION,
    );
    return data;
  },
  /**
   * Get upcoming events
   */
  getEvents: async (limit = 10): Promise<EventItem[]> => {
    const { data } = await axiosInstance.get<EventItem[]>(
      API_ENDPOINTS.EVENTS,
      {
        params: { limit },
      },
    );
    return data;
  },

  /**
   * Get live pedestrian counts per site
   */
  getPedestriansLive: async (limit = 20): Promise<PedestrianLive[]> => {
    const { data } = await axiosInstance.get<PedestrianLive[]>(
      API_ENDPOINTS.PEDESTRIANS_LIVE,
      { params: { limit } },
    );
    return data;
  },

  /**
   * Get active disruptions (construction, congestion, incidents)
   */
  getActiveDisruptions: async (): Promise<DisruptionItem[]> => {
    const { data } = await axiosInstance.get<DisruptionItem[]>(
      API_ENDPOINTS.DISRUPTIONS_ACTIVE,
    );
    return data;
  },
};
