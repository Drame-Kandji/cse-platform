package sn.cse.common.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Utilisateur authentifié, reconstruit à partir des claims du JWT Keycloak. Record pur : peut être
 * transmis aux cas d'utilisation et au domaine.
 *
 * @param id identifiant Keycloak ({@code sub})
 * @param nom nom complet affichable
 * @param email adresse e-mail
 * @param roles rôles realm (sans préfixe)
 * @param chantierIds chantiers attribués (chefs de chantier)
 * @param fournisseurId fournisseur rattaché (comptes du portail), sinon {@code null}
 */
public record CurrentUser(
    UUID id,
    String nom,
    String email,
    Set<String> roles,
    Set<UUID> chantierIds,
    UUID fournisseurId) {

  public CurrentUser {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
    chantierIds = chantierIds == null ? Set.of() : Set.copyOf(chantierIds);
  }

  /**
   * @param role rôle recherché
   * @return vrai si l'utilisateur détient ce rôle
   */
  public boolean hasRole(String role) {
    return roles.contains(role);
  }

  /**
   * @param candidats rôles acceptés
   * @return vrai si l'utilisateur détient au moins un des rôles
   */
  public boolean hasAnyRole(String... candidats) {
    return Arrays.stream(candidats).anyMatch(roles::contains);
  }

  /**
   * Un chef de chantier ne voit que ses chantiers ; les autres rôles internes ne sont pas
   * restreints.
   *
   * @param chantierId chantier visé
   * @return vrai si l'utilisateur peut accéder aux données de ce chantier
   */
  public boolean peutAccederAuChantier(UUID chantierId) {
    return !estRestreintAuxChantiers() || chantierIds.contains(chantierId);
  }

  /**
   * @return vrai si la visibilité doit être filtrée sur {@link #chantierIds()} (chef de chantier
   *     sans autre rôle élargi)
   */
  public boolean estRestreintAuxChantiers() {
    return hasRole(Roles.CHEF_CHANTIER)
        && !hasAnyRole(
            Roles.RESPONSABLE_MATERIEL,
            Roles.RESPONSABLE_ACHATS,
            Roles.DAF,
            Roles.DIRECTION_GENERALE,
            Roles.ADMIN_SYSTEME);
  }

  /**
   * @return le fournisseur rattaché, s'il existe
   */
  public Optional<UUID> fournisseur() {
    return Optional.ofNullable(fournisseurId);
  }
}
