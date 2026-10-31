package sn.cse.suivi.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;
import sn.cse.suivi.api.controller.AffectationController;
import sn.cse.suivi.api.controller.ChantierController;
import sn.cse.suivi.api.controller.MaintenanceController;
import sn.cse.suivi.api.controller.MaterielController;
import sn.cse.suivi.api.controller.MaterielOperationsController;
import sn.cse.suivi.api.controller.PanneController;
import sn.cse.suivi.api.controller.TypeEnginController;
import sn.cse.suivi.application.port.in.AffecterMaterielUseCase;
import sn.cse.suivi.application.port.in.ChangerEtatMaterielUseCase;
import sn.cse.suivi.application.port.in.ConfirmerReceptionUseCase;
import sn.cse.suivi.application.port.in.ConsulterAffectationsUseCase;
import sn.cse.suivi.application.port.in.ConsulterCarteUseCase;
import sn.cse.suivi.application.port.in.ConsulterChantiersUseCase;
import sn.cse.suivi.application.port.in.ConsulterMaintenancesUseCase;
import sn.cse.suivi.application.port.in.ConsulterMaterielsUseCase;
import sn.cse.suivi.application.port.in.ConsulterPannesUseCase;
import sn.cse.suivi.application.port.in.GererChantiersUseCase;
import sn.cse.suivi.application.port.in.GererMaintenancesUseCase;
import sn.cse.suivi.application.port.in.GererMaterielsUseCase;
import sn.cse.suivi.application.port.in.GererTypesEnginsUseCase;
import sn.cse.suivi.application.port.in.LibererMaterielUseCase;
import sn.cse.suivi.application.port.in.ReformerMaterielUseCase;
import sn.cse.suivi.application.port.in.ReleverHeuresUseCase;
import sn.cse.suivi.application.port.in.SignalerPanneUseCase;
import sn.cse.suivi.application.port.in.VerifierDisponibiliteUseCase;
import sn.cse.suivi.application.query.AffectationVue;
import sn.cse.suivi.application.query.CarteVue;
import sn.cse.suivi.application.query.ChantierVue;
import sn.cse.suivi.application.query.MaterielVue;
import sn.cse.suivi.domain.model.Affectation;
import sn.cse.suivi.domain.model.CategorieMateriel;
import sn.cse.suivi.domain.model.Chantier;
import sn.cse.suivi.domain.model.Coordonnees;
import sn.cse.suivi.domain.model.DescriptionMateriel;
import sn.cse.suivi.domain.model.DonneesChantier;
import sn.cse.suivi.domain.model.Materiel;
import sn.cse.suivi.domain.model.Periode;
import sn.cse.suivi.domain.service.PolitiqueAffectation;

/** Sécurité des routes (401/403), validation (400) et format ProblemDetail des erreurs métier. */
@WebMvcTest(
    controllers = {
      ChantierController.class,
      MaterielController.class,
      MaterielOperationsController.class,
      AffectationController.class,
      MaintenanceController.class,
      PanneController.class,
      TypeEnginController.class
    })
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class
})
class ApiSecuriteTest {

  private static final UUID ID = UUID.fromString("7b1e5c3a-2d4f-4a6b-8c9d-0e1f2a3b4c5d");
  private static final UUID RM = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final String AFFECTATION =
      "{\"chantierId\":\"c0000000-0000-0000-0000-000000000001\","
          + "\"dateDebut\":\"2026-11-02\",\"dateFinPrevue\":\"2027-02-28\"}";

  @Autowired MockMvc mvc;

