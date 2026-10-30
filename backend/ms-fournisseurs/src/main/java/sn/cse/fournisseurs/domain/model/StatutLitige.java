package sn.cse.fournisseurs.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Statuts d'un litige et table explicite des transitions autorisées.
 *
 * <p>OUVERT → EN_COURS → (RESOLU | CLASSE) ; OUVERT → (RESOLU | CLASSE). RESOLU et CLASSE sont
 * terminaux.
 */
public enum StatutLitige {
  OUVERT,
  EN_COURS,
  RESOLU,
  CLASSE;

  private static final Map<StatutLitige, Set<StatutLitige>> TRANSITIONS =
      Map.of(
          OUVERT, EnumSet.of(EN_COURS, RESOLU, CLASSE),
          EN_COURS, EnumSet.of(EN_COURS, RESOLU, CLASSE),
          RESOLU, EnumSet.noneOf(StatutLitige.class),
          CLASSE, EnumSet.noneOf(StatutLitige.class));

  /**
   * Indique si la transition vers {@code cible} est autorisée.
   *
   * @param cible statut visé
   * @return {@code true} si la transition figure dans la table
   */
  public boolean peutPasserA(StatutLitige cible) {
    return TRANSITIONS.get(this).contains(cible);
  }

  /**
   * @return {@code true} si le litige est encore ouvert (OUVERT ou EN_COURS)
   */
  public boolean estOuvert() {
    return this == OUVERT || this == EN_COURS;
  }
}
