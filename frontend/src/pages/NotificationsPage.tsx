import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Stack,
  Tooltip,
  Typography,
  CircularProgress,
} from "@mui/material";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutline";
import RadioButtonUncheckedIcon from "@mui/icons-material/RadioButtonUnchecked";
import { useEffect, useState } from "react";
import { useUserNotifications, useSetReadState, useMarkAllAsRead } from "@/hooks";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import { setNotificationBadgeCount } from "@/store/slices/uiSlice";
import { type Notification } from "@/types";

export const NotificationsPage = () => {
  const { username } = useAppSelector((state) => state.auth);
  const dispatch = useAppDispatch();

  const [selected, setSelected] = useState<Notification | null>(null);

  // Clear badge when user views notifications
  useEffect(() => {
    dispatch(setNotificationBadgeCount(0));
  }, [dispatch]);

  const { data, isLoading } = useUserNotifications(username || "", !!username);
  const setReadState = useSetReadState(username || "");
  const markAllAsRead = useMarkAllAsRead(username || "");

  const hasUnread = data?.notifications?.some((n) => !n.read) ?? false;

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
          justifyContent: "space-between",
          mb: 2,
        }}
      >
        <Typography variant="h4" sx={{ fontWeight: 600 }}>
          Notifications
        </Typography>
        {hasUnread && (
          <Button size="small" onClick={markAllAsRead}>
            Mark all as read
          </Button>
        )}
      </Box>

      {!data?.notifications || data.notifications.length === 0 ? (
        <Paper
          elevation={1}
          sx={{ borderRadius: 3, p: 4, maxWidth: 720, textAlign: "center" }}
        >
          <Typography variant="body1" color="text.secondary">
            No notifications available
          </Typography>
        </Paper>
      ) : (
        <Stack spacing={1.5} sx={{ maxWidth: 720 }}>
          {data.notifications.map((notification) => {
            const subject =
              (notification.metadata?.subject as string | undefined) ||
              notification.message.split(": ")[0];

            return (
              <Paper
                key={notification.id}
                elevation={1}
                onClick={() => setSelected(notification)}
                sx={{
                  borderRadius: 2,
                  px: 2.5,
                  py: 2,
                  cursor: "pointer",
                  display: "flex",
                  alignItems: "center",
                  gap: 1,
                  opacity: notification.read ? 0.45 : 1,
                  bgcolor: notification.read
                    ? "background.paper"
                    : (t) =>
                        t.palette.mode === "dark"
                          ? "rgba(25, 118, 210, 0.12)"
                          : "#e8f1fd",
                  transition: "opacity 0.2s, background-color 0.2s",
                  "&:hover": {
                    opacity: notification.read ? 0.65 : 0.9,
                  },
                }}
              >
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography
                    variant="body2"
                    sx={{ fontWeight: 600, mb: 0.25 }}
                    noWrap
                  >
                    {subject}
                  </Typography>
                  <Typography variant="caption" color="text.disabled">
                    {new Date(notification.timestamp).toLocaleString()}
                  </Typography>
                </Box>

                <Tooltip
                  title={notification.read ? "Mark as unread" : "Mark as read"}
                  placement="left"
                  arrow
                >
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      setReadState(notification.id, !notification.read);
                    }}
                    color={notification.read ? "default" : "primary"}
                  >
                    {notification.read ? (
                      <RadioButtonUncheckedIcon fontSize="small" />
                    ) : (
                      <CheckCircleOutlineIcon fontSize="small" />
                    )}
                  </IconButton>
                </Tooltip>
              </Paper>
            );
          })}
        </Stack>
      )}

      {/* Notification detail dialog */}
      {selected && (() => {
        const subject =
          (selected.metadata?.subject as string | undefined) ||
          selected.message.split(": ")[0];
        const body =
          (selected.metadata?.body as string | undefined) ||
          selected.message.split(": ").slice(1).join(": ");

        return (
          <Dialog
            open
            onClose={() => setSelected(null)}
            maxWidth="sm"
            fullWidth
          >
            <DialogTitle>{subject}</DialogTitle>
            <DialogContent sx={{ pt: 1 }}>
              <Typography variant="body1" sx={{ mb: 2 }}>
                {body}
              </Typography>
              <Typography variant="caption" color="text.disabled">
                {new Date(selected.timestamp).toLocaleString()}
              </Typography>
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2 }}>
              {!selected.read && (
                <Button
                  onClick={() => {
                    setReadState(selected.id, true);
                    setSelected((prev) =>
                      prev ? { ...prev, read: true } : prev,
                    );
                  }}
                >
                  Mark as read
                </Button>
              )}
              <Button variant="contained" onClick={() => setSelected(null)}>
                Close
              </Button>
            </DialogActions>
          </Dialog>
        );
      })()}
    </Box>
  );
};
