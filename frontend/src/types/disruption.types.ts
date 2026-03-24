export type DisruptionSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type DisruptionStatus =
  | "DETECTED"
  | "ANALYZING"
  | "NOTIFYING"
  | "ACTIVE"
  | "RESOLVED"
  | "CANCELLED"
  | "PENDING";
export type DisruptionType =
  | "DELAY"
  | "CANCELLATION"
  | "CONGESTION"
  | "CONSTRUCTION"
  | "EVENT"
  | "ACCIDENT";
export type TransportMode = "BUS" | "TRAM" | "TRAIN" | "CAR" | "CYCLE";

export interface ActiveDisruption {
  id: number;
  name: string;
  description: string | null;
  status: DisruptionStatus;
  severity: DisruptionSeverity;
  disruptionType: DisruptionType;
  affectedTransportModes: TransportMode[] | null;
  affectedRoutes: string[] | null;
  affectedArea: string | null;
  latitude: number | null;
  longitude: number | null;
  detectedAt: string | null;
  estimatedEndTime: string | null;
  delayMinutes: number | null;
  notificationSent: boolean;
  createdAt: string;
  updatedAt: string;
}
