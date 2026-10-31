# Guide du développeur

> Pour qui rejoint le projet. Lire d'abord : [README](../README.md) (démarrage), [architecture.md](architecture.md),
> puis les conventions détaillées : [BACKEND_CONVENTIONS.md](BACKEND_CONVENTIONS.md) (back) et
> [`frontend/apps/web-interne/CONVENTIONS.md`](../frontend/apps/web-interne/CONVENTIONS.md) (front).
> Règles permanentes du projet : [AGENTS.md](../AGENTS.md). Le contrat [API_CONTRACT.md](API_CONTRACT.md) fait foi.

## 1. Poste de travail

| Outil | Version | Vérification |
|---|---|---|
| JDK | 17 | `java -version` |
| Maven | ≥ 3.9 (règle enforcer) | `mvn -v` |
| Node.js / pnpm | 20 / 10 | `node -v`, `pnpm -v` |
| Docker + Compose v2 | Docker ≥ 29 accepté (Testcontainers 1.21.4) | `docker compose version` |
| Mémoire | 12 Go conseillés | voir [OPERATIONS.md](OPERATIONS.md) §9 |

```bash
cp .env.example .env && sed -i "s|^CSE_CRYPTO_KEY=.*|CSE_CRYPTO_KEY=$(head -c32 /dev/urandom | base64)|" .env
make infra build start front      # http://localhost:5173
```

Front sans backend : `pnpm --filter @cse/web-interne dev:ui` (mode `VITE_AUTH_MODE=dev`, sélecteur de rôle,
réservé à `vite dev`).

## 2. Organisation du dépôt

```
backend/              Maven multi-modules (pom parent : versions, Spotless, JaCoCo, enforcer)
  cse-common/         socle commun (sécurité, erreurs, pagination, outbox, Kafka, crypto, ArchUnit, test-jar)
  cse-events/         contrats d'événements (enveloppe, topics, types, payloads)
  ms-*/               10 microservices (README par service)
frontend/             monorepo pnpm : apps/web-interne (application), apps/portail-fournisseur (hors périmètre), packages/*
infra/                docker-compose, init PostgreSQL, realm Keycloak, Nginx, sauvegardes, certificats (non versionnés)
scripts/              cse.sh (pilotage local), token.sh, smoke.sh, backup-now.sh, restore.sh, gen-certs.sh
tests/perf/           test de charge k6
docs/                 documentation, ADR, collections .http, captures
```

## 3. Conventions (résumé)

- **Langue** : domaine, Javadoc/TSDoc, commentaires, messages d'erreur, commits et documentation en français ;
  suffixes techniques en anglais (`*Controller`, `*UseCase`, `*Repository`, `*Entity`, `*Adapter`).
