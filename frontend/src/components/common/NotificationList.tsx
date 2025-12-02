import React from 'react';
import {
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Typography,
  Badge,
  IconButton,
  Box,
  Divider,
} from '@mui/material';
import {
  Info,
  Warning,
  Error,
  CheckCircle,
  MarkEmailRead,
} from '@mui/icons-material';
import { formatDateTime } from '../../utils/helpers';

type NotificationType = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';

interface Notification {
  id: string | number;
  title?: string;
  message?: string;
  description?: string;
  type: NotificationType;
  read: boolean;
  createdAt?: string;
  timestamp?: string;
}

interface NotificationListProps {
  notifications?: Notification[];
  onMarkAsRead?: (id: string | number) => void;
  maxHeight?: number;
}

const NotificationList: React.FC<NotificationListProps> = ({
  notifications = [],
  onMarkAsRead,
  maxHeight = 400
}) => {
  const getIcon = (type: NotificationType) => {
    switch (type) {
      case 'WARNING':
        return <Warning sx={{ color: 'warning.main' }} />;
      case 'ERROR':
        return <Error sx={{ color: 'error.main' }} />;
      case 'SUCCESS':
        return <CheckCircle sx={{ color: 'success.main' }} />;
      default:
        return <Info sx={{ color: 'info.main' }} />;
    }
  };

  if (notifications.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 4 }}>
        <Typography variant="body2" color="text.secondary">
          No notifications
        </Typography>
      </Box>
    );
  }

  return (
    <List sx={{ maxHeight, overflow: 'auto' }}>
      {notifications.map((notification, index) => (
        <React.Fragment key={notification.id}>
          <ListItem
            sx={{
              backgroundColor: notification.read ? 'transparent' : 'action.hover',
              '&:hover': {
                backgroundColor: 'action.selected',
              },
            }}
            secondaryAction={
              !notification.read && onMarkAsRead && (
                <IconButton
                  edge="end"
                  aria-label="mark as read"
                  onClick={() => onMarkAsRead(notification.id)}
                  size="small"
                >
                  <MarkEmailRead fontSize="small" />
                </IconButton>
              )
            }
          >
            <ListItemIcon>
              <Badge color="primary" variant="dot" invisible={notification.read}>
                {getIcon(notification.type)}
              </Badge>
            </ListItemIcon>
            <ListItemText
              primary={notification.title || notification.message}
              secondary={
                <>
                  {notification.description && (
                    <Typography component="span" variant="body2" color="text.secondary" display="block">
                      {notification.description}
                    </Typography>
                  )}
                  <Typography component="span" variant="caption" color="text.secondary">
                    {formatDateTime(notification.createdAt || notification.timestamp)}
                  </Typography>
                </>
              }
            />
          </ListItem>
          {index < notifications.length - 1 && <Divider />}
        </React.Fragment>
      ))}
    </List>
  );
};

export default NotificationList;
