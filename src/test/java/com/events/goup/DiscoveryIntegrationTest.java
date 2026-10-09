package com.events.goup;

import com.events.goup.client.GeminiClient.Suggestion;
import com.events.goup.entity.Event;
import com.events.goup.entity.Location;
import com.events.goup.entity.User;
import com.events.goup.entity.enums.EventStatus;
import com.events.goup.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Descoberta Mágica (chatbot com Gemini, exclusivo Premium)")
class DiscoveryIntegrationTest extends MockedApisTestBase {

    private User premium;
    private User organizer;
    private Location blumenau;
    private Location joinville;

    @BeforeEach
    void setUp() {
        premium = createUser("premium@goup.com", true);
        organizer = createUser("organizador@goup.com", false);
        blumenau = createLocation("place-blu", "Vila Germânica", "Blumenau", -26.9194, -49.0661);
        joinville = createLocation("place-joi", "Expoville", "Joinville", -26.3045, -48.8487);
    }

    private ResultActions discover(User user, String json) throws Exception {
        return perform(post("/discovery")
                .header("Authorization", bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String message(String text) {
        return """
                {"message":"%s"}
                """.formatted(text);
    }

    private String capturePrompt() {
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).suggest(anyString(), prompt.capture());
        return prompt.getValue();
    }

    @Test
    @DisplayName("devolve só eventos reais: descarta ids inventados e repetidos, mantém a ordem da IA")
    void discover_returnsOnlyRealCandidates() throws Exception {
        Event oktoberfest = createPublishedEvent(organizer, blumenau, "Oktoberfest Blumenau", 5);
        Event festival = createPublishedEvent(organizer, joinville, "Festival de Dança", 8);

        when(geminiClient.suggest(anyString(), anyString())).thenReturn(new Suggestion(
                List.of(festival.getId(), 999_999L, oktoberfest.getId(), festival.getId()),
                "Separei dois eventos para você."));

        discover(premium, message("algo divertido esse mês"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Separei dois eventos para você."))
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].id").value(festival.getId().intValue()))
                .andExpect(jsonPath("$.events[0].title").value("Festival de Dança"))
                .andExpect(jsonPath("$.events[1].id").value(oktoberfest.getId().intValue()));
    }

    @Test
    @DisplayName("retorna no máximo 5 eventos mesmo que a IA devolva mais")
    void discover_limitsToFiveEvents() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            ids.add(createPublishedEvent(organizer, blumenau, "Evento " + i, i).getId());
        }
        when(geminiClient.suggest(anyString(), anyString())).thenReturn(new Suggestion(ids, "Muitos eventos."));

        discover(premium, message("quero tudo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(5));
    }

    @Test
    @DisplayName("a IA responder lista vazia devolve resumo e nenhum evento")
    void discover_aiReturnsNoEvents() throws Exception {
        createPublishedEvent(organizer, blumenau, "Oktoberfest Blumenau", 5);
        when(geminiClient.suggest(anyString(), anyString()))
                .thenReturn(new Suggestion(List.of(), "Nada combina com o seu pedido."));

        discover(premium, message("algo impossível"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Nada combina com o seu pedido."))
                .andExpect(jsonPath("$.events").isEmpty());
    }

    @Test
    @DisplayName("o prompt enviado ao Gemini tem só eventos PUBLISHED e futuros")
    void discover_promptHasOnlyPublishedFutureEvents() throws Exception {
        Event published = createPublishedEvent(organizer, blumenau, "Evento Publicado", 3);
        Event draft = createEvent(organizer, blumenau, null, EventStatus.DRAFT, daysFromNow(3), "Rascunho Secreto");
        Event cancelled = createEvent(organizer, blumenau, null, EventStatus.CANCELLED, daysFromNow(3), "Evento Cancelado");
        Event past = createPublishedEvent(organizer, blumenau, "Evento Passado", -2);
        createPublishedEvent(organizer, blumenau, "Evento de Hoje", 0);
        when(geminiClient.suggest(anyString(), anyString())).thenReturn(new Suggestion(List.of(), "ok"));

        discover(premium, message("o que tem pra hoje?")).andExpect(status().isOk());

        String prompt = capturePrompt();
        assertThat(prompt).contains("id=" + published.getId() + " |", "Evento Publicado", "Evento de Hoje");
        assertThat(prompt).doesNotContain("Rascunho Secreto", "Evento Cancelado", "Evento Passado");
        assertThat(prompt).doesNotContain("id=" + draft.getId() + " |", "id=" + cancelled.getId() + " |",
                "id=" + past.getId() + " |");
        assertThat(prompt).contains("o que tem pra hoje?");
    }

    @Test
    @DisplayName("as instruções de sistema obrigam a IA a escolher só entre os candidatos")
    void discover_systemInstructionRestrictsToCandidates() throws Exception {
        createPublishedEvent(organizer, blumenau, "Evento Publicado", 3);
        when(geminiClient.suggest(anyString(), anyString())).thenReturn(new Suggestion(List.of(), "ok"));

        discover(premium, message("qualquer coisa")).andExpect(status().isOk());

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).suggest(system.capture(), anyString());
        assertThat(system.getValue()).contains("SOMENTE eventos da lista de candidatos");
    }

    @Test
    @DisplayName("filtro de cidade: só eventos daquela cidade vão para o prompt")
    void discover_filtersByCity() throws Exception {
        Event inBlumenau = createPublishedEvent(organizer, blumenau, "Show em Blumenau", 3);
        createPublishedEvent(organizer, joinville, "Show em Joinville", 3);
        when(geminiClient.suggest(anyString(), anyString())).thenReturn(new Suggestion(List.of(inBlumenau.getId()), "Achei um."));

        discover(premium, """
                {"message":"um show","city":" Blumenau "}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1));

        assertThat(capturePrompt()).contains("Show em Blumenau").doesNotContain("Show em Joinville");
    }

    @Test
    @DisplayName("no máximo 20 candidatos vão para o Gemini (os mais próximos no tempo)")
    void discover_limitsCandidatesToTwenty() throws Exception {
        Event last = null;
        for (int i = 1; i <= 25; i++) {
            last = createPublishedEvent(organizer, blumenau, "Evento " + i, i);
        }
        when(geminiClient.suggest(anyString(), anyString())).thenReturn(new Suggestion(List.of(), "ok"));

        discover(premium, message("tudo")).andExpect(status().isOk());

        String prompt = capturePrompt();
        assertThat(prompt.split("- id=").length - 1).isEqualTo(20);
        assertThat(prompt).doesNotContain("id=" + last.getId() + " |");
    }

    @Test
    @DisplayName("sem candidatos o Gemini nem é chamado")
    void discover_noCandidates_doesNotCallGemini() throws Exception {
        discover(premium, message("algo legal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Não encontrei eventos futuros publicados no momento."))
                .andExpect(jsonPath("$.events").isEmpty());

        createPublishedEvent(organizer, blumenau, "Show em Blumenau", 3);
        discover(premium, """
                {"message":"algo legal","city":"Florianópolis"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Não encontrei eventos futuros publicados em Florianópolis no momento."))
                .andExpect(jsonPath("$.events").isEmpty());

        verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("falha do Gemini vira 502 com a mensagem do erro")
    void discover_geminiFailure_returns502() throws Exception {
        createPublishedEvent(organizer, blumenau, "Show em Blumenau", 3);
        when(geminiClient.suggest(anyString(), anyString()))
                .thenThrow(new ExternalServiceException("Falha ao consultar o Gemini"));

        discover(premium, message("um show"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Falha ao consultar o Gemini"));
    }

    @Test
    @DisplayName("resposta do Gemini em formato inválido também vira 502")
    void discover_invalidGeminiFormat_returns502() throws Exception {
        createPublishedEvent(organizer, blumenau, "Show em Blumenau", 3);
        when(geminiClient.suggest(anyString(), anyString()))
                .thenThrow(new ExternalServiceException("Resposta do Gemini em formato inválido"));

        discover(premium, message("um show"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Resposta do Gemini em formato inválido"));
    }

    @Test
    @DisplayName("valida o pedido: mensagem vazia, ausente, longa demais ou cidade longa demais dão 400")
    void discover_validation() throws Exception {
        discover(premium, "{\"message\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("message"));

        discover(premium, "{}").andExpect(status().isBadRequest());

        discover(premium, message("a".repeat(501))).andExpect(status().isBadRequest());

        discover(premium, """
                {"message":"ok","city":"%s"}
                """.formatted("c".repeat(101))).andExpect(status().isBadRequest());

        verifyNoInteractions(geminiClient);
    }

    @Test
    @DisplayName("sem token responde 401 e usuário Free responde 403, sem chamar o Gemini")
    void discover_requiresPremium() throws Exception {
        perform(post("/discovery").contentType(MediaType.APPLICATION_JSON).content(message("oi")))
                .andExpect(status().isUnauthorized());

        User free = createUser("free@goup.com", false);
        createPublishedEvent(organizer, blumenau, "Show em Blumenau", 3);
        discover(free, message("um show")).andExpect(status().isForbidden());

        verifyNoInteractions(geminiClient);
    }
}
