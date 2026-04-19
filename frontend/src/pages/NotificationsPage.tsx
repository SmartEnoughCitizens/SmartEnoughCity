import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  MenuItem,
  Select,
  Tab,
  Tabs,
  Tooltip,
  Typography,
  CircularProgress,
  Alert,
} from "@mui/material";
import type { SelectChangeEvent } from "@mui/material";
import NavigateBeforeIcon from "@mui/icons-material/NavigateBefore";
import NavigateNextIcon from "@mui/icons-material/NavigateNext";
import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import DoneAllIcon from "@mui/icons-material/DoneAll";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import RestoreFromTrashIcon from "@mui/icons-material/RestoreFromTrash";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import CircleIcon from "@mui/icons-material/Circle";
import DeleteForeverIcon from "@mui/icons-material/DeleteForever";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useEffect, useState } from "react";
import {
  useUserNotifications,
  useNotificationBin,
  useSetReadState,
  useMarkAllAsRead,
  useSoftDeleteNotification,
  useRestoreNotification,
} from "@/hooks";
import { useAppSelector, useAppDispatch } from "@/store/hooks";
import {
  setNotificationBadgeCount,
  requestNavigation,
} from "@/store/slices/uiSlice";
import { type Notification } from "@/types";

export const NotificationsPage = () => {
  const { username } = useAppSelector((state) => state.auth);
  const dispatch = useAppDispatch();
  const [tab, setTab] = useState(0);
  const [selected, setSelected] = useState<Notification | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);

  useEffect(() => {
    dispatch(setNotificationBadgeCount(0));
  }, [dispatch]);

  const { data, isLoading } = useUserNotifications(
    username || "",
    !!username,
    page,
    pageSize,
  );
  const { data: binData, isLoading: binLoading } = useNotificationBin(
    username || "",
    tab === 1,
  );
  const setReadState = useSetReadState(username || "");
  const markAllAsRead = useMarkAllAsRead(username || "");
  const softDelete = useSoftDeleteNotification(username || "");
  const restore = useRestoreNotification(username || "");

  const inbox = data?.notifications ?? [];
  const bin = binData?.notifications ?? [];
  const unreadCount = data?.totalCount ?? inbox.filter((n) => !n.read).length;

  const handleOpen = (n: Notification) => {
    setSelected(n);
    if (!n.read && tab === 0) setReadState(n.id, true);
  };

  const renderList = (items: Notification[], isBin: boolean) => {
    if (isLoading || (isBin && binLoading)) {
      return (
        <Box sx={{ display: "flex", justifyContent: "center", pt: 6 }}>
          <CircularProgress size={28} />
        </Box>
      );
    }
    if (items.length === 0) {
      return (
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            height: 200,
            gap: 1.5,
            color: "text.disabled",
          }}
        >
          {isBin ? (
            <DeleteForeverIcon sx={{ fontSize: 44 }} />
          ) : (
            <NotificationsNoneIcon sx={{ fontSize: 44 }} />
          )}
          <Typography variant="body2">
            {isBin ? "Bin is empty" : "No notifications yet"}
          </Typography>
        </Box>
      );
    }
    return items.map((n, i) => {
      const subject =
        (n.metadata?.subject as string | undefined) || n.message.split(": ")[0];
      const body =
        (n.metadata?.body as string | undefined) ||
        n.message.split(": ").slice(1).join(": ");
      const hasLink = !!(n.metadata?.actionUrl as string | undefined);

      return (
        <Box key={n.id}>
          <Box
            onClick={() => handleOpen(n)}
            sx={{
              px: 4,
              py: 2,
              display: "flex",
              alignItems: "flex-start",
              gap: 2,
              cursor: "pointer",
              bgcolor:
                !isBin && !n.read
                  ? (t) =>
                      t.palette.mode === "dark"
                        ? "rgba(25,118,210,0.07)"
                        : "#f0f6ff"
                  : "transparent",
              transition: "background-color 0.15s",
              "&:hover": {
                bgcolor: (t) =>
                  t.palette.mode === "dark"
                    ? "rgba(255,255,255,0.04)"
                    : "rgba(0,0,0,0.025)",
              },
            }}
          >
            {/* Unread dot */}
            <Box sx={{ pt: 0.6, width: 8, flexShrink: 0 }}>
              {!n.read && !isBin && (
                <CircleIcon sx={{ fontSize: 8, color: "primary.main" }} />
              )}
            </Box>

            {/* Content */}
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Box
                sx={{ display: "flex", alignItems: "center", gap: 1, mb: 0.25 }}
              >
                <Typography
                  variant="body2"
                  fontWeight={!isBin && !n.read ? 700 : 400}
                  noWrap
                  sx={{ flex: 1 }}
                >
                  {subject}
                </Typography>
                {hasLink && (
                  <OpenInNewIcon
                    sx={{ fontSize: 14, color: "text.disabled", flexShrink: 0 }}
                  />
                )}
              </Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{
                  display: "-webkit-box",
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: "vertical",
                  overflow: "hidden",
                }}
              >
                {body}
              </Typography>
              <Typography
                variant="caption"
                color="text.disabled"
                sx={{ mt: 0.4, display: "block" }}
              >
                {new Date(n.timestamp).toLocaleString("en-IE", {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </Typography>
            </Box>

            {/* Actions */}
            <Box
              sx={{ display: "flex", gap: 0.25, flexShrink: 0 }}
              onClick={(e) => e.stopPropagation()}
            >
              {!isBin && (
                <>
                  <Tooltip
                    title={n.read ? "Mark as unread" : "Mark as read"}
                    arrow
                    placement="top"
                  >
                    <IconButton
                      size="small"
                      onClick={() => setReadState(n.id, !n.read)}
                      sx={{ color: n.read ? "text.disabled" : "primary.main" }}
                    >
                      <DoneAllIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Move to bin" arrow placement="top">
                    <IconButton
                      size="small"
                      onClick={() => softDelete(n.id)}
                      sx={{
                        color: "text.disabled",
                        "&:hover": { color: "error.main" },
                      }}
                    >
                      <DeleteOutlineIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </>
              )}
              {isBin && (
                <Tooltip title="Restore" arrow placement="top">
                  <IconButton
                    size="small"
                    onClick={() => restore(n.id)}
                    sx={{
                      color: "text.disabled",
                      "&:hover": { color: "primary.main" },
                    }}
                  >
                    <RestoreFromTrashIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              )}
            </Box>
          </Box>
          {i < items.length - 1 && <Divider />}
        </Box>
      );
    });
  };

  return (
    <Box
      sx={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        bgcolor: (t) => t.palette.background.default,
      }}
    >
      {/* Header */}
      <Box
        sx={{
          px: 4,
          py: 2.5,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          borderBottom: 1,
          borderColor: "divider",
          bgcolor: "background.paper",
          flexShrink: 0,
        }}
      >
        <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
          <NotificationsNoneIcon sx={{ color: "primary.main" }} />
          <Typography variant="h6" fontWeight={700}>
            Notifications
          </Typography>
        </Box>
        {tab === 0 && unreadCount > 0 && (
          <Button
            size="small"
            startIcon={<DoneAllIcon />}
            onClick={markAllAsRead}
            sx={{ fontSize: "0.75rem" }}
          >
            Mark all as read
          </Button>
        )}
      </Box>

      {/* Tabs */}
      <Box
        sx={{
          borderBottom: 1,
          borderColor: "divider",
          bgcolor: "background.paper",
          flexShrink: 0,
        }}
      >
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          sx={{
            px: 3,
            "& .MuiTab-root": {
              fontSize: "0.8rem",
              textTransform: "none",
              minHeight: 40,
            },
          }}
        >
          <Tab label="Inbox" />
          <Tab label="Bin" />
        </Tabs>
      </Box>

      {/* Bin 30-day banner */}
      {tab === 1 && bin.length > 0 && (
        <Alert
          severity="warning"
          sx={{ borderRadius: 0, fontSize: "0.78rem", flexShrink: 0 }}
        >
          Items in the bin are permanently deleted after{" "}
          <strong>30 days</strong>.
        </Alert>
      )}

      {/* List */}
      <Box sx={{ flex: 1, overflow: "auto" }}>
        {tab === 0 ? renderList(inbox, false) : renderList(bin, true)}
      </Box>

      {/* Pagination (inbox only) */}
      {tab === 0 && (
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            px: 4,
            py: 1.5,
            borderTop: 1,
            borderColor: "divider",
            bgcolor: "background.paper",
            flexShrink: 0,
          }}
        >
          <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
            <Typography variant="caption" color="text.secondary">
              Rows per page:
            </Typography>
            <Select
              size="small"
              value={pageSize}
              onChange={(e: SelectChangeEvent<number>) => {
                setPageSize(Number(e.target.value));
                setPage(0);
              }}
              sx={{ fontSize: "0.75rem", height: 28 }}
            >
              {[10, 25, 100].map((s) => (
                <MenuItem key={s} value={s} sx={{ fontSize: "0.8rem" }}>
                  {s}
                </MenuItem>
              ))}
            </Select>
          </Box>

          <Box sx={{ display: "flex", alignItems: "center", gap: 0.5 }}>
            {data?.totalItems !== undefined && (
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ mr: 1 }}
              >
                {page * pageSize + 1}–
                {Math.min((page + 1) * pageSize, data.totalItems)} of{" "}
                {data.totalItems}
              </Typography>
            )}
            <IconButton
              size="small"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              <NavigateBeforeIcon fontSize="small" />
            </IconButton>
            <IconButton
              size="small"
              disabled={
                data?.totalItems === undefined ||
                (page + 1) * pageSize >= data.totalItems
              }
              onClick={() => setPage((p) => p + 1)}
            >
              <NavigateNextIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>
      )}

      {/* Detail dialog */}
      {selected &&
        (() => {
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
              <DialogTitle sx={{ fontWeight: 700 }}>{subject}</DialogTitle>
              <DialogContent sx={{ pt: 1 }}>
                <Box
                  sx={{
                    mb: 2,
                    fontSize: "0.9rem",
                    lineHeight: 1.6,
                    "& h2": {
                      fontSize: "0.95rem",
                      fontWeight: 700,
                      mt: 2,
                      mb: 0.5,
                    },
                    "& p": { mt: 0, mb: 0.75 },
                    "& ul": { pl: 2.5, mt: 0, mb: 0.75 },
                    "& li": { mb: 0.25 },
                    "& hr": { my: 1.5, borderColor: "divider" },
                    "& em": {
                      color: "text.secondary",
                      fontStyle: "normal",
                      fontSize: "0.8rem",
                    },
                    "& strong": { fontWeight: 700 },
                    "& table": {
                      borderCollapse: "collapse",
                      width: "100%",
                      fontSize: "0.8rem",
                      mb: 1,
                    },
                    "& th, & td": {
                      border: "1px solid",
                      borderColor: "divider",
                      px: 1,
                      py: 0.5,
                      textAlign: "left",
                    },
                    "& th": { bgcolor: "action.hover", fontWeight: 600 },
                  }}
                >
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {body}
                  </ReactMarkdown>
                </Box>
                <Typography variant="caption" color="text.disabled">
                  {new Date(selected.timestamp).toLocaleString("en-IE", {
                    day: "numeric",
                    month: "short",
                    year: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </Typography>
              </DialogContent>
              <DialogActions sx={{ px: 3, pb: 2, gap: 1 }}>
                {(selected.metadata?.actionUrl as string | undefined) && (
                  <Button
                    variant="contained"
                    startIcon={<OpenInNewIcon />}
                    onClick={() => {
                      const actionUrl = selected.metadata!.actionUrl as string;
                      const params = new URLSearchParams(
                        actionUrl.split("?")[1] ?? "",
                      );
                      dispatch(
                        requestNavigation({
                          view: params.get("view") ?? "",
                          tab: params.get("tab") ?? undefined,
                        }),
                      );
                      setSelected(null);
                    }}
                  >
                    View in Dashboard
                  </Button>
                )}
                <Button variant="outlined" onClick={() => setSelected(null)}>
                  Close
                </Button>
              </DialogActions>
            </Dialog>
          );
        })()}
    </Box>
  );
};
