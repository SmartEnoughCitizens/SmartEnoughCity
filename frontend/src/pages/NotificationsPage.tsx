/**
 * Notifications page — streamlined, compact list
 *
 * Write-permission users (Admin roles) can dismiss individual notifications.
 * Read-only users (Provider roles) can view but not remove notifications.
 */

import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  Chip,
  CircularProgress,
  Divider,
  IconButton,
  Tooltip,
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import { useEffect } from "react";
import { useUserNotifications, useDismissNotification } from "@/hooks";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import { setNotificationBadgeCount } from "@/store/slices/uiSlice";
import { Priority, NotificationType } from "@/types";
import { usePermissions } from "@/hooks/usePermissions";

const getPriorityColor = (priority: Priority) => {
  switch (priority) {
    case Priority.URGENT: {
      return "error";
    }
    case Priority.HIGH: {
      return "warning";
    }
    case Priority.MEDIUM: {
      return "info";
    }
    case Priority.LOW: {
      return "default";
    }
  }
};

const getTypeColor = (type: NotificationType) => {
  switch (type) {
    case NotificationType.ALERT: {
      return "error";
    }
    case NotificationType.ROUTE_RECOMMENDATION: {
      return "primary";
    }
    case NotificationType.UPDATE: {
      return "info";
    }
    case NotificationType.SYSTEM: {
      return "default";
    }
  }
};

export const NotificationsPage = () => {
  const { username } = useAppSelector((state) => state.auth);
  const dispatch = useAppDispatch();

  // RBAC: only Admin-level roles can dismiss notifications
  const { canWrite } = usePermissions();

  // Clear badge when user views notifications
  useEffect(() => {
    dispatch(setNotificationBadgeCount(0));
  }, [dispatch]);

  const { data, isLoading } = useUserNotifications(username || "", !!username);
  const dismissMutation = useDismissNotification(username || "");

  if (isLoading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100%",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  // Don't early-return on error — SSE notifications may still arrive

  return (
    <Box
      sx={{
        height: "100%",
        overflow: "auto",
        p: 3,
        bgcolor: (t) => t.palette.background.default,
      }}
    >
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          mb: 2,
        }}
      >
        <Typography variant="h4">Notifications</Typography>
        {/* Subtle read-only indicator for Provider roles */}
        {!canWrite && (
          <Chip
            label="Read only"
            size="small"
            variant="outlined"
            color="default"
            sx={{ fontSize: "0.7rem" }}
          />
        )}
      </Box>

      <Paper
        elevation={0}
        sx={{ borderRadius: 3, maxWidth: 720, overflow: "hidden" }}
      >
        {!data?.notifications || data.notifications.length === 0 ? (
          <Box sx={{ p: 4, textAlign: "center" }}>
            <Typography variant="body1" color="text.secondary">
              No notifications available
            </Typography>
          </Box>
        ) : (
          <List disablePadding>
            {data.notifications.map((notification, index) => (
              <Box key={notification.id}>
                <ListItem
                  sx={{
                    py: 1.5,
                    px: 2,
                    bgcolor: notification.read ? "transparent" : "action.hover",
                  }}
                  secondaryAction={
                    canWrite ? (
                      <Tooltip title="Dismiss notification" arrow>
                        <span>
                          <IconButton
                            edge="end"
                            size="small"
                            color="default"
                            disabled={dismissMutation.isPending}
                            onClick={() =>
                              dismissMutation.mutate(notification.id)
                            }
                            sx={{ opacity: 0.5, "&:hover": { opacity: 1 } }}
                          >
                            <DeleteOutlineIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    ) : (
                      <Tooltip title="Read-only access — cannot dismiss" arrow>
                        <span>
                          <IconButton edge="end" size="small" disabled>
                            <DeleteOutlineIcon fontSize="small" />
                          </IconButton>
                        </span>
                      </Tooltip>
                    )
                  }
                >
                  <ListItemText
                    primary={
                      <Box
                        sx={{
                          display: "flex",
                          gap: 0.75,
                          mb: 0.75,
                          alignItems: "center",
                          flexWrap: "wrap",
                        }}
                      >
                        <Chip
                          label={notification.type}
                          size="small"
                          color={getTypeColor(notification.type)}
                        />
                        <Chip
                          label={notification.priority}
                          size="small"
                          color={getPriorityColor(notification.priority)}
                        />
                        {!notification.read && (
                          <Chip
                            label="NEW"
                            size="small"
                            color="primary"
                            variant="outlined"
                          />
                        )}
                      </Box>
                    }
                    secondary={
                      <>
                        <Typography
                          variant="body2"
                          component="span"
                          display="block"
                          sx={{ mb: 0.5 }}
                        >
                          {notification.message}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {new Date(notification.timestamp).toLocaleString()}
                        </Typography>
                      </>
                    }
                  />
                </ListItem>
                {index < data.notifications.length - 1 && <Divider />}
              </Box>
            ))}
          </List>
        )}
      </Paper>
    </Box>
  );
};
