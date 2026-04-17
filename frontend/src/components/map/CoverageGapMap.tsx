/**
 * Coverage gap map — shows filled GeoJSON polygons coloured by coverage category,
 * with a simulation mode to place proposed stations and see instant impact.
 */

import { Fragment, useEffect, useRef, useState } from "react";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  GeoJSON,
  Popup,
  useMap,
  useMapEvents,
} from "react-leaflet";
import type { Layer, LeafletMouseEvent, PathOptions } from "leaflet";
import {
  Box,
  Paper,
  Typography,
  ToggleButton,
  ToggleButtonGroup,
  Button,
  Chip,
  TextField,
} from "@mui/material";
import AddLocationAltIcon from "@mui/icons-material/AddLocationAlt";
import CancelIcon from "@mui/icons-material/Cancel";
import SendIcon from "@mui/icons-material/Send";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import ThumbDownIcon from "@mui/icons-material/ThumbDown";
import { useSubmitStationProposal } from "@/hooks";
import L from "leaflet";
import type { CoverageGapDTO, StationProposalSummary } from "@/types";
import { safeJsonParse } from "@/utils/safeJsonParse";
import "leaflet/dist/leaflet.css";

const COVERAGE_FILL: Record<string, string> = {
  NO_COVERAGE: "#ef4444",
  POOR_COVERAGE: "#f97316",
  PARTIAL_COVERAGE: "#eab308",
  ADEQUATE: "#22c55e",
};

const CAT_LABEL: Record<string, string> = {
  NO_COVERAGE: "No Coverage (>3 km)",
  POOR_COVERAGE: "Poor Coverage (1–3 km)",
  PARTIAL_COVERAGE: "Partial Coverage (500 m–1 km)",
  ADEQUATE: "Adequate (<500 m)",
};

const DUBLIN_BOUNDS = L.latLngBounds([
  [53.22, -6.55],
  [53.55, -5.95],
]);

// ── Helpers ───────────────────────────────────────────────────────────────────

