import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, type RenderOptions } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { hasAnyRole } from '@/shared/auth/claims'
import { AuthContext } from '@/shared/auth/context'
import type { AuthContextValue, AuthUser } from '@/shared/auth/types'
import type { Role } from '@/shared/domain/labels'
import { TooltipProvider } from '@/shared/ui/tooltip'

/** Utilisateur de test configurable. */
export function testUser(
  roles: Role[] = ['RESPONSABLE_MATERIEL'],
  overrides: Partial<AuthUser> = {},
): AuthUser {
  return {
    id: 'u-1',
    username: 'test',
    name: 'Test Utilisateur',
    email: 'test@cse.sn',
    roles,
    chantierIds: [],
    ...overrides,
  }
}

/** Valeur d'AuthContext pour les tests (sans Keycloak). */
export function authValue(user: AuthUser | null): AuthContextValue {
  return {
    status: user ? 'authenticated' : 'unauthenticated',
    user,
    mode: 'dev',
    error: null,
    hasAnyRole: (roles) => hasAnyRole(user?.roles ?? [], roles),
    login: () => Promise.resolve(),
    logout: () => Promise.resolve(),
    getToken: () => Promise.resolve(null),
    devSignIn: undefined,
  }
}

interface Options extends Omit<RenderOptions, 'wrapper'> {
  user?: AuthUser | null
  route?: string
}

/**
 * Rendu avec routeur memoire, QueryClient isole, infobulles et session simulee.
 * @example renderWithProviders(<MaPage />, { user: testUser(['DAF']), route: '/finance' })
 */
export function renderWithProviders(
  ui: ReactElement,
  { user = testUser(), route = '/', ...options }: Options = {},
) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <AuthContext.Provider value={authValue(user)}>
          <TooltipProvider>
            <MemoryRouter
              initialEntries={[route]}
              future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
            >
              {children}
            </MemoryRouter>
          </TooltipProvider>
        </AuthContext.Provider>
      </QueryClientProvider>
    )
  }
  return { queryClient, ...render(ui, { wrapper: Wrapper, ...options }) }
}
