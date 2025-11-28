export interface SystemStatus {
  status: 'operational' | 'degraded' | 'offline';
  uptime: number; // in seconds
  lastUpdated: string;
  services: ServiceStatus[];
  metrics: SystemMetrics;
}

export interface ServiceStatus {
  name: string;
  status: 'up' | 'down' | 'maintenance';
  responseTime?: number; // in ms
  lastChecked: string;
  errorRate?: number; // percentage
}

export interface SystemMetrics {
  cpuUsage: number; // percentage
  memoryUsage: number; // percentage
  diskUsage: number; // percentage
  activeConnections: number;
  requestsPerMinute: number;
  averageResponseTime: number; // in ms
}

export interface SystemHealth {
  overall: 'healthy' | 'warning' | 'critical';
  checks: HealthCheck[];
  timestamp: string;
}

export interface HealthCheck {
  name: string;
  status: 'pass' | 'fail' | 'warn';
  message?: string;
  timestamp: string;
}
