/**
 * React Query hooks for notifications
 */

import { useQuery } from '@tanstack/react-query';
import { notificationApi } from '@/api';

export const NOTIFICATION_KEYS = {
  user: (userId: string) => ['notifications', userId] as const,
};

/**
 * Get user notifications
 */
export const useUserNotifications = (userId: string, enabled: boolean = true) => {
  return useQuery({
    queryKey: NOTIFICATION_KEYS.user(userId),
    queryFn: () => notificationApi.getUserNotifications(userId),
    enabled: !!userId && enabled,
    staleTime: 60_000, // 1 minute
    refetchInterval: 60_000, // Auto-refresh every minute
  });
};
