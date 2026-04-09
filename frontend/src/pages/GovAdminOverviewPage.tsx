/**
 * Government Admin Overview Page
 * Shows a read-only breakdown of platform users by role type as an org-tree chart.
 */

import { Box, Typography, Paper, Skeleton, Alert, Divider } from "@mui/material";
import PeopleIcon from "@mui/icons-material/People";
import { useGetUserCounts } from "@/hooks";

interface RoleGroup {
  label: string;
  roles: string[];
}

const ROLE_GROUPS: RoleGroup[] = [
  { label: "City Management", roles: ["City_Manager"] },
  { label: "Bus", roles: ["Bus_Admin", "Bus_Provider"] },
  { label: "Train", roles: ["Train_Admin", "Train_Provider"] },
  { label: "Cycles", roles: ["Cycle_Admin", "Cycle_Provider"] },
  { label: "Tram", roles: ["Tram_Admin", "Tram_Provider"] },
];

const formatRoleName = (role: string) => role.replaceAll("_", " ");

// ─── Layout constants ────────────────────────────────────────────────────────
const W = 1100;
const H = 430;

const ROOT_X = W / 2;
const ROOT_Y = 40;
const ROOT_W = 220;
const ROOT_H = 54;

const SPINE_Y = 130; // horizontal spine y

const GROUP_W = 168;
const GROUP_H = 52;
const GROUP_Y = 155;
const GROUP_XS = [110, 330, 550, 770, 990];

const ROLE_W = 155;
const ROLE_H = 40;
const ROLE1_Y = 285;
const ROLE2_Y = 338; // ROLE1_Y + ROLE_H + 13 gap

// ─── Colours ─────────────────────────────────────────────────────────────────
const C = {
  rootFill: "#1565c0",
  groupFill: "#1976d2",
  leafFill: "#ffffff",
  leafStroke: "#bbdefb",
  leafText: "#1565c0",
  subText: "#546e7a",
  line: "#90caf9",
  dashedLine: "#90caf9",
  rootSub: "#bbdefb",
  white: "#ffffff",
};

