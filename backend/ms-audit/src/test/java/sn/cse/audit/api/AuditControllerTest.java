package sn.cse.audit.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.audit.api.controller.AuditController;
import sn.cse.audit.application.port.in.ConsulterAuditUseCase;
import sn.cse.audit.domain.model.AuditLog;
import sn.cse.audit.domain.model.StatistiquesAudit;
import sn.cse.audit.domain.model.StatistiquesAudit.TotalParService;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;

@WebMvcTest(AuditController.class)
@ActiveProfiles("test")
@Import({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class
})
class AuditControllerTest {

  private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000601");

  @Autowired private MockMvc mvc;

  @MockitoBean private ConsulterAuditUseCase consulter;

  private static AuditLog entree() {
    Instant t = Instant.parse("2026-10-30T09:15:00Z");
    return new AuditLog(
        UUID.randomUUID(),
        UUID.randomUUID(),
        t,
        "ms-besoins",
        "BESOIN_VALIDE",
        "BesoinMateriel",
        "b1",
        "u1",
        "Ibrahima Fall",
        List.of("RESPONSABLE_MATERIEL"),
        "Besoin validé",
        "corr",
        t);
  }

  @Test
  void should_retourner_401_problem_detail_when_sans_jeton() throws Exception {
    mvc.perform(get("/api/audit"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @Test
  void should_refuser_403_when_utilisateur_non_admin_R12() throws Exception {
    mvc.perform(get("/api/audit").with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"))
        .andExpect(jsonPath("$.traceId").exists());
    mvc.perform(get("/api/audit/statistiques").with(CseJwt.as(UUID.randomUUID(), Roles.DAF)))
        .andExpect(status().isForbidden());
    verifyNoInteractions(consulter);
  }

  @Test
  void should_lister_journal_pagine_when_admin_R12() throws Exception {
    when(consulter.rechercher(any(), any()))
        .thenReturn(PageResponse.of(List.of(entree()), 0, 50, 1));

    mvc.perform(
            get("/api/audit")
                .param("service", "ms-besoins")
                .param("du", "2026-10-01")
                .param("au", "2026-10-30")
                .param("size", "500")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].action").value("BESOIN_VALIDE"))
        .andExpect(jsonPath("$.content[0].roles[0]").value("RESPONSABLE_MATERIEL"))
        .andExpect(jsonPath("$.totalElements").value(1));

    // Taille bornée à 50, tri par défaut occurredAt décroissant.
    verify(consulter)
        .rechercher(
            argThat(c -> "ms-besoins".equals(c.service()) && c.du() != null),
            argThat(p -> p.size() == 50 && "occurredAt".equals(p.sortField()) && !p.ascending()));
  }

  @Test
  void should_retourner_400_when_tri_non_autorise() throws Exception {
    mvc.perform(
            get("/api/audit")
                .param("sort", "resume,asc")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
  }

  @Test
  void should_retourner_400_when_periode_inversee() throws Exception {
    mvc.perform(
            get("/api/audit")
                .param("du", "2026-10-30")
                .param("au", "2026-10-01")
                .with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PERIODE_INVALIDE"));
  }

  @Test
  void should_retourner_400_when_date_mal_formee() throws Exception {
    mvc.perform(
            get("/api/audit").param("du", "30/10/2026").with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void should_retourner_statistiques_when_admin() throws Exception {
    when(consulter.statistiques())
        .thenReturn(new StatistiquesAudit(12, 2, 7, List.of(new TotalParService("ms-achats", 12))));

    mvc.perform(get("/api/audit/statistiques").with(CseJwt.as(ADMIN, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(12))
        .andExpect(jsonPath("$.aujourdhui").value(2))
        .andExpect(jsonPath("$.septDerniersJours").value(7))
        .andExpect(jsonPath("$.parService[0].service").value("ms-achats"));
  }
}
