package com.events.goup;

import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.exception.ExternalServiceException;
import com.events.goup.exception.NotFoundException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Locais via Google Places (Place Details)")
class LocationPlacesIntegrationTest extends MockedApisTestBase {

    private User user;

    @BeforeEach
    void setUp() {
        user = createUser("ana@goup.com", false);
    }

    private String placeJson(String placeId) {
        return """
                {"placeId":"%s"}
                """.formatted(placeId);
    }

    @Test
    @DisplayName("placeId novo: busca o Place Details no Google e salva o local com os campos convertidos")
    void resolve_newPlace_fetchesFromGoogleAndSaves() throws Exception {
        stubPlaceDetails("place-vila", "Vila Germânica", "Blumenau", -26.9194, -49.0661);

        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("place-vila")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.placeId").value("place-vila"))
                .andExpect(jsonPath("$.name").value("Vila Germânica"))
                .andExpect(jsonPath("$.formattedAddress").value("Rua XV de Novembro, 1000 - Centro, Blumenau - SC, Brasil"))
                .andExpect(jsonPath("$.address").value("Rua XV de Novembro"))
                .andExpect(jsonPath("$.number").value("1000"))
                .andExpect(jsonPath("$.neighborhood").value("Centro"))
                .andExpect(jsonPath("$.city").value("Blumenau"))
                .andExpect(jsonPath("$.state").value("SC"))
                .andExpect(jsonPath("$.zip").value("89010-000"))
                .andExpect(jsonPath("$.latitude").value(-26.9194))
                .andExpect(jsonPath("$.longitude").value(-49.0661))
                .andExpect(jsonPath("$.rating").value(4.6))
                .andExpect(jsonPath("$.priceLevel").value("MODERATE"));

        assertThat(locationRepository.findByPlaceId("place-vila")).isPresent();
        verify(googlePlacesClient, times(1)).getPlaceDetails("place-vila");
    }

    @Test
    @DisplayName("placeId repetido: reaproveita o local e não chama o Google de novo")
    void resolve_existingPlace_isReusedWithoutCallingGoogle() throws Exception {
        stubPlaceDetails("place-vila", "Vila Germânica", "Blumenau", -26.9194, -49.0661);

        String first = perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("place-vila")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String second = perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("  place-vila  ")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number firstId = JsonPath.read(first, "$.id");
        Number secondId = JsonPath.read(second, "$.id");
        assertThat(secondId.longValue()).isEqualTo(firstId.longValue());
        assertThat(locationRepository.count()).isEqualTo(1);
        verify(googlePlacesClient, times(1)).getPlaceDetails("place-vila");
    }

    @Test
    @DisplayName("local que já está no banco não chama o Google")
    void resolve_placeAlreadyInDatabase() throws Exception {
        createLocation("place-existente", "Local Antigo", "Blumenau", -26.9, -49.0);

        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("place-existente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Local Antigo"));

        verify(googlePlacesClient, never()).getPlaceDetails("place-existente");
    }

    @Test
    @DisplayName("placeId que o Google não conhece responde 404")
    void resolve_unknownPlace_returns404() throws Exception {
        when(googlePlacesClient.getPlaceDetails("place-fantasma"))
                .thenThrow(new NotFoundException("Local não encontrado no Google Places: place-fantasma"));

        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("place-fantasma")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Local não encontrado no Google Places: place-fantasma"));

        assertThat(locationRepository.findByPlaceId("place-fantasma")).isEmpty();
    }

    @Test
    @DisplayName("Google fora do ar responde 502 e não salva nada")
    void resolve_googleDown_returns502() throws Exception {
        when(googlePlacesClient.getPlaceDetails("place-vila"))
                .thenThrow(new ExternalServiceException("Falha ao consultar o Google Places"));

        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("place-vila")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Falha ao consultar o Google Places"));

        assertThat(locationRepository.count()).isZero();
    }

    @Test
    @DisplayName("placeId vazio ou ausente responde 400 sem chamar o Google")
    void resolve_validation() throws Exception {
        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(placeJson("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("placeId"));

        perform(post("/locations")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());

        verify(googlePlacesClient, never()).getPlaceDetails(anyString());
    }

    @Test
    @DisplayName("POST /locations exige login; GET /locations e /locations/{id} são públicos")
    void access_rules() throws Exception {
        perform(post("/locations").contentType(MediaType.APPLICATION_JSON).content(placeJson("place-vila")))
                .andExpect(status().isUnauthorized());

        Location saved = createLocation("place-vila", "Vila Germânica", "Blumenau", -26.9, -49.0);

        perform(get("/locations")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        perform(get("/locations/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value("place-vila"));
        perform(get("/locations/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT e DELETE em /locations não existem mais")
    void editAndDelete_wereRemoved() throws Exception {
        Location saved = createLocation("place-vila", "Vila Germânica", "Blumenau", -26.9, -49.0);

        perform(delete("/locations/" + saved.getId()).header("Authorization", bearer(user)))
                .andExpect(status().isMethodNotAllowed());
        perform(put("/locations/" + saved.getId()).header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(placeJson("x")))
                .andExpect(status().isMethodNotAllowed());
    }
}