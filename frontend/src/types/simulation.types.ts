export interface Simulation {
  id: string;
  name: string;
  description: string;
  scenario: SimulationScenario;
  parameters: SimulationParameters;
  status: SimulationStatus;
  createdBy: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  results?: SimulationResults;
}

export type SimulationStatus =
  | "pending"
  | "running"
  | "completed"
  | "failed"
  | "cancelled";

export type SimulationScenario =
  | "traffic-increase"
  | "event-impact"
  | "construction-impact"
  | "transit-optimization"
  | "emergency-response"
  | "custom";

export interface SimulationParameters {
  duration?: number; // in minutes
  trafficIncrease?: number; // percentage

  // Specific fields for the Thin Slice Demo
  transportMode?: "BUS" | "TRAIN" | "TRAM" | "CAR";
  modificationFactor?: number;

  affectedRoutes?: string[];
  weatherConditions?: "clear" | "rain" | "snow" | "fog";
  timeOfDay?: "morning" | "midday" | "evening" | "night";
  customParams?: Record<string, unknown>;
}

export interface SimulationResults {
  summary: {
    avgSpeed: number;
    congestionLevel: number;
    totalDelay: number; // in minutes
    affectedVehicles: number;
  };
  recommendations: string[];
  visualizations?: {
    heatmapData?: unknown[];
    timeSeriesData?: unknown[];
  };
}

export interface CreateSimulationRequest {
  name: string;
  description: string;
  scenario: SimulationScenario;
  parameters: SimulationParameters;
}
