/**
 * Map-centric dashboard layout with slim icon sidebar
 */

import { useState, useEffect } from "react";
import {
  Alert,
  Avatar,
  Badge,
  Box,
  Divider,
  IconButton,
  Menu,
  MenuItem,
  Snackbar,
  Tooltip,
  Typography,
} from "@mui/material";
import DashboardIcon from "@mui/icons-material/Dashboard";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import DirectionsCarIcon from "@mui/icons-material/DirectionsCar";
import TrainIcon from "@mui/icons-material/Train";
import NotificationsIcon from "@mui/icons-material/Notifications";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import EditIcon from "@mui/icons-material/Edit";
import LockIcon from "@mui/icons-material/Lock";
import LogoutIcon from "@mui/icons-material/Logout";
import Brightness4Icon from "@mui/icons-material/Brightness4";
import Brightness7Icon from "@mui/icons-material/Brightness7";
import { useNavigate, useLocation } from "react-router-dom";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import {
  toggleTheme,
  incrementNotificationBadge,
} from "@/store/slices/uiSlice";
import { clearAuthentication } from "@/store/slices/authSlice";
import { useLogout } from "@/hooks";
import { usePermissions } from "@/hooks/usePermissions";
import { getCreatableRoles } from "@/types";
import { EditProfileDialog } from "@/components/profile/EditProfileDialog";
import { ChangePasswordDialog } from "@/components/profile/ChangePasswordDialog";
import sseService from "@/services/sseService";

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export const DashboardLayout = ({ children }: DashboardLayoutProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { username, roles } = useAppSelector((state) => state.auth);
  const canManageUsers = getCreatableRoles(roles).length > 0;
  const { theme, notificationBadgeCount } = useAppSelector((state) => state.ui);
  const logoutMutation = useLogout();

  // RBAC: derive category visibility from roles
  const { canViewCategory } = usePermissions();

  // Connect SSE as soon as dashboard loads (user is authenticated)
  useEffect(() => {
    if (!username) return;

    sseService.connect(username);

    const unsubscribe = sseService.subscribe(() => {
      dispatch(incrementNotificationBadge());
    });

    return () => {
      unsubscribe();
      sseService.disconnect();
    };
  }, [username, dispatch]);

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [editProfileOpen, setEditProfileOpen] = useState(false);
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);
  const [snackbar, setSnackbar] = useState<{
    open: boolean;
    message: string;
  }>({ open: false, message: "" });

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    handleMenuClose();
    await logoutMutation.mutateAsync();
    dispatch(clearAuthentication());
    navigate("/login");
  };

  // Transport-category nav items filtered by RBAC role permissions.
  // Overview and Notifications are always visible; transport sections
  // are shown only when the user has access to that category.
  const navItems = [
    // Always visible
    { icon: <DashboardIcon />, path: "/dashboard", label: "Overview" },

    // Transport sections — hidden when user lacks category access
    ...(canViewCategory("bus")
      ? [
          {
            icon: <DirectionsBusIcon />,
            path: "/dashboard/bus",
            label: "Bus Data",
          },
        ]
      : []),
    ...(canViewCategory("cycle")
      ? [
          {
            icon: <DirectionsBikeIcon />,
            path: "/dashboard/cycle",
            label: "Cycles",
          },
        ]
      : []),
    ...(canViewCategory("car")
      ? [
          {
            icon: <DirectionsCarIcon />,
            path: "/dashboard/car",
            label: "Car",
          },
        ]
      : []),
    ...(canViewCategory("train")
      ? [
          {
            icon: <TrainIcon />,
            path: "/dashboard/train",
            label: "Trains",
          },
        ]
      : []),

    // Always visible
    {
      icon: (
        <Badge badgeContent={notificationBadgeCount} color="error">
          <NotificationsIcon />
        </Badge>
      ),
      path: "/dashboard/notifications",
      label: "Notifications",
    },

    // Only for users who can manage other users
    ...(canManageUsers
      ? [
          {
            icon: <PersonAddIcon />,
            path: "/dashboard/users",
            label: "User Management",
          },
        ]
      : []),
  ];

  const isActive = (path: string) => location.pathname === path;

  return (
    <Box sx={{ display: "flex", height: "100vh", overflow: "hidden" }}>
      {/* Slim icon rail */}
      <Box
        sx={{
          width: 56,
          flexShrink: 0,
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          py: 1.5,
          gap: 0.5,
          bgcolor: (t) =>
            t.palette.mode === "dark"
              ? "rgba(15, 23, 42, 0.95)"
              : "rgba(255, 255, 255, 0.95)",
          borderRight: 1,
          borderColor: "divider",
          zIndex: 1200,
          backdropFilter: "blur(16px)",
        }}
      >
        {/* Brand mark */}
        <Box
          sx={{
            width: 36,
            height: 36,
            borderRadius: "10px",
            bgcolor: "primary.main",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            mb: 1,
            cursor: "pointer",
          }}
          onClick={() => navigate("/dashboard")}
        >
          <Typography
            sx={{ color: "#fff", fontWeight: 800, fontSize: "0.9rem" }}
          >
            H
          </Typography>
        </Box>

        {/* Nav items */}
        {navItems.map((item) => (
          <Tooltip key={item.path} title={item.label} placement="right" arrow>
            <IconButton
              onClick={() => navigate(item.path)}
              sx={{
                width: 40,
                height: 40,
                borderRadius: "10px",
                color: isActive(item.path) ? "primary.main" : "text.secondary",
                bgcolor: isActive(item.path)
                  ? (t) =>
                      t.palette.mode === "dark"
                        ? "rgba(96, 165, 250, 0.12)"
                        : "rgba(37, 99, 235, 0.08)"
                  : "transparent",
                "&:hover": {
                  bgcolor: (t) =>
                    t.palette.mode === "dark"
                      ? "rgba(255,255,255,0.06)"
                      : "rgba(0,0,0,0.04)",
                },
              }}
            >
              {item.icon}
            </IconButton>
          </Tooltip>
        ))}

        {/* Spacer */}
        <Box sx={{ flexGrow: 1 }} />

        {/* Theme toggle */}
        <Tooltip
          title={theme === "dark" ? "Light mode" : "Dark mode"}
          placement="right"
          arrow
        >
          <IconButton
            onClick={() => dispatch(toggleTheme())}
            sx={{
              width: 40,
              height: 40,
              borderRadius: "10px",
              color: "text.secondary",
            }}
          >
            {theme === "dark" ? <Brightness7Icon /> : <Brightness4Icon />}
          </IconButton>
        </Tooltip>

        {/* User avatar */}
        <Tooltip title={username || "Account"} placement="right" arrow>
          <IconButton onClick={handleMenuOpen} sx={{ p: 0.5 }}>
            <Avatar
              sx={{
                width: 32,
                height: 32,
                bgcolor: "primary.main",
                fontSize: "0.85rem",
                fontWeight: 700,
              }}
            >
              {username?.charAt(0).toUpperCase() || "U"}
            </Avatar>
          </IconButton>
        </Tooltip>

        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={handleMenuClose}
          anchorOrigin={{ vertical: "center", horizontal: "right" }}
          transformOrigin={{ vertical: "center", horizontal: "left" }}
        >
          <MenuItem disabled>
            <AccountCircleIcon sx={{ mr: 1 }} />
            {username}
          </MenuItem>
          <Divider />
          <MenuItem
            onClick={() => {
              handleMenuClose();
              setEditProfileOpen(true);
            }}
          >
            <EditIcon sx={{ mr: 1 }} />
            Edit Profile
          </MenuItem>
          <MenuItem
            onClick={() => {
              handleMenuClose();
              setChangePasswordOpen(true);
            }}
          >
            <LockIcon sx={{ mr: 1 }} />
            Change Password
          </MenuItem>
          <Divider />
          <MenuItem onClick={handleLogout}>
            <LogoutIcon sx={{ mr: 1 }} />
            Logout
          </MenuItem>
        </Menu>
      </Box>

      {/* Main content — full bleed for map pages */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          height: "100vh",
          overflow: "hidden",
          position: "relative",
        }}
      >
        {children}
      </Box>

      <EditProfileDialog
        open={editProfileOpen}
        onClose={() => setEditProfileOpen(false)}
        onSuccess={(message) => {
          setEditProfileOpen(false);
          setSnackbar({ open: true, message });
        }}
      />
      <ChangePasswordDialog
        open={changePasswordOpen}
        onClose={() => setChangePasswordOpen(false)}
        onSuccess={(message) => {
          setChangePasswordOpen(false);
          setSnackbar({ open: true, message });
        }}
      />
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
      >
        <Alert
          severity="success"
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};
