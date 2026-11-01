package sn.cse.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.e2e.support.Api.anonyme;
import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;
import static sn.cse.e2e.support.Donnees.FOURNISSEUR_SAHEL;
import static sn.cse.e2e.support.Donnees.suffixe;
import static sn.cse.e2e.support.ProblemDetailAssert.problemDetail;

import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  @DisplayName("Le fournisseur ne voit que ses contrats : celui d'un autre fournisseur renvoie 404")
  void contrats_cloisonnesParFournisseur() {
    JsonPath mesContrats =
        attendu(en(Compte.FOURNISSEUR).queryParam("size", 50).get("/api/portail/contrats"), 200)
            .jsonPath();
    assertThat(mesContrats.<String>getList("content.fournisseurId"))
        .isNotEmpty()
        .containsOnly(FOURNISSEUR_SAHEL);
    String monContrat = mesContrats.getString("content[0].id");
    assertThat(
            attendu(en(Compte.FOURNISSEUR).get("/api/portail/contrats/{id}", monContrat), 200)
                .<String>path("fournisseurId"))
        .isEqualTo(FOURNISSEUR_SAHEL);

    // Contrat d'un autre fournisseur (jeu de démonstration, vu par le RA).
    List<Map<String, Object>> tous =
        attendu(en(Compte.RA).queryParam("size", 50).get("/api/contrats"), 200)
            .jsonPath()
            .getList("content");
    String autreContrat =
        tous.stream()
            .filter(c -> !FOURNISSEUR_SAHEL.equals(c.get("fournisseurId")))
            .map(c -> (String) c.get("id"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("aucun contrat d'un autre fournisseur"));
    problemDetail(
        en(Compte.FOURNISSEUR).get("/api/portail/contrats/{id}", autreContrat),
        404,
        "CONTRAT_INTROUVABLE");
    // Une action sur la ressource d'un autre fournisseur ne révèle pas non plus son existence.
    assertThat(
            en(Compte.FOURNISSEUR)
                .post("/api/portail/contrats/{id}/signer", autreContrat)
                .statusCode())
        .isEqualTo(404);

    // Les routes internes restent fermées au rôle FOURNISSEUR.
    problemDetail(en(Compte.FOURNISSEUR).get("/api/contrats"), 403, "ACCES_REFUSE");
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
