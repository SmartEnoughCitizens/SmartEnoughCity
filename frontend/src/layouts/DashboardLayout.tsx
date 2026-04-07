/**
 * Map-centric dashboard layout with slim icon sidebar
 * ALL dashboards are always mounted for seamless navigation
 */

import { useState, useEffect } from "react";
import {
  Alert,
  Avatar,
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
import CloseIcon from "@mui/icons-material/Close";
import { useNavigate } from "react-router-dom";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import {
  toggleTheme,
  incrementNotificationBadge,
  setNotificationBadgeCount,
  clearRequestedNavigation,
} from "@/store/slices/uiSlice";
import { useLogout, useUserNotifications } from "@/hooks";
import { clearAuthentication } from "@/store/slices/authSlice";
import { getCreatableRoles, TRANSPORT_ACCESS } from "@/types";
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

export type DashboardView =
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
  const { theme, notificationBadgeCount, requestedNavigation } = useAppSelector(
    (state) => state.ui,
  );
  const [newNotifBanner, setNewNotifBanner] = useState(false);
  const logoutMutation = useLogout();

  // Role-based view visibility — mirrors the allowedRoles from the original router
  const canSeeView: Record<DashboardView, boolean> = {
    overview: roles.includes("City_Manager"),
    bus: TRANSPORT_ACCESS.bus.some((r) => roles.includes(r)),
    cycle: TRANSPORT_ACCESS.cycle.some((r) => roles.includes(r)),
    car: TRANSPORT_ACCESS.car.some((r) => roles.includes(r)),
    train: TRANSPORT_ACCESS.train.some((r) => roles.includes(r)),
    tram: TRANSPORT_ACCESS.tram.some((r) => roles.includes(r)),
    misc: true,
    notifications: true,
    users: canManageUsers,
  };

  // Default view: first view the user is allowed to see
  const defaultView =
    (Object.keys(canSeeView) as DashboardView[]).find((v) => canSeeView[v]) ??
    "notifications";

  // State to track active dashboard view with persistence
  const [activeView, setActiveView] = useState<DashboardView>(() => {
    const saved = localStorage.getItem(
      "activeDashboardView",
    ) as DashboardView | null;
    if (saved && canSeeView[saved]) return saved;
    return defaultView;
  });

  // Save active view to localStorage
  useEffect(() => {
    localStorage.setItem("activeDashboardView", activeView);
  }, [activeView]);

  // Consume Redux navigation requests (e.g. from notification deep-links)
  useEffect(() => {
    if (!requestedNavigation) return;
    const view = requestedNavigation.view as DashboardView;
    if (canSeeView[view]) setActiveView(view); // eslint-disable-line react-hooks/set-state-in-effect
    dispatch(clearRequestedNavigation());
  }, [requestedNavigation]); // eslint-disable-line react-hooks/exhaustive-deps

  // Seed badge count from backend on login
  const { data: notifData } = useUserNotifications(username || "", !!username);
  useEffect(() => {
    if (notifData?.totalCount !== undefined) {
      dispatch(setNotificationBadgeCount(Number(notifData.totalCount)));
    }
  }, [notifData?.totalCount, dispatch]);

  // Connect SSE as soon as dashboard loads (user is authenticated)
  useEffect(() => {
    if (!username) return;

    sseService.connect(username);

    const unsubscribe = sseService.subscribe(() => {
      dispatch(incrementNotificationBadge());
      setNewNotifBanner(true);
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
    localStorage.removeItem("activeDashboardView");
    await logoutMutation.mutateAsync();
    dispatch(clearAuthentication());
    navigate("/login");
  };

  const allNavItems = [
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
        <Box sx={{ position: "relative", display: "inline-flex" }}>
          <NotificationsIcon />
          {notificationBadgeCount > 0 && (
            <Box
              sx={{
                position: "absolute",
                top: -4,
                right: -6,
                minWidth: 16,
                height: 16,
                borderRadius: "8px",
                bgcolor: "primary.main",
                color: "#fff",
                fontSize: "0.6rem",
                fontWeight: 700,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                px: 0.4,
                border: "1.5px solid",
                borderColor: "background.paper",
              }}
            >
              {notificationBadgeCount > 9 ? "9+" : notificationBadgeCount}
            </Box>
          )}
        </Box>
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

  // Only show nav items the current user's roles permit
  const navItems = allNavItems.filter(
    (item) => !item.view || canSeeView[item.view],
  );

  const isActive = (view: DashboardView) => activeView === view;

  const panelSx = (view: DashboardView) => ({
    position: "absolute" as const,
    inset: 0,
    opacity: activeView === view ? 1 : 0,
    visibility:
      activeView === view ? ("visible" as const) : ("hidden" as const),
    pointerEvents: activeView === view ? ("auto" as const) : ("none" as const),
    transition:
      activeView === view
        ? "opacity 0.5s ease-in-out"
        : "opacity 0.5s ease-in-out, visibility 0s linear 0.5s",
  });

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
          <Tooltip
            key={item.view ?? item.label}
            title={item.label}
            placement="right"
            arrow
          >
            <IconButton
              onClick={() => {
                if (item.view) setActiveView(item.view);
                else if ("path" in item && item.path) navigate(item.path);
              }}
              sx={{
                width: 40,
                height: 40,
                borderRadius: "10px",
                color:
                  item.view && isActive(item.view)
                    ? "primary.main"
                    : "text.secondary",
                bgcolor:
                  item.view && isActive(item.view)
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
          display: "flex",
          flexDirection: "column",
        }}
      >
        {/* New notification banner */}
        {newNotifBanner && (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              gap: 1.5,
              px: 2.5,
              py: 1,
              bgcolor: "primary.main",
              color: "#fff",
              fontSize: "0.8rem",
              zIndex: 1300,
              flexShrink: 0,
            }}
          >
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: "50%",
                bgcolor: "#fff",
                flexShrink: 0,
              }}
            />
            <Typography sx={{ fontSize: "0.8rem", flex: 1 }}>
              You have new notifications.{" "}
              <Box
                component="span"
                onClick={() => {
                  setNewNotifBanner(false);
                  setActiveView("notifications");
                }}
                sx={{
                  textDecoration: "underline",
                  cursor: "pointer",
                  fontWeight: 600,
                }}
              >
                View now
              </Box>
            </Typography>
            <IconButton
              size="small"
              onClick={() => setNewNotifBanner(false)}
              sx={{ color: "#fff", p: 0.25 }}
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
        )}
        <Box sx={{ flex: 1, position: "relative", overflow: "hidden" }}>
          {/* Overview Dashboard — City_Manager only */}
          {canSeeView.overview && (
            <Box sx={panelSx("overview")}>
              <Dashboard onNavigate={setActiveView} />
            </Box>
          )}

          {/* Bus Dashboard */}
          {canSeeView.bus && (
            <Box sx={panelSx("bus")}>
              <BusDashboard />
            </Box>
          )}

          {/* Cycle Dashboard */}
          {canSeeView.cycle && (
            <Box sx={panelSx("cycle")}>
              <CycleDashboard />
            </Box>
          )}

          {/* Car Dashboard */}
          {canSeeView.car && (
            <Box sx={panelSx("car")}>
              <CarDashboard />
            </Box>
          )}

          {/* Train Dashboard */}
          {canSeeView.train && (
            <Box sx={panelSx("train")}>
              <TrainDashboard />
            </Box>
          )}

          {/* Tram Dashboard */}
          {canSeeView.tram && (
            <Box sx={panelSx("tram")}>
              <TramDashboard />
            </Box>
          )}

          {/* Misc Dashboard (Events & Pedestrians) — all authenticated users */}
          <Box sx={panelSx("misc")}>
            <MiscDashboard />
          </Box>

          {/* Notifications Page — all authenticated users */}
          <Box sx={panelSx("notifications")}>
            <NotificationsPage />
          </Box>

          {/* User Management Page */}
          {canManageUsers && (
            <Box sx={panelSx("users")}>
              <UserManagementPage />
            </Box>
          )}
        </Box>
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
