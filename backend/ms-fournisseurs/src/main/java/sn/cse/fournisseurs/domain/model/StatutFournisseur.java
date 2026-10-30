package sn.cse.fournisseurs.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Statuts du cycle de vie d'un fournisseur et table explicite des transitions (SPEC §B.3).
 *
 * <pre>
 * EN_ATTENTE_VALIDATION → ACTIF ⇄ SUSPENDU → BLACKLISTE (terminal)
 * </pre>
 */
public enum StatutFournisseur {
  /** Inscription reçue (portail), en attente de validation par le Responsable des Achats. */
  EN_ATTENTE_VALIDATION,
  /** Fournisseur référencé, consultable et invitable aux appels d'offres (R14). */
  ACTIF,
  /** Fournisseur temporairement écarté. */
  SUSPENDU,
  /** Fournisseur définitivement exclu après approbation de la Direction Générale (R09). */
  BLACKLISTE;

  private static final Map<StatutFournisseur, Set<StatutFournisseur>> TRANSITIONS =
      Map.of(
          EN_ATTENTE_VALIDATION, EnumSet.of(ACTIF),
          ACTIF, EnumSet.of(SUSPENDU),
          SUSPENDU, EnumSet.of(ACTIF, BLACKLISTE),
          BLACKLISTE, EnumSet.noneOf(StatutFournisseur.class));

  /**
   * Indique si la transition vers {@code cible} est autorisée par la machine à états.
   *
   * @param cible statut visé
   * @return {@code true} si la transition figure dans la table
   */
  public boolean peutPasserA(StatutFournisseur cible) {
    return TRANSITIONS.get(this).contains(cible);
  }
}
