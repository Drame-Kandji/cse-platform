# ms-gateway — Passerelle d'API (port 8080)

Point d'entrée unique de la plateforme : Spring Cloud Gateway **WebFlux** (`spring-cloud-starter-gateway-server-webflux`,
Spring Cloud 2025.0). Spring MVC est interdit sur le classpath (règle `maven-enforcer` + test ArchUnit).
Pas de domaine ni de couche application : la porte de couverture JaCoCo est désactivée (`jacoco.check.skip`).

## Routage (`cse.gateway.services`, URL par `MS_<NOM>_URL`, défaut `http://localhost:808N`)
| Service | Chemins |
|---|---|
| ms-auth | `/api/me`, `/api/admin/**` |
| ms-besoins | `/api/besoins/**` |
| ms-achats | `/api/appels-offres/**`, `/api/contrats/**`, `/api/engagements/**`, `/api/factures/**`, `/api/finance/**`, `/api/besoins-a-acheter/**`, `/api/portail/{appels-offres,offres,attributions,factures}/**` |
| ms-fournisseurs | `/api/fournisseurs/**`, `/api/litiges/**`, `/api/portail/{profil,catalogue,documents,inscription,litiges}/**` |
| ms-suivi | `/api/materiels/**`, `/api/maintenances/**`, `/api/chantiers/**`, `/api/affectations/**`, `/api/pannes/**`, `/api/referentiels/**` |
| ms-reporting | `/api/reporting/**` |
| ms-chatbot | `/api/chatbot/**` |
| ms-notification | `/api/notifications/**` ; route dédiée `/api/notifications/stream` (SSE : sans coupe-circuit, délai `GATEWAY_SSE_TIMEOUT`=1 h, `X-Accel-Buffering: no`) |
| ms-audit | `/api/audit/**` |

Les chemins ne sont pas réécrits (les services exposent eux-mêmes `/api/...`).

## Sécurité
- `/api/**` : JWT Keycloak obligatoire (`JWK_SET_URI` + `JWT_ISSUER_URI`), rôles `realm_access.roles` via
  `KeycloakJwtConverter` (cse-common) adapté en réactif. Les services revérifient les rôles.
- `/api/portail/**` : rôle `FOURNISSEUR` (exceptions configurables : `cse.gateway.securite.portail-sans-role-fournisseur`).
- `/api/plateforme/**` : tout rôle interne.
- Publics : `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`, Swagger UI, `/v3/api-docs/**`.
- 401/403/404/429/503/504 au format ProblemDetail `{type,title,status,detail,code,traceId,timestamp}` ;
  codes `NON_AUTHENTIFIE`, `ACCES_REFUSE`, `RESSOURCE_INTROUVABLE`, `TROP_DE_REQUETES`, `SERVICE_INDISPONIBLE`, `DELAI_DEPASSE`.
- En-têtes : `X-Frame-Options: DENY`, `X-Content-Type-Options`, `Referrer-Policy: no-referrer`, `Permissions-Policy`, HSTS (HTTPS).
- CORS : origines exactes `GATEWAY_CORS_ORIGINS` (défaut `http://localhost:5173,http://localhost:5174`), identifiants
  autorisés, joker refusé au démarrage ; `X-Correlation-Id`, `Location`, `Content-Disposition` exposés.

## Filtres transverses
- **Corrélation** : `X-Correlation-Id` repris s'il est valide (`[A-Za-z0-9._-]{1,64}`), sinon généré ; ajouté à la requête
  relayée et à la réponse ; sert de `traceId`.
- **Limitation de débit** : `RequestRateLimiter` Redis, clé = `sub` du JWT (à défaut IP client) ;
  `GATEWAY_RATE_LIMIT_REPLENISH` (20/s), `GATEWAY_RATE_LIMIT_BURST` (40), désactivable (`GATEWAY_RATE_LIMIT_ENABLED=false`).
- **Coupe-circuit** Resilience4j par route (délai `GATEWAY_CIRCUIT_BREAKER_TIMEOUT`=20 s) avec repli 503 `SERVICE_INDISPONIBLE`.

## Documentation et supervision
- Swagger UI agrégé : `/swagger-ui.html` (OAuth2 PKCE, client `cse-swagger`) ; `/v3/api-docs/<service>` est relayé vers
  `/v3/api-docs` du service et ses `servers` sont remplacés par la passerelle. Documentation propre : `/v3/api-docs/ms-gateway`.
- `GET /api/plateforme/etat` (rôles internes) → `{ services: [{ nom, statut: 'UP'|'DOWN', latenceMs }] }`, sonde concurrente
  de `/actuator/health` de chaque service (délai `GATEWAY_HEALTH_PROBE_TIMEOUT`=2 s).

## Build et tests
```bash
cd backend && flock /tmp/cse-maven.lock env MAVEN_OPTS=-Xmx768m mvn -o -q -pl ms-gateway spotless:apply verify
```
`GatewayIntegrationTest` (`@SpringBootTest` + WireMock + jetons RS256 de test) : table de routage du contrat, 401 sans jeton,
403 portail sans FOURNISSEUR, relais du jeton et de la corrélation, repli du coupe-circuit, 404, CORS, sonde, Swagger agrégé.
Tests unitaires : clé de limitation, ProblemDetail 429, gestionnaire d'erreurs, corrélation. Redis n'est pas requis
(limitation désactivée dans le profil `test`).

## Docker
Dans `infra/docker-compose.yml`, renseigner `MS_<NOM>_URL=http://ms-<nom>:808N` pour la passerelle (les valeurs par
défaut visent `localhost`).
