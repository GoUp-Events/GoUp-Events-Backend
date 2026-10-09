package com.events.goup;

import com.events.goup.client.GooglePlacesClient.Place;
import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Locais próximos do evento (Google Places Nearby Search)")
class NearbyPlacesIntegrationTest extends MockedApisTestBase {

    private static final double LAT = -26.9194;
    private static final double LNG = -49.0661;
    // ~1000 m por grau de 0.009 de latitude
    private static final double STEP = 0.009;

    private User free;
    private User premium;
    private Event event;

    @BeforeEach
    void setUp() {
        free = createUser("free@goup.com", false);
        premium = createUser("premium@goup.com", true);
        Location location = createLocation("place-evento", "Vila Germânica", "Blumenau", LAT, LNG);
        event = createPublishedEvent(free, location, "Oktoberfest", 5);
    }

    /** O próprio local do evento primeiro (o Google costuma devolvê-lo como o mais próximo) e depois N lugares. */
    private List<Place> googleResults(int count) {
        List<Place> places = new ArrayList<>();
        places.add(place("place-evento", "Vila Germânica", LAT, LNG, null));
        for (int i = 1; i <= count; i++) {
            places.add(place("place-" + i, "Lugar " + i, LAT + STEP * i, LNG, "PRICE_LEVEL_MODERATE"));
        }
        return places;
    }

    private void stubGoogle(List<Place> places) {
        when(googlePlacesClient.searchNearby(eq("place-evento"), anyDouble(), anyDouble(), anyInt(), any()))
                .thenReturn(places);
    }

    @Test
    @DisplayName("visitante recebe 3 lugares, sem o próprio local do evento, com distância em metros")
    void nearby_visitorGetsThree() throws Exception {
        stubGoogle(googleResults(6));

        perform(get("/events/" + event.getId() + "/nearby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].placeId", not(hasItem("place-evento"))))
                .andExpect(jsonPath("$[0].name").value("Lugar 1"))
                .andExpect(jsonPath("$[0].type").value("Restaurante"))
                .andExpect(jsonPath("$[0].priceLevel").value("MODERATE"))
                .andExpect(jsonPath("$[0].rating").value(4.5))
                .andExpect(jsonPath("$[0].userRatingCount").value(120))
                .andExpect(jsonPath("$[0].googleMapsUri").value("https://maps.google.com/?cid=place-1"))
                .andExpect(jsonPath("$[0].distanceMeters", allOf(greaterThan(900), lessThan(1100))))
                .andExpect(jsonPath("$[1].distanceMeters", allOf(greaterThan(1900), lessThan(2100))));
    }

    @Test
    @DisplayName("usuário Free também recebe 3 lugares")
    void nearby_freeGetsThree() throws Exception {
        stubGoogle(googleResults(6));

        perform(get("/events/" + event.getId() + "/nearby").header("Authorization", bearer(free)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("usuário Premium recebe até 10 lugares")
    void nearby_premiumGetsTen() throws Exception {
        stubGoogle(googleResults(12));

        perform(get("/events/" + event.getId() + "/nearby").header("Authorization", bearer(premium)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    @DisplayName("pede ao Google o máximo dos planos + 1 (para descontar o próprio local) e todos os tipos")
    void nearby_callsGoogleWithMaxResultsAndAllTypes() throws Exception {
        stubGoogle(googleResults(3));

        perform(get("/events/" + event.getId() + "/nearby")).andExpect(status().isOk());

        verify(googlePlacesClient).searchNearby(eq("place-evento"), eq(LAT), eq(LNG), eq(11), isNull());
    }

    @Test
    @DisplayName("filtrar por tipo (?type=bar) é exclusivo Premium")
    void nearby_typeFilterIsPremiumOnly() throws Exception {
        stubGoogle(googleResults(3));

        perform(get("/events/" + event.getId() + "/nearby").param("type", "bar").header("Authorization", bearer(free)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Recurso exclusivo do GoUp Premium"));
        perform(get("/events/" + event.getId() + "/nearby").param("type", "bar"))
                .andExpect(status().isForbidden());

        verify(googlePlacesClient, never()).searchNearby(any(), anyDouble(), anyDouble(), anyInt(), any());

        perform(get("/events/" + event.getId() + "/nearby").param("type", " BAR ").header("Authorization", bearer(premium)))
                .andExpect(status().isOk());

        verify(googlePlacesClient).searchNearby(eq("place-evento"), eq(LAT), eq(LNG), eq(11), eq("bar"));
    }

    @Test
    @DisplayName("Premium com tipo inexistente recebe 400 listando os tipos válidos")
    void nearby_invalidTypeReturns400() throws Exception {
        perform(get("/events/" + event.getId() + "/nearby").param("type", "cassino").header("Authorization", bearer(premium)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Tipo de lugar inválido. Use um destes: restaurant, cafe, bar, bakery, tourist_attraction, park"));
    }

    @Test
    @DisplayName("Google sem resultados devolve lista vazia")
    void nearby_noResults() throws Exception {
        stubGoogle(List.of());

        perform(get("/events/" + event.getId() + "/nearby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Google fora do ar responde 502")
    void nearby_googleDown_returns502() throws Exception {
        when(googlePlacesClient.searchNearby(any(), anyDouble(), anyDouble(), anyInt(), any()))
                .thenThrow(new ExternalServiceException("Falha ao consultar locais próximos no Google Places"));

        perform(get("/events/" + event.getId() + "/nearby"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Falha ao consultar locais próximos no Google Places"));
    }

    @Test
    @DisplayName("evento inexistente responde 404 e rascunho de outra pessoa também (sem revelar que existe)")
    void nearby_visibility() throws Exception {
        perform(get("/events/999999/nearby")).andExpect(status().isNotFound());

        Location location = locationRepository.findByPlaceId("place-evento").orElseThrow();
        Event draft = createEvent(free, location, null, EventStatus.DRAFT, daysFromNow(5), "Rascunho");
        stubGoogle(googleResults(3));

        perform(get("/events/" + draft.getId() + "/nearby")).andExpect(status().isNotFound());
        perform(get("/events/" + draft.getId() + "/nearby").header("Authorization", bearer(premium)))
                .andExpect(status().isNotFound());
        perform(get("/events/" + draft.getId() + "/nearby").header("Authorization", bearer(free)))
                .andExpect(status().isOk());
    }
}
