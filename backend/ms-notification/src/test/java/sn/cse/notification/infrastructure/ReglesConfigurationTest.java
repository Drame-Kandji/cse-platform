package sn.cse.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import sn.cse.events.EventTypes;
import sn.cse.notification.domain.model.EvenementMetier;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.domain.model.NotificationPlanifiee;
import sn.cse.notification.domain.model.RegleRoutage;
import sn.cse.notification.domain.service.FormateurValeurs;
import sn.cse.notification.domain.service.MoteurRoutage;
import sn.cse.notification.infrastructure.config.NotificationProperties;
import sn.cse.notification.infrastructure.config.ReglesRoutageConfiguration;

/** Vérifie les règles réellement livrées dans application.yml. */
class ReglesConfigurationTest {

  private static List<RegleRoutage> regles;
  private static MoteurRoutage moteur;

  @BeforeAll
  static void charger() throws IOException {
    StandardEnvironment env = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("application", new ClassPathResource("application.yml"))
        .forEach(env.getPropertySources()::addLast);
    NotificationProperties props =
        Binder.get(env).bindOrCreate("cse.notification", NotificationProperties.class);
    ReglesRoutageConfiguration config = new ReglesRoutageConfiguration(props);
    regles = config.regles();
    moteur =
        new MoteurRoutage(regles, new FormateurValeurs(config.libelles()), config.valeurAbsente());
  }

  @Test
  void should_cover_all_required_event_types() {
    Set<String> couverts = regles.stream().map(RegleRoutage::evenement).collect(Collectors.toSet());
    assertThat(couverts)
        .contains(
            EventTypes.BESOIN_SOUMIS,
            EventTypes.BESOIN_VALIDE,
            EventTypes.BESOIN_REJETE,
            EventTypes.BESOIN_AFFECTE_DIRECT,
            EventTypes.BESOIN_EN_ACHAT,
            EventTypes.MATERIEL_AFFECTE,
            EventTypes.FIN_AFFECTATION_PROCHAINE,
            EventTypes.PANNE_SIGNALEE,
            EventTypes.ALERTE_MAINTENANCE,
            EventTypes.MATERIEL_REFORME_DEMANDEE,
            EventTypes.AO_PUBLIE,
            EventTypes.ATTRIBUTION_PROPOSEE,
            EventTypes.MARCHE_ATTRIBUE,
            EventTypes.CONTRAT_SIGNE,
            EventTypes.CONTRAT_SIGNATURE_REFUSEE,
            EventTypes.LIVRAISON_DEMARREE,
            EventTypes.ENGAGEMENT_DEMANDE,
            EventTypes.ENGAGEMENT_VALIDE,
            EventTypes.ENGAGEMENT_REJETE,
            EventTypes.LIVRAISON_EFFECTUEE,
            EventTypes.ALERTE_RETARD_LIVRAISON,
            EventTypes.FACTURE_RECUE,
            EventTypes.FOURNISSEUR_BLACKLISTAGE_DEMANDE,
            EventTypes.DOCUMENT_EXPIRE,
            EventTypes.SCORE_RECALCULE,
            EventTypes.LITIGE_OUVERT);
  }

  @ParameterizedTest
  @CsvSource({
    "BESOIN_SOUMIS,RESPONSABLE_MATERIEL",
    "ALERTE_MAINTENANCE,RESPONSABLE_MATERIEL",
    "MATERIEL_REFORME_DEMANDEE,DIRECTION_GENERALE",
    "MARCHE_ATTRIBUE,RESPONSABLE_ACHATS",
    "CONTRAT_SIGNE,RESPONSABLE_ACHATS",
    "CONTRAT_SIGNATURE_REFUSEE,RESPONSABLE_ACHATS",
    "ENGAGEMENT_DEMANDE,DAF",
    "ENGAGEMENT_VALIDE,RESPONSABLE_ACHATS",
    "ENGAGEMENT_REJETE,RESPONSABLE_ACHATS",
    "ALERTE_RETARD_LIVRAISON,RESPONSABLE_ACHATS",
    "FACTURE_RECUE,DAF",
    "FOURNISSEUR_BLACKLISTAGE_DEMANDE,DIRECTION_GENERALE",
    "DOCUMENT_EXPIRE,RESPONSABLE_ACHATS",
    "LITIGE_OUVERT,RESPONSABLE_ACHATS"
  })
  void should_route_event_to_expected_role(String type, String role) {
    assertThat(moteur.router(evt(type, Map.of())))
        .flatExtracting(p -> List.copyOf(p.roles()))
        .containsExactly(role);
  }

