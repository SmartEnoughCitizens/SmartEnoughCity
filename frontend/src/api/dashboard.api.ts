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
  CarFuelTypeStat,
  CycleDashboardResponse,
  CycleStation,
  EventItem,
  HighTrafficPoint,
  IndicatorType,
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

  /**
   * Get frequently delayed trains ordered by total average delay descending
   */
  getTrainFrequentDelays: async (): Promise<TrainDelay[]> => {
    const { data } = await axiosInstance.get<TrainDelay[]>(
      API_ENDPOINTS.TRAIN_FREQUENT_DELAYS,
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
};
