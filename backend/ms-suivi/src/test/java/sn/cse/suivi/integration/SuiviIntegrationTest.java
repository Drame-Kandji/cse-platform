package sn.cse.suivi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import sn.cse.common.exception.CseException;
import sn.cse.common.security.CurrentUser;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.events.Actor;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.ContratPayload;
import sn.cse.suivi.application.command.AffecterMaterielCommande;
import sn.cse.suivi.application.port.in.AffecterMaterielUseCase;
import sn.cse.suivi.application.port.in.DeclencherAlertesUseCase;
import sn.cse.suivi.infrastructure.messaging.LivraisonEffectueeListener;

/**
 * Parcours de bout en bout sur une vraie base PostgreSQL : amorçage, affectation complète
 * (réception, maintenance, libération), concurrence, consommation Kafka idempotente, alertes, R12
 * et R13.
 */
class SuiviIntegrationTest extends IntegrationTestSupport {

  private static final UUID RM = UUID.fromString("00000000-0000-0000-0000-000000000201");
  private static final UUID CHEF = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID DG = UUID.fromString("00000000-0000-0000-0000-000000000501");
  private static final UUID CHANTIER_1 = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  private static final UUID CHANTIER_2 = UUID.fromString("c0000000-0000-0000-0000-000000000002");
  private static final UUID CHANTIER_3 = UUID.fromString("c0000000-0000-0000-0000-000000000003");
  private static final AtomicInteger COMPTEUR = new AtomicInteger();

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  @Autowired AffecterMaterielUseCase affectation;
  @Autowired DeclencherAlertesUseCase alertes;
  @Autowired LivraisonEffectueeListener livraisons;

  @Test
  void should_seed_shared_reference_data_when_dev_profile_active() throws Exception {
    mvc.perform(get("/api/chantiers").param("q", "CH-DKR-01").with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].statut").value("EN_COURS"))
        .andExpect(jsonPath("$.content[0].id").value("c0000000-0000-0000-0000-000000000001"))
        .andExpect(
            jsonPath("$.content[0].dateDebut")
                .value(LocalDate.now(clock).withDayOfMonth(1).toString()));
    mvc.perform(get("/api/chantiers").with(CseJwt.chef(CHEF, CHANTIER_1, CHANTIER_2)))
        .andExpect(jsonPath("$.totalElements").value(2));
    mvc.perform(get("/api/materiels").param("q", "PEL-CAT320-01").with(rm()))
        .andExpect(jsonPath("$.content[0].etat").value("DISPONIBLE"))
        .andExpect(jsonPath("$.content[0].typeEnginLibelle").value("Pelle hydraulique"));
    mvc.perform(get("/api/materiels/{id}", "a1000000-0000-0000-0000-000000000001").with(rm()))
        .andExpect(jsonPath("$.code").value("PEL-CAT320-01"));
    mvc.perform(get("/api/materiels/{id}", "a1000000-0000-0000-0000-000000000012").with(rm()))
        .andExpect(jsonPath("$.code").value("INF-TAB-01"));
    mvc.perform(get("/api/referentiels/types-engins").with(rm()))
        .andExpect(jsonPath("$.length()").value(14));
    mvc.perform(get("/api/materiels/carte").with(rm()))
        .andExpect(jsonPath("$.type").value("FeatureCollection"))
        .andExpect(jsonPath("$.features[0].geometry.type").value("Point"));

    assertThat(
            compter(
                "SELECT count(*) FROM materiels WHERE created_by = 'systeme' AND contrat_id IS NULL"))
        .isEqualTo(15);
    assertThat(
            compter(
                "SELECT count(*) FROM materiels WHERE id::text LIKE 'a1000000-%'"
                    + " AND date_acquisition <> DATE '"
                    + LocalDate.now(clock).withDayOfMonth(1)
                    + "'"))
        .isZero();
    assertThat(
            compter(
                "SELECT count(*) FROM materiels WHERE id::text LIKE 'a1000000-%'"
                    + " AND etat = 'DISPONIBLE'"))
        .isEqualTo(9);
    // 6 engins affectés depuis le 1er du mois, réception confirmée (3 sur les chantiers du chef).
    assertThat(
            compter(
                "SELECT count(*) FROM affectations a JOIN materiels m ON m.id = a.materiel_id"
                    + " WHERE m.id::text LIKE 'a1000000-%' AND m.etat = 'AFFECTE'"
                    + " AND a.statut = 'EN_COURS' AND a.chantier_id = m.chantier_actuel_id"))
        .isEqualTo(6);
    mvc.perform(get("/api/materiels").param("q", "PEL-CAT320-02").with(rm()))
        .andExpect(jsonPath("$.content[0].etat").value("AFFECTE"))
        .andExpect(jsonPath("$.content[0].chantierActuelId").value(CHANTIER_1.toString()));
    assertThat(compter("SELECT count(*) FROM outbox_event WHERE event_type = 'CHANTIER_CREE'"))
        .isGreaterThanOrEqualTo(6);
  }

