import { z } from 'zod'

const emptyToUndefined = (value: unknown) => (value === '' ? undefined : value)

const envSchema = z.object({
  VITE_API_BASE_URL: z.preprocess(emptyToUndefined, z.string().min(1).default('/api')),
  VITE_KEYCLOAK_URL: z.preprocess(emptyToUndefined, z.string().url().default('http://localhost:8180')),
  VITE_KEYCLOAK_REALM: z.preprocess(emptyToUndefined, z.string().min(1).default('cse')),
  VITE_KEYCLOAK_CLIENT_ID: z.preprocess(emptyToUndefined, z.string().min(1).default('cse-web-interne')),
  VITE_AUTH_MODE: z.preprocess(emptyToUndefined, z.enum(['keycloak', 'dev']).default('keycloak')),
})

export type AuthMode = 'keycloak' | 'dev'

export interface AppEnv {
  readonly apiBaseUrl: string
  readonly keycloak: { readonly url: string; readonly realm: string; readonly clientId: string }
  readonly authMode: AuthMode
  readonly isDev: boolean
}

/**
 * Valide et normalise les variables d'environnement Vite.
 * Le mode d'authentification "dev" est refuse hors serveur de developpement.
 * @throws Error si une variable est invalide
 */
export function parseEnv(raw: Record<string, unknown>, isDev: boolean): AppEnv {
  const result = envSchema.safeParse(raw)
  if (!result.success) {
    const details = result.error.issues.map((issue) => `${issue.path.join('.')}: ${issue.message}`).join('; ')
    throw new Error(`Configuration invalide : ${details}`)
  }
  const value = result.data
  if (value.VITE_AUTH_MODE === 'dev' && !isDev) {
    throw new Error('VITE_AUTH_MODE=dev est interdit hors du serveur de developpement.')
  }
  return {
    apiBaseUrl: value.VITE_API_BASE_URL.replace(/\/+$/, ''),
    keycloak: {
      url: value.VITE_KEYCLOAK_URL,
      realm: value.VITE_KEYCLOAK_REALM,
      clientId: value.VITE_KEYCLOAK_CLIENT_ID,
    },
    authMode: value.VITE_AUTH_MODE,
    isDev,
  }
}

/** Configuration typee de l'application (lue une seule fois au demarrage). */
export const env: AppEnv = parseEnv(import.meta.env, import.meta.env.DEV)
