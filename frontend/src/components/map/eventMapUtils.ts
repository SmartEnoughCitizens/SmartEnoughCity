/**
 * Shared types, constants, and helpers for the event map
 */

import type { DisruptionItem, EventItem } from "@/types";

export type EventCategory = "construction" | "public" | "emergency";
export type EventSeverity = "high" | "medium" | "low";

export type SelectedMapItem =
  | { kind: "event"; item: EventItem }
  | { kind: "disruption"; item: DisruptionItem };

export const CATEGORY_EMOJI: Record<EventCategory, string> = {
  construction: "🚧",
  public: "🎉",
  emergency: "🚨",
};

export const CATEGORY_LABEL: Record<EventCategory, string> = {
  construction: "Construction",
  public: "Public",
  emergency: "Emergency",
};

export const SEVERITY_COLORS: Record<EventSeverity, string> = {
  high: "#EF4444",
  medium: "#F59E0B",
  low: "#10B981",
};

export function getEventCategory(eventType: string): EventCategory {
  const t = eventType.toLowerCase();
  if (t.includes("construction") || t.includes("roadwork"))
    return "construction";
  if (t.includes("emergency") || t.includes("incident") || t.includes("alert"))
    return "emergency";
  return "public";
}

export function getDisruptionCategory(
  disruptionType: DisruptionItem["disruptionType"],
): EventCategory {
  if (disruptionType === "CONSTRUCTION") return "construction";
  if (
    disruptionType === "ACCIDENT" ||
    disruptionType === "CANCELLATION" ||
    disruptionType === "DELAY"
  )
    return "emergency";
  if (disruptionType === "CONGESTION") return "emergency";
  return "public";
}

export function getDisruptionSeverity(
  severity: DisruptionItem["severity"],
): EventSeverity {
  if (severity === "CRITICAL" || severity === "HIGH") return "high";
  if (severity === "MEDIUM") return "medium";
  return "low";
}

export function getEventSeverity(event: EventItem): EventSeverity {
  const att = event.estimatedAttendance ?? 0;
  if (att > 5000) return "high";
  if (att > 1000) return "medium";
  return "low";
}
