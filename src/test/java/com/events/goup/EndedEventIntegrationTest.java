package com.events.goup;

import com.events.goup.client.GeminiClient.Suggestion;
import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(EndedEventIntegrationTest.FixedTime.class)
class EndedEventIntegrationTest extends MockedApisTestBase {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @TestConfiguration
    static class FixedTime {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-12T21:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    private User owner;
    private User premium;
    private Location location;

    @BeforeEach
    void setUp() {
        owner = createUser("owner-ended@goup.com", false);
        premium = createUser("premium-ended@goup.com", true);
        location = createLocation("ended-place", "Teatro", "Blumenau", -26.9, -49.0);
    }

    private Event event(String title, LocalDate date, LocalTime end, EventStatus status) {
        Event event = createEvent(owner, location, null, status, date, title);
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(end);
        return eventRepository.save(event);
    }

    @Test
    void publicListFiltersEndedEventsWithAndWithoutFilters() throws Exception {
        Event yesterday = event("Ontem", TODAY.minusDays(1), null, EventStatus.PUBLISHED);
        Event endedToday = event("Hoje encerrado", TODAY, LocalTime.of(14, 0), EventStatus.PUBLISHED);
        Event atBoundary = event("Exatamente agora", TODAY, LocalTime.of(18, 0), EventStatus.PUBLISHED);
        Event futureToday = event("Hoje futuro", TODAY, LocalTime.of(19, 0), EventStatus.PUBLISHED);
        Event noEndToday = event("Hoje sem fim", TODAY, null, EventStatus.PUBLISHED);
        Event noEndYesterday = event("Ontem sem fim", TODAY.minusDays(1), null, EventStatus.PUBLISHED);
        Event finished = event("Status finalizado", TODAY.plusDays(1), null, EventStatus.FINISHED);

        perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", hasItem(futureToday.getId().intValue())))
                .andExpect(jsonPath("$[*].id", hasItem(noEndToday.getId().intValue())));

        perform(get("/events").param("dateFrom", TODAY.minusDays(5).toString())
                .param("city", "Blumenau").param("freeOnly", "false").param("ageRating", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", not(hasItem(yesterday.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", not(hasItem(endedToday.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", not(hasItem(atBoundary.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", not(hasItem(noEndYesterday.getId().intValue()))))
                .andExpect(jsonPath("$[*].id", not(hasItem(finished.getId().intValue()))));
    }

    @Test
    void directOwnerAndFavoriteResponsesKeepEndedEvent() throws Exception {
        Event ended = event("Terminou", TODAY, LocalTime.of(14, 0), EventStatus.PUBLISHED);
        favoriteRepository.save(new Favorite(premium, ended));

        perform(get("/events/" + ended.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ended").value(true));
        perform(get("/users/me/events").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ended").value(true));
        perform(get("/users/me/favorites").header("Authorization", bearer(premium)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event.ended").value(true));

        Event finished = event("Finalizado", TODAY.plusDays(1), null, EventStatus.FINISHED);
        perform(get("/events/" + finished.getId()))
                .andExpect(jsonPath("$.ended").value(true));
    }

    @Test
    void discoveryAndRecommendationsExcludeEventThatEndedToday() throws Exception {
        Event ended = event("Acabou hoje", TODAY, LocalTime.of(14, 0), EventStatus.PUBLISHED);
        Event upcoming = event("Ainda disponível", TODAY, LocalTime.of(19, 0), EventStatus.PUBLISHED);
        when(geminiClient.suggest(anyString(), anyString()))
                .thenReturn(new Suggestion(List.of(ended.getId(), upcoming.getId()), "Sugestão"));

        perform(post("/discovery").header("Authorization", bearer(premium))
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"eventos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].id").value(upcoming.getId().intValue()));
        perform(get("/users/me/recommendations").header("Authorization", bearer(premium)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].event.id").value(upcoming.getId().intValue()));
    }
}
