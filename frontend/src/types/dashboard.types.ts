export interface DashboardStats {
  totalVehicles: number;
  activeRoutes: number;
  avgSpeed: number;
  congestionLevel: string;
  congestionScore?: number;
  co2Emissions?: number;
  publicTransitUsage?: number;
}

export interface DashboardOverview {
  stats: DashboardStats;
  recentAlerts: Alert[];
  trends: TrendData[];
}

export interface Alert {
  id: string;
  type: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';
  message: string;
  timestamp: string;
  severity?: 'low' | 'medium' | 'high';
}

export interface TrendData {
  timestamp: string;
  value: number;
  category: string;
}
