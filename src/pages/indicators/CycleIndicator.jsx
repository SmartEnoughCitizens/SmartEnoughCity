import React from 'react';
import { DirectionsBike } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { indicatorService } from '../../services/indicatorService';

const CycleIndicator = () => {
  return (
    <TransportIndicator
      mode="Cycle"
      icon={DirectionsBike}
      service={indicatorService.getCycleIndicators}
      color="success"
    />
  );
};

export default CycleIndicator;
