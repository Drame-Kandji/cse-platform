import type { AuthUser } from '@/shared/auth/types'

/** Role Keycloak requis pour utiliser le portail (claim `realm_access.roles`). */
export const ROLE_FOURNISSEUR = 'FOURNISSEUR'

/** Vrai si l'utilisateur est un fournisseur (seul profil autorise sur le portail). */
export function estFournisseur(user: AuthUser | null): boolean {
  return user?.realmRoles?.includes(ROLE_FOURNISSEUR) ?? false
}
