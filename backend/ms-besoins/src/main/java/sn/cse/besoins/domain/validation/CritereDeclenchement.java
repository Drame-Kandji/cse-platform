package sn.cse.besoins.domain.validation;

import sn.cse.besoins.domain.model.BesoinMateriel;
import sn.cse.besoins.domain.model.Priorite;

/**
 * Condition rendant une étape de validation obligatoire pour un besoin. Sans aucun seuil, l'étape
 * s'applique à tous les besoins ; sinon elle s'applique dès qu'un des seuils est atteint (OU).
 *
 * @param quantiteMinimale quantité à partir de laquelle l'étape s'applique (optionnel)
 * @param prioriteMinimale priorité à partir de laquelle l'étape s'applique (optionnel)
 */
public record CritereDeclenchement(Integer quantiteMinimale, Priorite prioriteMinimale) {

  /**
   * @return critère s'appliquant à tous les besoins
   */
  public static CritereDeclenchement toujours() {
    return new CritereDeclenchement(null, null);
  }

  /**
   * @return vrai si l'étape s'applique à tous les besoins
   */
  public boolean estInconditionnel() {
    return quantiteMinimale == null && prioriteMinimale == null;
  }

  /**
   * @param besoin besoin évalué
   * @return vrai si l'étape doit être franchie pour ce besoin
   */
  public boolean estSatisfaitPar(BesoinMateriel besoin) {
    if (estInconditionnel()) {
      return true;
    }
    boolean parQuantite =
        quantiteMinimale != null && besoin.description().quantite() >= quantiteMinimale;
    boolean parPriorite =
        prioriteMinimale != null
            && besoin.description().priorite().compareTo(prioriteMinimale) >= 0;
    return parQuantite || parPriorite;
  }
}
