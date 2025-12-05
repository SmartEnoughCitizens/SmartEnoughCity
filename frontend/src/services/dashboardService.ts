import api from './api';
import { API_ENDPOINTS } from '../constants';
import type { DashboardStats, DashboardOverview } from '../types';

export const dashboardService = {
  // Get dashboard statistics
  getStats: async (): Promise<DashboardStats> => {
    const response = await api.get<DashboardStats>(API_ENDPOINTS.DASHBOARD_STATS);
    return response.data;
  },

  // Get dashboard overview
  getOverview: async (): Promise<DashboardOverview> => {
    const response = await api.get<DashboardOverview>(API_ENDPOINTS.DASHBOARD_OVERVIEW);
    return response.data;
  },
};

export default dashboardService;
