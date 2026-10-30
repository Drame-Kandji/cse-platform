package sn.cse.suivi.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'une affectation : {@code EN_ATTENTE_RECEPTION → EN_COURS → TERMINEE} ; une
 * affectation non réceptionnée peut aussi être terminée (annulation / libération anticipée).
 */
public enum StatutAffectation {
  /** Matériel expédié, réception non confirmée par le chef de chantier. */
  EN_ATTENTE_RECEPTION,
  /** Matériel réceptionné et utilisé sur le chantier. */
  EN_COURS,
  /** Affectation close. */
  TERMINEE;

  /** Statuts d'une affectation qui mobilise encore le matériel. */
  public static final Set<StatutAffectation> ACTIFS = EnumSet.of(EN_ATTENTE_RECEPTION, EN_COURS);

  /**
   * @return vrai si l'affectation mobilise encore le matériel
   */
  public boolean estActif() {
    return ACTIFS.contains(this);
  }
}
