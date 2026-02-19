import { API_CONFIG, API_ENDPOINTS } from '@/config/api.config';

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

  connect(): void {
    // Already connected or connecting
    if (this.eventSource && this.eventSource.readyState !== EventSource.CLOSED) {
      return;
    }

    const url = `${API_CONFIG.BASE_URL}${API_ENDPOINTS.NOTIFICATIONS_STREAM}`;
    console.log('Connecting to SSE:', url);

    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      try {
        const notification: SSENotification = JSON.parse(event.data);
        console.log('Notification received:', notification);
        this.listeners.forEach(cb => cb(notification));
      } catch (error) {
        console.error('Error parsing SSE notification:', error);
      }
    });

    this.eventSource.onopen = () => {
      console.log('SSE Connected');
    };

    // Let EventSource handle reconnection automatically — just log the error
    this.eventSource.onerror = () => {
      console.warn('SSE connection error — browser will auto-reconnect');
    };
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  subscribe(callback: NotificationCallback): () => void {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback);
    };
  }
}

const sseService = new SSEService();
export default sseService;

