package sn.cse.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.e2e.support.Api.anonyme;
import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;
import static sn.cse.e2e.support.Donnees.CHANTIER_DAKAR;
import static sn.cse.e2e.support.Donnees.CHANTIER_SAINT_LOUIS;
import static sn.cse.e2e.support.Donnees.CHANTIER_THIES;
import static sn.cse.e2e.support.Donnees.FOURNISSEUR_SAHEL;
import static sn.cse.e2e.support.Donnees.besoin;
import static sn.cse.e2e.support.Donnees.suffixe;
import static sn.cse.e2e.support.ProblemDetailAssert.problemDetail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sn.cse.e2e.support.Compte;

/**
 * Contrôle d'accès par rôle à travers la passerelle (matrice de sécurité B.13) : chaque refus est
 * un 403 ProblemDetail {@code ACCES_REFUSE}, l'absence de jeton un 401.
 */
@DisplayName("RBAC — matrice d'accès (API)")
class RbacE2EIT {

  @Test
  @DisplayName("Sans jeton : 401 ProblemDetail")
  void sansJeton_401() {
    problemDetail(anonyme().get("/api/besoins"), 401, "NON_AUTHENTIFIE");
  }

  @Test
  @DisplayName("Chef de chantier : aucun accès aux appels d'offres (lecture ni création)")
  void chef_interditSurAppelsOffres() {
    problemDetail(en(Compte.CHEF).get("/api/appels-offres"), 403, "ACCES_REFUSE");
    problemDetail(
        en(Compte.CHEF)
            .body(
                Map.of(
                    "objet",
                    "Tentative chef (E2E " + suffixe() + ")",
                    "type",
                    "DEMANDE_PRIX",
                    "categorie",
                    "OUTILLAGE",
                    "quantite",
                    1,
                    "dateLimite",
                    Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                    "criteres",
                    List.of(Map.of("code", "PRIX", "poids", 100)),
                    "fournisseurIdsInvites",
                    List.of(FOURNISSEUR_SAHEL)))
            .post("/api/appels-offres"),
        403,
        "ACCES_REFUSE");
  }

  @Test
  @DisplayName("R12 : l'administrateur système n'accède pas aux contrats ni aux engagements")
  void admin_interditSurContrats_R12() {
    problemDetail(en(Compte.ADMIN).get("/api/contrats"), 403, "ACCES_REFUSE");
    problemDetail(en(Compte.ADMIN).get("/api/engagements"), 403, "ACCES_REFUSE");
    problemDetail(en(Compte.ADMIN).get("/api/finance/synthese"), 403, "ACCES_REFUSE");
  }

  @Test
  @DisplayName("Direction Générale : lecture des besoins autorisée, aucune écriture")
  void dg_lectureSeuleSurBesoins() {
    attendu(en(Compte.DG).get("/api/besoins"), 200);
    problemDetail(
        en(Compte.DG)
            .body(
                besoin(CHANTIER_DAKAR, "Tentative DG " + suffixe(), "OUTILLAGE", 1, "NORMAL", null))
            .post("/api/besoins"),
        403,
        "ACCES_REFUSE");
    // Besoin réel du chef : la DG ne peut ni le valider (R01) ni le rejeter.
    String besoinId =
        attendu(
                en(Compte.CHEF)
                    .body(
                        besoin(
                            CHANTIER_THIES,
                            "Cible RBAC E2E " + suffixe(),
                            "OUTILLAGE",
                            1,
                            "NORMAL",
                            null))
                    .post("/api/besoins"),
                201)
            .path("id");
    problemDetail(
        en(Compte.DG).body(Map.of()).patch("/api/besoins/{id}/valider", besoinId),
        403,
        "ACCES_REFUSE");
    problemDetail(
        en(Compte.DG)
            .body(Map.of("motif", "Tentative de rejet DG"))
            .patch("/api/besoins/{id}/rejeter", besoinId),
        403,
        "ACCES_REFUSE");
    // R01 : le RA non plus ne valide pas un besoin.
    problemDetail(
        en(Compte.RA).body(Map.of()).patch("/api/besoins/{id}/valider", besoinId),
        403,
        "ACCES_REFUSE");
    // Nettoyage : rejet par le RM.
    attendu(
        en(Compte.RM)
            .body(Map.of("motif", "Besoin de test RBAC (E2E)"))
            .patch("/api/besoins/{id}/rejeter", besoinId),
        200);
  }

  @Test
  @DisplayName("Chef : ne voit et ne soumet des besoins que sur ses chantiers")
  void chef_perimetreChantiers() {
    List<String> chantiers =
        attendu(en(Compte.CHEF).queryParam("size", 50).get("/api/besoins"), 200)
            .jsonPath()
            .getList("content.chantierId");
    assertThat(chantiers)
        .isNotEmpty()
        .allMatch(c -> c.equals(CHANTIER_DAKAR) || c.equals(CHANTIER_THIES));
    problemDetail(
        en(Compte.CHEF)
            .body(
                besoin(
                    CHANTIER_SAINT_LOUIS,
                    "Hors périmètre " + suffixe(),
                    "OUTILLAGE",
                    1,
                    "NORMAL",
                    null))
            .post("/api/besoins"),
        403,
        "CHANTIER_NON_ATTRIBUE");
  }
}
