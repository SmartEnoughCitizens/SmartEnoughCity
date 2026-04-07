/**
 * Train dashboard — full-viewport map with floating right-side panel.
 * Layout and style matches the Cycle dashboard.
 */

import { useState, useEffect, useMemo, useCallback } from "react";
import { DisruptionsTabContent } from "@/components/disruption/DisruptionsTabContent";
import { useSearchParams } from "react-router-dom";
import {
  Box,
  Paper,
  Typography,
  CircularProgress,
  Alert,
  IconButton,
  Chip,
  Tab,
  Tabs,
  TextField,
  InputAdornment,
  ListItemButton,
  Button,
  Autocomplete,
  Slider,
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import DeleteIcon from "@mui/icons-material/Delete";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import TrainIcon from "@mui/icons-material/Train";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import SpeedIcon from "@mui/icons-material/Speed";
import SearchIcon from "@mui/icons-material/Search";
import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  Polyline,
  useMap,
} from "react-leaflet";
import {
  useTrainData,
  useTrainFrequentDelays,
  useTrainKpis,
  useTrainLiveTrains,
  useTrainRoutes,
  useTrainDemand,
  useSimulateTrainDemand,
} from "@/hooks";
import type { StationDemand } from "@/types";
import { useAppSelector } from "@/store/hooks";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { approvalApi, type CreateApprovalDTO } from "@/api/approval.api";
import "leaflet/dist/leaflet.css";
import L from "leaflet";

// ── Constants ────────────────────────────────────────────────────────

const DUBLIN_CENTER: [number, number] = [53.3498, -6.2603];
const PANEL_W = 420;
const GAP = 16;

type TypeKey = "ALL" | "D" | "S" | "M" | "U";

const TYPE_CONFIG: Record<
  TypeKey,
  { label: string; color: string; short: string }
> = {
  ALL: { label: "All", color: "#607D8B", short: "•" },
  D: { label: "DART", color: "#00ACC1", short: "D" },
  S: { label: "Suburban", color: "#1976D2", short: "S" },
  M: { label: "Mainline", color: "#7B1FA2", short: "M" },
  U: { label: "Unknown", color: "#F57C00", short: "?" },
};

// ── Helpers ──────────────────────────────────────────────────────────

/** Normalise live-train type strings → TypeKey (null = ALL for filter purposes) */
function getTrainTypeKey(trainType?: string | null): TypeKey {
  if (!trainType) return "ALL";
  const t = trainType.toUpperCase();
  if (t === "D" || t === "DART") return "D";
  if (t === "M" || t === "MAINLINE") return "M";
  if (t === "S" || t === "SUBURBAN") return "S";
  return "ALL";
}

/** Normalise station type strings → TypeKey (null = U so they render distinctly) */
function getStationTypeKey(stationType?: string | null): TypeKey {
  if (!stationType) return "U";
  const t = stationType.toUpperCase();
  if (t === "D" || t === "DART") return "D";
  if (t === "M" || t === "MAINLINE") return "M";
  if (t === "S" || t === "SUBURBAN") return "S";
  return "U";
}

function getDirectionArrow(direction?: string | null): string {
  if (!direction) return "";
  const d = direction.toLowerCase();
  if (d.includes("north")) return "↑";
  if (d.includes("south")) return "↓";
  if (d.includes("east")) return "→";
  if (d.includes("west")) return "←";
  return "";
}

const isRunning = (status?: string | null): boolean => {
  if (!status) return false;
  const s = status.toUpperCase();
  return s === "R" || s === "RUNNING";
};

// ── Route colour by GTFS short_name ─────────────────────────────────

function getRouteColor(shortName?: string | null): string {
  if (!shortName) return "#1565C0";
  const s = shortName.toUpperCase();
  if (s === "DART") return "#00ACC1";
  if (s === "COMMUTER") return "#1976D2";
  if (s === "INTERCITY") return "#7B1FA2";
  return "#455A64"; // generic rail
}

// ── Demand colour helpers ────────────────────────────────────────────

/** hue 120 (green) → 0 (red) based on normalised demand score 0–1 */
function demandColor(score: number): string {
  const hue = Math.round((1 - score) * 120);
  return `hsl(${hue}, 80%, 42%)`;
}

function makeDemandIcon(score: number, affected: boolean): L.DivIcon {
  const color = demandColor(score);
  const ring = affected ? `box-shadow:0 0 0 3px #fff,0 0 0 5px ${color};` : "";
  return L.divIcon({
    html: `<div style="width:20px;height:20px;border-radius:50%;background:${color};border:2px solid rgba(255,255,255,0.85);display:flex;align-items:center;justify-content:center;${ring}cursor:pointer;"></div>`,
    className: "",
    iconSize: [20, 20],
    iconAnchor: [10, 10],
    popupAnchor: [0, -14],
  });
}

// ── Leaflet icon factories ───────────────────────────────────────────

function makeStationIcon(type: string | null): L.DivIcon {
  const cfg = TYPE_CONFIG[(type as TypeKey) ?? "ALL"] ?? TYPE_CONFIG.ALL;
  return L.divIcon({
    html: `<div style="width:26px;height:26px;border-radius:50%;background:${cfg.color};display:flex;align-items:center;justify-content:center;font-size:10px;font-weight:800;color:#fff;box-shadow:0 2px 8px rgba(0,0,0,0.4),0 0 0 2px rgba(255,255,255,0.2);cursor:pointer;">${cfg.short}</div>`,
    className: "",
    iconSize: [26, 26],
    iconAnchor: [13, 13],
    popupAnchor: [0, -16],
  });
}

function makeLiveTrainIcon(
  trainType: string | null,
  direction: string | null,
  running: boolean,
): L.DivIcon {
  const typeKey = getTrainTypeKey(trainType);
  const color =
    typeKey === "ALL"
      ? running
        ? "#2ea043"
        : "#d29922"
      : TYPE_CONFIG[typeKey].color;
  const inner = getDirectionArrow(direction) || TYPE_CONFIG[typeKey].short;
  const pulse = running ? `animation:trainPulse 1.6s ease-out infinite;` : "";
  return L.divIcon({
    html: `<div style="width:22px;height:22px;border-radius:50%;background:${color};border:2px solid rgba(255,255,255,0.75);display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:900;color:#fff;box-shadow:0 2px 6px rgba(0,0,0,0.4);cursor:pointer;${pulse}">${inner}</div>`,
    className: "",
    iconSize: [22, 22],
    iconAnchor: [11, 11],
    popupAnchor: [0, -14],
  });
}

// ── Map fly-to controller ────────────────────────────────────────────

function MapController({
  target,
}: {
  target: { center: [number, number]; id: number } | null;
}) {
  const map = useMap();
  useEffect(() => {
    if (target)
      map.flyTo(target.center, 15, { duration: 1.2, easeLinearity: 0.25 });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);
  return null;
}

// ── KPI row ──────────────────────────────────────────────────────────

function KpiRow({
  icon,
  label,
  value,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  value: string | number;
  color?: string;
}) {
  return (
    <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, py: 0.5 }}>
      <Box
        sx={{ color: color ?? "primary.main", display: "flex", fontSize: 18 }}
      >
        {icon}
      </Box>
      <Box>
        <Typography
          variant="caption"
          sx={{
            color: "text.secondary",
            fontSize: "0.65rem",
            textTransform: "uppercase",
            letterSpacing: 0.5,
            display: "block",
          }}
        >
          {label}
        </Typography>
        <Typography variant="body2" fontWeight={700} sx={{ lineHeight: 1.1 }}>
          {value}
        </Typography>
      </Box>
    </Box>
  );
}

