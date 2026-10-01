package com.events.goup.repository;

import com.events.goup.entity.Event;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"user", "location", "category"})
    @Query("select e from Event e where e.status <> :hidden order by e.eventDate asc, e.startTime asc")
    List<Event> findAllExceptStatus(@Param("hidden") EventStatus hidden);

    @EntityGraph(attributePaths = {"user", "location", "category"})
    @Query("select e from Event e where e.user = :user order by e.eventDate asc, e.startTime asc")
    List<Event> findAllByOwner(@Param("user") User user);

    @EntityGraph(attributePaths = {"user", "location", "category"})
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findWithDetailsById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "location", "category"})
    @Query("""
            select e from Event e
            where e.status = :status
              and e.eventDate >= :fromDate
              and (:city is null or e.location.city = :city)
            order by e.eventDate asc, e.startTime asc
            """)
    List<Event> findDiscoveryCandidates(@Param("status") EventStatus status,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("city") String city,
                                        Pageable pageable);
}
