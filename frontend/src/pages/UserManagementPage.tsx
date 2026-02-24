/**
 * User management page with user table, add, and delete functionality
 */

import { useState } from "react";
import {
  Box,
  Typography,
  Button,
  Snackbar,
  Alert,
  CircularProgress,
} from "@mui/material";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import { Navigate } from "react-router-dom";
import { AddUserDialog } from "@/components/usermanagement/AddUserDialog";
import { UserTable } from "@/components/usermanagement/UserTable";
import { DeleteUserDialog } from "@/components/usermanagement/DeleteUserDialog";
import { useAppSelector } from "@/store/hooks";
import { getCreatableRoles } from "@/types";
import { useGetUsers, useDeleteUser } from "@/hooks";
import { isAxiosError } from "axios";

export const UserManagementPage = () => {
  const { roles } = useAppSelector((state) => state.auth);
  const canManageUsers = getCreatableRoles(roles).length > 0;

  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  const usersQuery = useGetUsers();
  const deleteMutation = useDeleteUser();

  if (!canManageUsers) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSuccess = (message: string) => {
    setSnackbar({ open: true, message, severity: "success" });
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;

    try {
      const result = await deleteMutation.mutateAsync(deleteTarget);
      setSnackbar({ open: true, message: result.message, severity: "success" });
      setDeleteTarget(null);
    } catch (error) {
      const message =
        isAxiosError<{ message?: string }>(error)
          ? (error.response?.data?.message ?? error.message)
          : "Failed to delete user";
      setSnackbar({ open: true, message, severity: "error" });
    }
  };

  return (
    <Box sx={{ p: 3, height: "100%", overflow: "auto" }}>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 3,
        }}
      >
        <Typography variant="h5" fontWeight={600}>
          User Management
        </Typography>
        <Button
          variant="contained"
          startIcon={<PersonAddIcon />}
          onClick={() => setDialogOpen(true)}
        >
          Add User
        </Button>
      </Box>

      {usersQuery.isLoading && (
        <Box sx={{ display: "flex", justifyContent: "center", mt: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {usersQuery.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load users. Please try again.
        </Alert>
      )}

      {usersQuery.data && (
        <UserTable
          users={usersQuery.data}
          onDelete={(username) => setDeleteTarget(username)}
        />
      )}

      <AddUserDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSuccess={handleSuccess}
      />

      <DeleteUserDialog
        open={deleteTarget !== null}
        username={deleteTarget ?? ""}
        isPending={deleteMutation.isPending}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDeleteConfirm()}
      />

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          severity={snackbar.severity}
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};
