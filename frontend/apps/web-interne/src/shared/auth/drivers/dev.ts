import { ROLES, type Role } from '@/shared/domain/labels'
import { STORAGE_KEYS } from '@/shared/lib/storage'
import type { AuthDriver, AuthUser } from '../types'

/** Profil fictif aligne sur le jeu de donnees de demonstration (contrat d'API, section 12). */
export interface DevPersona {
  readonly id: string
  readonly name: string
  readonly email: string
  readonly chantierIds: readonly string[]
}

/**
 * Profils fictifs du mode developpement (VITE_AUTH_MODE=dev), identiques aux comptes Keycloak de
 * demonstration (memes identifiants, noms, e-mails et chantiers) pour que les donnees seedees
 * (demandeurs, auteurs, perimetre CHEF) restent coherentes.
 * Ce module n'est importe que si `import.meta.env.DEV` : il est exclu du build de production.
 */
export const DEV_PERSONAS: Readonly<Record<Role, DevPersona>> = {
  CHEF_CHANTIER: {
    id: '00000000-0000-0000-0000-000000000101',
    name: 'Mamadou Diop',
    email: 'chef.chantier@cse.sn',
    chantierIds: ['c0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002'],
  },
  RESPONSABLE_MATERIEL: {
    id: '00000000-0000-0000-0000-000000000201',
    name: 'Ibrahima Fall',
    email: 'resp.materiel@cse.sn',
    chantierIds: [],
  },
  RESPONSABLE_ACHATS: {
    id: '00000000-0000-0000-0000-000000000301',
    name: 'Fatou Sarr',
    email: 'resp.achats@cse.sn',
    chantierIds: [],
  },
  DAF: {
    id: '00000000-0000-0000-0000-000000000401',
    name: 'Ousmane Ba',
    email: 'daf@cse.sn',
    chantierIds: [],
  },
  DIRECTION_GENERALE: {
    id: '00000000-0000-0000-0000-000000000501',
    name: 'Aminata Sow',
    email: 'dg@cse.sn',
    chantierIds: [],
  },
  ADMIN_SYSTEME: {
    id: '00000000-0000-0000-0000-000000000601',
    name: 'Cheikh Ndiaye',
    email: 'admin@cse.sn',
    chantierIds: [],
  },
}

/** Utilisateur de session correspondant au profil fictif d'un role. */
export function personaUser(role: Role): AuthUser {
  const persona = DEV_PERSONAS[role]
  return {
    id: persona.id,
    username: persona.email,
    name: persona.name,
    email: persona.email,
    roles: [role],
    chantierIds: [...persona.chantierIds],
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
