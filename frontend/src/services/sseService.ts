import { API_ENDPOINTS } from "@/config/api.config";

type SSENotification = {
  subject: string;
  body: string;
  recipient?: string;
  channel?: string;
  qrCode?: string;
};

type NotificationCallback = (notification: SSENotification) => void;

class SSEService {
  private eventSource: EventSource | null = null;
  private listeners: NotificationCallback[] = [];
  private connectedUserId: string | null = null;

  connect(userId: string): void {
    // Already connected for this user
    if (
      this.eventSource &&
      this.eventSource.readyState !== EventSource.CLOSED &&
      this.connectedUserId === userId
    ) {
      return;
    }

    // Different user or stale connection — disconnect first
    this.disconnect();

    const url = `${API_ENDPOINTS.NOTIFICATIONS_STREAM}?userId=${encodeURIComponent(userId)}`;
    console.log("Connecting to SSE:", url);

    this.connectedUserId = userId;
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener("notification", (event: MessageEvent) => {
      try {
        const notification: SSENotification = JSON.parse(event.data);
        console.log("Notification received:", notification);
        for (const cb of this.listeners) {
          cb(notification);
        }
      } catch (error) {
        console.error("Error parsing SSE notification:", error);
      }
    });

    this.eventSource.addEventListener("open", () => {
      console.log("SSE Connected");
    });

    // Let EventSource handle reconnection automatically — just log the error
    this.eventSource.addEventListener("error", () => {
      console.warn("SSE connection error — browser will auto-reconnect");
    });
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.connectedUserId = null;
  }

  subscribe(callback: NotificationCallback): () => void {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter((cb) => cb !== callback);
    };
  }
}

const sseService = new SSEService();
export default sseService;
