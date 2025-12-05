import React, { useState } from 'react';
import {
  Grid,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
} from '@mui/material';
import { People, Add, Edit, Delete } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import { getRoleDisplayName } from '../../utils/helpers';
import { ROLES } from '../../constants';
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser } from '../../hooks/useUsers';
import { type UserProfile, type CreateUserRequest, type UpdateUserRequest, UserRole } from '../../types';

interface UserFormData {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  department: string;
  password?: string;
}

const UserManagement: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [currentUser, setCurrentUser] = useState<UserFormData>({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    role: ROLES.SERVICE_PROVIDER_USER,
    department: '',
  });
  const [editingUserId, setEditingUserId] = useState<string | null>(null);

  // TanStack Query hooks
  const { data: users = [], isLoading, error } = useUsers();
  const createUserMutation = useCreateUser();
  const updateUserMutation = useUpdateUser();
  const deleteUserMutation = useDeleteUser();

  const mockUsers: UserProfile[] = [
    {
      id: '1',
      username: 'admin',
      email: 'admin@city.gov',
      firstName: 'John',
      lastName: 'Admin',
      role: ROLES.GOVERNMENT_ADMIN,
      department: 'City Government',
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: '2',
      username: 'manager1',
      email: 'manager@city.gov',
      firstName: 'Jane',
      lastName: 'Manager',
      role: ROLES.CITY_MANAGER,
      department: 'City Transport Dept',
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: '3',
      username: 'provider_admin',
      email: 'admin@transport.com',
      firstName: 'Bob',
      lastName: 'Provider',
      role: ROLES.SERVICE_PROVIDER_ADMIN,
      department: 'City Transport Co.',
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
    {
      id: '4',
      username: 'user1',
      email: 'user1@transport.com',
      firstName: 'Alice',
      lastName: 'User',
      role: ROLES.SERVICE_PROVIDER_USER,
      department: 'City Transport Co.',
      isActive: true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    },
  ];

  // Use mock data if API fails or returns empty
  const displayUsers = error || users.length === 0 ? mockUsers : users;

  const handleOpenDialog = (user?: UserProfile) => {
    if (user) {
      setEditMode(true);
      setEditingUserId(user.id);
      setCurrentUser({
        username: user.username,
        email: user.email,
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        role: user.role,
        department: user.department || '',
      });
    } else {
      setEditMode(false);
      setEditingUserId(null);
      setCurrentUser({
        username: '',
        email: '',
        firstName: '',
        lastName: '',
        role: ROLES.SERVICE_PROVIDER_USER,
        department: '',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setCurrentUser({
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      role: ROLES.SERVICE_PROVIDER_USER,
      department: '',
    });
    setEditingUserId(null);
  };

  const handleSaveUser = () => {
    if (editMode && editingUserId) {
      // Update existing user
      const updateData: UpdateUserRequest = {
        email: currentUser.email,
        firstName: currentUser.firstName,
        lastName: currentUser.lastName,
        department: currentUser.department,
      };

      updateUserMutation.mutate(
        { id: editingUserId, data: updateData },
        {
          onSuccess: () => {
            handleCloseDialog();
          },
          onError: (error) => {
            console.error('Error updating user:', error);
          },
        }
      );
    } else {
      // Create new user
      const createData: CreateUserRequest = {
        username: currentUser.username,
        email: currentUser.email,
        password: currentUser.password || 'defaultPassword123', // You might want to add a password field
        role: currentUser.role,
        firstName: currentUser.firstName,
        lastName: currentUser.lastName,
        department: currentUser.department,
      };

      createUserMutation.mutate(createData, {
        onSuccess: () => {
          handleCloseDialog();
        },
        onError: (error) => {
          console.error('Error creating user:', error);
        },
      });
    }
  };

  const handleDeleteUser = (id: string) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      deleteUserMutation.mutate(id, {
        onError: (error) => {
          console.error('Error deleting user:', error);
        },
      });
    }
  };

  const getRoleColor = (role: UserRole): 'error' | 'warning' | 'info' | 'default' => {
    switch (role) {
      case ROLES.GOVERNMENT_ADMIN:
        return 'error';
      case ROLES.CITY_MANAGER:
        return 'warning';
      case ROLES.SERVICE_PROVIDER_ADMIN:
        return 'info';
      default:
        return 'default';
    }
  };

  const usersByRole = {
    [ROLES.GOVERNMENT_ADMIN]: displayUsers.filter((u) => u.role === ROLES.GOVERNMENT_ADMIN).length,
    [ROLES.CITY_MANAGER]: displayUsers.filter((u) => u.role === ROLES.CITY_MANAGER).length,
    [ROLES.SERVICE_PROVIDER_ADMIN]: displayUsers.filter((u) => u.role === ROLES.SERVICE_PROVIDER_ADMIN).length,
    [ROLES.SERVICE_PROVIDER_USER]: displayUsers.filter((u) => u.role === ROLES.SERVICE_PROVIDER_USER).length,
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <People sx={{ fontSize: 40 }} />
            User Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage system users and their permissions
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
          Add User
        </Button>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Users" value={displayUsers.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Admins" value={usersByRole[ROLES.GOVERNMENT_ADMIN]} color="error" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="City Managers" value={usersByRole[ROLES.CITY_MANAGER]} color="warning" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Service Providers"
            value={usersByRole[ROLES.SERVICE_PROVIDER_ADMIN] + usersByRole[ROLES.SERVICE_PROVIDER_USER]}
            color="info"
          />
        </Grid>
      </Grid>

      {/* Users Table */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            All Users
          </Typography>
          <TableContainer component={Paper} variant="outlined">
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Username</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Email</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell>Department</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {displayUsers.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>{user.username}</TableCell>
                    <TableCell>
                      {user.firstName} {user.lastName}
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Chip label={getRoleDisplayName(user.role)} color={getRoleColor(user.role)} size="small" />
                    </TableCell>
                    <TableCell>{user.department}</TableCell>
                    <TableCell>
                      <Chip label={user.isActive ? 'Active' : 'Inactive'} color={user.isActive ? 'success' : 'default'} size="small" />
                    </TableCell>
                    <TableCell>
                      <IconButton size="small" onClick={() => handleOpenDialog(user)}>
                        <Edit fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDeleteUser(user.id)}
                        disabled={deleteUserMutation.isPending}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      {/* Add/Edit User Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editMode ? 'Edit User' : 'Add New User'}</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              fullWidth
              label="Username"
              value={currentUser.username}
              onChange={(e) => setCurrentUser({ ...currentUser, username: e.target.value })}
              disabled={editMode}
            />
            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                fullWidth
                label="First Name"
                value={currentUser.firstName}
                onChange={(e) => setCurrentUser({ ...currentUser, firstName: e.target.value })}
              />
              <TextField
                fullWidth
                label="Last Name"
                value={currentUser.lastName}
                onChange={(e) => setCurrentUser({ ...currentUser, lastName: e.target.value })}
              />
            </Box>
            <TextField
              fullWidth
              label="Email"
              type="email"
              value={currentUser.email}
              onChange={(e) => setCurrentUser({ ...currentUser, email: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>Role</InputLabel>
              <Select
                value={currentUser.role}
                onChange={(e) => setCurrentUser({ ...currentUser, role: e.target.value as UserRole })}
                label="Role"
              >
                <MenuItem value={ROLES.GOVERNMENT_ADMIN}>Government Admin</MenuItem>
                <MenuItem value={ROLES.CITY_MANAGER}>City Manager</MenuItem>
                <MenuItem value={ROLES.SERVICE_PROVIDER_ADMIN}>Service Provider Admin</MenuItem>
                <MenuItem value={ROLES.SERVICE_PROVIDER_USER}>Service Provider User</MenuItem>
              </Select>
            </FormControl>
            <TextField
              fullWidth
              label="Department"
              value={currentUser.department}
              onChange={(e) => setCurrentUser({ ...currentUser, department: e.target.value })}
            />
            {!editMode && (
              <TextField
                fullWidth
                label="Password"
                type="password"
                value={currentUser.password || ''}
                onChange={(e) => setCurrentUser({ ...currentUser, password: e.target.value })}
              />
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSaveUser}
            disabled={createUserMutation.isPending || updateUserMutation.isPending}
          >
            {(createUserMutation.isPending || updateUserMutation.isPending) ? (
              <CircularProgress size={24} />
            ) : (
              editMode ? 'Update' : 'Create'
            )}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default UserManagement;
