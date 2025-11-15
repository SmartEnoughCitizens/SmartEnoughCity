import api from './api';
import { API_ENDPOINTS } from '../constants';

export const indicatorService = {
  // Get Car indicators
  getCarIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_CAR, { params });
    return response.data;
  },

  // Get Cycle indicators
  getCycleIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_CYCLE, { params });
    return response.data;
  },

  // Get Bus indicators
  getBusIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_BUS, { params });
    return response.data;
  },

  // Get Train indicators
  getTrainIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_TRAIN, { params });
    return response.data;
  },

  // Get Tram indicators
  getTramIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_TRAM, { params });
    return response.data;
  },

  // Get Pedestrian indicators
  getPedestrianIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_PEDESTRIAN, { params });
    return response.data;
  },

  // Get Events indicators
  getEventsIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_EVENTS, { params });
    return response.data;
  },

  // Get Construction indicators
  getConstructionIndicators: async (params) => {
    const response = await api.get(API_ENDPOINTS.INDICATORS_CONSTRUCTION, { params });
    return response.data;
  },
};

export default indicatorService;
