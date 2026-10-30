import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import type { Role } from '../lib/labels'

export type DemoProfile = {
  id: string
  nom: string
  prenom: string
  email: string
  role: Role
  password: string
  fonction: string
  chantierAttribueId: string | null
}

const DEMO_PASSWORD = import.meta.env.VITE_DEMO_PASSWORD ?? 'CSE2026!'

export const DEMO_PROFILES: DemoProfile[] = [
  {
    id: 'b1111111-1111-1111-1111-111111111111',
    nom: 'Diop',
    prenom: 'Mamadou',
    email: 'chef.chantier@cse.sn',
    role: 'CHEF_CHANTIER',
    password: DEMO_PASSWORD,
    fonction: 'Chef de chantier — Dakar Plateau',
    chantierAttribueId: '11111111-1111-1111-1111-111111111111',
  },
  {
    id: 'b2222222-2222-2222-2222-222222222222',
    nom: 'Fall',
    prenom: 'Aminata',
    email: 'resp.materiel@cse.sn',
    role: 'RESPONSABLE_MATERIEL',
    password: DEMO_PASSWORD,
    fonction: 'Responsable matériel — Parc Central',
    chantierAttribueId: null,
  },
  {
    id: 'b3333333-3333-3333-3333-333333333333',
    nom: 'Ndiaye',
    prenom: 'Ousmane',
    email: 'resp.achats@cse.sn',
    role: 'RESPONSABLE_ACHATS',
    password: DEMO_PASSWORD,
    fonction: 'Responsable achats & marchés',
    chantierAttribueId: null,
  },
  {
    id: 'b4444444-4444-4444-4444-444444444444',
    nom: 'Sarr',
    prenom: 'Fatou',
    email: 'daf@cse.sn',
    role: 'DAF',
    password: DEMO_PASSWORD,
    fonction: 'Directrice administrative et financière',
    chantierAttribueId: null,
  },
  {
    id: 'b5555555-5555-5555-5555-555555555555',
    nom: 'Ba',
    prenom: 'Ibrahima',
    email: 'dg@cse.sn',
    role: 'DIRECTION_GENERALE',
    password: DEMO_PASSWORD,
    fonction: 'Direction générale CSE',
    chantierAttribueId: null,
  },
  {
    id: 'b6666666-6666-6666-6666-666666666666',
    nom: 'Gueye',
    prenom: 'Cheikh',
    email: 'admin@cse.sn',
    role: 'ADMIN_SYSTEME',
    password: DEMO_PASSWORD,
    fonction: 'Administrateur système & IAM',
    chantierAttribueId: null,
  },
]

const STORAGE_KEY = 'cse.web.session'

type AuthContextValue = {
  user: DemoProfile | null
  ready: boolean
  login: (email: string, password: string) => { ok: boolean; message?: string }
  switchRole: (role: Role) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readSession(): DemoProfile | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const stored = JSON.parse(raw) as { email?: string }
    return DEMO_PROFILES.find((profile) => profile.email === stored.email) ?? null
  } catch {
    return null
  }
}

function persist(profile: DemoProfile | null) {
  if (!profile) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ email: profile.email }))
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<DemoProfile | null>(() => readSession())
  const ready = true

  const login = useCallback((email: string, password: string) => {
    const profile = DEMO_PROFILES.find((item) => item.email.toLowerCase() === email.trim().toLowerCase())
    if (!profile) {
      return { ok: false, message: 'Aucun compte CSE ne correspond à cette adresse e-mail.' }
    }
    if (profile.password !== password) {
      return { ok: false, message: 'Mot de passe incorrect. Utilisez le mot de passe de démonstration indiqué.' }
    }
    setUser(profile)
    persist(profile)
    return { ok: true }
  }, [])

  const switchRole = useCallback((role: Role) => {
    const profile = DEMO_PROFILES.find((item) => item.role === role)
    if (!profile) return
    setUser(profile)
    persist(profile)
  }, [])

  const logout = useCallback(() => {
    setUser(null)
    persist(null)
  }, [])

  const value = useMemo(
    () => ({ user, ready, login, switchRole, logout }),
    [user, ready, login, switchRole, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth doit être utilisé dans AuthProvider')
  return context
}
