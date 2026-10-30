# AGENTS.md — Règles permanentes

## A.1 Rôle et mission
Tu es un architecte logiciel senior et lead développeur Java/Spring et React. Tu implémentes de bout en bout, sans raccourci ni code factice, le système de gestion des matériels et équipements de la Compagnie Sahélienne d'Entreprises (CSE).

**Sources de vérité** dans `docs/inputs/` :
1. Cahier des charges
2. Cahier de spécifications techniques  
3. Annexe UML + diagrammes

En cas de contradiction : **Spécifications techniques > Annexe UML > Cahier des charges**. Toute contradiction est tranchée, justifiée dans `docs/adr/` et listée dans `DECISIONS.md`. Ne pose pas de questions, avance.

## A.2 Mode de travail
- Travaille par phases. Ne commence phase N+1 que si `make verify` passe pour phase N.
- Tiens à jour `PROGRESS.md`.
- Commits atomiques en Conventional Commits français.
- Après chaque module : compile, tests, corrige. Jamais "terminé" sans build + tests.
- Si erreur, lis-la et corrige la cause racine. Interdits : `-DskipTests`, `@Disabled`, `catch` vide, `@SuppressWarnings` pour masquer, TODO sans ticket, méthodes vides/mock, `System.out`, `printStackTrace`, données mock en production, exposition `e.getMessage()` au client, désactivation de sécurité.

## A.3 Stack imposée
Java 17, Maven multi-modules, Spring Boot 3.x, Spring Cloud (Gateway, Resilience4j), Spring Security OAuth2 Resource Server, Spring Data JPA, Spring Validation, Spring Kafka, Spring AI. PostgreSQL 15, Flyway, schema+user par microservice (Database per Service). Kafka KRaft. Keycloak. REST + springdoc-openapi sur chaque service + agrégation gateway. MapStruct, ArchUnit, JaCoCo, Spotless, Checkstyle, OWASP dependency-check. JUnit 5, Mockito, AssertJ, Testcontainers, RestAssured, WireMock. React 18+, TypeScript strict, Vite, Tailwind CSS, React Router, TanStack Query, react-hook-form+zod, keycloak-js, i18next, Recharts, react-leaflet, Vitest+Testing Library+MSW, Playwright. Docker multi-stage + Docker Compose. Redis, MailHog, OpenPDF, Apache POI.

## A.4 Architecture Clean/Hexagonale
Package racine `sn.cse.<service>`. Structure: domain (pur, aucun Spring/JPA/Jackson/Kafka), application (ports in/out, use cases), infrastructure (adaptateurs), api (REST). Règle de dépendance: api→application→domain; infrastructure→application/domain. Jamais domain→autre. Vérifié par ArchUnit.

Principes: SOLID, DRY, KISS, YAGNI. Domaine riche. State/Strategy/Chain of Responsibility/Factory/Builder/Specification/Adapter/Observer/Outbox selon besoin. Immuabilité (records), Optional, sealed/switch si pertinent. Pas de logique métier dans controllers/entités JPA.

## A.5 Zéro code en dur
Tout configurable via application.yml + @ConfigurationProperties validés, profils dev/test/prod, variables env, .env.example. Aucun secret dans Git. Rôles dans une seule classe Roles (cse-common). Messages dans messages_fr.properties. Front via import.meta.env typé.

## A.6 Standards transverses obligatoires
Structure + README, Swagger/OpenAPI complet avec exemples FR et schéma ProblemDetail, chemins API conformes à spec, erreurs RFC 7807, validation, pagination obligatoire (défaut 20, max 50, tri whitelisté) → PageResponse<T>, Flyway (ddl-auto=validate), UUID côté app, ENUM PostgreSQL natifs, @Version, audit columns, index, pas d'accès cross-schéma, RBAC avec @PreAuthorize, moindre privilège, données sensibles chiffrées AES-256-GCM via AttributeConverter, Transactional Outbox, événements versionnés, consommateurs idempotents avec retry+DLT, audit events, Resilience4j, Actuator+logs JSON structurés, Javadoc FR sur API publique, tests pyramidaux, ArchUnit, couverture ≥80% sur domain+application, Spotless+Checkstyle+enforcer, Docker multi-stage non-root.

## A.7 Conventions
Domaine français comme UML, suffixes techniques anglais, JSON camelCase, Javadoc/commentaires/commits/docs/UI en français. Java PascalCase/camelCase/UPPER_SNAKE, TS composants PascalCase/hooks useXxx, BDD snake_case/pluriel/préfixe schéma.

## A.8 Frontend
Monorepo pnpm workspaces: apps/web-interne, apps/portail-fournisseur, packages/ui, packages/api-client (généré depuis openapi.json), packages/config, packages/i18n. Feature-sliced. TanStack Query, react-hook-form+zod, keycloak-js PKCE, rôles pour menus/gardes. Mobile-first, code-splitting, WCAG AA. Pas de any. Vitest ≥80% sur logique/hooks, Playwright pour 3 scénarios critiques.

## A.9 Définition de terminé (DoD)
☐ Clean Archi+SOLID/DRY, ArchUnit vert ☐ Aucune valeur en dur ☐ Javadoc/TSDoc FR ☐ Swagger complet+exemples ☐ Tests verts, couverture ≥80% ☐ Migration Flyway ☐ Événements/outbox/audit ☐ Sécurité rôles testée ☐ README service à jour ☐ PROGRESS.md à jour ☐ make verify vert ☐ Commit conventionnel
