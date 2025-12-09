/**
 * Notification API client
 */

import { axiosInstance } from '@/utils/axios';
import { API_ENDPOINTS } from '@/config/api.config';
import type { NotificationResponse } from '@/types';

export const notificationApi = {
  /**
   * Get user notifications
   */
  getUserNotifications: async (userId: string): Promise<NotificationResponse> => {
    const { data } = await axiosInstance.get<NotificationResponse>(
      API_ENDPOINTS.NOTIFICATIONS(userId)
    );
    return data;
  },
};