  @MockitoBean ConsulterChantiersUseCase consulterChantiers;
  @MockitoBean GererChantiersUseCase gererChantiers;
  @MockitoBean ConsulterMaterielsUseCase consulterMateriels;
  @MockitoBean GererMaterielsUseCase gererMateriels;
  @MockitoBean VerifierDisponibiliteUseCase disponibilite;
  @MockitoBean ConsulterCarteUseCase carte;
  @MockitoBean AffecterMaterielUseCase affecter;
  @MockitoBean ConfirmerReceptionUseCase reception;
  @MockitoBean LibererMaterielUseCase liberer;
  @MockitoBean ReleverHeuresUseCase heures;
  @MockitoBean ChangerEtatMaterielUseCase etat;
  @MockitoBean ReformerMaterielUseCase reforme;
  @MockitoBean ConsulterAffectationsUseCase affectations;
  @MockitoBean GererMaintenancesUseCase maintenances;
  @MockitoBean ConsulterMaintenancesUseCase consulterMaintenances;
  @MockitoBean SignalerPanneUseCase pannes;
  @MockitoBean ConsulterPannesUseCase consulterPannes;
  @MockitoBean GererTypesEnginsUseCase types;

  private static Chantier chantier() {
    return Chantier.ouvrir(
        new DonneesChantier(
            "CH-DKR-01",
            "Extension autoroute Dakar-AIBD",
            "Diamniadio",
            "Dakar",
            new Coordonnees(14.7167, -17.4677),
            new BigDecimal("18500000000"),
            new BigDecimal("21000000000"),
            LocalDate.of(2026, 1, 5),
            null,
            null,
            null));
  }

  private static Materiel materiel() {
    return Materiel.enregistrer(
        new DescriptionMateriel(
            "PEL-CAT320-01",
            null,
            "Pelle hydraulique",
            CategorieMateriel.MATERIEL_ROULANT,
            null,
            "Caterpillar",
            "320 GC",
            new BigDecimal("350000"),
            null,
            null),
        null,
        100,
        Materiel.Origine.AUCUNE,
        LocalDate.of(2026, 10, 30));
  }

