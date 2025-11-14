import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Grid,
  Typography,
  Box,
  Button,
  Card,
  CardContent,
  ButtonGroup,
} from '@mui/material';
import { Notifications, MarkEmailRead, Delete } from '@mui/icons-material';
import MetricCard from '../../components/common/MetricCard';
import NotificationList from '../../components/common/NotificationList';
import { notificationService } from '../../services/notificationService';
import {
  setNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  selectNotifications,
  selectUnreadCount,
} from '../../redux/slices/uiSlice';

const NotificationCenter = () => {
  const dispatch = useDispatch();
  const notifications = useSelector(selectNotifications);
  const unreadCount = useSelector(selectUnreadCount);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('all'); // 'all', 'unread', 'read'

  useEffect(() => {
    fetchNotifications();
  }, []);

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const data = await notificationService.getNotifications();
      dispatch(setNotifications(data));
    } catch (error) {
      console.error('Error fetching notifications:', error);
      // Use mock data
      dispatch(setNotifications(mockNotifications));
    } finally {
      setLoading(false);
    }
  };

  const mockNotifications = [
    {
      id: 1,
      type: 'WARNING',
      title: 'High Traffic Alert',
      message: 'High traffic alert on Main Street',
      description: 'Traffic congestion detected on Main Street. Consider alternative routes.',
      read: false,
      createdAt: new Date('2024-01-14T10:30:00'),
    },
    {
      id: 2,
      type: 'INFO',
      title: 'Simulation Complete',
      message: 'Peak hour simulation completed',
      description: 'Your peak hour traffic simulation has completed successfully. View results now.',
      read: false,
      createdAt: new Date('2024-01-14T09:15:00'),
    },
    {
      id: 3,
      type: 'SUCCESS',
      title: 'System Update',
      message: 'System updated successfully',
      description: 'The sustainable city management system has been updated to version 2.1.0.',
      read: true,
      createdAt: new Date('2024-01-13T14:20:00'),
    },
    {
      id: 4,
      type: 'ERROR',
      title: 'Sensor Malfunction',
      message: 'Traffic sensor offline',
      description: 'Sensor #A42 at North Bridge is not responding. Maintenance required.',
      read: true,
      createdAt: new Date('2024-01-13T08:45:00'),
    },
    {
      id: 5,
      type: 'INFO',
      title: 'New Event Scheduled',
      message: 'Marathon event added',
      description: 'A new marathon event has been scheduled for January 20th. Expected high traffic impact.',
      read: true,
      createdAt: new Date('2024-01-12T16:00:00'),
    },
  ];

  const handleMarkAsRead = async (id) => {
    try {
      await notificationService.markAsRead(id);
      dispatch(markNotificationRead(id));
    } catch (error) {
      console.error('Error marking notification as read:', error);
      dispatch(markNotificationRead(id));
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      dispatch(markAllNotificationsRead());
    } catch (error) {
      console.error('Error marking all as read:', error);
      dispatch(markAllNotificationsRead());
    }
  };

  const getFilteredNotifications = () => {
    switch (filter) {
      case 'unread':
        return notifications.filter((n) => !n.read);
      case 'read':
        return notifications.filter((n) => n.read);
      default:
        return notifications;
    }
  };

  const filteredNotifications = getFilteredNotifications();

  const notificationsByType = {
    info: notifications.filter((n) => n.type === 'INFO').length,
    warning: notifications.filter((n) => n.type === 'WARNING').length,
    error: notifications.filter((n) => n.type === 'ERROR').length,
    success: notifications.filter((n) => n.type === 'SUCCESS').length,
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
          disabled={unreadCount === 0}
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
