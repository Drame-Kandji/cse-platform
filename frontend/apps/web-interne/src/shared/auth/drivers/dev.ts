import { ROLES, type Role } from '@/shared/domain/labels'
import { STORAGE_KEYS } from '@/shared/lib/storage'
import type { AuthDriver, AuthUser } from '../types'

/**
 * Profils fictifs du mode developpement (VITE_AUTH_MODE=dev).
 * Ce module n'est importe que si `import.meta.env.DEV` : il est exclu du build de production.
 */
export const DEV_PERSONAS: Readonly<Record<Role, { name: string; username: string }>> = {
  CHEF_CHANTIER: { name: 'Moussa Ndiaye', username: 'm.ndiaye' },
  RESPONSABLE_MATERIEL: { name: 'Fatou Sarr', username: 'f.sarr' },
  RESPONSABLE_ACHATS: { name: 'Ibrahima Fall', username: 'i.fall' },
  DAF: { name: 'Aminata Diallo', username: 'a.diallo' },
  DIRECTION_GENERALE: { name: 'Cheikh Ba', username: 'c.ba' },
  ADMIN_SYSTEME: { name: 'Awa Diop', username: 'a.diop' },
}

function personaUser(role: Role): AuthUser {
  const persona = DEV_PERSONAS[role]
  return {
    id: `dev-${persona.username}`,
    username: persona.username,
    name: persona.name,
    email: `${persona.username}@cse.sn`,
    roles: [role],
    chantierIds: role === 'CHEF_CHANTIER' ? ['dev-chantier-diamniadio'] : [],
  }
}

function readSession(): Role | null {
  try {
    const value = window.sessionStorage.getItem(STORAGE_KEYS.devSession)
    return ROLES.find((role) => role === value) ?? null
  } catch {
    return null
  }
}

/** Pilote factice : aucun jeton, session conservee dans sessionStorage. */
export function createDevDriver(): AuthDriver {
  const listeners = new Set<(user: AuthUser | null) => void>()
  return {
    mode: 'dev',
    init: () => {
      const role = readSession()
      return Promise.resolve(role ? personaUser(role) : null)
    },
    login: () => Promise.resolve(),
    logout: () => {
      try {
        window.sessionStorage.removeItem(STORAGE_KEYS.devSession)
      } catch {
        // sessionStorage indisponible : la session disparait au rechargement.
      }
      listeners.forEach((listener) => {
        listener(null)
      })
      return Promise.resolve()
    },
    getToken: () => Promise.resolve(null),
    subscribe(listener) {
      listeners.add(listener)
      return () => {
        listeners.delete(listener)
      }
    },
    devSignIn(role) {
      try {
        window.sessionStorage.setItem(STORAGE_KEYS.devSession, role)
      } catch {
        // Session non persistee : acceptable en developpement.
      }
      return personaUser(role)
    },
  }
}