  @Test
  void should_route_besoin_outcomes_to_demandeur() {
    UUID demandeur = UUID.randomUUID();
    for (String type :
        List.of("BESOIN_VALIDE", "BESOIN_REJETE", "BESOIN_AFFECTE_DIRECT", "BESOIN_EN_ACHAT")) {
      assertThat(moteur.router(evt(type, Map.of("demandeurId", demandeur.toString()))))
          .singleElement()
          .satisfies(p -> assertThat(p.utilisateurs()).containsExactly(demandeur));
    }
    NotificationPlanifiee rejet =
        moteur
            .router(
                evt(
                    "BESOIN_REJETE",
                    Map.of(
                        "demandeurId",
                        demandeur.toString(),
                        "reference",
                        "BES-2026-0012",
                        "motif",
                        "Doublon",
                        "besoinId",
                        "b1",
                        "designation",
                        "Pelle",
                        "chantierNom",
                        "Thiès")))
            .get(0);
    assertThat(rejet.titre()).isEqualTo("Besoin BES-2026-0012 rejeté");
    assertThat(rejet.message()).contains("Motif : Doublon");
    assertThat(rejet.lien()).isEqualTo("/besoins/b1");
    assertThat(rejet.email()).isTrue();
  }

  @Test
  void should_route_panne_with_level_depending_on_gravity() {
    assertThat(moteur.router(evt("PANNE_SIGNALEE", Map.of("gravite", "BLOQUANTE"))))
        .singleElement()
        .extracting(NotificationPlanifiee::niveau)
        .isEqualTo(NiveauNotification.CRITIQUE);
    NotificationPlanifiee mineure =
        moteur.router(evt("PANNE_SIGNALEE", Map.of("gravite", "MINEURE"))).get(0);
    assertThat(mineure.niveau()).isEqualTo(NiveauNotification.ALERTE);
    assertThat(mineure.titre()).startsWith("Panne mineure");
  }

  @Test
  void should_add_dg_to_attribution_only_when_approval_required() {
    assertThat(moteur.router(evt("ATTRIBUTION_PROPOSEE", Map.of("approbationDgRequise", false))))
        .flatExtracting(p -> List.copyOf(p.roles()))
        .containsExactly("RESPONSABLE_MATERIEL");
    assertThat(
            moteur.router(
                evt(
                    "ATTRIBUTION_PROPOSEE",
                    Map.of("approbationDgRequise", true, "montantAttribue", 180000000))))
        .flatExtracting(p -> List.copyOf(p.roles()))
        .containsExactlyInAnyOrder("RESPONSABLE_MATERIEL", "DIRECTION_GENERALE");
  }

  @Test
  void should_alert_on_score_only_when_supplier_service_flags_it() {
    assertThat(
            moteur.router(
                evt(
                    "SCORE_RECALCULE",
                    Map.of("scorePerformance", 72, "motif", "Recalcul après évaluation"))))
        .isEmpty();
    assertThat(moteur.router(evt("SCORE_RECALCULE", Map.of("scorePerformance", 72)))).isEmpty();
    NotificationPlanifiee p =
        moteur
            .router(
                evt(
                    "SCORE_RECALCULE",
                    Map.of(
                        "scorePerformance",
                        48.5,
                        "raisonSociale",
                        "Sahel Engins SA",
                        "motif",
                        "ALERTE : score inférieur au seuil de 60")))
            .get(0);
    assertThat(p.message())
        .contains("48,5/100")
        .contains("ALERTE : score inférieur au seuil de 60");
    assertThat(p.roles()).containsExactly("RESPONSABLE_ACHATS");
    assertThat(moteur.router(evt("FOURNISSEUR_BLACKLISTAGE_REFUSE", Map.of()))).hasSize(1);
  }

