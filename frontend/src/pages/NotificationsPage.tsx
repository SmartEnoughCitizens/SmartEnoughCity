/**
 * Notifications page
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
  Alert,
  Divider,
} from "@mui/material";
import { useUserNotifications } from "@/hooks";
import { useAppSelector } from "@/store/hooks";
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

  const { data, isLoading, error } = useUserNotifications(
    username || "",
    !!username,
  );

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box>
        <Typography variant="h4" gutterBottom>
          Notifications
        </Typography>
        <Alert severity="error">Failed to load notifications</Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Notifications
      </Typography>

      <Paper sx={{ mt: 2 }}>
        {!data?.notifications || data.notifications.length === 0 ? (
          <Box sx={{ p: 4, textAlign: "center" }}>
            <Typography variant="body1" color="text.secondary">
              No notifications available
            </Typography>
          </Box>
        ) : (
          <List>
            {data.notifications.map((notification, index) => (
              <Box key={notification.id}>
                <ListItem
                  sx={{
                    bgcolor: notification.read ? "transparent" : "action.hover",
                  }}
                >
                  <ListItemText
                    primary={
                      <Box
                        sx={{
                          display: "flex",
                          gap: 1,
                          mb: 1,
                          alignItems: "center",
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
                          variant="body1"
                          component="span"
                          display="block"
                          sx={{ mb: 1 }}
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
