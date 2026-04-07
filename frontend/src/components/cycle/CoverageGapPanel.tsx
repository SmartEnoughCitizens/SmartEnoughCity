import {
  Box,
  Chip,
  CircularProgress,
  Divider,
  MenuItem,
  Select,
  Stack,
  Tooltip,
  Typography,
} from "@mui/material";
import SignalCellularNoSimIcon from "@mui/icons-material/SignalCellularNoSim";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import TaskAltIcon from "@mui/icons-material/TaskAlt";
import ApartmentIcon from "@mui/icons-material/Apartment";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import AddLocationAltIcon from "@mui/icons-material/AddLocationAlt";
import type {
  CoverageCategory,
  CoverageGapDTO,
  StationProposalSummary,
} from "@/types";
import { useUpdateImplementationStatus } from "@/hooks";

function gapPriority(g: CoverageGapDTO): number {
  const order: Record<CoverageCategory, number> = {
    NO_COVERAGE: 0,
    POOR_COVERAGE: 1,
    PARTIAL_COVERAGE: 2,
    ADEQUATE: 3,
  };
  return order[g.coverageCategory];
}

// ── Category helpers ───────────────────────────────────────────────────────────

const CATEGORY_META: Record<
  CoverageCategory,
  { label: string; color: string; bg: string; icon: React.ReactNode }
> = {
  NO_COVERAGE: {
    label: "No Coverage",
    color: "#ef4444",
    bg: "#fef2f2",
    icon: <SignalCellularNoSimIcon sx={{ fontSize: "0.8rem" }} />,
  },
  POOR_COVERAGE: {
    label: "Poor Coverage",
    color: "#f97316",
    bg: "#fff7ed",
    icon: <WarningAmberIcon sx={{ fontSize: "0.8rem" }} />,
  },
  PARTIAL_COVERAGE: {
    label: "Partial Coverage",
    color: "#eab308",
    bg: "#fefce8",
    icon: <WarningAmberIcon sx={{ fontSize: "0.8rem" }} />,
  },
  ADEQUATE: {
    label: "Adequate",
    color: "#22c55e",
    bg: "#f0fdf4",
    icon: <CheckCircleOutlineIcon sx={{ fontSize: "0.8rem" }} />,
  },
};

function distanceText(m: number | null): string {
  if (m == null) return "N/A";
  return m >= 1000 ? `${(m / 1000).toFixed(2)} km` : `${Math.round(m)} m`;
}

// ── Gap card ───────────────────────────────────────────────────────────────────

function GapCard({ gap }: { gap: CoverageGapDTO }) {
  const meta = CATEGORY_META[gap.coverageCategory] ?? CATEGORY_META.ADEQUATE;
  const processed = gap.processedForImplementation;

  return (
    <Box
      sx={{
        px: 1.5,
        py: 1.25,
        borderLeft: `3px solid ${processed ? "#94a3b8" : meta.color}`,
        bgcolor: processed ? "action.hover" : "background.paper",
        opacity: processed ? 0.7 : 1,
        "&:hover": { bgcolor: "action.hover" },
      }}
    >
      {/* Header */}
      <Box
        sx={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
          mb: 0.5,
        }}
      >
        <Typography
          variant="body2"
          fontWeight={600}
          sx={{
            lineHeight: 1.3,
            flex: 1,
            mr: 1,
            textDecoration: processed ? "line-through" : "none",
          }}
        >
          {gap.electoralDivision}
        </Typography>
        {processed ? (
          <Chip
            size="small"
            icon={<TaskAltIcon sx={{ fontSize: "0.7rem !important" }} />}
            label="Planned"
            sx={{
              fontSize: "0.65rem",
              height: 20,
              color: "#64748b",
              bgcolor: "#f1f5f9",
            }}
          />
        ) : (
          <Chip
            size="small"
            label={meta.label}
            sx={{
              fontSize: "0.65rem",
              fontWeight: 700,
              color: meta.color,
              bgcolor: meta.bg,
              border: `1px solid ${meta.color}33`,
              height: 20,
            }}
          />
        )}
      </Box>

      {/* Description */}
      {!processed && (
        <Box
          sx={{ display: "flex", alignItems: "flex-start", gap: 0.5, mb: 0.5 }}
        >
          <Box sx={{ color: meta.color, mt: "1px", flexShrink: 0 }}>
            {meta.icon}
          </Box>
          <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
            {gap.coverageCategory === "NO_COVERAGE"
              ? `No station within 3 km — nearest is ${distanceText(gap.minDistanceM)} away.`
              : gap.coverageCategory === "POOR_COVERAGE"
                ? `Nearest station ${distanceText(gap.minDistanceM)} away — poor coverage (1–3 km).`
                : gap.coverageCategory === "PARTIAL_COVERAGE"
                  ? `Nearest station ${distanceText(gap.minDistanceM)} away — partial coverage (500 m–1 km).`
                  : `Nearest station ${distanceText(gap.minDistanceM)} away.`}
          </Typography>
        </Box>
      )}

      {processed && gap.processedAt && (
        <Typography
          variant="caption"
          color="text.disabled"
          sx={{ fontSize: "0.6rem", display: "block", mb: 0.5 }}
        >
          Planned on {new Date(gap.processedAt).toLocaleDateString()}
        </Typography>
      )}

      {/* Stats + action */}
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
        <Tooltip title="Apartment / flat dwellings">
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.4 }}>
            <ApartmentIcon
              sx={{ fontSize: "0.7rem", color: "text.disabled" }}
            />
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontSize: "0.65rem" }}
            >
              {gap.flatApartmentCount.toLocaleString()} apts
            </Typography>
          </Box>
        </Tooltip>
        <Tooltip title="Total dwellings in area">
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.4 }}>
            <DirectionsBikeIcon
              sx={{ fontSize: "0.7rem", color: "text.disabled" }}
            />
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{ fontSize: "0.65rem" }}
            >
              {gap.totalDwellings.toLocaleString()} total
            </Typography>
          </Box>
        </Tooltip>

        <Box sx={{ flex: 1 }} />
      </Box>
    </Box>
  );
}

