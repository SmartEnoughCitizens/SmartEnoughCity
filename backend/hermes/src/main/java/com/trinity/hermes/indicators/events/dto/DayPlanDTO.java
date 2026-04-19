package com.trinity.hermes.indicators.events.dto;

import java.util.List;

public record DayPlanDTO(String date, List<DayPlanModeDTO> modes) {}
