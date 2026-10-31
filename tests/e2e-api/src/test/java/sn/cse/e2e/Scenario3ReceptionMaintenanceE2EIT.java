package sn.cse.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;
import static sn.cse.e2e.support.Donnees.CHANTIER_DAKAR;
import static sn.cse.e2e.support.Donnees.CHANTIER_SAINT_LOUIS;
import static sn.cse.e2e.support.Donnees.CHANTIER_THIES;
import static sn.cse.e2e.support.Donnees.attendreNotification;
import static sn.cse.e2e.support.Donnees.creerMateriel;
import static sn.cse.e2e.support.Donnees.jour;
import static sn.cse.e2e.support.Donnees.reformer;
import static sn.cse.e2e.support.Donnees.suffixe;
import static sn.cse.e2e.support.ProblemDetailAssert.problemDetail;

import io.restassured.path.json.JsonPath;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sn.cse.e2e.support.Compte;

/**
 * Scénario critique 3 (spec §6.2) : confirmation de réception par le chef, panne signalée,
 * maintenance planifiée / démarrée / clôturée par le RM avec retour du matériel à l'état AFFECTE ou
 * DISPONIBLE.
 */
@DisplayName("Scénario 3 — Réception et clôture de maintenance (API)")
class Scenario3ReceptionMaintenanceE2EIT {

  @Test
  @DisplayName(
      "Réception confirmée par le chef, panne vue par le RM, OT correctif, R04 pendant la maintenance,"
          + " clôture -> AFFECTE")
  void reception_panne_maintenance_retourAffecte() {
    String sfx = suffixe();
    String designation = "Compacteur E2E API " + sfx;
    String materielId =
        creerMateriel("E2E-API-CMP-" + sfx, designation, "MATERIEL_ROULANT").getString("id");
    String affectationId =
        attendu(
                en(Compte.RM)
                    .body(
                        Map.of(
                            "chantierId", CHANTIER_DAKAR,
                            "dateDebut", jour(0),
                            "dateFinPrevue", jour(20)))
                    .post("/api/materiels/{id}/affecter", materielId),
                201)
            .path("id");
    attendreNotification(Compte.CHEF, "Matériel E2E-API-CMP-" + sfx + " affecté");

    // Un chef ne peut pas confirmer la réception hors de ses chantiers ; sur le sien, oui.
    JsonPath reception =
        attendu(
                en(Compte.CHEF)
                    .body(
                        Map.of(
                            "affectationId",
                            affectationId,
                            "observations",
                            "Reçu en bon état (E2E)"))
                    .post("/api/materiels/{id}/confirmer-reception", materielId),
                200)
            .jsonPath();
    assertThat(reception.getString("statut")).isEqualTo("EN_COURS");
    assertThat(reception.getString("dateReception")).isNotNull();

    // Panne signalée par le chef, visible du RM (liste + notification).
    String panneId =
        attendu(
                en(Compte.CHEF)
                    .body(
                        Map.of(
                            "materielId",
                            materielId,
                            "gravite",
                            "MAJEURE",
                            "description",
                            "Fuite hydraulique (E2E " + sfx + ")"))
                    .post("/api/pannes"),
                201)
            .path("id");
    JsonPath pannes =
        attendu(en(Compte.RM).queryParam("materielId", materielId).get("/api/pannes"), 200)
            .jsonPath();
    assertThat(pannes.<String>getList("content.id")).containsExactly(panneId);
    assertThat(pannes.getString("content[0].statut")).isEqualTo("SIGNALEE");
    assertThat(pannes.getString("content[0].signaleParNom")).isEqualTo("Mamadou Diop");
    attendreNotification(Compte.RM, designation);

    // OT correctif lié à la panne, démarré : matériel EN_MAINTENANCE.
    String maintenanceId =
        attendu(
                en(Compte.RM)
                    .body(
                        Map.of(
                            "materielId",
                            materielId,
                            "type",
                            "CORRECTIVE",
                            "description",
                            "Remplacement du joint de vérin",
                            "datePlanifiee",
                            jour(0),
                            "technicien",
                            "Atelier central",
                            "panneId",
                            panneId))
                    .post("/api/maintenances"),
                201)
            .path("id");
    assertThat(
            attendu(en(Compte.RM).patch("/api/maintenances/{id}/demarrer", maintenanceId), 200)
                .<String>path("statut"))
        .isEqualTo("EN_COURS");
    assertThat(etat(materielId)).isEqualTo("EN_MAINTENANCE");

    // R04 : un matériel non DISPONIBLE ne peut pas être affecté.
    JsonPath r04 =
        problemDetail(
            en(Compte.RM)
                .body(
                    Map.of(
                        "chantierId",
                        CHANTIER_THIES,
                        "dateDebut",
                        jour(40),
                        "dateFinPrevue",
                        jour(45)))
                .post("/api/materiels/{id}/affecter", materielId),
            422,
            "MATERIEL_NON_DISPONIBLE");
    assertThat(r04.getString("detail")).contains("R04");

    JsonPath cloture =
        attendu(
                en(Compte.RM)
                    .body(
                        Map.of(
                            "cout", 350_000,
                            "observations", "Joint remplacé",
                            "heuresCompteur", 120))
                    .patch("/api/maintenances/{id}/cloturer", maintenanceId),
                200)
            .jsonPath();
    assertThat(cloture.getString("statut")).isEqualTo("CLOTUREE");
    assertThat(cloture.getLong("cout")).isEqualTo(350_000L);
    // Affectation toujours en cours : retour à AFFECTE, panne résolue.
    assertThat(etat(materielId)).isEqualTo("AFFECTE");
    assertThat(
            attendu(en(Compte.RM).queryParam("materielId", materielId).get("/api/pannes"), 200)
                .<String>path("content[0].statut"))
        .isEqualTo("RESOLUE");

    // Nettoyage : libération puis réforme du matériel de test.
    attendu(en(Compte.RM).body(Map.of()).post("/api/materiels/{id}/liberer", materielId), 200);
    assertThat(etat(materielId)).isEqualTo("DISPONIBLE");
    reformer(materielId);
  }

