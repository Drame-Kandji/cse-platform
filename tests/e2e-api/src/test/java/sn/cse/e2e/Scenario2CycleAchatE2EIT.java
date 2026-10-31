package sn.cse.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;
import static sn.cse.e2e.support.Donnees.CHANTIER_DAKAR;
import static sn.cse.e2e.support.Donnees.FOURNISSEUR_SAHEL;
import static sn.cse.e2e.support.Donnees.FOURNISSEUR_TERANGA;
import static sn.cse.e2e.support.Donnees.aTerme;
import static sn.cse.e2e.support.Donnees.attendreNotification;
import static sn.cse.e2e.support.Donnees.besoin;
import static sn.cse.e2e.support.Donnees.jour;
import static sn.cse.e2e.support.Donnees.suffixe;
import static sn.cse.e2e.support.ProblemDetailAssert.problemDetail;

import io.restassured.path.json.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sn.cse.e2e.support.Compte;

/**
 * Scénario critique 2 (spec §6.2) : cycle complet d'achat, du besoin EN_ACHAT à la livraison et à
 * la création des matériels dans ms-suivi (cohérence à terme via {@code LIVRAISON_EFFECTUEE}).
 */
@DisplayName("Scénario 2 · Cycle complet d'achat (API)")
class Scenario2CycleAchatE2EIT {

  private static final List<Map<String, Object>> CRITERES =
      List.of(
          Map.of("code", "PRIX", "poids", 50),
          Map.of("code", "DELAI", "poids", 20),
          Map.of("code", "QUALITE_TECHNIQUE", "poids", 30));

