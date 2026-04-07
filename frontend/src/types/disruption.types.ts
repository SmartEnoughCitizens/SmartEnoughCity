export type DisruptionType =
  | "DELAY"
  | "CANCELLATION"
  | "CONGESTION"
  | "CONSTRUCTION"
  | "EVENT"
  | "ACCIDENT"
  | "TRAM_DISRUPTION";

export interface DisruptionCause {
  id: number;
  causeType: "EVENT" | "CONGESTION" | "CROSS_MODE";
  causeDescription: string;
  confidence: "HIGH" | "MEDIUM" | "LOW";
}

export interface DisruptionAlternative {
  id: number;
  mode: string;
  description: string;
  etaMinutes: number | null;
  stopName: string | null;
  availabilityCount: number | null;
  lat: number | null;
  lon: number | null;
}

export type DisruptionSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type DisruptionStatus =
  | "DETECTED"
  | "ANALYZING"
  | "NOTIFYING"
  | "ACTIVE"
  | "RESOLVED"
  | "CANCELLED"
  | "PENDING";

export type TransportMode = "BUS" | "TRAM" | "TRAIN" | "CAR" | "CYCLE";

export interface DisruptionItem {
  id: number;
  name: string | null;
  description: string | null;
  status: DisruptionStatus;
  disruptionType: DisruptionType;
  severity: DisruptionSeverity;
  latitude: number | null;
  longitude: number | null;
  affectedArea: string | null;
  affectedTransportModes: TransportMode[] | null;
  affectedRoutes: string[] | null;
  detectedAt: string | null;
  startTime: string | null;
  estimatedEndTime: string | null;
  constructionDetails: string | null;
  delayMinutes: number | null;
  notificationSent: boolean | null;
  createdAt: string | null;
  updatedAt: string | null;
  causes?: DisruptionCause[];
  alternatives?: DisruptionAlternative[];
}

/** Alias kept for compatibility with main-branch imports */
export type ActiveDisruption = DisruptionItem;
