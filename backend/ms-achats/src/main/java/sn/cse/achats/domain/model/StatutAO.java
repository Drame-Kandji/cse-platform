package sn.cse.achats.domain.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import sn.cse.achats.domain.exception.CodesErreur;
import sn.cse.common.exception.ConflictException;

/** Cycle de vie d'un appel d'offres : PUBLIE → CLOTURE → ATTRIBUE (SPEC §B.3). */
public enum StatutAO {
  /** Ouvert au dépôt des offres, offres anonymisées (R10). */
  PUBLIE,
  /** Dépôts fermés, anonymat levé, évaluation en cours. */
  CLOTURE,
  /** Marché attribué après toutes les approbations (R11). */
  ATTRIBUE;

  private static final Map<StatutAO, Set<StatutAO>> TRANSITIONS = new EnumMap<>(StatutAO.class);

  static {
    TRANSITIONS.put(PUBLIE, EnumSet.of(CLOTURE));
    TRANSITIONS.put(CLOTURE, EnumSet.of(ATTRIBUE));
    TRANSITIONS.put(ATTRIBUE, EnumSet.noneOf(StatutAO.class));
  }

  /**
   * @param cible statut visé
   * @return vrai si la transition est autorisée
   */
  public boolean peutPasserA(StatutAO cible) {
    return TRANSITIONS.get(this).contains(cible);
  }

  /**
   * @param cible statut visé
   * @throws ConflictException si la transition est interdite
   */
  public void exigerTransition(StatutAO cible) {
    if (!peutPasserA(cible)) {
      throw new ConflictException(
          CodesErreur.TRANSITION_INTERDITE,
          "Transition interdite de l'appel d'offres : " + this + " → " + cible + ".");
    }
  }
}
