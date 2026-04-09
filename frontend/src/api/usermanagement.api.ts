/**
 * User management API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  ChangePasswordRequest,
  RegisterUserRequest,
  RegisterUserResponse,
  UpdateProfileRequest,
  UserInfo,
  UserProfile,
} from "@/types";

export const userManagementApi = {
  registerUser: async (
    request: RegisterUserRequest,
  ): Promise<RegisterUserResponse> => {
    const { data } = await axiosInstance.post<RegisterUserResponse>(
      API_ENDPOINTS.USER_REGISTER,
      request,
    );
    return data;
  },

  getUsers: async (): Promise<UserInfo[]> => {
    const { data } = await axiosInstance.get<UserInfo[]>(
      API_ENDPOINTS.USER_LIST,
    );
    return data;
  },

  deleteUser: async (username: string): Promise<{ message: string }> => {
    const { data } = await axiosInstance.delete<{ message: string }>(
      API_ENDPOINTS.USER_DELETE,
      { params: { username } },
    );
    return data;
  },

  getProfile: async (): Promise<UserProfile> => {
    const { data } = await axiosInstance.get<UserProfile>(
      API_ENDPOINTS.USER_PROFILE,
    );
    return data;
  },

  updateProfile: async (
    request: UpdateProfileRequest,
  ): Promise<{ message: string }> => {
    const { data } = await axiosInstance.put<{ message: string }>(
      API_ENDPOINTS.USER_PROFILE,
      request,
    );
    return data;
  },

  changePassword: async (
    request: ChangePasswordRequest,
  ): Promise<{ message: string }> => {
    const { data } = await axiosInstance.put<{ message: string }>(
      API_ENDPOINTS.USER_PASSWORD,
      request,
    );
    return data;
  },

  getUserCounts: async (): Promise<Record<string, number>> => {
    const { data } = await axiosInstance.get<Record<string, number>>(
      API_ENDPOINTS.USER_COUNTS,
    );
    return data;
  },
};
