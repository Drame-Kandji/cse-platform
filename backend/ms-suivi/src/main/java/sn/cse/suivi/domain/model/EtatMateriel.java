package sn.cse.suivi.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Cycle de vie d'un matériel (SPEC B.3). La table de transitions est explicite :
 *
 * <pre>
 * DISPONIBLE     → AFFECTE, EN_MAINTENANCE, REFORME
 * AFFECTE        → DISPONIBLE, EN_MAINTENANCE, REFORME
 * EN_MAINTENANCE → DISPONIBLE, AFFECTE (retour sur chantier après clôture), REFORME
 * REFORME        → (terminal)
 * </pre>
 */
public enum EtatMateriel {
  /** Au parc, affectable. */
  DISPONIBLE,
  /** Affecté à un chantier. */
  AFFECTE,
  /** Immobilisé pour maintenance. */
  EN_MAINTENANCE,
  /** Sorti définitivement du parc (R13). */
  REFORME;

  private static final Map<EtatMateriel, Set<EtatMateriel>> TRANSITIONS =
      new EnumMap<>(EtatMateriel.class);

  static {
    TRANSITIONS.put(DISPONIBLE, EnumSet.of(AFFECTE, EN_MAINTENANCE, REFORME));
    TRANSITIONS.put(AFFECTE, EnumSet.of(DISPONIBLE, EN_MAINTENANCE, REFORME));
    TRANSITIONS.put(EN_MAINTENANCE, EnumSet.of(DISPONIBLE, AFFECTE, REFORME));
    TRANSITIONS.put(REFORME, EnumSet.noneOf(EtatMateriel.class));
  }

  /**
   * @return états atteignables depuis cet état
   */
  public Set<EtatMateriel> transitionsPossibles() {
    return Collections.unmodifiableSet(TRANSITIONS.get(this));
  }

  /**
   * @param cible état visé
   * @return vrai si la transition est autorisée
   */
  public boolean peutPasserA(EtatMateriel cible) {
    return TRANSITIONS.get(this).contains(cible);
  }

  /**
   * @return vrai pour l'état terminal {@link #REFORME}
   */
  public boolean estTerminal() {
    return TRANSITIONS.get(this).isEmpty();
  }

  /**
   * @param cible état visé
   * @throws sn.cse.common.exception.ConflictException si la transition est interdite
   */
  public void verifierTransition(EtatMateriel cible) {
    if (!peutPasserA(cible)) {
      throw SuiviErreurs.transitionInvalide("le matériel", this, cible);
    }
  }
}
