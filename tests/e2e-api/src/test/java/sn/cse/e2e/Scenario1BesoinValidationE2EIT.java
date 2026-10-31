package sn.cse.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;
import static sn.cse.e2e.support.Donnees.CHANTIER_DAKAR;
import static sn.cse.e2e.support.Donnees.CHANTIER_THIES;
import static sn.cse.e2e.support.Donnees.aTerme;
import static sn.cse.e2e.support.Donnees.attendreNotification;
import static sn.cse.e2e.support.Donnees.besoin;
import static sn.cse.e2e.support.Donnees.creerMateriel;
import static sn.cse.e2e.support.Donnees.suffixe;
import static sn.cse.e2e.support.ProblemDetailAssert.problemDetail;

import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sn.cse.e2e.support.Compte;

/**
 * Scénario critique 1 (spec §6.2) : soumission d'un besoin par le chef, arbitrage par le
 * Responsable Matériel (R01), orientation (affectation directe ou achat), rejet motivé (R08),
 * historique et notification du demandeur.
 */
@DisplayName("Scénario 1 — Soumission et validation d'un besoin (API)")
class Scenario1BesoinValidationE2EIT {

  @Test
  @DisplayName("R08 : besoin URGENT sans justification refusé en 422 ProblemDetail")
  void besoinUrgentSansJustification_R08_refuse() {
    problemDetail(
        en(Compte.CHEF)
            .body(
                besoin(
                    CHANTIER_DAKAR,
                    "Pelle R08 " + suffixe(),
                    "MATERIEL_ROULANT",
                    1,
                    "URGENT",
                    null))
            .post("/api/besoins"),
        422,
        "JUSTIFICATION_OBLIGATOIRE");
  }

  @Test
  @DisplayName("Besoin URGENT justifié, validé par le RM, affecté directement depuis le parc")
  void besoinUrgent_valideParRm_affecteDirect() {
    String sfx = suffixe();
    String designation = "Pelle E2E API " + sfx;
    String materielId =
        creerMateriel("E2E-API-PEL-" + sfx, designation, "MATERIEL_ROULANT").getString("id");

    JsonPath soumis =
        attendu(
                en(Compte.CHEF)
                    .body(
                        besoin(
                            CHANTIER_DAKAR,
                            designation,
                            "MATERIEL_ROULANT",
                            1,
                            "URGENT",
                            "Pelle principale en panne, terrassement arrêté"))
                    .post("/api/besoins"),
                201)
            .jsonPath();
    String besoinId = soumis.getString("id");
    String reference = soumis.getString("reference");
    assertThat(soumis.getString("statut")).isEqualTo("EN_ATTENTE");
    assertThat(reference).matches("BES-\\d{4}-\\d{4}");

    // Le RM voit le besoin dans sa file EN_ATTENTE et la disponibilité du parc.
    List<String> enAttente =
        attendu(
                en(Compte.RM)
                    .queryParam("statut", "EN_ATTENTE")
                    .queryParam("q", designation)
                    .get("/api/besoins"),
                200)
            .jsonPath()
            .getList("content.id");
    assertThat(enAttente).containsExactly(besoinId);
    JsonPath dispo =
        attendu(
                en(Compte.RM)
                    .queryParam("categorie", "MATERIEL_ROULANT")
                    .queryParam("designation", designation)
                    .queryParam("quantite", 1)
                    .get("/api/materiels/disponibilite"),
                200)
            .jsonPath();
    assertThat(dispo.getBoolean("disponible")).isTrue();
    assertThat(dispo.<String>getList("materiels.id")).containsExactly(materielId);

    JsonPath valide =
        attendu(
                en(Compte.RM)
                    .body(
                        Map.of("commentaire", "Validé E2E API", "materielIds", List.of(materielId)))
                    .patch("/api/besoins/{id}/valider", besoinId),
                200)
            .jsonPath();
    assertThat(valide.getString("statut")).isEqualTo("AFFECTE_DIRECT");
    assertThat(valide.<String>getList("materielIdsAffectes")).containsExactly(materielId);
    assertThat(valide.getString("validateurNom")).isEqualTo("Ibrahima Fall");

    // L'entrée MATERIEL_AFFECTE provient de l'événement Kafka de ms-suivi : cohérence à terme.
    aTerme("historique complet du besoin")
        .untilAsserted(
            () ->
                assertThat(
                        attendu(en(Compte.CHEF).get("/api/besoins/{id}/historique", besoinId), 200)
                            .jsonPath()
                            .<String>getList("action"))
                    .containsExactly(
                        "SOUMISSION", "VALIDATION", "MATERIEL_AFFECTE", "AFFECTATION_DIRECTE"));

    // ms-suivi : matériel AFFECTE au chantier, affectation en attente de réception liée au besoin.
    JsonPath materiel =
        attendu(en(Compte.RM).get("/api/materiels/{id}", materielId), 200).jsonPath();
    assertThat(materiel.getString("etat")).isEqualTo("AFFECTE");
    assertThat(materiel.getString("chantierActuelId")).isEqualTo(CHANTIER_DAKAR);
    assertThat(materiel.getString("affectations[0].statut")).isEqualTo("EN_ATTENTE_RECEPTION");
    assertThat(materiel.getString("affectations[0].besoinId")).isEqualTo(besoinId);

    attendreNotification(Compte.CHEF, "Besoin " + reference + " satisfait par le parc");
  }

