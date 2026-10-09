package com.events.goup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("CORS para o frontend")
class CorsIntegrationTest extends IntegrationTestBase {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

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
}