  @Test
  void should_run_complete_affectation_flow_with_events() throws Exception {
    String chantierId = creerChantier();
    String materielId = creerMateriel();
    LocalDate aujourdhui = LocalDate.now(clock);

    String affectationJson =
        mvc.perform(affecter(materielId, chantierId, aujourdhui, aujourdhui.plusDays(30)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RECEPTION"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(affecter(materielId, chantierId, aujourdhui.plusDays(10), aujourdhui.plusDays(40)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLIT_AFFECTATION"));

    UUID chantier = UUID.fromString(chantierId);
    mvc.perform(
            post("/api/materiels/{id}/confirmer-reception", materielId)
                .with(CseJwt.chef(CHEF, CHANTIER_1)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CHANTIER_NON_AUTORISE"));
    mvc.perform(
            post("/api/materiels/{id}/confirmer-reception", materielId)
                .with(CseJwt.chef(CHEF, chantier))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observations\":\"Reçu complet\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("EN_COURS"))
        .andExpect(jsonPath("$.observationsReception").value("Reçu complet"));
    mvc.perform(
            json(
                    post("/api/materiels/{id}/heures", materielId),
                    Map.of("heuresFonctionnement", 1300))
                .with(CseJwt.chef(CHEF, chantier)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.heuresFonctionnement").value(1300));

    String maintenanceId =
        JsonPath.read(
            mvc.perform(
                    json(
                            post("/api/maintenances"),
                            Map.of(
                                "materielId",
                                materielId,
                                "type",
                                "PREVENTIVE",
                                "description",
                                "Vidange et filtres",
                                "datePlanifiee",
                                aujourdhui.toString()))
                        .with(rm()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOt").value(matchesPattern("OT-\\d{4}-\\d{4}")))
                .andExpect(jsonPath("$.chantierId").value(chantierId))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.id");
    mvc.perform(patch("/api/maintenances/{id}/demarrer", maintenanceId).with(rm()))
        .andExpect(jsonPath("$.statut").value("EN_COURS"));
    mvc.perform(get("/api/materiels/{id}", materielId).with(rm()))
        .andExpect(jsonPath("$.etat").value("EN_MAINTENANCE"));
    mvc.perform(
            json(
                    patch("/api/maintenances/{id}/cloturer", maintenanceId),
                    Map.of("cout", 250000, "heuresCompteur", 1310))
                .with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("CLOTUREE"));
    mvc.perform(get("/api/materiels/{id}", materielId).with(rm()))
        .andExpect(jsonPath("$.etat").value("AFFECTE"))
        .andExpect(jsonPath("$.heuresDepuisMaintenance").value(0));

    mvc.perform(post("/api/materiels/{id}/liberer", materielId).with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statut").value("TERMINEE"))
        .andExpect(jsonPath("$.dateFinReelle").value(aujourdhui.toString()));
    mvc.perform(get("/api/materiels/{id}", materielId).with(rm()))
        .andExpect(jsonPath("$.etat").value("DISPONIBLE"))
        .andExpect(jsonPath("$.affectations.length()").value(1))
        .andExpect(jsonPath("$.maintenances.length()").value(1))
        .andExpect(jsonPath("$.mouvements.length()").value(6));
    mvc.perform(
            get("/api/affectations/planning")
                .param("chantierId", chantierId)
                .param("du", aujourdhui.minusDays(1).toString())
                .param("au", aujourdhui.plusDays(5).toString())
                .with(rm()))
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(patch("/api/chantiers/{id}/terminer", chantierId).with(rm()))
        .andExpect(jsonPath("$.statut").value("TERMINE"));

    String affectationId = JsonPath.read(affectationJson, "$.id");
    assertThat(
            jdbc.queryForList(
                "SELECT event_type FROM outbox_event WHERE message_key = ? AND topic = 'cse.suivi.events'"
                    + " ORDER BY created_at",
                String.class,
                affectationId))
        .containsExactly(
            EventTypes.MATERIEL_AFFECTE,
            EventTypes.RECEPTION_CONFIRMEE,
            EventTypes.AFFECTATION_TERMINEE);
    assertThat(
            jdbc.queryForList(
                "SELECT event_type FROM outbox_event WHERE message_key = ?"
                    + " AND topic = 'cse.suivi.events'",
                String.class,
                maintenanceId))
        .containsExactlyInAnyOrder(
            EventTypes.MAINTENANCE_PLANIFIEE,
            EventTypes.MAINTENANCE_DEMARREE,
            EventTypes.MAINTENANCE_CLOTUREE);
  }

  @Test
  void should_accept_only_one_of_two_concurrent_affectations_R04() throws Exception {
    UUID chantierA = UUID.fromString(creerChantier());
    UUID chantierB = UUID.fromString(creerChantier());
    UUID materiel = UUID.fromString(creerMateriel());
    LocalDate debut = LocalDate.now(clock);
    CurrentUser rm =
        new CurrentUser(RM, "Ibrahima Fall", null, Set.of(Roles.RESPONSABLE_MATERIEL), null, null);
    CyclicBarrier depart = new CyclicBarrier(2);
    ExecutorService executeur = Executors.newFixedThreadPool(2);
    List<Future<String>> resultats = new ArrayList<>();
    for (UUID chantier : List.of(chantierA, chantierB)) {
      resultats.add(
          executeur.submit(
              () -> {
                depart.await(5, TimeUnit.SECONDS);
                try {
                  affectation.affecter(
                      new AffecterMaterielCommande(
                          materiel, chantier, null, debut, debut.plusDays(60), rm));
                  return "OK";
                } catch (CseException e) {
                  return e.getCode();
                }
              }));
    }
    List<String> codes = new ArrayList<>();
    for (Future<String> f : resultats) {
      codes.add(f.get(30, TimeUnit.SECONDS));
    }
    executeur.shutdown();

    assertThat(codes).containsExactlyInAnyOrder("OK", "CONFLIT_AFFECTATION");
    assertThat(
            compter(
                "SELECT count(*) FROM affectations WHERE statut <> 'TERMINEE' AND materiel_id = '"
                    + materiel
                    + "'"))
        .isEqualTo(1);
  }

  @Test
  void should_create_delivered_materiels_once_when_event_replayed() throws Exception {
    UUID contrat = UUID.randomUUID();
    ContratPayload payload =
        new ContratPayload(
            contrat,
            "CTR-2026-042",
            UUID.randomUUID(),
            null,
            CHANTIER_3,
            "Pont de la lagune",
            UUID.fromString("f0000000-0000-0000-0000-000000000001"),
            "Sahel Engins SA",
            "Groupe électrogène 30 kVA",
            "OUTILLAGE",
            3,
            new BigDecimal("13500000"),
            "LIVRE",
            LocalDate.now(clock).minusDays(20),
            LocalDate.now(clock),
            LocalDate.now(clock),
            new BigDecimal("15000"));
    String message =
        mapper.writeValueAsString(
            new EventEnvelope(
                UUID.randomUUID(),
                EventTypes.LIVRAISON_EFFECTUEE,
                1,
                Instant.now(clock),
                "ms-achats",
                "Contrat",
                contrat.toString(),
                null,
                Actor.SYSTEME,
                mapper.valueToTree(payload)));

    livraisons.recevoir(message);
    livraisons.recevoir(message);
    livraisons.recevoir(message.replace(EventTypes.LIVRAISON_EFFECTUEE, EventTypes.CONTRAT_SIGNE));

    assertThat(compter("SELECT count(*) FROM materiels WHERE contrat_id = '" + contrat + "'"))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForList(
                "SELECT a.statut || ':' || a.chantier_id FROM affectations a"
                    + " JOIN materiels m ON m.id = a.materiel_id WHERE m.contrat_id = ?",
                String.class,
                contrat))
        .containsOnly("EN_ATTENTE_RECEPTION:" + CHANTIER_3)
        .hasSize(3);
    assertThat(
            jdbc.queryForList(
                "SELECT code FROM materiels WHERE contrat_id = ?", String.class, contrat))
        .allMatch(code -> code.matches("OUT-\\d{4}-\\d{4}"));
  }

  @Test
  void should_raise_each_alert_only_once() throws Exception {
    // Données propres au test : le jeu de démonstration ne contient ni affectation ni maintenance
    // due.
    LocalDate aujourdhui = LocalDate.now(clock);
    String finProche = creerMateriel();
    mvc.perform(affecter(finProche, CHANTIER_1.toString(), aujourdhui, aujourdhui.plusDays(1)))
        .andExpect(status().isCreated());
    String usage = creerMateriel();
    mvc.perform(
            json(post("/api/materiels/{id}/heures", usage), Map.of("heuresFonctionnement", 1800))
                .with(rm()))
        .andExpect(status().is2xxSuccessful());

    assertThat(alertes.alerterFinsAffectation()).isGreaterThanOrEqualTo(1);
    assertThat(alertes.alerterFinsAffectation()).isZero();
    assertThat(alertes.alerterMaintenancesPreventives()).isGreaterThanOrEqualTo(1);
    assertThat(alertes.alerterMaintenancesPreventives()).isZero();
    assertThat(
            compter(
                "SELECT count(*) FROM outbox_event WHERE event_type = 'FIN_AFFECTATION_PROCHAINE'"))
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void should_reform_in_two_steps_and_keep_dg_read_only_R12_R13() throws Exception {
    String materielId = creerMateriel();
    mvc.perform(
            json(post("/api/materiels/{id}/reforme/demande", materielId), Map.of("motif", "Usure"))
                .with(rm()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reformeDemandee").value(true));
    mvc.perform(
            json(patch("/api/materiels/{id}/reforme/valider", materielId), Map.of("approuve", true))
                .with(rm()))
        .andExpect(status().isForbidden());
    mvc.perform(
            json(patch("/api/materiels/{id}/reforme/valider", materielId), Map.of("approuve", true))
                .with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etat").value("REFORME"));
    mvc.perform(
            affecter(materielId, CHANTIER_1.toString(), LocalDate.now(clock), LocalDate.now(clock)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MATERIEL_NON_DISPONIBLE"));
    mvc.perform(
            json(
                    post("/api/materiels/{id}/affecter", materielId),
                    Map.of(
                        "chantierId", CHANTIER_1.toString(),
                        "dateDebut", LocalDate.now(clock).toString(),
                        "dateFinPrevue", LocalDate.now(clock).toString()))
                .with(CseJwt.as(DG, Roles.DIRECTION_GENERALE)))
        .andExpect(status().isForbidden());
  }

  private String creerChantier() throws Exception {
    int n = COMPTEUR.incrementAndGet();
    Map<String, Object> corps =
        Map.of(
            "code",
            "CH-IT-" + n + "-" + UUID.randomUUID().toString().substring(0, 4),
            "nom",
            "Chantier d'intégration " + n,
            "localisation",
            "Rufisque",
            "region",
            "Dakar",
            "latitude",
            14.7,
            "longitude",
            -17.2,
            "budget",
            1_000_000_000,
            "montantMarche",
            1_200_000_000,
            "dateDebut",
            LocalDate.now(clock).minusDays(10).toString());
    return JsonPath.read(
        mvc.perform(json(post("/api/chantiers"), corps).with(rm()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  private String creerMateriel() throws Exception {
    Map<String, Object> corps =
        Map.of(
            "code",
            "IT-" + UUID.randomUUID().toString().substring(0, 8),
            "designation",
            "Pelle d'essai",
            "categorie",
            "MATERIEL_ROULANT",
            "typeEnginId",
            "e0000000-0000-0000-0000-000000000001",
            "tarifJournalier",
            300000,
            "heuresFonctionnement",
            1200);
    return JsonPath.read(
        mvc.perform(json(post("/api/materiels"), corps).with(rm()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.etat").value("DISPONIBLE"))
            .andExpect(jsonPath("$.seuilMaintenanceHeures").value(500))
            .andReturn()
            .getResponse()
            .getContentAsString(),
        "$.id");
  }

  private MockHttpServletRequestBuilder affecter(
      String materielId, String chantierId, LocalDate debut, LocalDate fin) throws Exception {
    return json(
            post("/api/materiels/{id}/affecter", materielId),
            Map.of(
                "chantierId", chantierId,
                "dateDebut", debut.toString(),
                "dateFinPrevue", fin.toString()))
        .with(rm());
  }

  private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, Object corps)
      throws Exception {
    return requete
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(corps));
  }

  private static RequestPostProcessor rm() {
    return CseJwt.as(RM, Roles.RESPONSABLE_MATERIEL);
  }

  private long compter(String sql) {
    Long n = jdbc.queryForObject(sql, Long.class);
    return n == null ? 0 : n;
  }
}
