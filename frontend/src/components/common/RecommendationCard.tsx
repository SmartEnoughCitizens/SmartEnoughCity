import React, { type ReactNode } from 'react';
import { Card, CardContent, Typography, Box, Chip } from '@mui/material';
import { Lightbulb, Warning, CheckCircle } from '@mui/icons-material';

type RecommendationType = 'info' | 'warning' | 'success' | 'error';
type PriorityLevel = 'low' | 'medium' | 'high';

interface RecommendationCardProps {
  title: string;
  description: string;
  priority?: PriorityLevel;
  type?: RecommendationType;
  actions?: ReactNode;
}

const RecommendationCard: React.FC<RecommendationCardProps> = ({
  title,
  description,
  priority = 'medium',
  type = 'info',
  actions
}) => {
  const getIcon = () => {
    switch (type) {
      case 'warning':
        return <Warning sx={{ color: 'warning.main' }} />;
      case 'success':
        return <CheckCircle sx={{ color: 'success.main' }} />;
      case 'error':
        return <Warning sx={{ color: 'error.main' }} />;
      default:
        return <Lightbulb sx={{ color: 'info.main' }} />;
    }
  };

  const getPriorityColor = (): 'error' | 'warning' | 'success' | 'default' => {
    switch (priority) {
      case 'high':
        return 'error';
      case 'medium':
        return 'warning';
      case 'low':
        return 'success';
      default:
        return 'default';
    }
  };

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, mb: 2 }}>
          <Box sx={{ mt: 0.5 }}>{getIcon()}</Box>
          <Box sx={{ flex: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
              <Typography variant="h6" component="div">
                {title}
              </Typography>
              <Chip label={priority.toUpperCase()} color={getPriorityColor()} size="small" />
            </Box>
            <Typography variant="body2" color="text.secondary">
              {description}
            </Typography>
          </Box>
        </Box>
        {actions && <Box sx={{ mt: 2 }}>{actions}</Box>}
      </CardContent>
    </Card>
  );
};

export default RecommendationCard;
