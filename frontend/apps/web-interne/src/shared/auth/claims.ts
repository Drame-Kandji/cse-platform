import { z } from 'zod'
import { ROLES, type Role } from '@/shared/domain/labels'
import type { AuthUser } from './types'

const claimsSchema = z
  .object({
    sub: z.string().min(1),
    name: z.string().optional(),
    given_name: z.string().optional(),
    family_name: z.string().optional(),
    preferred_username: z.string().optional(),
    email: z.string().optional(),
    realm_access: z
      .object({ roles: z.array(z.string()) })
      .partial()
      .optional(),
    chantier_ids: z.union([z.array(z.string()), z.string()]).optional(),
  })
  .passthrough()

const KNOWN_ROLES: ReadonlySet<string> = new Set(ROLES)

/** Vrai si la chaine est un role CSE connu. */
export function isRole(value: string): value is Role {
  return KNOWN_ROLES.has(value)
}

/**
 * Construit l'utilisateur a partir des claims d'un jeton (tokenParsed Keycloak).
 * Les roles techniques Keycloak (offline_access...) sont ignores.
 * @throws Error si le claim `sub` est absent
 */
export function userFromClaims(claims: unknown): AuthUser {
  const parsed = claimsSchema.safeParse(claims)
  if (!parsed.success) throw new Error('Jeton invalide : claims obligatoires absents.')
  const c = parsed.data
  const fullName = [c.given_name, c.family_name].filter(Boolean).join(' ')
  const chantierIds = c.chantier_ids === undefined ? [] : [c.chantier_ids].flat().filter(Boolean)
  return {
    id: c.sub,
    username: c.preferred_username ?? c.sub,
    name: c.name ?? (fullName || c.preferred_username || c.sub),
    email: c.email,
    roles: (c.realm_access?.roles ?? []).filter(isRole),
    chantierIds,
  }
}

/** Vrai si `userRoles` contient au moins un role de `required` (liste vide = acces libre). */
export function hasAnyRole(userRoles: readonly Role[], required: readonly Role[]): boolean {
  return required.length === 0 || required.some((role) => userRoles.includes(role))
}
