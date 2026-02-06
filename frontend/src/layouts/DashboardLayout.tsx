/**
 * Map-centric dashboard layout with slim icon sidebar
 */

import { useState } from "react";
import {
  Box,
  IconButton,
  Typography,
  Tooltip,
  Badge,
  Menu,
  MenuItem,
  Avatar,
  Divider,
} from "@mui/material";
import DashboardIcon from "@mui/icons-material/Dashboard";
import DirectionsBusIcon from "@mui/icons-material/DirectionsBus";
import DirectionsBikeIcon from "@mui/icons-material/DirectionsBike";
import NotificationsIcon from "@mui/icons-material/Notifications";
import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import LogoutIcon from "@mui/icons-material/Logout";
import Brightness4Icon from "@mui/icons-material/Brightness4";
import Brightness7Icon from "@mui/icons-material/Brightness7";
import { useNavigate, useLocation } from "react-router-dom";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import { toggleTheme } from "@/store/slices/uiSlice";
import { clearAuthentication } from "@/store/slices/authSlice";
import { useLogout } from "@/hooks";

interface DashboardLayoutProps {
  children: React.ReactNode;
}

export const DashboardLayout = ({ children }: DashboardLayoutProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useAppDispatch();
  const { username } = useAppSelector((state) => state.auth);
  const { theme, notificationBadgeCount } = useAppSelector((state) => state.ui);
  const logoutMutation = useLogout();

  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

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

  const navItems = [
    { icon: <DashboardIcon />, path: "/dashboard", label: "Overview" },
    { icon: <DirectionsBusIcon />, path: "/dashboard/bus", label: "Bus Data" },
    {
      icon: <DirectionsBikeIcon />,
      path: "/dashboard/cycle",
      label: "Cycles",
    },
    {
      icon: (
        <Badge badgeContent={notificationBadgeCount} color="error">
          <NotificationsIcon />
        </Badge>
      ),
      path: "/dashboard/notifications",
      label: "Notifications",
    },
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
    </Box>
  );
};
