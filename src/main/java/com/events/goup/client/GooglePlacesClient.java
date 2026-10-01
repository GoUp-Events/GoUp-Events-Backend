package com.events.goup.client;

import com.events.goup.exception.ExternalServiceException;
import com.events.goup.exception.NotFoundException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente da Google Places API (New): https://developers.google.com/maps/documentation/places/web-service
 * O field mask define o que é cobrado, então pedimos só os campos que usamos.
 */
@Component
public class GooglePlacesClient {

    private static final String BASE_URL = "https://places.googleapis.com/v1";
    private static final String DETAILS_FIELD_MASK =
            "id,displayName,formattedAddress,addressComponents,location,rating,priceLevel";
    private static final String NEARBY_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location,"
                    + "places.primaryTypeDisplayName,places.googleMapsUri";

    private final RestClient restClient;
    private final String apiKey;
    private final double nearbyRadiusMeters;
    private final List<String> nearbyTypes;

    public GooglePlacesClient(@Value("${goup.google.places.api-key:}") String apiKey,
                              @Value("${goup.google.places.nearby-radius-meters:1500}") double nearbyRadiusMeters,
                              @Value("${goup.google.places.nearby-types:}") String nearbyTypes) {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
        this.nearbyRadiusMeters = nearbyRadiusMeters;
        this.nearbyTypes = Arrays.stream(nearbyTypes.split(","))
                .map(String::trim)
                .filter(type -> !type.isEmpty())
                .toList();
    }

    public PlaceDetails getPlaceDetails(String placeId) {
        requireApiKey();
        try {
            PlaceDetails details = restClient.get()
                    .uri(uri -> uri.path("/places/{placeId}")
                            .queryParam("languageCode", "pt-BR")
                            .queryParam("regionCode", "BR")
                            .build(placeId))
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", DETAILS_FIELD_MASK)
                    .retrieve()
                    .body(PlaceDetails.class);

            if (details == null || details.location() == null) {
                throw new NotFoundException("Local não encontrado no Google Places: " + placeId);
            }
            return details;
        } catch (HttpClientErrorException exception) {
            throw new NotFoundException("Local não encontrado no Google Places: " + placeId);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Falha ao consultar o Google Places", exception);
        }
    }

    /**
     * Resultado é cacheado por placeId: os lugares ao redor de um local quase não mudam,
     * e isso evita gastar cota da API a cada visualização do evento.
     */
    @Cacheable(cacheNames = "nearbyPlaces", key = "#placeId")
    public List<Place> searchNearby(String placeId, double latitude, double longitude, int maxResults) {
        requireApiKey();

        Map<String, Object> body = new HashMap<>();
        body.put("maxResultCount", maxResults);
        body.put("rankPreference", "DISTANCE");
        body.put("languageCode", "pt-BR");
        body.put("regionCode", "BR");
        body.put("locationRestriction", Map.of("circle", Map.of(
                "center", Map.of("latitude", latitude, "longitude", longitude),
                "radius", nearbyRadiusMeters)));
        if (!nearbyTypes.isEmpty()) {
            body.put("includedTypes", nearbyTypes);
        }

        try {
            NearbyResponse response = restClient.post()
                    .uri("/places:searchNearby")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", NEARBY_FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(NearbyResponse.class);

            return response != null && response.places() != null ? response.places() : List.of();
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Falha ao consultar locais próximos no Google Places", exception);
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalServiceException("Chave do Google Places não configurada (GOOGLE_PLACES_API_KEY)");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlaceDetails(String id,
                               LocalizedText displayName,
                               String formattedAddress,
                               List<AddressComponent> addressComponents,
                               LatLng location,
                               Double rating,
                               String priceLevel) {

        public String component(String type, boolean shortText) {
            if (addressComponents == null) {
                return null;
            }
            return addressComponents.stream()
                    .filter(component -> component.types() != null && component.types().contains(type))
                    .map(component -> shortText ? component.shortText() : component.longText())
                    .findFirst()
                    .orElse(null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(String id,
                        LocalizedText displayName,
                        String formattedAddress,
                        LatLng location,
                        LocalizedText primaryTypeDisplayName,
                        String googleMapsUri) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocalizedText(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressComponent(String longText, String shortText, List<String> types) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LatLng(Double latitude, Double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record NearbyResponse(List<Place> places) {
    }
}
