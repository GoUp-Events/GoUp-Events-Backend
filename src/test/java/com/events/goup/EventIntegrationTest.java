package com.events.goup;

import com.events.goup.entity.Category;
import com.events.goup.entity.Event;
import com.events.goup.entity.Favorite;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Eventos: criação, visibilidade de rascunhos, permissões e filtros")
class EventIntegrationTest extends MockedApisTestBase {

    private User owner;
    private User other;
    private User admin;
    private Location blumenau;
    private Location joinville;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = createUser("dono@goup.com", false);
        other = createUser("outro@goup.com", false);
        admin = createAdmin("admin@goup.com");
        blumenau = createLocation("place-blu", "Vila Germânica", "Blumenau", -26.9194, -49.0661);
        joinville = createLocation("place-joi", "Expoville", "Joinville", -26.3045, -48.8487);
        category = categoryAt(0);
    }

    private String eventJson(String title, LocalDate date, String start, String end,
                             String status, String placeId, Long categoryId) {
        return """
                {"title":"%s","description":"Descrição","eventDate":"%s","startTime":"%s","endTime":%s,
                 "price":25.00,"status":%s,"ageRating":"FREE","placeId":"%s","categoryId":%s}
                """.formatted(title, date, start,
                end == null ? "null" : "\"" + end + "\"",
                status == null ? "null" : "\"" + status + "\"",
                placeId, categoryId);
    }

    // ---------- criação ----------

    @Test
    @DisplayName("qualquer usuário logado cria evento: dono vem do JWT e o status padrão é DRAFT")
    void create_ok() throws Exception {
        perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Oktoberfest", daysFromNow(10), "20:00", "23:00", null, "place-blu", category.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Oktoberfest"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.eventDate").value(daysFromNow(10).toString()))
                .andExpect(jsonPath("$.user.id").value(owner.getId().intValue()))
                .andExpect(jsonPath("$.location.placeId").value("place-blu"))
                .andExpect(jsonPath("$.category.id").value(category.getId().intValue()));
    }

    @Test
    @DisplayName("evento com placeId novo busca o local no Google e salva")
    void create_withNewPlaceId_callsGoogle() throws Exception {
        stubPlaceDetails("place-novo", "Teatro Carlos Gomes", "Blumenau", -26.92, -49.07);

        perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Peça de Teatro", daysFromNow(10), "19:00", null, "PUBLISHED", "place-novo", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.location.name").value("Teatro Carlos Gomes"))
                .andExpect(jsonPath("$.category").doesNotExist());

        verify(googlePlacesClient).getPlaceDetails("place-novo");
        assertThat(locationRepository.findByPlaceId("place-novo")).isPresent();
    }

    @Test
    @DisplayName("validações: campos obrigatórios, data no passado, horário final antes do inicial e categoria inexistente")
    void create_validation() throws Exception {
        perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors").isNotEmpty());

        perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Passado", daysFromNow(-1), "20:00", null, null, "place-blu", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].message", hasItem("A data do evento não pode estar no passado")));

        perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Horário errado", daysFromNow(5), "20:00", "19:00", null, "place-blu", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O horário de término deve ser posterior ao horário de início"));

        perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Sem categoria", daysFromNow(5), "20:00", null, null, "place-blu", 999_999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Categoria não encontrada com o id: 999999"));
    }

    // ---------- visibilidade de rascunhos ----------

    @Test
    @DisplayName("rascunho não aparece na lista pública, mas aparece em /users/me/events do dono")
    void draft_hiddenFromPublicList() throws Exception {
        Event published = createPublishedEvent(owner, blumenau, "Publicado", 3);
        Event draft = createEvent(owner, blumenau, null, EventStatus.DRAFT, daysFromNow(3), "Rascunho");

        perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(published.getId().intValue())))
                .andExpect(jsonPath("$[*].id", not(hasItem(draft.getId().intValue()))));

        perform(get("/users/me/events").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        perform(get("/users/me/events").header("Authorization", bearer(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /events/{id} de rascunho: 404 para visitante e outros usuários; 200 para dono e ADMIN")
    void draft_visibilityById() throws Exception {
        Event draft = createEvent(owner, blumenau, null, EventStatus.DRAFT, daysFromNow(3), "Rascunho");
        String url = "/events/" + draft.getId();

        perform(get(url)).andExpect(status().isNotFound());
        perform(get(url).header("Authorization", bearer(other))).andExpect(status().isNotFound());
        perform(get(url).header("Authorization", bearer(owner))).andExpect(status().isOk());
        perform(get(url).header("Authorization", bearer(admin))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("evento publicado é público; id inexistente dá 404")
    void published_isPublic() throws Exception {
        Event published = createPublishedEvent(owner, blumenau, "Publicado", 3);

        perform(get("/events/" + published.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Publicado"));
        perform(get("/events/999999")).andExpect(status().isNotFound());
    }

    // ---------- permissões ----------

    @Test
    @DisplayName("editar: dono e ADMIN podem; outro usuário recebe 403; sem login recebe 401")
    void update_permissions() throws Exception {
        Event event = createPublishedEvent(owner, blumenau, "Original", 5);
        String url = "/events/" + event.getId();
        String body = eventJson("Editado", daysFromNow(6), "21:00", null, null, "place-blu", null);

        perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        perform(put(url).header("Authorization", bearer(other)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem permissão para alterar este evento"));

        perform(put(url).header("Authorization", bearer(owner)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Editado"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        perform(put(url).header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Editado pelo admin", daysFromNow(6), "21:00", null, "CANCELLED", "place-blu", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Editado pelo admin"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.user.id").value(owner.getId().intValue()));
    }

    @Test
    @DisplayName("editar evento inexistente dá 404")
    void update_notFound() throws Exception {
        perform(put("/events/999999").header("Authorization", bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("X", daysFromNow(6), "21:00", null, null, "place-blu", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("excluir: outro usuário recebe 403; dono exclui (204) e os favoritos do evento somem junto")
    void delete_permissionsAndFavoritesCleanup() throws Exception {
        Event event = createPublishedEvent(owner, blumenau, "Para excluir", 5);
        favoriteRepository.save(new Favorite(other, event));
        String url = "/events/" + event.getId();

        perform(delete(url).header("Authorization", bearer(other))).andExpect(status().isForbidden());
        assertThat(favoriteRepository.count()).isEqualTo(1);

        perform(delete(url).header("Authorization", bearer(owner))).andExpect(status().isNoContent());

        assertThat(favoriteRepository.count()).isZero();
        perform(get(url)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN pode excluir evento de qualquer usuário")
    void delete_adminCanDeleteAnyEvent() throws Exception {
        Event event = createPublishedEvent(owner, blumenau, "Do dono", 5);

        perform(delete("/events/" + event.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
    }

    // ---------- filtros ----------

    @Test
    @DisplayName("filtros por cidade, categoria, preço e período")
    void filters() throws Exception {
        Event inBlumenau = createEvent(owner, blumenau, category, EventStatus.PUBLISHED, daysFromNow(3), "Em Blumenau");
        Event inJoinville = createEvent(owner, joinville, categoryAt(1), EventStatus.PUBLISHED, daysFromNow(20), "Em Joinville");

        perform(get("/events").param("city", "BLUMENAU"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inBlumenau.getId().intValue()));

        perform(get("/events").param("categoryId", categoryAt(1).getId().toString()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inJoinville.getId().intValue()));

        perform(get("/events").param("maxPrice", "10"))
                .andExpect(jsonPath("$").isEmpty());
        perform(get("/events").param("maxPrice", "30"))
                .andExpect(jsonPath("$.length()").value(2));

        perform(get("/events")
                .param("dateFrom", daysFromNow(10).toString())
                .param("dateTo", daysFromNow(30).toString()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inJoinville.getId().intValue()));
    }

    @Test
    @DisplayName("data final anterior à inicial responde 400")
    void filters_invalidDateRange() throws Exception {
        perform(get("/events")
                .param("dateFrom", daysFromNow(10).toString())
                .param("dateTo", daysFromNow(5).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A data final não pode ser anterior à data inicial"));
    }

    @Test
    @DisplayName("JSON do evento criado pode ser relido pelo id")
    void create_thenReadById() throws Exception {
        String body = perform(post("/events")
                .header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson("Rodada", daysFromNow(10), "20:00", null, "PUBLISHED", "place-blu", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.id");

        perform(get("/events/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Rodada"));
    }
}
