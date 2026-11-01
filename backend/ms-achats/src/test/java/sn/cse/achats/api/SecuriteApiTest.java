package sn.cse.achats.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.achats.Donnees;
import sn.cse.achats.api.controller.AppelOffreController;
import sn.cse.achats.api.controller.BesoinsAAcheterController;
import sn.cse.achats.api.controller.ContratController;
import sn.cse.achats.api.controller.EngagementController;
import sn.cse.achats.api.controller.FactureController;
import sn.cse.achats.api.controller.FinanceController;
import sn.cse.achats.api.controller.PortailFournisseurController;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.port.in.AttribuerMarcheUseCase;
import sn.cse.achats.application.port.in.CloturerAppelOffreUseCase;
import sn.cse.achats.application.port.in.ConsulterAppelsOffresUseCase;
import sn.cse.achats.application.port.in.ConsulterContratsUseCase;
import sn.cse.achats.application.port.in.ConsulterFinanceUseCase;
import sn.cse.achats.application.port.in.ContratsPortailUseCase;
import sn.cse.achats.application.port.in.DeposerOffreUseCase;
import sn.cse.achats.application.port.in.DocumentPdf;
import sn.cse.achats.application.port.in.EvaluerOffresUseCase;
import sn.cse.achats.application.port.in.ExecuterContratUseCase;
import sn.cse.achats.application.port.in.GenererContratUseCase;
import sn.cse.achats.application.port.in.GenererProcesVerbalUseCase;
import sn.cse.achats.application.port.in.GererFacturesUseCase;
import sn.cse.achats.application.port.in.MettreAJourProjectionsUseCase;
import sn.cse.achats.application.port.in.OffreVue;
import sn.cse.achats.application.port.in.PortailFournisseurUseCase;
import sn.cse.achats.application.port.in.PublierAppelOffreUseCase;
import sn.cse.achats.application.port.in.ValiderEngagementUseCase;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.TypeProcesVerbal;
import sn.cse.achats.domain.service.OffreEvaluee;
import sn.cse.achats.domain.service.SyntheseFinance;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;

/** Contrôle d'accès (401/403), validation (400) et règles (422) au format ProblemDetail. */
@ActiveProfiles("test")
@WebMvcTest(
    controllers = {
      AppelOffreController.class,
      BesoinsAAcheterController.class,
      ContratController.class,
      EngagementController.class,
      FactureController.class,
      FinanceController.class,
      PortailFournisseurController.class
    })
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class,
  OAuth2ResourceServerAutoConfiguration.class
})
@Import(AchatsApiMapper.class)
class SecuriteApiTest {

  private static final UUID UTILISATEUR = UUID.fromString("00000000-0000-0000-0000-000000000301");
  private static final UUID ID = UUID.fromString("d0000000-0000-0000-0000-000000000001");
  private static final String PROBLEME = "application/problem+json";

  @Autowired MockMvc mvc;

  @MockitoBean PublierAppelOffreUseCase publication;
  @MockitoBean DeposerOffreUseCase depot;
  @MockitoBean CloturerAppelOffreUseCase cloture;
  @MockitoBean EvaluerOffresUseCase evaluation;
  @MockitoBean AttribuerMarcheUseCase attribution;
  @MockitoBean ConsulterAppelsOffresUseCase consultation;
  @MockitoBean GenererProcesVerbalUseCase procesVerbaux;
  @MockitoBean ConsulterContratsUseCase contrats;
  @MockitoBean GenererContratUseCase generation;
  @MockitoBean ExecuterContratUseCase execution;
  @MockitoBean ValiderEngagementUseCase engagements;
  @MockitoBean GererFacturesUseCase factures;
  @MockitoBean ConsulterFinanceUseCase finance;
  @MockitoBean PortailFournisseurUseCase portail;
  @MockitoBean ContratsPortailUseCase contratsPortail;
  @MockitoBean MettreAJourProjectionsUseCase projections;

