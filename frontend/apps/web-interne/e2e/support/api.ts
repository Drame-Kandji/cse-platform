import { expect } from '@playwright/test'
import { comptesApi, e2eEnv, type RoleApi } from './env'

/**
 * Client API de préparation des données (passerelle + jetons Keycloak du client de développement
 * `cse-tests-e2e`, password grant). Sert uniquement aux étapes préparatoires et aux vérifications
 * de cohérence à terme : les décisions métier testées passent par l'interface.
 */
const jetons = new Map<RoleApi, { valeur: string; expireLe: number }>()

async function jeton(role: RoleApi): Promise<string> {
  const enCache = jetons.get(role)
  if (enCache && enCache.expireLe > Date.now() + 30_000) return enCache.valeur
  const reponse = await fetch(`${e2eEnv.keycloakURL}/realms/${e2eEnv.realm}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      client_id: e2eEnv.testClientId,
      grant_type: 'password',
      username: comptesApi[role],
      password: e2eEnv.password,
    }),
  })
  if (!reponse.ok) throw new Error(`Jeton impossible pour ${role} : HTTP ${reponse.status}`)
  const corps = (await reponse.json()) as { access_token: string; expires_in: number }
  jetons.set(role, { valeur: corps.access_token, expireLe: Date.now() + corps.expires_in * 1000 })
  return corps.access_token
}

/** Appel brut : renvoie le statut et le corps JSON (ou null). */
export async function appeler(
  role: RoleApi,
  methode: 'GET' | 'POST' | 'PUT' | 'PATCH',
  chemin: string,
  corps?: unknown,
): Promise<{ status: number; body: unknown }> {
  const reponse = await fetch(`${e2eEnv.gatewayURL}${chemin}`, {
    method: methode,
    headers: {
      Authorization: `Bearer ${await jeton(role)}`,
      ...(corps === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: corps === undefined ? undefined : JSON.stringify(corps),
  })
  const texte = await reponse.text()
  return { status: reponse.status, body: texte ? (JSON.parse(texte) as unknown) : null }
}

/** Appel qui doit réussir (2xx), sinon échec explicite avec le ProblemDetail. */
export async function api<T>(
  role: RoleApi,
  methode: 'GET' | 'POST' | 'PUT' | 'PATCH',
  chemin: string,
  corps?: unknown,
): Promise<T> {
  const { status, body } = await appeler(role, methode, chemin, corps)
  if (status >= 300) {
    throw new Error(`${methode} ${chemin} (${role}) -> HTTP ${status} : ${JSON.stringify(body)}`)
  }
  return body as T
}

/** Attend qu'une lecture satisfasse le prédicat (cohérence à terme via Kafka). */
export async function attendre<T>(
  lecture: () => Promise<T>,
  predicat: (valeur: T) => boolean,
  message: string,
  timeout = 60_000,
): Promise<T> {
  let derniere: T | undefined
  await expect
    .poll(
      async () => {
        derniere = await lecture()
        return predicat(derniere)
      },
      { message, timeout, intervals: [500, 1000, 2000] },
    )
    .toBe(true)
  return derniere as T
}

export interface Page<T> {
  content: T[]
  totalElements: number
}

export interface MaterielDto {
  id: string
  code: string
  designation: string
  etat: string
  chantierActuelId: string | null
}

export interface BesoinDto {
  id: string
  reference: string
  statut: string
  designation: string
}

export interface AffectationDto {
  id: string
  materielId: string
  materielCode: string
  materielDesignation: string
  chantierId: string
  besoinId: string | null
  contratId: string | null
  statut: string
}
