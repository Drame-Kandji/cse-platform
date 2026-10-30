package sn.cse.fournisseurs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.events.payload.ContratPayload;
import sn.cse.fournisseurs.application.port.in.SurveillanceFournisseursUseCase;
import sn.cse.fournisseurs.application.port.out.FournisseurRepository;
import sn.cse.fournisseurs.domain.model.Fournisseur;
import sn.cse.fournisseurs.infrastructure.messaging.AchatsEventsListener;
import sn.cse.fournisseurs.infrastructure.messaging.SuiviEventsListener;

/**
 * Bout en bout sur PostgreSQL (Testcontainers) : migrations Flyway et types ENUM natifs,
 * chiffrement des coordonnées bancaires, outbox, consommateurs idempotents, contrat OpenAPI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
class FournisseursIntegrationTest {

  private static final UUID RA = UUID.fromString("00000000-0000-0000-0000-000000000301");
  private static final UUID RM = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final UUID DG = UUID.fromString("00000000-0000-0000-0000-000000000501");
  private static final String IBAN = "SN08SN0100152000048500003035";
  private static final Path SNAPSHOT = Path.of("src/test/resources/openapi.json");

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private FournisseurRepository fournisseurs;
  @Autowired private AchatsEventsListener achats;
  @Autowired private SuiviEventsListener suivi;
  @Autowired private SurveillanceFournisseursUseCase surveillance;

  @Test
  void should_chiffrer_les_coordonnees_bancaires_au_repos_et_les_relire_en_clair()
      throws Exception {
    UUID id = creer("Chiffrement SA", "OUTILLAGE");
    Map<String, Object> brut =
        jdbc.queryForMap("SELECT banque, iban FROM fournisseurs WHERE id = ?", id);
    assertThat((String) brut.get("iban")).isNotBlank().doesNotContain("SN08").isNotEqualTo(IBAN);
    assertThat((String) brut.get("banque")).doesNotContain("Sahélienne");
    Fournisseur relu = fournisseurs.parId(id).orElseThrow();
    assertThat(relu.getCoordonneesBancaires())
        .get()
        .satisfies(
            c -> {
              assertThat(c.iban()).isEqualTo(IBAN);
              assertThat(c.banque()).isEqualTo("Banque Sahélienne");
            });
    // Chiffrement probabiliste (IV aléatoire) : le même IBAN ne produit pas le même texte chiffré.
    UUID autre = creer("Chiffrement Bis SA", "OUTILLAGE");
    String ibanAutre =
        jdbc.queryForObject("SELECT iban FROM fournisseurs WHERE id = ?", String.class, autre);
    assertThat(ibanAutre).isNotEqualTo(brut.get("iban"));
  }

  @Test
  void should_derouler_evaluation_litige_et_outbox_R07() throws Exception {
    UUID id = creer("Parcours SARL", "MATERIEL_ROULANT");
    mvc.perform(
            json(
                post("/api/fournisseurs/" + id + "/evaluer"),
                RM,
                Roles.RESPONSABLE_MATERIEL,
                Map.of(
                    "qualite",
                    8,
                    "delais",
                    7,
                    "conformite",
                    9,
                    "sav",
                    6,
                    "prix",
                    7,
                    "commentaire",
                    "RAS")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.noteGlobale").value(76.0));
    String litige =
        mvc.perform(
                json(
                    post("/api/fournisseurs/" + id + "/litiges"),
                    RA,
                    Roles.RESPONSABLE_ACHATS,
                    Map.of(
                        "objet",
                        "Livraison incomplète",
                        "description",
                        "2 sur 5",
                        "gravite",
                        "MOYENNE")))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.reference")
                    .value(org.hamcrest.Matchers.matchesPattern("LIT-\\d{4}-\\d{3}")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String litigeId = mapper.readTree(litige).get("id").asText();
    mvc.perform(
            json(
                patch("/api/litiges/" + litigeId + "/action"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of("type", "RESOLUTION", "commentaire", "Avoir accepté")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("RESOLU"))
        .andExpect(jsonPath("$.actions.length()").value(2));
    mvc.perform(get("/api/fournisseurs/" + id).with(CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        // 76 − 2 (litige résolu)
        .andExpect(jsonPath("$.scorePerformance").value(74.0))
        .andExpect(jsonPath("$.nombreEvaluations").value(1))
        .andExpect(jsonPath("$.nombreLitigesOuverts").value(0))
        .andExpect(jsonPath("$.coordonneesBancaires").doesNotExist());

    List<String> types =
        jdbc.queryForList(
            "SELECT event_type FROM outbox_event WHERE topic = 'cse.fournisseurs.events' AND message_key IN (?, ?)"
                + " ORDER BY created_at",
            String.class,
            id.toString(),
            litigeId);
    assertThat(types)
        .contains(
            EventTypes.FOURNISSEUR_INSCRIT,
            EventTypes.SCORE_RECALCULE,
            EventTypes.LITIGE_OUVERT,
            EventTypes.LITIGE_RESOLU);
    Integer audits =
        jdbc.queryForObject(
            "SELECT count(*) FROM outbox_event WHERE topic = 'cse.audit.events' AND message_key = ?",
            Integer.class,
            id.toString());
    assertThat(audits).isGreaterThanOrEqualTo(3);
  }

  @Test
  void should_blacklister_en_deux_etapes_via_l_api_R09() throws Exception {
    UUID id = creer("Blacklistage SA", "CONSOMMABLE");
    mvc.perform(
            json(
                post("/api/fournisseurs/" + id + "/blacklistage/demande"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of("motif", "Fraude")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TRANSITION_STATUT_INVALIDE"));
    mvc.perform(
            json(
                patch("/api/fournisseurs/" + id + "/suspendre"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of("motif", "Non-conformités")))
        .andExpect(jsonPath("$.statut").value("SUSPENDU"));
    mvc.perform(
            json(
                post("/api/fournisseurs/" + id + "/blacklistage/demande"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of("motif", "Fraude")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("SUSPENDU"))
        .andExpect(jsonPath("$.blacklistageDemande.motif").value("Fraude"));
    mvc.perform(
            json(
                patch("/api/fournisseurs/" + id + "/blacklister"),
                DG,
                Roles.DIRECTION_GENERALE,
                Map.of("approuve", true)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("BLACKLISTE"))
        .andExpect(jsonPath("$.blacklistageDemande").doesNotExist());
  }

  @Test
  void should_filtrer_les_actifs_par_specialite_pour_ms_achats_R14() throws Exception {
    UUID actif = creer("Filtre Actif Informatique", "EQUIPEMENT_INFORMATIQUE");
    UUID suspendu = creer("Filtre Suspendu Informatique", "EQUIPEMENT_INFORMATIQUE");
    mvc.perform(
        json(
            patch("/api/fournisseurs/" + suspendu + "/suspendre"),
            RA,
            Roles.RESPONSABLE_ACHATS,
            Map.of("motif", "Contrôle")));
    String reponse =
        mvc.perform(
                get("/api/fournisseurs?statut=ACTIF&specialite=EQUIPEMENT_INFORMATIQUE&q=filtre&size=50")
                    .with(CseJwt.as(RA, Roles.RESPONSABLE_ACHATS)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode contenu = mapper.readTree(reponse).get("content");
    assertThat(contenu.findValuesAsText("id"))
        .contains(actif.toString())
        .doesNotContain(suspendu.toString());
    assertThat(contenu.findValuesAsText("statut")).containsOnly("ACTIF");
  }

  @Test
  void should_traiter_une_seule_fois_les_evenements_consommes_R07() throws Exception {
    UUID id = creer("Livraisons SA", "MATERIEL_ROULANT");
    ContratPayload contrat =
        new ContratPayload(
            UUID.randomUUID(),
            "CTR-2026-001",
            UUID.randomUUID(),
            null,
            null,
            null,
            id,
            "Livraisons SA",
            "Camion-benne 6x4",
            "MATERIEL_ROULANT",
            2,
            java.math.BigDecimal.valueOf(120_000_000),
            "LIVRE",
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 6, 28),
            null);
    String message = enveloppe(EventTypes.LIVRAISON_EFFECTUEE, contrat);
    achats.recevoir(message);
    achats.recevoir(message);
    achats.recevoir(enveloppe(EventTypes.CONTRAT_SIGNE, contrat));
    Fournisseur f = fournisseurs.parId(id).orElseThrow();
    assertThat(f.getPerformance().livraisonsTotal()).isEqualTo(1);
    assertThat(f.getPerformance().tauxLivraisonALHeure())
        .get()
        .satisfies(t -> assertThat(t).isEqualByComparingTo("100"));
    // 50 × 0,8 + 100 × 0,2 = 60
    assertThat(f.getPerformance().scorePerformance()).isEqualByComparingTo("60.0");

    AffectationPayload reception =
        new AffectationPayload(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ENG-001",
            "Camion-benne",
            "MATERIEL_ROULANT",
            null,
            UUID.randomUUID(),
            "Contournement de Thiès",
            null,
            null,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 9, 1),
            null,
            "EN_COURS",
            id,
            contrat.contratId());
    String msgReception = enveloppe(EventTypes.RECEPTION_CONFIRMEE, reception);
    suivi.recevoir(msgReception);
    suivi.recevoir(msgReception);
    Integer traites =
        jdbc.queryForObject(
            "SELECT count(*) FROM processed_event WHERE consumer LIKE 'ms-fournisseurs.%'",
            Integer.class);
    assertThat(traites).isGreaterThanOrEqualTo(2);
  }

  @Test
  void should_historiser_les_prix_et_comparer_entre_fournisseurs() throws Exception {
    UUID a = creer("Comparaison Alpha", "OUTILLAGE");
    UUID b = creer("Comparaison Beta", "OUTILLAGE");
    ligne(a, "ALP-GE250", "Groupe électrogène 250 kVA comparé", 30_000_000);
    ligne(a, "ALP-GE250", "Groupe électrogène 250 kVA comparé", 31_000_000);
    ligne(b, "BET-GE250", "Groupe electrogene 250 kVA compare", 29_500_000);
    mvc.perform(
            get("/api/fournisseurs/" + a + "/prix/historique?reference=alp-ge250")
                .with(CseJwt.as(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].prixUnitaire").value(31_000_000));
    mvc.perform(
            get("/api/fournisseurs/comparaison-prix?categorie=OUTILLAGE&q=250 kVA")
                .with(CseJwt.as(RA, Roles.RESPONSABLE_ACHATS)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].prixMin").value(29_500_000))
        .andExpect(jsonPath("$[0].prixMax").value(31_000_000))
        .andExpect(jsonPath("$[0].offres.length()").value(2));
    mvc.perform(
            json(
                post("/api/fournisseurs/" + a + "/catalogue"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of(
                    "reference",
                    "X",
                    "designation",
                    "Ciment",
                    "categorie",
                    "CONSOMMABLE",
                    "prixUnitaire",
                    80000,
                    "unite",
                    "tonne",
                    "delaiLivraisonJours",
                    3)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CATEGORIE_HORS_SPECIALITES"));
  }

  @Test
  void should_signaler_documents_expires_et_refuser_version_obsolete() throws Exception {
    UUID id = creer("Documents SA", "OUTILLAGE");
    mvc.perform(
            json(
                post("/api/fournisseurs/" + id + "/documents"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of(
                    "type",
                    "ASSURANCE",
                    "libelle",
                    "Assurance RC",
                    "dateExpiration",
                    LocalDate.now().minusDays(2).toString())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.expire").value(true));
    assertThat(surveillance.signalerDocumentsExpires()).isGreaterThanOrEqualTo(1);
    Integer evenements =
        jdbc.queryForObject(
            "SELECT count(*) FROM outbox_event WHERE event_type = 'DOCUMENT_EXPIRE' AND message_key = ?",
            Integer.class,
            id.toString());
    assertThat(evenements).isEqualTo(1);
    assertThat(surveillance.signalerDocumentsExpires()).isZero();

    Map<String, Object> corps = new LinkedHashMap<>(corpsFournisseur("Documents SA", "OUTILLAGE"));
    corps.put(
        "ninea",
        jdbc.queryForObject("SELECT ninea FROM fournisseurs WHERE id = ?", String.class, id));
    corps.put("version", 99);
    mvc.perform(json(put("/api/fournisseurs/" + id), RA, Roles.RESPONSABLE_ACHATS, corps))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLIT_CONCURRENCE"));
  }

  @Test
  void should_correspondre_au_contrat_openapi_publie() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode actuel = mapper.readTree(brut);
    if (Boolean.getBoolean("openapi.regenerer") || !Files.exists(SNAPSHOT)) {
      Files.writeString(
          SNAPSHOT,
          mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(actuel));
    }
    JsonNode attendu = mapper.readTree(Files.readString(SNAPSHOT));
    assertThat(actuel)
        .as(
            "Le contrat OpenAPI a changé : régénérer src/test/resources/openapi.json avec -Dopenapi.regenerer=true")
        .isEqualTo(attendu);
    assertThat(
            actuel
                .at("/paths/~1api~1fournisseurs~1{id}~1blacklister/patch/responses/403")
                .isMissingNode())
        .isFalse();
  }

  // --- utilitaires ------------------------------------------------------------------------------

  private UUID creer(String nom, String specialite) throws Exception {
    String reponse =
        mvc.perform(
                json(
                    post("/api/fournisseurs"),
                    RA,
                    Roles.RESPONSABLE_ACHATS,
                    corpsFournisseur(nom, specialite)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.coordonneesBancaires.iban").value(IBAN))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(mapper.readTree(reponse).get("id").asText());
  }

  private void ligne(UUID id, String ref, String designation, long prix) throws Exception {
    mvc.perform(
            json(
                post("/api/fournisseurs/" + id + "/catalogue"),
                RA,
                Roles.RESPONSABLE_ACHATS,
                Map.of(
                    "reference",
                    ref,
                    "designation",
                    designation,
                    "categorie",
                    "OUTILLAGE",
                    "prixUnitaire",
                    prix,
                    "unite",
                    "unité",
                    "delaiLivraisonJours",
                    10)))
        .andExpect(status().isOk());
  }

  private static Map<String, Object> corpsFournisseur(String nom, String specialite) {
    Map<String, Object> corps = new LinkedHashMap<>();
    corps.put("raisonSociale", nom);
    corps.put(
        "ninea",
        String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000)) + " 1A1");
    corps.put("pays", "Sénégal");
    corps.put("ville", "Thiès");
    corps.put("telephone", "+221 33 951 22 48");
    corps.put("email", "contact@test.sn");
    corps.put("specialites", List.of(specialite));
    corps.put("certifications", List.of("ISO 9001:2015"));
    corps.put("coordonneesBancaires", Map.of("banque", "Banque Sahélienne", "iban", IBAN));
    return corps;
  }

  private MockHttpServletRequestBuilder json(
      MockHttpServletRequestBuilder requete, UUID user, String role, Object corps)
      throws Exception {
    return requete
        .with(CseJwt.as(user, role))
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(corps));
  }

  private String enveloppe(String type, Object payload) throws Exception {
    return mapper.writeValueAsString(
        new EventEnvelope(
            UUID.randomUUID(),
            type,
            1,
            Instant.now(),
            "ms-test",
            "Test",
            UUID.randomUUID().toString(),
            null,
            null,
            mapper.valueToTree(payload)));
  }
}
