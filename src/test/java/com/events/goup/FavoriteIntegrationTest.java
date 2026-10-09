package com.events.goup;

import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Favoritos (o roteiro simplificado)")
class FavoriteIntegrationTest extends MockedApisTestBase {

    private User ana;
    private User bia;
    private Event first;
    private Event second;

    @BeforeEach
    void setUp() {
        ana = createUser("ana@goup.com", false);
        bia = createUser("bia@goup.com", false);
        Location location = createLocation("place-blu", "Vila Germânica", "Blumenau", -26.9, -49.0);
        first = createPublishedEvent(bia, location, "Primeiro", 3);
        second = createPublishedEvent(bia, location, "Segundo", 4);
    }

    @Test
    @DisplayName("favoritar é idempotente: repetir não dá erro nem duplica")
    void favorite_isIdempotent() throws Exception {
        perform(post("/events/" + first.getId() + "/favorite").header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());
        perform(post("/events/" + first.getId() + "/favorite").header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());

        assertThat(favoriteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("cada usuário vê só os próprios favoritos")
    void list_isPerUser() throws Exception {
        perform(post("/events/" + first.getId() + "/favorite").header("Authorization", bearer(ana)));
        perform(post("/events/" + second.getId() + "/favorite").header("Authorization", bearer(ana)));
        perform(post("/events/" + second.getId() + "/favorite").header("Authorization", bearer(bia)));

        perform(get("/users/me/favorites").header("Authorization", bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].event.title", containsInAnyOrder("Primeiro", "Segundo")));

        perform(get("/users/me/favorites").header("Authorization", bearer(bia)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].event.title").value("Segundo"));
    }

    @Test
    @DisplayName("desfavoritar remove; desfavoritar o que não era favorito também responde 204")
    void unfavorite() throws Exception {
        perform(post("/events/" + first.getId() + "/favorite").header("Authorization", bearer(ana)));

        perform(delete("/events/" + first.getId() + "/favorite").header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());
        assertThat(favoriteRepository.count()).isZero();

        perform(delete("/events/" + first.getId() + "/favorite").header("Authorization", bearer(ana)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("favoritar evento inexistente ou rascunho de outra pessoa responde 404")
    void favorite_notFoundOrHiddenDraft() throws Exception {
        Location location = locationRepository.findByPlaceId("place-blu").orElseThrow();
        Event draft = createEvent(bia, location, null, EventStatus.DRAFT, daysFromNow(5), "Rascunho");

        perform(post("/events/999999/favorite").header("Authorization", bearer(ana)))
                .andExpect(status().isNotFound());
        perform(post("/events/" + draft.getId() + "/favorite").header("Authorization", bearer(ana)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("todas as rotas de favoritos exigem login")
    void requiresLogin() throws Exception {
        perform(post("/events/" + first.getId() + "/favorite")).andExpect(status().isUnauthorized());
        perform(delete("/events/" + first.getId() + "/favorite")).andExpect(status().isUnauthorized());
        perform(get("/users/me/favorites")).andExpect(status().isUnauthorized());
    }
}
