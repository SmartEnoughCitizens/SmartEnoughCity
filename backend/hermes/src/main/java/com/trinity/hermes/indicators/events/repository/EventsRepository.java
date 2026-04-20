package com.trinity.hermes.indicators.events.repository;

import com.trinity.hermes.indicators.events.entity.Events;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Query(
      """
          SELECT e FROM Events e
          JOIN FETCH e.venue v
          WHERE e.eventDate >= CURRENT_DATE
            AND v.capacity >= :minCapacity
          ORDER BY e.eventDate ASC, e.startTime ASC
          """)
  List<Events> findUpcomingEventsAtLargeVenues(
      @Param("minCapacity") int minCapacity, Pageable pageable);

  @Query(
      """
          SELECT e FROM Events e
          LEFT JOIN FETCH e.venue v
          WHERE e.eventDate >= CURRENT_DATE
            AND e.eventDate < :endDate
          ORDER BY e.eventDate ASC, e.startTime ASC
          """)
  List<Events> findUpcomingEventsDays(
      @Param("endDate") java.time.LocalDate endDate, Pageable pageable);

  @Query("SELECT e FROM Events e LEFT JOIN FETCH e.venue WHERE e.id = :id")
  java.util.Optional<Events> findByIdWithVenue(@Param("id") Integer id);
}
