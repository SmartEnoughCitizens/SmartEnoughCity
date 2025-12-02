export type TimeRange = '24h' | '7d' | '30d';

export type TransportMode = 'car' | 'cycle' | 'bus' | 'train' | 'tram' | 'pedestrian';

export interface IndicatorData {
  timestamp: string;
  value: number;
  label?: string;
}

export interface TransportIndicatorData {
  mode: TransportMode;
  currentValue: number;
  change: number;
  changePercentage: number;
  historicalData: IndicatorData[];
  metrics: TransportMetrics;
}

export interface TransportMetrics {
  avgSpeed?: number;
  totalVehicles?: number;
  peakHour?: string;
  efficiency?: number;
  carbonEmissions?: number;
}

export interface EventIndicator {
  id: string;
  name: string;
  type: string;
  location: string;
  startDate: string;
  endDate: string;
  expectedImpact: 'low' | 'medium' | 'high';
  status: 'planned' | 'active' | 'completed';
  affectedRoutes?: string[];
}

export interface ConstructionIndicator {
  id: string;
  projectName: string;
  location: string;
  startDate: string;
  estimatedEndDate: string;
  progress: number;
  impact: 'low' | 'medium' | 'high';
  affectedAreas: string[];
  status: 'planned' | 'in-progress' | 'completed';
}

export interface IndicatorFilters {
  timeRange?: TimeRange;
  mode?: TransportMode;
  dateRange?: {
    start: string;
    end: string;
  };
}
