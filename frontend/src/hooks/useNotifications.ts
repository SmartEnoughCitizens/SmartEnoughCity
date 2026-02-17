/**
 * React Query hooks for notifications with SSE real-time updates
 */

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import { notificationApi } from "@/api";
import sseService from "@/services/sseService";
import { Notification, NotificationType, Priority } from "@/types/notification";

export const NOTIFICATION_KEYS = {
  user: (userId: string) => ["notifications", userId] as const,
};

/**
 * Get user notifications with SSE real-time updates
 */
export const useUserNotifications = (
  userId: string,
  enabled: boolean = true,
) => {
  const queryClient = useQueryClient();
  const hasConnectedRef = useRef(false);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  // Initial fetch from API
  const query = useQuery({
    queryKey: NOTIFICATION_KEYS.user(userId),
    queryFn: () => notificationApi.getUserNotifications(userId),
    enabled: !!userId && enabled,
    staleTime: 60_000,
  });

  // Connect to SSE for real-time updates (only once!)
  useEffect(() => {
    if (!userId || !enabled) return;
    if (hasConnectedRef.current) {
      console.log('⚠️ Already connected to SSE, skipping...');
      return;
    }

    console.log('🚀 Connecting to SSE for user:', userId);
    hasConnectedRef.current = true;

    // Connect to SSE
    sseService.connect();

    // Subscribe to new notifications from SSE
    unsubscribeRef.current = sseService.subscribe((notification: any) => {
      console.log('📨 New notification via SSE:', notification);

      // Add new notification to React Query cache
      queryClient.setQueryData(
        NOTIFICATION_KEYS.user(userId),
        (old: any) => {
          if (!old) return old;

          // Convert backend SSE notification to your Notification type
          const newNotification: Notification = {
            id: String(Date.now()),
            type: "ALERT" as NotificationType,
            message: `${notification.subject}: ${notification.body}`,
            priority: "MEDIUM" as Priority,
            timestamp: new Date().toISOString(),
            read: false,
            metadata: {
              subject: notification.subject,
              body: notification.body,
              qrCode: notification.qrCode,
              channel: notification.channel,
              recipient: notification.recipient,
            },
          };

          return {
            userId: old.userId,
            notifications: [newNotification, ...old.notifications],
            totalCount: old.totalCount + 1,
          };
        }
      );
    });

    // Cleanup function - only unsubscribe, DON'T disconnect
    return () => {
      console.log('🧹 Cleaning up SSE subscription (keeping connection alive)');
      if (unsubscribeRef.current) {
        unsubscribeRef.current();
        unsubscribeRef.current = null;
      }
      // Don't call sseService.disconnect() here!
      // The connection stays alive for the entire session
    };
  }, [userId, enabled]); // Removed queryClient from dependencies

  return query;
};