  @Test
  @DisplayName(
      "AO au-delà du seuil (R11) : offres, clôture, notes, attribution RM + DG, contrat, engagement"
          + " DAF (R06), livraison, matériels créés")
  void cycleComplet_auDelaDuSeuil_jusquALaLivraison() {
    String sfx = suffixe();
    String designation = "Niveleuse E2E API " + sfx;

    // Besoin du chef orienté vers les achats (aucun matériel de cette désignation au parc).
    String besoinId =
        attendu(
                en(Compte.CHEF)
                    .body(
                        besoin(CHANTIER_DAKAR, designation, "MATERIEL_ROULANT", 2, "NORMAL", null))
                    .post("/api/besoins"),
                201)
            .path("id");
    assertThat(
            attendu(en(Compte.RM).body(Map.of()).patch("/api/besoins/{id}/valider", besoinId), 200)
                .<String>path("statut"))
        .isEqualTo("EN_ACHAT");
    aTerme("projection BesoinAAcheter")
        .until(
            () ->
                attendu(en(Compte.RA).queryParam("size", 50).get("/api/besoins-a-acheter"), 200)
                    .jsonPath()
                    .<String>getList("content.besoinId")
                    .contains(besoinId));

    // RA publie l'AO depuis le besoin, invite deux fournisseurs ACTIF (R14).
    JsonPath ao =
        attendu(
                en(Compte.RA)
                    .body(
                        Map.of(
                            "besoinId",
                            besoinId,
                            "objet",
                            "Acquisition de deux niveleuses (E2E " + sfx + ")",
                            "type",
                            "APPEL_OFFRES",
                            "categorie",
                            "MATERIEL_ROULANT",
                            "quantite",
                            2,
                            "budgetEstime",
                            400_000_000,
                            "dateLimite",
                            Instant.now().plus(30, ChronoUnit.MINUTES).toString(),
                            "criteres",
                            CRITERES,
                            "fournisseurIdsInvites",
                            List.of(FOURNISSEUR_SAHEL, FOURNISSEUR_TERANGA)))
                    .post("/api/appels-offres"),
                201)
            .jsonPath();
    String aoId = ao.getString("id");
    assertThat(ao.getString("statut")).isEqualTo("PUBLIE");
    assertThat(ao.getString("chantierId")).isEqualTo(CHANTIER_DAKAR);

    String offreSahel = saisirOffre(aoId, FOURNISSEUR_SAHEL, 320_000_000, 20);
    saisirOffre(aoId, FOURNISSEUR_TERANGA, 345_000_000, 30);

    // R10 : offres anonymisées tant que l'AO est publié.
    JsonPath offresPubliees =
        attendu(en(Compte.RA).get("/api/appels-offres/{id}/offres", aoId), 200).jsonPath();
    assertThat(offresPubliees.<String>getList("fournisseurId")).containsOnlyNulls().hasSize(2);
    assertThat(offresPubliees.<String>getList("codeAnonyme")).allMatch(c -> c.startsWith("OFR-"));

    assertThat(
            attendu(en(Compte.RA).post("/api/appels-offres/{id}/cloturer", aoId), 200)
                .<String>path("statut"))
        .isEqualTo("CLOTURE");
    JsonPath offres =
        attendu(en(Compte.RA).get("/api/appels-offres/{id}/offres", aoId), 200).jsonPath();
    assertThat(offres.<String>getList("fournisseurNom"))
        .containsExactlyInAnyOrder("Sahel Engins SA", "Teranga Équipements BTP");
    for (String offreId : offres.<String>getList("id")) {
      attendu(
          en(Compte.RA)
              .body(Map.of("QUALITE_TECHNIQUE", 80))
              .put("/api/appels-offres/{id}/offres/{offreId}/evaluation", aoId, offreId),
          200);
    }
    JsonPath comparatif =
        attendu(en(Compte.RM).get("/api/appels-offres/{id}/comparatif", aoId), 200).jsonPath();
    assertThat(comparatif.getString("recommandationOffreId")).isEqualTo(offreSahel);
    assertThat(comparatif.getInt("offres[0].rang")).isEqualTo(1);

    // Attribution > seuil de délégation (150 M FCFA) : co-validation RM ET approbation DG (R11).
    JsonPath attribution =
        attendu(
                en(Compte.RA)
                    .body(Map.of("offreId", offreSahel, "justification", "Meilleur score (E2E)"))
                    .post("/api/appels-offres/{id}/attribuer", aoId),
                200)
            .jsonPath();
    assertThat(attribution.getString("attribution.coValidationMateriel.statut"))
        .isEqualTo("EN_ATTENTE");
    assertThat(attribution.getString("attribution.approbationDg.statut")).isEqualTo("EN_ATTENTE");
    // R01/R11 : le RA ne peut pas co-valider à la place du RM.
    problemDetail(
        en(Compte.RA)
            .body(Map.of("approuve", true))
            .post("/api/appels-offres/{id}/attribution/co-valider", aoId),
        403,
        "ACCES_REFUSE");
    JsonPath apresRm =
        attendu(
                en(Compte.RM)
                    .body(Map.of("approuve", true, "commentaire", "Conforme (E2E)"))
                    .post("/api/appels-offres/{id}/attribution/co-valider", aoId),
                200)
            .jsonPath();
    assertThat(apresRm.getString("statut")).isEqualTo("CLOTURE");
    assertThat(apresRm.getString("contratId")).isNull();
    attendreNotification(Compte.DG, "Attribution " + ao.getString("reference") + " à approuver");
    JsonPath attribue =
        attendu(
                en(Compte.DG)
                    .body(Map.of("approuve", true, "commentaire", "Approuvé (E2E)"))
                    .post("/api/appels-offres/{id}/attribution/approuver", aoId),
                200)
            .jsonPath();
    assertThat(attribue.getString("statut")).isEqualTo("ATTRIBUE");
    String contratId = attribue.getString("contratId");
    assertThat(contratId).isNotNull();

    // Contrat BROUILLON (R03) -> clauses -> signature -> engagement EN_ATTENTE.
    JsonPath contrat = attendu(en(Compte.RA).get("/api/contrats/{id}", contratId), 200).jsonPath();
    assertThat(contrat.getString("statut")).isEqualTo("BROUILLON");
    assertThat(contrat.getString("fournisseurId")).isEqualTo(FOURNISSEUR_SAHEL);
    assertThat(contrat.getString("besoinId")).isEqualTo(besoinId);
    assertThat(contrat.getLong("montant")).isEqualTo(320_000_000L);
    attendu(
        en(Compte.RA)
            .body(
                Map.of(
                    "garantieMois",
                    24,
                    "conditionsLivraison",
                    "Franco chantier Dakar-AIBD",
                    "penaliteRetardPctParJour",
                    0.1,
                    "delaiLivraisonJours",
                    20,
                    "jalons",
                    List.of(Map.of("libelle", "Expédition", "datePrevue", jour(10)))))
            .put("/api/contrats/{id}/clauses", contratId),
        200);
    JsonPath signe =
        attendu(en(Compte.RA).post("/api/contrats/{id}/signer", contratId), 200).jsonPath();
    assertThat(signe.getString("statut")).isEqualTo("SIGNE");
    assertThat(signe.getString("engagement.statut")).isEqualTo("EN_ATTENTE");

    // R06 : livraison impossible avant l'engagement DAF.
    problemDetail(
        en(Compte.RA).post("/api/contrats/{id}/livraison/demarrer", contratId),
        422,
        "ENGAGEMENT_NON_VALIDE");
    assertThat(
            attendu(
                    en(Compte.DAF)
                        .queryParam("statut", "EN_ATTENTE")
                        .queryParam("size", 50)
                        .get("/api/engagements"),
                    200)
                .jsonPath()
                .<String>getList("content.contratId"))
        .contains(contratId);
    assertThat(
            attendu(
                    en(Compte.DAF)
                        .body(Map.of("approuve", true, "commentaire", "Crédits disponibles"))
                        .post("/api/engagements/{id}/valider", contratId),
                    200)
                .<String>path("statut"))
        .isEqualTo("ENGAGEMENT_VALIDE");

    assertThat(
            attendu(en(Compte.RA).post("/api/contrats/{id}/livraison/demarrer", contratId), 200)
                .<String>path("statut"))
        .isEqualTo("EN_LIVRAISON");
    JsonPath livre =
        attendu(
                en(Compte.RA)
                    .body(
                        Map.of(
                            "dateLivraisonReelle",
                            jour(0),
                            "numeroBonLivraison",
                            "BL-E2E-API-" + sfx,
                            "observations",
                            "Deux niveleuses réceptionnées"))
                    .post("/api/contrats/{id}/livraison/confirmer", contratId),
                200)
            .jsonPath();
    assertThat(livre.getString("statut")).isEqualTo("LIVRE");

    // ms-suivi : 2 matériels créés, affectés EN_ATTENTE_RECEPTION au chantier du besoin.
    AtomicReference<JsonPath> affectations = new AtomicReference<>();
    aTerme("matériels livrés dans ms-suivi")
        .until(
            () -> {
              affectations.set(
                  attendu(
                          en(Compte.RM)
                              .queryParam("chantierId", CHANTIER_DAKAR)
                              .queryParam("statut", "EN_ATTENTE_RECEPTION")
                              .queryParam("size", 50)
                              .get("/api/affectations"),
                          200)
                      .jsonPath());
              return affectations.get().<String>getList("content.contratId").stream()
                      .filter(contratId::equals)
                      .count()
                  == 2;
            });
    List<Map<String, Object>> livrees =
        affectations.get().<Map<String, Object>>getList("content").stream()
            .filter(a -> contratId.equals(a.get("contratId")))
            .toList();
    assertThat(livrees)
        .allSatisfy(
            a -> {
              assertThat(a.get("besoinId")).isEqualTo(besoinId);
              assertThat(a.get("materielDesignation")).isEqualTo(designation);
              assertThat(a.get("fournisseurId")).isEqualTo(FOURNISSEUR_SAHEL);
            });
    String materielId = (String) livrees.get(0).get("materielId");
    JsonPath materiel =
        attendu(en(Compte.CHEF).get("/api/materiels/{id}", materielId), 200).jsonPath();
    assertThat(materiel.getString("contratId")).isEqualTo(contratId);
    assertThat(materiel.getString("chantierActuelId")).isEqualTo(CHANTIER_DAKAR);
  }

