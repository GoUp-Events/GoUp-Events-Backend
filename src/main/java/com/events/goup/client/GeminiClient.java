package com.events.goup.client;

import com.events.goup.exception.ExternalServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente da Gemini API (generateContent) com saída JSON estruturada.
 */
@Component
public class GeminiClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "eventIds", Map.of("type", "ARRAY", "items", Map.of("type", "INTEGER")),
                    "summary", Map.of("type", "STRING")),
            "required", List.of("eventIds", "summary"));

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public GeminiClient(JsonMapper jsonMapper,
                        @Value("${goup.gemini.api-key:}") String apiKey,
                        @Value("${goup.gemini.model:gemini-2.5-flash}") String model) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory).build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public Suggestion suggest(String systemInstruction, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException("Chave do Gemini não configurada (GEMINI_API_KEY)");
        }

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA));

        GenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GenerateContentResponse.class);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Falha ao consultar o Gemini", exception);
        }

        String text = extractText(response);
        try {
            Suggestion suggestion = jsonMapper.readValue(text, Suggestion.class);
            List<Long> ids = suggestion.eventIds() != null ? suggestion.eventIds() : List.of();
            return new Suggestion(ids, suggestion.summary());
        } catch (JacksonException exception) {
            throw new ExternalServiceException("Resposta do Gemini em formato inválido", exception);
        }
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new ExternalServiceException("O Gemini não retornou nenhuma resposta");
        }
        Content content = response.candidates().getFirst().content();
        if (content == null || content.parts() == null || content.parts().isEmpty()
                || content.parts().getFirst().text() == null) {
            throw new ExternalServiceException("O Gemini não retornou nenhuma resposta");
        }
        return content.parts().getFirst().text();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(List<Long> eventIds, String summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateContentResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {
    }
}
