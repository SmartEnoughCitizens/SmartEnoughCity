/**
 * Notification API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type { NotificationResponse } from "@/types";

export const notificationApi = {
  getUserNotifications: async (
    userId: string,
  ): Promise<NotificationResponse> => {
    const { data } = await axiosInstance.get<NotificationResponse>(
      API_ENDPOINTS.NOTIFICATIONS(userId),
    );
    return data;
  },

  setReadState: async (
    userId: string,
    notificationId: string,
    read: boolean,
  ): Promise<void> => {
    await axiosInstance.patch(
      `${API_ENDPOINTS.NOTIFICATIONS(userId)}/${notificationId}/read`,
      null,
      { params: { read } },
    );
  },

  softDelete: async (userId: string, notificationId: string): Promise<void> => {
    await axiosInstance.delete(
      `${API_ENDPOINTS.NOTIFICATIONS(userId)}/${notificationId}`,
    );
  },

  restore: async (userId: string, notificationId: string): Promise<void> => {
    await axiosInstance.patch(
      `${API_ENDPOINTS.NOTIFICATIONS(userId)}/${notificationId}/restore`,
    );
  },

  getBin: async (userId: string): Promise<NotificationResponse> => {
    const { data } = await axiosInstance.get<NotificationResponse>(
      API_ENDPOINTS.NOTIFICATIONS_BIN(userId),
    );
    return data;
  },
};
