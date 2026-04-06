/**
 * Login form component
 */

import { useState, type FormEvent } from "react";
import {
  Box,
  Button,
  IconButton,
  InputAdornment,
  Link,
  TextField,
  Typography,
  Alert,
  CircularProgress,
} from "@mui/material";
import VisibilityIcon from "@mui/icons-material/Visibility";
import VisibilityOffIcon from "@mui/icons-material/VisibilityOff";
import { isAxiosError } from "axios";
import { motion, useReducedMotion } from "framer-motion";
import { useLogin } from "@/hooks";
import { useAppDispatch } from "@/store/hooks";
import { setAuthenticated } from "@/store/slices/authSlice";
import { getRolesFromToken } from "@/utils/jwt";
import { getLandingPage } from "@/types";
import { Link as RouterLink, useNavigate } from "react-router-dom";

export const LoginForm = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const shouldReduceMotion = useReducedMotion();

  const loginMutation = useLogin();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!username.trim() || !password.trim()) {
      return;
    }

    try {
      const response = await loginMutation.mutateAsync({
        username,
        password,
      });

      const roles = getRolesFromToken(response.accessToken);

      dispatch(
        setAuthenticated({
          accessToken: response.accessToken,
          username: response.username,
          roles,
        }),
      );

      navigate(getLandingPage(roles));
    } catch {
      // Error is handled by mutation
    }
  };

  const fieldDelay = (index: number) =>
    shouldReduceMotion ? 0 : Math.min(index, 2) * 0.06;

  return (
    <motion.div
      initial={{ opacity: 0, x: shouldReduceMotion ? 0 : 24 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.45, ease: "easeOut" }}
    >
      <Box sx={{ width: "100%", maxWidth: 380 }}>
        {/* Logo shown only on mobile (left panel hidden) */}
        <Box
          sx={{
            display: { xs: "flex", md: "none" },
            justifyContent: "center",
            mb: 3,
          }}
        >
          <img src="/favicon.svg" height={300} alt="SmartEnoughCity" />
        </Box>

        <Typography variant="h4" component="h1" fontWeight={700} gutterBottom>
          Sign in
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Welcome back to SmartEnoughCity
        </Typography>

        {loginMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {isAxiosError<{ error?: string }>(loginMutation.error)
              ? (loginMutation.error.response?.data?.error ??
                "Invalid username or password")
              : "Invalid username or password"}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
          <motion.div
            initial={{ opacity: 0, y: shouldReduceMotion ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: fieldDelay(0) }}
          >
            <TextField
              fullWidth
              label="Username"
              variant="outlined"
              margin="normal"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loginMutation.isPending}
              required
            />
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: shouldReduceMotion ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: fieldDelay(1) }}
          >
            <TextField
              fullWidth
              label="Password"
              type={showPassword ? "text" : "password"}
              variant="outlined"
              margin="normal"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={loginMutation.isPending}
              required
              slotProps={{
                input: {
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword((v) => !v)}
                        edge="end"
                        aria-label={
                          showPassword ? "Hide password" : "Show password"
                        }
                      >
                        {showPassword ? (
                          <VisibilityOffIcon />
                        ) : (
                          <VisibilityIcon />
                        )}
                      </IconButton>
                    </InputAdornment>
                  ),
                },
              }}
            />
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: shouldReduceMotion ? 0 : 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: fieldDelay(2) }}
          >
            <Box sx={{ textAlign: "right", mt: 1 }}>
              <Link
                component={RouterLink}
                to="/forgot-password"
                variant="body2"
                underline="hover"
              >
                Forgot password?
              </Link>
            </Box>

            <Button
              fullWidth
              type="submit"
              variant="contained"
              size="large"
              sx={{ mt: 2 }}
              disabled={loginMutation.isPending || !username || !password}
            >
              {loginMutation.isPending ? (
                <CircularProgress size={24} color="inherit" />
              ) : (
                "Login"
              )}
            </Button>
          </motion.div>
        </form>
      </Box>
    </motion.div>
  );
};
