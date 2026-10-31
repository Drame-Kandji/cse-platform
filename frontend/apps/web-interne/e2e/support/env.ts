/**
 * Paramètres des tests de bout en bout (surchargés par variables d'environnement).
 * La pile doit tourner : `make infra && make start && make front`.
 */
export const e2eEnv = {
  baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
  gatewayURL: process.env.E2E_GATEWAY_URL ?? 'http://localhost:8080',
  keycloakURL: process.env.E2E_KEYCLOAK_URL ?? 'http://localhost:8180',
  realm: process.env.E2E_KEYCLOAK_REALM ?? 'cse',
  /** Client de développement (password grant), absent en production. */
  testClientId: process.env.E2E_KEYCLOAK_TEST_CLIENT ?? 'cse-tests-e2e',
  password: process.env.E2E_PASSWORD ?? 'Cse@2026',
} as const

/** Comptes de démonstration (contrat §12), un par rôle. */
export const comptes = {
  chef: 'chef.chantier@cse.sn',
  rm: 'resp.materiel@cse.sn',
  ra: 'resp.achats@cse.sn',
  daf: 'daf@cse.sn',
  dg: 'dg@cse.sn',
  admin: 'admin@cse.sn',
} as const

export type RoleE2e = keyof typeof comptes

/** Chantiers du chef de démonstration. */
export const chantiers = {
  dakar: { id: 'c0000000-0000-0000-0000-000000000001', nom: 'Extension autoroute Dakar–AIBD' },
  thies: { id: 'c0000000-0000-0000-0000-000000000002', nom: 'Contournement de Thiès RN2' },
} as const

/** Fournisseurs ACTIF spécialisés MATERIEL_ROULANT (contrat §12). */
export const fournisseurs = {
  sahel: { id: 'f0000000-0000-0000-0000-000000000001', nom: 'Sahel Engins SA' },
  teranga: { id: 'f0000000-0000-0000-0000-000000000002', nom: 'Teranga Équipements BTP' },
} as const

/** Fichier storageState d'un rôle (rempli par la configuration globale). */
export const storageStateOf = (role: RoleE2e) => `e2e/.auth/${role}.json`
