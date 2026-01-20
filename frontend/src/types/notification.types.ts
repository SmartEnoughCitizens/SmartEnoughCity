/**
 * Notification types matching backend DTOs
 */

export const NotificationType = {
  ROUTE_RECOMMENDATION: 'ROUTE_RECOMMENDATION',
  ALERT: 'ALERT',
  UPDATE: 'UPDATE',
  SYSTEM: 'SYSTEM',
} as const;

export type NotificationType = (typeof NotificationType)[keyof typeof NotificationType];

export const Priority = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  URGENT: 'URGENT',
} as const;

export type Priority = (typeof Priority)[keyof typeof Priority];

export interface Notification {
  id: string;
  type: NotificationType;
  message: string;
  priority: Priority;
  timestamp: string;
  metadata?: Record<string, unknown>;
  read: boolean;
}

export interface NotificationResponse {
  userId: string;
  notifications: Notification[];
  totalCount: number;
}
