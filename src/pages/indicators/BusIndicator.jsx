import React from 'react';
import { DirectionsBus } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { indicatorService } from '../../services/indicatorService';

const BusIndicator = () => {
  return (
    <TransportIndicator
      mode="Bus"
      icon={DirectionsBus}
      service={indicatorService.getBusIndicators}
      color="secondary"
    />
  );
};

export default BusIndicator;
