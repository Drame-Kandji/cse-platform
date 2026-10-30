package sn.cse.auth.domain.exception;

import sn.cse.common.exception.BusinessRuleException;

/** Attribut de profil incohérent (montant négatif, champ obligatoire manquant) : HTTP 422. */
public class AttributProfilInvalideException extends BusinessRuleException {

  /**
   * @param champ attribut concerné
   * @param raison explication en français
   */
  public AttributProfilInvalideException(String champ, String raison) {
    super("ATTRIBUT_PROFIL_INVALIDE", "Attribut « " + champ + " » invalide : " + raison + ".");
  }
}
