import React, { useState } from 'react';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Container,
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Link,
  ToggleButtonGroup,
  ToggleButton,
  InputAdornment,
  IconButton,
} from '@mui/material';
import { Visibility, VisibilityOff, PersonAdd } from '@mui/icons-material';
import { useRegisterCityManager, useRegisterServiceProviderAdmin } from '@/hooks';
import { UserRole } from '../../types';

interface RegisterFormData {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  organization: string;
}

type UserTypeSelection = 'CITY_MANAGER' | 'SERVICE_PROVIDER_ADMIN';

const Register: React.FC = () => {
  const navigate = useNavigate();

  const [userType, setUserType] = useState<UserTypeSelection>('CITY_MANAGER');
  const [formData, setFormData] = useState<RegisterFormData>({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
    organization: '',
  });
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');

  const registerCityManagerMutation = useRegisterCityManager();
  const registerServiceProviderMutation = useRegisterServiceProviderAdmin();

  const currentMutation = userType === 'CITY_MANAGER'
    ? registerCityManagerMutation
    : registerServiceProviderMutation;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    const registrationData = {
      username: formData.username,
      email: formData.email,
      password: formData.password,
      firstName: formData.firstName,
      lastName: formData.lastName,
      userType: userType === 'CITY_MANAGER' ? UserRole.CITY_MANAGER : UserRole.SERVICE_PROVIDER_ADMIN,
      organization: formData.organization, // Include organization field
    } as any; // Type assertion needed as organization is not in RegisterData interface

    currentMutation.mutate(registrationData, {
      onSuccess: () => {
        setSuccess('Registration successful! Redirecting to login...');
        setTimeout(() => {
          navigate('/login');
        }, 2000);
      },
      onError: (err: Error) => {
        const errorMessage = (err as any).response?.data?.message || 'Registration failed. Please try again.';
        setError(errorMessage);
      },
    });
  };

  const handleUserTypeChange = (
    _event: React.MouseEvent<HTMLElement>,
    newValue: UserTypeSelection | null
  ) => {
    if (newValue) {
      setUserType(newValue);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        py: 4,
      }}
    >
      <Container maxWidth="sm">
        <Card>
          <CardContent sx={{ p: 4 }}>
            <Box sx={{ textAlign: 'center', mb: 4 }}>
              <PersonAdd sx={{ fontSize: 48, color: 'primary.main', mb: 2 }} />
              <Typography variant="h4" component="h1" gutterBottom>
                Create Account
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Register for Sustainable City Management
              </Typography>
            </Box>

            {error && (
              <Alert severity="error" sx={{ mb: 3 }}>
                {error}
              </Alert>
            )}

            {success && (
              <Alert severity="success" sx={{ mb: 3 }}>
                {success}
              </Alert>
            )}

            <Box sx={{ mb: 3, display: 'flex', justifyContent: 'center' }}>
              <ToggleButtonGroup
                value={userType}
                exclusive
                onChange={handleUserTypeChange}
                aria-label="user type"
              >
                <ToggleButton value="CITY_MANAGER" aria-label="city manager">
                  City Manager
                </ToggleButton>
                <ToggleButton value="SERVICE_PROVIDER_ADMIN" aria-label="service provider">
                  Service Provider
                </ToggleButton>
              </ToggleButtonGroup>
            </Box>

            <form onSubmit={handleSubmit}>
              <Box sx={{ display: 'flex', gap: 2 }}>
                <TextField
                  fullWidth
                  label="First Name"
                  name="firstName"
                  value={formData.firstName}
                  onChange={handleChange}
                  margin="normal"
                  required
                />
                <TextField
                  fullWidth
                  label="Last Name"
                  name="lastName"
                  value={formData.lastName}
                  onChange={handleChange}
                  margin="normal"
                  required
                />
              </Box>

              <TextField
                fullWidth
                label="Username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                margin="normal"
                required
              />

              <TextField
                fullWidth
                label="Email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                margin="normal"
                required
              />

              <TextField
                fullWidth
                label="Organization"
                name="organization"
                value={formData.organization}
                onChange={handleChange}
                margin="normal"
                required
              />

              <TextField
                fullWidth
                label="Password"
                name="password"
                type={showPassword ? 'text' : 'password'}
                value={formData.password}
                onChange={handleChange}
                margin="normal"
                required
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword(!showPassword)}
                        edge="end"
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                fullWidth
                label="Confirm Password"
                name="confirmPassword"
                type={showPassword ? 'text' : 'password'}
                value={formData.confirmPassword}
                onChange={handleChange}
                margin="normal"
                required
              />

              <Button
                type="submit"
                fullWidth
                variant="contained"
                size="large"
                disabled={currentMutation.isPending}
                sx={{ mt: 3, mb: 2 }}
              >
                {currentMutation.isPending ? 'Registering...' : 'Register'}
              </Button>

              <Box sx={{ mt: 2, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  Already have an account?{' '}
                  <Link component={RouterLink} to="/login" underline="hover">
                    Sign in here
                  </Link>
                </Typography>
              </Box>
            </form>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
};

export default Register;
