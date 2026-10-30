package sn.cse.fournisseurs.domain.service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Paramètres de la politique de score pondéré, issus de la configuration {@code
 * cse.fournisseurs.score.*}.
 *
 * @param poidsQualite poids du critère qualité
 * @param poidsDelais poids du critère délais
 * @param poidsConformite poids du critère conformité
 * @param poidsSav poids du critère SAV
 * @param poidsPrix poids du critère prix
 * @param poidsLivraisonALHeure part (en %) du taux de livraison à l'heure dans le score
 * @param penaliteLitigeOuvert points retirés par litige ouvert
 * @param penaliteLitigeResolu points retirés par litige résolu
 * @param scoreInitial score d'un fournisseur sans historique
 * @param seuilAlerte seuil en dessous duquel une alerte est émise
 */
public record ParametresScore(
    int poidsQualite,
    int poidsDelais,
    int poidsConformite,
    int poidsSav,
    int poidsPrix,
    int poidsLivraisonALHeure,
    BigDecimal penaliteLitigeOuvert,
    BigDecimal penaliteLitigeResolu,
    BigDecimal scoreInitial,
    BigDecimal seuilAlerte) {

  /** Vérifie la cohérence des paramètres. */
  public ParametresScore {
    if (poidsQualite < 0
        || poidsDelais < 0
        || poidsConformite < 0
        || poidsSav < 0
        || poidsPrix < 0) {
      throw new IllegalArgumentException("Les poids des critères doivent être positifs");
    }
    if (poidsQualite + poidsDelais + poidsConformite + poidsSav + poidsPrix == 0) {
      throw new IllegalArgumentException("La somme des poids des critères doit être non nulle");
    }
    if (poidsLivraisonALHeure < 0 || poidsLivraisonALHeure > 100) {
      throw new IllegalArgumentException(
          "Le poids de la ponctualité doit être compris entre 0 et 100");
    }
    Objects.requireNonNull(penaliteLitigeOuvert, "penaliteLitigeOuvert");
    Objects.requireNonNull(penaliteLitigeResolu, "penaliteLitigeResolu");
    Objects.requireNonNull(scoreInitial, "scoreInitial");
    Objects.requireNonNull(seuilAlerte, "seuilAlerte");
  }

  /**
   * @return somme des poids des cinq critères
   */
  public int sommePoidsCriteres() {
    return poidsQualite + poidsDelais + poidsConformite + poidsSav + poidsPrix;
  }
}
