export type DisruptionType =
  | "DELAY"
  | "CANCELLATION"
  | "CONGESTION"
  | "CONSTRUCTION"
  | "EVENT"
  | "ACCIDENT";

export type DisruptionSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export type DisruptionStatus =
  | "DETECTED"
  | "ANALYZING"
  | "RESOLVED"
  | "CANCELLED";

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
  affectedTransportModes: string[] | null;
  detectedAt: string | null;
  startTime: string | null;
  estimatedEndTime: string | null;
  constructionDetails: string | null;
  delayMinutes: number | null;
}
