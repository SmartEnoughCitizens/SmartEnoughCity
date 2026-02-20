/**
 * User management page with add user functionality
 */

import { useState } from "react";
import { Box, Typography, Button, Snackbar, Alert } from "@mui/material";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import { Navigate } from "react-router-dom";
import { AddUserDialog } from "@/components/usermanagement/AddUserDialog";
import { useAppSelector } from "@/store/hooks";
import { getCreatableRoles } from "@/types";

export const UserManagementPage = () => {
  const { roles } = useAppSelector((state) => state.auth);
  const canManageUsers = getCreatableRoles(roles).length > 0;

  const [dialogOpen, setDialogOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
    severity: "success" | "error";
  }>({ open: false, message: "", severity: "success" });

  if (!canManageUsers) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleSuccess = (message: string) => {
    setSnackbar({ open: true, message, severity: "success" });
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

      <AddUserDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onSuccess={handleSuccess}
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
