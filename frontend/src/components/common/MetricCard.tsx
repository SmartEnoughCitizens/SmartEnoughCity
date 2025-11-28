import React from 'react';
import { Card, CardContent, Typography, Box } from '@mui/material';
import { TrendingUp, TrendingDown } from '@mui/icons-material';
import { SvgIconComponent } from '@mui/icons-material';

interface MetricCardProps {
  title: string;
  value: string | number;
  unit?: string;
  trend?: number;
  icon?: SvgIconComponent;
  color?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
}

const MetricCard: React.FC<MetricCardProps> = ({
  title,
  value,
  unit,
  trend,
  icon: Icon,
  color = 'primary'
}) => {
  const getTrendIcon = () => {
    if (trend && trend > 0) return <TrendingUp sx={{ color: 'success.main', fontSize: 20 }} />;
    if (trend && trend < 0) return <TrendingDown sx={{ color: 'error.main', fontSize: 20 }} />;
    return null;
  };

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Typography color="text.secondary" variant="body2">
            {title}
          </Typography>
          {Icon && <Icon sx={{ color: `${color}.main`, fontSize: 32 }} />}
        </Box>
        <Typography variant="h4" component="div" sx={{ mb: 1 }}>
          {value}
          {unit && (
            <Typography component="span" variant="body1" color="text.secondary" sx={{ ml: 1 }}>
              {unit}
            </Typography>
          )}
        </Typography>
        {trend !== undefined && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            {getTrendIcon()}
            <Typography variant="body2" color={trend > 0 ? 'success.main' : trend < 0 ? 'error.main' : 'text.secondary'}>
              {Math.abs(trend)}% {trend > 0 ? 'increase' : trend < 0 ? 'decrease' : 'no change'}
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default MetricCard;
