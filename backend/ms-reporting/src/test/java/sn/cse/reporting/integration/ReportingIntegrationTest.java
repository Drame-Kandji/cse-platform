package sn.cse.reporting.integration;

import static java.math.BigDecimal.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sn.cse.common.security.Roles;
import sn.cse.common.test.CseJwt;
import sn.cse.common.test.PostgresTestConfiguration;
import sn.cse.events.Actor;
import sn.cse.events.EventEnvelope;
import sn.cse.events.EventTypes;
import sn.cse.events.payload.AffectationPayload;
import sn.cse.events.payload.AppelOffrePayload;
import sn.cse.events.payload.BesoinPayload;
import sn.cse.events.payload.ChantierPayload;
import sn.cse.events.payload.ContratPayload;
import sn.cse.events.payload.FacturePayload;
import sn.cse.events.payload.FournisseurPayload;
import sn.cse.events.payload.MaterielPayload;
import sn.cse.reporting.infrastructure.messaging.ReportingEventListener;
import sn.cse.reporting.infrastructure.scheduling.InstantaneParcJob;

/**
 * Bout en bout sur PostgreSQL réel : enveloppes JSON remises directement aux consommateurs Kafka
 * (sans broker), migration et SQL des vues, idempotence, API sécurisée, exports et instantané du
 * contrat OpenAPI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class ReportingIntegrationTest {

  private static final Path SNAPSHOT = Path.of("src/test/resources/openapi.json");
  private static final Instant MAINTENANT = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  private static final LocalDate AUJ = LocalDate.ofInstant(MAINTENANT, ZoneOffset.UTC);
  private static final UUID C = UUID.randomUUID();
  private static final UUID M = UUID.randomUUID();
  private static final UUID F = UUID.randomUUID();
  private static final UUID AO = UUID.randomUUID();
  private static final UUID K = UUID.randomUUID();

  @Autowired private ReportingEventListener listener;
  @Autowired private InstantaneParcJob job;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapper;
  @Autowired private MockMvc mvc;

  private String json(UUID eventId, String type, Object payload, Instant quand) throws Exception {
    return mapper.writeValueAsString(
        new EventEnvelope(
            eventId,
            type,
            1,
            quand,
            "ms-test",
            "Agregat",
            UUID.randomUUID().toString(),
            "corr",
            new Actor("u", "Utilisateur", List.of("RESPONSABLE_MATERIEL")),
            mapper.valueToTree(payload)));
  }

  private String json(String type, Object payload, Instant quand) throws Exception {
    return json(UUID.randomUUID(), type, payload, quand);
  }

  private static Instant ilYa(long jours) {
    return MAINTENANT.minus(Duration.ofDays(jours));
  }

  private void alimenter() throws Exception {
    listener.surSuivi(
        json(
            EventTypes.CHANTIER_CREE,
            new ChantierPayload(
                C,
                "CH-IT",
                "Chantier IT",
                "Dakar",
                "EN_COURS",
                valueOf(10_000_000),
                valueOf(36_600_000),
                null,
                AUJ.minusDays(100),
                AUJ.plusDays(265)),
            ilYa(100)));
    listener.surSuivi(
        json(
            EventTypes.MATERIEL_ENREGISTRE,
            new MaterielPayload(
                M,
                "M-IT",
                "Pelle",
                "MATERIEL_ROULANT",
                "DISPONIBLE",
                null,
                null,
                valueOf(100_000),
                valueOf(80_000_000),
                0,
                null,
                null,
                null),
            ilYa(10)));
    String affecte =
        json(
            UUID.randomUUID(),
            EventTypes.MATERIEL_AFFECTE,
            new AffectationPayload(
                UUID.randomUUID(),
                M,
                "M-IT",
                "Pelle",
                "MATERIEL_ROULANT",
                valueOf(100_000),
                C,
                "Chantier IT",
                null,
                null,
                AUJ.minusDays(9),
                AUJ.plusDays(30),
                null,
                "EN_COURS",
                null,
                null),
            ilYa(9));
    listener.surSuivi(affecte);
    listener.surSuivi(affecte);
    listener.surBesoins(
        json(
            EventTypes.BESOIN_SOUMIS,
            new BesoinPayload(
                UUID.randomUUID(),
                "BES-IT",
                C,
                "Chantier IT",
                UUID.randomUUID(),
                "Chef",
                "Pelle",
                "MATERIEL_ROULANT",
                1,
                null,
                "URGENT",
                "EN_ATTENTE",
                AUJ.plusDays(10),
                30,
                null,
                List.of()),
            ilYa(5)));
    listener.surFournisseurs(
        json(
            EventTypes.FOURNISSEUR_VALIDE,
            new FournisseurPayload(
                F,
                "Sahel Engins SA",
                "c@sahel.sn",
                List.of("MATERIEL_ROULANT"),
                "ACTIF",
                valueOf(82.5),
                null),
            ilYa(60)));
    listener.surAchats(
        json(
            EventTypes.AO_PUBLIE,
            new AppelOffrePayload(
                AO,
                "AO-IT",
                "Pelle",
                "APPEL_OFFRES",
                null,
                C,
                "MATERIEL_ROULANT",
                1,
                ilYa(20),
                "PUBLIE",
                0,
                List.of(F),
                null,
                null,
                null,
                false),
            ilYa(30)));
    listener.surAchats(
        json(EventTypes.ENGAGEMENT_VALIDE, contrat("ENGAGEMENT_VALIDE", null), ilYa(14)));
    listener.surAchats(
        json(EventTypes.LIVRAISON_EFFECTUEE, contrat("LIVRE", AUJ.minusDays(4)), ilYa(4)));
    listener.surAchats(
        json(
            EventTypes.FACTURE_PAYEE,
            new FacturePayload(
                UUID.randomUUID(), "FA-IT", K, F, valueOf(1_000_000), "PAYEE", AUJ.plusDays(30)),
            ilYa(2)));
    listener.surAchats(
        json(
            "TYPE_FUTUR",
            new ChantierPayload(C, null, null, null, null, null, null, null, null, null),
            ilYa(1)));
  }

  private static ContratPayload contrat(String statut, LocalDate reelle) {
    return new ContratPayload(
        K,
        "CTR-IT",
        AO,
        null,
        C,
        "Chantier IT",
        F,
        "Sahel Engins SA",
        "Pelle",
        "MATERIEL_ROULANT",
        1,
        valueOf(3_000_000),
        statut,
        AUJ.minusDays(15),
        AUJ.minusDays(3),
        reelle,
        null);
  }

  @Test
  void should_projeter_les_evenements_et_servir_les_tableaux() throws Exception {
    alimenter();

    assertThat(jdbc.queryForObject("SELECT count(*) FROM affectations", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM processed_event WHERE consumer = 'ms-reporting.cse.suivi.events'",
                Integer.class))
        .isEqualTo(3);
    // Les références créées par les besoins / contrats n'écrasent pas le chantier complet.
    assertThat(
            jdbc.queryForObject(
                "SELECT complet FROM chantiers WHERE code = 'CH-IT'", Boolean.class))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT raison_sociale FROM fournisseurs WHERE complet", String.class))
        .isEqualTo("Sahel Engins SA");
    assertThat(jdbc.queryForObject("SELECT etat::text FROM materiels", String.class))
        .isEqualTo("AFFECTE");

    mvc.perform(
            get("/api/reporting/kpis").with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.profil").value("DG"))
        .andExpect(jsonPath("$.parc.total").value(1))
        .andExpect(jsonPath("$.parc.affectes").value(1))
        .andExpect(jsonPath("$.parc.tauxUtilisation").isNumber())
        .andExpect(jsonPath("$.besoins.total").value(1))
        .andExpect(jsonPath("$.besoins.parPriorite.URGENT").value(1))
        .andExpect(jsonPath("$.finances.montantEngage").value(3000000))
        .andExpect(jsonPath("$.finances.montantPaye").value(1000000))
        .andExpect(jsonPath("$.achats.delaiMoyenApprovisionnementJours").value(26.0))
        .andExpect(jsonPath("$.achats.tauxLivraisonALHeure").value(100.0))
        .andExpect(jsonPath("$.fournisseurs.actifs").value(1))
        .andExpect(jsonPath("$.fournisseurs.top[0].scorePerformance").value(82.5))
        .andExpect(jsonPath("$.chantiers[0].chantierNom").value("Chantier IT"))
        // 10 jours (J-9 → J) × 100 000 : l'affectation rejouée n'est comptée qu'une fois.
        .andExpect(jsonPath("$.chantiers[0].coutExploitation").value(1000000))
        .andExpect(jsonPath("$.chantiers[0].materielsAffectes").value(1));

    mvc.perform(
            get("/api/reporting/kpis")
                .param("chantierId", C.toString())
                .with(CseJwt.chef(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CHANTIER_NON_AUTORISE"));
    mvc.perform(get("/api/reporting/kpis").with(CseJwt.chef(UUID.randomUUID(), C)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parc.total").value(1))
        .andExpect(jsonPath("$.besoins.total").value(1));

    job.photographierAujourdhui();
    mvc.perform(
            get("/api/reporting/series")
                .param("indicateur", "TAUX_DISPONIBILITE")
                .param("granularite", "JOUR")
                .param("du", AUJ.toString())
                .param("au", AUJ.toString())
                .with(CseJwt.as(UUID.randomUUID(), Roles.RESPONSABLE_MATERIEL)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].periode").value(AUJ.toString()))
        .andExpect(jsonPath("$[0].valeur").value(0.0));

    mvc.perform(
            get("/api/reporting/export")
                .param("format", "xlsx")
                .with(CseJwt.as(UUID.randomUUID(), Roles.DIRECTION_GENERALE)))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"tableau-de-bord-DG-" + AUJ + ".xlsx\""));
  }

  @Test
  void should_rejeter_sans_rejeu_when_message_illisible() {
    assertThatThrownBy(() -> listener.surSuivi("{pas du json"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_respecter_instantane_openapi() throws Exception {
    String brut =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode actuel = mapper.readTree(brut);
    if (Boolean.getBoolean("openapi.regenerer")) {
      Files.writeString(
          SNAPSHOT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(actuel) + "\n");
    }
    assertThat(SNAPSHOT)
        .as("Instantané OpenAPI absent : lancer une fois avec -Dopenapi.regenerer=true")
        .exists();
    assertThat(actuel)
        .as(
            "Le contrat OpenAPI a changé : régénérer volontairement src/test/resources/openapi.json")
        .isEqualTo(mapper.readTree(Files.readString(SNAPSHOT)));
  }
}
