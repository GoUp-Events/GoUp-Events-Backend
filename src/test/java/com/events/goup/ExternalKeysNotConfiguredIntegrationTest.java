package com.events.goup;

import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aqui os clientes do Gemini e do Google Places são os REAIS, mas com a chave em branco de propósito
 * (por isso nenhuma chamada de rede acontece). Garante que a API responde 502 com mensagem clara,
 * em vez de estourar erro 500, quando alguém esquece de configurar GEMINI_API_KEY ou GOOGLE_PLACES_API_KEY.
 */
@SpringBootTest(properties = {
        "goup.gemini.api-key=",
        "goup.google.places.api-key="
})
@DisplayName("Chaves do Gemini e do Google Places não configuradas")
class ExternalKeysNotConfiguredIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Descoberta Mágica sem GEMINI_API_KEY responde 502")
    void discovery_withoutGeminiKey() throws Exception {
        User premium = createUser("premium@goup.com", true);
        Location location = createLocation("place-blu", "Vila Germânica", "Blumenau", -26.9, -49.0);
        createPublishedEvent(premium, location, "Show", 3);

        perform(post("/discovery")
                .header("Authorization", bearer(premium))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"um show\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Chave do Gemini não configurada (GEMINI_API_KEY)"));
    }

    @Test
    @DisplayName("POST /locations sem GOOGLE_PLACES_API_KEY responde 502")
    void resolveLocation_withoutPlacesKey() throws Exception {
        User user = createUser("ana@goup.com", false);

        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"placeId\":\"qualquer-place-id\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Chave do Google Places não configurada (GOOGLE_PLACES_API_KEY)"));
    }

    @Test
    @DisplayName("locais próximos sem GOOGLE_PLACES_API_KEY responde 502")
    void nearby_withoutPlacesKey() throws Exception {
        User user = createUser("ana@goup.com", false);
        Location location = createLocation("place-blu", "Vila Germânica", "Blumenau", -26.9, -49.0);
        Event event = createPublishedEvent(user, location, "Show", 3);

        perform(get("/events/" + event.getId() + "/nearby"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Chave do Google Places não configurada (GOOGLE_PLACES_API_KEY)"));
    }
}
