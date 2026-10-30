package sn.cse.besoins.api.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.besoins.application.port.in.ConsoliderBesoinsUseCase;
import sn.cse.besoins.application.port.in.ConsulterBesoinsUseCase;
import sn.cse.besoins.application.port.in.ModifierBesoinUseCase;
import sn.cse.besoins.application.port.in.OrienterBesoinUseCase;
import sn.cse.besoins.application.port.in.RejeterBesoinUseCase;
import sn.cse.besoins.application.port.in.SoumettreBesoinUseCase;
import sn.cse.besoins.application.port.in.StatistiquesBesoinsUseCase;
import sn.cse.besoins.application.port.in.ValiderBesoinUseCase;
import sn.cse.besoins.application.query.BesoinVue;
import sn.cse.besoins.application.query.StatistiquesBesoins;
import sn.cse.besoins.domain.exception.BesoinNonModifiableException;
import sn.cse.besoins.domain.model.CategorieMateriel;
import sn.cse.besoins.domain.model.Priorite;
import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;

@WebMvcTest(BesoinController.class)
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class
})
@ActiveProfiles("test")
class BesoinControllerTest {

  private static final UUID ID = UUID.randomUUID();
  private static final UUID CHEF = UUID.randomUUID();
  private static final UUID CHANTIER = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  private static final String CORPS =
      """
      {"chantierId":"c0000000-0000-0000-0000-000000000001","designation":"Pelle hydraulique 20 t",
       "categorie":"MATERIEL_ROULANT","quantite":2,"priorite":"URGENT","dureeUtilisationJours":30,
       "dateBesoin":"2026-11-15","justification":"Chantier à l'arrêt"}
      """;

  @Autowired MockMvc mvc;
  @MockitoBean SoumettreBesoinUseCase soumission;
  @MockitoBean ModifierBesoinUseCase modification;
  @MockitoBean ValiderBesoinUseCase validation;
  @MockitoBean RejeterBesoinUseCase rejet;
  @MockitoBean OrienterBesoinUseCase orientation;
  @MockitoBean ConsulterBesoinsUseCase consultation;
  @MockitoBean ConsoliderBesoinsUseCase consolidation;
  @MockitoBean StatistiquesBesoinsUseCase statistiques;

  private static BesoinVue vue(StatutBesoin statut) {
    return new BesoinVue(
        ID,
        "BES-2026-0001",
        "Pelle hydraulique 20 t",
        CategorieMateriel.MATERIEL_ROULANT,
        null,
        2,
        null,
        Priorite.URGENT,
        statut,
        CHANTIER,
        "Extension autoroute Dakar–AIBD",
        CHEF,
        "Mamadou Diop",
        30,
        LocalDate.parse("2026-11-15"),
        "Chantier à l'arrêt",
        null,
        null,
        null,
        null,
        List.of(),
        false,
        Instant.parse("2026-10-30T09:00:00Z"),
        Instant.parse("2026-10-30T09:00:00Z"),
        0);
  }