// ─── Tree chart ───────────────────────────────────────────────────────────────
const OrgTreeChart = ({ counts }: { counts: Record<string, number> }) => {
  const totalUsers = Object.values(counts).reduce((sum, n) => sum + n, 0);

  return (
    <Box sx={{ width: "100%", overflowX: "auto" }}>
      <svg
        viewBox={`0 0 ${W} ${H}`}
        style={{ width: "100%", minWidth: 640, height: "auto", display: "block" }}
        aria-label="Platform user hierarchy tree"
      >
        {/* ── Connectors: root → spine ── */}
        <line
          x1={ROOT_X}
          y1={ROOT_Y + ROOT_H}
          x2={ROOT_X}
          y2={SPINE_Y}
          stroke={C.line}
          strokeWidth={2}
        />

        {/* ── Connectors: horizontal spine ── */}
        <line
          x1={GROUP_XS[0]}
          y1={SPINE_Y}
          x2={GROUP_XS[GROUP_XS.length - 1]}
          y2={SPINE_Y}
          stroke={C.line}
          strokeWidth={2}
        />

        {/* ── Connectors: spine → each group ── */}
        {GROUP_XS.map((gx) => (
          <line
            key={`spine-group-${gx}`}
            x1={gx}
            y1={SPINE_Y}
            x2={gx}
            y2={GROUP_Y}
            stroke={C.line}
            strokeWidth={2}
          />
        ))}

        {/* ── Connectors: group → roles ── */}
        {ROLE_GROUPS.map((group, i) => {
          const gx = GROUP_XS[i];
          return (
            <g key={`role-lines-${group.label}`}>
              <line
                x1={gx}
                y1={GROUP_Y + GROUP_H}
                x2={gx}
                y2={ROLE1_Y}
                stroke={C.dashedLine}
                strokeWidth={1.5}
                strokeDasharray="5 3"
              />
              {group.roles.length > 1 && (
                <line
                  x1={gx}
                  y1={ROLE1_Y + ROLE_H}
                  x2={gx}
                  y2={ROLE2_Y}
                  stroke={C.dashedLine}
                  strokeWidth={1.5}
                  strokeDasharray="5 3"
                />
              )}
            </g>
          );
        })}

        {/* ── Root node ── */}
        <rect
          x={ROOT_X - ROOT_W / 2}
          y={ROOT_Y}
          width={ROOT_W}
          height={ROOT_H}
          rx={10}
          fill={C.rootFill}
        />
        <text
          x={ROOT_X}
          y={ROOT_Y + 21}
          textAnchor="middle"
          fill={C.white}
          fontSize={13}
          fontWeight="bold"
          fontFamily="sans-serif"
        >
          Smart Enough City
        </text>
        <text
          x={ROOT_X}
          y={ROOT_Y + 40}
          textAnchor="middle"
          fill={C.rootSub}
          fontSize={12}
          fontFamily="sans-serif"
        >
          {totalUsers} total users
        </text>

        {/* ── Group nodes ── */}
        {ROLE_GROUPS.map((group, i) => {
          const gx = GROUP_XS[i];
          const groupTotal = group.roles.reduce(
            (sum, r) => sum + (counts[r] ?? 0),
            0
          );
          return (
            <g key={`group-${group.label}`}>
              <rect
                x={gx - GROUP_W / 2}
                y={GROUP_Y}
                width={GROUP_W}
                height={GROUP_H}
                rx={8}
                fill={C.groupFill}
              />
              <text
                x={gx}
                y={GROUP_Y + 21}
                textAnchor="middle"
                fill={C.white}
                fontSize={12}
                fontWeight="bold"
                fontFamily="sans-serif"
              >
                {group.label}
              </text>
              <text
                x={gx}
                y={GROUP_Y + 39}
                textAnchor="middle"
                fill={C.rootSub}
                fontSize={11}
                fontFamily="sans-serif"
              >
                {groupTotal} users
              </text>
            </g>
          );
        })}

        {/* ── Role (leaf) nodes ── */}
        {ROLE_GROUPS.map((group, i) => {
          const gx = GROUP_XS[i];
          return group.roles.map((role, j) => {
            const ry = j === 0 ? ROLE1_Y : ROLE2_Y;
            const count = counts[role] ?? 0;
            return (
              <g key={`role-${role}`}>
                <rect
                  x={gx - ROLE_W / 2}
                  y={ry}
                  width={ROLE_W}
                  height={ROLE_H}
                  rx={6}
                  fill={C.leafFill}
                  stroke={C.leafStroke}
                  strokeWidth={1.5}
                />
                <text
                  x={gx}
                  y={ry + 15}
                  textAnchor="middle"
                  fill={C.subText}
                  fontSize={10.5}
                  fontFamily="sans-serif"
                >
                  {formatRoleName(role)}
                </text>
                <text
                  x={gx}
                  y={ry + 31}
                  textAnchor="middle"
                  fill={C.leafText}
                  fontSize={13}
                  fontWeight="bold"
                  fontFamily="sans-serif"
                >
                  {count}
                </text>
              </g>
            );
          });
        })}
      </svg>
    </Box>
  );
};

// ─── Page ─────────────────────────────────────────────────────────────────────
export const GovAdminOverviewPage = () => {
  const { data: counts, isLoading, isError } = useGetUserCounts();

  const totalUsers = counts
    ? Object.values(counts).reduce((sum, n) => sum + n, 0)
    : 0;

  return (
    <Box
      sx={{
        height: "100%",
        overflow: "auto",
        p: { xs: 2, md: 4 },
        boxSizing: "border-box",
        bgcolor: "background.default",
      }}
    >
      {/* Header */}
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.5, mb: 1 }}>
        <PeopleIcon sx={{ fontSize: 32, color: "primary.main" }} />
        <Box>
          <Typography variant="h5" fontWeight={700}>
            Platform Overview
          </Typography>
          <Typography variant="body2" color="text.secondary">
            User breakdown across all roles
          </Typography>
        </Box>
        {!isLoading && !isError && (
          <Paper variant="outlined" sx={{ ml: "auto", px: 2, py: 1, borderRadius: 2 }}>
            <Typography variant="caption" color="text.secondary">
              Total users
            </Typography>
            <Typography variant="h6" fontWeight={700} lineHeight={1.2}>
              {totalUsers}
            </Typography>
          </Paper>
        )}
      </Box>

      <Divider sx={{ mb: 3 }} />

      {/* Loading */}
      {isLoading && (
        <Skeleton variant="rounded" width="100%" height={380} sx={{ borderRadius: 2 }} />
      )}

      {/* Error */}
      {isError && (
        <Alert severity="error" sx={{ maxWidth: 500 }}>
          Failed to load user data. Please try again later.
        </Alert>
      )}

      {/* Tree chart */}
      {counts && (
        <Paper variant="outlined" sx={{ p: { xs: 1, md: 2 }, borderRadius: 2 }}>
          <OrgTreeChart counts={counts} />
        </Paper>
      )}
    </Box>
  );
};
