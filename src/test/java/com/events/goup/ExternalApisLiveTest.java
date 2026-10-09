package com.events.goup;

import com.events.goup.client.GeminiClient;
import com.events.goup.client.GeminiClient.Suggestion;
import com.events.goup.client.GooglePlacesClient;
import com.events.goup.client.GooglePlacesClient.Place;
import com.events.goup.client.GooglePlacesClient.PlaceDetails;
import com.events.goup.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TESTES COM CHAMADA REAL (gastam um pouco da cota). Ficam PULADOS se as variáveis de ambiente não existirem,
 * então nunca quebram o build de quem não tem as chaves. Para rodar:
 *
 *   export GEMINI_API_KEY=...
 *   export GOOGLE_PLACES_API_KEY=...
 *   export GOUP_TEST_PLACE_ID=...   (um placeId real, ex.: o de um local de Blumenau pelo autocomplete)
 *   ./mvnw test -Dtest=ExternalApisLiveTest
 *
 * Estes são os únicos testes que provam que o formato da requisição/resposta do Google e do Gemini está certo.
 */
@SpringBootTest(properties = {
        "goup.gemini.api-key=${GEMINI_API_KEY:}",
        "goup.google.places.api-key=${GOOGLE_PLACES_API_KEY:}"
})
@ActiveProfiles("test")
@DisplayName("APIs externas de verdade (Gemini e Google Places)")
class ExternalApisLiveTest {

    @Autowired private GeminiClient geminiClient;
    @Autowired private GooglePlacesClient googlePlacesClient;

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    @DisplayName("Gemini responde o JSON no formato { eventIds, summary }")
    void gemini_returnsStructuredJson() {
        Suggestion suggestion = geminiClient.suggest(
                "Você escolhe eventos de uma lista. Responda em JSON com eventIds (ids da lista) e summary (uma frase curta em português).",
                "Pedido do usuário: \"quero um show\"\n\nEventos candidatos:\n- id=1 | título: Show de rock\n- id=2 | título: Aula de yoga\n");

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.summary()).isNotBlank();
        assertThat(suggestion.eventIds()).isNotNull();
        assertThat(suggestion.eventIds()).isSubsetOf(1L, 2L);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_PLACES_API_KEY", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "GOUP_TEST_PLACE_ID", matches = ".+")
    @DisplayName("Places: Place Details devolve nome, endereço e coordenadas")
    void places_details() {
        String placeId = System.getenv("GOUP_TEST_PLACE_ID");

        PlaceDetails details = googlePlacesClient.getPlaceDetails(placeId);

        assertThat(details.displayName()).isNotNull();
        assertThat(details.displayName().text()).isNotBlank();
        assertThat(details.formattedAddress()).isNotBlank();
        assertThat(details.location()).isNotNull();
        assertThat(details.location().latitude()).isNotNull();
        assertThat(details.location().longitude()).isNotNull();
        assertThat(details.addressComponents()).isNotEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_PLACES_API_KEY", matches = ".+")
    @EnabledIfEnvironmentVariable(named = "GOUP_TEST_PLACE_ID", matches = ".+")
    @DisplayName("Places: Nearby Search devolve lugares perto do local")
    void places_nearby() {
        String placeId = System.getenv("GOUP_TEST_PLACE_ID");
        PlaceDetails details = googlePlacesClient.getPlaceDetails(placeId);

        List<Place> places = googlePlacesClient.searchNearby(
                placeId, details.location().latitude(), details.location().longitude(), 5, null);

        assertThat(places).isNotEmpty().hasSizeLessThanOrEqualTo(5);
        assertThat(places).allSatisfy(place -> {
            assertThat(place.id()).isNotBlank();
            assertThat(place.location()).isNotNull();
        });
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GOOGLE_PLACES_API_KEY", matches = ".+")
    @DisplayName("Places: placeId inválido vira NotFoundException")
    void places_invalidPlaceId() {
        assertThatThrownBy(() -> googlePlacesClient.getPlaceDetails("placeId-que-nao-existe"))
                .isInstanceOf(NotFoundException.class);
    }
}
