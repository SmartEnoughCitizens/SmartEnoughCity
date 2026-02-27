/**
 * User management API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type {
  RegisterUserRequest,
  RegisterUserResponse,
  UserInfo,
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
};
