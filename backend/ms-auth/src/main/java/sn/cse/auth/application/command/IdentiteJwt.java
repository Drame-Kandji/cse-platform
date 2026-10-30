package sn.cse.auth.application.command;

import java.util.Set;
import java.util.UUID;

/**
 * Identité extraite du jeton d'accès de l'utilisateur courant.
 *
 * @param id {@code sub}
 * @param email claim {@code email}
 * @param prenom claim {@code given_name}
 * @param nom claim {@code family_name}
 * @param roles rôles realm
 * @param chantierIds claim {@code chantier_ids}
 * @param fournisseurId claim {@code fournisseur_id}
 */
public record IdentiteJwt(
    UUID id,
    String email,
    String prenom,
    String nom,
    Set<String> roles,
    Set<UUID> chantierIds,
    UUID fournisseurId) {

  public IdentiteJwt {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
    chantierIds = chantierIds == null ? Set.of() : Set.copyOf(chantierIds);
  }
}
