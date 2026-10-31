package sn.cse.chatbot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.chatbot.api.controller.ChatbotController;
import sn.cse.chatbot.application.exception.LimiteDebitDepasseeException;
import sn.cse.chatbot.application.port.in.ConsulterHistoriqueUseCase;
import sn.cse.chatbot.application.port.in.ReponseChatbot;
import sn.cse.chatbot.application.port.in.SuggestionsUseCase;
import sn.cse.chatbot.application.port.in.TraiterQuestionUseCase;
import sn.cse.chatbot.domain.model.Conversation;
import sn.cse.chatbot.domain.model.ConversationResume;
import sn.cse.chatbot.domain.model.Intention;
import sn.cse.chatbot.domain.model.Source;
import sn.cse.chatbot.domain.model.TypeSource;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;

@WebMvcTest(ChatbotController.class)
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class
})
class ChatbotControllerTest {

  private static final UUID MOI = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final Instant T0 = Instant.parse("2026-10-30T09:15:00Z");

  @Autowired private MockMvc mvc;
  @MockitoBean private TraiterQuestionUseCase questions;
  @MockitoBean private ConsulterHistoriqueUseCase historique;
  @MockitoBean private SuggestionsUseCase suggestions;

  @Test
  void should_require_authentication_and_internal_role() throws Exception {
    mvc.perform(get("/api/chatbot/suggestions")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"x\"}")
                .with(CseJwt.as(MOI, Roles.FOURNISSEUR)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    verify(questions, never()).traiter(any());
  }

  @Test
  void should_answer_question_with_contract_shape() throws Exception {
    UUID conv = UUID.randomUUID();
    when(questions.traiter(
            argThat(
                c ->
                    c.question().equals("Où est la niveleuse ?")
                        && c.utilisateur().id().equals(MOI)
                        && conv.equals(c.conversationId()))))
        .thenReturn(
            new ReponseChatbot(
                conv,
                UUID.randomUUID(),
                "**NIV-002** à Thiès",
                List.of(
                    new Source(TypeSource.MATERIEL, "m1", "NIV-002 · Niveleuse", "/materiels/m1")),
                Intention.LOCALISER_ENGIN,
                true,
                T0));
    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"question\":\"Où est la niveleuse ?\",\"conversationId\":\"" + conv + "\"}")
                .with(CseJwt.chef(MOI, UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversationId").value(conv.toString()))
        .andExpect(jsonPath("$.reponse").value("**NIV-002** à Thiès"))
        .andExpect(jsonPath("$.sources[0].type").value("MATERIEL"))
        .andExpect(jsonPath("$.sources[0].lien").value("/materiels/m1"))
        .andExpect(jsonPath("$.intention").value("LOCALISER_ENGIN"))
        .andExpect(jsonPath("$.modeDegrade").value(true))
        .andExpect(jsonPath("$.horodatage").value("2026-10-30T09:15:00Z"));
  }

  @Test
  void should_return_problem_details_for_invalid_question_and_rate_limit() throws Exception {
    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"  \"}")
                .with(CseJwt.as(MOI, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("question"));
    when(questions.traiter(any())).thenThrow(new LimiteDebitDepasseeException());
    mvc.perform(
            post("/api/chatbot/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"Où est la pelle ?\"}")
                .with(CseJwt.as(MOI, Roles.DAF)))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("TROP_DE_REQUETES"));
  }

  @Test
  void should_list_read_and_delete_history() throws Exception {
    UUID id = UUID.randomUUID();
    when(historique.lister(any(), argThat(q -> q.size() == 50)))
        .thenReturn(
            PageResponse.of(
                List.of(new ConversationResume(id, "Où est la pelle ?", "Au dépôt", 2, T0)),
                0,
                50,
                1));
    mvc.perform(
            get("/api/chatbot/historique?size=500").with(CseJwt.as(MOI, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].titre").value("Où est la pelle ?"))
        .andExpect(jsonPath("$.content[0].nombreMessages").value(2))
        .andExpect(jsonPath("$.size").value(50));

    Conversation c = Conversation.demarrer(MOI, "Où est la pelle ?", T0, 80);
    c.ajouterQuestion("Où est la pelle ?", T0);
    c.ajouterReponse("Au dépôt", List.of(), Intention.LOCALISER_ENGIN, true, T0);
    when(historique.detail(any(), eq(c.id()))).thenReturn(c);
    mvc.perform(
            get("/api/chatbot/historique/{id}", c.id())
                .with(CseJwt.as(MOI, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.messages[0].role").value("UTILISATEUR"))
        .andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
        .andExpect(jsonPath("$.messages[1].sources").isArray());

    when(historique.detail(any(), eq(id)))
        .thenThrow(new ResourceNotFoundException("Conversation", id));
    mvc.perform(
            get("/api/chatbot/historique/{id}", id).with(CseJwt.as(MOI, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CONVERSATION_INTROUVABLE"));
    mvc.perform(
            delete("/api/chatbot/historique/{id}", id).with(CseJwt.as(MOI, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isNoContent());
    verify(historique).supprimer(any(), eq(id));
  }

  @Test
  void should_return_role_suggestions() throws Exception {
    when(suggestions.suggestions(any())).thenReturn(List.of("Où en est mon dernier besoin ?"));
    mvc.perform(get("/api/chatbot/suggestions").with(CseJwt.chef(MOI)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("Où en est mon dernier besoin ?"));
  }
}
