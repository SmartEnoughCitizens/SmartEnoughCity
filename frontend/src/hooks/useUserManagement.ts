/**
 * React Query hooks for user management
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { userManagementApi } from "@/api";
import type {
  ChangePasswordRequest,
  RegisterUserRequest,
  UpdateProfileRequest,
} from "@/types";

const USER_KEYS = {
  list: ["users"] as const,
};

const PROFILE_KEYS = {
  profile: ["profile"] as const,
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

/**
 * Fetch current user's profile
 */
export const useGetProfile = () => {
  return useQuery({
    queryKey: PROFILE_KEYS.profile,
    queryFn: () => userManagementApi.getProfile(),
  });
};

/**
 * Update current user's profile mutation
 */
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: UpdateProfileRequest) =>
      userManagementApi.updateProfile(request),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: PROFILE_KEYS.profile });
    },
  });
};

/**
 * Change current user's password mutation
 */
export const useChangePassword = () => {
  return useMutation({
    mutationFn: (request: ChangePasswordRequest) =>
      userManagementApi.changePassword(request),
  });
};

/**
 * Fetch user counts per role (Government_Admin only)
 */
export const useGetUserCounts = () => {
  return useQuery({
    queryKey: ["user-counts"],
    queryFn: () => userManagementApi.getUserCounts(),
  });
};
