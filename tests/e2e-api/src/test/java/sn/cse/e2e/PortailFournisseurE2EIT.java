package sn.cse.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.e2e.support.Api.anonyme;
import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;
import static sn.cse.e2e.support.Donnees.FOURNISSEUR_SAHEL;
import static sn.cse.e2e.support.Donnees.FOURNISSEUR_TERANGA;
import static sn.cse.e2e.support.Donnees.suffixe;
import static sn.cse.e2e.support.ProblemDetailAssert.problemDetail;

import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sn.cse.e2e.support.Compte;

/**
 * Portail fournisseur (contrat d'API §13) : cloisonnement par le claim {@code fournisseur_id} (une
 * ressource d'un autre fournisseur renvoie 404, jamais 403) et inscription publique sans jeton.
 */
@DisplayName("Portail fournisseur (API)")
class PortailFournisseurE2EIT {

  @Test
  @DisplayName(
      "Cloisonnement : la demande de prix d'un autre fournisseur et un contrat inconnu renvoient 404")
  void ressources_cloisonneesParFournisseur() {
    // Données créées par le test : la base de démonstration démarre sans appel d'offres ni contrat.
    String sfx = suffixe();
    String pourSahel = demandeDePrix("Sahel " + sfx, FOURNISSEUR_SAHEL);
    String pourTeranga = demandeDePrix("Teranga " + sfx, FOURNISSEUR_TERANGA);

    assertThat(
            attendu(en(Compte.FOURNISSEUR).get("/api/portail/appels-offres/{id}", pourSahel), 200)
                .<Boolean>path("invite"))
        .isTrue();
    problemDetail(
        en(Compte.FOURNISSEUR).get("/api/portail/appels-offres/{id}", pourTeranga),
        404,
        "APPELOFFRE_INTROUVABLE");
    // Déposer une offre sur la ressource d'un autre fournisseur ne révèle pas non plus son existence.
    assertThat(
            en(Compte.FOURNISSEUR)
                .body(Map.of("montantPropose", 10_000_000, "delaiLivraisonJours", 10))
                .post("/api/portail/appels-offres/{id}/offres", pourTeranga)
                .statusCode())
        .isEqualTo(404);

    // Les contrats listés sont tous ceux du fournisseur ; un contrat inconnu renvoie 404.
    JsonPath mesContrats =
        attendu(en(Compte.FOURNISSEUR).queryParam("size", 50).get("/api/portail/contrats"), 200)
            .jsonPath();
    assertThat(mesContrats.<String>getList("content.fournisseurId"))
        .allMatch(FOURNISSEUR_SAHEL::equals);
    problemDetail(
        en(Compte.FOURNISSEUR).get("/api/portail/contrats/{id}", UUID.randomUUID()),
        404,
        "CONTRAT_INTROUVABLE");

    // Les routes internes restent fermées au rôle FOURNISSEUR.
    problemDetail(en(Compte.FOURNISSEUR).get("/api/contrats"), 403, "ACCES_REFUSE");
  }

  private static String demandeDePrix(String objet, String fournisseurInvite) {
    return attendu(
            en(Compte.RA)
                .body(
                    Map.of(
                        "objet",
                        "Demande de prix E2E " + objet,
                        "type",
                        "DEMANDE_PRIX",
                        "categorie",
                        "MATERIEL_ROULANT",
                        "quantite",
                        1,
                        "dateLimite",
                        Instant.now().plus(Duration.ofDays(2)).toString(),
                        "criteres",
                        List.of(Map.of("code", "PRIX", "poids", 100)),
                        "fournisseurIdsInvites",
                        List.of(fournisseurInvite)))
                .post("/api/appels-offres"),
            201)
        .path("id");
  }

  @Test
  @DisplayName(
      "Inscription publique : 201 EN_ATTENTE_VALIDATION, puis 409 sur NINEA ou e-mail déjà connu")
  void inscriptionPublique_puisDoublon() {
    String sfx = suffixe();
    String chiffres =
        String.format(Locale.ROOT, "%09d", Math.floorMod(sfx.hashCode(), 1_000_000_000));
    String raisonSociale = "Fournisseur E2E " + sfx;
    Map<String, Object> inscription =
        Map.of(
            "raisonSociale",
            raisonSociale,
            "ninea",
            chiffres + " 2E7",
            "ville",
            "Thiès",
            "pays",
            "Sénégal",
            "email",
            "inscription-" + sfx.toLowerCase(Locale.ROOT) + "@e2e.cse.sn",
            "contactNom",
            "Contact E2E",
            "specialites",
            List.of("CONSOMMABLE"));

    JsonPath cree =
        attendu(anonyme().body(inscription).post("/api/portail/inscription"), 201).jsonPath();
    assertThat(cree.getString("id")).isNotBlank();
    assertThat(cree.getString("raisonSociale")).isEqualTo(raisonSociale);
    assertThat(cree.getString("statut")).isEqualTo("EN_ATTENTE_VALIDATION");

    // Le RA voit la demande dans les inscriptions à valider.
    assertThat(
            attendu(
                    en(Compte.RA)
                        .queryParam("statut", "EN_ATTENTE_VALIDATION")
                        .queryParam("q", raisonSociale)
                        .get("/api/fournisseurs"),
                    200)
                .jsonPath()
                .<String>getList("content.id"))
        .contains(cree.getString("id"));

    problemDetail(
        anonyme().body(inscription).post("/api/portail/inscription"), 409, "FOURNISSEUR_EXISTANT");
  }
}