  @Test
  @DisplayName("R02 : offre refusée (422) après la date limite de dépôt")
  void offreApresDateLimite_R02_refusee() {
    Instant dateLimite = Instant.now().plusSeconds(3);
    String aoId =
        attendu(
                en(Compte.RA)
                    .body(
                        Map.of(
                            "objet",
                            "Demande de prix R02 (E2E " + suffixe() + ")",
                            "type",
                            "DEMANDE_PRIX",
                            "categorie",
                            "MATERIEL_ROULANT",
                            "quantite",
                            1,
                            "dateLimite",
                            dateLimite.toString(),
                            "criteres",
                            List.of(Map.of("code", "PRIX", "poids", 100)),
                            "fournisseurIdsInvites",
                            List.of(FOURNISSEUR_SAHEL)))
                    .post("/api/appels-offres"),
                201)
            .path("id");
    aTerme("dépassement de la date limite")
        .until(() -> Instant.now().isAfter(dateLimite.plusSeconds(1)));
    problemDetail(
        en(Compte.RA)
            .body(
                Map.of(
                    "fournisseurId",
                    FOURNISSEUR_SAHEL,
                    "montantPropose",
                    10_000_000,
                    "delaiLivraisonJours",
                    10))
            .post("/api/appels-offres/{id}/offres", aoId),
        422,
        "DATE_LIMITE_DEPASSEE");
  }

  private static String saisirOffre(String aoId, String fournisseurId, long montant, int delai) {
    JsonPath offre =
        attendu(
                en(Compte.RA)
                    .body(
                        Map.of(
                            "fournisseurId", fournisseurId,
                            "montantPropose", montant,
                            "delaiLivraisonJours", delai,
                            "garantieMois", 12))
                    .post("/api/appels-offres/{id}/offres", aoId),
                201)
            .jsonPath();
    return offre.getString("id");
  }
}
