package com.events.goup;

import com.events.goup.client.GeminiClient;
import com.events.goup.client.GooglePlacesClient;
import com.events.goup.client.GooglePlacesClient.AddressComponent;
import com.events.goup.client.GooglePlacesClient.LatLng;
import com.events.goup.client.GooglePlacesClient.LocalizedText;
import com.events.goup.client.GooglePlacesClient.Place;
import com.events.goup.client.GooglePlacesClient.PlaceDetails;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Igual à base normal, mas o Gemini e o Google Places são mockados: nenhuma chamada de rede é feita,
 * então não gasta cota, não depende de chave e o resultado é sempre o mesmo.
 * O que roda de verdade: banco, segurança, JWT, controllers, services e regras de plano.
 */
public abstract class MockedApisTestBase extends IntegrationTestBase {

    @MockitoBean protected GeminiClient geminiClient;
    @MockitoBean protected GooglePlacesClient googlePlacesClient;

    @BeforeEach
    void stubNearbyTypes() {
        when(googlePlacesClient.getNearbyTypes())
                .thenReturn(List.of("restaurant", "cafe", "bar", "bakery", "tourist_attraction", "park"));
    }

    protected PlaceDetails stubPlaceDetails(String placeId, String name, String city, double latitude, double longitude) {
        PlaceDetails details = new PlaceDetails(
                placeId,
                new LocalizedText(name),
                "Rua XV de Novembro, 1000 - Centro, " + city + " - SC, Brasil",
                List.of(
                        new AddressComponent("Rua XV de Novembro", "R. XV de Novembro", List.of("route")),
                        new AddressComponent("1000", "1000", List.of("street_number")),
                        new AddressComponent("Centro", "Centro", List.of("sublocality_level_1", "sublocality", "political")),
                        new AddressComponent(city, city, List.of("locality", "political")),
                        new AddressComponent("Santa Catarina", "SC", List.of("administrative_area_level_1", "political")),
                        new AddressComponent("89010-000", "89010-000", List.of("postal_code"))),
                new LatLng(latitude, longitude),
                4.6,
                "PRICE_LEVEL_MODERATE");
        when(googlePlacesClient.getPlaceDetails(placeId)).thenReturn(details);
        return details;
    }

    protected Place place(String id, String name, double latitude, double longitude, String priceLevel) {
        return new Place(id, new LocalizedText(name), name + ", Blumenau", new LatLng(latitude, longitude),
                new LocalizedText("Restaurante"), "https://maps.google.com/?cid=" + id, 4.5, 120, priceLevel);
    }
}
