import type { ReactElement } from 'react'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders as renderInterne, testUser } from '../../../web-interne/src/test/render'
import type { AuthUser } from '@/shared/auth/types'
import { ThemeProvider } from '@/shared/theme/ThemeProvider'

export const FOURNISSEUR_ID = 'f0000000-0000-0000-0000-000000000001'

/** Utilisateur fournisseur de test (role FOURNISSEUR, claim fournisseur_id). */
export function fournisseurUser(overrides: Partial<AuthUser> = {}): AuthUser {
  return testUser([], {
    name: 'Moussa Kane',
    email: 'fournisseur@cse.sn',
    realmRoles: ['FOURNISSEUR'],
    fournisseurId: FOURNISSEUR_ID,
    ...overrides,
  })
}

/**
 * Rendu avec le socle de test de web-interne (routeur memoire, QueryClient isole, session simulee)
 * et un fournisseur connecte, sous ThemeProvider. `path` declare la route pour lire les parametres (`/contrats/:id`).
 */
export function renderPortail(
  ui: ReactElement,
  {
    route = '/',
    path,
    user = fournisseurUser(),
  }: { route?: string; path?: string; user?: AuthUser | null } = {},
) {
  const element = path ? (
    <Routes>
      <Route path={path} element={ui} />
    </Routes>
  ) : (
    ui
  )
  return renderInterne(<ThemeProvider>{element}</ThemeProvider>, { route, user })
}
