# Tests de bout en bout : Plateforme CSE

Les 3 scénarios critiques (spec §6.2) et le circuit complet avec le portail fournisseur sont couverts à
deux niveaux, **contre la pile qui tourne** :

| Niveau | Emplacement | Outils | Contenu |
|---|---|---|---|
| Interface | `frontend/apps/web-interne/e2e/` | Playwright (chromium) | 5 tests : besoin validé (affectation directe) et rejeté, cycle d'achat complet (contresignature du fournisseur, livraison hors portail), réception + maintenance, **circuit complet avec le fournisseur** (application interne + portail) |
| API | `tests/e2e-api/` | JUnit 5, RestAssured, AssertJ, Awaitility | 16 tests : les 3 scénarios, RBAC, portail fournisseur (cloisonnement 404, inscription publique 201 puis 409), forme ProblemDetail (R02, R04, R05, R06, R08, R10, R11, R12) |
| Charge | `tests/perf/` | k6 | voir `tests/perf/README.md` |

## Prérequis

```bash
make infra && make start && make front   # PostgreSQL, Kafka, Keycloak… + microservices (profil dev) + Vite :5173
make portail                              # portail fournisseur :5174 (autre terminal), requis par le scénario 4
cd frontend/apps/web-interne && npx playwright install chromium   # une fois
```

- Données de démonstration (profil `dev`, contrat d'API §12) et comptes `*@cse.sn` / `Cse@2026`, dont le
  fournisseur F1 `fournisseur@cse.sn` (Sahel Engins SA) pour le portail.
- Client Keycloak de développement `cse-tests-e2e` (password grant, absent en production) : jetons des
  préparations Playwright et des tests API (voir `scripts/token.sh`).

## Lancer

```bash
make e2e                                   # API puis interface
make e2e-api                               # mvn -f tests/e2e-api/pom.xml verify -De2e.gateway=http://localhost:8080
make e2e-ui                                # pnpm --filter @cse/web-interne test:e2e
```

Variables : `E2E_GATEWAY` / `E2E_BASE_URL` (Makefile), côté Playwright `E2E_BASE_URL`, `E2E_GATEWAY_URL`,
`E2E_PORTAIL_URL` (défaut http://localhost:5174), `E2E_KEYCLOAK_URL` ; côté Maven `-De2e.gateway`, `-De2e.keycloak`, `-De2e.realm`, `-De2e.client`,
`-De2e.password`. Le module `tests/e2e-api` n'est **pas** listé dans `backend/pom.xml` : le build normal
n'exige pas de pile démarrée.

## Fonctionnement (Playwright)

- `e2e/global-setup.ts` connecte chaque rôle (CHEF, RM, RA, DAF, DG, ADMIN) **une fois** par la vraie page
  de connexion Keycloak et enregistre la session dans `e2e/.auth/<role>.json` (ignoré par Git) ; il connecte
  aussi le fournisseur F1 au portail (`e2e/.auth/fournisseur.json`). Pour itérer sans reconnexion :
  `E2E_SKIP_LOGIN=1 pnpm test:e2e`.
- La fixture `en(role)` ouvre un contexte navigateur par rôle dans le même test (chef, RM, RA, DAF, DG) ; la
  fixture `portail()` ouvre le portail fournisseur (origine distincte) dans son propre contexte, avec
  reconnexion automatique si la session a expiré. Objets de page du portail : `e2e/pages/portail.ts`.
- Préparations par API (`e2e/support/api.ts`) ; les décisions métier (soumission, validation, rejet,
  offres, clôture, notes, attribution, co-validation RM, approbation DG, signature, engagement DAF,
  livraison, réception, panne, maintenance) passent par l'interface. Dans le scénario 2, la contresignature
  du fournisseur passe par l'API du portail (client `cse-tests-e2e`, compte `fournisseur@cse.sn`) ; le
  scénario 4 la fait dans le portail, comme le dépôt d'offre, l'expédition et la facture. Localisateurs accessibles uniquement
  (`getByRole`, libellés), objets de page dans `e2e/pages/`.
- Rejouables et indépendants : chaque scénario crée ses propres données (AO, contrat, facture), suffixées par
  un horodatage ; les attentes de cohérence à terme
  (Kafka : notifications, historique, matériels livrés) sondent ou rechargent la page.
- Échec : trace et capture dans `test-results/`, rapport HTML dans `playwright-report/`
  (`npx playwright show-report`).

## Scénarios

| Fichier | Scénario |
|---|---|
| `01-besoin-validation.spec.ts` | Scénario 1 · Soumission et validation d'un besoin (2 tests) |
| `02-cycle-achat.spec.ts` | Scénario 2 · Cycle complet d'achat : AO au-delà du seuil, RM + DG, signature CSE puis fournisseur, engagement DAF, livraison hors portail, matériel créé |
| `03-reception-maintenance.spec.ts` | Scénario 3 · Réception et clôture de maintenance |
| `04-circuit-fournisseur.spec.ts` | Scénario 4 · Circuit complet avec le fournisseur : AO publié par l'assistant (F1 invité), offre déposée sur le portail, attribution sous le seuil (RM seul), double signature, engagement DAF, expédition déclarée sur le portail, réception, facture déposée sur le portail, mandatement et paiement, clôture |

Côté API : `Scenario1BesoinValidationE2EIT`, `Scenario2CycleAchatE2EIT` (signature CSE, 403 si le RA tente
la signature fournisseur, contresignature par `Compte.FOURNISSEUR`, 409 `TRANSITION_INTERDITE` sur une
seconde signature), `Scenario3ReceptionMaintenanceE2EIT`, `RbacE2EIT`, `PortailFournisseurE2EIT`.

Données : les tests créent des AO, contrats, factures et inscriptions de test ; repartir des données de
démonstration avec `scripts/cse.sh reset --yes`.
