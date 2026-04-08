/**
 * Government Admin Overview Page
 * Shows a read-only breakdown of platform users by role type.
 */

import {
  Box,
  Typography,
  Paper,
  Skeleton,
  Alert,
  Divider,
} from "@mui/material";
import PeopleIcon from "@mui/icons-material/People";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import TrainIcon from "@mui/icons-material/Train";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import TramIcon from "@mui/icons-material/Tram";
import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import { useGetUserCounts } from "@/hooks";

interface RoleGroup {
  label: string;
  icon: React.ReactNode;
  roles: string[];
}

const ROLE_GROUPS: RoleGroup[] = [
  {
    label: "City Management",
    icon: <AdminPanelSettingsIcon />,
    roles: ["City_Manager"],
  },
  {
    label: "Bus",
    icon: <DirectionsBusIcon />,
    roles: ["Bus_Admin", "Bus_Provider"],
  },
  {
    label: "Train",
    icon: <TrainIcon />,
    roles: ["Train_Admin", "Train_Provider"],
  },
  {
    label: "Cycles",
    icon: <DirectionsBikeIcon />,
    roles: ["Cycle_Admin", "Cycle_Provider"],
  },
  {
    label: "Tram",
    icon: <TramIcon />,
    roles: ["Tram_Admin", "Tram_Provider"],
  },
];

const formatRoleName = (role: string) => role.replaceAll("_", " ");

const RoleRow = ({ role, count }: { role: string; count: number }) => (
  <Box
    sx={{
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      py: 0.75,
      px: 1,
      borderRadius: 1,
      "&:hover": { bgcolor: "action.hover" },
    }}
  >
    <Typography variant="body2" sx={{ color: "text.secondary" }}>
      {formatRoleName(role)}
    </Typography>
    <Typography
      variant="body2"
      fontWeight={600}
      sx={{ color: "text.primary", minWidth: 28, textAlign: "right" }}
    >
      {count}
    </Typography>
  </Box>
);

const GroupCard = ({
  group,
  counts,
}: {
  group: RoleGroup;
  counts: Record<string, number>;
}) => {
  const total = group.roles.reduce((sum, r) => sum + (counts[r] ?? 0), 0);

  return (
    <Paper
      variant="outlined"
      sx={{
        p: 2,
        borderRadius: 2,
        flex: "1 1 220px",
        minWidth: 200,
        maxWidth: 320,
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 1 }}>
        <Box sx={{ color: "primary.main", display: "flex" }}>{group.icon}</Box>
        <Typography variant="subtitle2" fontWeight={600}>
          {group.label}
        </Typography>
        <Typography
          variant="caption"
          sx={{
            ml: "auto",
            bgcolor: "primary.main",
            color: "primary.contrastText",
            borderRadius: 10,
            px: 1,
            py: 0.25,
            fontWeight: 700,
          }}
        >
          {total}
        </Typography>
      </Box>
      <Divider sx={{ mb: 1 }} />
      {group.roles.map((role) => (
        <RoleRow key={role} role={role} count={counts[role] ?? 0} />
      ))}
    </Paper>
  );
};

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
          <Paper
            variant="outlined"
            sx={{ ml: "auto", px: 2, py: 1, borderRadius: 2 }}
          >
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

      {/* Loading state */}
      {isLoading && (
        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2 }}>
          {ROLE_GROUPS.map((g) => (
            <Skeleton
              key={g.label}
              variant="rounded"
              width={240}
              height={140}
              sx={{ borderRadius: 2 }}
            />
          ))}
        </Box>
      )}

      {/* Error state */}
      {isError && (
        <Alert severity="error" sx={{ maxWidth: 500 }}>
          Failed to load user data. Please try again later.
        </Alert>
      )}

      {/* Role group cards */}
      {counts && (
        <Box
          sx={{
            display: "flex",
            flexWrap: "wrap",
            gap: 2,
          }}
        >
          {ROLE_GROUPS.map((group) => (
            <GroupCard key={group.label} group={group} counts={counts} />
          ))}
        </Box>
      )}
    </Box>
  );
};
