import api from './api';
import { API_ENDPOINTS } from '../constants';

export const authService = {
  // Login
  login: async (credentials) => {
    const response = await api.post(API_ENDPOINTS.LOGIN, credentials);
    return response.data;
  },

  // Register City Manager
  registerCityManager: async (userData) => {
    const response = await api.post(API_ENDPOINTS.REGISTER_CITY_MANAGER, userData);
    return response.data;
  },

  // Register Service Provider Admin
  registerServiceProviderAdmin: async (userData) => {
    const response = await api.post(API_ENDPOINTS.REGISTER_SERVICE_PROVIDER_ADMIN, userData);
    return response.data;
  },
};

export default authService;
