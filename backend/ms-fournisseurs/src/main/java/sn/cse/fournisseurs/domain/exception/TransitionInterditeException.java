package sn.cse.fournisseurs.domain.exception;

import sn.cse.common.exception.ConflictException;

/** Levée lorsqu'une transition de statut n'est pas prévue par la machine à états (HTTP 409). */
public class TransitionInterditeException extends ConflictException {

  /**
   * @param entite libellé de l'entité concernée (« fournisseur », « litige »)
   * @param depuis statut courant
   * @param vers statut visé
   */
  public TransitionInterditeException(String entite, Enum<?> depuis, Enum<?> vers) {
    super(
        CodesErreur.TRANSITION_STATUT_INVALIDE,
        "Transition impossible pour ce "
            + entite
            + " : "
            + depuis.name()
            + " → "
            + vers.name()
            + ".");
  }
}
