# Rapport qualité : plateforme CSE v2.1.0

Mesures du **1er novembre 2026** sur le poste de développement (12 Go), à partir du code de la branche
principale (version 2.1.0, portail fournisseur). Méthode : `mvn -o -pl <module> test jacoco:report` pour
chaque module (rapports Surefire et `target/site/jacoco/jacoco.csv` lus et agrégés), `make front-verify`
(web interne et portail fournisseur), `make e2e-ui` et `make e2e-api` contre la pile démarrée. La couverture
frontend (63,2 %) date de la mesure du 31 octobre (web interne).
La couverture « domaine + application » est le ratio de lignes couvertes des paquets `*.domain.*` et
`*.application.*`, celui contrôlé par la porte JaCoCo (seuil 80 %).

## 1. Synthèse

| Indicateur | Valeur | Cible | État |
|---|---|---|---|
| Tests backend | **1 017** (0 échec, 0 ignoré) | tous verts | ✅ |
| Couverture lignes domaine + application (services métier) | **97,2 % à 100 %** selon le service | ≥ 80 % | ✅ |
| Règles ArchUnit exécutées | 5 par service métier (45) + 2 pour la passerelle | vertes | ✅ |
| Opérations REST documentées (OpenAPI, figées par instantané) | **131** sur 9 services + 1 passerelle = 132 | 100 % | ✅ |
| Opérations protégées par `@PreAuthorize` | 132 / 132 (dont l'inscription fournisseur, publique explicitement) | 100 % | ✅ |
| Tests frontend (Vitest) | **271** : web interne 191 (45 fichiers), portail fournisseur 80 (13 fichiers), tous verts | verts | ✅ |
| Tests de bout en bout | Playwright **5/5** (dont le circuit complet avec le portail), RestAssured **16/16** | verts | ✅ |
| Couverture frontend (lignes, tout `src/`) | **63,2 %** (branches 79 %) | ≥ 80 % sur logique/hooks | ⚠️ |
| Performance (k6, 50 utilisateurs) | p95 **23 ms**, p99 33 ms, 0 % d'erreur | p95 < 2 s, erreurs < 1 % | ✅ (campagne 150 VU à faire en recette) |
| Restauration de sauvegarde | testée : 74 tables identiques, base `cse` restaurée en 4 s | RTO < 4 h | ✅ |

## 2. Backend par module

| Module | Tests | Échecs | Couverture domaine + application | Couverture totale (lignes) | ArchUnit | Instantané OpenAPI |
|---|---|---|---|---|---|---|
| cse-common | 17 | 0 | n/a (socle) | 42,3 % | - | - |
| cse-events | 5 | 0 | n/a (contrats) | 45,9 % | - | - |
| ms-auth | 104 | 0 | 99,0 % (483/488) | 94,1 % | 5 ✅ | ✅ 8 opérations |
| ms-gateway | 23 | 0 | n/a (porte désactivée : pas de domaine) | 92,3 % | 2 ✅ | agrégation testée |
| ms-besoins | 94 | 0 | 99,5 % (652/655) | 97,5 % | 5 ✅ | ✅ 10 |
| ms-achats | 226 | 0 | 99,7 % (1 329/1 333) | 97,3 % | 5 ✅ | ✅ 43 |
| ms-fournisseurs | 154 | 0 | 99,5 % (867/871) | 97,4 % | 5 ✅ | ✅ 26 |
| ms-suivi | 125 | 0 | 99,5 % (1 248/1 254) | 95,0 % | 5 ✅ | ✅ 29 |
| ms-reporting | 71 | 0 | 98,5 % (1 288/1 307) | 95,0 % | 5 ✅ | ✅ 3 |
| ms-chatbot | 101 | 0 | 97,2 % (1 101/1 133) | 91,7 % | 5 ✅ | ✅ 5 |
| ms-notification | 69 | 0 | 100 % (296/296) | 95,2 % | 5 ✅ | ✅ 5 |
| ms-audit | 28 | 0 | 100 % (104/104) | 98,0 % | 5 ✅ | ✅ 2 |
| **Total** | **1 017** | **0** | | | | **131** |

La couverture de `cse-common` et `cse-events` paraît basse car ces modules sont surtout exercés **par les
tests des services** (outbox, idempotence, sécurité, chiffrement testés avec PostgreSQL réel dans les tests
d'intégration de chaque service) ; JaCoCo ne compte que les tests du module lui-même.

Types de tests présents dans chaque service métier : domaine pur (une règle au moins par test nommé
`*_R0X`), cas d'utilisation sur ports simulés, `@WebMvcTest` sécurité (401/403/400/409/422 au format
ProblemDetail), 1 à 2 classes d'intégration Testcontainers PostgreSQL (persistance, outbox, consommateurs
idempotents, instantané OpenAPI, jeu de démonstration), ArchUnit. Tests d'adaptateurs HTTP avec WireMock
(ms-besoins → ms-suivi, ms-achats → ms-fournisseurs, ms-auth → Keycloak, ms-gateway). Test de concurrence
(double affectation simultanée → 409) dans ms-suivi.

