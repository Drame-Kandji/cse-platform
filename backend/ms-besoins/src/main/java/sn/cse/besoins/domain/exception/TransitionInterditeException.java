package sn.cse.besoins.domain.exception;

import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.common.exception.ConflictException;

/** Transition refusée par la machine à états du besoin (HTTP 409). */
public class TransitionInterditeException extends ConflictException {

  /**
   * @param reference référence du besoin
   * @param depuis statut courant
   * @param vers statut visé
   */
  public TransitionInterditeException(String reference, StatutBesoin depuis, StatutBesoin vers) {
    super(
        CodesErreurBesoin.TRANSITION_STATUT_INTERDITE,
        "Le besoin " + reference + " ne peut pas passer de " + depuis + " à " + vers + ".");
  }
}
