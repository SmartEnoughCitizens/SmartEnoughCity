import api from './api';
import { API_ENDPOINTS } from '../constants';
import { TransportIndicatorData, EventIndicator, ConstructionIndicator, IndicatorFilters } from '../types';

export const indicatorService = {
  // Get Car indicators
  getCarIndicators: async (params?: IndicatorFilters): Promise<TransportIndicatorData> => {
    const response = await api.get<TransportIndicatorData>(API_ENDPOINTS.INDICATORS_CAR, { params });
    return response.data;
  },

  // Get Cycle indicators
  getCycleIndicators: async (params?: IndicatorFilters): Promise<TransportIndicatorData> => {
    const response = await api.get<TransportIndicatorData>(API_ENDPOINTS.INDICATORS_CYCLE, { params });
    return response.data;
  },

  // Get Bus indicators
  getBusIndicators: async (params?: IndicatorFilters): Promise<TransportIndicatorData> => {
    const response = await api.get<TransportIndicatorData>(API_ENDPOINTS.INDICATORS_BUS, { params });
    return response.data;
  },

  // Get Train indicators
  getTrainIndicators: async (params?: IndicatorFilters): Promise<TransportIndicatorData> => {
    const response = await api.get<TransportIndicatorData>(API_ENDPOINTS.INDICATORS_TRAIN, { params });
    return response.data;
  },

  // Get Tram indicators
  getTramIndicators: async (params?: IndicatorFilters): Promise<TransportIndicatorData> => {
    const response = await api.get<TransportIndicatorData>(API_ENDPOINTS.INDICATORS_TRAM, { params });
    return response.data;
  },

  // Get Pedestrian indicators
  getPedestrianIndicators: async (params?: IndicatorFilters): Promise<TransportIndicatorData> => {
    const response = await api.get<TransportIndicatorData>(API_ENDPOINTS.INDICATORS_PEDESTRIAN, { params });
    return response.data;
  },

  // Get Events indicators
  getEventsIndicators: async (params?: IndicatorFilters): Promise<EventIndicator[]> => {
    const response = await api.get<EventIndicator[]>(API_ENDPOINTS.INDICATORS_EVENTS, { params });
    return response.data;
  },

  // Get Construction indicators
  getConstructionIndicators: async (params?: IndicatorFilters): Promise<ConstructionIndicator[]> => {
    const response = await api.get<ConstructionIndicator[]>(API_ENDPOINTS.INDICATORS_CONSTRUCTION, { params });
    return response.data;
  },
};

export default indicatorService;
