package com.events.goup;

import com.events.goup.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CORS para o frontend")
class CorsIntegrationTest extends IntegrationTestBase {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String PUBLISHED_ORIGIN = "https://goup-events.lovable.app";

    @ParameterizedTest
    @ValueSource(strings = {"/events", "/categories", "/auth/login"})
    @DisplayName("preflight do frontend publicado retorna CORS nas rotas usadas pelo site")
    void publishedFrontendPreflightForRoutes(String path) throws Exception {
        perform(options(path)
                .header("Origin", PUBLISHED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PUBLISHED_ORIGIN));
    }

    @Test
    @DisplayName("site publicado pode enviar Authorization em preflight")
    void publishedFrontendPreflight() throws Exception {
        perform(options("/locations/autocomplete")
                .header("Origin", "https://goup-events.lovable.app")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://goup-events.lovable.app"));
    }

    @Test
    @DisplayName("preflight (OPTIONS) de uma origem permitida é aceito, mesmo sem token")
    void preflight_allowedOrigin() throws Exception {
        perform(options("/events")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("requisição normal de origem permitida recebe o cabeçalho de CORS")
    void request_allowedOrigin() throws Exception {
        perform(get("/categories").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("origem não permitida é recusada")
    void preflight_deniedOrigin() throws Exception {
        perform(options("/events")
                .header("Origin", "http://site-malicioso.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("erros de autenticação, permissão e rota preservam CORS")
    void errorResponsesIncludeCors() throws Exception {
        perform(post("/discovery")
                .header("Origin", PUBLISHED_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"oi\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", PUBLISHED_ORIGIN));

        User free = createUser("cors-free@goup.com", false);
        perform(post("/discovery")
                .header("Origin", PUBLISHED_ORIGIN)
                .header("Authorization", bearer(free))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"oi\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", PUBLISHED_ORIGIN));

        perform(get("/events/99999999").header("Origin", PUBLISHED_ORIGIN))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Access-Control-Allow-Origin", PUBLISHED_ORIGIN));

        perform(get("/error").header("Origin", PUBLISHED_ORIGIN))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string("Access-Control-Allow-Origin", PUBLISHED_ORIGIN));
    }
}
