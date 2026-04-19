export interface Notification {
  id: string;
  message: string;
  timestamp: string;
  read: boolean;
  metadata?: Record<string, unknown>;
}

export interface NotificationResponse {
  userId: string;
  notifications: Notification[];
  totalCount: number; // unread count (badge)
  totalItems?: number; // total non-deleted (pagination)
  page?: number;
  pageSize?: number;
}
