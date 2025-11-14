import React from 'react';
import { DirectionsWalk } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { indicatorService } from '../../services/indicatorService';

const PedestrianIndicator = () => {
  return (
    <TransportIndicator
      mode="Pedestrian"
      icon={DirectionsWalk}
      service={indicatorService.getPedestrianIndicators}
      color="success"
    />
  );
};

export default PedestrianIndicator;