  @Test
  void should_repondre_401_en_problem_detail_sans_jeton() throws Exception {
    mvc.perform(get("/api/besoins"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header()
                .string(
                    "Content-Type",
                    org.hamcrest.Matchers.containsString("application/problem+json")))
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void should_creer_un_besoin_pour_un_chef_de_chantier() throws Exception {
    when(soumission.soumettre(any())).thenReturn(vue(StatutBesoin.EN_ATTENTE));

    mvc.perform(
            post("/api/besoins")
                .with(CseJwt.chef(CHEF, CHANTIER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/besoins/" + ID)))
        .andExpect(jsonPath("$.reference").value("BES-2026-0001"))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
        .andExpect(jsonPath("$.materielIdsAffectes").isArray())
        .andExpect(jsonPath("$.orientationEnAttente").value(false));
  }

  @Test
  void should_repondre_403_when_role_non_demandeur() throws Exception {
    mvc.perform(
            post("/api/besoins")
                .with(CseJwt.as(UUID.randomUUID(), Roles.DAF))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    verifyNoInteractions(soumission);
  }

  @Test
  void should_repondre_400_avec_les_champs_invalides() throws Exception {
    mvc.perform(
            post("/api/besoins")
                .with(CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"designation\":\"\",\"quantite\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION"))
        .andExpect(jsonPath("$.errors[?(@.field == 'chantierId')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'quantite')]").exists());
  }

  @Test
  void should_repondre_422_when_justification_absente_R08() throws Exception {
    when(soumission.soumettre(any()))
        .thenThrow(
            new BusinessRuleException("JUSTIFICATION_OBLIGATOIRE", "Justification obligatoire"));

    mvc.perform(
            post("/api/besoins")
                .with(CseJwt.chef(CHEF, CHANTIER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPS))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("JUSTIFICATION_OBLIGATOIRE"))
        .andExpect(jsonPath("$.detail").value("Justification obligatoire"));
  }

  @Test
  void should_repondre_409_when_besoin_non_modifiable_R05() throws Exception {
    when(modification.modifier(any()))
        .thenThrow(new BesoinNonModifiableException("BES-2026-0001", StatutBesoin.VALIDE));
    String corps = CORPS.replace("}", ",\"version\":0}");

    mvc.perform(
            put("/api/besoins/{id}", ID)
                .with(CseJwt.chef(CHEF, CHANTIER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BESOIN_NON_MODIFIABLE"));
  }

  @Test
  void should_reserver_la_validation_au_responsable_materiel_R01() throws Exception {
    mvc.perform(patch("/api/besoins/{id}/valider", ID).with(CseJwt.chef(CHEF, CHANTIER)))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/besoins/{id}/rejeter", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"x\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/besoins/{id}/orienter", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden());
    verifyNoInteractions(validation, rejet, orientation);
  }

  @Test
  void should_valider_sans_corps_pour_le_responsable_materiel_R01() throws Exception {
    when(validation.valider(any())).thenReturn(vue(StatutBesoin.AFFECTE_DIRECT));

    mvc.perform(
            patch("/api/besoins/{id}/valider", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("AFFECTE_DIRECT"));
  }

  @Test
  void should_rejeter_et_orienter_pour_le_responsable_materiel() throws Exception {
    when(rejet.rejeter(any())).thenReturn(vue(StatutBesoin.REJETE));
    when(orientation.orienter(any())).thenReturn(vue(StatutBesoin.EN_ACHAT));
    var rm = CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_MATERIEL);

    mvc.perform(
            patch("/api/besoins/{id}/rejeter", ID)
                .with(rm)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"Doublon\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("REJETE"));
    mvc.perform(
            post("/api/besoins/{id}/orienter", ID)
                .with(rm)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"materielIds\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_ACHAT"));
  }

  @Test
  void should_lister_avec_pagination_et_refuser_un_tri_non_autorise() throws Exception {
    when(consultation.rechercher(any(), any(), any()))
        .thenReturn(PageResponse.of(List.of(vue(StatutBesoin.EN_ATTENTE)), 0, 20, 1));
    var ra = CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_ACHATS);

    mvc.perform(get("/api/besoins").param("statut", "EN_ATTENTE").param("size", "500").with(ra))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].reference").value("BES-2026-0001"))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/besoins").param("sort", "motifRejet,asc").with(ra))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
    mvc.perform(get("/api/besoins").with(CseJwt.as(UUID.randomUUID(), Roles.DAF)))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_exposer_detail_historique_consolidation_et_statistiques() throws Exception {
    when(consultation.obtenir(eq(ID), any())).thenReturn(vue(StatutBesoin.VALIDE));
    when(consultation.historique(eq(ID), any())).thenReturn(List.of());
    when(consolidation.consolider(any())).thenReturn(List.of());
    when(statistiques.statistiques(any()))
        .thenReturn(
            new StatistiquesBesoins(4, Map.of(StatutBesoin.EN_ATTENTE, 4L), Map.of(), null));
    var dg = CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE);

    mvc.perform(get("/api/besoins/{id}", ID).with(dg))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("VALIDE"));
    mvc.perform(get("/api/besoins/{id}/historique", ID).with(dg)).andExpect(status().isOk());
    mvc.perform(get("/api/besoins/consolidation").with(dg)).andExpect(status().isForbidden());
    mvc.perform(
            get("/api/besoins/consolidation")
                .param("statut", "EN_ATTENTE")
                .with(CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/besoins/statistiques").with(CseJwt.as(UUID.randomUUID(), Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(4))
        .andExpect(jsonPath("$.parStatut.EN_ATTENTE").value(4))
        .andExpect(jsonPath("$.parStatut.REJETE").value(0))
        .andExpect(jsonPath("$.parPriorite.CRITIQUE").value(0));
    mvc.perform(get("/api/besoins/{id}", "pas-un-uuid").with(dg))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
    mvc.perform(
            get("/api/besoins/statistiques").with(CseJwt.as(UUID.randomUUID(), Roles.FOURNISSEUR)))
        .andExpect(status().isForbidden());
  }
}
