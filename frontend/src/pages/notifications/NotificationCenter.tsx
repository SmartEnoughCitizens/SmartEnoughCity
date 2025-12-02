import React, { useState, useMemo } from 'react';
import {
  Grid,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  ButtonGroup,
} from '@mui/material';
import { Notifications, MarkEmailRead } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import NotificationList from '../../components/common/NotificationList';
import {
  useNotifications,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
} from '../../hooks/useNotifications';
// import { Notification, NotificationType } from '../../types';

type FilterType = 'all' | 'unread' | 'read';

const NotificationCenter: React.FC = () => {
  const [filter, setFilter] = useState<FilterType>('all');

  // TanStack Query hooks
  const { data: notifications = [] } = useNotifications();
  const markNotificationRead = useMarkNotificationRead();
  const markAllNotificationsRead = useMarkAllNotificationsRead();

  // Calculate unread count
  const unreadCount = useMemo(() => {
    return notifications.filter((n) => !n.read).length;
  }, [notifications]);

  // Calculate notifications by type
  const notificationsByType = useMemo(() => {
    return {
      info: notifications.filter((n) => n.type === 'INFO').length,
      warning: notifications.filter((n) => n.type === 'WARNING').length,
      error: notifications.filter((n) => n.type === 'ERROR').length,
      success: notifications.filter((n) => n.type === 'SUCCESS').length,
    };
  }, [notifications]);

  // Filter notifications based on selected filter
  const filteredNotifications = useMemo(() => {
    switch (filter) {
      case 'unread':
        return notifications.filter((n) => !n.read);
      case 'read':
        return notifications.filter((n) => n.read);
      default:
        return notifications;
    }
  }, [notifications, filter]);

  const handleMarkAsRead = (id: string | number) => {
    markNotificationRead.mutate(String(id));
  };

  const handleMarkAllAsRead = () => {
    markAllNotificationsRead.mutate();
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Notifications sx={{ fontSize: 40 }} />
            Notification Center
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Stay updated with system alerts and important messages
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<MarkEmailRead />}
          onClick={handleMarkAllAsRead}
          disabled={unreadCount === 0 || markAllNotificationsRead.isPending}
        >
          Mark All as Read
        </Button>
      </Box>

      {/* Metrics */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Total Notifications" value={notifications.length} color="primary" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Unread" value={unreadCount} color="warning" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Warnings" value={notificationsByType.warning} color="error" />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard title="Info" value={notificationsByType.info} color="info" />
        </Grid>
      </Grid>

      {/* Filter and Notifications List */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Notifications</Typography>
            <ButtonGroup size="small" variant="outlined">
              <Button variant={filter === 'all' ? 'contained' : 'outlined'} onClick={() => setFilter('all')}>
                All ({notifications.length})
              </Button>
              <Button variant={filter === 'unread' ? 'contained' : 'outlined'} onClick={() => setFilter('unread')}>
                Unread ({unreadCount})
              </Button>
              <Button
                variant={filter === 'read' ? 'contained' : 'outlined'}
                onClick={() => setFilter('read')}
              >
                Read ({notifications.length - unreadCount})
              </Button>
            </ButtonGroup>
          </Box>
          <NotificationList
            notifications={filteredNotifications}
            onMarkAsRead={handleMarkAsRead}
            maxHeight={600}
          />
        </CardContent>
      </Card>
    </Box>
  );
};

export default NotificationCenter;
