export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
  timestamp?: string;
}

export interface ApiError {
  message: string;
  code?: string;
  status?: number;
  details?: Record<string, unknown>;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export interface ChartDataPoint {
  x: string | number;
  y: number;
  label?: string;
  category?: string;
}

export interface MetricCardData {
  title: string;
  value: string | number;
  unit?: string;
  trend?: number; // percentage change
  trendDirection?: 'up' | 'down' | 'stable';
  icon?: React.ReactNode;
  color?: string;
}

export interface MapMarker {
  id: string;
  position: [number, number]; // [lat, lng]
  title: string;
  description?: string;
  type?: string;
  icon?: string;
}

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}
