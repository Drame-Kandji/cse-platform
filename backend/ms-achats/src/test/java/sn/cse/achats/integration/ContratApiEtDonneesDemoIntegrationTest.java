package sn.cse.achats.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import sn.cse.achats.application.port.out.AchatsEventPublisher;
import sn.cse.achats.application.port.out.AppelOffreRepository;
import sn.cse.achats.application.port.out.BesoinAAcheterRepository;
import sn.cse.achats.application.port.out.ContratRepository;
import sn.cse.achats.application.port.out.FactureRepository;
import sn.cse.achats.application.port.out.FournisseurProjectionRepository;
import sn.cse.achats.application.port.out.ReferenceGenerator;
import sn.cse.achats.application.service.ParametresAchats;
import sn.cse.achats.infrastructure.messaging.BesoinsEventsListener;
import sn.cse.achats.infrastructure.seed.DevDataSeeder;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;

/**
 * Contrat OpenAPI figé (instantané {@code src/test/resources/openapi.json}) et jeu de données de
 * démonstration rejoué sur une base vide. Régénérer volontairement l'instantané avec {@code
 * -Dcse.openapi.regenerer=true}.
 */
class ContratApiEtDonneesDemoIntegrationTest extends IntegrationTestBase {

  private static final Path INSTANTANE = Path.of("src/test/resources/openapi.json");
  private static final UUID DG = UUID.fromString("00000000-0000-0000-0000-000000000501");
  private static final UUID BESOIN_13 = UUID.fromString("b0000000-0000-0000-0000-000000000013");
  private static final UUID F1 = UUID.fromString("f0000000-0000-0000-0000-000000000001");

  @Autowired AppelOffreRepository appelsOffres;
  @Autowired ContratRepository contrats;
  @Autowired FactureRepository factures;
  @Autowired BesoinAAcheterRepository besoins;
  @Autowired FournisseurProjectionRepository fournisseurs;
  @Autowired ReferenceGenerator references;
  @Autowired AchatsEventPublisher evenements;
  @Autowired ParametresAchats parametres;
  @Autowired PlatformTransactionManager transactions;
  @Autowired BesoinsEventsListener besoinsListener;

