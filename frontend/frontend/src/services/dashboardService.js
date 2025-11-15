import api from './api';
import { API_ENDPOINTS } from '../constants';

export const dashboardService = {
  // Get dashboard statistics
  getStats: async () => {
    const response = await api.get(API_ENDPOINTS.DASHBOARD_STATS);
    return response.data;
  },

  // Get dashboard overview
  getOverview: async () => {
    const response = await api.get(API_ENDPOINTS.DASHBOARD_OVERVIEW);
    return response.data;
  },
};

export default dashboardService;
