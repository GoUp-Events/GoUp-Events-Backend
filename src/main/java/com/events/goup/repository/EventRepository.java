package com.events.goup.repository;

import com.events.goup.entity.Event;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Pesquisa pública. Todo filtro nulo é ignorado.
     */
    @EntityGraph(attributePaths = {"user", "location", "category"})
    @Query("""
            select e from Event e
            where e.status <> :hidden
              and (:text is null or lower(e.title) like :text or lower(e.description) like :text)
              and (:city is null or lower(e.location.city) = :city)
              and (:categoryId is null or e.category.id = :categoryId)
              and (:dateFrom is null or e.eventDate >= :dateFrom)
              and (:dateTo is null or e.eventDate <= :dateTo)
              and (:maxPrice is null or e.price <= :maxPrice)
            order by e.eventDate asc, e.startTime asc
            """)
    List<Event> search(@Param("hidden") EventStatus hidden,
                       @Param("text") String text,
                       @Param("city") String city,
                       @Param("categoryId") Long categoryId,
                       @Param("dateFrom") LocalDate dateFrom,
                       @Param("dateTo") LocalDate dateTo,
                       @Param("maxPrice") BigDecimal maxPrice);

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
