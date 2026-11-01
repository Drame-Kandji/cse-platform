package sn.cse.achats.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import sn.cse.achats.Donnees;
import sn.cse.achats.api.controller.ContratController;
import sn.cse.achats.api.controller.PortailFournisseurController;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.port.in.ConsulterContratsUseCase;
import sn.cse.achats.application.port.in.ContratsPortailUseCase;
import sn.cse.achats.application.port.in.ExecuterContratUseCase;
import sn.cse.achats.application.port.in.GenererContratUseCase;
import sn.cse.achats.application.port.in.PortailFournisseurUseCase;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.achats.domain.model.AppelOffre;
import sn.cse.achats.domain.model.ContratAchat;
import sn.cse.achats.domain.model.StatutContrat;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.exception.ResourceNotFoundException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;

/**
 * Portail fournisseur, volet contrats (API_CONTRACT §13) : 401 sans jeton, 403 pour les rôles
 * internes, 404 pour la ressource d'un autre fournisseur, validation et règles au format
 * ProblemDetail ; signature du CSE côté RA.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = {PortailFournisseurController.class, ContratController.class})
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class,
  OAuth2ResourceServerAutoConfiguration.class
})
@Import(AchatsApiMapper.class)
class PortailContratsApiTest {

  private static final UUID UTILISATEUR = UUID.fromString("00000000-0000-0000-0000-000000000701");
  private static final UUID ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
  private static final String PROBLEME = "application/problem+json";
  private static final String MOTIF = "{\"motif\":\"Pénalités trop élevées\"}";
  private static final String EXPEDITION = "{\"dateExpedition\":\"2026-10-20\"}";
  private static final String JALON = "{\"dateReelle\":\"2026-10-21\"}";

  @Autowired MockMvc mvc;

  @MockitoBean PortailFournisseurUseCase portail;
  @MockitoBean ContratsPortailUseCase contrats;
  @MockitoBean ConsulterContratsUseCase consultation;
  @MockitoBean GenererContratUseCase generation;
  @MockitoBean ExecuterContratUseCase execution;

  private static RequestPostProcessor fournisseur() {
    return CseJwt.avecClaims(
        UTILISATEUR, Map.of("fournisseur_id", Donnees.F1.id().toString()), Roles.FOURNISSEUR);
  }

  private static MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder requete, String corps) {
    return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
  }

  /** Toutes les nouvelles routes du portail, avec un corps valide. */
  static Stream<Arguments> routes() {
    return Stream.of(
        Arguments.of("GET liste", get("/api/portail/contrats")),
        Arguments.of("GET détail", get("/api/portail/contrats/{id}", ID)),
        Arguments.of("POST signer", post("/api/portail/contrats/{id}/signer", ID)),
        Arguments.of("POST refuser", json(post("/api/portail/contrats/{id}/refuser", ID), MOTIF)),
        Arguments.of(
            "POST expedier", json(post("/api/portail/contrats/{id}/expedier", ID), EXPEDITION)),
        Arguments.of(
            "PATCH jalon", json(patch("/api/portail/contrats/{id}/jalons/{j}", ID, ID), JALON)),
        Arguments.of("GET appel d'offres", get("/api/portail/appels-offres/{id}", ID)));
  }

  static Stream<Arguments> routesEtRolesInternes() {
    // Une requête neuve par rôle : un constructeur de requête accumule ses post-processeurs.
    return Stream.of(
            Roles.RESPONSABLE_ACHATS,
            Roles.DAF,
            Roles.RESPONSABLE_MATERIEL,
            Roles.DIRECTION_GENERALE,
            Roles.ADMIN_SYSTEME)
        .flatMap(role -> routes().map(r -> Arguments.of(r.get()[0], r.get()[1], role)));
  }

  private static ContratAchat enAttenteSignature() {
    ContratAchat c = Donnees.contratBrouillon();
    c.signerPourLeCse(Donnees.RA, Instant.parse("2026-10-14T10:00:00Z"));
    return c;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("routes")
  void should_repondre_401_sans_jeton(String nom, MockHttpServletRequestBuilder requete)
      throws Exception {
    mvc.perform(requete)
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @ParameterizedTest(name = "{0} avec {2}")
  @MethodSource("routesEtRolesInternes")
  void should_repondre_403_pour_les_roles_internes(
      String nom, MockHttpServletRequestBuilder requete, String role) throws Exception {
    mvc.perform(requete.with(CseJwt.as(UTILISATEUR, role)))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    verifyNoInteractions(contrats, portail);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("routes")
  void should_repondre_404_pour_la_ressource_d_un_autre_fournisseur(
      String nom, MockHttpServletRequestBuilder requete) throws Exception {
    ResourceNotFoundException introuvable = new ResourceNotFoundException("Contrat", ID);
    when(contrats.contrats(any(), any(), any())).thenThrow(introuvable);
    when(contrats.contrat(any(), eq(ID))).thenThrow(introuvable);
    when(contrats.signer(any(), eq(ID))).thenThrow(introuvable);
    when(contrats.refuser(any(), eq(ID), any())).thenThrow(introuvable);
    when(contrats.expedier(any(), eq(ID), any(), any())).thenThrow(introuvable);
    when(contrats.enregistrerJalon(any(), eq(ID), eq(ID), any())).thenThrow(introuvable);
    when(portail.appelOffre(any(), eq(ID)))
        .thenThrow(new ResourceNotFoundException("AppelOffre", ID));

    mvc.perform(requete.with(fournisseur()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").exists());
  }

  @Test
  void should_lister_les_contrats_du_fournisseur_filtres_par_statut() throws Exception {
    when(contrats.contrats(any(), eq(StatutContrat.EN_ATTENTE_SIGNATURE), any()))
        .thenReturn(PageResponse.of(List.of(enAttenteSignature()), 0, 20, 1));

    mvc.perform(get("/api/portail/contrats?statut=EN_ATTENTE_SIGNATURE").with(fournisseur()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].statut").value("EN_ATTENTE_SIGNATURE"))
        .andExpect(jsonPath("$.content[0].signatureCse.parNom").value("Fatou Sarr"))
        .andExpect(jsonPath("$.content[0].signatureFournisseur").doesNotExist());
    mvc.perform(get("/api/portail/contrats?sort=montant,desc").with(fournisseur()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
  }

  @Test
  void should_signer_pour_le_fournisseur() throws Exception {
    ContratAchat signe = enAttenteSignature();
    signe.signerPourLeFournisseur(Donnees.REPRESENTANT_F1, Instant.parse("2026-10-15T10:00:00Z"));
    when(contrats.signer(any(), eq(ID))).thenReturn(signe);

    mvc.perform(post("/api/portail/contrats/{id}/signer", ID).with(fournisseur()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("SIGNE"))
        .andExpect(jsonPath("$.signatureFournisseur.parNom").value("Moussa Kane"))
        .andExpect(jsonPath("$.signatureFournisseur.date").value("2026-10-15T10:00:00Z"))
        .andExpect(jsonPath("$.engagement.statut").value("EN_ATTENTE"));
  }

  @Test
  void should_repondre_409_when_signature_hors_attente() throws Exception {
    when(contrats.signer(any(), eq(ID)))
        .thenThrow(
            new ConflictException(
                CodesErreur.TRANSITION_INTERDITE,
                "Transition interdite du contrat : SIGNE → SIGNE."));

    mvc.perform(post("/api/portail/contrats/{id}/signer", ID).with(fournisseur()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRANSITION_INTERDITE"));
  }

  @Test
  void should_exiger_un_motif_pour_refuser() throws Exception {
    ContratAchat refuse = enAttenteSignature();
    refuse.refuserSignature("Pénalités trop élevées");
    when(contrats.refuser(any(), eq(ID), eq("Pénalités trop élevées"))).thenReturn(refuse);

    mvc.perform(json(post("/api/portail/contrats/{id}/refuser", ID), "{}").with(fournisseur()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.errors[?(@.field=='motif')]").exists());
    mvc.perform(json(post("/api/portail/contrats/{id}/refuser", ID), MOTIF).with(fournisseur()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("BROUILLON"))
        .andExpect(jsonPath("$.motifRefusSignature").value("Pénalités trop élevées"))
        .andExpect(jsonPath("$.signatureCse").doesNotExist());
  }

  @Test
  void should_declarer_l_expedition_et_appliquer_R06() throws Exception {
    ContratAchat expedie =
        Donnees.signer(Donnees.contratBrouillon(), Instant.parse("2026-10-15T10:00:00Z"));
    expedie.deciderEngagement(true, null, Donnees.DAF, Instant.parse("2026-10-16T10:00:00Z"));
    expedie.declarerExpedition(
        LocalDate.of(2026, 10, 20), "Porte-char", LocalDate.of(2026, 10, 20));
    when(contrats.expedier(any(), eq(ID), eq(LocalDate.of(2026, 10, 20)), isNull()))
        .thenReturn(expedie);

    mvc.perform(json(post("/api/portail/contrats/{id}/expedier", ID), "{}").with(fournisseur()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field=='dateExpedition')]").exists());
    mvc.perform(
            json(post("/api/portail/contrats/{id}/expedier", ID), EXPEDITION).with(fournisseur()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_LIVRAISON"))
        .andExpect(jsonPath("$.dateExpedition").value("2026-10-20"))
        .andExpect(jsonPath("$.commentaireExpedition").value("Porte-char"));

    when(contrats.expedier(any(), eq(ID), any(), any()))
        .thenThrow(
            new BusinessRuleException(
                CodesErreur.ENGAGEMENT_NON_VALIDE, "Engagement non validé (R06)."));
    mvc.perform(
            json(post("/api/portail/contrats/{id}/expedier", ID), EXPEDITION).with(fournisseur()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(PROBLEME))
        .andExpect(jsonPath("$.code").value("ENGAGEMENT_NON_VALIDE"));
  }

  @Test
  void should_renseigner_un_jalon() throws Exception {
    when(contrats.enregistrerJalon(any(), eq(ID), eq(ID), eq(LocalDate.of(2026, 10, 21))))
        .thenReturn(enAttenteSignature());

    mvc.perform(
            json(patch("/api/portail/contrats/{id}/jalons/{j}", ID, ID), JALON).with(fournisseur()))
        .andExpect(status().isOk());
    mvc.perform(
            json(patch("/api/portail/contrats/{id}/jalons/{j}", ID, ID), "{}").with(fournisseur()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void should_exposer_l_appel_d_offres_avec_l_offre_du_fournisseur() throws Exception {
    AppelOffre ao = Donnees.aoPublie();
    var offre = ao.deposerOffre(Donnees.F1, new BigDecimal("298000000"), 60, 24, null, Donnees.T0);
    when(portail.appelOffre(any(), eq(ID))).thenReturn(ao);

    mvc.perform(get("/api/portail/appels-offres/{id}", ID).with(fournisseur()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reference").value("AO-2026-001"))
        .andExpect(jsonPath("$.invite").value(true))
        .andExpect(jsonPath("$.offreDeposee").value(true))
        .andExpect(jsonPath("$.monOffreId").value(offre.getId().toString()))
        .andExpect(jsonPath("$.chantierNom").value("Extension autoroute Dakar-AIBD"))
        .andExpect(jsonPath("$.budgetEstime").doesNotExist());
  }

  @Test
  void should_reserver_la_signature_du_cse_au_responsable_achats() throws Exception {
    when(execution.signer(eq(ID), any())).thenReturn(enAttenteSignature());

    mvc.perform(post("/api/contrats/{id}/signer", ID).with(fournisseur()))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/contrats/{id}/signer", ID).with(CseJwt.as(UTILISATEUR, Roles.DAF)))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/contrats/{id}/signer", ID)
                .with(CseJwt.as(UTILISATEUR, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_SIGNATURE"))
        .andExpect(jsonPath("$.signatureCse.parNom").value("Fatou Sarr"))
        .andExpect(jsonPath("$.engagement").doesNotExist());
    verify(execution).signer(eq(ID), any());
  }
}
