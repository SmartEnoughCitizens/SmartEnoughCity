import api from './api';
import { API_ENDPOINTS } from '../constants';

export const systemService = {
  // Get system status
  getStatus: async () => {
    const response = await api.get(API_ENDPOINTS.SYSTEM_STATUS);
    return response.data;
  },

  // Get system health
  getHealth: async () => {
    const response = await api.get(API_ENDPOINTS.SYSTEM_HEALTH);
    return response.data;
  },
};

export default systemService;
