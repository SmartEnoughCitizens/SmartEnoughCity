import api from './api';
import { API_ENDPOINTS } from '../constants';
import type { Notification } from '../types';

export const notificationService = {
  // Get all notifications
  getNotifications: async (): Promise<Notification[]> => {
    const response = await api.get<Notification[]>(API_ENDPOINTS.NOTIFICATIONS);
    return response.data;
  },

  // Mark notification as read
  markAsRead: async (id: string): Promise<Notification> => {
    const response = await api.put<Notification>(API_ENDPOINTS.MARK_NOTIFICATION_READ(id));
    return response.data;
  },

  // Mark all notifications as read
  markAllAsRead: async (): Promise<void> => {
    const response = await api.put(API_ENDPOINTS.MARK_ALL_READ);
    return response.data;
  },
};

export default notificationService;
