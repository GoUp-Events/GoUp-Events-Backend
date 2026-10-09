package com.events.goup;

import com.events.goup.entity.Category;
import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Recomendações personalizadas (baseadas em favoritos, exclusivo Premium)")
class RecommendationIntegrationTest extends MockedApisTestBase {

    private User me;
    private User organizer;
    private Location blumenau;
    private Location joinville;
    private Category categoryA;
    private Category categoryB;

    @BeforeEach
    void setUp() {
        me = createUser("premium@goup.com", true);
        organizer = createUser("organizador@goup.com", false);
        blumenau = createLocation("place-blu", "Vila Germânica", "Blumenau", -26.9194, -49.0661);
        joinville = createLocation("place-joi", "Expoville", "Joinville", -26.3045, -48.8487);
        categoryA = categoryAt(0);
        categoryB = categoryAt(1);
    }

    private Event published(User owner, Location location, Category category, int daysAhead, String title) {
        return createEvent(owner, location, category, EventStatus.PUBLISHED, daysFromNow(daysAhead), title);
    }

    @Test
    @DisplayName("sem favoritos: lista eventos futuros por data, com o motivo padrão")
    void recommend_withoutFavorites() throws Exception {
        Event first = published(organizer, blumenau, categoryA, 2, "Primeiro");
        Event second = published(organizer, joinville, categoryB, 5, "Segundo");

        perform(get("/users/me/recommendations").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].event.id").value(first.getId().intValue()))
                .andExpect(jsonPath("$[0].reason").value("Próximo evento na agenda"))
                .andExpect(jsonPath("$[1].event.id").value(second.getId().intValue()));
    }

    @Test
    @DisplayName("com favoritos: prioriza a mesma categoria (peso 3) e depois a mesma cidade (peso 1)")
    void recommend_scoresByCategoryAndCity() throws Exception {
        Event favorited = published(organizer, blumenau, categoryA, 2, "Favorito");
        favoriteRepository.save(new Favorite(me, favorited));

        Event sameCategoryAndCity = published(organizer, blumenau, categoryA, 3, "Mesma categoria e cidade");
        Event onlySameCity = published(organizer, blumenau, categoryB, 4, "Só mesma cidade");
        Event unrelated = published(organizer, joinville, categoryB, 5, "Nada a ver");

        perform(get("/users/me/recommendations").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].event.id").value(sameCategoryAndCity.getId().intValue()))
                .andExpect(jsonPath("$[0].reason").value("Porque você favoritou eventos de " + categoryA.getName()))
                .andExpect(jsonPath("$[1].event.id").value(onlySameCity.getId().intValue()))
                .andExpect(jsonPath("$[1].reason").value("Porque você costuma ir a eventos em Blumenau"))
                .andExpect(jsonPath("$[2].event.id").value(unrelated.getId().intValue()))
                .andExpect(jsonPath("$[2].reason").value("Próximo evento na agenda"));
    }

    @Test
    @DisplayName("não recomenda favoritos, eventos próprios, rascunhos, cancelados nem eventos passados")
    void recommend_excludesIrrelevantEvents() throws Exception {
        Event favorited = published(organizer, blumenau, categoryA, 2, "Já favoritado");
        favoriteRepository.save(new Favorite(me, favorited));

        published(me, blumenau, categoryA, 3, "Evento meu");
        createEvent(organizer, blumenau, categoryA, EventStatus.DRAFT, daysFromNow(3), "Rascunho");
        createEvent(organizer, blumenau, categoryA, EventStatus.CANCELLED, daysFromNow(3), "Cancelado");
        published(organizer, blumenau, categoryA, -2, "Evento passado");
        Event valid = published(organizer, blumenau, categoryA, 4, "Evento válido");

        perform(get("/users/me/recommendations").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].event.id").value(valid.getId().intValue()));
    }

    @Test
    @DisplayName("devolve no máximo 10 recomendações")
    void recommend_limitsToTen() throws Exception {
        for (int i = 1; i <= 12; i++) {
            published(organizer, blumenau, categoryA, i, "Evento " + i);
        }

        perform(get("/users/me/recommendations").header("Authorization", bearer(me)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    @DisplayName("não usa IA: o Gemini nunca é chamado")
    void recommend_doesNotUseGemini() throws Exception {
        published(organizer, blumenau, categoryA, 2, "Evento");

        perform(get("/users/me/recommendations").header("Authorization", bearer(me)))
                .andExpect(status().isOk());

        verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("Free recebe 403 e visitante recebe 401")
    void recommend_requiresPremium() throws Exception {
        User free = createUser("free@goup.com", false);

        perform(get("/users/me/recommendations").header("Authorization", bearer(free)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Recurso exclusivo do GoUp Premium"));
        perform(get("/users/me/recommendations")).andExpect(status().isUnauthorized());
    }
}
