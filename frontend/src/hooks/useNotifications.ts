import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect } from "react";
import { notificationApi } from "@/api";
import sseService from "@/services/sseService";
import { type Notification, type NotificationResponse } from "@/types";

export const NOTIFICATION_KEYS = {
  user: (userId: string, page = 0, size = 25) =>
    ["notifications", userId, page, size] as const,
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
  actionUrl?: string;
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
    actionUrl: raw.actionUrl,
  },
});

export const useUserNotifications = (
  userId: string,
  enabled: boolean = true,
  page = 0,
  size = 25,
) => {
  const queryClient = useQueryClient();

  const query = useQuery<NotificationResponse>({
    queryKey: NOTIFICATION_KEYS.user(userId, page, size),
    queryFn: async () => {
      const data = await notificationApi.getUserNotifications(
        userId,
        page,
        size,
      );
      const rawItems: RawNotification[] = Array.isArray(data)
        ? (data as RawNotification[])
        : data.notifications || [];
      const notifications = rawItems.map((item, index) =>
        toFrontendNotification(item, index),
      );
      const unreadCount = Array.isArray(data)
        ? notifications.filter((n) => !n.read).length
        : (data.totalCount ?? 0);
      return {
        userId,
        notifications,
        totalCount: unreadCount,
        totalItems: Array.isArray(data) ? undefined : data.totalItems,
        page: Array.isArray(data) ? page : (data.page ?? page),
        pageSize: Array.isArray(data) ? size : (data.pageSize ?? size),
      };
    },
    enabled: !!userId && enabled,
    staleTime: 60_000,
  });

  // Subscribe to SSE to push new notifications into React Query cache (page 0 only)
  // (SSE connection is managed by DashboardLayout)
  useEffect(() => {
    if (!userId || !enabled) return;

    const unsubscribe = sseService.subscribe((notification) => {
      queryClient.setQueryData<NotificationResponse>(
        NOTIFICATION_KEYS.user(userId, 0, size),
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
    // Optimistic update — mark all items read AND reset unread count
    queryClient.setQueriesData<NotificationResponse>(
      { queryKey: ["notifications", userId] },
      (old) => {
        if (!old) return old;
        return {
          ...old,
          totalCount: 0,
          notifications: old.notifications.map((n) => ({ ...n, read: true })),
        };
      },
    );

    // Backend call — invalidate all pages on both success and error to sync
    notificationApi
      .markAllAsRead(userId)
      .then(() => {
        queryClient.invalidateQueries({ queryKey: ["notifications", userId] });
      })
      .catch(() => {
        queryClient.invalidateQueries({ queryKey: ["notifications", userId] });
      });
  }, [userId, queryClient]);
};

export const useNotificationBin = (userId: string, enabled: boolean = true) => {
  return useQuery<NotificationResponse>({
    queryKey: ["notifications-bin", userId],
    queryFn: async () => {
      const data = await notificationApi.getBin(userId);
      const rawItems: RawNotification[] = Array.isArray(data)
        ? (data as RawNotification[])
        : data.notifications || [];
      return {
        userId,
        notifications: rawItems.map((item, i) =>
          toFrontendNotification(item, i),
        ),
        totalCount: rawItems.length,
      };
    },
    enabled: !!userId && enabled,
    staleTime: 60_000,
  });
};

export const useSoftDeleteNotification = (userId: string) => {
  const queryClient = useQueryClient();
  return useCallback(
    (notificationId: string) => {
      // Optimistic: remove from inbox across all cached pages
      queryClient.setQueriesData<NotificationResponse>(
        { queryKey: ["notifications", userId] },
        (old) => {
          if (!old) return old;
          const removed = old.notifications.find(
            (n) => n.id === notificationId,
          );
          const wasUnread = removed && !removed.read;
          return {
            ...old,
            totalCount: wasUnread
              ? Math.max(0, (old.totalCount ?? 0) - 1)
              : (old.totalCount ?? 0),
            totalItems: Math.max(0, (old.totalItems ?? 0) - 1),
            notifications: old.notifications.filter(
              (n) => n.id !== notificationId,
            ),
          };
        },
      );
      notificationApi
        .softDelete(userId, notificationId)
        .then(() => {
          queryClient.invalidateQueries({
            queryKey: ["notifications-bin", userId],
          });
        })
        .catch(() => {
          queryClient.invalidateQueries({
            queryKey: NOTIFICATION_KEYS.user(userId),
          });
        });
    },
    [userId, queryClient],
  );
};

export const useRestoreNotification = (userId: string) => {
  const queryClient = useQueryClient();
  return useCallback(
    (notificationId: string) => {
      notificationApi
        .restore(userId, notificationId)
        .then(() => {
          queryClient.invalidateQueries({
            queryKey: NOTIFICATION_KEYS.user(userId),
          });
          queryClient.invalidateQueries({
            queryKey: ["notifications-bin", userId],
          });
        })
        .catch(() => {});
    },
    [userId, queryClient],
  );
};

export const useSetReadState = (userId: string) => {
  const queryClient = useQueryClient();

  return useCallback(
    (notificationId: string, read: boolean) => {
      // Optimistic update across all cached pages — also adjust totalCount for badge
      queryClient.setQueriesData<NotificationResponse>(
        { queryKey: ["notifications", userId] },
        (old) => {
          if (!old) return old;
          const prev = old.notifications.find((n) => n.id === notificationId);
          const wasUnread = prev && !prev.read;
          const delta = read ? (wasUnread ? -1 : 0) : wasUnread ? 0 : 1;
          return {
            ...old,
            totalCount: Math.max(0, (old.totalCount ?? 0) + delta),
            notifications: old.notifications.map((n) =>
              n.id === notificationId ? { ...n, read } : n,
            ),
          };
        },
      );
      notificationApi.setReadState(userId, notificationId, read).catch(() => {
        queryClient.invalidateQueries({ queryKey: ["notifications", userId] });
      });
    },
    [userId, queryClient],
  );
};