Règles de gestion couvertes par des tests nommés : R01, R05, R08 (ms-besoins), R02, R03, R06, R10, R11, R14
(ms-achats), R07, R09, R14 (ms-fournisseurs), R04, R12, R13 (ms-suivi), R12 (ms-audit, lecture réservée à
l'administrateur). ms-auth, ms-chatbot, ms-notification et ms-reporting n'ont pas de règle numérotée propre.

## 3. Frontend (`@cse/web-interne` et `@cse/portail-fournisseur`)

- Web interne : 191 tests Vitest + Testing Library + MSW (hooks, schémas zod, mapping d'erreurs
  ProblemDetail, gardes de rôle, pages principales : besoins, validation, matériels, chantiers,
  maintenances, fournisseurs dont la validation d'inscription, contrats dont la signature en deux temps et
  le démarrage hors portail, finance, audit, tableau de bord).
- Portail fournisseur : 80 tests (schémas, domaine des contrats, factures et offres, liste « À faire »,
  inscription, dépôt d'offre, nouvelle facture, détail de contrat, garde d'authentification).
- `typecheck` (TypeScript strict) et `lint` (`strictTypeChecked`, jsx-a11y, 0 avertissement toléré) dans
  `make front-verify` et la CI ; scripts de garde `check-palette.mjs` et `check-no-emoji.mjs`.
- Couverture globale 63,2 % des lignes : les pages et composants visuels tirent la moyenne vers le bas ;
  l'objectif de 80 % « sur la logique et les hooks » n'est pas mesuré séparément (pas de seuil
  configuré dans Vitest) : **écart à résorber**.
- Tests de bout en bout (1er novembre 2026, pile complète démarrée) : Playwright **5/5** (scénarios 1 à 3
  critiques, scénario 4 « Circuit complet avec le fournisseur » qui pilote l'application interne et le
  portail) et RestAssured **16/16** via la passerelle (dont le portail : cloisonnement 404, inscription 201
  puis 409) ; `make e2e`, voir `tests/README.md`.

## 4. Définition de terminé par service

Légende : ✅ conforme et vérifié ; ⚠️ partiel ou non vérifiable automatiquement ; - sans objet.

| Critère | auth | gateway | besoins | achats | fournisseurs | suivi | reporting | chatbot | notification | audit |
|---|---|---|---|---|---|---|---|---|---|---|
| Clean Archi + ArchUnit vert | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Aucune valeur en dur (`@ConfigurationProperties`) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Javadoc FR sur l'API publique | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ | ⚠️¹ |
| Swagger complet + exemples FR | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tests verts, couverture ≥ 80 % | ✅ | ✅² | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Migration Flyway (`ddl-auto=validate`) | ✅ | - | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Événements / outbox / audit | ✅ | - | ✅ | ✅ | ✅ | ✅ | ✅³ | ⚠️⁴ | ✅³ | ✅³ |
| Sécurité par rôle testée (401/403) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| README du service à jour | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `make verify` vert | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ | ✅⁵ |

1. Javadoc française présente sur les classes publiques relues, mais aucun outil (Checkstyle, `-Xdoclint`)
   ne l'impose : non vérifiable exhaustivement.
2. Pas de domaine : porte JaCoCo désactivée (`jacoco.check.skip`), couverture totale 92 %.
3. Consommateurs uniquement (idempotents + DLT) ; ms-audit est la destination de l'audit.
4. ms-chatbot ne publie aucun événement : la création/suppression de conversations n'est donc pas auditée
   (données personnelles de l'utilisateur, non métier).
5. Tests et couverture ré-exécutés le 01/11/2026 ; `spotless:check` et le contrôle JaCoCo s'exécutent dans
   `mvn verify` (CI).

## 5. Portes de qualité outillées

| Outil | Où | État |
|---|---|---|
| Maven Enforcer (Java ≥ 17, Maven ≥ 3.9, versions dupliquées) | `mvn verify` | ✅ |
| Spotless (google-java-format 1.28, imports inutilisés) | `mvn verify` (`check`) | ✅ |
| JaCoCo ≥ 80 % lignes domaine + application | `mvn verify` | ✅ |
| ArchUnit (`HexagonalArchitectureRules`, forme `@TestFactory` réellement exécutée) | tests | ✅ |
| Instantanés OpenAPI | tests d'intégration | ✅ |
| ESLint strict + TypeScript strict + garde palette/emoji | `make front-verify` | ✅ |
| OWASP dependency-check (CVSS ≥ 7) + `pnpm audit` | CI hebdomadaire | ✅ (hors `make verify`) |
| Checkstyle | - | ⚠️ non branché (exigé par la spécification) |
| Journaux JSON structurés | - | ⚠️ format texte avec corrélation |

## 6. Dette connue

Écarts connus, repris des différents documents :

- Jobs planifiés sans verrou distribué (ShedLock à ajouter pour du multi-instance).
- Registre SSE et limiteur de débit de l'assistant en mémoire (mono-instance).
- 2FA : rôles concernés marqués `CONFIGURE_TOTP` à la création par ms-auth ; l'OTP n'est pas imposé aux
  comptes de démonstration importés.
- Client Keycloak `cse-tests-e2e` (mot de passe direct) à retirer du realm de production.
- TLS entre services non activé (procédure dans `OPERATIONS.md` §4).
- Pas de rotation outillée de la clé AES ; montants en clair dans Kafka et les vues de reporting.
- Couverture frontend 63 % < 80 % ; Checkstyle absent ; journaux non JSON.
- Portail fournisseur : couverture Vitest non mesurée ; pas de test Playwright de l'inscription publique
  jusqu'à la connexion (le lien de MailHog n'est pas automatisé).
- Écran Finance réservé DAF/DG alors que l'API autorise le RA à saisir des factures (pas d'écran RA).
- Campagne de charge 150-200 utilisateurs à exécuter sur l'environnement de recette.
