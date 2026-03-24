export interface EventItem {
  id: number;
  eventName: string;
  eventType: string;
  venueName: string;
  latitude: number;
  longitude: number;
  eventDate: string;
  startTime: string;
  endTime: string | null;
  estimatedAttendance: number | null;
}

export interface PedestrianLive {
  siteId: number;
  siteName: string;
  lat: number;
  lon: number;
  totalCount: number;
  lastUpdated: string | null;
}
