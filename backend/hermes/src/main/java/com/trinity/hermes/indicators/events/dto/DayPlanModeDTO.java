package com.trinity.hermes.indicators.events.dto;

import java.util.List;

public record DayPlanModeDTO(String mode, List<DayPlanStopDTO> stops) {}
