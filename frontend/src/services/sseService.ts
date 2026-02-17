// frontend/src/services/sseService.ts

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
  private reconnectAttempts: number = 0;
  private readonly maxReconnectAttempts: number = 5;
  private readonly reconnectDelay: number = 3000;

  connect(): void {
    if (this.eventSource) {
      console.log('SSE already connected');
      return;
    }

    const url = 'http://localhost:8080/notification/v1/notifications/stream';

    console.log('🔌 Connecting to SSE:', url);

    this.eventSource = new EventSource(url, {
      withCredentials: true,
    });

    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      try {
        const notification: SSENotification = JSON.parse(event.data);
        console.log('📬 Notification received:', notification);

        this.listeners.forEach(callback => callback(notification));
        this.showBrowserNotification(notification);
        this.reconnectAttempts = 0;
      } catch (error) {
        console.error('❌ Error parsing notification:', error);
      }
    });

    this.eventSource.onopen = () => {
      console.log('✅ SSE Connected');
      this.reconnectAttempts = 0;
    };

    this.eventSource.onerror = (error: Event) => {
      console.error('❌ SSE Error:', error);
      this.handleReconnect();
    };
  }

  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      console.log('⏹️ SSE disconnected');
    }
  }

  subscribe(callback: NotificationCallback): () => void {
    this.listeners.push(callback);

    return () => {
      this.listeners = this.listeners.filter(cb => cb !== callback);
    };
  }

  private handleReconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('❌ Max SSE reconnection attempts reached');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts;

    console.log(`🔄 Reconnecting SSE in ${delay}ms (attempt ${this.reconnectAttempts})`);

    setTimeout(() => {
      this.connect();
    }, delay);
  }

  private showBrowserNotification(notification: SSENotification): void {
    if (!('Notification' in window)) return;

    if (Notification.permission === 'granted') {
      new Notification(notification.subject || 'New Notification', {
        body: notification.body || '',
        icon: '/notification-icon.png',
      });
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          this.showBrowserNotification(notification);
        }
      });
    }
  }

  isConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN;
  }
}

// Export singleton instance as default
const sseService = new SSEService();
export default sseService;