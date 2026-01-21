/**
 * Authentication API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type { LoginRequest, LoginResponse, HealthResponse } from "@/types";

export const authApi = {
  /**
   * Login user
   */
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const { data } = await axiosInstance.post<LoginResponse>(
      API_ENDPOINTS.AUTH_LOGIN,
      credentials,
    );
    return data;
  },

  /**
   * Check auth service health
   */
  checkHealth: async (): Promise<HealthResponse> => {
    const { data } = await axiosInstance.get<HealthResponse>(
      API_ENDPOINTS.AUTH_HEALTH,
    );
    return data;
  },

  /**
   * Logout (client-side only)
   */
  logout: (): void => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("username");
  },
};
