package sn.cse.notification.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;
import sn.cse.notification.api.controller.NotificationController;
import sn.cse.notification.api.sse.AbonnementsSse;
import sn.cse.notification.api.sse.SseProperties;
import sn.cse.notification.application.port.in.ConsulterNotificationsUseCase;
import sn.cse.notification.domain.model.Destinataire;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.Notification;
import sn.cse.notification.domain.model.NotificationVue;

@WebMvcTest(NotificationController.class)
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class
})
@Import(AbonnementsSse.class)
@EnableConfigurationProperties(SseProperties.class)
class NotificationControllerTest {

  private static final UUID MOI = UUID.fromString("00000000-0000-0000-0000-000000000201");

  @Autowired private MockMvc mvc;
  @MockitoBean private ConsulterNotificationsUseCase useCase;

  @Test
  void should_return_401_without_token() throws Exception {
    mvc.perform(get("/api/notifications"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    mvc.perform(get("/api/notifications/stream")).andExpect(status().isUnauthorized());
  }

  @Test
  void should_return_403_for_supplier_account() throws Exception {
    mvc.perform(get("/api/notifications/non-lues/count").with(CseJwt.as(MOI, Roles.FOURNISSEUR)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }

  @Test
  void should_list_notifications_of_current_user() throws Exception {
    Notification n =
        new Notification(
            UUID.randomUUID(),
            "BESOIN_SOUMIS",
            "Nouveau besoin",
            "Message",
            NiveauNotification.INFO,
            "/besoins/1",
            "BESOIN",
            "1",
            Destinataire.role(Roles.RESPONSABLE_MATERIEL),
            UUID.randomUUID(),
            Instant.parse("2026-10-30T09:15:00Z"));
    when(useCase.lister(any(), eq(false), any()))
        .thenReturn(PageResponse.of(List.of(new NotificationVue(n, false)), 0, 20, 1));
    mvc.perform(
            get("/api/notifications?lue=false&size=200")
                .with(CseJwt.as(MOI, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].titre").value("Nouveau besoin"))
        .andExpect(jsonPath("$.content[0].lue").value(false))
        .andExpect(jsonPath("$.content[0].niveau").value("INFO"))
        .andExpect(jsonPath("$.content[0].lien").value("/besoins/1"))
        .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-30T09:15:00Z"))
        .andExpect(jsonPath("$.totalElements").value(1));
    verify(useCase)
        .lister(any(), eq(false), org.mockito.ArgumentMatchers.argThat(q -> q.size() == 50));
  }

  @Test
  void should_count_mark_and_mark_all() throws Exception {
    when(useCase.compterNonLues(any())).thenReturn(3L);
    mvc.perform(get("/api/notifications/non-lues/count").with(CseJwt.chef(MOI, UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(3));
    mvc.perform(post("/api/notifications/tout-lire").with(CseJwt.as(MOI, Roles.DAF)))
        .andExpect(status().isNoContent());
    UUID inconnue = UUID.randomUUID();
    when(useCase.marquerLue(any(), eq(inconnue)))
        .thenThrow(new ResourceNotFoundException("Notification", inconnue));
    mvc.perform(patch("/api/notifications/{id}/lue", inconnue).with(CseJwt.as(MOI, Roles.DAF)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOTIFICATION_INTROUVABLE"));
    mvc.perform(patch("/api/notifications/pas-un-uuid/lue").with(CseJwt.as(MOI, Roles.DAF)))
        .andExpect(status().isBadRequest());
    verify(useCase).toutLire(any());
  }

  @Test
  void should_open_sse_stream_with_initial_ping() throws Exception {
    mvc.perform(get("/api/notifications/stream").with(CseJwt.as(MOI, Roles.DIRECTION_GENERALE)))
        .andExpect(request().asyncStarted())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("event:ping")));
  }

  @Test
  void should_reject_invalid_lue_filter() throws Exception {
    mvc.perform(get("/api/notifications?lue=peut-etre").with(CseJwt.as(MOI, Roles.DAF)))
        .andExpect(status().isBadRequest());
    verify(useCase, org.mockito.Mockito.never()).lister(any(), isNull(), any());
  }
}
