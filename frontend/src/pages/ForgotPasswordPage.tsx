/**
 * Forgot password page
 */

import { type FormEvent, useState } from "react";
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  TextField,
  Typography,
} from "@mui/material";
import { Link as RouterLink } from "react-router-dom";
import { useForgotPassword } from "@/hooks";

export const ForgotPasswordPage = () => {
  const [email, setEmail] = useState("");
  const forgotPasswordMutation = useForgotPassword();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) return;
    await forgotPasswordMutation.mutateAsync({ email });
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
          Reset Password
        </Typography>
        <Typography
          variant="body2"
          align="center"
          color="text.secondary"
          sx={{ mb: 3 }}
        >
          Enter your email address and we will send you a link to reset your
          password.
        </Typography>

        {forgotPasswordMutation.isSuccess && (
          <Alert severity="success" sx={{ mb: 2 }}>
            {forgotPasswordMutation.data.message}
          </Alert>
        )}

        {forgotPasswordMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Something went wrong. Please try again.
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <TextField
            fullWidth
            label="Email Address"
            type="email"
            variant="outlined"
            margin="normal"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={
              forgotPasswordMutation.isPending ||
              forgotPasswordMutation.isSuccess
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
              forgotPasswordMutation.isPending ||
              forgotPasswordMutation.isSuccess ||
              !email
            }
          >
            {forgotPasswordMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              "Send Reset Link"
            )}
          </Button>
        </form>

        <Box sx={{ mt: 2, textAlign: "center" }}>
          <RouterLink
            to="/login"
            style={{ color: "inherit", fontSize: "0.875rem" }}
          >
            Back to Login
          </RouterLink>
        </Box>
      </Paper>
    </Box>
  );
};
