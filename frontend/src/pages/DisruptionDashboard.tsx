/**
 * DisruptionDashboard — Full-viewport map with floating collapsible panels.
 * Same pattern as CycleDashboard: map background, right side panel, bottom detail panel.
 */

import { useState, useMemo } from "react";
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  Paper,
  Tab,
  Tabs,
  Tooltip,
  Typography,
} from "@mui/material";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import FiberManualRecordIcon from "@mui/icons-material/FiberManualRecord";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import TramIcon from "@mui/icons-material/Tram";
import PedalBikeIcon from "@mui/icons-material/PedalBike";
import EventIcon from "@mui/icons-material/Event";
import TrafficIcon from "@mui/icons-material/Traffic";
import CompareArrowsIcon from "@mui/icons-material/CompareArrows";
import CampaignIcon from "@mui/icons-material/Campaign";
import CloseIcon from "@mui/icons-material/Close";
import MenuOpenIcon from "@mui/icons-material/MenuOpen";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import { useQuery } from "@tanstack/react-query";
import { useActiveDisruptions } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
import type {
  ActiveDisruption,
  DisruptionAlternative,
  DisruptionCause,
  DisruptionSeverity,
  DisruptionType,
} from "@/types";
import { dashboardApi } from "@/api";
import { NetworkImpactMap } from "@/components/disruption/NetworkImpactMap";
import { RippleEffectVisualization } from "@/components/disruption/RippleEffectVisualization";

// ── Layout constants ───────────────────────────────────────────────────
const PANEL_WIDTH = 400;
const DETAIL_HEIGHT = 300;
const GAP = 16;

// ── Colour / label maps ────────────────────────────────────────────────
const SEVERITY_COLORS: Record<DisruptionSeverity, string> = {
  LOW: "#10B981",
  MEDIUM: "#F59E0B",
  HIGH: "#EF4444",
  CRITICAL: "#7C3AED",
};

const SEVERITY_ORDER: Record<DisruptionSeverity, number> = {
  CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3,
};

const TYPE_LABELS: Record<DisruptionType, string> = {
  DELAY: "Delay",
  CANCELLATION: "Cancellation",
  CONGESTION: "Congestion",
  CONSTRUCTION: "Construction",
  EVENT: "Service Pressure",
  ACCIDENT: "Accident",
  TRAM_DISRUPTION: "Tram Disruption",
};

const CAUSE_ICONS: Record<string, React.ReactNode> = {
  EVENT: <EventIcon sx={{ fontSize: 14 }} />,
  CONGESTION: <TrafficIcon sx={{ fontSize: 14 }} />,
  CROSS_MODE: <CompareArrowsIcon sx={{ fontSize: 14 }} />,
};

const CONFIDENCE_COLORS: Record<string, string> = {
  HIGH: "#EF4444", MEDIUM: "#F59E0B", LOW: "#10B981",
};

const ALT_MODE_COLORS: Record<string, string> = {
  bus: "#3B82F6",
  rail: "#F59E0B",
  bike: "#10B981",
};

type ModeFilter = "ALL" | "BUS" | "TRAM" | "TRAIN" | "CONGESTION" | "EVENT";

const MODE_TABS: { key: ModeFilter; label: string }[] = [
  { key: "ALL", label: "All" },
  { key: "BUS", label: "Bus" },
  { key: "TRAM", label: "Tram" },
  { key: "TRAIN", label: "Train" },
  { key: "CONGESTION", label: "Traffic" },
  { key: "EVENT", label: "Events" },
];

// ── Helpers ────────────────────────────────────────────────────────────

function altIcon(mode: string): React.ReactNode {
  const m = mode.toUpperCase();
  if (m.includes("BUS")) return <DirectionsBusIcon sx={{ fontSize: 14 }} />;
  if (m.includes("BIKE") || m.includes("CYCLE")) return <PedalBikeIcon sx={{ fontSize: 14 }} />;
  if (m.includes("TRAM")) return <TramIcon sx={{ fontSize: 14 }} />;
  return <TrainIcon sx={{ fontSize: 14 }} />;
}

