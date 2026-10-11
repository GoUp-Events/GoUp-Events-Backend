package com.events.goup.client;

import com.events.goup.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GeminiClientTest {

    private static final String MODEL_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent";

    private GeminiClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new GeminiClient(JsonMapper.builder().build(), "test-key", "gemini-3.6-flash");
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(client, "restClient", builder.build());
    }

    @Test
    void modelNotFoundReturnsClearError() {
        server.expect(requestTo(MODEL_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"Model not found\"}}"));

        assertThatThrownBy(() -> client.suggest("instrução", "pedido"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("HTTP 404")
                .hasMessageContaining("modelo inexistente");
        server.verify();
    }

    @Test
    void successfulResponsePreservesSuggestionContract() {
        server.expect(requestTo(MODEL_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"eventIds\\\":[1],\\\"summary\\\":\\\"Encontrei um evento.\\\"}\"}]}}]}"));

        GeminiClient.Suggestion suggestion = client.suggest("instrução", "pedido");

        assertThat(suggestion.eventIds()).containsExactly(1L);
        assertThat(suggestion.summary()).isEqualTo("Encontrei um evento.");
        server.verify();
    }
}
