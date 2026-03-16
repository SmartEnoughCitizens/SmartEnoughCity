import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect } from "react";
import { notificationApi } from "@/api";
import sseService from "@/services/sseService";
import { type Notification, type NotificationResponse } from "@/types";

export const NOTIFICATION_KEYS = {
  user: (userId: string) => ["notifications", userId] as const,
};

interface RawNotification {
  id?: string;
  type?: string;
  message?: string;
  subject?: string;
  body?: string;
  priority?: string;
  timestamp?: string;
  read?: boolean;
  metadata?: Record<string, unknown>;
  qrCode?: string;
  channel?: string;
  recipient?: string;
}

const toFrontendNotification = (
  raw: RawNotification,
  index: number,
): Notification => ({
  id: raw.id || String(Date.now() + index),
  message: raw.message || `${raw.subject || ""}: ${raw.body || ""}`,
  timestamp: raw.timestamp || new Date().toISOString(),
  read: raw.read ?? false,
  metadata: raw.metadata || {
    subject: raw.subject,
    body: raw.body,
    qrCode: raw.qrCode,
    channel: raw.channel,
    recipient: raw.recipient,
  },
});

export const useUserNotifications = (
  userId: string,
  enabled: boolean = true,
) => {
  const queryClient = useQueryClient();

  const query = useQuery<NotificationResponse>({
    queryKey: NOTIFICATION_KEYS.user(userId),
    queryFn: async () => {
      const data = await notificationApi.getUserNotifications(userId);
      const rawItems: RawNotification[] = Array.isArray(data)
        ? (data as RawNotification[])
        : data.notifications || [];
      const notifications = rawItems.map((item, index) =>
        toFrontendNotification(item, index),
      );
      return { userId, notifications, totalCount: notifications.length };
    },
    enabled: !!userId && enabled,
    staleTime: 60_000,
  });

  // Subscribe to SSE to push new notifications into React Query cache
  // (SSE connection is managed by DashboardLayout)
  useEffect(() => {
    if (!userId || !enabled) return;

    const unsubscribe = sseService.subscribe((notification) => {
      queryClient.setQueryData<NotificationResponse>(
        NOTIFICATION_KEYS.user(userId),
        (old) => {
          const existing: NotificationResponse = old?.notifications
            ? old
            : { userId, notifications: [], totalCount: 0 };

          const newNotification = toFrontendNotification(notification, 0);

          return {
            userId: existing.userId,
            notifications: [newNotification, ...existing.notifications],
            totalCount: existing.totalCount + 1,
          };
        },
      );
    });

    return () => {
      unsubscribe();
    };
  }, [userId, enabled, queryClient]);

  return query;
};

export const useMarkAllAsRead = (userId: string) => {
  const queryClient = useQueryClient();

  return useCallback(() => {
    queryClient.setQueryData<NotificationResponse>(
      NOTIFICATION_KEYS.user(userId),
      (old) => {
        if (!old) return old;
        return {
          ...old,
          notifications: old.notifications.map((n) => ({ ...n, read: true })),
        };
      },
    );
  }, [userId, queryClient]);
};

export const useSetReadState = (userId: string) => {
  const queryClient = useQueryClient();

  return useCallback(
    (notificationId: string, read: boolean) => {
      queryClient.setQueryData<NotificationResponse>(
        NOTIFICATION_KEYS.user(userId),
        (old) => {
          if (!old) return old;
          return {
            ...old,
            notifications: old.notifications.map((n) =>
              n.id === notificationId ? { ...n, read } : n,
            ),
          };
        },
      );
    },
    [userId, queryClient],
  );
};

/**
 * useDismissNotification — write-permission mutation
 *
 * Optimistically removes the notification from the React Query cache
 * and calls the backend DELETE endpoint. On failure the cache is rolled back.
 *
 * Frontend usage must be gated by `usePermissions().canWrite`
 * (or <PermissionGate require="write">).
 */
export const useDismissNotification = (userId: string) => {
  const queryClient = useQueryClient();

  return useMutation<void, Error, string>({
    mutationFn: (notificationId: string) =>
      notificationApi.dismissNotification(userId, notificationId),

    // Optimistic update — remove from list immediately
    onMutate: async (notificationId) => {
      await queryClient.cancelQueries({
        queryKey: NOTIFICATION_KEYS.user(userId),
      });

      const previous = queryClient.getQueryData<NotificationResponse>(
        NOTIFICATION_KEYS.user(userId),
      );

      queryClient.setQueryData<NotificationResponse>(
        NOTIFICATION_KEYS.user(userId),
        (old) => {
          if (!old) return old;
          const notifications = old.notifications.filter(
            (n) => n.id !== notificationId,
          );
          return { ...old, notifications, totalCount: notifications.length };
        },
      );

      return { previous };
    },

    // Roll back on error
    onError: (_err, _id, context) => {
      const ctx = context as { previous?: NotificationResponse } | undefined;
      if (ctx?.previous) {
        queryClient.setQueryData(NOTIFICATION_KEYS.user(userId), ctx.previous);
      }
    },
  });
};
