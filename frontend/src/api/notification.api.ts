/**
 * Notification API client
 */

import { axiosInstance } from "@/utils/axios";
import { API_ENDPOINTS } from "@/config/api.config";
import type { NotificationResponse } from "@/types";

export const notificationApi = {
  /**
   * Get user notifications
   */
  getUserNotifications: async (
    userId: string,
  ): Promise<NotificationResponse> => {
    const { data } = await axiosInstance.get<NotificationResponse>(
      API_ENDPOINTS.NOTIFICATIONS(userId),
    );
    return data;
  },

  /**
   * Dismiss (delete) a single notification.
   * Frontend gates this behind write permission; backend should also enforce it.
   */
  dismissNotification: async (
    userId: string,
    notificationId: string,
  ): Promise<void> => {
    await axiosInstance.delete(
      API_ENDPOINTS.NOTIFICATION_DISMISS(userId, notificationId),
    );
  },
};
