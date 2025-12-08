import api from './api';
import { API_ENDPOINTS } from '../constants';
import type { SystemStatus, SystemHealth } from '../types';

export const systemService = {
  // Get system status
  getStatus: async (): Promise<SystemStatus> => {
    const response = await api.get<SystemStatus>(API_ENDPOINTS.SYSTEM_STATUS);
    return response.data;
  },

  // Get system health
  getHealth: async (): Promise<SystemHealth> => {
    const response = await api.get<SystemHealth>(API_ENDPOINTS.SYSTEM_HEALTH);
    return response.data;
  },
};

export default systemService;
