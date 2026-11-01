package sn.cse.fournisseurs.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.config.CseCoreAutoConfiguration;
import sn.cse.common.exception.BusinessRuleException;
import sn.cse.common.exception.ConflictException;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.security.CseSecurityAutoConfiguration;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.web.CseWebAutoConfiguration;
import sn.cse.fournisseurs.api.controller.CatalogueController;
import sn.cse.fournisseurs.api.controller.EvaluationController;
import sn.cse.fournisseurs.api.controller.FournisseurController;
import sn.cse.fournisseurs.api.controller.InscriptionController;
import sn.cse.fournisseurs.api.controller.LitigeController;
import sn.cse.fournisseurs.api.controller.PortailController;
import sn.cse.fournisseurs.application.port.in.EvaluerFournisseurUseCase;
import sn.cse.fournisseurs.application.port.in.GererCatalogueUseCase;
import sn.cse.fournisseurs.application.port.in.GererCycleVieUseCase;
import sn.cse.fournisseurs.application.port.in.GererLitigesUseCase;
import sn.cse.fournisseurs.application.port.in.GererReferentielUseCase;
import sn.cse.fournisseurs.application.port.in.InscrireFournisseurUseCase;
import sn.cse.fournisseurs.application.port.in.PortailFournisseurUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.application.port.out.LitigeRepository;
import sn.cse.fournisseurs.application.query.FournisseurVue;
import sn.cse.fournisseurs.application.service.ChargeurFournisseur;
import sn.cse.fournisseurs.application.service.ConsultationService;
import sn.cse.fournisseurs.domain.exception.CodesErreur;
import sn.cse.fournisseurs.domain.exception.TransitionInterditeException;
import sn.cse.fournisseurs.domain.model.Auteur;
import sn.cse.fournisseurs.domain.model.CategorieMateriel;
import sn.cse.fournisseurs.domain.model.DonneesLigneCatalogue;
import sn.cse.fournisseurs.domain.model.Evaluation;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.domain.model.GraviteLitige;
import sn.cse.fournisseurs.domain.model.LigneCatalogue;
import sn.cse.fournisseurs.domain.model.Litige;
import sn.cse.fournisseurs.domain.model.NotesEvaluation;
import sn.cse.fournisseurs.domain.model.StatutFournisseur;
import sn.cse.fournisseurs.support.Fixtures;

/** Sécurité et format d'erreur de la couche REST (contrôleurs réels, cas d'utilisation simulés). */
@WebMvcTest(
    controllers = {
      FournisseurController.class,
      EvaluationController.class,
      CatalogueController.class,
      LitigeController.class,
      PortailController.class,
      InscriptionController.class
    })
@ImportAutoConfiguration({
  CseCoreAutoConfiguration.class,
  CseSecurityAutoConfiguration.class,
  CseWebAutoConfiguration.class
})
@Import({ConsultationService.class, ChargeurFournisseur.class})
@ActiveProfiles("test")
class ApiSecuriteTest {

  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000999");
  private static final String PROBLEM = "application/problem+json";

  @Autowired private MockMvc mvc;

  @MockitoBean private GererReferentielUseCase referentiel;
  @MockitoBean private GererCycleVieUseCase cycleVie;
  @MockitoBean private EvaluerFournisseurUseCase evaluations;
  @MockitoBean private GererCatalogueUseCase catalogue;
  @MockitoBean private GererLitigesUseCase litiges;
  @MockitoBean private FournisseurRepository fournisseurs;
  @MockitoBean private LitigeRepository litigeRepository;
  @MockitoBean private PortailFournisseurUseCase portail;
  @MockitoBean private InscrireFournisseurUseCase inscription;

  private Fournisseur fournisseurConnu() {
    Fournisseur f = Fixtures.actif();
    when(fournisseurs.parId(f.getId())).thenReturn(Optional.of(f));
    when(litigeRepository.compterParFournisseur(anyList())).thenReturn(Map.of());
    return f;
  }