  private static String json(String corps) {
    return corps.replace('\'', '"');
  }

  @Test
  void should_repondre_401_problem_detail_sans_jeton() throws Exception {
    mvc.perform(get("/api/appels-offres"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @Test
  void should_repondre_403_when_role_insuffisant() throws Exception {
    mvc.perform(get("/api/appels-offres").with(CseJwt.chef(UTILISATEUR, UUID.randomUUID())))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    mvc.perform(get("/api/appels-offres/{id}/offres", ID).with(CseJwt.as(UTILISATEUR, Roles.DAF)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/besoins-a-acheter").with(CseJwt.as(UTILISATEUR, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/factures").with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/finance/synthese").with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/portail/appels-offres")
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/appels-offres/{id}/offres/{o}/evaluation", ID, ID)
                .with(CseJwt.as(UTILISATEUR, Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_reserver_la_publication_au_responsable_achats() throws Exception {
    mvc.perform(
            post("/api/appels-offres")
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        "{'objet':'Pelles','type':'APPEL_OFFRES','categorie':'MATERIEL_ROULANT','quantite':2,"
                            + "'dateLimite':'2026-11-20T17:00:00Z','criteres':[{'code':'PRIX','poids':100}]}")))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_repondre_400_avec_erreurs_par_champ() throws Exception {
    mvc.perform(
            post("/api/appels-offres")
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("{'objet':'','quantite':0,'criteres':[]}")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("VALIDATION"))
        .andExpect(jsonPath("$.errors[?(@.field=='objet')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field=='dateLimite')]").exists());
  }

  @Test
  void should_repondre_400_when_tri_non_autorise_ou_identifiant_invalide() throws Exception {
    mvc.perform(get("/api/contrats?sort=montant,desc").with(CseJwt.as(UTILISATEUR, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
    mvc.perform(get("/api/appels-offres/pas-un-uuid").with(CseJwt.as(UTILISATEUR, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
  }

  @Test
  void should_repondre_422_when_offre_apres_date_limite_R02() throws Exception {
    when(depot.saisirOffre(any()))
        .thenThrow(
            new BusinessRuleException(
                CodesErreur.DATE_LIMITE_DEPASSEE,
                "La date limite de dépôt des offres est dépassée (R02)."));

    mvc.perform(
            post("/api/appels-offres/{id}/offres", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        "{'fournisseurId':'f0000000-0000-0000-0000-000000000001','montantPropose':298000000,"
                            + "'delaiLivraisonJours':60}")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("DATE_LIMITE_DEPASSEE"))
        .andExpect(
            jsonPath("$.detail").value("La date limite de dépôt des offres est dépassée (R02)."));
  }

  @Test
  void should_repondre_422_when_livraison_sans_engagement_R06() throws Exception {
    when(execution.demarrerLivraison(ID))
        .thenThrow(
            new BusinessRuleException(
                CodesErreur.ENGAGEMENT_NON_VALIDE, "Engagement non validé (R06)."));

    mvc.perform(
            post("/api/contrats/{id}/livraison/demarrer", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ENGAGEMENT_NON_VALIDE"));
  }

  @Test
  void should_masquer_identite_des_soumissionnaires_when_ao_publie_R10() throws Exception {
    AppelOffre ao = Donnees.aoPublie();
    var offre = ao.deposerOffre(Donnees.F1, new BigDecimal("298000000"), 60, 24, null, Donnees.T0);
    when(consultation.offres(ID))
        .thenReturn(List.of(new OffreVue(ID, true, OffreEvaluee.sansNote(offre))));

    mvc.perform(
            get("/api/appels-offres/{id}/offres", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].fournisseurId").value(nullValue()))
        .andExpect(jsonPath("$[0].fournisseurNom").value(nullValue()))
        .andExpect(jsonPath("$[0].codeAnonyme").value(offre.getCodeAnonyme()))
        .andExpect(jsonPath("$[0].montantPropose").value(298000000));
  }

  @Test
  void should_reserver_co_validation_au_rm_et_approbation_a_la_dg_R11() throws Exception {
    AppelOffre ao = Donnees.aoAttribue();
    when(attribution.coValider(any())).thenReturn(ao);
    when(attribution.approuver(any())).thenReturn(ao);
    String decision = json("{'approuve':true,'commentaire':'Conforme'}");

    mvc.perform(
            post("/api/appels-offres/{id}/attribution/co-valider", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/appels-offres/{id}/attribution/co-valider", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("ATTRIBUE"))
        .andExpect(jsonPath("$.attribution.coValidationMateriel.statut").value("APPROUVEE"));
    mvc.perform(
            post("/api/appels-offres/{id}/attribution/approuver", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/appels-offres/{id}/attribution/approuver", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/appels-offres/{id}/attribution/approuver", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_reserver_engagement_a_la_daf_R06() throws Exception {
    ContratAchat contrat = Donnees.contratBrouillon();
    when(engagements.decider(any())).thenReturn(contrat);
    String decision = json("{'approuve':true}");

    mvc.perform(
            post("/api/engagements/{id}/valider", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/engagements/{id}/valider", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.DAF))
                .contentType(MediaType.APPLICATION_JSON)
                .content(decision))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.numero").value("CTR-2026-001"))
        .andExpect(jsonPath("$.montant").value(120000000));
  }

  @Test
  void should_autoriser_lectures_selon_matrice() throws Exception {
    when(finance.synthese())
        .thenReturn(
            new SyntheseFinance(
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.ONE,
                2,
                1));
    when(consultation.rechercher(any(), any()))
        .thenReturn(PageResponse.of(List.of(Donnees.aoPublie()), 0, 20, 1));
    when(portail.appelsOffresOuverts(any(), any()))
        .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

    mvc.perform(get("/api/finance/synthese").with(CseJwt.as(UTILISATEUR, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.facturesEnRetard").value(2));
    mvc.perform(get("/api/appels-offres?statut=PUBLIE").with(CseJwt.as(UTILISATEUR, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].reference").value("AO-2026-001"))
        .andExpect(jsonPath("$.content[0].criteres", hasSize(3)))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get("/api/portail/appels-offres")
                .with(
                    CseJwt.avecClaims(
                        UTILISATEUR,
                        Map.of("fournisseur_id", Donnees.F1.id().toString()),
                        Roles.FOURNISSEUR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(0)));
  }

  @Test
  void should_servir_proces_verbal_en_pdf() throws Exception {
    when(procesVerbaux.generer(eq(ID), eq(TypeProcesVerbal.OUVERTURE)))
        .thenReturn(new DocumentPdf("PV-ouverture-AO-2026-001.pdf", "%PDF-1.4".getBytes()));

    mvc.perform(
            get("/api/appels-offres/{id}/proces-verbaux/OUVERTURE", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(
            header()
                .string(
                    "Content-Disposition", "inline; filename=\"PV-ouverture-AO-2026-001.pdf\""));
  }

  @Test
  void should_transmettre_notes_saisies_a_l_evaluation() throws Exception {
    AppelOffre ao = Donnees.aoCloture("100", "110");
    var offre = ao.getOffres().get(0);
    when(evaluation.evaluer(any()))
        .thenReturn(new OffreVue(ao.getId(), false, OffreEvaluee.sansNote(offre)));

    mvc.perform(
            put("/api/appels-offres/{id}/offres/{o}/evaluation", ID, offre.getId())
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("{'QUALITE_TECHNIQUE':80,'SAV':65}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fournisseurNom").value("Sahel Engins SA"));
    mvc.perform(
            put("/api/appels-offres/{id}/offres/{o}/evaluation", ID, offre.getId())
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("{'GARANTIE':140}")))
        .andExpect(status().isBadRequest());
  }
}
