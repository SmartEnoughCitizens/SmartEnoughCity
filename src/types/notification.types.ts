export type NotificationType = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  userId?: string;
  metadata?: Record<string, unknown>;
}

export interface NotificationFilters {
  type?: NotificationType;
  read?: boolean;
  startDate?: string;
  endDate?: string;
}

export interface NotificationStats {
  total: number;
  unread: number;
  byType: Record<NotificationType, number>;
}
