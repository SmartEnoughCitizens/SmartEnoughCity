import React from 'react';
import { DirectionsBike } from '@mui/icons-material';
import TransportIndicator from '../../components/indicators/TransportIndicator';
import { useCycleIndicators } from '@/hooks';

const CycleIndicator: React.FC = () => {
  return (
    <TransportIndicator
      mode="Cycle"
      icon={DirectionsBike}
      useIndicatorHook={useCycleIndicators}
      color="success"
    />
  );
};

export default CycleIndicator;