function fmtTime(iso: string | null): string {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleTimeString("en-IE", { hour: "2-digit", minute: "2-digit" }); }
  catch { return iso; }
}

function fmtEta(iso: string | null): string {
  if (!iso) return "";
  try {
    const diff = Math.round((new Date(iso).getTime() - Date.now()) / 60_000);
    if (diff < 0) return "overdue";
    if (diff < 60) return `~${diff} min`;
    return `~${Math.round(diff / 60)}h`;
  } catch { return ""; }
}

function matchesMode(d: ActiveDisruption, f: ModeFilter): boolean {
  if (f === "ALL") return true;
  if (f === "EVENT") return d.disruptionType === "EVENT";
  if (f === "CONGESTION") return d.disruptionType === "CONGESTION";
  return d.affectedTransportModes?.includes(f) ?? false;
}

// ── Disruption row ─────────────────────────────────────────────────────

function DisruptionRow({
  d, selected, onClick,
}: { d: ActiveDisruption; selected: boolean; onClick: () => void }) {
  const color = SEVERITY_COLORS[d.severity] ?? "#6B7280";
  return (
    <Box
      onClick={onClick}
      sx={{
        px: 2, py: 1,
        cursor: "pointer",
        bgcolor: selected ? `${color}12` : "transparent",
        borderLeft: `3px solid ${selected ? color : "transparent"}`,
        "&:hover": { bgcolor: selected ? `${color}18` : "rgba(0,0,0,0.025)" },
        transition: "all 0.12s",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "flex-start", gap: 1 }}>
        <Box sx={{ mt: 0.55, width: 8, height: 8, borderRadius: "50%", bgcolor: color, flexShrink: 0, boxShadow: `0 0 5px ${color}99` }} />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            <Typography noWrap sx={{ fontSize: "0.82rem", fontWeight: selected ? 600 : 500, flex: 1 }}>
              {d.affectedArea ?? d.name}
            </Typography>
            {d.notificationSent && (
              <Tooltip title="Notification sent" arrow>
                <CheckCircleOutlineIcon sx={{ fontSize: 12, color: "#10B981", flexShrink: 0 }} />
              </Tooltip>
            )}
          </Box>

          {d.description && (
            <Typography sx={{
              fontSize: "0.7rem", color: "text.disabled", mt: 0.2, lineHeight: 1.35,
              display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden",
            }}>
              {d.description}
            </Typography>
          )}

          <Box sx={{ display: "flex", gap: 0.4, mt: 0.35, flexWrap: "wrap", alignItems: "center" }}>
            <Chip size="small" label={d.severity} sx={{ fontSize: "0.56rem", height: 14, bgcolor: `${color}22`, color, border: `1px solid ${color}44` }} />
            <Chip size="small" label={TYPE_LABELS[d.disruptionType] ?? d.disruptionType} sx={{ fontSize: "0.56rem", height: 14 }} />
            {d.disruptionType === "EVENT" && (
              <Chip
                size="small"
                icon={<CampaignIcon sx={{ fontSize: "0.65rem !important" }} />}
                label="Operator Alert"
                variant="outlined"
                sx={{ fontSize: "0.53rem", height: 14, borderColor: "#F59E0B88", color: "#F59E0B" }}
              />
            )}
            {(d.affectedTransportModes ?? []).slice(0, 2).map((m) => (
              <Chip key={m} size="small" label={m} sx={{ fontSize: "0.53rem", height: 13 }} />
            ))}
            {(d.disruptionType === "CONGESTION" || d.disruptionType === "EVENT") &&
              (d.affectedRoutes ?? []).length > 0 && (() => {
                const routes = d.affectedRoutes!;
                const shown = routes.slice(0, 3);
                const extra = routes.length - shown.length;
                return (
                  <>
                    {shown.map((r) => (
                      <Chip key={r} size="small" label={r} sx={{ fontSize: "0.53rem", height: 13, bgcolor: "rgba(0,0,0,0.08)", color: "text.secondary" }} />
                    ))}
                    {extra > 0 && (
                      <Typography sx={{ fontSize: "0.53rem", color: "text.disabled" }}>+{extra} more</Typography>
                    )}
                  </>
                );
              })()}
            {d.delayMinutes != null && d.delayMinutes > 0 && (
              <Typography sx={{ fontSize: "0.62rem", color, ml: "auto" }}>+{d.delayMinutes} min</Typography>
            )}
          </Box>

          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mt: 0.3 }}>
            <AccessTimeIcon sx={{ fontSize: 9, color: "text.disabled" }} />
            <Typography sx={{ fontSize: "0.6rem", color: "text.disabled" }}>{fmtTime(d.detectedAt)}</Typography>
            {d.estimatedEndTime && (
              <Typography sx={{ fontSize: "0.6rem", color: "text.disabled" }}>· ETA {fmtEta(d.estimatedEndTime)}</Typography>
            )}
          </Box>
        </Box>
      </Box>
    </Box>
  );
}

