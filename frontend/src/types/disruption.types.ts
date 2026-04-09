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
  googleMapsWalkingUrl: string | null;
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

/** A city event (concert, match, festival, etc.) returned by the events API. */
export interface EventItem {
  id: number;
  eventName: string;
  eventType: string;
  eventDate: string; // ISO date string, e.g. "2025-06-01"
  startTime: string | null;
  endTime: string | null;
  venueName: string;
  venueCapacity: number | null;
  latitude: number | null;
  longitude: number | null;
  estimatedAttendance: number | null;
  /** Pre-computed from venue capacity by the backend: CRITICAL / HIGH / MEDIUM / LOW */
  riskLevel: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
}
