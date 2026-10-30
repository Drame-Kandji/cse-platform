# Conventions backend (v2) — à suivre pour chaque microservice

Complète `AGENTS.md` (règles) et `docs/API_CONTRACT.md` (contrat). En cas de doute : le contrat fait foi.

## 1. Socle fourni par `cse-common` (ne pas réimplémenter)

| Besoin | Fourni | Usage |
|---|---|---|
| Sécurité JWT Keycloak + RBAC | `CseSecurityAutoConfiguration` (auto) | `@PreAuthorize("hasRole('RESPONSABLE_MATERIEL')")` sur les **contrôleurs** ; constantes dans `Roles` |
| Utilisateur courant | `CurrentUserProvider.require()` → `CurrentUser` (record pur) | Le contrôleur le passe dans la commande du cas d'utilisation (`CurrentUser` est autorisé dans le domaine) |
| Erreurs RFC 7807 | `GlobalExceptionHandler` (auto) | Lever `BusinessRuleException` (422), `ConflictException` (409), `ResourceNotFoundException` (404), `ForbiddenOperationException` (403), `InvalidRequestException` (400) — exceptions pures, utilisables dans le domaine. Sous-classer dans `domain.exception` si utile. |
| Pagination | `PageableFactory.query(page,size,sort,TRIABLES,"champ,desc")` → `PageQuery` ; `PageableFactory.toPageable(q)` ; `PageResponse.of(...)` / `.map(...)` | Taille bornée à 50, tri en liste blanche |
| Événements | `OutboxWriter.publier(topic, type, aggregateType, aggregateId, payload, resumeAudit)` | **Dans la transaction** (sinon exception). Topics : `Topics.*`, types : `EventTypes.*`, payloads : `sn.cse.events.payload.*`. L'entrée d'audit est générée automatiquement. Appelé depuis un adaptateur `infrastructure.messaging` implémentant un port `application.port.out.*EventPublisher`. |
| Consommation Kafka | `EventEnvelopeReader.lire(String)`, `.payload(env, Xxx.class)` ; `IdempotentEventHandler.traiterUneFois(eventId, "consommateur", () -> ...)` | `@KafkaListener(topics = Topics.ACHATS, groupId = "${spring.application.name}")` sur un `String` ; rejeu + DLT automatiques |
| Appels inter-services | `ServiceRestClientFactory.creer(baseUrl)` → `RestClient` (relaie JWT + corrélation) | Adaptateur `infrastructure.client` + Resilience4j (`@CircuitBreaker`, `@Retry`) avec repli explicite |
| Chiffrement au repos | `@Convert(converter = EncryptedStringConverter.class)` / `EncryptedBigDecimalConverter` | Colonnes `TEXT` ; jamais d'agrégat SQL sur ces colonnes |
| Horloge | bean `Clock` (UTC) | Injecter `Clock`, jamais `LocalDate.now()` sans horloge |
| OpenAPI | `OpenApiAutoConfiguration` (bearer + OAuth2 Keycloak) | Renseigner `cse.openapi.title/description` |
| Tables techniques | migration `classpath:db/common` (`outbox_event`, `processed_event`) | Ajouter la location Flyway (voir §2) |
| Tests | test-jar : `CseJwt.as(userId, roles...)`, `CseJwt.chef(userId, chantiers...)`, `PostgresTestConfiguration` | voir §5 |
| ArchUnit | `HexagonalArchitectureRules` | voir §5 |

## 2. `application.yml` type (remplacer `xxx`, port selon le contrat)

```yaml
server:
  port: ${SERVER_PORT:808N}
  shutdown: graceful
spring:
  application:
    name: ms-xxx
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5433/cse}
    username: ${DB_USERNAME:ms_xxx}
    password: ${DB_PASSWORD:changeit-services}
    hikari:
      # Garantit l'isolement du schéma même si l'utilisateur n'a pas de search_path (tests).
      connection-init-sql: SET search_path TO xxx
      maximum-pool-size: 10
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    schemas: xxx
    default-schema: xxx
    locations: classpath:db/migration,classpath:db/common
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
      auto-offset-reset: earliest
    producer:
      acks: all
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${JWK_SET_URI:http://localhost:8180/realms/cse/protocol/openid-connect/certs}
          issuer-uri: ${JWT_ISSUER_URI:http://localhost:8180/realms/cse}
cse:
  openapi:
    title: ms-xxx — <libellé>
    description: <description FR>
  crypto:
    key: ${CSE_CRYPTO_KEY:}        # seulement si le service chiffre des colonnes
management:
  endpoints.web.exposure.include: health,info,metrics,prometheus
  endpoint.health.probes.enabled: true
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    oauth:
      client-id: cse-swagger
      use-pkce-with-authorization-code-grant: true
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{correlationId:-}]"
---
spring.config.activate.on-profile: dev
cse.seed.enabled: true
```
Ports : gateway 8080, auth 8081, besoins 8082, achats 8083, fournisseurs 8084, suivi 8085, reporting 8086, chatbot 8087, notification 8088, audit 8089.

