/**
 * React Query hooks for user management
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { userManagementApi } from "@/api";
import type { RegisterUserRequest } from "@/types";

const USER_KEYS = {
  list: ["users"] as const,
};

/**
 * Fetch manageable users
 */
export const useGetUsers = () => {
  return useQuery({
    queryKey: USER_KEYS.list,
    queryFn: () => userManagementApi.getUsers(),
  });
};

/**
 * Register user mutation
 */
export const useRegisterUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: RegisterUserRequest) =>
      userManagementApi.registerUser(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: USER_KEYS.list });
    },
  });
};

/**
 * Delete user mutation
 */
export const useDeleteUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (username: string) => userManagementApi.deleteUser(username),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: USER_KEYS.list });
    },
  });
};
