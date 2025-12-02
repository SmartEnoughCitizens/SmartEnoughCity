import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Box,
  Typography,
} from '@mui/material';
import {
  Dashboard,
  DirectionsCar,
  DirectionsBike,
  DirectionsBus,
  Train,
  Tram,
  DirectionsWalk,
  Event,
  Construction,
  Science,
  Notifications,
  People,
  Settings,
} from '@mui/icons-material';
import { selectUserRole } from '../../redux/slices/authSlice';
import { ROLES } from '../../constants';

const drawerWidth = 240;

interface SidebarProps {
  open: boolean;
}

interface MenuItem {
  text: string;
  icon?: React.ReactElement;
  path?: string;
  roles?: string[];
  divider?: boolean;
}

const Sidebar: React.FC<SidebarProps> = ({ open }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const userRole = useSelector(selectUserRole);

  const menuItems: MenuItem[] = [
    { text: 'Dashboard', icon: <Dashboard />, path: '/dashboard', roles: [] },
    { divider: true, text: 'Transport Indicators' },
    { text: 'Car', icon: <DirectionsCar />, path: '/indicators/car', roles: [] },
    { text: 'Cycle', icon: <DirectionsBike />, path: '/indicators/cycle', roles: [] },
    { text: 'Bus', icon: <DirectionsBus />, path: '/indicators/bus', roles: [] },
    { text: 'Train', icon: <Train />, path: '/indicators/train', roles: [] },
    { text: 'Tram', icon: <Tram />, path: '/indicators/tram', roles: [] },
    { text: 'Pedestrian', icon: <DirectionsWalk />, path: '/indicators/pedestrian', roles: [] },
    { divider: true, text: 'Events & Construction' },
    { text: 'Events', icon: <Event />, path: '/indicators/events', roles: [] },
    { text: 'Construction', icon: <Construction />, path: '/indicators/construction', roles: [] },
    { divider: true, text: 'Management' },
    { text: 'Simulation Center', icon: <Science />, path: '/simulation', roles: [] },
    { text: 'Notifications', icon: <Notifications />, path: '/notifications', roles: [] },
    {
      text: 'User Management',
      icon: <People />,
      path: '/users',
      roles: [ROLES.GOVERNMENT_ADMIN, ROLES.SERVICE_PROVIDER_ADMIN],
    },
    { text: 'System Status', icon: <Settings />, path: '/system-status', roles: [ROLES.GOVERNMENT_ADMIN] },
  ];

  const hasAccess = (item: MenuItem): boolean => {
    if (!item.roles || item.roles.length === 0) return true;
    if (!userRole) return false;
    return item.roles.includes(userRole);
  };

  const handleNavigation = (path: string): void => {
    navigate(path);
  };

  return (
    <Drawer
      variant="persistent"
      anchor="left"
      open={open}
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
          marginTop: '64px',
        },
      }}
    >
      <Box sx={{ overflow: 'auto' }}>
        <List>
          {menuItems.map((item, index) => {
            if (item.divider) {
              return (
                <Box key={index}>
                  <Divider sx={{ my: 1 }} />
                  <Typography
                    variant="caption"
                    sx={{
                      px: 2,
                      py: 1,
                      display: 'block',
                      color: 'text.secondary',
                      fontWeight: 'bold',
                      textTransform: 'uppercase',
                    }}
                  >
                    {item.text}
                  </Typography>
                </Box>
              );
            }

            if (!hasAccess(item)) return null;

            const isSelected = location.pathname === item.path;

            return (
              <ListItem key={index} disablePadding>
                <ListItemButton
                  selected={isSelected}
                  onClick={() => handleNavigation(item.path!)}
                >
                  <ListItemIcon sx={{ color: isSelected ? 'primary.main' : 'inherit' }}>
                    {item.icon}
                  </ListItemIcon>
                  <ListItemText primary={item.text} />
                </ListItemButton>
              </ListItem>
            );
          })}
        </List>
      </Box>
    </Drawer>
  );
};

export default Sidebar;
