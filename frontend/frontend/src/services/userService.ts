import api from './api';
import { API_ENDPOINTS } from '../constants';
import { UserProfile, CreateUserRequest, UpdateUserRequest } from '../types';

export const userService = {
  // Get all users
  getAllUsers: async (): Promise<UserProfile[]> => {
    const response = await api.get<UserProfile[]>(API_ENDPOINTS.USERS);
    return response.data;
  },

  // Get user by ID
  getUserById: async (id: string): Promise<UserProfile> => {
    const response = await api.get<UserProfile>(API_ENDPOINTS.USER_BY_ID(id));
    return response.data;
  },

  // Create service provider user
  createServiceProviderUser: async (userData: CreateUserRequest): Promise<UserProfile> => {
    const response = await api.post<UserProfile>(API_ENDPOINTS.SERVICE_PROVIDER_USERS, userData);
    return response.data;
  },

  // Update user
  updateUser: async (id: string, userData: UpdateUserRequest): Promise<UserProfile> => {
    const response = await api.put<UserProfile>(API_ENDPOINTS.USER_BY_ID(id), userData);
    return response.data;
  },

  // Delete user
  deleteUser: async (id: string): Promise<void> => {
    const response = await api.delete(API_ENDPOINTS.USER_BY_ID(id));
    return response.data;
  },

  // Get service provider users
  getServiceProviderUsers: async (): Promise<UserProfile[]> => {
    const response = await api.get<UserProfile[]>(API_ENDPOINTS.SERVICE_PROVIDER_USERS);
    return response.data;
  },
};

export default userService;