  @Test
  void should_route_affectation_and_livraison_to_chefs_of_chantier() {
    String chantier = UUID.randomUUID().toString();
    assertThat(moteur.router(evt("MATERIEL_AFFECTE", Map.of("chantierId", chantier))))
        .singleElement()
        .satisfies(p -> assertThat(p.chantierPourChefs()).hasToString(chantier));
    assertThat(
            moteur.router(
                evt("LIVRAISON_EFFECTUEE", Map.of("chantierId", chantier, "contratId", "c1"))))
        .hasSize(2)
        .anySatisfy(p -> assertThat(p.roles()).containsExactly("RESPONSABLE_MATERIEL"))
        .anySatisfy(p -> assertThat(p.chantierPourChefs()).hasToString(chantier));
    assertThat(moteur.router(evt("FIN_AFFECTATION_PROCHAINE", Map.of("chantierId", chantier))))
        .hasSize(2);
  }

  @Test
  void should_notify_ra_of_supplier_signature_refusal_with_motif() {
    NotificationPlanifiee refus =
        moteur
            .router(
                evt(
                    "CONTRAT_SIGNATURE_REFUSEE",
                    Map.of(
                        "contratId",
                        "k1",
                        "numero",
                        "CTR-2026-007",
                        "fournisseurNom",
                        "Sahel Engins SA",
                        "designation",
                        "Camion malaxeur 8 m³",
                        "statut",
                        "BROUILLON",
                        "motif",
                        "Pénalités trop élevées")))
            .get(0);
    assertThat(refus.roles()).containsExactly("RESPONSABLE_ACHATS");
    assertThat(refus.niveau()).isEqualTo(NiveauNotification.ALERTE);
    assertThat(refus.titre()).isEqualTo("Signature refusée : CTR-2026-007");
    assertThat(refus.message())
        .contains("Sahel Engins SA a refusé de signer le contrat CTR-2026-007")
        .contains("Motif : Pénalités trop élevées");
    assertThat(refus.lien()).isEqualTo("/contrats/k1");
    assertThat(refus.email()).isTrue();
    // L'envoi en signature concerne le fournisseur (portail) : aucune notification interne.
    assertThat(moteur.router(evt(EventTypes.CONTRAT_ENVOYE_SIGNATURE, Map.of()))).isEmpty();
  }

  @Test
  void should_notify_ra_and_rm_only_when_supplier_declares_shipment() {
    assertThat(moteur.router(evt("LIVRAISON_DEMARREE", Map.of("contratId", "k1")))).isEmpty();
    List<NotificationPlanifiee> expedition =
        moteur.router(
            evt(
                "LIVRAISON_DEMARREE",
                Map.of(
                    "contratId",
                    "k1",
                    "numero",
                    "CTR-2026-005",
                    "fournisseurNom",
                    "Sahel Engins SA",
                    "quantite",
                    1,
                    "designation",
                    "Finisseur d'enrobés",
                    "dateExpedition",
                    "2026-11-02",
                    "dateLivraisonPrevue",
                    "2026-11-20")));
    assertThat(expedition)
        .hasSize(2)
        .flatExtracting(p -> List.copyOf(p.roles()))
        .containsExactlyInAnyOrder("RESPONSABLE_ACHATS", "RESPONSABLE_MATERIEL");
    assertThat(expedition)
        .allSatisfy(
            p ->
                assertThat(p.message())
                    .contains("le 02/11/2026 (contrat CTR-2026-005)")
                    .contains("Livraison prévue le 20/11/2026"));
  }

  private static EvenementMetier evt(String type, Map<String, Object> donnees) {
    return new EvenementMetier(
        UUID.randomUUID(), type, "a", java.time.Instant.EPOCH, "Fatou Sarr", donnees);
  }
}
