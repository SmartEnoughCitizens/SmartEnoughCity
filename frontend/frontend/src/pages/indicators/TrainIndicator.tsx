import React from 'react';
import { Train } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { useTrainIndicators } from '@/hooks';

const TrainIndicator: React.FC = () => {
  return (
    <TransportIndicator
      mode="Train"
      icon={Train}
      useIndicatorHook={useTrainIndicators}
      color="warning"
    />
  );
};

export default TrainIndicator;
