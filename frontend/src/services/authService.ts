import api from './api';
import { API_ENDPOINTS } from '../constants';
import type { LoginCredentials, RegisterData, AuthResponse } from '../types';

export const authService = {
  // Login
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>(API_ENDPOINTS.LOGIN, credentials);
    return response.data;
  },

  // Register City Manager
  registerCityManager: async (userData: RegisterData): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>(API_ENDPOINTS.REGISTER_CITY_MANAGER, userData);
    return response.data;
  },

  // Register Service Provider Admin
  registerServiceProviderAdmin: async (userData: RegisterData): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>(API_ENDPOINTS.REGISTER_SERVICE_PROVIDER_ADMIN, userData);
    return response.data;
  },
};

export default authService;
