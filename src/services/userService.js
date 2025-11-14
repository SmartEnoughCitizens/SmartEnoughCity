import api from './api';
import { API_ENDPOINTS } from '../constants';

export const userService = {
  // Get all users
  getAllUsers: async () => {
    const response = await api.get(API_ENDPOINTS.USERS);
    return response.data;
  },

  // Get user by ID
  getUserById: async (id) => {
    const response = await api.get(API_ENDPOINTS.USER_BY_ID(id));
    return response.data;
  },

  // Create service provider user
  createServiceProviderUser: async (userData) => {
    const response = await api.post(API_ENDPOINTS.SERVICE_PROVIDER_USERS, userData);
    return response.data;
  },

  // Update user
  updateUser: async (id, userData) => {
    const response = await api.put(API_ENDPOINTS.USER_BY_ID(id), userData);
    return response.data;
  },

  // Delete user
  deleteUser: async (id) => {
    const response = await api.delete(API_ENDPOINTS.USER_BY_ID(id));
    return response.data;
  },

  // Get service provider users
  getServiceProviderUsers: async () => {
    const response = await api.get(API_ENDPOINTS.SERVICE_PROVIDER_USERS);
    return response.data;
  },
};

export default userService;
