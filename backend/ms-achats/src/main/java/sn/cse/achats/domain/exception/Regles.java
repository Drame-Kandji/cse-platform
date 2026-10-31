package sn.cse.achats.domain.exception;

import java.math.BigDecimal;
import sn.cse.common.exception.BusinessRuleException;

/** Petites assertions métier levant une {@link BusinessRuleException} (HTTP 422). */
public final class Regles {

  private Regles() {}

  /**
   * @param condition condition qui doit être vraie
   * @param code code d'erreur
   * @param message message en français
   * @throws BusinessRuleException si la condition est fausse
   */
  public static void exiger(boolean condition, String code, String message) {
    if (!condition) {
      throw new BusinessRuleException(code, message);
    }
  }

  /**
   * @param valeur texte
   * @param champ libellé du champ
   * @return le texte nettoyé
   * @throws BusinessRuleException si le texte est vide
   */
  public static String texteObligatoire(String valeur, String champ) {
    exiger(
        valeur != null && !valeur.isBlank(),
        CodesErreur.DONNEE_INVALIDE,
        "Le champ « " + champ + " » est obligatoire.");
    return valeur.trim();
  }

  /**
   * @param valeur montant
   * @param champ libellé du champ
   * @return le montant
   * @throws BusinessRuleException si le montant est absent ou non strictement positif
   */
  public static BigDecimal montantPositif(BigDecimal valeur, String champ) {
    exiger(
        valeur != null && valeur.signum() > 0,
        CodesErreur.DONNEE_INVALIDE,
        "Le montant « " + champ + " » doit être strictement positif.");
    return valeur;
  }

  /**
   * @param valeur texte facultatif
   * @return le texte nettoyé, ou {@code null} s'il est vide
   */
  public static String facultatif(String valeur) {
    return valeur == null || valeur.isBlank() ? null : valeur.trim();
  }
}
