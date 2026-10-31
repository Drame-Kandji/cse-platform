package sn.cse.e2e.support;

import static sn.cse.e2e.support.Api.attendu;
import static sn.cse.e2e.support.Api.en;

import io.restassured.path.json.JsonPath;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

/**
 * Jeu de données de démonstration (contrat §12) et fabriques de données de test uniques, afin que
 * les tests soient rejouables sur une base déjà peuplée.
 */
public final class Donnees {

  /** Chantier 1 (Dakar-AIBD) du chef de démonstration. */
  public static final String CHANTIER_DAKAR = "c0000000-0000-0000-0000-000000000001";

  /** Chantier 2 (Thiès) du chef de démonstration. */
  public static final String CHANTIER_THIES = "c0000000-0000-0000-0000-000000000002";

  /** Chantier 3 (Saint-Louis), hors périmètre du chef de démonstration. */
  public static final String CHANTIER_SAINT_LOUIS = "c0000000-0000-0000-0000-000000000003";

  /** Sahel Engins SA, ACTIF, MATERIEL_ROULANT. */
  public static final String FOURNISSEUR_SAHEL = "f0000000-0000-0000-0000-000000000001";

  /** Teranga Équipements BTP, ACTIF, MATERIEL_ROULANT et OUTILLAGE. */
  public static final String FOURNISSEUR_TERANGA = "f0000000-0000-0000-0000-000000000002";

  private Donnees() {}

  /**
   * Suffixe unique (horodatage base 36 + aléa) pour désignations et codes.
   *
   * @return suffixe en majuscules
   */
  public static String suffixe() {
    return (Long.toString(System.currentTimeMillis(), 36)
            + Integer.toString(ThreadLocalRandom.current().nextInt(1296), 36))
        .toUpperCase(Locale.ROOT);
  }

  /**
   * Date ISO à J+n.
   *
   * @param jours décalage en jours
   * @return date au format aaaa-mm-jj
   */
  public static String jour(int jours) {
    return LocalDate.now().plusDays(jours).toString();
  }

  /**
   * Attente de cohérence à terme (événements Kafka entre services).
   *
   * @param description objet de l'attente, affiché en cas d'échec
   * @return condition Awaitility (60 s max, sondage toutes les 500 ms)
   */
  public static ConditionFactory aTerme(String description) {
    return Awaitility.await(description)
        .atMost(Duration.ofSeconds(60))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions();
  }

  /**
   * Enregistre un matériel DISPONIBLE (RM).
   *
   * @param code code unique
   * @param designation désignation unique
   * @param categorie catégorie
   * @return le matériel créé
   */
  public static JsonPath creerMateriel(String code, String designation, String categorie) {
    return attendu(
            en(Compte.RM)
                .body(
                    Map.of(
                        "code",
                        code,
                        "designation",
                        designation,
                        "categorie",
                        categorie,
                        "tarifJournalier",
                        120000))
                .post("/api/materiels"),
            201)
        .jsonPath();
  }

  /**
   * Corps de soumission d'un besoin.
   *
   * @param chantierId chantier
   * @param designation désignation
   * @param categorie catégorie
   * @param quantite quantité
   * @param priorite priorité
   * @param justification justification (null si absente)
   * @return corps JSON
   */
  public static Map<String, Object> besoin(
      String chantierId,
      String designation,
      String categorie,
      int quantite,
      String priorite,
      String justification) {
    var corps =
        new java.util.HashMap<String, Object>(
            Map.of(
                "chantierId", chantierId,
                "designation", designation,
                "categorie", categorie,
                "quantite", quantite,
                "priorite", priorite,
                "dureeUtilisationJours", 15,
                "dateBesoin", jour(2)));
    if (justification != null) {
      corps.put("justification", justification);
    }
    return corps;
  }

  /**
   * Retire du parc un matériel de test (demande RM puis validation DG, R13) pour ne pas fausser les
   * disponibilités des exécutions suivantes.
   *
   * @param materielId matériel à réformer (non affecté)
   */
  public static void reformer(String materielId) {
    attendu(
        en(Compte.RM)
            .body(Map.of("motif", "Matériel de test E2E"))
            .post("/api/materiels/{id}/reforme/demande", materielId),
        200);
    attendu(
        en(Compte.DG)
            .body(Map.of("approuve", true, "commentaire", "Nettoyage E2E"))
            .patch("/api/materiels/{id}/reforme/valider", materielId),
        200);
  }

  /**
   * Attend qu'une notification dont le titre contient le fragment parvienne au compte.
   *
   * @param compte destinataire
   * @param fragmentTitre fragment du titre attendu
   */
  public static void attendreNotification(Compte compte, String fragmentTitre) {
    aTerme("notification « " + fragmentTitre + " » pour " + compte)
        .until(
            () ->
                attendu(en(compte).queryParam("size", 50).get("/api/notifications"), 200)
                    .jsonPath()
                    .<String>getList("content.titre")
                    .stream()
                    .anyMatch(t -> t.contains(fragmentTitre)));
  }
}