  @Test
  @DisplayName("Maintenance préventive d'un matériel DISPONIBLE : clôture -> DISPONIBLE")
  void maintenancePreventive_materielDisponible_retourDisponible() {
    String sfx = suffixe();
    String materielId =
        creerMateriel("E2E-API-GRP-" + sfx, "Groupe E2E API " + sfx, "OUTILLAGE").getString("id");
    String maintenanceId =
        attendu(
                en(Compte.RM)
                    .body(
                        Map.of(
                            "materielId",
                            materielId,
                            "type",
                            "PREVENTIVE",
                            "description",
                            "Vidange 250 h",
                            "datePlanifiee",
                            jour(1)))
                    .post("/api/maintenances"),
                201)
            .path("id");
    attendu(en(Compte.RM).patch("/api/maintenances/{id}/demarrer", maintenanceId), 200);
    assertThat(etat(materielId)).isEqualTo("EN_MAINTENANCE");
    attendu(
        en(Compte.RM)
            .body(Map.of("cout", 45_000))
            .patch("/api/maintenances/{id}/cloturer", maintenanceId),
        200);
    assertThat(etat(materielId)).isEqualTo("DISPONIBLE");
    reformer(materielId);
  }

  @Test
  @DisplayName("Le chef ne confirme pas la réception d'un matériel affecté hors de ses chantiers")
  void chef_horsPerimetre_receptionRefusee() {
    String sfx = suffixe();
    String materielId =
        creerMateriel("E2E-API-HP-" + sfx, "Pompe E2E API " + sfx, "OUTILLAGE").getString("id");
    attendu(
        en(Compte.RM)
            .body(
                Map.of(
                    "chantierId",
                    CHANTIER_SAINT_LOUIS,
                    "dateDebut",
                    jour(0),
                    "dateFinPrevue",
                    jour(5)))
            .post("/api/materiels/{id}/affecter", materielId),
        201);
    int statut =
        en(Compte.CHEF)
            .body(Map.of())
            .post("/api/materiels/{id}/confirmer-reception", materielId)
            .statusCode();
    assertThat(statut).isIn(403, 404);
    attendu(en(Compte.RM).body(Map.of()).post("/api/materiels/{id}/liberer", materielId), 200);
    reformer(materielId);
  }

  private static String etat(String materielId) {
    return attendu(en(Compte.RM).get("/api/materiels/{id}", materielId), 200).path("etat");
  }
}
