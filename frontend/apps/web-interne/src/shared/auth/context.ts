import { createContext, useContext } from 'react'
import type { Role } from '@/shared/domain/labels'
import type { AuthContextValue } from './types'

export const AuthContext = createContext<AuthContextValue | null>(null)

/**
 * Acces a la session courante (utilisateur, roles, jeton, deconnexion).
 * @throws Error si utilise hors d'un AuthProvider
 */
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth doit etre utilise dans un <AuthProvider>.')
  return value
}

/**
 * Utilisateur connecte (non nul) : a utiliser uniquement sous la garde d'authentification.
 * @throws Error si aucun utilisateur n'est connecte
 */
export function useCurrentUser() {
  const { user } = useAuth()
  if (!user) throw new Error('useCurrentUser appele sans utilisateur connecte.')
  return user
}

/** Vrai si l'utilisateur a l'un des roles : `const peutValider = useHasRole(['RESPONSABLE_MATERIEL'])`. */
export function useHasRole(roles: readonly Role[]): boolean {
  return useAuth().hasAnyRole(roles)
}
