import React from 'react';
import { Train } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { indicatorService } from '../../services/indicatorService';

const TrainIndicator = () => {
  return (
    <TransportIndicator
      mode="Train"
      icon={Train}
      service={indicatorService.getTrainIndicators}
      color="warning"
    />
  );
};

export default TrainIndicator;
