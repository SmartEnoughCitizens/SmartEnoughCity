/**
 * First-login change password page.
 * Shown when backend returns passwordChangeRequired: true after login.
 */

import { useState, type FormEvent } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
  Alert,
  Paper,
  CircularProgress,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import { useChangePassword } from "@/hooks";
import { isAxiosError } from "axios";

const getErrorMessage = (error: Error | null): string => {
  if (!error) return "Failed to change password";
  if (isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || error.message;
  }
  return error.message;
};

export const ChangePasswordPage = () => {
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [validationError, setValidationError] = useState("");
  const navigate = useNavigate();
  const changePasswordMutation = useChangePassword();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setValidationError("");

    if (newPassword.length < 8) {
      setValidationError("Password must be at least 8 characters");
      return;
    }

    if (newPassword !== confirmPassword) {
      setValidationError("Passwords do not match");
      return;
    }

    try {
      await changePasswordMutation.mutateAsync(newPassword);
      navigate("/dashboard");
    } catch {
      // Error displayed via mutation state
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        bgcolor: "background.default",
      }}
    >
      <Paper elevation={3} sx={{ p: 4, maxWidth: 400, width: "100%", mx: 2 }}>
        <Typography variant="h5" component="h1" gutterBottom align="center">
          Set Your Password
        </Typography>
        <Typography
          variant="body2"
          align="center"
          color="text.secondary"
          sx={{ mb: 3 }}
        >
          This is your first login. Please set a new password to continue.
        </Typography>

        {(validationError || changePasswordMutation.isError) && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {validationError || getErrorMessage(changePasswordMutation.error)}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="New Password"
            type="password"
            variant="outlined"
            margin="normal"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            disabled={changePasswordMutation.isPending}
            required
          />
          <TextField
            fullWidth
            label="Confirm Password"
            type="password"
            variant="outlined"
            margin="normal"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={changePasswordMutation.isPending}
            required
          />
          <Button
            fullWidth
            type="submit"
            variant="contained"
            size="large"
            sx={{ mt: 3 }}
            disabled={
              changePasswordMutation.isPending || !newPassword || !confirmPassword
            }
          >
            {changePasswordMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Set Password"
            )}
          </Button>
        </form>
      </Paper>
    </Box>
  );
};
