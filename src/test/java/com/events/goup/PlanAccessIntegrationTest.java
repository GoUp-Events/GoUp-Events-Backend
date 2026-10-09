package com.events.goup;

import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Planos Free x Premium")
class PlanAccessIntegrationTest extends MockedApisTestBase {

    private User free;
    private User premium;

    @BeforeEach
    void setUp() {
        free = createUser("free@goup.com", false);
        premium = createUser("premium@goup.com", true);
    }

    @Test
    @DisplayName("GET /plans é público e mostra Free (3 lugares) e Premium (10 lugares)")
    void plans_arePublic() throws Exception {
        perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Free"))
                .andExpect(jsonPath("$[0].premium").value(false))
                .andExpect(jsonPath("$[0].nearbyLimit").value(3))
                .andExpect(jsonPath("$[1].name").value("Premium"))
                .andExpect(jsonPath("$[1].premium").value(true))
                .andExpect(jsonPath("$[1].nearbyLimit").value(10))
                .andExpect(jsonPath("$[1].benefits", hasItem("Chatbot com IA para pedidos em linguagem natural")));
    }

    @Test
    @DisplayName("usuário Free recebe 403 nos recursos Premium, sem gastar o Gemini")
    void free_isBlockedFromPremiumFeatures() throws Exception {
        perform(get("/users/me/recommendations").header("Authorization", bearer(free)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(PlanService.PREMIUM_REQUIRED_MESSAGE));

        perform(post("/discovery")
                .header("Authorization", bearer(free))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"message":"algo para fazer no sábado"}
                        """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(PlanService.PREMIUM_REQUIRED_MESSAGE));

        perform(get("/events").param("sort", "popular").header("Authorization", bearer(free)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("sem login: recursos Premium protegidos dão 401, e a ordenação popular dá 403")
    void visitor_isBlockedFromPremiumFeatures() throws Exception {
        perform(get("/users/me/recommendations")).andExpect(status().isUnauthorized());
        perform(post("/discovery").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"oi\"}"))
                .andExpect(status().isUnauthorized());
        perform(get("/events").param("sort", "popular")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("usuário Premium acessa recomendações e ordenação popular")
    void premium_canAccessPremiumFeatures() throws Exception {
        perform(get("/users/me/recommendations").header("Authorization", bearer(premium)))
                .andExpect(status().isOk());
        perform(get("/events").param("sort", "popular").header("Authorization", bearer(premium)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("virar Premium no banco vale na próxima requisição, com o mesmo token")
    void premiumChangeInDatabase_appliesWithSameToken() throws Exception {
        String token = bearer(free);

        perform(get("/users/me/recommendations").header("Authorization", token))
                .andExpect(status().isForbidden());

        User user = userRepository.findById(free.getId()).orElseThrow();
        user.setPremium(true);
        userRepository.save(user);

        perform(get("/users/me/recommendations").header("Authorization", token))
                .andExpect(status().isOk());
        perform(get("/users/me").header("Authorization", token))
                .andExpect(jsonPath("$.premium").value(true));
    }

    @Test
    @DisplayName("sort inválido responde 400; sem sort ordena por data")
    void sort_validation() throws Exception {
        perform(get("/events").param("sort", "aleatorio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ordenação inválida: use \"date\" ou \"popular\""));

        Location location = createLocation("place-a", "Local A", "Blumenau", -26.9, -49.0);
        Event later = createPublishedEvent(free, location, "Mais tarde", 10);
        Event sooner = createPublishedEvent(free, location, "Mais cedo", 2);

        perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sooner.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(later.getId().intValue()));
    }

    @Test
    @DisplayName("sort=popular coloca os mais favoritados primeiro")
    void sortPopular_ordersByFavorites() throws Exception {
        User fan1 = createUser("fan1@goup.com", false);
        User fan2 = createUser("fan2@goup.com", false);
        Location location = createLocation("place-a", "Local A", "Blumenau", -26.9, -49.0);

        Event a = createPublishedEvent(free, location, "A - 1 favorito", 1);
        Event b = createPublishedEvent(free, location, "B - 2 favoritos", 2);
        Event c = createPublishedEvent(free, location, "C - nenhum favorito", 3);

        favoriteRepository.save(new Favorite(fan1, a));
        favoriteRepository.save(new Favorite(fan1, b));
        favoriteRepository.save(new Favorite(fan2, b));

        perform(get("/events").param("sort", "popular").header("Authorization", bearer(premium)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(b.getId().intValue()))
                .andExpect(jsonPath("$[1].id").value(a.getId().intValue()))
                .andExpect(jsonPath("$[2].id").value(c.getId().intValue()));
    }
}
