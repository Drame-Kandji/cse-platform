package sn.cse.besoins.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'un besoin (SPEC B.3) : {@code EN_ATTENTE → VALIDE → AFFECTE_DIRECT | EN_ACHAT} et
 * {@code EN_ATTENTE → REJETE}. Les états {@code REJETE}, {@code AFFECTE_DIRECT} et {@code EN_ACHAT}
 * sont terminaux.
 */
public enum StatutBesoin {
  /** Soumis, en attente de la décision du Responsable Matériel. */
  EN_ATTENTE,
  /** Validé, en cours d'orientation (affectation directe ou achat). */
  VALIDE,
  /** Rejeté avec motif. */
  REJETE,
  /** Satisfait par affectation de matériels du parc. */
  AFFECTE_DIRECT,
  /** Transmis au service Achats faute de matériel disponible. */
  EN_ACHAT;

  /**
   * @return les statuts atteignables directement depuis ce statut
   */
  public Set<StatutBesoin> suivantsPossibles() {
    return switch (this) {
      case EN_ATTENTE -> EnumSet.of(VALIDE, REJETE);
      case VALIDE -> EnumSet.of(AFFECTE_DIRECT, EN_ACHAT);
      case REJETE, AFFECTE_DIRECT, EN_ACHAT -> EnumSet.noneOf(StatutBesoin.class);
    };
  }

  /**
   * @param cible statut visé
   * @return vrai si la transition est autorisée par la machine à états
   */
  public boolean peutPasserA(StatutBesoin cible) {
    return suivantsPossibles().contains(cible);
  }

  /**
   * Règle R05 : un besoin n'est plus modifiable dès qu'il est validé (ou au-delà).
   *
   * @return vrai si le contenu du besoin peut encore être modifié
   */
  public boolean estModifiable() {
    return this == EN_ATTENTE;
  }

  /**
   * @return vrai si aucun changement de statut n'est plus possible
   */
  public boolean estTerminal() {
    return suivantsPossibles().isEmpty();
  }
}
