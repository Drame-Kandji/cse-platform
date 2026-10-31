// Test de charge CSE (k6) : utilisateurs simultanés répartis par rôle, endpoints de lecture principaux,
// à travers la passerelle (JWT + RBAC + limitation de débit réels). Voir tests/perf/README.md.
//
// Variables (-e NOM=valeur) :
//   BASE_URL        passerelle ou frontal          (défaut http://localhost:8080)
//   KEYCLOAK_URL    Keycloak                       (défaut http://localhost:8180)
//   REALM           realm                          (défaut cse)
//   CLIENT_ID       client « password grant » de DÉVELOPPEMENT, absent en production (défaut cse-tests-e2e)
//   MOT_DE_PASSE    mot de passe des comptes de démonstration (défaut Cse@2026, valeur de dev du realm)
//   VUS             utilisateurs virtuels au plateau (défaut 150 ; cible du cahier des charges : 100 à 200)
//   MONTEE / PLATEAU / DESCENTE  durées des paliers (défaut 1m / 5m / 30s)
//   PAUSE_MIN / PAUSE_MAX        temps de réflexion entre deux écrans, en secondes (défaut 1 / 3)
//   INSECURE        "true" pour accepter le certificat auto-signé du frontal HTTPS de développement
import http from 'k6/http'
import { check, group, sleep } from 'k6'
import { Counter, Trend } from 'k6/metrics'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:8180'
const REALM = __ENV.REALM || 'cse'
const CLIENT_ID = __ENV.CLIENT_ID || 'cse-tests-e2e'
const MOT_DE_PASSE = __ENV.MOT_DE_PASSE || 'Cse@2026'
const VUS = parseInt(__ENV.VUS || '150', 10)
const PAUSE_MIN = parseFloat(__ENV.PAUSE_MIN || '1')
const PAUSE_MAX = parseFloat(__ENV.PAUSE_MAX || '3')

// Répartition approximative de la population réelle : beaucoup de chefs de chantier, peu de direction.
// Chaque rôle n'appelle que des endpoints qu'il a le droit de lire (un 403 serait compté comme erreur).
const PROFILS = [
  {
    role: 'CHEF_CHANTIER',
    compte: 'chef.chantier@cse.sn',
    poids: 35,
    lectures: ['/api/me', '/api/besoins?size=20', '/api/materiels?size=20', '/api/chantiers', '/api/notifications', '/api/maintenances?size=20'],
  },
  {
    role: 'RESPONSABLE_MATERIEL',
    compte: 'resp.materiel@cse.sn',
    poids: 25,
    lectures: ['/api/me', '/api/materiels?size=20', '/api/maintenances?size=20', '/api/besoins?size=20', '/api/chantiers', '/api/besoins/statistiques', '/api/notifications'],
  },
  {
    role: 'RESPONSABLE_ACHATS',
    compte: 'resp.achats@cse.sn',
    poids: 20,
    lectures: ['/api/me', '/api/appels-offres?size=20', '/api/contrats?size=20', '/api/factures?size=20', '/api/engagements?size=20', '/api/besoins?size=20', '/api/notifications'],
  },
  {
    role: 'DAF',
    compte: 'daf@cse.sn',
    poids: 10,
    lectures: ['/api/me', '/api/engagements?size=20', '/api/factures?size=20', '/api/contrats?size=20', '/api/materiels?size=20', '/api/notifications'],
  },
  {
    role: 'DIRECTION_GENERALE',
    compte: 'dg@cse.sn',
    poids: 10,
    lectures: ['/api/me', '/api/plateforme/etat', '/api/materiels?size=20', '/api/besoins?size=20', '/api/contrats?size=20', '/api/appels-offres?size=20', '/api/notifications'],
  },
]

const limiteDebit = new Counter('cse_reponses_429')
const dureeEcran = new Trend('cse_duree_ecran', true)

export const options = {
  insecureSkipTLSVerify: __ENV.INSECURE === 'true',
  scenarios: {
    utilisateurs: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.MONTEE || '1m', target: VUS },
        { duration: __ENV.PLATEAU || '5m', target: VUS },
        { duration: __ENV.DESCENTE || '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  // Exigences non fonctionnelles : p95 < 2 s et moins de 1 % d'erreurs sur l'API (hors obtention des jetons).
  thresholds: {
    'http_req_duration{type:api}': ['p(95)<2000'],
    'http_req_failed{type:api}': ['rate<0.01'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

// Sélection pondérée et stable d'un profil par utilisateur virtuel.
function profilDuVu() {
  const total = PROFILS.reduce((s, p) => s + p.poids, 0)
  let tirage = (__VU * 7919) % total
  for (const p of PROFILS) {
    if (tirage < p.poids) return p
    tirage -= p.poids
  }
  return PROFILS[0]
}

// Jeton par utilisateur virtuel, renouvelé 60 s avant expiration (durée de vie realm : 15 min).
let jeton = null
let expiration = 0

function jetonValide(profil) {
  const maintenant = Date.now() / 1000
  if (jeton && maintenant < expiration - 60) return jeton
  const res = http.post(
    `${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token`,
    { client_id: CLIENT_ID, grant_type: 'password', username: profil.compte, password: MOT_DE_PASSE },
    { tags: { type: 'jeton', name: 'keycloak-token' } },
  )
  if (!check(res, { 'jeton obtenu': (r) => r.status === 200 })) {
    jeton = null
    return null
  }
  const corps = res.json()
  jeton = corps.access_token
  expiration = maintenant + corps.expires_in
  return jeton
}

export default function () {
  const profil = profilDuVu()
  const acces = jetonValide(profil)
  if (!acces) {
    sleep(PAUSE_MAX)
    return
  }
  // Un « écran » = 2 à 3 lectures parallèles (comme le frontend avec TanStack Query).
  const nb = 2 + Math.floor(Math.random() * 2)
  const chemins = []
  for (let i = 0; i < nb; i++) chemins.push(profil.lectures[Math.floor(Math.random() * profil.lectures.length)])

  group(profil.role, () => {
    const debut = Date.now()
    const reponses = http.batch(
      chemins.map((c) => ({
        method: 'GET',
        url: `${BASE_URL}${c}`,
        params: {
          headers: { Authorization: `Bearer ${acces}`, Accept: 'application/json' },
          // Nom agrégé sans paramètres de requête, pour des métriques lisibles par endpoint.
          tags: { type: 'api', role: profil.role, name: c.split('?')[0] },
        },
      })),
    )
    dureeEcran.add(Date.now() - debut, { role: profil.role })
    for (const r of reponses) {
      if (r.status === 429) limiteDebit.add(1, { role: profil.role })
      check(r, { 'statut 200': (x) => x.status === 200 })
    }
  })
  sleep(PAUSE_MIN + Math.random() * (PAUSE_MAX - PAUSE_MIN))
}
