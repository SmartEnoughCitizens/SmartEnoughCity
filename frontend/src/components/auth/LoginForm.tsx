/**
 * Login form component
 */

import { useState, type FormEvent } from 'react';
import {
  Box,
  Button,
  TextField,
  Typography,
  Alert,
  Paper,
  CircularProgress,
} from '@mui/material';
import { useLogin } from '@/hooks';
import { useAppDispatch } from '@/store/hooks';
import { setAuthenticated } from '@/store/slices/authSlice';
import { useNavigate } from 'react-router-dom';

export const LoginForm = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

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

      dispatch(
        setAuthenticated({
          accessToken: response.accessToken,
          username: response.username,
        })
      );

      navigate('/dashboard');
    } catch {
      // Error is handled by mutation
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
      }}
    >
      <Paper
        elevation={3}
        sx={{
          p: 4,
          maxWidth: 400,
          width: '100%',
          mx: 2,
        }}
      >
        <Typography variant="h4" component="h1" gutterBottom align="center">
          Hermes
        </Typography>
        <Typography
          variant="subtitle1"
          gutterBottom
          align="center"
          color="text.secondary"
          sx={{ mb: 3 }}
        >
          Smart City Transport Analytics
        </Typography>

        {loginMutation.isError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {loginMutation.error instanceof Error
              ? loginMutation.error.message
              : 'Invalid username or password'}
          </Alert>
        )}

        <form onSubmit={handleSubmit}>
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

          <TextField
            fullWidth
            label="Password"
            type="password"
            variant="outlined"
            margin="normal"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loginMutation.isPending}
            required
          />

          <Button
            fullWidth
            type="submit"
            variant="contained"
            size="large"
            sx={{ mt: 3 }}
            disabled={loginMutation.isPending || !username || !password}
          >
            {loginMutation.isPending ? (
              <CircularProgress size={24} color="inherit" />
            ) : (
              'Login'
            )}
          </Button>
        </form>
      </Paper>
    </Box>
  );
};
