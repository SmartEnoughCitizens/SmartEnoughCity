import React, { type ReactNode } from 'react';
import { Card, CardContent, Typography, Box } from '@mui/material';

interface ChartCardProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
  actions?: ReactNode;
}

const ChartCard: React.FC<ChartCardProps> = ({ title, subtitle, children, actions }) => {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h6" component="div">
              {title}
            </Typography>
            {subtitle && (
              <Typography variant="body2" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          {actions && <Box>{actions}</Box>}
        </Box>
        <Box sx={{ mt: 2, minHeight: 300 }}>
          {children}
        </Box>
      </CardContent>
    </Card>
  );
};

export default ChartCard;
