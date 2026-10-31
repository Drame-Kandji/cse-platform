import type { AuthMode } from '@/shared/config/env'
import type { Role } from '@/shared/domain/labels'

/** Utilisateur connecte, derive des claims du jeton Keycloak. */
export interface AuthUser {
  /** Identifiant Keycloak (claim `sub`). */
  readonly id: string
  readonly username: string
  readonly name: string
  readonly email: string | undefined
  /** Roles CSE reconnus (claim `realm_access.roles`, filtres). */
  readonly roles: readonly Role[]
  /** Chantiers rattaches (claim `chantier_ids`, vide si absent). */
  readonly chantierIds: readonly string[]
}

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error'

/** Strategie d'authentification interchangeable (Keycloak ou developpement). */
export interface AuthDriver {
  readonly mode: AuthMode
  init: () => Promise<AuthUser | null>
  login: () => Promise<void>
  logout: () => Promise<void>
  /** Jeton d'acces valide (rafraichi si necessaire) ou null. */
  getToken: () => Promise<string | null>
  /** Notifie les changements de session (rafraichissement, expiration). */
  subscribe: (listener: (user: AuthUser | null) => void) => () => void
  /** Connexion simulee (mode dev uniquement). */
  devSignIn?: (role: Role) => AuthUser
}

export interface AuthContextValue {
  readonly status: AuthStatus
  readonly user: AuthUser | null
  readonly mode: AuthMode
  readonly error: Error | null
  /** Vrai si l'utilisateur possede au moins un des roles (liste vide = toujours vrai). */
  hasAnyRole: (roles: readonly Role[]) => boolean
  login: () => Promise<void>
  logout: () => Promise<void>
  getToken: () => Promise<string | null>
  /** Disponible uniquement en mode dev : connexion avec un role choisi. */
  readonly devSignIn: ((role: Role) => void) | undefined
}
