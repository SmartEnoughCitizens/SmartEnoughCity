/**
 * Reset password page — reached via the link in the reset email
 */

import { type FormEvent, useEffect, useState } from "react";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  TextField,
  Typography,
} from "@mui/material";
import {
  Link as RouterLink,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { useResetPassword } from "@/hooks";

export const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordMismatch, setPasswordMismatch] = useState(false);

  const resetPasswordMutation = useResetPassword();

  useEffect(() => {
    if (resetPasswordMutation.isSuccess) {
      const timer = setTimeout(() => {
        navigate("/login", {
          state: {
            successMessage: "Password reset successfully. Please log in.",
          },
        });
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [resetPasswordMutation.isSuccess, navigate]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setPasswordMismatch(true);
      return;
    }
    setPasswordMismatch(false);
    await resetPasswordMutation.mutateAsync({ token, newPassword });
  };

  if (!token) {
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
          <Alert severity="error" sx={{ mb: 2 }}>
            Invalid reset link. Please request a new password reset.
          </Alert>
          <Box sx={{ textAlign: "center" }}>
            <RouterLink to="/forgot-password" style={{ fontSize: "0.875rem" }}>
              Request new reset link
            </RouterLink>
          </Box>
        </Paper>
      </Box>
    );
  }

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
      <Paper
        elevation={3}
        sx={{
          p: 4,
          maxWidth: 400,
          width: "100%",
          mx: 2,
        }}
      >
        <Typography variant="h5" component="h1" gutterBottom align="center">
          Set New Password
        </Typography>

        {resetPasswordMutation.isSuccess && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {resetPasswordMutation.data.message} Redirecting to login...
          </Alert>
        )}

        {resetPasswordMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {resetPasswordMutation.error instanceof Error
              ? resetPasswordMutation.error.message
              : "Invalid or expired reset link. Please request a new one."}
          </Alert>
        )}

        {passwordMismatch && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Passwords do not match.
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
            disabled={
              resetPasswordMutation.isPending || resetPasswordMutation.isSuccess
            }
            inputProps={{ minLength: 8 }}
            required
          />

          <TextField
            fullWidth
            label="Confirm New Password"
            type="password"
            variant="outlined"
            margin="normal"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            disabled={
              resetPasswordMutation.isPending || resetPasswordMutation.isSuccess
            }
            required
          />

          <Button
            fullWidth
            type="submit"
            variant="contained"
            size="large"
            sx={{ mt: 3 }}
            disabled={
              resetPasswordMutation.isPending ||
              resetPasswordMutation.isSuccess ||
              !newPassword ||
              !confirmPassword
            }
          >
            {resetPasswordMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Reset Password"
            )}
          </Button>
        </form>

        <Box sx={{ mt: 2, textAlign: "center" }}>
          <RouterLink to="/login" style={{ fontSize: "0.875rem" }}>
            Back to Login
          </RouterLink>
        </Box>
      </Paper>
    </Box>
  );
};