  @Test
  void should_return_401_problem_without_token() throws Exception {
    mvc.perform(get("/api/materiels"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    verifyNoInteractions(consulterMateriels);
  }

  @Test
  void should_keep_direction_generale_read_only_R12() throws Exception {
    mvc.perform(
            post("/api/materiels/{id}/affecter", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AFFECTATION))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    mvc.perform(
            patch("/api/maintenances/{id}/demarrer", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE)))
        .andExpect(status().isForbidden());
    verifyNoInteractions(affecter, maintenances);
  }

  @Test
  void should_reserve_state_change_and_reform_request_to_rm() throws Exception {
    mvc.perform(
            patch("/api/materiels/{id}/etat", ID)
                .with(CseJwt.chef(UUID.randomUUID(), UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"etat\":\"EN_MAINTENANCE\",\"motif\":\"Contrôle\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/materiels/{id}/reforme/valider", ID)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approuve\":true}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(etat, reforme);
  }

  @Test
  void should_let_dg_decide_reform_R13() throws Exception {
    Materiel m = materiel();
    when(reforme.decider(eq(ID), anyBoolean(), any(), any()))
        .thenReturn(new MaterielVue(m, null, null, false));

    mvc.perform(
            patch("/api/materiels/{id}/reforme/valider", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approuve\":false,\"commentaire\":\"Réparable\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("PEL-CAT320-01"));
  }

  @Test
  void should_return_400_with_field_errors_when_body_invalid() throws Exception {
    mvc.perform(
            post("/api/materiels/{id}/affecter", ID)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dateDebut\":\"2026-11-02\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(org.hamcrest.Matchers.hasItems("chantierId", "dateFinPrevue")));
  }

  @Test
  void should_translate_business_rule_into_422_problem_R04() throws Exception {
    when(affecter.affecter(any()))
        .thenThrow(new BusinessRuleException("MATERIEL_NON_DISPONIBLE", "Le matériel est affecté"));

    mvc.perform(
            post("/api/materiels/{id}/affecter", ID)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AFFECTATION))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("MATERIEL_NON_DISPONIBLE"))
        .andExpect(jsonPath("$.detail").value("Le matériel est affecté"));
  }

  @Test
  void should_create_affectation_for_rm() throws Exception {
    Materiel m = materiel();
    Chantier c = chantier();
    Affectation a =
        PolitiqueAffectation.affecter(
            m,
            c,
            null,
            new Periode(LocalDate.of(2026, 11, 2), LocalDate.of(2027, 2, 28)),
            List.of());
    when(affecter.affecter(any()))
        .thenReturn(
            new AffectationVue(a, m.getCode(), m.getDesignation(), m.getCategorie(), c.getNom()));

    mvc.perform(
            post("/api/materiels/{id}/affecter", ID)
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(AFFECTATION))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RECEPTION"))
        .andExpect(jsonPath("$.chantierNom").value("Extension autoroute Dakar-AIBD"));
  }

  @Test
  void should_let_chef_confirm_reception_without_body() throws Exception {
    Materiel m = materiel();
    Chantier c = chantier();
    Affectation a =
        PolitiqueAffectation.affecter(
            m,
            c,
            null,
            new Periode(LocalDate.of(2026, 11, 2), LocalDate.of(2027, 2, 28)),
            List.of());
    when(reception.confirmer(any()))
        .thenReturn(
            new AffectationVue(a, m.getCode(), m.getDesignation(), m.getCategorie(), c.getNom()));

    mvc.perform(
            post("/api/materiels/{id}/confirmer-reception", ID)
                .with(CseJwt.chef(UUID.randomUUID(), c.getId())))
        .andExpect(status().isOk());
    mvc.perform(post("/api/materiels/{id}/liberer", ID).with(CseJwt.chef(UUID.randomUUID())))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_list_chantiers_for_any_internal_role_and_reject_unknown_sort() throws Exception {
    when(consulterChantiers.rechercher(any(), any(), any(), any()))
        .thenReturn(PageResponse.of(List.of(new ChantierVue(chantier(), 3)), 0, 20, 1));

    mvc.perform(get("/api/chantiers").with(CseJwt.as(UUID.randomUUID(), Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].code").value("CH-DKR-01"))
        .andExpect(jsonPath("$.content[0].nombreMateriels").value(3))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get("/api/chantiers")
                .param("sort", "budgetSecret,desc")
                .with(CseJwt.as(UUID.randomUUID(), Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
    mvc.perform(get("/api/chantiers").with(CseJwt.as(UUID.randomUUID(), Roles.FOURNISSEUR)))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_require_version_on_update() throws Exception {
    mvc.perform(
            put("/api/chantiers/{id}", ID)
                .with(CseJwt.as(UUID.randomUUID(), Roles.ADMIN_SYSTEME))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"CH-1\",\"nom\":\"N\",\"localisation\":\"L\",\"region\":\"R\","
                        + "\"latitude\":14.7,\"longitude\":-17.4,\"budget\":1,\"montantMarche\":1,"
                        + "\"dateDebut\":\"2026-01-01\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VERSION_OBLIGATOIRE"));
    verify(gererChantiers, org.mockito.Mockito.never()).modifier(any(), any(), anyLong(), any());
  }

  @Test
  void should_reserve_engine_types_writes_to_admin() throws Exception {
    String corps =
        "{\"code\":\"NIVELEUSE\",\"libelle\":\"Niveleuse\",\"categorie\":\"MATERIEL_ROULANT\","
            + "\"seuilMaintenanceHeures\":500,\"actif\":true}";
    mvc.perform(
            post("/api/referentiels/types-engins")
                .with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isForbidden());
    when(types.lister()).thenReturn(List.of());
    mvc.perform(get("/api/referentiels/types-engins").with(CseJwt.chef(UUID.randomUUID())))
        .andExpect(status().isOk());
  }

  @Test
  void should_serve_geojson_map() throws Exception {
    Materiel m = materiel();
    m.localiser(new Coordonnees(14.71, -17.46));
    when(carte.carte(any(), any(), any()))
        .thenReturn(
            new CarteVue(
                List.of(new ChantierVue(chantier(), 1)),
                List.of(new MaterielVue(m, null, null, false))));

    mvc.perform(get("/api/materiels/carte").with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("FeatureCollection"))
        .andExpect(jsonPath("$.features[0].properties.kind").value("CHANTIER"))
        .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(-17.4677))
        .andExpect(jsonPath("$.features[1].properties.kind").value("MATERIEL"))
        .andExpect(jsonPath("$.features[1].properties.etat").value("DISPONIBLE"))
        .andExpect(jsonPath("$.features[1].properties.chantierId").doesNotExist());
  }
}