  @Test
  void should_repondre_401_problem_detail_when_sans_jeton() throws Exception {
    mvc.perform(get("/api/fournisseurs"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM))
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @Test
  void should_repondre_403_when_chef_de_chantier_consulte() throws Exception {
    mvc.perform(get("/api/fournisseurs").with(CseJwt.as(USER, Roles.CHEF_CHANTIER)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }

  @Test
  void should_repondre_403_when_rm_cree_un_fournisseur() throws Exception {
    mvc.perform(
            post("/api/fournisseurs")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsFournisseurValide()))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_repondre_400_avec_erreurs_par_champ_when_corps_invalide() throws Exception {
    mvc.perform(
            post("/api/fournisseurs")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"ninea\":\"abc\",\"pays\":\"Sénégal\",\"email\":\"pas-un-email\",\"specialites\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM))
        .andExpect(jsonPath("$.code").value("VALIDATION"))
        .andExpect(jsonPath("$.errors[?(@.field == 'raisonSociale')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'ninea')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'specialites')]").exists());
  }

  @Test
  void should_creer_fournisseur_when_ra() throws Exception {
    Fournisseur f = Fixtures.actif();
    when(referentiel.creer(any(), any()))
        .thenReturn(new FournisseurVue(f, 0, Fixtures.AUJOURDHUI, true));
    mvc.perform(
            post("/api/fournisseurs")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsFournisseurValide()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/fournisseurs/" + f.getId()))
        .andExpect(jsonPath("$.statut").value("ACTIF"))
        .andExpect(jsonPath("$.coordonneesBancaires.iban").value("SN08SN0100152000048500003035"));
  }

  @Test
  void should_exiger_version_when_modification() throws Exception {
    mvc.perform(
            put("/api/fournisseurs/" + UUID.randomUUID())
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsFournisseurValide()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'version')]").exists());
  }

  @Test
  void should_masquer_coordonnees_bancaires_when_responsable_materiel() throws Exception {
    Fournisseur f = fournisseurConnu();
    mvc.perform(
            get("/api/fournisseurs/" + f.getId()).with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.raisonSociale").value("Sahel Engins SA"))
        .andExpect(jsonPath("$.coordonneesBancaires").value(nullValue()));
    mvc.perform(
            get("/api/fournisseurs/" + f.getId()).with(CseJwt.as(USER, Roles.DIRECTION_GENERALE)))
        .andExpect(jsonPath("$.coordonneesBancaires").value(nullValue()));
  }

  @Test
  void should_afficher_coordonnees_bancaires_when_daf_ou_ra() throws Exception {
    Fournisseur f = fournisseurConnu();
    mvc.perform(get("/api/fournisseurs/" + f.getId()).with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coordonneesBancaires.banque").value("Banque Sahélienne"));
    mvc.perform(
            get("/api/fournisseurs/" + f.getId()).with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.coordonneesBancaires.iban").value("SN08SN0100152000048500003035"));
  }

  @Test
  void should_repondre_404_when_fournisseur_inconnu() throws Exception {
    UUID id = UUID.randomUUID();
    when(fournisseurs.parId(id)).thenReturn(Optional.empty());
    mvc.perform(get("/api/fournisseurs/" + id).with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("FOURNISSEUR_INTROUVABLE"));
  }

  @Test
  void should_repondre_400_when_identifiant_invalide() throws Exception {
    mvc.perform(get("/api/fournisseurs/pas-un-uuid").with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PARAMETRE_INVALIDE"));
  }

  @Test
  void should_lister_avec_pagination_et_refuser_tri_inconnu() throws Exception {
    Fournisseur f = Fixtures.actif();
    when(fournisseurs.rechercher(any(), any())).thenReturn(PageResponse.of(List.of(f), 0, 20, 1));
    when(litigeRepository.compterParFournisseur(anyList())).thenReturn(Map.of());
    mvc.perform(
            get("/api/fournisseurs?statut=ACTIF&specialite=MATERIEL_ROULANT&sort=scorePerformance,desc")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].scorePerformance").value(50.0))
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get("/api/fournisseurs?sort=ninea,asc")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TRI_NON_AUTORISE"));
  }

  @Test
  void should_reserver_la_decision_de_blacklistage_a_la_dg_R09() throws Exception {
    UUID id = UUID.randomUUID();
    String corps = "{\"approuve\":true,\"commentaire\":\"Confirmé\"}";
    mvc.perform(
            patch("/api/fournisseurs/" + id + "/blacklister")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isForbidden());
    Fournisseur f = Fixtures.actif();
    when(cycleVie.deciderBlacklistage(eq(id), eq(true), eq("Confirmé"), any()))
        .thenReturn(new FournisseurVue(f, 0, Fixtures.AUJOURDHUI, false));
    mvc.perform(
            patch("/api/fournisseurs/" + id + "/blacklister")
                .with(CseJwt.as(USER, Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(f.getId().toString()));
  }

  @Test
  void should_reserver_la_demande_de_blacklistage_au_ra_R09() throws Exception {
    mvc.perform(
            post("/api/fournisseurs/" + UUID.randomUUID() + "/blacklistage/demande")
                .with(CseJwt.as(USER, Roles.DIRECTION_GENERALE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"Fraude\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_traduire_regle_metier_en_422_et_conflit_en_409() throws Exception {
    UUID id = UUID.randomUUID();
    when(cycleVie.suspendre(eq(id), anyString(), any()))
        .thenThrow(new BusinessRuleException("MOTIF_OBLIGATOIRE", "Le motif est obligatoire."));
    mvc.perform(
            patch("/api/fournisseurs/" + id + "/suspendre")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"x\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM))
        .andExpect(jsonPath("$.code").value("MOTIF_OBLIGATOIRE"));
    when(cycleVie.reactiver(eq(id), any()))
        .thenThrow(
            new TransitionInterditeException(
                "fournisseur", StatutFournisseur.ACTIF, StatutFournisseur.ACTIF));
    mvc.perform(
            patch("/api/fournisseurs/" + id + "/reactiver")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRANSITION_STATUT_INVALIDE"));
  }

  @Test
  void should_autoriser_l_evaluation_au_ra_et_au_rm_seulement() throws Exception {
    UUID id = UUID.randomUUID();
    String corps = "{\"qualite\":8,\"delais\":7,\"conformite\":9,\"sav\":6,\"prix\":7}";
    mvc.perform(
            post("/api/fournisseurs/" + id + "/evaluer")
                .with(CseJwt.as(USER, Roles.DAF))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isForbidden());
    Evaluation e =
        new Evaluation(
            UUID.randomUUID(),
            id,
            null,
            new NotesEvaluation(8, 7, 9, 6, 7),
            new BigDecimal("76.0"),
            null,
            new Auteur(USER, "Ibrahima Fall"),
            Instant.EPOCH);
    when(evaluations.evaluer(eq(id), any(), any())).thenReturn(e);
    mvc.perform(
            post("/api/fournisseurs/" + id + "/evaluer")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.noteGlobale").value(76.0))
        .andExpect(jsonPath("$.evaluateurNom").value("Ibrahima Fall"));
    mvc.perform(
            post("/api/fournisseurs/" + id + "/evaluer")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qualite\":11,\"delais\":7,\"conformite\":9,\"sav\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field == 'qualite')]").exists())
        .andExpect(jsonPath("$.errors[?(@.field == 'prix')]").exists());
    when(evaluations.evaluations(id)).thenReturn(List.of(e));
    mvc.perform(get("/api/fournisseurs/" + id + "/evaluations").with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void should_appliquer_la_matrice_d_acces_des_litiges() throws Exception {
    mvc.perform(get("/api/litiges").with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(status().isForbidden());
    Fournisseur f = Fixtures.actif();
    Litige l =
        Litige.ouvrir(
            "LIT-2026-001",
            f,
            null,
            "Objet",
            "Description",
            GraviteLitige.FAIBLE,
            Fixtures.RA,
            Instant.EPOCH);
    when(litiges.rechercher(any(), any(), any())).thenReturn(PageResponse.of(List.of(l), 0, 20, 1));
    mvc.perform(get("/api/litiges?statut=OUVERT").with(CseJwt.as(USER, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].reference").value("LIT-2026-001"))
        .andExpect(jsonPath("$.content[0].actions[0].type").value("OUVERTURE"));
    mvc.perform(
            patch("/api/litiges/" + l.getId() + "/action")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"RESOLUTION\",\"commentaire\":\"ok\"}"))
        .andExpect(status().isForbidden());
    when(litiges.agir(eq(l.getId()), any(), eq("ok"), any())).thenReturn(l);
    mvc.perform(
            patch("/api/litiges/" + l.getId() + "/action")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"RESOLUTION\",\"commentaire\":\"ok\"}"))
        .andExpect(status().isOk());
    when(litiges.ouvrir(eq(f.getId()), any(), any())).thenReturn(l);
    mvc.perform(
            post("/api/fournisseurs/" + f.getId() + "/litiges")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"objet\":\"Objet\",\"description\":\"Description\",\"gravite\":\"FAIBLE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statut").value("OUVERT"));
  }

  @Test
  void should_publier_catalogue_par_ra_et_comparer_les_prix() throws Exception {
    Fournisseur f = Fixtures.actif();
    LigneCatalogue ligne =
        LigneCatalogue.creer(
            f,
            new DonneesLigneCatalogue(
                "PH20",
                "Pelle hydraulique 20 t",
                CategorieMateriel.MATERIEL_ROULANT,
                BigDecimal.valueOf(98_500_000),
                "unité",
                45),
            Instant.EPOCH);
    String corps =
        "{\"reference\":\"PH20\",\"designation\":\"Pelle hydraulique 20 t\",\"categorie\":\"MATERIEL_ROULANT\",\"prixUnitaire\":98500000,\"unite\":\"unité\",\"delaiLivraisonJours\":45}";
    mvc.perform(
            post("/api/fournisseurs/" + f.getId() + "/catalogue")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isForbidden());
    when(catalogue.publierLigne(eq(f.getId()), any())).thenReturn(ligne);
    mvc.perform(
            post("/api/fournisseurs/" + f.getId() + "/catalogue")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.prixUnitaire").value(98500000));
    when(catalogue.catalogue(f.getId())).thenReturn(List.of(ligne));
    mvc.perform(
            get("/api/fournisseurs/" + f.getId() + "/catalogue").with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(jsonPath("$[0].reference").value("PH20"));
    when(catalogue.historique(f.getId(), "PH20"))
        .thenReturn(List.of(sn.cse.fournisseurs.domain.model.PrixHistorique.depuis(ligne)));
    mvc.perform(
            get("/api/fournisseurs/" + f.getId() + "/prix/historique?reference=PH20")
                .with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(jsonPath("$[0].prixUnitaire").value(98500000));
    when(catalogue.comparer(CategorieMateriel.MATERIEL_ROULANT, null)).thenReturn(List.of());
    mvc.perform(
            get("/api/fournisseurs/comparaison-prix?categorie=MATERIEL_ROULANT")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void should_exposer_classement_et_statistiques_aux_lecteurs() throws Exception {
    Fournisseur f = Fixtures.actif();
    when(fournisseurs.parStatut(StatutFournisseur.ACTIF)).thenReturn(List.of(f));
    when(fournisseurs.compterParStatut()).thenReturn(Map.of(StatutFournisseur.ACTIF, 1L));
    when(litigeRepository.compterParFournisseur(anyList())).thenReturn(Map.of());
    mvc.perform(
            get("/api/fournisseurs/classement?categorie=MATERIEL_ROULANT")
                .with(CseJwt.as(USER, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].rang").value(1))
        .andExpect(jsonPath("$[0].raisonSociale").value("Sahel Engins SA"));
    mvc.perform(get("/api/fournisseurs/statistiques").with(CseJwt.as(USER, Roles.DAF)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.actifs").value(1));
    mvc.perform(get("/api/fournisseurs/statistiques").with(CseJwt.as(USER, Roles.ADMIN_SYSTEME)))
        .andExpect(status().isForbidden());
  }

  @Test
  void should_ajouter_document_et_gerer_le_cycle_de_vie_when_ra() throws Exception {
    UUID id = UUID.randomUUID();
    Fournisseur f = Fixtures.enAttente();
    var doc =
        f.ajouterDocument(
            sn.cse.fournisseurs.domain.model.TypeDocument.QUITUS_FISCAL,
            "Quitus",
            Fixtures.AUJOURDHUI.minusDays(1));
    when(referentiel.ajouterDocument(eq(id), any())).thenReturn(doc);
    mvc.perform(
            post("/api/fournisseurs/" + id + "/documents")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"type\":\"QUITUS_FISCAL\",\"libelle\":\"Quitus\",\"dateExpiration\":\"2026-10-29\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("QUITUS_FISCAL"));
    FournisseurVue vue = new FournisseurVue(f, 0, Fixtures.AUJOURDHUI, false);
    when(cycleVie.validerInscription(eq(id), any())).thenReturn(vue);
    when(cycleVie.demanderBlacklistage(eq(id), eq("Fraude"), any())).thenReturn(vue);
    when(referentiel.modifier(eq(id), eq(2L), any(), any())).thenReturn(vue);
    mvc.perform(
            patch("/api/fournisseurs/" + id + "/valider-inscription")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentsExpires").value(1));
    mvc.perform(
            post("/api/fournisseurs/" + id + "/blacklistage/demande")
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"Fraude\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/fournisseurs/" + id)
                .with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsFournisseurValide().replace("}\n", ",\"version\":2}\n")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documents[0].expire").value(true));
  }

  @Test
  void should_reserver_le_portail_aux_comptes_fournisseurs() throws Exception {
    mvc.perform(get("/api/portail/profil").with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden());
    Fournisseur f = Fixtures.actif();
    when(portail.profil(any())).thenReturn(new FournisseurVue(f, 0, Fixtures.AUJOURDHUI, true));
    when(portail.catalogue(any())).thenReturn(List.of());
    when(portail.modifierProfil(any(), eq(1L), any()))
        .thenReturn(new FournisseurVue(f, 0, Fixtures.AUJOURDHUI, true));
    var compte =
        CseJwt.avecClaims(USER, Map.of("fournisseur_id", f.getId().toString()), Roles.FOURNISSEUR);
    mvc.perform(get("/api/portail/profil").with(compte))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(f.getId().toString()))
        .andExpect(jsonPath("$.coordonneesBancaires.banque").value("Banque Sahélienne"));
    mvc.perform(get("/api/portail/catalogue").with(compte)).andExpect(status().isOk());
    mvc.perform(
            put("/api/portail/profil")
                .with(compte)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsFournisseurValide().replace("}\n", ",\"version\":1}\n")))
        .andExpect(status().isOk());
    Fournisseur proprietaire = Fixtures.actif();
    LigneCatalogue ligne =
        LigneCatalogue.creer(
            proprietaire,
            new DonneesLigneCatalogue(
                "PH20", "Pelle", CategorieMateriel.MATERIEL_ROULANT, BigDecimal.TEN, "unité", 4),
            Instant.EPOCH);
    when(portail.publierLigne(any(), any())).thenReturn(ligne);
    mvc.perform(
            put("/api/portail/catalogue")
                .with(compte)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"reference\":\"PH20\",\"designation\":\"Pelle\",\"categorie\":\"MATERIEL_ROULANT\",\"prixUnitaire\":10,\"unite\":\"unité\",\"delaiLivraisonJours\":4}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reference").value("PH20"));
  }

  // --- Inscription publique (contrat §13) ---

  @Test
  void should_accepter_l_inscription_sans_jeton_et_repondre_201() throws Exception {
    when(inscription.inscrire(any())).thenReturn(Fixtures.enAttente());
    mvc.perform(
            post("/api/portail/inscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsInscriptionValide()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.raisonSociale").value("Baobab Logistique"))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_VALIDATION"))
        .andExpect(header().doesNotExist("Location"));
  }

  @Test
  void should_repondre_400_par_champ_when_inscription_invalide() throws Exception {
    mvc.perform(
            post("/api/portail/inscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    corpsInscriptionValide()
                        .replace("008789012 2E7", "12-AB")
                        .replace("contact@baobab-logistique.sn", "pas-un-email")
                        .replace("[\"CONSOMMABLE\"]", "[]")
                        .replace("Seynabou Gueye", " ")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM))
        .andExpect(jsonPath("$.errors", hasSize(4)));
  }

  @Test
  void should_repondre_409_fournisseur_existant_when_ninea_ou_email_connu() throws Exception {
    when(inscription.inscrire(any()))
        .thenThrow(new ConflictException(CodesErreur.FOURNISSEUR_EXISTANT, "Déjà enregistré."));
    mvc.perform(
            post("/api/portail/inscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsInscriptionValide()))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(PROBLEM))
        .andExpect(jsonPath("$.code").value("FOURNISSEUR_EXISTANT"));
  }

  @Test
  void should_garder_le_reste_du_portail_authentifie_when_inscription_publique() throws Exception {
    mvc.perform(get("/api/portail/profil"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    mvc.perform(put("/api/portail/catalogue").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/fournisseurs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpsFournisseurValide()))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/portail/profil").with(CseJwt.as(USER, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isForbidden());
  }

  private static String corpsInscriptionValide() {
    return """
        {"raisonSociale":"Baobab Logistique","ninea":"008789012 2E7","pays":"Sénégal",\
        "ville":"Thiès","telephone":"+221 33 951 22 10","email":"contact@baobab-logistique.sn",\
        "contactNom":"Seynabou Gueye","contactFonction":"Gérante","specialites":["CONSOMMABLE"]}
        """;
  }

  private static String corpsFournisseurValide() {
    return """
        {"raisonSociale":"Sahel Engins SA","ninea":"005123456 2G3","rccm":"SN-DKR-2015-B-12345",\
        "pays":"Sénégal","telephone":"+221 33 849 12 34","email":"contact@sahel-engins.sn",\
        "specialites":["MATERIEL_ROULANT"],"certifications":["ISO 9001:2015"],\
        "coordonneesBancaires":{"banque":"Banque Sahélienne","iban":"SN08SN0100152000048500003035"}}
        """;
  }
}
