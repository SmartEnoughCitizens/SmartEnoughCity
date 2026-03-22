package com.trinity.hermes.indicators.events.repository;

import com.trinity.hermes.indicators.events.entity.Events;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EventsRepository extends JpaRepository<Events, Integer> {

  @Query(
      """
          SELECT e FROM Events e
          WHERE e.eventDate >= CURRENT_DATE
          ORDER BY e.eventDate ASC, e.startTime ASC
          """)
  List<Events> findUpcomingEvents(Pageable pageable);
}
