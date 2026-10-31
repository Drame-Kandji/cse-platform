package sn.cse.achats.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ConflictException;

/** Cycle de vie d'une facture fournisseur : RECUE → MANDATEE → PAYEE, ou RECUE → REJETEE. */
public enum StatutFacture {
  /** Reçue, en attente de mandatement. */
  RECUE,
  /** Mandatée par la DAF. */
  MANDATEE,
  /** Payée. */
  PAYEE,
  /** Rejetée (motif obligatoire). */
  REJETEE;

  private static final Map<StatutFacture, Set<StatutFacture>> TRANSITIONS =
      new EnumMap<>(StatutFacture.class);

  static {
    TRANSITIONS.put(RECUE, EnumSet.of(MANDATEE, REJETEE));
    TRANSITIONS.put(MANDATEE, EnumSet.of(PAYEE));
    TRANSITIONS.put(PAYEE, EnumSet.noneOf(StatutFacture.class));
    TRANSITIONS.put(REJETEE, EnumSet.noneOf(StatutFacture.class));
  }

  /**
   * @param cible statut visé
   * @throws ConflictException si la transition est interdite
   */
  public void exigerTransition(StatutFacture cible) {
    if (!TRANSITIONS.get(this).contains(cible)) {
      throw new ConflictException(
          CodesErreur.TRANSITION_INTERDITE,
          "Transition interdite de la facture : " + this + " → " + cible + ".");
    }
  }

  /**
   * @return vrai tant que la facture reste à payer
   */
  public boolean estOuverte() {
    return this == RECUE || this == MANDATEE;
  }
}
