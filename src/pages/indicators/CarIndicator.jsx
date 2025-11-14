import React from 'react';
import { DirectionsCar } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { indicatorService } from '../../services/indicatorService';

const CarIndicator = () => {
  return (
    <TransportIndicator
      mode="Car"
      icon={DirectionsCar}
      service={indicatorService.getCarIndicators}
      color="primary"
    />
  );
};

export default CarIndicator;