// ── Implementation status config ──────────────────────────────────────────────

const IMPL_STATUS_META = {
  PLANNED: { label: "Planned", color: "#64748b", bg: "#f1f5f9" },
  IN_PROGRESS: { label: "In Progress", color: "#3b82f6", bg: "#eff6ff" },
  COMPLETED: { label: "Completed", color: "#16a34a", bg: "#dcfce7" },
} as const;

// ── Planned proposal card ──────────────────────────────────────────────────────

function PlannedProposalCard({
  proposal,
  isCycleAdmin,
}: {
  proposal: StationProposalSummary;
  isCycleAdmin: boolean;
}) {
  const { mutate: updateStatus, isPending } = useUpdateImplementationStatus();
  const status = proposal.implementationStatus ?? "PLANNED";
  const meta = IMPL_STATUS_META[status] ?? IMPL_STATUS_META.PLANNED;
  const acceptedDate = proposal.reviewedAt
    ? new Date(proposal.reviewedAt).toLocaleDateString()
    : proposal.submittedAt
      ? new Date(proposal.submittedAt).toLocaleDateString()
      : null;

  return (
    <Box
      sx={{
        px: 1.5,
        py: 1.25,
        borderLeft: `3px solid ${meta.color}`,
        bgcolor: "background.paper",
        "&:hover": { bgcolor: "action.hover" },
      }}
    >
      {/* Header row */}
      <Box
        sx={{
          display: "flex",
          alignItems: "flex-start",
          justifyContent: "space-between",
          mb: 0.5,
        }}
      >
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 0.5,
            flex: 1,
            mr: 1,
          }}
        >
          <AddLocationAltIcon
            sx={{ fontSize: "0.85rem", color: meta.color, flexShrink: 0 }}
          />
          <Typography variant="body2" fontWeight={600} sx={{ lineHeight: 1.3 }}>
            {proposal.stationCount} proposed station
            {proposal.stationCount === 1 ? "" : "s"}
          </Typography>
        </Box>

        {isCycleAdmin ? (
          <Select
            size="small"
            value={status}
            disabled={isPending}
            onChange={(e) =>
              updateStatus({ id: proposal.id, status: e.target.value })
            }
            sx={{
              fontSize: "0.65rem",
              height: 22,
              color: meta.color,
              bgcolor: meta.bg,
              ".MuiOutlinedInput-notchedOutline": {
                borderColor: `${meta.color}55`,
              },
              ".MuiSelect-select": { py: 0, px: 1 },
              ".MuiSvgIcon-root": { fontSize: "0.85rem", color: meta.color },
            }}
          >
            {Object.entries(IMPL_STATUS_META).map(([val, m]) => (
              <MenuItem key={val} value={val} sx={{ fontSize: "0.7rem" }}>
                {m.label}
              </MenuItem>
            ))}
          </Select>
        ) : (
          <Chip
            size="small"
            label={meta.label}
            sx={{
              fontSize: "0.65rem",
              height: 20,
              color: meta.color,
              bgcolor: meta.bg,
            }}
          />
        )}
      </Box>

      {/* Details */}
      <Typography
        variant="caption"
        color="text.secondary"
        display="block"
        sx={{ mb: 0.25 }}
      >
        {proposal.improvedAreaCount} area
        {proposal.improvedAreaCount === 1 ? "" : "s"} improved
        {proposal.reviewedBy ? ` · Accepted by ${proposal.reviewedBy}` : ""}
        {acceptedDate ? ` on ${acceptedDate}` : ""}
      </Typography>
      <Typography
        variant="caption"
        color="text.disabled"
        sx={{ fontSize: "0.6rem" }}
      >
        Submitted by {proposal.submittedBy} ({proposal.submittedByRole})
      </Typography>
    </Box>
  );
}

