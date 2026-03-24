/**
 * Axios instance with interceptors
 */

import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { API_CONFIG, API_ENDPOINTS } from "@/config/api.config";
import type { LoginResponse } from "@/types";

// Create axios instance
export const axiosInstance = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: API_CONFIG.HEADERS,
});

// Request interceptor
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Add auth token if available
    const token = localStorage.getItem("accessToken");
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  },
);

// Token refresh state
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null) {
  for (const { resolve, reject } of failedQueue) {
    if (error) {
      reject(error);
    } else {
      resolve(token!);
    }
  }
  failedQueue = [];
}

function clearAuthAndRedirect() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("username");
  if (globalThis.location.pathname !== "/login") {
    globalThis.location.href = "/login";
  }
}

// Response interceptor
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    if (error.response?.status !== 401 || originalRequest._retry) {
      throw error;
    }

    const storedRefreshToken = localStorage.getItem("refreshToken");
    if (!storedRefreshToken) {
      clearAuthAndRedirect();
      throw error;
    }

    if (isRefreshing) {
      // Queue this request until the refresh completes
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((newToken) => {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return axiosInstance(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      // Use raw axios to avoid triggering this interceptor again
      const { data } = await axios.post<LoginResponse>(
        `${API_CONFIG.BASE_URL}${API_ENDPOINTS.AUTH_REFRESH}`,
        { refreshToken: storedRefreshToken },
        { headers: { "Content-Type": "application/json" } },
      );

      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);

      processQueue(null, data.accessToken);
      originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
      return axiosInstance(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      clearAuthAndRedirect();
      throw refreshError;
    } finally {
      isRefreshing = false;
    }
  },
);

export default axiosInstance;
