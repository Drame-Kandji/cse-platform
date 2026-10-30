package sn.cse.auth.application.query;

import sn.cse.auth.domain.model.Role;

/**
 * Filtres de la liste des utilisateurs (tous facultatifs).
 *
 * @param role rôle exact
 * @param actif statut du compte
 * @param texte recherche libre sur e-mail, prénom et nom
 */
public record CriteresRecherche(Role role, Boolean actif, String texte) {

  public CriteresRecherche {
    texte = texte == null || texte.isBlank() ? null : texte.strip();
  }
}
