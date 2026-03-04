package com.trinity.hermes.indicators.bus.repository;

import com.trinity.hermes.indicators.bus.entity.BusCalendarSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusCalendarScheduleRepository extends JpaRepository<BusCalendarSchedule, Integer> {

  @Query("SELECT c FROM BusCalendarSchedule c WHERE c.serviceId = :serviceId")
  List<BusCalendarSchedule> findByServiceId(@Param("serviceId") Integer serviceId);
}
