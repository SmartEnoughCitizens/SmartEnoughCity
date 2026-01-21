/**
 * Notification types matching backend DTOs
 */

export enum NotificationType {
  ROUTE_RECOMMENDATION = 'ROUTE_RECOMMENDATION',
  ALERT = 'ALERT',
  UPDATE = 'UPDATE',
  SYSTEM = 'SYSTEM',
}

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

export interface Notification {
  id: string;
  type: NotificationType;
  message: string;
  priority: Priority;
  timestamp: string;
  metadata?: Record<string, any>;
  read: boolean;
}

export interface NotificationResponse {
  userId: string;
  notifications: Notification[];
  totalCount: number;
}
