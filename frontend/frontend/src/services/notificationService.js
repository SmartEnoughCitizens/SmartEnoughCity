import api from './api';
import { API_ENDPOINTS } from '../constants';

export const notificationService = {
  // Get all notifications
  getNotifications: async () => {
    const response = await api.get(API_ENDPOINTS.NOTIFICATIONS);
    return response.data;
  },

  // Mark notification as read
  markAsRead: async (id) => {
    const response = await api.put(API_ENDPOINTS.MARK_NOTIFICATION_READ(id));
    return response.data;
  },

  // Mark all notifications as read
  markAllAsRead: async () => {
    const response = await api.put(API_ENDPOINTS.MARK_ALL_READ);
    return response.data;
  },
};

export default notificationService;
