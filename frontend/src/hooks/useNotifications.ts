import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { notificationApi } from "@/api";
import sseService from "@/services/sseService";
import {
  type Notification,
  type NotificationResponse,
  NotificationType,
  Priority,
} from "@/types";

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
  type: (raw.type as NotificationType) || "ALERT",
  message: raw.message || `${raw.subject || ""}: ${raw.body || ""}`,
  priority: (raw.priority as Priority) || "MEDIUM",
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