  @Test
  void should_exposer_le_contrat_openapi_fige() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    ObjectNode actuel = (ObjectNode) mapper.readTree(brut);
    // L'URL du serveur dépend de l'environnement d'exécution.
    actuel.remove("servers");
    String normalise =
        mapper.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(actuel) + "\n";
    if (Boolean.getBoolean("cse.openapi.regenerer")) {
      Files.writeString(INSTANTANE, normalise, StandardCharsets.UTF_8);
    }
    assertThat(INSTANTANE)
        .as("Instantané OpenAPI absent : lancer une fois avec -Dcse.openapi.regenerer=true")
        .exists();
    JsonNode attendu = mapper.readTree(Files.readString(INSTANTANE, StandardCharsets.UTF_8));
    assertThat(actuel).isEqualTo(attendu);
    assertThat(
            actuel
                .at(
                    "/paths/~1api~1appels-offres~1{id}~1proces-verbaux~1{type}/get/responses/200/content")
                .has("application/pdf"))
        .isTrue();
    assertThat(actuel.at("/components/schemas/ProblemDetail").isMissingNode()).isFalse();
  }

  @Test
  void should_semer_un_historique_coherent_et_idempotent() throws Exception {
    jdbc.execute(
        "TRUNCATE offre_note, offre, appel_offre_critere, appel_offre_invite, contrat_jalon, facture,"
            + " contrat_achat, appel_offre, besoin_a_acheter, fournisseur_projection, outbox_event,"
            + " processed_event CASCADE");
    horloge.regler(Instant.parse("2026-10-30T08:00:00Z"));
    DevDataSeeder seeder =
        new DevDataSeeder(
            appelsOffres,
            contrats,
            factures,
            besoins,
            fournisseurs,
            references,
            evenements,
            parametres,
            transactions,
            horloge);

    seeder.run(null);
    seeder.run(null);

    assertThat(compter("SELECT count(*) FROM appel_offre")).isEqualTo(13);
    assertThat(compter("SELECT count(DISTINCT statut) FROM appel_offre")).isEqualTo(3);
    assertThat(compter("SELECT count(*) FROM contrat_achat")).isEqualTo(9);
    assertThat(compter("SELECT count(DISTINCT statut) FROM contrat_achat")).isEqualTo(7);
    assertThat(compter("SELECT count(*) FROM facture")).isEqualTo(11);
    assertThat(compter("SELECT count(DISTINCT statut) FROM facture")).isEqualTo(4);
    assertThat(compter("SELECT count(*) FROM besoin_a_acheter")).isEqualTo(11);
    assertThat(compter("SELECT count(*) FROM besoin_a_acheter WHERE appel_offre_id IS NULL"))
        .isEqualTo(2);
    // Tous les besoins référencés sont les besoins EN_ACHAT 13 à 23 du seed de ms-besoins.
    assertThat(
            compter(
                "SELECT count(*) FROM appel_offre WHERE besoin_id IS NOT NULL AND besoin_id NOT IN"
                    + " (SELECT besoin_id FROM besoin_a_acheter)"))
        .isZero();
    assertThat(compter("SELECT count(*) FROM appel_offre WHERE besoin_id IS NOT NULL"))
        .isEqualTo(9);
    assertThat(
            compter(
                "SELECT count(*) FROM contrat_achat c JOIN besoin_a_acheter b ON b.besoin_id ="
                    + " c.besoin_id WHERE c.designation <> b.designation OR c.categorie <> b.categorie"
                    + " OR c.quantite <> b.quantite"))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT reference || '|' || designation FROM besoin_a_acheter WHERE besoin_id = ?",
                String.class,
                BESOIN_13))
        .isEqualTo("BES-2026-0002|Grue mobile 50 t");
    assertThat(compter("SELECT count(*) FROM fournisseur_projection")).isEqualTo(7);
    assertThat(
            compter("SELECT count(*) FROM appel_offre WHERE approbation_dg_statut = 'EN_ATTENTE'"))
        .isEqualTo(1);
    assertThat(compter("SELECT count(*) FROM appel_offre WHERE covalidation_statut = 'EN_ATTENTE'"))
        .isEqualTo(1);
    assertThat(compter("SELECT count(*) FROM contrat_achat WHERE engagement_statut = 'EN_ATTENTE'"))
        .isEqualTo(1);
    // Signatures en deux temps : tout contrat signé porte les deux signatures.
    assertThat(
            compter(
                "SELECT count(*) FROM contrat_achat WHERE date_signature IS NOT NULL AND"
                    + " (signature_cse_par_nom IS NULL OR signature_fournisseur_par_nom IS NULL)"))
        .isZero();
    assertThat(
            compter(
                "SELECT count(*) FROM outbox_event WHERE event_type = 'CONTRAT_SIGNATURE_REFUSEE'"))
        .isEqualTo(1);
    assertThat(
            compter(
                "SELECT count(*) FROM outbox_event WHERE event_type = 'ALERTE_RETARD_LIVRAISON'"))
        .isEqualTo(1);
    assertThat(
            compter("SELECT count(*) FROM outbox_event WHERE event_type = 'LIVRAISON_EFFECTUEE'"))
        .isEqualTo(4);
    assertThat(
            compter(
                "SELECT count(*) FROM appel_offre WHERE date_publication < TIMESTAMPTZ '2026-10-30' - INTERVAL '8 months'"))
        .isPositive();

    mvc.perform(get("/api/finance/synthese").with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.facturesEnRetard").value(2))
        .andExpect(jsonPath("$.engagementsEnAttente").value(1));
    mvc.perform(
            get("/api/appels-offres?statut=PUBLIE").with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        // Les séquences de référence ne sont pas réinitialisées par TRUNCATE : seul l'objet est
        // figé.
        .andExpect(jsonPath("$.content[0].objet").value("Acquisition d'un porte-char 50 t"))
        .andExpect(jsonPath("$.content[0].besoinReference").value("BES-2026-0020"))
        .andExpect(jsonPath("$.content[0].nombreOffres").value(1));

    // Portail du fournisseur 1 (compte de démonstration fournisseur@cse.sn).
    var portailF1 =
        CseJwt.avecClaims(
            UUID.fromString("00000000-0000-0000-0000-000000000701"),
            Map.of("fournisseur_id", F1.toString()),
            Roles.FOURNISSEUR);
    mvc.perform(get("/api/portail/appels-offres").with(portailF1))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.content[?(@.objet==\"Acquisition d'un porte-char 50 t\")].invite")
                .value(true))
        .andExpect(
            jsonPath("$.content[?(@.objet==\"Acquisition d'un porte-char 50 t\")].offreDeposee")
                .value(false));
    assertThat(
            compter(
                "SELECT count(*) FROM appel_offre WHERE statut = 'PUBLIE' AND date_limite >"
                    + " TIMESTAMPTZ '2026-10-30 08:00:00+00' + INTERVAL '10 days'"))
        .isEqualTo(1);
    for (String statut : List.of("EN_ATTENTE_SIGNATURE", "ENGAGEMENT_VALIDE", "LIVRE")) {
      mvc.perform(get("/api/portail/contrats?statut=" + statut).with(portailF1))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").isNotEmpty())
          .andExpect(jsonPath("$.content[0].fournisseurId").value(F1.toString()));
    }
    assertThat(
            compter(
                "SELECT count(*) FROM contrat_achat c WHERE c.fournisseur_id = '"
                    + F1
                    + "' AND c.statut = 'LIVRE' AND NOT EXISTS (SELECT 1 FROM facture f WHERE"
                    + " f.contrat_id = c.id)"))
        .isEqualTo(1);
    assertThat(
            compter(
                "SELECT count(DISTINCT f.statut) FROM facture f WHERE f.fournisseur_id = '"
                    + F1
                    + "' AND f.statut IN ('RECUE', 'MANDATEE', 'PAYEE')"))
        .isEqualTo(3);
  }

  @Test
  void should_absorber_le_besoin_en_achat_publie_par_ms_besoins_apres_le_seed() throws Exception {
    if (compter("SELECT count(*) FROM besoin_a_acheter WHERE besoin_id = '" + BESOIN_13 + "'")
        == 0) {
      should_semer_un_historique_coherent_et_idempotent();
    }
    String aoAvant =
        jdbc.queryForObject(
            "SELECT appel_offre_id::text FROM besoin_a_acheter WHERE besoin_id = ?",
            String.class,
            BESOIN_13);
    String evenement =
        ("{'eventId':'"
                + UUID.randomUUID()
                + "','type':'BESOIN_EN_ACHAT','version':1,"
                + "'occurredAt':'2026-02-11T11:01:00Z','source':'ms-besoins','aggregateType':'BesoinMateriel',"
                + "'aggregateId':'"
                + BESOIN_13
                + "','payload':{'besoinId':'"
                + BESOIN_13
                + "',"
                + "'reference':'BES-2026-0002','chantierId':'c0000000-0000-0000-0000-000000000001',"
                + "'chantierNom':'Extension autoroute Dakar-AIBD','designation':'Grue mobile 50 t',"
                + "'categorie':'MATERIEL_ROULANT','quantite':1,'specifications':'Flèche télescopique 40 m',"
                + "'priorite':'CRITIQUE','statut':'EN_ACHAT','dateBesoin':'2026-02-25',"
                + "'dureeUtilisationJours':120,'materielIds':[]}}")
            .replace('\'', '"');

    besoinsListener.recevoir(evenement);
    besoinsListener.recevoir(evenement);

    assertThat(compter("SELECT count(*) FROM besoin_a_acheter")).isEqualTo(11);
    assertThat(
            jdbc.queryForObject(
                "SELECT appel_offre_id::text FROM besoin_a_acheter WHERE besoin_id = ?",
                String.class,
                BESOIN_13))
        .isEqualTo(aoAvant)
        .isNotNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT reference FROM besoin_a_acheter WHERE besoin_id = ?",
                String.class,
                BESOIN_13))
        .isEqualTo("BES-2026-0002");
  }

  private int compter(String sql) {
    Integer n = jdbc.queryForObject(sql, Integer.class);
    return n == null ? 0 : n;
  }
}