function haversineM(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number,
): number {
  const R = 6_371_000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function categorize(
  flatCount: number,
  houseCount: number,
  distM: number | null,
): string {
  if (distM === null) return "NO_COVERAGE";
  if (distM > 5000) return "NO_COVERAGE";
  if (flatCount > 50 && distM > 3000) return "NO_COVERAGE";
  if (houseCount > 200 && distM > 3000) return "NO_COVERAGE";
  if (flatCount > 50 && distM > 1000) return "POOR_COVERAGE";
  if (houseCount > 200 && distM > 2000) return "POOR_COVERAGE";
  if (flatCount > 50 && distM > 500) return "PARTIAL_COVERAGE";
  if (houseCount > 200 && distM > 1000) return "PARTIAL_COVERAGE";
  return "ADEQUATE";
}

// ── Sub-components ────────────────────────────────────────────────────────────

function FitDublin() {
  const map = useMap();
  useEffect(() => {
    map.fitBounds(DUBLIN_BOUNDS, { padding: [10, 10] });
  }, [map]);
  return null;
}

function MapClickHandler({
  active,
  onPlace,
}: {
  active: boolean;
  onPlace: (lat: number, lon: number) => void;
}) {
  // react-leaflet v5 registers handlers once on mount (empty-deps useEffect),
  // so we must use refs to always read the latest prop values.
  const activeRef = useRef(active);
  const onPlaceRef = useRef(onPlace);
  useEffect(() => {
    activeRef.current = active;
  }, [active]);
  useEffect(() => {
    onPlaceRef.current = onPlace;
  }, [onPlace]);

  useMapEvents({
    click(e) {
      if (activeRef.current) onPlaceRef.current(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

// ── Types ─────────────────────────────────────────────────────────────────────

type CategoryFilter =
  | "ALL"
  | "NO_COVERAGE"
  | "POOR_COVERAGE"
  | "PARTIAL_COVERAGE"
  | "ADEQUATE";

interface CoverageGapMapProps {
  gaps: CoverageGapDTO[];
  height?: number | string;
  darkTiles?: boolean;
  /** When set, enters read-only review mode with this proposal's stations pre-loaded. */
  reviewProposal?: StationProposalSummary | null;
  onAccept?: (proposalId: number) => void;
  onReject?: (proposalId: number, reason: string) => void;
  isReviewing?: boolean;
  /** Only Cycle_Provider may submit proposals. */
  canSubmit?: boolean;
  /** Role of the current reviewer — controls the primary action button label. */
  reviewerRole?: string;
  /** Top offset (px) to push the legend below a floating tray above it. */
  legendTopOffset?: number;
}

// ── Main component ────────────────────────────────────────────────────────────

export const CoverageGapMap = ({
  gaps,
  height = "100%",
  darkTiles = true,
  reviewProposal,
  onAccept,
  onReject,
  isReviewing = false,
  canSubmit = false,
  reviewerRole,
  legendTopOffset,
}: CoverageGapMapProps) => {
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>("ALL");
  const [simulateMode, setSimulateMode] = useState(false);
  const [proposedStations, setProposedStations] = useState<[number, number][]>(
    [],
  );
  const [submitted, setSubmitted] = useState(false);
  const [rejectMode, setRejectMode] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const { mutate: submitProposal, isPending: isSubmitting } =
    useSubmitStationProposal();

  // Refs so GeoJSON onEachFeature handlers (registered once on mount) always
  // read the latest simulate state without needing the layer to re-create.
  const simulateModeRef = useRef(simulateMode);
  const isReviewModeRef = useRef(!!reviewProposal);
  useEffect(() => {
    simulateModeRef.current = simulateMode;
  }, [simulateMode]);
  useEffect(() => {
    isReviewModeRef.current = !!reviewProposal;
  }, [reviewProposal]);
  const placeStationRef = useRef((lat: number, lon: number) => {
    setProposedStations((prev) => [...prev, [lat, lon]]);
  });

  const isReviewMode = !!reviewProposal;

  // Sync pre-loaded stations when reviewProposal changes.
  // Multiple synchronous setState calls here are intentional: they batch into a single render
  // cycle in React 18+ and keep the proposal review state consistent.
  useEffect(() => {
    if (reviewProposal) {
      const stationSchema = {
        parse: (data: unknown) =>
          (data as Array<{ lat: number; lon: number }>).map(
            (s) => [s.lat, s.lon] as [number, number],
          ),
      };
      const parsed = safeJsonParse(reviewProposal.stationsJson, stationSchema);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setProposedStations(parsed);
      setSimulateMode(true);
      setSubmitted(false);
      setRejectMode(false);
      setRejectReason("");
    } else {
      setProposedStations([]);
      setSimulateMode(false);
      setSubmitted(false);
      setRejectMode(false);
      setRejectReason("");
    }
  }, [reviewProposal]);

  const defaultCenter: [number, number] = [53.3498, -6.2603];

  const tileUrl = darkTiles
    ? "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
    : "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

  const tileAttribution = darkTiles
    ? '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/">CARTO</a>'
    : '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

  // Compute simulated category for each gap
  const gapsWithSim = gaps.map((g) => {
    if (proposedStations.length === 0)
      return {
        ...g,
        simCategory: g.coverageCategory,
        simDistM: g.minDistanceM,
      };
    const minProposedDist = Math.min(
      ...proposedStations.map(([lat, lon]) =>
        haversineM(g.centroidLat, g.centroidLon, lat, lon),
      ),
    );
    const simDistM =
      g.minDistanceM == null
        ? minProposedDist
        : Math.min(g.minDistanceM, minProposedDist);
    return {
      ...g,
      simDistM,
      simCategory: categorize(
        g.flatApartmentCount,
        g.houseBungalowCount,
        simDistM,
      ),
    };
  });

  const improvedCount = gapsWithSim.filter(
    (g) => g.simCategory !== g.coverageCategory,
  ).length;

  const filtered = (simulateMode ? gapsWithSim : gapsWithSim).filter((g) => {
    const cat = simulateMode ? g.simCategory : g.coverageCategory;
    return categoryFilter === "ALL" || cat === categoryFilter;
  });

  const handleToggleSimulate = () => {
    if (isReviewMode) return; // cannot toggle in review mode
    setSimulateMode((v) => !v);
    if (simulateMode) {
      setProposedStations([]);
      setSubmitted(false);
    }
  };

  const handleSubmitProposal = () => {
    const impactedAreas = gapsWithSim
      .filter((g) => g.simCategory !== g.coverageCategory)
      .map((g) => ({
        electoralDivision: g.electoralDivision,
        fromCategory: g.coverageCategory,
        toCategory: g.simCategory,
        simulatedDistanceM: g.simDistM ?? 0,
      }));

    submitProposal(
      {
        proposedStations: proposedStations.map(([lat, lon]) => ({
          latitude: lat,
          longitude: lon,
        })),
        impactedAreas,
        totalImprovedAreas: improvedCount,
        submittedBy: localStorage.getItem("username") ?? "unknown",
      },
      { onSuccess: () => setSubmitted(true) },
    );
  };

  return (
    <Box
      sx={{
        height,
        width: "100%",
        position: "relative",
        cursor: simulateMode ? "crosshair" : "default",
      }}
    >
      {/* Filter buttons */}
      <Box
        sx={{
          position: "absolute",
          top: 16,
          left: "50%",
          transform: "translateX(-50%)",
          zIndex: 1000,
        }}
      >
        <ToggleButtonGroup
          value={categoryFilter}
          exclusive
          onChange={(_, v) => {
            if (v) setCategoryFilter(v);
          }}
          size="small"
          sx={{
            bgcolor: darkTiles
              ? "rgba(20,20,20,0.85)"
              : "rgba(255,255,255,0.9)",
            backdropFilter: "blur(8px)",
            borderRadius: 2,
            "& .MuiToggleButton-root": {
              fontSize: "0.7rem",
              textTransform: "none",
              px: 1.5,
              py: 0.5,
              border: "none",
              color: darkTiles ? "#cbd5e1" : "text.secondary",
              "&.Mui-selected": { color: "#fff", bgcolor: "transparent" },
            },
          }}
        >
          <ToggleButton
            value="ALL"
            sx={{
              "&.Mui-selected": {
                bgcolor: "rgba(148,163,184,0.4) !important",
                color: "#fff !important",
              },
            }}
          >
            All
          </ToggleButton>
          <ToggleButton
            value="NO_COVERAGE"
            sx={{
              "&.Mui-selected": {
                bgcolor: `${COVERAGE_FILL.NO_COVERAGE} !important`,
              },
            }}
          >
            No Coverage
          </ToggleButton>
          <ToggleButton
            value="POOR_COVERAGE"
            sx={{
              "&.Mui-selected": {
                bgcolor: `${COVERAGE_FILL.POOR_COVERAGE} !important`,
              },
            }}
          >
            Poor Coverage
          </ToggleButton>
          <ToggleButton
            value="PARTIAL_COVERAGE"
            sx={{
              "&.Mui-selected": {
                bgcolor: `${COVERAGE_FILL.PARTIAL_COVERAGE} !important`,
              },
            }}
          >
            Partial Coverage
          </ToggleButton>
          <ToggleButton
            value="ADEQUATE"
            sx={{
              "&.Mui-selected": {
                bgcolor: `${COVERAGE_FILL.ADEQUATE} !important`,
              },
            }}
          >
            Adequate
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {/* Simulate toggle button — hidden in review mode */}
      {!isReviewMode && (
        <Box
          sx={{
            position: "absolute",
            top: 16,
            left: 16,
            zIndex: 1000,
            display: "flex",
            gap: 1,
          }}
        >
          {simulateMode && proposedStations.length > 0 && (
            <Button
              size="small"
              variant="outlined"
              startIcon={<CancelIcon />}
              onClick={() => setProposedStations([])}
              sx={{
                fontSize: "0.7rem",
                textTransform: "none",
                borderColor: "#94a3b8",
                color: darkTiles ? "#cbd5e1" : "text.secondary",
                bgcolor: darkTiles
                  ? "rgba(20,20,20,0.85)"
                  : "rgba(255,255,255,0.9)",
                backdropFilter: "blur(8px)",
                "&:hover": { borderColor: "#ef4444", color: "#ef4444" },
              }}
            >
              Clear ({proposedStations.length})
            </Button>
          )}
          <Button
            size="small"
            variant={simulateMode ? "contained" : "outlined"}
            startIcon={<AddLocationAltIcon />}
            onClick={handleToggleSimulate}
            sx={{
              fontSize: "0.7rem",
              textTransform: "none",
              bgcolor: simulateMode
                ? "#3b82f6"
                : darkTiles
                  ? "rgba(20,20,20,0.85)"
                  : "rgba(255,255,255,0.9)",
              borderColor: simulateMode ? "#3b82f6" : "#94a3b8",
              color: simulateMode
                ? "#fff"
                : darkTiles
                  ? "#cbd5e1"
                  : "text.secondary",
              backdropFilter: "blur(8px)",
              "&:hover": { bgcolor: simulateMode ? "#2563eb" : undefined },
            }}
          >
            {simulateMode ? "Exit Simulate" : "Simulate Station"}
          </Button>
        </Box>
      )}

      {/* Review mode badge */}
      {isReviewMode && (
        <Chip
          label={`Reviewing proposal from ${reviewProposal?.submittedBy ?? ""}`}
          size="small"
          sx={{
            position: "absolute",
            top: 16,
            left: 16,
            zIndex: 1000,
            bgcolor: "rgba(251,191,36,0.2)",
            color: "#fbbf24",
            fontWeight: 700,
            fontSize: "0.68rem",
            border: "1px solid rgba(251,191,36,0.4)",
          }}
        />
      )}

      {/* Simulate mode hint */}
      {simulateMode && !isReviewMode && (
        <Chip
          label="Click anywhere on the map to place a proposed station"
          size="small"
          sx={{
            position: "absolute",
            bottom: 80,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 1000,
            bgcolor: "#3b82f6",
            color: "#fff",
            fontSize: "0.7rem",
            fontWeight: 600,
          }}
        />
      )}

      {/* Improvement panel — always shown in review mode; otherwise requires at least one improvement */}
      {simulateMode &&
        proposedStations.length > 0 &&
        (improvedCount > 0 || isReviewMode) &&
        (() => {
          // Group improvements by "from → to" transition
          const groups: Record<
            string,
            { from: string; to: string; areas: string[] }
          > = {};
          for (const g of gapsWithSim) {
            if (g.simCategory === g.coverageCategory) continue;
            const key = `${g.coverageCategory}__${g.simCategory}`;
            if (!groups[key])
              groups[key] = {
                from: g.coverageCategory,
                to: g.simCategory,
                areas: [],
              };
            groups[key].areas.push(g.electoralDivision);
          }
          const entries = Object.values(groups).toSorted(
            (a, b) => b.areas.length - a.areas.length,
          );
          return (
            <Paper
              elevation={0}
              sx={{
                position: "absolute",
                bottom: 24,
                left: 16,
                zIndex: 1000,
                width: 280,
                maxHeight: 360,
                borderRadius: 2,
                overflow: "hidden",
                display: "flex",
                flexDirection: "column",
                bgcolor: darkTiles
                  ? "rgba(20,20,20,0.92)"
                  : "rgba(255,255,255,0.95)",
                backdropFilter: "blur(10px)",
                border: "1px solid rgba(34,197,94,0.4)",
              }}
            >
              {/* Header */}
              <Box
                sx={{
                  px: 1.5,
                  py: 1,
                  borderBottom: "1px solid rgba(255,255,255,0.08)",
                }}
              >
                <Typography
                  variant="caption"
                  fontWeight={700}
                  sx={{
                    color: isReviewMode ? "#fbbf24" : "#22c55e",
                    fontSize: "0.72rem",
                  }}
                >
                  {isReviewMode ? "Proposal Impact" : "Simulation Impact"}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{
                    display: "block",
                    color: darkTiles ? "#94a3b8" : "text.secondary",
                    fontSize: "0.65rem",
                  }}
                >
                  {improvedCount > 0
                    ? `${improvedCount} area${improvedCount > 1 ? "s" : ""} improved with ${proposedStations.length} proposed station${proposedStations.length > 1 ? "s" : ""}`
                    : `${proposedStations.length} proposed station${proposedStations.length > 1 ? "s" : ""} — no coverage change detected`}
                </Typography>
              </Box>

              {/* Groups */}
              <Box sx={{ flex: 1, overflow: "auto", px: 1.5, py: 0.75, pb: 0 }}>
                {entries.map((entry) => (
                  <Box key={`${entry.from}-${entry.to}`} sx={{ mb: 1.25 }}>
                    {/* Transition label */}
                    <Box
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 0.5,
                        mb: 0.5,
                      }}
                    >
                      <Box
                        sx={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          bgcolor: COVERAGE_FILL[entry.from],
                          flexShrink: 0,
                        }}
                      />
                      <Typography
                        variant="caption"
                        sx={{
                          fontSize: "0.65rem",
                          color: COVERAGE_FILL[entry.from],
                          fontWeight: 700,
                        }}
                      >
                        {entry.from.replace("_", " ")}
                      </Typography>
                      <Typography
                        variant="caption"
                        sx={{
                          fontSize: "0.65rem",
                          color: darkTiles ? "#64748b" : "text.disabled",
                        }}
                      >
                        →
                      </Typography>
                      <Box
                        sx={{
                          width: 8,
                          height: 8,
                          borderRadius: "50%",
                          bgcolor: COVERAGE_FILL[entry.to],
                          flexShrink: 0,
                        }}
                      />
                      <Typography
                        variant="caption"
                        sx={{
                          fontSize: "0.65rem",
                          color: COVERAGE_FILL[entry.to],
                          fontWeight: 700,
                        }}
                      >
                        {entry.to.replace("_", " ")}
                      </Typography>
                      <Chip
                        label={entry.areas.length}
                        size="small"
                        sx={{
                          ml: "auto",
                          height: 16,
                          fontSize: "0.6rem",
                          bgcolor: COVERAGE_FILL[entry.to] + "33",
                          color: COVERAGE_FILL[entry.to],
                          fontWeight: 700,
                        }}
                      />
                    </Box>
                    {/* Area names */}
                    {entry.areas.map((name) => (
                      <Box
                        key={name}
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          gap: 0.5,
                          pl: 1,
                          py: 0.15,
                        }}
                      >
                        <Box
                          sx={{
                            width: 4,
                            height: 4,
                            borderRadius: "50%",
                            bgcolor: COVERAGE_FILL[entry.to],
                            flexShrink: 0,
                          }}
                        />
                        <Typography
                          variant="caption"
                          sx={{
                            fontSize: "0.62rem",
                            color: darkTiles ? "#94a3b8" : "text.secondary",
                            lineHeight: 1.5,
                          }}
                        >
                          {name}
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                ))}
              </Box>

              {/* Action buttons */}
              <Box
                sx={{
                  px: 1.5,
                  py: 1,
                  borderTop: "1px solid rgba(255,255,255,0.08)",
                }}
              >
                {isReviewMode ? (
                  // ── Review mode: Accept / Reject ──
                  rejectMode ? (
                    <Box
                      sx={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 0.75,
                      }}
                    >
                      <TextField
                        size="small"
                        placeholder="Reason for rejection..."
                        value={rejectReason}
                        onChange={(e) => setRejectReason(e.target.value)}
                        multiline
                        minRows={2}
                        fullWidth
                        sx={{
                          "& .MuiInputBase-root": {
                            fontSize: "0.7rem",
                            bgcolor: "rgba(255,255,255,0.05)",
                          },
                        }}
                      />
                      <Box sx={{ display: "flex", gap: 0.75 }}>
                        <Button
                          size="small"
                          variant="outlined"
                          onClick={() => {
                            setRejectMode(false);
                            setRejectReason("");
                          }}
                          sx={{
                            flex: 1,
                            fontSize: "0.68rem",
                            textTransform: "none",
                            borderColor: "#475569",
                            color: "#94a3b8",
                          }}
                        >
                          Cancel
                        </Button>
                        <Button
                          size="small"
                          variant="contained"
                          disabled={!rejectReason.trim() || isReviewing}
                          onClick={() =>
                            onReject?.(
                              reviewProposal?.id ?? 0,
                              rejectReason.trim(),
                            )
                          }
                          sx={{
                            flex: 1,
                            fontSize: "0.68rem",
                            textTransform: "none",
                            bgcolor: "#ef4444",
                            "&:hover": { bgcolor: "#dc2626" },
                          }}
                        >
                          Confirm Reject
                        </Button>
                      </Box>
                    </Box>
                  ) : (
                    <Box sx={{ display: "flex", gap: 0.75 }}>
                      <Button
                        size="small"
                        variant="contained"
                        fullWidth
                        startIcon={
                          reviewerRole === "Cycle_Admin" ? (
                            <SendIcon sx={{ fontSize: "0.85rem" }} />
                          ) : (
                            <CheckCircleIcon sx={{ fontSize: "0.85rem" }} />
                          )
                        }
                        disabled={isReviewing}
                        onClick={() => onAccept?.(reviewProposal?.id ?? 0)}
                        sx={{
                          fontSize: "0.7rem",
                          textTransform: "none",
                          fontWeight: 700,
                          bgcolor: "#22c55e",
                          "&:hover": { bgcolor: "#16a34a" },
                        }}
                      >
                        {reviewerRole === "Cycle_Admin"
                          ? "Forward to City Manager"
                          : "Approve"}
                      </Button>
                      <Button
                        size="small"
                        variant="outlined"
                        fullWidth
                        startIcon={
                          <ThumbDownIcon sx={{ fontSize: "0.85rem" }} />
                        }
                        disabled={isReviewing}
                        onClick={() => setRejectMode(true)}
                        sx={{
                          fontSize: "0.7rem",
                          textTransform: "none",
                          fontWeight: 700,
                          borderColor: "#ef4444",
                          color: "#ef4444",
                          "&:hover": { bgcolor: "rgba(239,68,68,0.1)" },
                        }}
                      >
                        Reject
                      </Button>
                    </Box>
                  )
                ) : submitted ? (
                  <Typography
                    variant="caption"
                    sx={{
                      color: "#22c55e",
                      fontWeight: 700,
                      fontSize: "0.7rem",
                    }}
                  >
                    ✓ Proposal sent to Cycle Admin
                  </Typography>
                ) : canSubmit ? (
                  <Button
                    fullWidth
                    size="small"
                    variant="contained"
                    startIcon={<SendIcon sx={{ fontSize: "0.85rem" }} />}
                    disabled={isSubmitting}
                    onClick={handleSubmitProposal}
                    sx={{
                      fontSize: "0.7rem",
                      textTransform: "none",
                      fontWeight: 700,
                      bgcolor: "#3b82f6",
                      "&:hover": { bgcolor: "#2563eb" },
                    }}
                  >
                    {isSubmitting
                      ? "Submitting..."
                      : "Submit Proposal to Cycle Admin"}
                  </Button>
                ) : null}
              </Box>
            </Paper>
          );
        })()}

      <MapContainer
        center={defaultCenter}
        zoom={12}
        style={{ height: "100%", width: "100%" }}
        zoomControl={false}
      >
        <TileLayer attribution={tileAttribution} url={tileUrl} />
        <FitDublin />
        <MapClickHandler
          active={simulateMode && !isReviewMode}
          onPlace={(lat, lon) =>
            setProposedStations((prev) => [...prev, [lat, lon]])
          }
        />

        {/* Gap polygons — GeoJSON fill, fallback to CircleMarker if geomGeoJson is absent */}
        {filtered.map((g) => {
          const displayCat = simulateMode ? g.simCategory : g.coverageCategory;
          const improved = simulateMode && g.simCategory !== g.coverageCategory;
          const color = COVERAGE_FILL[displayCat] ?? "#94a3b8";
          const distText =
            g.minDistanceM == null
              ? "N/A"
              : `${(g.minDistanceM / 1000).toFixed(2)} km`;
          const simDistText =
            improved && g.simDistM != null
              ? `${(g.simDistM / 1000).toFixed(2)} km`
              : null;

          const popupContent = (
            <div
              style={{ minWidth: 210, fontFamily: "sans-serif", fontSize: 13 }}
            >
              <strong>{g.electoralDivision}</strong>
              <br />
              {improved ? (
                <span style={{ fontSize: 12 }}>
                  <span
                    style={{
                      color: COVERAGE_FILL[g.coverageCategory],
                      fontWeight: 600,
                      textDecoration: "line-through",
                    }}
                  >
                    {CAT_LABEL[g.coverageCategory] ?? g.coverageCategory}
                  </span>
                  {" → "}
                  <span style={{ color, fontWeight: 700 }}>
                    {CAT_LABEL[displayCat] ?? displayCat}
                  </span>
                </span>
              ) : (
                <span style={{ color, fontWeight: 600 }}>
                  {CAT_LABEL[displayCat] ?? displayCat}
                </span>
              )}
              <hr
                style={{
                  margin: "6px 0",
                  border: "none",
                  borderTop: "1px solid #ccc",
                }}
              />
              <b>Apartments / Flats:</b>{" "}
              {Number(g.flatApartmentCount).toLocaleString()}
              <br />
              <b>Houses / Bungalows:</b>{" "}
              {Number(g.houseBungalowCount).toLocaleString()}
              <br />
              <b>Total Dwellings:</b>{" "}
              {Number(g.totalDwellings).toLocaleString()}
              <br />
              <b>Nearest station:</b> {distText}
              {simDistText && (
                <>
                  <br />
                  <span style={{ color: "#22c55e" }}>
                    <b>With proposed:</b> {simDistText}
                  </span>
                </>
              )}
              {g.processedForImplementation && (
                <>
                  <br />
                  <span style={{ color: "#22c55e" }}>
                    ✓ Planned for implementation
                  </span>
                </>
              )}
            </div>
          );

          if (g.geomGeoJson) {
            type GeoJsonData = Exclude<
              NonNullable<Parameters<typeof L.geoJSON>[0]>,
              unknown[]
            >;
            const geomData = safeJsonParse<GeoJsonData>(g.geomGeoJson, {
              parse: (data) => {
                if (
                  typeof data !== "object" ||
                  data === null ||
                  !("type" in data)
                ) {
                  throw new Error("Invalid GeoJSON: missing type field");
                }
                return data as GeoJsonData;
              },
            });
            const baseStyle: PathOptions = {
              fillColor: color,
              fillOpacity: improved ? 0.65 : 0.45,
              color: improved ? "#fff" : color,
              weight: improved ? 2 : 1,
              dashArray: improved ? "6 4" : undefined,
            };
            return (
              <GeoJSON
                key={`${g.electoralDivision}-${displayCat}`}
                data={geomData}
                style={baseStyle}
                onEachFeature={(_feature, layer: Layer) => {
                  layer.bindPopup(
                    () => {
                      const div = document.createElement("div");
                      // Render popup via a temporary React root isn't feasible here,
                      // so we replicate the content as HTML string.
                      div.innerHTML = `
                        <div style="min-width:210px;font-family:sans-serif;font-size:13px">
                          <strong>${g.electoralDivision}</strong><br/>
                          ${
                            improved
                              ? `<span style="font-size:12px">
                                  <span style="color:${COVERAGE_FILL[g.coverageCategory]};font-weight:600;text-decoration:line-through">
                                    ${CAT_LABEL[g.coverageCategory] ?? g.coverageCategory}
                                  </span>
                                  &rarr;
                                  <span style="color:${color};font-weight:700">
                                    ${CAT_LABEL[displayCat] ?? displayCat}
                                  </span>
                                </span>`
                              : `<span style="color:${color};font-weight:600">${CAT_LABEL[displayCat] ?? displayCat}</span>`
                          }
                          <hr style="margin:6px 0;border:none;border-top:1px solid #ccc"/>
                          <b>Apartments / Flats:</b> ${Number(g.flatApartmentCount).toLocaleString()}<br/>
                          <b>Houses / Bungalows:</b> ${Number(g.houseBungalowCount).toLocaleString()}<br/>
                          <b>Total Dwellings:</b> ${Number(g.totalDwellings).toLocaleString()}<br/>
                          <b>Nearest station:</b> ${distText}
                          ${simDistText ? `<br/><span style="color:#22c55e"><b>With proposed:</b> ${simDistText}</span>` : ""}
                          ${g.processedForImplementation ? `<br/><span style="color:#22c55e">✓ Planned for implementation</span>` : ""}
                        </div>`;
                      return div;
                    },
                    { maxWidth: 280 },
                  );
                  layer.on({
                    click(e: LeafletMouseEvent) {
                      if (simulateModeRef.current && !isReviewModeRef.current) {
                        L.DomEvent.stopPropagation(e);
                        placeStationRef.current(e.latlng.lat, e.latlng.lng);
                      }
                    },
                    mouseover(e: LeafletMouseEvent) {
                      (e.target as L.Path).setStyle({ fillOpacity: 0.75 });
                    },
                    mouseout(e: LeafletMouseEvent) {
                      (e.target as L.Path).setStyle({
                        fillOpacity: improved ? 0.65 : 0.45,
                      });
                    },
                  });
                }}
              />
            );
          }

          // Fallback for missing geometry
          return (
            <Fragment key={g.electoralDivision}>
              <CircleMarker
                center={[g.centroidLat, g.centroidLon]}
                radius={14}
                pathOptions={{
                  fillColor: color,
                  fillOpacity: 0.65,
                  color: "#fff",
                  weight: improved ? 3 : 2,
                  dashArray: improved ? "5 3" : undefined,
                }}
              >
                <Popup>{popupContent}</Popup>
              </CircleMarker>
            </Fragment>
          );
        })}

        {/* Proposed station markers */}
        {proposedStations.map(([lat, lon], i) => (
          <Fragment key={`proposed-${i}`}>
            <CircleMarker
              center={[lat, lon]}
              radius={22}
              interactive={false}
              pathOptions={{
                fillColor: "#3b82f6",
                fillOpacity: 0.15,
                color: "#3b82f6",
                weight: 1.5,
                dashArray: "4 3",
              }}
            />
            <CircleMarker
              center={[lat, lon]}
              radius={10}
              pathOptions={{
                fillColor: "#3b82f6",
                fillOpacity: 0.95,
                color: "#fff",
                weight: 2.5,
              }}
            >
              <Popup>
                <div style={{ fontFamily: "sans-serif", fontSize: 13 }}>
                  <strong style={{ color: "#3b82f6" }}>
                    Proposed Station #{i + 1}
                  </strong>
                  <br />
                  <span style={{ fontSize: 11, color: "#64748b" }}>
                    {lat.toFixed(5)}, {lon.toFixed(5)}
                  </span>
                </div>
              </Popup>
            </CircleMarker>
          </Fragment>
        ))}
      </MapContainer>

      {/* Legend — positioned below the proposal tray when present, otherwise bottom-left */}
      <Paper
        elevation={0}
        sx={{
          position: "absolute",
          ...(legendTopOffset == null
            ? simulateMode && proposedStations.length > 0 && improvedCount > 0
              ? { top: 60, left: 16 }
              : { bottom: 24, left: 16 }
            : { top: legendTopOffset, left: 16 }),
          zIndex: 1000,
          px: 1.5,
          py: 1,
          borderRadius: 2,
          bgcolor: darkTiles ? "rgba(20,20,20,0.85)" : "rgba(255,255,255,0.9)",
          backdropFilter: "blur(8px)",
        }}
      >
        <Typography
          variant="caption"
          fontWeight={700}
          sx={{
            display: "block",
            mb: 0.5,
            color: darkTiles ? "#e2e8f0" : "text.primary",
          }}
        >
          {categoryFilter === "ALL"
            ? "All Coverage Gaps"
            : CAT_LABEL[categoryFilter]}
        </Typography>
        {(
          [
            "NO_COVERAGE",
            "POOR_COVERAGE",
            "PARTIAL_COVERAGE",
            "ADEQUATE",
          ] as const
        ).map((cat) => (
          <Box
            key={cat}
            sx={{ display: "flex", alignItems: "center", gap: 0.75, mb: 0.25 }}
          >
            <Box
              sx={{
                width: 12,
                height: 12,
                borderRadius: 0.5,
                bgcolor: COVERAGE_FILL[cat],
                opacity: 0.75,
                flexShrink: 0,
              }}
            />
            <Typography
              variant="caption"
              sx={{
                color: darkTiles ? "#cbd5e1" : "text.secondary",
                fontSize: "0.65rem",
              }}
            >
              {CAT_LABEL[cat]}
            </Typography>
          </Box>
        ))}
        {simulateMode && (
          <Box
            sx={{ display: "flex", alignItems: "center", gap: 0.75, mt: 0.5 }}
          >
            <Box
              sx={{
                width: 10,
                height: 10,
                borderRadius: "50%",
                bgcolor: "#3b82f6",
                flexShrink: 0,
                border: "2px solid #fff",
              }}
            />
            <Typography
              variant="caption"
              sx={{
                color: darkTiles ? "#cbd5e1" : "text.secondary",
                fontSize: "0.65rem",
              }}
            >
              Proposed Station
            </Typography>
          </Box>
        )}
      </Paper>
    </Box>
  );
};
