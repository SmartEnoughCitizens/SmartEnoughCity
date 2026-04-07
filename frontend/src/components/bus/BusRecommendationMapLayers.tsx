import { useEffect, useMemo } from "react";
import { CircleMarker, Polyline, Popup, useMap } from "react-leaflet";
import L from "leaflet";
import type { BusNewStopRecommendation, BusRouteDetail } from "@/types";

function useRecommendationMapGeometry(
  recommendation: BusNewStopRecommendation | null,
  routeDetail: BusRouteDetail | undefined,
) {
  const candidatePosition: [number, number] | null = recommendation
    ? [recommendation.candidateLat, recommendation.candidateLon]
    : null;

  const polylinePositions = useMemo((): [number, number][] | null => {
    if (!routeDetail?.shape?.length) return null;
    return routeDetail.shape.map((p) => [p.lat, p.lon]);
  }, [routeDetail]);

  const stopsWithCoords = useMemo(() => {
    if (!routeDetail?.stops?.length) return [];
    return routeDetail.stops.filter(
      (s): s is typeof s & { lat: number; lon: number } =>
        s.lat != null && s.lon != null,
    );
  }, [routeDetail]);

  return { candidatePosition, polylinePositions, stopsWithCoords };
}

/** Fits map to route shape, stops, and candidate when a recommendation is active. */
export function BusRecommendationFitBounds({
  recommendation,
  routeDetail,
}: {
  recommendation: BusNewStopRecommendation | null;
  routeDetail: BusRouteDetail | undefined;
}) {
  const map = useMap();
  const { candidatePosition, polylinePositions, stopsWithCoords } =
    useRecommendationMapGeometry(recommendation, routeDetail);

  useEffect(() => {
    if (!recommendation || !candidatePosition) return;

    const points: L.LatLngExpression[] = [];
    polylinePositions?.forEach((pt) => points.push(pt));
    stopsWithCoords.forEach((s) => points.push([s.lat, s.lon]));
    points.push(candidatePosition);

    if (points.length === 1) {
      map.setView(candidatePosition, 15);
      return;
    }

    const b = L.latLngBounds(points);
    map.fitBounds(b, { padding: [52, 52], maxZoom: 16 });
  }, [
    map,
    recommendation,
    candidatePosition,
    polylinePositions,
    stopsWithCoords,
  ]);

  return null;
}

/** Route polyline + stop markers (rendered under live vehicles). */
export function BusRecommendationPolylineAndStops({
  routeDetail,
}: {
  routeDetail: BusRouteDetail | undefined;
}) {
  const polylinePositions = useMemo((): [number, number][] | null => {
    if (!routeDetail?.shape?.length) return null;
    return routeDetail.shape.map((p) => [p.lat, p.lon]);
  }, [routeDetail]);

  const stopsWithCoords = useMemo(() => {
    if (!routeDetail?.stops?.length) return [];
    return routeDetail.stops.filter(
      (s): s is typeof s & { lat: number; lon: number } =>
        s.lat != null && s.lon != null,
    );
  }, [routeDetail]);

  return (
    <>
      {polylinePositions && polylinePositions.length >= 2 && (
        <Polyline
          positions={polylinePositions}
          pathOptions={{
            color: "#2563eb",
            weight: 5,
            opacity: 0.92,
            lineJoin: "round",
            lineCap: "round",
          }}
        />
      )}
      {stopsWithCoords.map((s) => (
        <CircleMarker
          key={`${s.stopId}-${s.sequence}`}
          center={[s.lat, s.lon]}
          radius={5}
          pathOptions={{
            color: "#ffffff",
            weight: 2,
            fillColor: "#475569",
            fillOpacity: 0.95,
          }}
        >
          <Popup>
            <strong>{s.name ?? s.stopId}</strong>
            {s.code != null && (
              <>
                <br />
                Code {s.code}
              </>
            )}
          </Popup>
        </CircleMarker>
      ))}
    </>
  );
}

/** Highlighted suggested new stop — render after live vehicles so it stays visible. */
export function BusRecommendationCandidate({
  recommendation,
}: {
  recommendation: BusNewStopRecommendation;
}) {
  const center: [number, number] = [
    recommendation.candidateLat,
    recommendation.candidateLon,
  ];

  return (
    <>
      <CircleMarker
        center={center}
        radius={26}
        pathOptions={{
          color: "#f97316",
          weight: 2,
          fillColor: "#f97316",
          fillOpacity: 0.22,
        }}
      />
      <CircleMarker
        center={center}
        radius={15}
        pathOptions={{
          color: "#ffffff",
          weight: 4,
          fillColor: "#ea580c",
          fillOpacity: 1,
        }}
      >
        <Popup>
          <strong>Suggested new stop</strong>
          <br />
          Score {recommendation.combinedScore.toFixed(2)}
          <br />
          Between {recommendation.stopA.name} → {recommendation.stopB.name}
        </Popup>
      </CircleMarker>
    </>
  );
}
