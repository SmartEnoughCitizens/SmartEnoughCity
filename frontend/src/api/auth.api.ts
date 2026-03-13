/**
 * Authentication API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  ForgotPasswordRequest,
  HealthResponse,
  LoginRequest,
  LoginResponse,
  MessageResponse,
  ResetPasswordRequest,
} from "@/types";

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
   * Request a password reset email
   */
  forgotPassword: async (request: ForgotPasswordRequest): Promise<MessageResponse> => {
    const { data } = await axiosInstance.post<MessageResponse>(
      API_ENDPOINTS.AUTH_FORGOT_PASSWORD,
      request,
    );
    return data;
  },

  /**
   * Reset password using a token from the reset email
   */
  resetPassword: async (request: ResetPasswordRequest): Promise<MessageResponse> => {
    const { data } = await axiosInstance.post<MessageResponse>(
      API_ENDPOINTS.AUTH_RESET_PASSWORD,
      request,
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
