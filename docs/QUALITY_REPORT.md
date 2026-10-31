# Rapport qualité : plateforme CSE v2.0.0

Mesures du **31 octobre 2026** sur le poste de développement (12 Go), à partir du code de la branche
principale. Méthode : `mvn -o -pl <module> test jacoco:report` pour chaque module (rapports Surefire et
`target/site/jacoco/jacoco.csv` lus et agrégés), `pnpm --filter @cse/web-interne test` et `coverage`.
La couverture « domaine + application » est le ratio de lignes couvertes des paquets `*.domain.*` et
`*.application.*`, celui contrôlé par la porte JaCoCo (seuil 80 %).

## 1. Synthèse

| Indicateur | Valeur | Cible | État |
|---|---|---|---|
| Tests backend | **894** (0 échec, 0 ignoré) | tous verts | ✅ |
| Couverture lignes domaine + application (services métier) | **97,2 % à 100 %** selon le service | ≥ 80 % | ✅ |
| Règles ArchUnit exécutées | 5 par service métier (45) + 2 pour la passerelle | vertes | ✅ |
| Opérations REST documentées (OpenAPI, figées par instantané) | **123** sur 9 services + 1 passerelle = 124 | 100 % | ✅ |
| Opérations protégées par `@PreAuthorize` | 124 / 124 | 100 % | ✅ |
| Tests frontend (Vitest) | **186** dans 45 fichiers, tous verts | verts | ✅ |
| Couverture frontend (lignes, tout `src/`) | **63,2 %** (branches 79 %) | ≥ 80 % sur logique/hooks | ⚠️ |
| Performance (k6, 50 utilisateurs) | p95 **23 ms**, p99 33 ms, 0 % d'erreur | p95 < 2 s, erreurs < 1 % | ✅ (campagne 150 VU à faire en recette) |
| Restauration de sauvegarde | testée : 74 tables identiques, base `cse` restaurée en 4 s | RTO < 4 h | ✅ |

## 2. Backend par module

| Module | Tests | Échecs | Couverture domaine + application | Couverture totale (lignes) | ArchUnit | Instantané OpenAPI |
|---|---|---|---|---|---|---|
| cse-common | 17 | 0 | n/a (socle) | 42,3 % | - | - |
| cse-events | 5 | 0 | n/a (contrats) | 48,6 % | - | - |
| ms-auth | 81 | 0 | 99,5 % (373/375) | 93,3 % | 5 ✅ | ✅ 8 opérations |
| ms-gateway | 18 | 0 | n/a (porte désactivée : pas de domaine) | 93,1 % | 2 ✅ | agrégation testée |
| ms-besoins | 94 | 0 | 99,5 % (652/655) | 97,6 % | 5 ✅ | ✅ 10 |
| ms-achats | 147 | 0 | 99,6 % (1 228/1 233) | 96,6 % | 5 ✅ | ✅ 36 |
| ms-fournisseurs | 143 | 0 | 99,5 % (846/850) | 97,4 % | 5 ✅ | ✅ 25 |
| ms-suivi | 125 | 0 | 99,5 % (1 248/1 254) | 95,0 % | 5 ✅ | ✅ 29 |
| ms-reporting | 70 | 0 | 98,5 % (1 286/1 306) | 94,9 % | 5 ✅ | ✅ 3 |
| ms-chatbot | 101 | 0 | 97,2 % (1 101/1 133) | 91,7 % | 5 ✅ | ✅ 5 |
| ms-notification | 65 | 0 | 100 % (296/296) | 95,2 % | 5 ✅ | ✅ 5 |
| ms-audit | 28 | 0 | 100 % (104/104) | 98,0 % | 5 ✅ | ✅ 2 |
| **Total** | **894** | **0** | | | | **123** |

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

## 3. Frontend (`@cse/web-interne`)

- 186 tests Vitest + Testing Library + MSW (hooks, schémas zod, mapping d'erreurs ProblemDetail, gardes de
  rôle, pages principales : besoins, validation, matériels, chantiers, maintenances, fournisseurs, contrats,
  finance, audit, tableau de bord).
- `typecheck` (TypeScript strict) et `lint` (`strictTypeChecked`, jsx-a11y, 0 avertissement toléré) dans
  `make front-verify` et la CI ; scripts de garde `check-palette.mjs` et `check-no-emoji.mjs`.
- Couverture globale 63,2 % des lignes : les pages et composants visuels tirent la moyenne vers le bas ;
  l'objectif de 80 % « sur la logique et les hooks » n'est pas mesuré séparément (pas de seuil
  configuré dans Vitest) : **écart à résorber**.
- Tests de bout en bout : Playwright pour web-interne (4/4, dont les 3 scénarios critiques) et scénarios
  API via la passerelle avec RestAssured (14/14) ; `make e2e`, voir `tests/README.md`.

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
2. Pas de domaine : porte JaCoCo désactivée (`jacoco.check.skip`), couverture totale 93 %.
3. Consommateurs uniquement (idempotents + DLT) ; ms-audit est la destination de l'audit.
4. ms-chatbot ne publie aucun événement : la création/suppression de conversations n'est donc pas auditée
   (données personnelles de l'utilisateur, non métier).
5. Tests et couverture ré-exécutés le 31/10/2026 ; `spotless:check` et le contrôle JaCoCo s'exécutent dans
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
- Portail fournisseur : API backend présentes, application front hors périmètre (sources vides).
- Écran Finance réservé DAF/DG alors que l'API autorise le RA à saisir des factures (pas d'écran RA).
- Campagne de charge 150-200 utilisateurs à exécuter sur l'environnement de recette.
