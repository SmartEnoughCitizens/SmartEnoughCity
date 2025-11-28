import { useQuery, useMutation, useQueryClient, UseQueryResult, UseMutationResult } from '@tanstack/react-query';
import { notificationService } from '../services/notificationService';
import { Notification } from '../types';

// Query keys
export const notificationKeys = {
  all: ['notifications'] as const,
  lists: () => [...notificationKeys.all, 'list'] as const,
};

// Hook for fetching notifications
export const useNotifications = (): UseQueryResult<Notification[], Error> => {
  return useQuery({
    queryKey: notificationKeys.lists(),
    queryFn: () => notificationService.getNotifications(),
    staleTime: 30000,
    refetchInterval: 60000, // Poll every minute
  });
};

// Hook for marking notification as read
export const useMarkNotificationRead = (): UseMutationResult<Notification, Error, string> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => notificationService.markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
    },
  });
};

// Hook for marking all notifications as read
export const useMarkAllNotificationsRead = (): UseMutationResult<void, Error, void> => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.lists() });
    },
  });
};
