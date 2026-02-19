/**
 * Notifications page — streamlined, compact list
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
} from "@mui/material";
import { useEffect } from "react";
import { useUserNotifications } from "@/hooks";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import { setNotificationBadgeCount } from "@/store/slices/uiSlice";
import { Priority, NotificationType } from "@/types";

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

  // Clear badge when user views notifications
  useEffect(() => {
    dispatch(setNotificationBadgeCount(0));
  }, [dispatch]);

  const { data, isLoading } = useUserNotifications(username || "", !!username);

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
      <Typography variant="h4" sx={{ mb: 2 }}>
        Notifications
      </Typography>

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
