import React from 'react';
import { DirectionsCar } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { useCarIndicators } from '@/hooks';

const CarIndicator: React.FC = () => {
  return (
    <TransportIndicator
      mode="Car"
      icon={DirectionsCar}
      useIndicatorHook={useCarIndicators}
      color="primary"
    />
  );
};

export default CarIndicator;
