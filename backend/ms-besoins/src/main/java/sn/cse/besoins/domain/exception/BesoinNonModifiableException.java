package sn.cse.besoins.domain.exception;

import sn.cse.besoins.domain.model.StatutBesoin;
import sn.cse.common.exception.ConflictException;

/** Règle R05 : un besoin validé (ou au-delà) n'est plus modifiable (HTTP 409). */
public class BesoinNonModifiableException extends ConflictException {

  /**
   * @param reference référence du besoin
   * @param statut statut courant
   */
  public BesoinNonModifiableException(String reference, StatutBesoin statut) {
    super(
        CodesErreurBesoin.BESOIN_NON_MODIFIABLE,
        "Le besoin "
            + reference
            + " est au statut "
            + statut
            + " : il n'est plus modifiable (règle R05).");
  }
}