Mot de passe de dev des utilisateurs `ms_<service>` : valeur de `CSE_DB_PASSWORD` dans `.env` (défaut de l'exemple : `changeit-services`).

## 3. Structure et règles

```
sn.cse.xxx
 ├─ domain/{model,event?,exception,service}     # pur Java : agrégats riches, machines à états, politiques
 ├─ application/{port/in,port/out,command,query,service}   # un cas d'utilisation = une classe @Service @Transactional
 ├─ infrastructure/{persistence,messaging,client,config,seed}
 └─ api/{controller,dto,mapper?}                 # records DTO avec @Schema(description, example) en FR
```
- Les entités JPA (`*Entity`) sont distinctes des objets du domaine ; mapping MapStruct ou manuel.
- Enums métier : types `ENUM` PostgreSQL natifs créés dans la migration du service (`CREATE TYPE ...`),
  mappés avec `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- UUID générés côté application (`UUID.randomUUID()` dans la fabrique de l'agrégat).
- `@Version` sur chaque agrégat modifiable ; colonnes `created_at/created_by/updated_at/updated_by`.
- Références lisibles (`BES-2026-0001`, `AO-2026-001`, `CTR-2026-001`, `OT-2026-0001`) via une **séquence PostgreSQL** par année
  (jamais `System.currentTimeMillis()`).
- Aucune valeur métier en dur : seuils, poids, délais → `@ConfigurationProperties` validées (`cse.<service>.*`).
- Contrôleurs fins : DTO → commande → cas d'utilisation → DTO. Aucune règle métier dans le contrôleur.
- Contrôle d'accès « données » (ex. chef limité à ses chantiers) dans le cas d'utilisation via `CurrentUser`.
- Swagger : `@Tag`, `@Operation(summary, description)`, `@ApiResponse` 200/201/400/401/403/404/409/422,
  `@Schema(example)` sur tous les champs des DTO.
- Javadoc FR sur classes/méthodes publiques ; commentaires du « pourquoi » ; `// Règle R0X` aux endroits clés.
- Pas de `System.out`, `printStackTrace`, `catch` vide, `@SuppressWarnings` de complaisance, TODO.

## 4. Données de démonstration

`infrastructure/seed/DevDataSeeder` : `@Profile("dev")` + `@ConditionalOnProperty("cse.seed.enabled")`,
`ApplicationRunner`, **idempotent** (ne fait rien si les données existent), passe par les agrégats et
l'`OutboxWriter` (transaction) afin que reporting/notification/audit reçoivent de vrais événements.
Identifiants fixes et référentiel partagé : `docs/API_CONTRACT.md` §12. Dates métier réparties sur les 9 derniers
mois (relatives à l'horloge) pour alimenter les séries temporelles. Pas de seed SQL.

## 5. Tests (couverture ≥ 80 % sur `domain` + `application`, imposée par JaCoCo)

- Domaine : tests unitaires purs, un test au moins par règle, nommé `should_..._when_..._R0X`.
- Application : tests unitaires avec ports simulés (Mockito) ou fakes en mémoire.
- API : `@WebMvcTest(Controller.class)` + `@Import` des auto-configurations nécessaires, requêtes avec
  `.with(CseJwt.as(id, Roles.DAF))` ; vérifier 401 sans jeton, 403 mauvais rôle, 400/422 au format ProblemDetail.
  Pour `@WebMvcTest`, ajouter `spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/certs` dans
  `src/test/resources/application.yml`.
- Persistance / bout en bout : `@SpringBootTest` + `@Import(PostgresTestConfiguration.class)` (Testcontainers),
  `cse.outbox.relay-enabled=false`, Kafka non requis (`spring.kafka.listener.auto-startup=false`).
  **Machine de développement limitée en mémoire** : 1 à 2 classes d'intégration Spring par service, pas plus.
- Architecture (la forme `@ArchTest ArchTests.in(...)` n'est PAS exécutée par Surefire : utiliser celle-ci) :
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
- Contrat OpenAPI : test de snapshot comparant `/v3/api-docs` à `src/test/resources/openapi.json`
  (régénérer le fichier volontairement lors d'une évolution du contrat).

Dépendances de test à déclarer dans le POM du service :
`spring-boot-starter-test`, `spring-security-test`, `spring-boot-testcontainers`, `org.testcontainers:postgresql`,
`org.testcontainers:junit-jupiter`, `archunit-junit5`, `cse-common` (type `test-jar`, scope test).

## 6. Build

- Compiler un service sans reconstruire les autres : `cd backend && mvn -o -q -pl ms-xxx spotless:apply verify`
  (`cse-common` / `cse-events` sont installés dans `~/.m2`). Ne jamais lancer `-am` ni `mvn install` à la racine
  en parallèle d'autres builds.
- `MAVEN_OPTS="-Xmx768m"` recommandé.
