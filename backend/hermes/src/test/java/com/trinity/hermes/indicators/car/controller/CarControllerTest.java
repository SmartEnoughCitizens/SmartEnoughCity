package com.trinity.hermes.indicators.car.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trinity.hermes.indicators.car.dto.CarDashboardDTO;
import com.trinity.hermes.indicators.car.dto.HighTrafficPointsDTO;
import com.trinity.hermes.indicators.car.dto.JunctionEmissionDTO;
import com.trinity.hermes.indicators.car.facade.CarFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CarController.class)
@AutoConfigureMockMvc(addFilters = false)
class CarControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CarFacade carFacade;

  @Test
  void getFuelTypeStatistics_returnsOkWithFuelData() throws Exception {
    CarDashboardDTO petrol = CarDashboardDTO.builder().fuelType("PETROL").count(5000L).build();
    CarDashboardDTO electric = CarDashboardDTO.builder().fuelType("ELECTRIC").count(1200L).build();
    when(carFacade.getFuelTypeStatistics()).thenReturn(List.of(petrol, electric));

    mockMvc
        .perform(get("/api/v1/car/fuel-type-statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fuelType").value("PETROL"))
        .andExpect(jsonPath("$[0].count").value(5000))
        .andExpect(jsonPath("$[1].fuelType").value("ELECTRIC"))
        .andExpect(jsonPath("$[1].count").value(1200));
  }

  @Test
  void getHighTrafficPoints_returnsOkWithTrafficList() throws Exception {
    HighTrafficPointsDTO point =
        HighTrafficPointsDTO.builder()
            .siteId(101)
            .lat(53.34)
            .lon(-6.26)
            .avgVolume(350.0)
            .dayType("weekday")
            .timeSlot("morning_peak")
            .build();
    when(carFacade.getHighTrafficPoints()).thenReturn(List.of(point));

    mockMvc
        .perform(get("/api/v1/car/high-traffic-points"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].siteId").value(101))
        .andExpect(jsonPath("$[0].lat").value(53.34))
        .andExpect(jsonPath("$[0].lon").value(-6.26))
        .andExpect(jsonPath("$[0].avgVolume").value(350.0))
        .andExpect(jsonPath("$[0].dayType").value("weekday"))
        .andExpect(jsonPath("$[0].timeSlot").value("morning_peak"));
  }

  @Test
  void getJunctionEmissions_returnsOkWithEmissionsList() throws Exception {
    JunctionEmissionDTO emission =
        JunctionEmissionDTO.builder()
            .siteId(101)
            .lat(53.34)
            .lon(-6.26)
            .dayType("weekday")
            .timeSlot("morning_peak")
            .carVolume(75.0)
            .lcvVolume(12.0)
            .busVolume(5.0)
            .hgvVolume(5.0)
            .motorcycleVolume(3.0)
            .totalEmissionG(8417.5)
            .build();
    when(carFacade.getJunctionEmissions()).thenReturn(List.of(emission));

    mockMvc
        .perform(get("/api/v1/car/junction-emissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].siteId").value(101))
        .andExpect(jsonPath("$[0].dayType").value("weekday"))
        .andExpect(jsonPath("$[0].timeSlot").value("morning_peak"))
        .andExpect(jsonPath("$[0].carVolume").value(75.0))
        .andExpect(jsonPath("$[0].lcvVolume").value(12.0))
        .andExpect(jsonPath("$[0].busVolume").value(5.0))
        .andExpect(jsonPath("$[0].hgvVolume").value(5.0))
        .andExpect(jsonPath("$[0].motorcycleVolume").value(3.0))
        .andExpect(jsonPath("$[0].totalEmissionG").value(8417.5));
  }
}