// ── Summary row ────────────────────────────────────────────────────────────────

function CoverageSummaryRow({
  gaps,
  plannedCount,
}: {
  gaps: CoverageGapDTO[];
  plannedCount: number;
}) {
  const noCoverage = gaps.filter(
    (g) => g.coverageCategory === "NO_COVERAGE",
  ).length;
  const poorCoverage = gaps.filter(
    (g) => g.coverageCategory === "POOR_COVERAGE",
  ).length;
  const partialCoverage = gaps.filter(
    (g) => g.coverageCategory === "PARTIAL_COVERAGE",
  ).length;
  const adequate = gaps.filter((g) => g.coverageCategory === "ADEQUATE").length;
  const affectedDwellings = gaps
    .filter(
      (g) => g.coverageCategory !== "ADEQUATE" && !g.processedForImplementation,
    )
    .reduce((sum, g) => sum + g.flatApartmentCount, 0);

  return (
    <Box
      sx={{
        px: 1.5,
        py: 1,
        display: "flex",
        gap: 1.5,
        flexWrap: "wrap",
        alignItems: "flex-end",
      }}
    >
      {[
        { count: noCoverage, label: "No Coverage", color: "#ef4444" },
        { count: poorCoverage, label: "Poor Coverage", color: "#f97316" },
        { count: partialCoverage, label: "Partial", color: "#eab308" },
        { count: adequate, label: "Adequate", color: "#22c55e" },
        { count: plannedCount, label: "Planned", color: "#16a34a" },
      ].map(({ count, label, color }) => (
        <Box key={label} sx={{ textAlign: "center" }}>
          <Typography
            variant="h6"
            fontWeight={700}
            sx={{ color, lineHeight: 1 }}
          >
            {count}
          </Typography>
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ fontSize: "0.6rem" }}
          >
            {label}
          </Typography>
        </Box>
      ))}
      <Box sx={{ flex: 1 }} />
      <Tooltip title="Apartment dwellings in unaddressed under-served areas">
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ fontSize: "0.6rem" }}
        >
          {affectedDwellings.toLocaleString()} affected apts
        </Typography>
      </Tooltip>
    </Box>
  );
}

// ── Main panel ─────────────────────────────────────────────────────────────────

interface Props {
  gaps: CoverageGapDTO[];
  acceptedProposals: StationProposalSummary[];
  isLoading: boolean;
  isCycleAdmin: boolean;
}

export function CoverageGapPanel({
  gaps,
  acceptedProposals,
  isLoading,
  isCycleAdmin,
}: Props) {
  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (gaps.length === 0) {
    return (
      <Stack alignItems="center" spacing={1} sx={{ py: 4 }}>
        <SignalCellularNoSimIcon
          sx={{ fontSize: 32, color: "text.disabled" }}
        />
        <Typography variant="body2" color="text.secondary">
          Coverage analysis not available yet. Computed nightly.
        </Typography>
      </Stack>
    );
  }

  // Unprocessed problem areas first (NO_COVERAGE → POOR_COVERAGE → ADEQUATE), then processed
  const unprocessed = gaps
    .filter((g) => !g.processedForImplementation)
    .toSorted(
      (a, b) =>
        gapPriority(a) - gapPriority(b) ||
        b.flatApartmentCount - a.flatApartmentCount,
    );
  const processed = gaps.filter((g) => g.processedForImplementation);
  const sorted = [...unprocessed, ...processed];

  return (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
      <CoverageSummaryRow gaps={gaps} plannedCount={acceptedProposals.length} />
      <Divider />
      <Box sx={{ flex: 1, overflow: "auto" }}>
        {acceptedProposals.length > 0 && (
          <>
            <Box sx={{ px: 1.5, py: 0.75, bgcolor: "action.hover" }}>
              <Typography
                variant="caption"
                fontWeight={700}
                sx={{
                  color: "#16a34a",
                  fontSize: "0.65rem",
                  textTransform: "uppercase",
                  letterSpacing: 0.5,
                }}
              >
                Planned ({acceptedProposals.length})
              </Typography>
            </Box>
            {acceptedProposals.map((p, i) => (
              <Box key={p.id}>
                {i > 0 && <Divider />}
                <PlannedProposalCard proposal={p} isCycleAdmin={isCycleAdmin} />
              </Box>
            ))}
            <Divider />
          </>
        )}
        {sorted.map((gap, i) => (
          <Box key={gap.electoralDivision}>
            {i > 0 && <Divider />}
            <GapCard gap={gap} />
          </Box>
        ))}
      </Box>
    </Box>
  );
}
