/**
 * React Query hooks for user management
 */

import { useMutation } from "@tanstack/react-query";
import { userManagementApi } from "@/api";
import type { RegisterUserRequest } from "@/types";

/**
 * Register user mutation
 */
export const useRegisterUser = () => {
  return useMutation({
    mutationFn: (request: RegisterUserRequest) =>
      userManagementApi.registerUser(request),
  });
};
