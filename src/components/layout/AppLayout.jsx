import React, { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { useSelector, useDispatch } from 'react-redux';
import { Box, Alert, Snackbar } from '@mui/material';
import Navbar from './Navbar';
import Sidebar from './Sidebar';
import { selectSidebarOpen, selectIsOffline, selectSnackbar, hideSnackbar } from '../../redux/slices/uiSlice';

const drawerWidth = 240;

const AppLayout = () => {
  const dispatch = useDispatch();
  const sidebarOpen = useSelector(selectSidebarOpen);
  const isOffline = useSelector(selectIsOffline);
  const snackbar = useSelector(selectSnackbar);

  const handleCloseSnackbar = () => {
    dispatch(hideSnackbar());
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <Navbar />
      <Sidebar open={sidebarOpen} />

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          mt: 8,
          ml: sidebarOpen ? 0 : `-${drawerWidth}px`,
          transition: (theme) =>
            theme.transitions.create(['margin'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.leavingScreen,
            }),
        }}
      >
        {isOffline && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            You are currently in offline mode. Some features may be unavailable.
          </Alert>
        )}

        <Outlet />
      </Box>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default AppLayout;
