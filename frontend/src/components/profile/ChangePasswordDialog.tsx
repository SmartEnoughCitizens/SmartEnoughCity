/**
 * Dialog for changing the current user's password
 */

import { useState } from "react";
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from "@mui/material";
import { isAxiosError } from "axios";
import { useChangePassword } from "@/hooks";

const getErrorMessage = (error: Error | null): string => {
  if (!error) return "Failed to change password";
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error.message;
};

interface ChangePasswordDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export const ChangePasswordDialog = ({
  open,
  onClose,
  onSuccess,
}: ChangePasswordDialogProps) => {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const changePasswordMutation = useChangePassword();

  const passwordMismatch =
    confirmPassword.length > 0 && newPassword !== confirmPassword;

  const resetForm = () => {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    changePasswordMutation.reset();
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await changePasswordMutation.mutateAsync({
        currentPassword,
        newPassword,
      });
      onSuccess(response.message);
      handleClose();
    } catch {
      // Error displayed via mutation state
    }
  };

  const isFormValid =
    currentPassword.trim() &&
    newPassword.trim() &&
    confirmPassword.trim() &&
    newPassword === confirmPassword &&
    newPassword.length >= 8;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Change Password</DialogTitle>
        <DialogContent
          sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 1 }}
        >
          {changePasswordMutation.isError && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {getErrorMessage(changePasswordMutation.error)}
            </Alert>
          )}

          <TextField
            label="Current Password"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            disabled={changePasswordMutation.isPending}
            required
            fullWidth
            margin="dense"
          />
          <TextField
            label="New Password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            disabled={changePasswordMutation.isPending}
            required
            fullWidth
            margin="dense"
            helperText="Minimum 8 characters"
          />
          <TextField
            label="Confirm New Password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={changePasswordMutation.isPending}
            required
            fullWidth
            margin="dense"
            error={passwordMismatch}
            helperText={passwordMismatch ? "Passwords do not match" : ""}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            onClick={handleClose}
            disabled={changePasswordMutation.isPending}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={changePasswordMutation.isPending || !isFormValid}
          >
            {changePasswordMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Change Password"
            )}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};
