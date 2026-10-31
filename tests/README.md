# Tests de bout en bout — Plateforme CSE

Les 3 scénarios critiques (spec §6.2) sont couverts à deux niveaux, **contre la pile qui tourne** :

| Niveau | Emplacement | Outils | Contenu |
|---|---|---|---|
| Interface | `frontend/apps/web-interne/e2e/` | Playwright (chromium) | 4 tests : besoin validé (affectation directe) et rejeté, cycle d'achat complet, réception + maintenance |
| API | `tests/e2e-api/` | JUnit 5, RestAssured, AssertJ, Awaitility | 14 tests : les 3 scénarios, RBAC, forme ProblemDetail (R02, R04, R05, R06, R08, R10, R11, R12) |
| Charge | `tests/perf/` | k6 | voir `tests/perf/README.md` |

## Prérequis

```bash
make infra && make start && make front   # PostgreSQL, Kafka, Keycloak… + microservices (profil dev) + Vite :5173
cd frontend/apps/web-interne && npx playwright install chromium   # une fois
```

- Données de démonstration (profil `dev`, contrat d'API §12) et comptes `*@cse.sn` / `Cse@2026`.
- Client Keycloak de développement `cse-tests-e2e` (password grant, absent en production) : jetons des
  préparations Playwright et des tests API (voir `scripts/token.sh`).

## Lancer

```bash
make e2e                                   # API puis interface
make e2e-api                               # mvn -f tests/e2e-api/pom.xml verify -De2e.gateway=http://localhost:8080
make e2e-ui                                # pnpm --filter @cse/web-interne test:e2e
```

Variables : `E2E_GATEWAY` / `E2E_BASE_URL` (Makefile), côté Playwright `E2E_BASE_URL`, `E2E_GATEWAY_URL`,
`E2E_KEYCLOAK_URL` ; côté Maven `-De2e.gateway`, `-De2e.keycloak`, `-De2e.realm`, `-De2e.client`,
`-De2e.password`. Le module `tests/e2e-api` n'est **pas** listé dans `backend/pom.xml` : le build normal
n'exige pas de pile démarrée.

## Fonctionnement (Playwright)

- `e2e/global-setup.ts` connecte chaque rôle (CHEF, RM, RA, DAF, DG, ADMIN) **une fois** par la vraie page
  de connexion Keycloak et enregistre la session dans `e2e/.auth/<role>.json` (ignoré par Git). Pour
  itérer sans reconnexion : `E2E_SKIP_LOGIN=1 pnpm test:e2e`.
- La fixture `en(role)` ouvre un contexte navigateur par rôle dans le même test (chef, RM, RA, DAF, DG).
- Préparations par API (`e2e/support/api.ts`) ; les décisions métier (soumission, validation, rejet,
  offres, clôture, notes, attribution, co-validation RM, approbation DG, signature, engagement DAF,
  livraison, réception, panne, maintenance) passent par l'interface. Localisateurs accessibles uniquement
  (`getByRole`, libellés), objets de page dans `e2e/pages/`.
- Rejouables : désignations et codes suffixés par un horodatage ; les attentes de cohérence à terme
  (Kafka : notifications, historique, matériels livrés) sondent ou rechargent la page.
- Échec : trace et capture dans `test-results/`, rapport HTML dans `playwright-report/`
  (`npx playwright show-report`).
