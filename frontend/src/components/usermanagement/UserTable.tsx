/**
 * Table displaying manageable users with role, access scope, and delete action.
 *
 * Role column shows:
 *  - The Keycloak role name (if returned by the backend)
 *  - An "access scope" chip: which transport categories the role covers
 *  - A "Read only" badge for Provider roles; "Read & write" for Admin roles
 */

import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Typography,
  Chip,
  Stack,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import LockIcon from "@mui/icons-material/Lock";
import EditNoteIcon from "@mui/icons-material/EditNote";
import type { UserInfo } from "@/types";
import { getRoleAccessLabel, getRoleAccessLevel } from "@/config/permissions";

interface UserTableProps {
  users: UserInfo[];
  onDelete: (username: string) => void;
}

export const UserTable = ({ users, onDelete }: UserTableProps) => {
  if (users.length === 0) {
    return (
      <Typography color="text.secondary" sx={{ mt: 2 }}>
        No users found.
      </Typography>
    );
  }

  return (
    <TableContainer component={Paper} variant="outlined">
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Username</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>First Name</TableCell>
            <TableCell>Last Name</TableCell>
            <TableCell>Role & Access</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map((user) => {
            // Use the first recognised role from the array, if available
            const primaryRole = user.roles?.[0];
            const accessLevel = primaryRole
              ? getRoleAccessLevel(primaryRole)
              : null;
            const accessLabel = primaryRole
              ? getRoleAccessLabel(primaryRole)
              : null;

            return (
              <TableRow key={user.id} hover>
                <TableCell>{user.username}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.firstName}</TableCell>
                <TableCell>{user.lastName}</TableCell>

                {/* Role & Access column */}
                <TableCell>
                  {primaryRole ? (
                    <Stack
                      direction="column"
                      spacing={0.5}
                      alignItems="flex-start"
                    >
                      <Typography variant="body2" fontWeight={500}>
                        {primaryRole.replaceAll("_", " ")}
                      </Typography>
                      <Stack direction="row" spacing={0.5} flexWrap="wrap">
                        {/* Transport category scope */}
                        <Chip
                          label={accessLabel}
                          size="small"
                          variant="outlined"
                          color="primary"
                          sx={{ fontSize: "0.68rem", height: 20 }}
                        />
                        {/* Read / Write indicator */}
                        {accessLevel === "read-only" ? (
                          <Tooltip
                            title="Provider role — view only. Cannot dismiss notifications, act on recommendations, or export data."
                            arrow
                          >
                            <Chip
                              icon={
                                <LockIcon
                                  sx={{ fontSize: "0.75rem !important" }}
                                />
                              }
                              label="Read only"
                              size="small"
                              color="default"
                              sx={{ fontSize: "0.68rem", height: 20 }}
                            />
                          </Tooltip>
                        ) : (
                          <Tooltip
                            title="Admin role — can dismiss notifications, act on recommendations, and export data."
                            arrow
                          >
                            <Chip
                              icon={
                                <EditNoteIcon
                                  sx={{ fontSize: "0.75rem !important" }}
                                />
                              }
                              label="Read & write"
                              size="small"
                              color="success"
                              variant="outlined"
                              sx={{ fontSize: "0.68rem", height: 20 }}
                            />
                          </Tooltip>
                        )}
                      </Stack>
                    </Stack>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      —
                    </Typography>
                  )}
                </TableCell>

                <TableCell align="right">
                  <Tooltip title="Delete user">
                    <IconButton
                      color="error"
                      size="small"
                      onClick={() => onDelete(user.username)}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
