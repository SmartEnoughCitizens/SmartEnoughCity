/**
 * React Query hooks for authentication
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/api";
import type { ForgotPasswordRequest, LoginRequest, ResetPasswordRequest } from "@/types";

export const AUTH_KEYS = {
  health: ["auth", "health"] as const,
};

/**
 * Login mutation
 */
export const useLogin = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (credentials: LoginRequest) => authApi.login(credentials),
    onSuccess: (data) => {
      // Store tokens in localStorage
      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);
      localStorage.setItem("username", data.username);

      // Invalidate all queries on login
      queryClient.invalidateQueries();
    },
  });
};

/**
 * Logout mutation
 */
export const useLogout = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => {
      authApi.logout();
      return Promise.resolve();
    },
    onSuccess: () => {
      // Clear all queries on logout
      queryClient.clear();
    },
  });
};

/**
 * Forgot password mutation — sends a reset link to the given email
 */
export const useForgotPassword = () => {
  return useMutation({
    mutationFn: (request: ForgotPasswordRequest) => authApi.forgotPassword(request),
  });
};

/**
 * Reset password mutation — validates the token and sets a new password
 */
export const useResetPassword = () => {
  return useMutation({
    mutationFn: (request: ResetPasswordRequest) => authApi.resetPassword(request),
  });
};

/**
 * Check auth service health
 */
export const useAuthHealth = () => {
  return useQuery({
    queryKey: AUTH_KEYS.health,
    queryFn: () => authApi.checkHealth(),
    staleTime: 60_000, // 1 minute
  });
};
