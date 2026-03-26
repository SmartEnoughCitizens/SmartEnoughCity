/**
 * Fetches OSRM cycling routes for a list of OD pairs in batches.
 * Call this as early as possible so routes are ready by the time the map opens.
 */

import { useEffect, useRef, useState } from "react";
import type { StationODPairDTO } from "@/types";

export type LatLon = [number, number];
export type ODRouteCache = Map<string, LatLon[]>;

const OSRM_BASE = "https://router.project-osrm.org/route/v1/cycling";
const MAX_ROUTES = 25;
const CONCURRENT = 5;

interface OsrmResponse {
  code: string;
  routes?: Array<{ geometry: { coordinates: [number, number][] } }>;
}

async function fetchOne(
  pair: StationODPairDTO,
  signal: AbortSignal,
): Promise<{ key: string; coords: LatLon[] | null }> {
  const key = `${pair.originStationId}-${pair.destStationId}`;
  try {
    const url = `${OSRM_BASE}/${pair.originLon},${pair.originLat};${pair.destLon},${pair.destLat}?overview=full&geometries=geojson`;
    const res = await fetch(url, { signal });
    if (!res.ok) return { key, coords: null };
    const data = (await res.json()) as OsrmResponse;
    if (data.code !== "Ok" || !data.routes?.[0]) return { key, coords: null };
    const coords = data.routes[0].geometry.coordinates.map(
      ([lon, lat]) => [lat, lon] as LatLon,
    );
    return { key, coords };
  } catch {
    return { key, coords: null };
  }
}

export const useODRoutes = (odPairs: StationODPairDTO[]) => {
  const [routeCache, setRouteCache] = useState<ODRouteCache>(() => new Map());
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  // Track which set of pairs we've already fetched to avoid re-fetching on re-renders
  const fetchedKeyRef = useRef<string>("");

  useEffect(() => {
    const pairs = odPairs.slice(0, MAX_ROUTES);
    if (pairs.length === 0) return;

    // Stable key representing this exact pair list
    const batchKey = pairs.map((p) => `${p.originStationId}-${p.destStationId}`).join("|");
    if (batchKey === fetchedKeyRef.current) return; // already fetching/fetched this set
    fetchedKeyRef.current = batchKey;

    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const run = async () => {
      setRouteCache(new Map());
      setProgress({ done: 0, total: pairs.length });

      for (let i = 0; i < pairs.length; i += CONCURRENT) {
        if (controller.signal.aborted) return;
        const batch = pairs.slice(i, i + CONCURRENT);
        const results = await Promise.allSettled(batch.map((p) => fetchOne(p, controller.signal)));
        if (controller.signal.aborted) return;

        setRouteCache((prev) => {
          const next = new Map(prev);
          for (const r of results) {
            if (r.status === "fulfilled" && r.value.coords) {
              next.set(r.value.key, r.value.coords);
            }
          }
          return next;
        });
        setProgress({ done: Math.min(i + CONCURRENT, pairs.length), total: pairs.length });
      }
      setProgress(null);
    };

    run();
    return () => controller.abort();
  }, [odPairs]);

  return { routeCache, progress };
};
