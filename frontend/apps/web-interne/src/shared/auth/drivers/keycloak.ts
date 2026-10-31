import { userFromClaims } from '../claims'
import type { AuthDriver, AuthUser } from '../types'

/** Duree minimale de validite restante (s) avant rafraichissement du jeton. */
const MIN_VALIDITY_SECONDS = 30

interface KeycloakDriverConfig {
  readonly url: string
  readonly realm: string
  readonly clientId: string
}

/**
 * Pilote Keycloak : Authorization Code + PKCE S256, SSO silencieux, rafraichissement auto.
 * keycloak-js est charge dynamiquement pour alleger le bundle initial.
 */
export async function createKeycloakDriver(config: KeycloakDriverConfig): Promise<AuthDriver> {
  const { default: Keycloak } = await import('keycloak-js')
  const keycloak = new Keycloak({ url: config.url, realm: config.realm, clientId: config.clientId })
  const listeners = new Set<(user: AuthUser | null) => void>()
  const emit = (user: AuthUser | null) => {
    listeners.forEach((listener) => {
      listener(user)
    })
  }
  const currentUser = () => (keycloak.authenticated ? userFromClaims(keycloak.tokenParsed) : null)

  return {
    mode: 'keycloak',
    async init() {
      keycloak.onTokenExpired = () => {
        keycloak.updateToken(MIN_VALIDITY_SECONDS).catch(() => {
          emit(null)
        })
      }
      keycloak.onAuthRefreshSuccess = () => {
        emit(currentUser())
      }
      keycloak.onAuthLogout = () => {
        emit(null)
      }
      await keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
      })
      return currentUser()
    },
    login: () => keycloak.login({ redirectUri: window.location.href }),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    async getToken() {
      if (!keycloak.authenticated) return null
      try {
        await keycloak.updateToken(MIN_VALIDITY_SECONDS)
      } catch {
        emit(null)
        return null
      }
      return keycloak.token ?? null
    },
    subscribe(listener) {
      listeners.add(listener)
      return () => {
        listeners.delete(listener)
      }
    },
  }
}
