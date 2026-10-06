package com.events.goup.repository;

import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByUserAndEvent(User user, Event event);

    Optional<Favorite> findByUserAndEvent(User user, Event event);

    @EntityGraph(attributePaths = {"event", "event.user", "event.location", "event.category"})
    List<Favorite> findAllByUserOrderByCreatedAtAsc(User user);

    @Query("select f.event.id as eventId, count(f) as total from Favorite f group by f.event.id")
    List<EventFavoriteCount> countGroupedByEvent();

    @Modifying
    @Query("delete from Favorite f where f.event = :event")
    void deleteAllByEvent(@Param("event") Event event);

    interface EventFavoriteCount {
        Long getEventId();

        Long getTotal();
    }
}
