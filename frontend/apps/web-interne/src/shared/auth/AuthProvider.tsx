import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { configureHttp } from '@/shared/api/http'
import { env } from '@/shared/config/env'
import type { Role } from '@/shared/domain/labels'
import { hasAnyRole } from './claims'
import { AuthContext } from './context'
import type { AuthContextValue, AuthDriver, AuthStatus, AuthUser } from './types'

async function createDriver(): Promise<AuthDriver> {
  if (import.meta.env.DEV && env.authMode === 'dev') {
    const { createDevDriver } = await import('./drivers/dev')
    return createDevDriver()
  }
  const { createKeycloakDriver } = await import('./drivers/keycloak')
  return createKeycloakDriver(env.keycloak)
}

interface State {
  status: AuthStatus
  user: AuthUser | null
  error: Error | null
}

/**
 * Fournit la session a toute l'application et branche la couche HTTP
 * (jeton Bearer + redirection de connexion sur 401).
 * @param driverFactory permet d'injecter un pilote (tests)
 */
export function AuthProvider({
  children,
  driverFactory = createDriver,
}: {
  children: ReactNode
  driverFactory?: () => Promise<AuthDriver>
}) {
  const [state, setState] = useState<State>({ status: 'loading', user: null, error: null })
  const driverRef = useRef<AuthDriver | null>(null)
  const startedRef = useRef(false)

  useEffect(() => {
    if (startedRef.current) return
    startedRef.current = true
    const apply = (user: AuthUser | null) => {
      setState({ status: user ? 'authenticated' : 'unauthenticated', user, error: null })
    }
    driverFactory()
      .then(async (driver) => {
        driverRef.current = driver
        configureHttp({
          getToken: () => driver.getToken(),
          onUnauthorized: () => {
            if (driver.mode === 'keycloak') void driver.login()
          },
        })
        driver.subscribe(apply)
        apply(await driver.init())
      })
      .catch((error: unknown) => {
        setState({
          status: 'error',
          user: null,
          error: error instanceof Error ? error : new Error(String(error)),
        })
      })
  }, [driverFactory])

  const login = useCallback(async () => {
    await driverRef.current?.login()
  }, [])
  const logout = useCallback(async () => {
    await driverRef.current?.logout()
  }, [])
  const getToken = useCallback(async () => (await driverRef.current?.getToken()) ?? null, [])
  const devSignIn = useCallback((role: Role) => {
    const user = driverRef.current?.devSignIn?.(role)
    if (user) setState({ status: 'authenticated', user, error: null })
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      mode: env.authMode,
      hasAnyRole: (roles) => hasAnyRole(state.user?.roles ?? [], roles),
      login,
      logout,
      getToken,
      devSignIn: env.authMode === 'dev' ? devSignIn : undefined,
    }),
    [state, login, logout, getToken, devSignIn],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