// ── Main Component ───────────────────────────────────────────────────

type CorridorEntry = {
  origin: StationDemand | null;
  dest: StationDemand | null;
  count: number;
};

const TAB_NAME_MAP: Record<string, number> = {
  stations: 0,
  trains: 1,
  delays: 2,
  demand: 3,
  approvals: 4,
};

export const TrainDashboard = () => {
  const [searchParams] = useSearchParams();
  const [panelOpen, setPanelOpen] = useState(true);
  const [tabValue, setTabValue] = useState(() => {
    const tab = searchParams.get("tab");
    return tab ? (TAB_NAME_MAP[tab.toLowerCase()] ?? 0) : 0;
  });

  // Consume Redux navigation requests targeting this dashboard
  const requestedNavigation = useAppSelector(
    (state) => state.ui.requestedNavigation,
  );
  useEffect(() => {
    if (!requestedNavigation || requestedNavigation.view !== "train") return;
    const tab = requestedNavigation.tab;
    if (tab) {
      const idx = TAB_NAME_MAP[tab.toLowerCase()];
      if (idx !== undefined) setTabValue(idx); // eslint-disable-line react-hooks/set-state-in-effect
    }
  }, [requestedNavigation]);

  const [typeFilter, setTypeFilter] = useState<TypeKey>("ALL");
  const [search, setSearch] = useState("");
  const [flyTarget, setFlyTarget] = useState<{
    center: [number, number];
    id: number;
  } | null>(null);
  const [selectedStationCode, setSelectedStationCode] = useState<string | null>(
    null,
  );
  const [selectedTrainCode, setSelectedTrainCode] = useState<string | null>(
    null,
  );
  const [selectedDisruptionId, setSelectedDisruptionId] = useState<
    number | null
  >(null);

  // Demand simulation state — up to 3 corridors
  const [corridors, setCorridors] = useState<CorridorEntry[]>([
    { origin: null, dest: null, count: 1 },
  ]);
  const [simResult, setSimResult] = useState<{
    simulatedDemand: StationDemand[];
    baseDemand: StationDemand[];
    affectedStopIds: string[];
  } | null>(null);

  const theme = useAppSelector((state) => state.ui.theme);
  const roles = useAppSelector((state) => state.auth.roles);
  const isTrainAdmin =
    roles.includes("Train_Admin") && !roles.includes("City_Manager");
  const isCityManager =
    roles.includes("City_Manager") || roles.includes("Government_Admin");

  // ── Approvals ────────────────────────────────────────────────────────
  const [reviewNote, setReviewNote] = useState("");
  const [reviewingId, setReviewingId] = useState<number | null>(null);
  const [pendingStatus, setPendingStatus] = useState<
    "APPROVED" | "DENIED" | null
  >(null);
  const queryClient = useQueryClient();

  const { data: approvals = [], isLoading: approvalsLoading } = useQuery({
    queryKey: ["approvals", "train"],
    queryFn: () => approvalApi.list("train"),
    enabled: tabValue === 4,
    refetchInterval: 30_000,
  });

  const submitApprovalMutation = useMutation({
    mutationFn: (dto: CreateApprovalDTO) => approvalApi.create(dto),
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["approvals", "train"] }),
  });

  const reviewMutation = useMutation({
    mutationFn: ({
      id,
      status,
    }: {
      id: number;
      status: "APPROVED" | "DENIED";
    }) =>
      approvalApi.review(id, { status, reviewNote: reviewNote || undefined }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["approvals", "train"] });
      setReviewingId(null);
      setReviewNote("");
      setPendingStatus(null);
    },
  });
  const { data: trainData, isLoading: dataLoading, error } = useTrainData(500);
  const { data: kpiData } = useTrainKpis();
  const { data: liveTrains = [] } = useTrainLiveTrains();
  const { data: frequentDelays = [], isLoading: delaysLoading } =
    useTrainFrequentDelays();
  const { data: trainRoutes = [] } = useTrainRoutes();
  const { data: demandData = [] } = useTrainDemand();
  const simulateMutation = useSimulateTrainDemand();

  // Lookup map: stopId → demand score (uses simulated when available)
  const activeDemand: StationDemand[] = useMemo(() => {
    const base = simResult?.simulatedDemand ?? demandData;
    if (typeFilter === "ALL") return base;
    return base.filter((d) => getStationTypeKey(d.stationType) === typeFilter);
  }, [simResult, demandData, typeFilter]);
  const demandByStop = useMemo(
    () => new Map(activeDemand.map((d) => [d.stopId, d])),
    [activeDemand],
  );
  const affectedSet = useMemo(
    () => new Set(simResult?.affectedStopIds),
    [simResult],
  );

  // Compute summary metrics comparing base vs simulated for affected stops
  const simMetrics = useMemo(() => {
    if (!simResult || simResult.affectedStopIds.length === 0) return null;
    const baseMap = new Map(simResult.baseDemand.map((d) => [d.stopId, d]));
    const affected = simResult.affectedStopIds;
    let totalRelativeRelief = 0;
    let totalDemandReduction = 0;
    let peakStop: StationDemand | null = null;
    let peakRelief = 0;
    let validCount = 0;
    for (const id of affected) {
      const base = baseMap.get(id);
      const sim = simResult.simulatedDemand.find((d) => d.stopId === id);
      if (!base || !sim) continue;
      if (base.normPressure > 0) {
        const relief =
          ((base.normPressure - sim.normPressure) / base.normPressure) * 100;
        totalRelativeRelief += relief;
        if (relief > peakRelief) {
          peakRelief = relief;
          peakStop = sim;
        }
      }
      totalDemandReduction += (base.demandScore - sim.demandScore) * 100;
      validCount++;
    }
    const avgReliefPct = validCount > 0 ? totalRelativeRelief / validCount : 0;
    const avgDemandReduction =
      validCount > 0 ? totalDemandReduction / validCount : 0;

    return {
      count: affected.length,
      avgReliefPct,
      avgDemandReduction,
      peakStop,
      peakRelief,
    };
  }, [simResult]);

  // Inject pulse keyframes + Leaflet popup styles (always light card — matches cycle dashboard)
  useEffect(() => {
    const el = document.createElement("style");
    el.dataset["trainUi"] = "1";
    el.innerHTML = `
      @keyframes trainPulse{0%{box-shadow:0 0 0 0 rgba(46,160,67,.75)}70%{box-shadow:0 0 0 14px rgba(46,160,67,0)}100%{box-shadow:0 0 0 0 rgba(46,160,67,0)}}
      .train-map .leaflet-popup-content-wrapper{background:#fff!important;color:#111827!important;border:1px solid rgba(0,0,0,0.08)!important;border-radius:12px!important;box-shadow:0 6px 24px rgba(0,0,0,0.14)!important;padding:0!important;}
      .train-map .leaflet-popup-tip{background:#fff!important;}
      .train-map .leaflet-popup-close-button{color:#6b7280!important;top:8px!important;right:10px!important;font-size:18px!important;}
      .train-map .leaflet-popup-content{margin:0!important;}
    `;
    document.head.append(el);
    return () => el.remove();
  }, []);

  const tileUrl =
    theme === "dark"
      ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
      : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";
  const tileAttr =
    theme === "dark"
      ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
      : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';

  const filteredStations = useMemo(() => {
    let list = trainData?.data ?? [];
    if (typeFilter !== "ALL")
      list = list.filter(
        (s) => getStationTypeKey(s.stationType) === typeFilter,
      );
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (s) =>
          s.stationDesc?.toLowerCase().includes(q) ||
          s.stationCode?.toLowerCase().includes(q),
      );
    }
    return list;
  }, [trainData, typeFilter, search]);

  const filteredLiveTrains = useMemo(() => {
    if (typeFilter === "ALL") return liveTrains;
    return liveTrains.filter(
      (t) => getTrainTypeKey(t.trainType) === typeFilter,
    );
  }, [liveTrains, typeFilter]);

  const sortedTrains = useMemo(
    () =>
      filteredLiveTrains.toSorted((a, b) => {
        const aR = isRunning(a.status) ? 0 : 1;
        const bR = isRunning(b.status) ? 0 : 1;
        return aR - bR || (a.trainCode ?? "").localeCompare(b.trainCode ?? "");
      }),
    [filteredLiveTrains],
  );

  const runningCount = filteredLiveTrains.filter((t) =>
    isRunning(t.status),
  ).length;
  const scheduledCount = filteredLiveTrains.length - runningCount;

  const handleStationClick = useCallback(
    (lat: number, lon: number, code: string) => {
      setSelectedStationCode(code);
      setSelectedTrainCode(null);
      setFlyTarget({ center: [lat, lon], id: Date.now() });
    },
    [],
  );

  const handleTrainClick = useCallback(
    (lat: number, lon: number, code: string) => {
      setSelectedTrainCode(code);
      setSelectedStationCode(null);
      setFlyTarget({ center: [lat, lon], id: Date.now() });
    },
    [],
  );

  const handleTabChange = (_: React.SyntheticEvent, v: number) => {
    setTabValue(v);
    setSelectedStationCode(null);
    setSelectedTrainCode(null);
    setSelectedDisruptionId(null);
  };

  // Build a set of connected stop-ID pairs from route data
  const connectedPairs = useMemo(() => {
    const pairs = new Set<string>();
    for (const route of trainRoutes) {
      const ids = route.stopIds;
      for (let i = 0; i < ids.length; i++) {
        for (let j = i + 1; j < ids.length; j++) {
          pairs.add([ids[i], ids[j]].toSorted().join("|"));
        }
      }
    }
    return pairs;
  }, [trainRoutes]);

  const corridorErrors = useMemo(
    () =>
      corridors.map((c) => {
        if (!c.origin || !c.dest) return null;
        if (c.origin.stopId === c.dest.stopId)
          return "Origin and destination must be different";
        const key = [c.origin.stopId, c.dest.stopId].toSorted().join("|");
        if (!connectedPairs.has(key))
          return "No route connects these two stations";
        return null;
      }),
    [corridors, connectedPairs],
  );

  const hasCorridorErrors = corridorErrors.some((e) => e !== null);

  const handleSimulate = () => {
    const ready = corridors.filter(
      (c, i) => c.origin && c.dest && !corridorErrors[i],
    );
    if (ready.length === 0) return;
    submitApprovalMutation.reset();
    simulateMutation.mutate(
      {
        corridors: ready.map((c) => ({
          originStopId: c.origin!.stopId,
          destinationStopId: c.dest!.stopId,
          trainCount: c.count,
        })),
      },
      {
        onSuccess: (res) =>
          setSimResult({
            simulatedDemand: res.simulatedDemand,
            baseDemand: res.baseDemand,
            affectedStopIds: res.affectedStopIds,
          }),
      },
    );
  };

  const updateCorridor = (
    idx: number,
    patch: Partial<{
      origin: StationDemand | null;
      dest: StationDemand | null;
      count: number;
    }>,
  ) => {
    setCorridors((prev) =>
      prev.map((c, i) => (i === idx ? { ...c, ...patch } : c)),
    );
    setSimResult(null);
  };

  const addCorridor = () => {
    if (corridors.length < 3)
      setCorridors((prev) => [...prev, { origin: null, dest: null, count: 1 }]);
  };

  const removeCorridor = (idx: number) => {
    setCorridors((prev) => prev.filter((_, i) => i !== idx));
  };

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* ── Full-viewport map ── */}
      <MapContainer
        center={DUBLIN_CENTER}
        zoom={11}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
        className="train-map"
      >
        <TileLayer attribution={tileAttr} url={tileUrl} />
        <MapController target={flyTarget} />

        {/* Route corridor polylines */}
        {tabValue === 3
          ? /* Demand mode — per-segment colour green→red */
            trainRoutes.flatMap((route, rIdx) =>
              route.stops.slice(0, -1).map((from, sIdx) => {
                const to = route.stops[sIdx + 1];
                const idA = route.stopIds?.[sIdx];
                const idB = route.stopIds?.[sIdx + 1];
                const scoreA = idA
                  ? (demandByStop.get(idA)?.demandScore ?? 0)
                  : 0;
                const scoreB = idB
                  ? (demandByStop.get(idB)?.demandScore ?? 0)
                  : 0;
                const color = demandColor((scoreA + scoreB) / 2);
                return (
                  <Polyline
                    key={`${rIdx}-${sIdx}`}
                    positions={[from, to]}
                    pathOptions={{ color, weight: 5, opacity: 0.8 }}
                  />
                );
              }),
            )
          : /* Normal mode — single colour per route */
            trainRoutes.map((route, idx) => (
              <Polyline
                key={`${route.routeName}-${idx}`}
                positions={route.stops}
                pathOptions={{
                  color: getRouteColor(route.shortName),
                  weight: 4,
                  opacity: 0.6,
                }}
              >
                <Popup>
                  <Box sx={{ p: 1.25 }}>
                    <Typography
                      fontWeight={700}
                      sx={{ fontSize: "0.85rem", color: "#111827" }}
                    >
                      {route.routeName}
                    </Typography>
                    <Typography sx={{ fontSize: "0.72rem", color: "#6b7280" }}>
                      {route.shortName} · {route.stops.length} stops
                    </Typography>
                  </Box>
                </Popup>
              </Polyline>
            ))}

        {/* Demand mode — station markers coloured by demand */}
        {tabValue === 3 &&
          activeDemand.map((d) => {
            const isAffected = affectedSet.has(d.stopId);
            const baseEntry = simResult?.baseDemand.find(
              (b) => b.stopId === d.stopId,
            );
            const color = demandColor(d.demandScore);
            return (
              <Marker
                key={d.stopId}
                position={[d.lat, d.lon]}
                icon={makeDemandIcon(d.demandScore, isAffected)}
              >
                <Popup>
                  <Box sx={{ minWidth: 160, p: 1.25 }}>
                    {/* Station name */}
                    <Typography
                      fontWeight={700}
                      sx={{ fontSize: "0.88rem", color: "#111827", mb: 0.75 }}
                    >
                      {d.name}
                    </Typography>

                    {/* Demand score */}
                    <Typography
                      sx={{
                        fontSize: "0.58rem",
                        textTransform: "uppercase",
                        letterSpacing: 0.5,
                        color: "#6b7280",
                        display: "block",
                        mb: 0.25,
                      }}
                    >
                      Demand score
                    </Typography>
                    <Typography
                      fontWeight={700}
                      sx={{ fontSize: "1.25rem", color }}
                    >
                      {(d.demandScore * 100).toFixed(0)}%
                    </Typography>

                    {/* Simulation: demand improvement */}
                    {isAffected &&
                      baseEntry &&
                      (() => {
                        const before = baseEntry.demandScore * 100;
                        const after = d.demandScore * 100;
                        const delta = after - before;
                        return (
                          <Box
                            sx={{
                              mt: 1,
                              pt: 0.75,
                              borderTop: "1px solid #f3f4f6",
                              display: "flex",
                              alignItems: "center",
                              gap: 0.75,
                            }}
                          >
                            <Typography
                              sx={{ fontSize: "0.72rem", color: "#6b7280" }}
                            >
                              {before.toFixed(0)}%
                            </Typography>
                            <Typography
                              sx={{ fontSize: "0.72rem", color: "#9ca3af" }}
                            >
                              →
                            </Typography>
                            <Typography
                              fontWeight={700}
                              sx={{ fontSize: "0.72rem", color: "#16a34a" }}
                            >
                              {after.toFixed(0)}%
                            </Typography>
                            {delta > 0 && (
                              <Box
                                sx={{
                                  ml: "auto",
                                  bgcolor: "#dcfce7",
                                  borderRadius: 1,
                                  px: 0.75,
                                  py: 0.2,
                                }}
                              >
                                <Typography
                                  fontWeight={700}
                                  sx={{ fontSize: "0.68rem", color: "#16a34a" }}
                                >
                                  +{delta.toFixed(1)}%
                                </Typography>
                              </Box>
                            )}
                          </Box>
                        );
                      })()}
                  </Box>
                </Popup>
              </Marker>
            );
          })}

        {/* Station markers — hidden in demand mode (demand markers replace them) */}
        {tabValue !== 3 &&
          filteredStations.map((station) => (
            <Marker
              key={station.id}
              position={[station.lat, station.lon]}
              icon={makeStationIcon(getStationTypeKey(station.stationType))}
              eventHandlers={{
                click: () =>
                  handleStationClick(
                    station.lat,
                    station.lon,
                    station.stationCode,
                  ),
              }}
            >
              <Popup>
                <Box sx={{ minWidth: 180, p: 1.5 }}>
                  <Typography
                    fontWeight={700}
                    sx={{ fontSize: "0.9rem", mb: 0.75, color: "#111827" }}
                  >
                    {station.stationDesc}
                  </Typography>
                  <Box sx={{ display: "flex", gap: 0.75, flexWrap: "wrap" }}>
                    <Chip
                      size="small"
                      label={station.stationCode}
                      sx={{
                        fontSize: "0.65rem",
                        height: 18,
                        bgcolor: "#f3f4f6",
                        color: "#374151",
                      }}
                    />
                    {(() => {
                      const key = getStationTypeKey(station.stationType);
                      const cfg = key === "ALL" ? null : TYPE_CONFIG[key];
                      return cfg ? (
                        <Chip
                          size="small"
                          label={cfg.label}
                          sx={{
                            fontSize: "0.65rem",
                            height: 18,
                            bgcolor: cfg.color + "22",
                            color: cfg.color,
                          }}
                        />
                      ) : null;
                    })()}
                  </Box>
                </Box>
              </Popup>
            </Marker>
          ))}

        {/* Live train markers — hidden in demand mode */}
        {tabValue !== 3 &&
          filteredLiveTrains
            .filter((t) => t.lat && t.lon)
            .map((t) => {
              const running = isRunning(t.status);
              const typeKey = getTrainTypeKey(t.trainType);
              const typCfg = TYPE_CONFIG[typeKey];
              const dirArrow = getDirectionArrow(t.direction);
              return (
                <Marker
                  key={t.trainCode}
                  position={[t.lat, t.lon]}
                  icon={makeLiveTrainIcon(
                    t.trainType ?? null,
                    t.direction ?? null,
                    running,
                  )}
                  eventHandlers={{
                    click: () => handleTrainClick(t.lat, t.lon, t.trainCode),
                  }}
                >
                  <Popup>
                    <Box sx={{ minWidth: 190, p: 1.5 }}>
                      <Typography
                        fontWeight={700}
                        sx={{ fontSize: "0.9rem", mb: 0.5, color: "#111827" }}
                      >
                        🚂 {t.trainCode}
                      </Typography>
                      <Box
                        sx={{
                          display: "flex",
                          gap: 0.5,
                          mb: 0.5,
                          flexWrap: "wrap",
                        }}
                      >
                        <Chip
                          size="small"
                          label={running ? "● Running" : "○ Scheduled"}
                          sx={{
                            fontSize: "0.65rem",
                            height: 18,
                            bgcolor: (running ? "#2ea043" : "#d29922") + "22",
                            color: running ? "#2ea043" : "#d29922",
                          }}
                        />
                        {t.trainType && (
                          <Chip
                            size="small"
                            label={
                              typCfg.label === "All"
                                ? t.trainType
                                : typCfg.label
                            }
                            sx={{
                              fontSize: "0.65rem",
                              height: 18,
                              bgcolor: typCfg.color + "22",
                              color: typCfg.color,
                            }}
                          />
                        )}
                      </Box>
                      {t.direction && (
                        <Typography
                          sx={{ fontSize: "0.72rem", color: "#374151" }}
                        >
                          {dirArrow} {t.direction}
                        </Typography>
                      )}
                      {t.publicMessage && (
                        <Typography
                          sx={{
                            fontSize: "0.72rem",
                            color: "#6b7280",
                            mt: 0.4,
                          }}
                        >
                          {t.publicMessage}
                        </Typography>
                      )}
                    </Box>
                  </Popup>
                </Marker>
              );
            })}
      </MapContainer>

      {error && (
        <Alert
          severity="error"
          sx={{
            position: "absolute",
            top: GAP,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1001,
            borderRadius: 2,
          }}
        >
          Failed to load train data
        </Alert>
      )}

      {/* ── Toggle button (when panel closed) ── */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* ── Floating right panel ── */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute",
            top: GAP,
            right: GAP,
            bottom: GAP,
            width: PANEL_W,
            zIndex: 1000,
            borderRadius: 3,
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          {/* Header */}
          <Box
            sx={{
              p: 2,
              pb: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              <TrainIcon color="primary" />
              <Typography variant="h5">Dublin Rail Network</Typography>
            </Box>
            <IconButton size="small" onClick={() => setPanelOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>

          {/* KPIs */}
          {kpiData && (
            <Box
              sx={{
                px: 2,
                pb: 1,
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 0.5,
              }}
            >
              <KpiRow
                icon={<LocationOnIcon fontSize="inherit" />}
                label="Stations"
                value={kpiData.totalStations}
                color="#00ACC1"
              />
              <KpiRow
                icon={<TrainIcon fontSize="inherit" />}
                label="Live Trains"
                value={kpiData.liveTrainsRunning}
                color="#2ea043"
              />
              <KpiRow
                icon={<AccessTimeIcon fontSize="inherit" />}
                label="On Time"
                value={`${kpiData.onTimePct.toFixed(1)}%`}
                color={kpiData.onTimePct >= 80 ? "#2ea043" : "#d29922"}
              />
              <KpiRow
                icon={<SpeedIcon fontSize="inherit" />}
                label="Avg Delay"
                value={`${kpiData.avgDelayMinutes.toFixed(1)} min`}
                color={kpiData.avgDelayMinutes <= 2 ? "#2ea043" : "#d29922"}
              />
            </Box>
          )}

          {/* Running / Scheduled chips */}
          {liveTrains.length > 0 && (
            <Box sx={{ px: 2, pb: 1, display: "flex", gap: 0.75 }}>
              <Chip
                size="small"
                label={`${runningCount} Running`}
                color="success"
                variant="outlined"
                sx={{ fontSize: "0.68rem" }}
              />
              <Chip
                size="small"
                label={`${scheduledCount} Scheduled`}
                sx={{
                  fontSize: "0.68rem",
                  borderColor: "#d29922",
                  color: "#d29922",
                  border: "1px solid",
                }}
                variant="outlined"
              />
            </Box>
          )}

          {/* Type filter chips */}
          <Box
            sx={{ px: 2, pb: 1, display: "flex", gap: 0.5, flexWrap: "wrap" }}
          >
            {(["ALL", "D", "S", "M"] as TypeKey[]).map((key) => {
              const active = typeFilter === key;
              return (
                <Chip
                  key={key}
                  size="small"
                  label={TYPE_CONFIG[key].label}
                  onClick={() => setTypeFilter(key)}
                  color={active && key === "ALL" ? "primary" : undefined}
                  sx={{
                    fontSize: "0.7rem",
                    cursor: "pointer",
                    ...(key !== "ALL" && {
                      bgcolor: active ? TYPE_CONFIG[key].color : undefined,
                      color: active ? "#fff" : TYPE_CONFIG[key].color,
                      borderColor: TYPE_CONFIG[key].color,
                      border: "1px solid",
                    }),
                  }}
                  variant={active && key !== "ALL" ? "filled" : "outlined"}
                />
              );
            })}
          </Box>

          {/* Tabs */}
          <Tabs
            value={tabValue}
            onChange={handleTabChange}
            variant="scrollable"
            scrollButtons="auto"
            sx={{
              minHeight: 36,
              "& .MuiTab-root": {
                minHeight: 36,
                fontSize: "0.7rem",
                textTransform: "none",
                minWidth: 0,
                px: 1.5,
              },
              "& .MuiTabScrollButton-root": { width: 24 },
            }}
          >
            <Tab label={`Stations (${filteredStations.length})`} />
            <Tab
              label={`Trains (${sortedTrains.filter((t) => t.lat && t.lon).length})`}
            />
            <Tab label={`Delays (${frequentDelays.length})`} />
            <Tab label="Demand" />
            <Tab
              label={`Approvals${approvals.some((a) => a.status === "PENDING") ? ` (${approvals.filter((a) => a.status === "PENDING").length})` : ""}`}
            />
            <Tab label="Disruptions" />
          </Tabs>

          {/* Tab content */}
          <Box sx={{ flex: 1, overflow: "auto", px: 1, pt: 0.5 }}>
            {/* ── Stations ── */}
            {tabValue === 0 && (
              <>
                <Box sx={{ px: 1, pt: 0.5, pb: 0.5 }}>
                  <TextField
                    size="small"
                    fullWidth
                    placeholder="Search stations…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    slotProps={{
                      input: {
                        startAdornment: (
                          <InputAdornment position="start">
                            <SearchIcon sx={{ fontSize: 16 }} />
                          </InputAdornment>
                        ),
                      },
                    }}
                  />
                </Box>
                {dataLoading ? (
                  <Box
                    sx={{ display: "flex", justifyContent: "center", py: 4 }}
                  >
                    <CircularProgress size={24} />
                  </Box>
                ) : (
                  filteredStations.map((station) => {
                    const typCfg =
                      TYPE_CONFIG[getStationTypeKey(station.stationType)];
                    const selected =
                      selectedStationCode === station.stationCode;
                    return (
                      <ListItemButton
                        key={station.id}
                        onClick={() =>
                          handleStationClick(
                            station.lat,
                            station.lon,
                            station.stationCode,
                          )
                        }
                        selected={selected}
                        sx={{ py: 0.875, px: 1, borderRadius: 1 }}
                      >
                        <Box
                          sx={{
                            width: 22,
                            height: 22,
                            borderRadius: "50%",
                            bgcolor: typCfg.color,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            flexShrink: 0,
                            mr: 1.5,
                          }}
                        >
                          <Typography
                            sx={{ fontSize: 9, fontWeight: 800, color: "#fff" }}
                          >
                            {typCfg.short}
                          </Typography>
                        </Box>
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <Typography
                            noWrap
                            variant="body2"
                            fontWeight={selected ? 600 : 400}
                          >
                            {station.stationDesc}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {station.stationCode} · {typCfg.label}
                          </Typography>
                        </Box>
                      </ListItemButton>
                    );
                  })
                )}
              </>
            )}

            {/* ── Live Trains ── */}
            {tabValue === 1 &&
              (sortedTrains.length === 0 ? (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <TrainIcon
                    sx={{ fontSize: 32, color: "action.disabled", mb: 1 }}
                  />
                  <Typography variant="body2" color="text.secondary">
                    No live trains at the moment
                  </Typography>
                </Box>
              ) : (
                sortedTrains.map((t) => {
                  const running = isRunning(t.status);
                  const typeKey = getTrainTypeKey(t.trainType);
                  const typCfg = TYPE_CONFIG[typeKey];
                  const dirArrow = getDirectionArrow(t.direction);
                  const hasCoors = t.lat && t.lon;
                  const selected = selectedTrainCode === t.trainCode;
                  return (
                    <ListItemButton
                      key={t.trainCode}
                      onClick={() =>
                        hasCoors && handleTrainClick(t.lat, t.lon, t.trainCode)
                      }
                      disabled={!hasCoors}
                      selected={selected}
                      sx={{
                        py: 0.875,
                        px: 1,
                        borderRadius: 1,
                        opacity: hasCoors ? 1 : 0.45,
                      }}
                    >
                      <Box
                        sx={{
                          width: 22,
                          height: 22,
                          borderRadius: "50%",
                          bgcolor: typCfg.color,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          mr: 1.5,
                          boxShadow: running
                            ? `0 0 0 3px ${typCfg.color}44`
                            : "none",
                        }}
                      >
                        <Typography
                          sx={{ fontSize: 10, fontWeight: 900, color: "#fff" }}
                        >
                          {dirArrow || typCfg.short}
                        </Typography>
                      </Box>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography variant="body2" fontWeight={600}>
                          {t.trainCode}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {typCfg.label === "All"
                            ? (t.trainType ?? "Unknown")
                            : typCfg.label}
                          {t.direction ? ` · ${dirArrow} ${t.direction}` : ""}
                        </Typography>
                      </Box>
                      <Chip
                        size="small"
                        label={running ? "Running" : "Sched."}
                        sx={{
                          fontSize: "0.62rem",
                          height: 18,
                          ml: 0.5,
                          flexShrink: 0,
                          bgcolor: (running ? "#2ea043" : "#d29922") + "20",
                          color: running ? "#2ea043" : "#d29922",
                          border: `1px solid ${running ? "#2ea043" : "#d29922"}40`,
                        }}
                      />
                    </ListItemButton>
                  );
                })
              ))}

            {/* ── Delays ── */}
            {tabValue === 2 &&
              (delaysLoading ? (
                <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
                  <CircularProgress size={24} />
                </Box>
              ) : frequentDelays.length === 0 ? (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <Typography variant="body2" color="text.secondary">
                    No delay data available
                  </Typography>
                </Box>
              ) : (
                <Box
                  component="table"
                  sx={{
                    width: "100%",
                    borderCollapse: "collapse",
                    fontSize: "0.75rem",
                  }}
                >
                  <Box component="thead">
                    <Box component="tr">
                      {["Train", "Route", "Direction", "Avg Delay"].map((h) => (
                        <Box
                          component="th"
                          key={h}
                          sx={{
                            textAlign: "left",
                            py: 0.75,
                            px: 1,
                            color: "text.secondary",
                            fontSize: "0.62rem",
                            textTransform: "uppercase",
                            letterSpacing: 0.6,
                            borderBottom: 1,
                            borderColor: "divider",
                            position: "sticky",
                            top: 0,
                            bgcolor: "background.paper",
                          }}
                        >
                          {h}
                        </Box>
                      ))}
                    </Box>
                  </Box>
                  <Box component="tbody">
                    {frequentDelays.map((row, idx) => (
                      <Box component="tr" key={`${row.trainCode}-${idx}`}>
                        <Box
                          component="td"
                          sx={{
                            py: 0.75,
                            px: 1,
                            fontWeight: 600,
                            borderBottom: 1,
                            borderColor: "divider",
                          }}
                        >
                          {row.trainCode}
                        </Box>
                        <Box
                          component="td"
                          sx={{
                            py: 0.75,
                            px: 1,
                            borderBottom: 1,
                            borderColor: "divider",
                            maxWidth: 120,
                          }}
                        >
                          <Typography variant="caption" display="block">
                            {row.origin}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            → {row.destination}
                          </Typography>
                        </Box>
                        <Box
                          component="td"
                          sx={{
                            py: 0.75,
                            px: 1,
                            borderBottom: 1,
                            borderColor: "divider",
                          }}
                        >
                          <Typography variant="caption" color="text.secondary">
                            {row.direction}
                          </Typography>
                        </Box>
                        <Box
                          component="td"
                          sx={{
                            py: 0.75,
                            px: 1,
                            borderBottom: 1,
                            borderColor: "divider",
                          }}
                        >
                          <Chip
                            size="small"
                            label={`${row.totalAvgDelayMinutes} min`}
                            sx={{
                              fontSize: "0.65rem",
                              height: 18,
                              bgcolor:
                                row.totalAvgDelayMinutes > 10
                                  ? "#da362320"
                                  : row.totalAvgDelayMinutes > 5
                                    ? "#d2992220"
                                    : "#2ea04320",
                              color:
                                row.totalAvgDelayMinutes > 10
                                  ? "#da3623"
                                  : row.totalAvgDelayMinutes > 5
                                    ? "#d29922"
                                    : "#2ea043",
                            }}
                          />
                        </Box>
                      </Box>
                    ))}
                  </Box>
                </Box>
              ))}

            {/* ── Demand ── */}
            {tabValue === 3 && (
              <Box
                sx={{
                  p: 1.5,
                  display: "flex",
                  flexDirection: "column",
                  gap: 1.5,
                }}
              >
                {/* Legend */}
                <Box
                  sx={{ display: "flex", flexDirection: "column", gap: 0.4 }}
                >
                  <Box
                    sx={{ display: "flex", alignItems: "center", gap: 0.75 }}
                  >
                    <Typography
                      variant="caption"
                      sx={{
                        fontSize: "0.6rem",
                        color: "hsl(120,70%,38%)",
                        whiteSpace: "nowrap",
                      }}
                    >
                      Low
                    </Typography>
                    <Box
                      sx={{
                        flex: 1,
                        height: 7,
                        borderRadius: 4,
                        background:
                          "linear-gradient(to right, hsl(120,80%,42%), hsl(60,80%,42%), hsl(0,80%,42%))",
                      }}
                    />
                    <Typography
                      variant="caption"
                      sx={{
                        fontSize: "0.6rem",
                        color: "hsl(0,70%,38%)",
                        whiteSpace: "nowrap",
                      }}
                    >
                      High
                    </Typography>
                  </Box>
                </Box>

                {/* Corridors */}
                <Box
                  sx={{
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                  }}
                >
                  <Typography
                    variant="caption"
                    fontWeight={700}
                    sx={{
                      textTransform: "uppercase",
                      letterSpacing: 0.5,
                      color: "text.secondary",
                    }}
                  >
                    Simulate new services
                  </Typography>
                </Box>

                {corridors.map((corridor, idx) => (
                  <Box
                    key={idx}
                    sx={{
                      border: 1,
                      borderColor: "divider",
                      borderRadius: 2,
                      p: 1.25,
                      display: "flex",
                      flexDirection: "column",
                      gap: 1,
                      position: "relative",
                    }}
                  >
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                        mb: 0.25,
                      }}
                    >
                      <Typography
                        variant="caption"
                        fontWeight={700}
                        color="primary"
                        sx={{ fontSize: "0.68rem" }}
                      >
                        Corridor {idx + 1}
                      </Typography>
                      {corridors.length > 1 && (
                        <IconButton
                          size="small"
                          onClick={() => removeCorridor(idx)}
                          sx={{ p: 0.25 }}
                        >
                          <DeleteIcon sx={{ fontSize: 14 }} />
                        </IconButton>
                      )}
                    </Box>

                    <Autocomplete
                      size="small"
                      options={demandData}
                      getOptionLabel={(o) => o.name}
                      value={corridor.origin}
                      onChange={(_, v) => updateCorridor(idx, { origin: v })}
                      renderInput={(params) => (
                        <TextField {...params} label="Origin" />
                      )}
                      isOptionEqualToValue={(a, b) => a.stopId === b.stopId}
                    />
                    <Autocomplete
                      size="small"
                      options={demandData}
                      getOptionLabel={(o) => o.name}
                      value={corridor.dest}
                      onChange={(_, v) => updateCorridor(idx, { dest: v })}
                      renderInput={(params) => (
                        <TextField
                          {...params}
                          label="Destination"
                          error={!!corridorErrors[idx]}
                          helperText={corridorErrors[idx] ?? undefined}
                        />
                      )}
                      isOptionEqualToValue={(a, b) => a.stopId === b.stopId}
                    />

                    <Box sx={{ px: 0.5 }}>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          mb: 0.25,
                        }}
                      >
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          sx={{ fontSize: "0.65rem" }}
                        >
                          Trains to add
                        </Typography>
                        <Typography
                          variant="caption"
                          fontWeight={700}
                          sx={{ fontSize: "0.68rem" }}
                        >
                          {corridor.count}
                        </Typography>
                      </Box>
                      <Slider
                        size="small"
                        min={1}
                        max={20}
                        value={corridor.count}
                        onChange={(_, v) =>
                          updateCorridor(idx, { count: Number(v) })
                        }
                        marks={[
                          { value: 1, label: "1" },
                          { value: 10, label: "10" },
                          { value: 20, label: "20" },
                        ]}
                        sx={{
                          "& .MuiSlider-markLabel": { fontSize: "0.58rem" },
                        }}
                      />
                    </Box>
                  </Box>
                ))}

                <Box sx={{ display: "flex", gap: 1 }}>
                  {corridors.length < 3 && (
                    <Button
                      size="small"
                      variant="outlined"
                      startIcon={<AddIcon />}
                      onClick={addCorridor}
                      sx={{ fontSize: "0.72rem" }}
                    >
                      Add corridor
                    </Button>
                  )}
                  <Button
                    size="small"
                    variant="contained"
                    disabled={
                      corridors.every((c) => !c.origin || !c.dest) ||
                      hasCorridorErrors ||
                      simulateMutation.isPending
                    }
                    onClick={handleSimulate}
                    sx={{ ml: "auto", fontSize: "0.72rem" }}
                  >
                    {simulateMutation.isPending ? "Simulating…" : "Simulate"}
                  </Button>
                </Box>

                {simMetrics && (
                  <Box
                    sx={{
                      borderRadius: 2,
                      border: 1,
                      borderColor: "#2ea04340",
                      bgcolor: "#2ea04308",
                      overflow: "hidden",
                    }}
                  >
                    <Box sx={{ px: 1.5, py: 1, bgcolor: "#2ea04318" }}>
                      <Typography
                        variant="caption"
                        fontWeight={700}
                        sx={{
                          color: "#2ea043",
                          textTransform: "uppercase",
                          letterSpacing: 0.5,
                          fontSize: "0.65rem",
                        }}
                      >
                        Simulation active
                      </Typography>
                    </Box>
                    <Box
                      sx={{
                        p: 1.25,
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr",
                        gap: 1,
                      }}
                    >
                      {(
                        [
                          {
                            label: "Stations improved",
                            value: String(simMetrics.count),
                          },
                          {
                            label: "Overall score improved by",
                            value: `${simMetrics.avgDemandReduction.toFixed(1)}%`,
                          },
                          {
                            label: "Train crowding reduced by",
                            value: `${simMetrics.avgReliefPct.toFixed(1)}%`,
                          },
                        ] as { label: string; value: string }[]
                      ).map(({ label, value }) => (
                        <Box key={label}>
                          <Typography
                            variant="caption"
                            sx={{
                              color: "text.secondary",
                              fontSize: "0.6rem",
                              textTransform: "uppercase",
                              letterSpacing: 0.4,
                              display: "block",
                            }}
                          >
                            {label}
                          </Typography>
                          <Typography
                            fontWeight={700}
                            sx={{ fontSize: "1rem", color: "#2ea043" }}
                          >
                            {value}
                          </Typography>
                        </Box>
                      ))}
                      {simMetrics.peakStop && (
                        <Box sx={{ gridColumn: "1 / -1" }}>
                          <Typography
                            variant="caption"
                            sx={{
                              color: "text.secondary",
                              fontSize: "0.6rem",
                              textTransform: "uppercase",
                              letterSpacing: 0.4,
                              display: "block",
                            }}
                          >
                            Biggest improvement
                          </Typography>
                          <Typography
                            variant="body2"
                            fontWeight={600}
                            noWrap
                            sx={{ fontSize: "0.78rem" }}
                          >
                            {simMetrics.peakStop.name}
                          </Typography>
                          <Typography
                            variant="caption"
                            sx={{ color: "#2ea043", fontSize: "0.7rem" }}
                          >
                            crowding reduced by{" "}
                            {simMetrics.peakRelief.toFixed(1)}%
                          </Typography>
                        </Box>
                      )}
                    </Box>
                    <Box sx={{ px: 1.25, pb: 1 }}>
                      <Typography
                        variant="caption"
                        sx={{ color: "text.secondary", fontSize: "0.62rem" }}
                      >
                        White-ringed markers = boosted stations · hover for
                        before/after
                      </Typography>
                    </Box>
                  </Box>
                )}

                {simResult && (
                  <Box
                    sx={{
                      display: "flex",
                      flexDirection: "column",
                      gap: 1,
                      mt: 0.5,
                    }}
                  >
                    <Button
                      fullWidth
                      variant="outlined"
                      size="small"
                      sx={{
                        fontSize: "0.72rem",
                        borderColor: "#d1d5db",
                        color: "text.secondary",
                      }}
                      onClick={() => {
                        setSimResult(null);
                        setCorridors([{ origin: null, dest: null, count: 1 }]);
                      }}
                    >
                      Reset simulation
                    </Button>
                    {isTrainAdmin && (
                      <Button
                        fullWidth
                        variant="contained"
                        size="small"
                        color="primary"
                        sx={{ fontSize: "0.72rem" }}
                        disabled={
                          submitApprovalMutation.isPending ||
                          submitApprovalMutation.isSuccess
                        }
                        onClick={() => {
                          if (!simResult || !simMetrics) return;
                          const corridorSummary = corridors
                            .filter((c) => c.origin && c.dest)
                            .map(
                              (c) =>
                                `${c.origin?.name} → ${c.dest?.name} (+${c.count})`,
                            )
                            .join("; ");
                          const affectedStations = simResult.affectedStopIds
                            .map((id) => {
                              const sim = simResult.simulatedDemand.find(
                                (s) => s.stopId === id,
                              );
                              const base = simResult.baseDemand.find(
                                (s) => s.stopId === id,
                              );
                              if (!sim || !base) return null;
                              const scoreDelta =
                                (base.demandScore - sim.demandScore) * 100;
                              return {
                                station: sim.name,
                                demandBefore: `${(base.demandScore * 100).toFixed(0)}%`,
                                demandAfter: `${(sim.demandScore * 100).toFixed(0)}%`,
                                demandReduction: `${scoreDelta.toFixed(1)}%`,
                                overcrowdingRisk: `${(base.normPressure * 100).toFixed(0)}% → ${(sim.normPressure * 100).toFixed(0)}%`,
                                annualPassengers: `${(base.normRidership * 100).toFixed(0)}%`,
                                nearbyResidents: `${(base.normUptake * 100).toFixed(0)}%`,
                              };
                            })
                            .filter(Boolean);
                          const avgScoreDelta = simMetrics.avgDemandReduction;
                          submitApprovalMutation.mutate({
                            indicator: "train",
                            actionUrl: "/dashboard?view=train&tab=approvals",
                            payloadJson: JSON.stringify({
                              corridors: corridors
                                .filter((c) => c.origin && c.dest)
                                .map((c) => ({
                                  origin: c.origin?.name,
                                  destination: c.dest?.name,
                                  trainsAdded: c.count,
                                })),
                              impact: {
                                stationsAffected: simMetrics.count,
                                avgDemandReduction: `${avgScoreDelta.toFixed(1)}%`,
                                mostImprovedStation: simMetrics.peakStop?.name,
                              },
                              stationBreakdown: affectedStations,
                            }),
                            summary: `Add trains on: ${corridorSummary}. Demand improvement across ${simMetrics.count} station(s). Most improved: ${simMetrics.peakStop?.name ?? "N/A"}.`,
                          });
                        }}
                      >
                        {submitApprovalMutation.isSuccess
                          ? "Sent for approval ✓"
                          : submitApprovalMutation.isPending
                            ? "Sending…"
                            : "Send for approval"}
                      </Button>
                    )}
                  </Box>
                )}

                {simulateMutation.isError && (
                  <Alert severity="error" sx={{ fontSize: "0.72rem" }}>
                    Simulation failed — please try again.
                  </Alert>
                )}
              </Box>
            )}

            {/* ── Tab 4: Approvals ─────────────────────────────────── */}
            {tabValue === 4 && (
              <Box
                sx={{
                  p: 1.5,
                  display: "flex",
                  flexDirection: "column",
                  gap: 1.5,
                }}
              >
                <Typography
                  variant="caption"
                  fontWeight={700}
                  sx={{
                    textTransform: "uppercase",
                    letterSpacing: 0.5,
                    color: "text.secondary",
                  }}
                >
                  {isCityManager
                    ? "All approval requests"
                    : "My approval requests"}
                </Typography>

                {/* Filters — City Manager sees status chips */}
                {isCityManager && (
                  <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
                    {(["ALL", "PENDING", "APPROVED", "DENIED"] as const).map(
                      (s) => {
                        const count =
                          s === "ALL"
                            ? approvals.length
                            : approvals.filter((a) => a.status === s).length;
                        return (
                          <Chip
                            key={s}
                            size="small"
                            label={`${s} (${count})`}
                            sx={{
                              fontSize: "0.62rem",
                              height: 20,
                              bgcolor:
                                s === "PENDING"
                                  ? "#fef3c7"
                                  : s === "APPROVED"
                                    ? "#dcfce7"
                                    : s === "DENIED"
                                      ? "#fee2e2"
                                      : undefined,
                              color:
                                s === "PENDING"
                                  ? "#92400e"
                                  : s === "APPROVED"
                                    ? "#166534"
                                    : s === "DENIED"
                                      ? "#991b1b"
                                      : undefined,
                            }}
                          />
                        );
                      },
                    )}
                  </Box>
                )}

                {approvalsLoading && (
                  <CircularProgress size={20} sx={{ alignSelf: "center" }} />
                )}

                {!approvalsLoading && approvals.length === 0 && (
                  <Typography
                    variant="caption"
                    sx={{
                      color: "text.secondary",
                      textAlign: "center",
                      py: 2,
                      display: "block",
                    }}
                  >
                    No approval requests yet.
                  </Typography>
                )}

                {/* Approval rows — sorted: PENDING first, then by date */}
                {approvals
                  .toSorted((a, b) => {
                    if (a.status === "PENDING" && b.status !== "PENDING")
                      return -1;
                    if (b.status === "PENDING" && a.status !== "PENDING")
                      return 1;
                    return (
                      new Date(b.createdAt).getTime() -
                      new Date(a.createdAt).getTime()
                    );
                  })
                  .map((req) => (
                    <Box
                      key={req.id}
                      sx={{
                        border: "1px solid #e5e7eb",
                        borderRadius: 2,
                        p: 1.25,
                        display: "flex",
                        flexDirection: "column",
                        gap: 0.75,
                      }}
                    >
                      {/* Header row */}
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          gap: 0.75,
                        }}
                      >
                        <Chip
                          size="small"
                          label={req.status}
                          sx={{
                            fontSize: "0.6rem",
                            height: 18,
                            fontWeight: 700,
                            bgcolor:
                              req.status === "PENDING"
                                ? "#fef3c7"
                                : req.status === "APPROVED"
                                  ? "#dcfce7"
                                  : "#fee2e2",
                            color:
                              req.status === "PENDING"
                                ? "#92400e"
                                : req.status === "APPROVED"
                                  ? "#166534"
                                  : "#991b1b",
                          }}
                        />
                        <Typography
                          sx={{
                            fontSize: "0.6rem",
                            color: "text.secondary",
                            fontFamily: "monospace",
                          }}
                        >
                          APR-{String(req.id).padStart(6, "0")}
                        </Typography>
                        <Typography
                          sx={{
                            fontSize: "0.62rem",
                            color: "text.secondary",
                            ml: "auto",
                          }}
                        >
                          {new Date(req.createdAt).toLocaleDateString("en-IE", {
                            day: "numeric",
                            month: "short",
                            year: "numeric",
                          })}
                        </Typography>
                      </Box>

                      {/* Summary */}
                      <Typography
                        sx={{ fontSize: "0.72rem", color: "#374151" }}
                      >
                        {req.summary ?? "No summary provided."}
                      </Typography>

                      {/* Requested by (City Manager view) */}
                      {isCityManager && (
                        <Typography
                          sx={{ fontSize: "0.62rem", color: "text.secondary" }}
                        >
                          Requested by: <strong>{req.requestedBy}</strong>
                        </Typography>
                      )}

                      {/* Review note (if any) */}
                      {req.reviewNote && (
                        <Typography
                          sx={{
                            fontSize: "0.62rem",
                            color: "text.secondary",
                            fontStyle: "italic",
                          }}
                        >
                          Note: {req.reviewNote}
                        </Typography>
                      )}

                      {/* Approve / Deny actions — City Manager, PENDING only */}
                      {isCityManager && req.status === "PENDING" && (
                        <Box
                          sx={{
                            display: "flex",
                            flexDirection: "column",
                            gap: 0.75,
                            mt: 0.5,
                          }}
                        >
                          {reviewingId === req.id ? (
                            /* ── Step 2: note field + confirm ── */
                            <>
                              <TextField
                                size="small"
                                placeholder="Optional review note…"
                                value={reviewNote}
                                onChange={(e) => setReviewNote(e.target.value)}
                                multiline
                                rows={2}
                                sx={{
                                  "& .MuiInputBase-input": {
                                    fontSize: "0.72rem",
                                  },
                                }}
                              />
                              <Box sx={{ display: "flex", gap: 0.75 }}>
                                <Button
                                  size="small"
                                  variant="contained"
                                  color={
                                    pendingStatus === "APPROVED"
                                      ? "primary"
                                      : "error"
                                  }
                                  sx={{ flex: 1, fontSize: "0.68rem" }}
                                  disabled={reviewMutation.isPending}
                                  onClick={() =>
                                    reviewMutation.mutate({
                                      id: req.id,
                                      status: pendingStatus!,
                                    })
                                  }
                                >
                                  {reviewMutation.isPending
                                    ? "Saving…"
                                    : `Confirm ${pendingStatus === "APPROVED" ? "Approve" : "Deny"}`}
                                </Button>
                                <Button
                                  size="small"
                                  variant="text"
                                  sx={{
                                    fontSize: "0.68rem",
                                    color: "text.secondary",
                                  }}
                                  disabled={reviewMutation.isPending}
                                  onClick={() => {
                                    setReviewingId(null);
                                    setReviewNote("");
                                    setPendingStatus(null);
                                  }}
                                >
                                  Cancel
                                </Button>
                              </Box>
                            </>
                          ) : (
                            /* ── Step 1: choose action ── */
                            <Box sx={{ display: "flex", gap: 0.75 }}>
                              <Button
                                size="small"
                                variant="contained"
                                color="primary"
                                sx={{ flex: 1, fontSize: "0.68rem" }}
                                onClick={() => {
                                  setReviewingId(req.id);
                                  setPendingStatus("APPROVED");
                                }}
                              >
                                Approve
                              </Button>
                              <Button
                                size="small"
                                variant="outlined"
                                color="error"
                                sx={{ flex: 1, fontSize: "0.68rem" }}
                                onClick={() => {
                                  setReviewingId(req.id);
                                  setPendingStatus("DENIED");
                                }}
                              >
                                Deny
                              </Button>
                            </Box>
                          )}
                        </Box>
                      )}
                    </Box>
                  ))}
              </Box>
            )}

            {/* ── Disruptions ── */}
            {tabValue === 5 && (
              <DisruptionsTabContent
                mode="TRAIN"
                selectedId={selectedDisruptionId}
                onSelect={(d) => {
                  setSelectedDisruptionId(d.id);
                  if (d.latitude != null && d.longitude != null) {
                    setFlyTarget({
                      center: [d.latitude, d.longitude],
                      id: Date.now(),
                    });
                  }
                }}
              />
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};
