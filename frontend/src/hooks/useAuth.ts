/**
 * React Query hooks for authentication
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authApi } from "@/api";
import type { LoginRequest } from "@/types";

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
 * Check auth service health
 */
export const useAuthHealth = () => {
  return useQuery({
    queryKey: AUTH_KEYS.health,
    queryFn: () => authApi.checkHealth(),
    staleTime: 60_000, // 1 minute
  });
};