- **Hexagonal** : `domain` (Java pur) ← `application` (ports + cas d'utilisation `@Service @Transactional`)
  ← `infrastructure` (JPA, Kafka, clients REST, config, seed) et `api` (contrôleurs fins, DTO records).
- **Zéro valeur en dur** : seuils, poids, délais, crons → `@ConfigurationProperties` validées
  (`cse.<service>.*`), surchargeables par variable d'environnement ; rôles via `Roles` ; horloge injectée (`Clock`).
- **Base** : Flyway (`ddl-auto=validate`), UUID côté application, `ENUM` PostgreSQL natifs, `@Version`,
  colonnes d'audit, index, séquences annuelles pour les références lisibles.
- **API** : chemins du contrat, pagination `PageResponse<T>` (20 par défaut, 50 max, tri en liste blanche),
  erreurs RFC 7807 avec `code` stable, Swagger complet (`@Operation`, `@ApiResponse`, `@Schema(example)` en français).
- **Interdits** (A.2) : `-DskipTests`, `@Disabled`, `catch` vide, `@SuppressWarnings` de complaisance, TODO sans
  ticket, `System.out`, `printStackTrace`, données factices en production, `e.getMessage()` renvoyé au client,
  désactivation de la sécurité.
- **Front** : feature-sliced, TanStack Query, react-hook-form + zod, jetons de couleur uniquement, pas de `any`,
  pas d'emoji (scripts de garde), chemins via `paths`/`to`, droits via `access.ts`.

## 4. Recettes

### 4.1 Ajouter un cas d'utilisation (backend)

Exemple : « clôturer un litige » dans ms-fournisseurs.

1. **Domaine** : méthode métier sur l'agrégat (`Litige.classer(auteur, commentaire)`), qui vérifie la transition
   et lève une exception métier (`ConflictException`/`BusinessRuleException` ou sous-classe de
   `domain.exception`) avec un code stable. Test unitaire pur, nommé `should_…_when_…[_R0X]`.
2. **Port entrant** : `application/port/in/ClasserLitigeUseCase` + commande (`record ClasserLitigeCommand`).
3. **Service** : `application/service/…Service implements ClasserLitigeUseCase`, `@Transactional` : charge via
   le port sortant, appelle le domaine, sauvegarde, publie l'événement via le port `…EventPublisher`.
   Contrôle de périmètre avec `CurrentUser` passé dans la commande. Test avec ports simulés / fakes en mémoire.
4. **Adaptateurs** : implémentation JPA si nouveau besoin de persistance ; implémentation du publisher via
   `OutboxWriter.publier(...)`.
5. **API** : méthode de contrôleur avec `@PreAuthorize(Acces.XXX)`, DTO de requête validé (`@Valid`), mapping
   DTO → commande → DTO de réponse. Test `@WebMvcTest` : 200/201, 400, 401 sans jeton, 403 mauvais rôle,
   409/422 en ProblemDetail (`.with(CseJwt.as(id, Roles.RESPONSABLE_ACHATS))`).
6. Mettre à jour `docs/API_CONTRACT.md`, le README du service et régénérer l'instantané OpenAPI (§6.3).

### 4.2 Ajouter un endpoint de lecture

Port `…QueryPort` (requête dédiée, projection) plutôt que de charger des agrégats ; pagination via
`PageableFactory.query(page, size, sort, TRIABLES, "champ,desc")` ; ajouter le champ à la liste blanche de tri
si besoin ; filtre de périmètre pour les chefs de chantier (`CurrentUser.estRestreintAuxChantiers()`).

### 4.3 Ajouter un événement

1. `cse-events` : constante dans `EventTypes` ; payload existant enrichi d'un champ **optionnel** (compatible) ou
   nouveau record dans `sn.cse.events.payload` ; `mvn -o -q -pl cse-events install`.
2. Producteur : publier dans la transaction métier (`OutboxWriter`, voir [events.md](events.md) §4). L'entrée
   d'audit est automatique.
3. Consommateurs : `@KafkaListener(topics = Topics.X, groupId = "${spring.application.name}")` sur un `String`,
   `EventEnvelopeReader.lire`, filtre sur `type`, `IdempotentEventHandler.traiterUneFois(eventId,
   "<service>.<consommateur>", …)` ; jamais de `catch` qui avale l'erreur (rejeu + DLT gérés par cse-common).
   Test : même événement deux fois → effet unique.
4. Notification éventuelle : règle YAML dans `ms-notification/src/main/resources/application.yml`
   (`cse.notification.regles`), validée au démarrage.
5. Reporting : cas d'aiguillage dans ms-reporting si l'indicateur en dépend (les types inconnus sont ignorés).
6. Documenter : [events.md](events.md) et `API_CONTRACT.md` §11.

### 4.4 Ajouter un microservice

1. Module `backend/ms-xxx` (copier le POM d'un service proche), ajouté aux `<modules>` du POM parent.
2. Port libre, schéma et utilisateur : ajouter `xxx` à `SERVICES` dans `infra/init/01-init-databases.sh`
   (nouvelle base uniquement) **et** créer le schéma sur une base existante
   (`CREATE ROLE ms_xxx …; CREATE SCHEMA xxx AUTHORIZATION ms_xxx; ALTER ROLE ms_xxx SET search_path = xxx;`).
3. `application.yml` type ([BACKEND_CONVENTIONS.md](BACKEND_CONVENTIONS.md) §2) ; Flyway `classpath:db/migration,classpath:db/common`.
4. Route(s) dans `ms-gateway` (`cse.gateway.services`, variable `MS_XXX_URL`) et dans la sonde de plateforme.
5. Service dans `infra/docker-compose.yml` (profil `apps`), entrée `SERVICES` de `scripts/cse.sh`, URI de
   redirection Swagger du client `cse-swagger`.
6. `ArchitectureTest` (forme `@TestFactory`, §6.2), snapshot OpenAPI, README, ADR si décision structurante.

### 4.5 Ajouter un écran (front)

Suivre [CONVENTIONS.md](../frontend/apps/web-interne/CONVENTIONS.md) §3 : page dans `features/<module>/pages`
exportant `Component`, chemin dans `app/routes.ts`, droits dans `app/access.ts`, route `guarded(...)` dans
`app/router.tsx`, entrée de menu dans `app/navigation.ts` + libellé `locales/fr/navigation.json`. Types DTO
dans `features/<module>/types.ts` (miroir du contrat), appels dans `api/`, hooks dans `hooks/`, tests Vitest + MSW.

## 5. Données de démonstration

Chaque service a un `infrastructure/seed/DevDataSeeder` (profil `dev`, idempotent) qui passe par le domaine et
l'outbox ([ADR-0008](adr/0008-donnees-de-demonstration.md)). Identifiants partagés : `API_CONTRACT.md` §12.
Pour repartir d'un jeu propre : `scripts/cse.sh reset` ([OPERATIONS.md](OPERATIONS.md) §10). Jetons de test :
`TOKEN=$(scripts/token.sh resp.materiel@cse.sn)`.

## 6. Tests et portes de qualité

### 6.1 Pyramide

| Niveau | Outils | Où |
|---|---|---|
| Domaine | JUnit 5 + AssertJ, sans Spring | `src/test/java/.../domain` — au moins un test par règle (`*_R0X`) |
| Application | Mockito ou fakes en mémoire | `.../application` |
| API / sécurité | `@WebMvcTest` + `spring-security-test` + `CseJwt` | 401/403/400/409/422 en ProblemDetail |
| Adaptateurs HTTP | WireMock (avec les vrais aspects Resilience4j) | ms-besoins, ms-achats, ms-auth, ms-gateway |
| Intégration | `@SpringBootTest` + Testcontainers PostgreSQL (`PostgresTestConfiguration`), Kafka non requis | 1 à 2 classes par service (mémoire limitée) |
| Architecture | ArchUnit (`HexagonalArchitectureRules`) | `ArchitectureTest` par service |
| Contrat | instantané OpenAPI `src/test/resources/openapi.json` | 9 services |
| Front | Vitest + Testing Library + MSW | `*.test.ts(x)` à côté du code |
| Bout en bout | tests de bout en bout (Playwright pour web-interne, API via la passerelle) | `make e2e` (plateforme démarrée) |
| Charge | k6 | `make perf`, [`tests/perf/README.md`](../tests/perf/README.md) |
| Fumée | curl par rôle | `scripts/smoke.sh` |

### 6.2 Commandes

```bash
make verify                                    # porte complète : backend (mvn verify) puis frontend
make test                                      # tests backend seuls
cd backend && flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m \
  mvn -o -q -pl ms-besoins spotless:apply verify   # un seul service (cse-common/cse-events installés)
pnpm --dir frontend --filter @cse/web-interne typecheck|lint|test|build
pnpm --dir frontend --filter @cse/web-interne coverage
```

`mvn verify` exécute, par module : enforcer (Java ≥ 17, Maven ≥ 3.9, pas de version dupliquée), compilation,
tests (Surefire, `-Xmx512m`), rapport JaCoCo (`target/site/jacoco/`), **contrôle JaCoCo ≥ 80 % des lignes
sur `domain` + `application`** (`jacoco.minimum.coverage`, désactivé pour ms-gateway qui n'a pas ces couches),
et `spotless:check` (google-java-format). Corriger le format : `mvn spotless:apply`.

> Checkstyle et OWASP dependency-check ne sont pas branchés dans `mvn verify` : dependency-check tourne dans
> la CI hebdomadaire (`.github/workflows/dependency-check.yml`).

Forme ArchUnit **qui s'exécute réellement** (la forme `@ArchTest ArchTests.in(...)` n'est pas vue par Surefire) :

```java
class ArchitectureTest {
  @TestFactory
  Stream<DynamicTest> regles() {
    JavaClasses classes = HexagonalArchitectureRules.classesDe("sn.cse.xxx");
    return HexagonalArchitectureRules.toutes().stream()
        .map(r -> DynamicTest.dynamicTest(r.getDescription(), () -> r.check(classes)));
  }
}
```

### 6.3 Régénérer un instantané OpenAPI

Le test compare `/v3/api-docs` à `src/test/resources/openapi.json` ; toute évolution volontaire du contrat
se régénère explicitement (le drapeau diffère selon les services) :

| Service | Commande |
|---|---|
| ms-achats | `mvn -o -q -pl ms-achats verify -Dcse.openapi.regenerer=true` |
| ms-auth, ms-besoins | `mvn -o -q -pl ms-auth verify -Dopenapi.snapshot.update=true` |
| ms-audit, ms-chatbot, ms-fournisseurs, ms-notification, ms-reporting, ms-suivi | `mvn -o -q -pl ms-suivi verify -Dopenapi.regenerer=true` |

Relire le diff de `openapi.json` dans la revue : c'est le contrat publié.

### 6.4 Pièges connus

- L'IDE qui recompile dans `target/classes` peut désynchroniser les classes : `mvn clean` sur le module.
- Ne jamais lancer deux builds Maven en parallèle ni `mvn install -am` pendant qu'un autre build tourne
  (`flock /tmp/cse-maven.lock`).
- Testcontainers exige Docker ; sur Docker 29, Testcontainers ≥ 1.21.4 ([ADR-0005](adr/0005-versions-socle.md)).

## 7. Flux Git

- Branche par sujet depuis `main` (`feat/besoins-consolidation`, `fix/reporting-dates`), PR vers `main`.
- **Conventional Commits en français**, atomiques : `feat(ms-achats): …`, `fix(web-interne): …`, `docs: …`,
  `test(e2e): …`, `chore(scripts): …` (voir `git log`).
- La CI (`.github/workflows/ci.yml`) doit être verte : backend `mvn -B verify`, frontend typecheck/lint/test/build,
  images sur `main`. `make verify` en local avant de pousser.
- Toute décision structurante → ADR dans `docs/adr/` + ligne dans [DECISIONS.md](../DECISIONS.md).
- Mettre à jour `PROGRESS.md`, le README du service et `CHANGELOG.md` à chaque lot (DoD A.9).

## 8. Collections HTTP

`docs/http/*.http` (format REST Client VS Code / IntelliJ HTTP Client) : renseigner la variable `@token`
avec `scripts/token.sh <e-mail>` et lancer les requêtes contre la passerelle `http://localhost:8080`.
