package sn.cse.suivi.application.query;

import java.util.Set;
import java.util.UUID;
import sn.cse.common.security.CurrentUser;
import sn.cse.suivi.domain.exception.SuiviErreurs;

/**
 * Chantiers visibles par l'utilisateur : tous pour les rôles élargis, uniquement les siens pour un
 * chef de chantier.
 *
 * @param chantierIds chantiers visibles, ou {@code null} si aucune restriction
 */
public record Perimetre(Set<UUID> chantierIds) {

  /** Aucune restriction. */
  public static final Perimetre TOUS = new Perimetre(null);

  public Perimetre {
    chantierIds = chantierIds == null ? null : Set.copyOf(chantierIds);
  }

  /**
   * @param utilisateur utilisateur courant
   * @return son périmètre de visibilité
   */
  public static Perimetre de(CurrentUser utilisateur) {
    return utilisateur.estRestreintAuxChantiers() ? new Perimetre(utilisateur.chantierIds()) : TOUS;
  }

  /**
   * @return vrai si la visibilité est restreinte
   */
  public boolean restreint() {
    return chantierIds != null;
  }

  /**
   * Combine le périmètre avec un filtre explicite sur un chantier.
   *
   * @param chantierId chantier demandé (peut être {@code null})
   * @return le périmètre effectif
   * @throws sn.cse.common.exception.ForbiddenOperationException si le chantier est hors périmètre
   */
  public Perimetre restreindreA(UUID chantierId) {
    if (chantierId == null) {
      return this;
    }
    if (restreint() && !chantierIds.contains(chantierId)) {
      throw SuiviErreurs.chantierNonAutorise(chantierId);
    }
    return new Perimetre(Set.of(chantierId));
  }
}
