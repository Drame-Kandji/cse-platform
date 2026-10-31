import { describe, expect, it } from 'vitest'
import { parseEnv } from './env'

describe('parseEnv', () => {
  it('applique les valeurs par defaut', () => {
    expect(parseEnv({}, true)).toEqual({
      apiBaseUrl: '/api',
      keycloak: { url: 'http://localhost:8180', realm: 'cse', clientId: 'cse-web-interne' },
      authMode: 'keycloak',
      isDev: true,
    })
  })
  it('traite les chaines vides comme absentes et retire le slash final', () => {
    expect(parseEnv({ VITE_API_BASE_URL: 'https://api.cse.sn/', VITE_AUTH_MODE: '' }, false)).toMatchObject({
      apiBaseUrl: 'https://api.cse.sn',
      authMode: 'keycloak',
    })
  })
  it('refuse le mode dev hors serveur de developpement', () => {
    expect(() => parseEnv({ VITE_AUTH_MODE: 'dev' }, false)).toThrow(/interdit/)
    expect(parseEnv({ VITE_AUTH_MODE: 'dev' }, true).authMode).toBe('dev')
  })
  it('rejette une configuration invalide', () => {
    expect(() => parseEnv({ VITE_KEYCLOAK_URL: 'pas-une-url' }, true)).toThrow(/VITE_KEYCLOAK_URL/)
    expect(() => parseEnv({ VITE_AUTH_MODE: 'demo' }, true)).toThrow(/VITE_AUTH_MODE/)
  })
})