// ── Severity section header ────────────────────────────────────────────

function SectionHeader({ label, color, count }: { label: string; color: string; count: number }) {
  return (
    <Box sx={{
      px: 2, py: 0.4,
      display: "flex", alignItems: "center", gap: 0.75,
      bgcolor: `${color}0d`,
      borderTop: "1px solid rgba(0,0,0,0.05)",
      position: "sticky", top: 0, zIndex: 1,
    }}>
      <Box sx={{ width: 6, height: 6, borderRadius: "50%", bgcolor: color }} />
      <Typography sx={{ fontSize: "0.58rem", fontWeight: 700, color, textTransform: "uppercase", letterSpacing: 0.8 }}>
        {label}
      </Typography>
      <Typography sx={{ fontSize: "0.56rem", color: `${color}bb`, ml: "auto" }}>{count}</Typography>
    </Box>
  );
}

// ── Detail panel ───────────────────────────────────────────────────────

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <Typography sx={{ fontSize: "0.58rem", fontWeight: 700, color: "text.disabled", textTransform: "uppercase", letterSpacing: 0.7, mb: 0.75 }}>
      {children}
    </Typography>
  );
}

function DetailPanel({ id, onClose }: { id: number; onClose: () => void }) {
  const { data, isLoading } = useQuery({
    queryKey: ["disruption", "detail", id],
    queryFn: () => dashboardApi.getDisruptionById(id),
    staleTime: 30_000,
  });
  const color = data ? (SEVERITY_COLORS[data.severity] ?? "#6B7280") : "#6B7280";
  const causes: DisruptionCause[] = data?.causes ?? [];
  const alternatives: DisruptionAlternative[] = data?.alternatives ?? [];

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Header */}
      <Box sx={{
        px: 2, py: 1,
        display: "flex", alignItems: "center", gap: 1,
        borderBottom: "1px solid rgba(0,0,0,0.07)",
        flexShrink: 0,
        bgcolor: `${color}08`,
      }}>
        <Box sx={{ width: 8, height: 8, borderRadius: "50%", bgcolor: color, flexShrink: 0, boxShadow: `0 0 6px ${color}88` }} />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="subtitle2" fontWeight={700} noWrap>
            {data?.affectedArea ?? data?.name ?? "Disruption Detail"}
          </Typography>
          {data && (
            <Box sx={{ display: "flex", gap: 0.4, mt: 0.2, alignItems: "center" }}>
              <Chip size="small" label={data.severity} sx={{ fontSize: "0.54rem", height: 13, bgcolor: `${color}22`, color, border: `1px solid ${color}44` }} />
              <Chip size="small" label={TYPE_LABELS[data.disruptionType] ?? data.disruptionType} sx={{ fontSize: "0.54rem", height: 13 }} />
            </Box>
          )}
        </Box>
        <IconButton size="small" onClick={onClose}>
          <KeyboardArrowDownIcon fontSize="small" />
        </IconButton>
      </Box>

      {isLoading ? (
        <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", flex: 1 }}>
          <CircularProgress size={18} sx={{ color }} />
        </Box>
      ) : (
        <Box sx={{ flex: 1, overflow: "auto", px: 2, py: 1.5, display: "flex", flexDirection: "column", gap: 1.5 }}>
          {/* Description card */}
          {data?.description && (
            <Box>
              <SectionLabel>
                {data.disruptionType === "CONGESTION"
                  ? "Impact & Recommendations"
                  : data.disruptionType === "EVENT"
                    ? "Service Pressure Alert"
                    : "What happened"}
              </SectionLabel>
              <Box sx={{
                p: 1.25, borderRadius: 1.5,
                bgcolor: `${color}0a`,
                border: `1px solid ${color}22`,
                borderLeft: `3px solid ${color}`,
              }}>
                <Typography sx={{ fontSize: "0.76rem", color: "text.secondary", lineHeight: 1.55 }}>
                  {data.description}
                </Typography>
              </Box>
            </Box>
          )}

          {/* Causes */}
          {causes.length > 0 && (
            <Box>
              <SectionLabel>Possible Causes</SectionLabel>
              <Box sx={{ display: "flex", flexDirection: "column", gap: 0.6 }}>
                {causes.map((c) => {
                  const confColor = CONFIDENCE_COLORS[c.confidence] ?? "#6B7280";
                  return (
                    <Box key={c.id} sx={{
                      display: "flex", alignItems: "center", gap: 1,
                      p: 1, borderRadius: 1.5,
                      border: `1px solid ${confColor}22`,
                      borderLeft: `3px solid ${confColor}`,
                      bgcolor: `${confColor}08`,
                    }}>
                      <Box sx={{
                        color: confColor, flexShrink: 0,
                        width: 22, height: 22,
                        borderRadius: 1,
                        bgcolor: `${confColor}18`,
                        display: "flex", alignItems: "center", justifyContent: "center",
                      }}>
                        {CAUSE_ICONS[c.causeType] ?? <WarningAmberIcon sx={{ fontSize: 14 }} />}
                      </Box>
                      <Typography sx={{ fontSize: "0.73rem", flex: 1, color: "text.secondary", lineHeight: 1.4 }}>
                        {c.causeDescription}
                      </Typography>
                      <Chip
                        label={c.confidence}
                        size="small"
                        sx={{ fontSize: "0.52rem", height: 14, bgcolor: `${confColor}18`, color: confColor, border: `1px solid ${confColor}33`, flexShrink: 0 }}
                      />
                    </Box>
                  );
                })}
              </Box>
            </Box>
          )}

          {/* Alternatives */}
          {alternatives.length > 0 && (
            <Box>
              <SectionLabel>Alternative Transport</SectionLabel>
              <Box sx={{ display: "flex", flexDirection: "column", gap: 0.6 }}>
                {alternatives.map((a) => {
                  const modeKey = (a.mode ?? "").toLowerCase();
                  const modeColor = ALT_MODE_COLORS[modeKey] ?? "#6B7280";
                  return (
                    <Box key={a.id} sx={{
                      display: "flex", alignItems: "center", gap: 1,
                      p: 1, borderRadius: 1.5,
                      border: `1px solid ${modeColor}22`,
                      borderLeft: `3px solid ${modeColor}`,
                      bgcolor: `${modeColor}08`,
                    }}>
                      <Box sx={{
                        color: modeColor, flexShrink: 0,
                        width: 22, height: 22,
                        borderRadius: 1,
                        bgcolor: `${modeColor}18`,
                        display: "flex", alignItems: "center", justifyContent: "center",
                      }}>
                        {altIcon(a.mode)}
                      </Box>
                      <Box sx={{ flex: 1, minWidth: 0 }}>
                        <Typography sx={{ fontSize: "0.73rem", color: "text.primary", lineHeight: 1.3, fontWeight: 500 }} noWrap>
                          {a.description}
                        </Typography>
                        {a.etaMinutes != null && (
                          <Typography sx={{ fontSize: "0.62rem", color: "text.disabled" }}>~{a.etaMinutes} min</Typography>
                        )}
                      </Box>
                      {a.availabilityCount != null && (
                        <Box sx={{ textAlign: "right", flexShrink: 0 }}>
                          <Typography sx={{ fontSize: "0.78rem", fontWeight: 800, color: modeColor, lineHeight: 1 }}>{a.availabilityCount}</Typography>
                          <Typography sx={{ fontSize: "0.56rem", color: "text.disabled" }}>avail.</Typography>
                        </Box>
                      )}
                    </Box>
                  );
                })}
              </Box>
            </Box>
          )}

          {!data?.description && causes.length === 0 && alternatives.length === 0 && (
            <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", flex: 1 }}>
              <Typography sx={{ fontSize: "0.75rem", color: "text.disabled" }}>
                No additional details available
              </Typography>
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
}

// ── Main component ─────────────────────────────────────────────────────

export const DisruptionDashboard = () => {
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [panelOpen, setPanelOpen] = useState(true);
  const [detailOpen, setDetailOpen] = useState(false);
  const [modeTab, setModeTab] = useState(0);

  const theme = useAppSelector((s) => s.ui.theme);

  const { data: disruptions = [], isLoading, error, dataUpdatedAt } = useActiveDisruptions();

  const lastUpdated = dataUpdatedAt
    ? new Date(dataUpdatedAt).toLocaleTimeString("en-IE", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
    : null;

  const modeFilter = MODE_TABS[modeTab]?.key ?? "ALL";

  const filtered = useMemo(() =>
    disruptions
      .filter((d) => matchesMode(d, modeFilter))
      .toSorted((a, b) => (SEVERITY_ORDER[a.severity] ?? 4) - (SEVERITY_ORDER[b.severity] ?? 4)),
    [disruptions, modeFilter],
  );

  const sections = useMemo(() => {
    const groups: { severity: DisruptionSeverity; items: ActiveDisruption[] }[] = [];
    for (const sev of ["CRITICAL", "HIGH", "MEDIUM", "LOW"] as DisruptionSeverity[]) {
      const items = filtered.filter((d) => d.severity === sev);
      if (items.length > 0) groups.push({ severity: sev, items });
    }
    return groups;
  }, [filtered]);

  const counts = useMemo(() => ({
    total: disruptions.length,
    critical: disruptions.filter((d) => d.severity === "CRITICAL").length,
    high: disruptions.filter((d) => d.severity === "HIGH").length,
    medium: disruptions.filter((d) => d.severity === "MEDIUM").length,
    low: disruptions.filter((d) => d.severity === "LOW").length,
  }), [disruptions]);

  function handleSelect(id: number) {
    const next = selectedId === id ? null : id;
    setSelectedId(next);
    setDetailOpen(next != null);
  }

  return (
    <Box sx={{ position: "relative", height: "100%", width: "100%" }}>
      {/* ── Full-viewport map ── */}
      <NetworkImpactMap
        disruptions={disruptions}
        selectedId={selectedId}
        onMarkerClick={(id) => handleSelect(id)}
        darkTiles={theme === "dark"}
      />

      {error && (
        <Alert severity="error" sx={{ position: "absolute", top: GAP, left: "50%", transform: "translateX(-50%)", zIndex: 1200, borderRadius: 2 }}>
          Failed to load disruptions
        </Alert>
      )}

      {/* ── Top-left KPI strip ── */}
      <Box
        sx={{
          position: "absolute", top: GAP, left: GAP, zIndex: 1000,
          display: "flex", gap: 0.75, flexWrap: "wrap",
        }}
      >
        {[
          { label: "Total", value: counts.total, color: "#6B7280" },
          { label: "Critical", value: counts.critical, color: "#7C3AED" },
          { label: "High", value: counts.high, color: "#EF4444" },
          { label: "Medium", value: counts.medium, color: "#F59E0B" },
          { label: "Low", value: counts.low, color: "#10B981" },
        ].map(({ label, value, color }) => (
          <Box
            key={label}
            sx={{
              px: 1.5, py: 0.75, borderRadius: 2,
              bgcolor: (t) => t.palette.mode === "dark" ? "rgba(30,30,30,0.85)" : "rgba(255,255,255,0.88)",
              backdropFilter: "blur(10px)",
              border: `1px solid ${color}33`,
              textAlign: "center",
              minWidth: 52,
            }}
          >
            <Typography sx={{ fontSize: "1.1rem", fontWeight: 800, color, lineHeight: 1 }}>{value}</Typography>
            <Typography sx={{ fontSize: "0.58rem", color: "text.secondary", mt: 0.1 }}>{label}</Typography>
          </Box>
        ))}
        {!isLoading && (
          <Box sx={{
            display: "flex", alignItems: "center", gap: 0.5, px: 1.25, borderRadius: 2,
            bgcolor: (t) => t.palette.mode === "dark" ? "rgba(30,30,30,0.85)" : "rgba(255,255,255,0.88)",
            backdropFilter: "blur(10px)",
          }}>
            <FiberManualRecordIcon sx={{
              fontSize: 8,
              color: disruptions.length > 0 ? "#EF4444" : "#10B981",
              animation: disruptions.length > 0 ? "pulse 1.5s ease-in-out infinite" : "none",
              "@keyframes pulse": { "0%, 100%": { opacity: 1 }, "50%": { opacity: 0.3 } },
            }} />
            <Typography sx={{ fontSize: "0.6rem", color: "text.secondary" }}>
              {disruptions.length > 0 ? `${disruptions.length} active` : "All clear"}
            </Typography>
            {lastUpdated && (
              <Typography sx={{ fontSize: "0.58rem", color: "text.disabled", ml: 0.5 }}>· {lastUpdated}</Typography>
            )}
          </Box>
        )}
      </Box>

      {/* ── Side panel open button (when closed) ── */}
      {!panelOpen && (
        <IconButton
          onClick={() => setPanelOpen(true)}
          sx={{
            position: "absolute", top: GAP, right: GAP, zIndex: 1000,
            bgcolor: (t) => t.palette.background.paper,
            backdropFilter: "blur(12px)",
            "&:hover": { bgcolor: (t) => t.palette.background.paper },
          }}
        >
          <MenuOpenIcon />
        </IconButton>
      )}

      {/* ── Right side panel: disruption list ── */}
      {panelOpen && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute", top: GAP, right: GAP,
            bottom: detailOpen ? DETAIL_HEIGHT + GAP * 2 : GAP,
            width: PANEL_WIDTH, zIndex: 1000,
            borderRadius: 3,
            display: "flex", flexDirection: "column", overflow: "hidden",
          }}
        >
          {/* Panel header */}
          <Box sx={{ px: 2, pt: 1.5, pb: 1, flexShrink: 0 }}>
            <Box sx={{ display: "flex", alignItems: "center", mb: 1 }}>
              <WarningAmberIcon sx={{ fontSize: 16, color: "#EF4444", mr: 0.75 }} />
              <Typography variant="subtitle1" fontWeight={700} sx={{ flex: 1, lineHeight: 1 }}>
                Active Disruptions
              </Typography>
              <Typography variant="caption" sx={{ color: "text.disabled", mr: 1 }}>
                {filtered.length}{filtered.length === disruptions.length ? "" : `/${disruptions.length}`}
              </Typography>
              <IconButton size="small" onClick={() => setPanelOpen(false)}>
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>

            {/* Mode tabs */}
            <Tabs
              value={modeTab}
              onChange={(_, v) => setModeTab(v)}
              variant="scrollable"
              scrollButtons={false}
              sx={{
                minHeight: 28,
                "& .MuiTab-root": { minHeight: 28, fontSize: "0.62rem", textTransform: "none", px: 1.25, py: 0 },
                "& .MuiTabs-indicator": { height: 2 },
              }}
            >
              {MODE_TABS.map((t) => (
                <Tab
                  key={t.key}
                  label={
                    t.key === "ALL"
                      ? `All (${disruptions.length})`
                      : `${t.label} (${disruptions.filter((d) => matchesMode(d, t.key)).length})`
                  }
                />
              ))}
            </Tabs>
          </Box>

          <Divider />

          {/* List */}
          {isLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
              <CircularProgress size={20} sx={{ color: "#EF4444" }} />
            </Box>
          ) : (
            <Box sx={{ flex: 1, overflow: "auto" }}>
              {filtered.length === 0 ? (
                <Box sx={{ px: 2, py: 4, textAlign: "center" }}>
                  <CheckCircleOutlineIcon sx={{ fontSize: 28, color: "#10B981", mb: 0.75 }} />
                  <Typography sx={{ fontSize: "0.78rem", color: "text.secondary" }}>No disruptions in this category</Typography>
                </Box>
              ) : (
                sections.map(({ severity, items }) => (
                  <Box key={severity}>
                    <SectionHeader label={severity} color={SEVERITY_COLORS[severity]} count={items.length} />
                    {items.map((d, idx) => (
                      <Box key={d.id}>
                        <DisruptionRow d={d} selected={selectedId === d.id} onClick={() => handleSelect(d.id)} />
                        {idx < items.length - 1 && <Divider sx={{ borderColor: "rgba(0,0,0,0.04)", mx: 2 }} />}
                      </Box>
                    ))}
                  </Box>
                ))
              )}
            </Box>
          )}

          <Box sx={{ px: 2, py: 0.75, borderTop: "1px solid rgba(0,0,0,0.06)", flexShrink: 0 }}>
            <Typography sx={{ fontSize: "0.58rem", color: "text.disabled" }}>
              Auto-detected · refreshed every 5 min
            </Typography>
          </Box>
        </Paper>
      )}

      {/* ── Bottom detail panel (when a disruption is selected) ── */}
      {!detailOpen && selectedId != null && (
        <Chip
          icon={<WarningAmberIcon sx={{ fontSize: "0.9rem !important" }} />}
          label="Show disruption detail"
          onClick={() => setDetailOpen(true)}
          sx={{
            position: "absolute", bottom: GAP,
            right: panelOpen ? PANEL_WIDTH + GAP * 2 : GAP,
            zIndex: 1000,
            backdropFilter: "blur(12px)",
            bgcolor: (t) => t.palette.background.paper,
            fontWeight: 600, fontSize: "0.7rem", cursor: "pointer",
            transition: "right 0.2s ease",
            "& .MuiChip-icon": { color: "#EF4444" },
          }}
        />
      )}

      {detailOpen && selectedId != null && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute", bottom: GAP,
            left: GAP,
            right: panelOpen ? PANEL_WIDTH + GAP * 2 : GAP,
            height: DETAIL_HEIGHT, zIndex: 1000,
            borderRadius: 3, overflow: "hidden",
            transition: "right 0.2s ease",
          }}
        >
          <DetailPanel id={selectedId} onClose={() => setDetailOpen(false)} />
        </Paper>
      )}

      {/* ── Bottom mode impact panel (when no selection) ── */}
      {!detailOpen && !selectedId && disruptions.length > 0 && (
        <Paper
          elevation={0}
          sx={{
            position: "absolute", bottom: GAP, left: GAP,
            right: panelOpen ? PANEL_WIDTH + GAP * 2 : GAP,
            height: 200, zIndex: 1000,
            borderRadius: 3, overflow: "hidden",
            display: "flex", flexDirection: "column",
            transition: "right 0.2s ease",
          }}
        >
          <Box sx={{ px: 2, py: 1, borderBottom: "1px solid rgba(0,0,0,0.07)", flexShrink: 0, display: "flex", alignItems: "center", gap: 1 }}>
            <Typography variant="subtitle2" fontWeight={700} sx={{ flex: 1 }}>Mode Impact</Typography>
            <Typography sx={{ fontSize: "0.6rem", color: "text.secondary" }}>Click a disruption to see causes & alternatives</Typography>
          </Box>
          <Box sx={{ flex: 1, minHeight: 0, overflow: "hidden" }}>
            <RippleEffectVisualization disruptions={disruptions} />
          </Box>
        </Paper>
      )}
    </Box>
  );
};
