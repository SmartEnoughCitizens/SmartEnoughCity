/**
 * Map-centric dashboard layout with slim icon sidebar
 * ALL dashboards are always mounted for seamless navigation
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
import TramIcon from "@mui/icons-material/Tram";
import NotificationsIcon from "@mui/icons-material/Notifications";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import AccountCircleIcon from "@mui/icons-material/AccountCircle";
import EditIcon from "@mui/icons-material/Edit";
import LockIcon from "@mui/icons-material/Lock";
import LogoutIcon from "@mui/icons-material/Logout";
import Brightness4Icon from "@mui/icons-material/Brightness4";
import Brightness7Icon from "@mui/icons-material/Brightness7";
import EventNoteIcon from "@mui/icons-material/EventNote";
import ReportProblemIcon from "@mui/icons-material/ReportProblem";
import { useNavigate } from "react-router-dom";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import {
  toggleTheme,
  incrementNotificationBadge,
} from "@/store/slices/uiSlice";
import { clearAuthentication } from "@/store/slices/authSlice";
import { useLogout } from "@/hooks";
import { getCreatableRoles } from "@/types";
import { EditProfileDialog } from "@/components/profile/EditProfileDialog";
import { ChangePasswordDialog } from "@/components/profile/ChangePasswordDialog";
import sseService from "@/services/sseService";
import { Dashboard } from "@/pages/Dashboard";
import { BusDashboard } from "@/pages/BusDashboard";
import { CycleDashboard } from "@/pages/CycleDashboard";
import { CarDashboard } from "@/pages/CarDashboard";
import { TrainDashboard } from "@/pages/TrainDashboard";
import { TramDashboard } from "@/pages/TramDashboard";
import { MiscDashboard } from "@/pages/MiscDashboard";
import { NotificationsPage } from "@/pages/NotificationsPage";
import { UserManagementPage } from "@/pages/UserManagementPage";
import { MapContainer, TileLayer } from "react-leaflet";
import "leaflet/dist/leaflet.css";

type DashboardView =
  | "overview"
  | "bus"
  | "cycle"
  | "car"
  | "train"
  | "tram"
  | "misc"
  | "notifications"
  | "users";

export const DashboardLayout = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const { username, roles } = useAppSelector((state) => state.auth);
  const canManageUsers = getCreatableRoles(roles).length > 0;
  const { theme, notificationBadgeCount } = useAppSelector((state) => state.ui);
  const logoutMutation = useLogout();

  // State to track active dashboard view with persistence
  const [activeView, setActiveView] = useState<DashboardView>(() => {
    const saved = localStorage.getItem("activeDashboardView");
    return (saved as DashboardView) || "overview";
  });

  // Save active view to localStorage
  useEffect(() => {
    localStorage.setItem("activeDashboardView", activeView);
  }, [activeView]);

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

  const navItems = [
    {
      icon: <DashboardIcon />,
      view: "overview" as DashboardView,
      label: "Overview",
    },
    {
      icon: <DirectionsBusIcon />,
      view: "bus" as DashboardView,
      label: "Bus Data",
    },
    {
      icon: <DirectionsBikeIcon />,
      view: "cycle" as DashboardView,
      label: "Cycles",
    },
    {
      icon: <DirectionsCarIcon />,
      view: "car" as DashboardView,
      label: "Car",
    },
    {
      icon: <TrainIcon />,
      view: "train" as DashboardView,
      label: "Trains",
    },
    {
      icon: <TramIcon />,
      view: "tram" as DashboardView,
      label: "Trams",
    },
    {
      icon: <EventNoteIcon />,
      view: "misc" as DashboardView,
      label: "Events & Pedestrians",
    },
    {
      icon: <ReportProblemIcon />,
      path: "/dashboard/disruptions",
      label: "Disruptions",
    },
    {
      icon: (
        <Badge badgeContent={notificationBadgeCount} color="error">
          <NotificationsIcon />
        </Badge>
      ),
      view: "notifications" as DashboardView,
      label: "Notifications",
    },
    ...(canManageUsers
      ? [
          {
            icon: <PersonAddIcon />,
            view: "users" as DashboardView,
            label: "User Management",
          },
        ]
      : []),
  ];

  const isActive = (view: DashboardView) => activeView === view;

  return (
    <Box sx={{ display: "flex", height: "100vh", overflow: "hidden" }}>
      {/* Tile warmer — loads Dublin tiles into browser cache on login.
          Positioned off-screen with real dimensions so Leaflet fetches tiles.
          All other maps in the app reuse these cached tile images. */}
      <div
        style={{
          position: "absolute",
          left: -9999,
          top: 0,
          width: 512,
          height: 512,
          pointerEvents: "none",
          visibility: "hidden",
        }}
      >
        <MapContainer
          center={[53.3498, -6.2603]}
          zoom={13}
          zoomControl={false}
          attributionControl={false}
          style={{ width: "100%", height: "100%" }}
        >
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        </MapContainer>
      </div>
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
          onClick={() => setActiveView("overview")}
        >
          <Typography
            sx={{ color: "#fff", fontWeight: 800, fontSize: "0.9rem" }}
          >
            H
          </Typography>
        </Box>

        {/* Nav items */}
        {navItems.map((item) => (
          <Tooltip key={item.view} title={item.label} placement="right" arrow>
            <IconButton
              onClick={() => setActiveView(item.view)}
              sx={{
                width: 40,
                height: 40,
                borderRadius: "10px",
                color: isActive(item.view) ? "primary.main" : "text.secondary",
                bgcolor: isActive(item.view)
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
        {/* Overview Dashboard */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "overview" ? "visible" : "hidden",
            opacity: activeView === "overview" ? 1 : 0,
            pointerEvents: activeView === "overview" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <Dashboard />
        </Box>

        {/* Bus Dashboard */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "bus" ? "visible" : "hidden",
            opacity: activeView === "bus" ? 1 : 0,
            pointerEvents: activeView === "bus" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <BusDashboard />
        </Box>

        {/* Cycle Dashboard */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "cycle" ? "visible" : "hidden",
            opacity: activeView === "cycle" ? 1 : 0,
            pointerEvents: activeView === "cycle" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <CycleDashboard />
        </Box>

        {/* Car Dashboard */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "car" ? "visible" : "hidden",
            opacity: activeView === "car" ? 1 : 0,
            pointerEvents: activeView === "car" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <CarDashboard />
        </Box>

        {/* Train Dashboard */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "train" ? "visible" : "hidden",
            opacity: activeView === "train" ? 1 : 0,
            pointerEvents: activeView === "train" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <TrainDashboard />
        </Box>

        {/* Tram Dashboard */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "tram" ? "visible" : "hidden",
            opacity: activeView === "tram" ? 1 : 0,
            pointerEvents: activeView === "tram" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <TramDashboard />
        </Box>

        {/* Misc Dashboard (Events & Pedestrians) */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "misc" ? "visible" : "hidden",
            opacity: activeView === "misc" ? 1 : 0,
            pointerEvents: activeView === "misc" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <MiscDashboard />
        </Box>

        {/* Notifications Page */}
        <Box
          sx={{
            position: "absolute",
            inset: 0,
            visibility: activeView === "notifications" ? "visible" : "hidden",
            opacity: activeView === "notifications" ? 1 : 0,
            pointerEvents: activeView === "notifications" ? "auto" : "none",
            transition: "opacity 0.15s ease-in-out",
          }}
        >
          <NotificationsPage />
        </Box>

        {/* User Management Page */}
        {canManageUsers && (
          <Box
            sx={{
              position: "absolute",
              inset: 0,
              visibility: activeView === "users" ? "visible" : "hidden",
              opacity: activeView === "users" ? 1 : 0,
              pointerEvents: activeView === "users" ? "auto" : "none",
              transition: "opacity 0.15s ease-in-out",
            }}
          >
            <UserManagementPage />
          </Box>
        )}
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