  @Test
  @DisplayName("Besoin sans matériel disponible : validé puis orienté EN_ACHAT, visible des achats")
  void besoinSansStock_valide_enAchat() {
    String designation = "Foreuse E2E API " + suffixe();
    String besoinId =
        attendu(
                en(Compte.CHEF)
                    .body(
                        besoin(CHANTIER_THIES, designation, "MATERIEL_ROULANT", 2, "NORMAL", null))
                    .post("/api/besoins"),
                201)
            .path("id");
    String statut =
        attendu(
                en(Compte.RM)
                    .body(Map.of("commentaire", "Pas de stock"))
                    .patch("/api/besoins/{id}/valider", besoinId),
                200)
            .path("statut");
    assertThat(statut).isEqualTo("EN_ACHAT");
    aTerme("projection BesoinAAcheter dans ms-achats")
        .until(
            () ->
                attendu(en(Compte.RA).queryParam("size", 50).get("/api/besoins-a-acheter"), 200)
                    .jsonPath()
                    .<String>getList("content.besoinId")
                    .contains(besoinId));
  }

  @Test
  @DisplayName("Rejet : motif obligatoire (R08, 422) puis REJETE avec motif et notification")
  void rejet_motifObligatoire_puisRejete() {
    String designation = "Bétonnière E2E API " + suffixe();
    JsonPath soumis =
        attendu(
                en(Compte.CHEF)
                    .body(besoin(CHANTIER_THIES, designation, "OUTILLAGE", 1, "NORMAL", null))
                    .post("/api/besoins"),
                201)
            .jsonPath();
    String besoinId = soumis.getString("id");

    problemDetail(
        en(Compte.RM).body(Map.of("motif", "  ")).patch("/api/besoins/{id}/rejeter", besoinId),
        422,
        "MOTIF_REJET_OBLIGATOIRE");

    JsonPath rejete =
        attendu(
                en(Compte.RM)
                    .body(Map.of("motif", "Doublon avec la bétonnière du chantier voisin"))
                    .patch("/api/besoins/{id}/rejeter", besoinId),
                200)
            .jsonPath();
    assertThat(rejete.getString("statut")).isEqualTo("REJETE");
    assertThat(rejete.getString("motifRejet"))
        .isEqualTo("Doublon avec la bétonnière du chantier voisin");

    // R05 : un besoin traité n'est plus modifiable par son demandeur.
    Map<String, Object> modification =
        besoin(CHANTIER_THIES, designation + " bis", "OUTILLAGE", 1, "NORMAL", null);
    modification.put("version", rejete.getInt("version"));
    problemDetail(
        en(Compte.CHEF).body(modification).put("/api/besoins/{id}", besoinId),
        409,
        "BESOIN_NON_MODIFIABLE");

    attendreNotification(Compte.CHEF, "Besoin " + soumis.getString("reference") + " rejeté");
  }
}
