import React, { useState, useEffect } from 'react';
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
} from '@mui/material';
import { People, Add, Edit, Delete } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import { userService } from '../../services/userService';
import { getRoleDisplayName } from '../../utils/helpers';
import { ROLES } from '../../constants';

const UserManagement = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [openDialog, setOpenDialog] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [currentUser, setCurrentUser] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    role: ROLES.SERVICE_PROVIDER_USER,
    organization: '',
  });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data = await userService.getAllUsers();
      setUsers(data);
    } catch (error) {
      console.error('Error fetching users:', error);
      setUsers(mockUsers);
    } finally {
      setLoading(false);
    }
  };

  const mockUsers = [
    {
      id: 1,
      username: 'admin',
      email: 'admin@city.gov',
      firstName: 'John',
      lastName: 'Admin',
      role: ROLES.GOVERNMENT_ADMIN,
      organization: 'City Government',
      status: 'Active',
    },
    {
      id: 2,
      username: 'manager1',
      email: 'manager@city.gov',
      firstName: 'Jane',
      lastName: 'Manager',
      role: ROLES.CITY_MANAGER,
      organization: 'City Transport Dept',
      status: 'Active',
    },
    {
      id: 3,
      username: 'provider_admin',
      email: 'admin@transport.com',
      firstName: 'Bob',
      lastName: 'Provider',
      role: ROLES.SERVICE_PROVIDER_ADMIN,
      organization: 'City Transport Co.',
      status: 'Active',
    },
    {
      id: 4,
      username: 'user1',
      email: 'user1@transport.com',
      firstName: 'Alice',
      lastName: 'User',
      role: ROLES.SERVICE_PROVIDER_USER,
      organization: 'City Transport Co.',
      status: 'Active',
    },
  ];

  const handleOpenDialog = (user = null) => {
    if (user) {
      setEditMode(true);
      setCurrentUser(user);
    } else {
      setEditMode(false);
      setCurrentUser({
        username: '',
        email: '',
        firstName: '',
        lastName: '',
        role: ROLES.SERVICE_PROVIDER_USER,
        organization: '',
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
      organization: '',
    });
  };

  const handleSaveUser = async () => {
    try {
      if (editMode) {
        await userService.updateUser(currentUser.id, currentUser);
      } else {
        await userService.createServiceProviderUser(currentUser);
      }
      fetchUsers();
      handleCloseDialog();
    } catch (error) {
      console.error('Error saving user:', error);
    }
  };

  const handleDeleteUser = async (id) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        await userService.deleteUser(id);
        fetchUsers();
      } catch (error) {
        console.error('Error deleting user:', error);
      }
    }
  };

  const getRoleColor = (role) => {
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
    [ROLES.GOVERNMENT_ADMIN]: users.filter((u) => u.role === ROLES.GOVERNMENT_ADMIN).length,
    [ROLES.CITY_MANAGER]: users.filter((u) => u.role === ROLES.CITY_MANAGER).length,
    [ROLES.SERVICE_PROVIDER_ADMIN]: users.filter((u) => u.role === ROLES.SERVICE_PROVIDER_ADMIN).length,
    [ROLES.SERVICE_PROVIDER_USER]: users.filter((u) => u.role === ROLES.SERVICE_PROVIDER_USER).length,
  };

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
          <MetricCard title="Total Users" value={users.length} color="primary" />
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
                  <TableCell>Organization</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>{user.username}</TableCell>
                    <TableCell>
                      {user.firstName} {user.lastName}
                    </TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Chip label={getRoleDisplayName(user.role)} color={getRoleColor(user.role)} size="small" />
                    </TableCell>
                    <TableCell>{user.organization}</TableCell>
                    <TableCell>
                      <Chip label={user.status || 'Active'} color="success" size="small" />
                    </TableCell>
                    <TableCell>
                      <IconButton size="small" onClick={() => handleOpenDialog(user)}>
                        <Edit fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="error" onClick={() => handleDeleteUser(user.id)}>
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
                onChange={(e) => setCurrentUser({ ...currentUser, role: e.target.value })}
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
              label="Organization"
              value={currentUser.organization}
              onChange={(e) => setCurrentUser({ ...currentUser, organization: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button variant="contained" onClick={handleSaveUser}>
            {editMode ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default UserManagement;
