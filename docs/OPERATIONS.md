# Opérations : plateforme CSE

Ce document décrit l'exploitation : déploiement, frontal HTTPS, sauvegardes et restauration (RTO/RPO),
chiffrement des flux internes, intégration continue, tests de charge et supervision.

## 1. Démarrage

| Mode | Commande | Usage |
|---|---|---|
| Infrastructure seule | `make infra` | développement : PostgreSQL, Kafka, Keycloak, Redis, MailHog en Docker |
| Services sur l'hôte | `make build` puis `make start` / `make status` / `make stop` | développement (JVM sobres, profil dev, journaux dans `logs/`) |
| Front de développement | `make front` | Vite sur http://localhost:5173, `/api` relayé vers la passerelle :8080 |
| Plateforme conteneurisée | `make up` | recette / démonstration : 10 microservices + frontal HTTPS + sauvegardes |
| Images seules | `make images` | construction une image à la fois (`COMPOSE_PARALLEL_LIMIT=1`) |
| Tout arrêter | `make down` | services sur l'hôte + conteneurs (volumes conservés) |
| Réinitialiser la démonstration | `scripts/cse.sh reset` | §10 |

Variables : `cp .env.example .env`, puis renseigner au minimum `CSE_CRYPTO_KEY` et les mots de passe.

**Profils Docker Compose** (`infra/docker-compose.yml`, projet `cse`) :

| Profil | Services | Commande |
|---|---|---|
| *(aucun)* | `postgres` (:5433), `kafka` (:9092), `keycloak` (:8180), `redis` (:6379), `mailhog` (:1025 SMTP, :8025 web) | `make infra` (`scripts/cse.sh infra`, attend que Keycloak soit sain) |
| `apps` | infrastructure + `ms-gateway` (:8080) et les 9 autres services (réseau interne seulement) + `web-interne` (:8443 HTTPS, :8000 → 301) + `backup` | `make up` |
| `backup` | `backup` seul | `docker compose --env-file .env -f infra/docker-compose.yml --profile backup up -d backup` |

Les services conteneurisés utilisent l'image générique `backend/Dockerfile` (multi-étapes Maven → JRE 17
Alpine, utilisateur non-root `cse`, `HEALTHCHECK` sur `/actuator/health`), `SPRING_PROFILES_ACTIVE`
(défaut `dev` : données de démonstration ; mettre `prod` hors démonstration) et
`JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70`.

## 2. Frontal HTTPS (`web-interne`)

Image `frontend/apps/web-interne/Dockerfile` (multi-étapes) : `node:20` + pnpm (`--frozen-lockfile`, filtré
sur `@cse/web-interne`) → `vite build` (variables `VITE_*` passées en `--build-arg`, figées dans le bundle :
une image par environnement) → `nginxinc/nginx-unprivileged` (UID 101, aucun privilège).

Configuration `infra/nginx/web-interne.conf` (modèle `envsubst`) :

- **HTTPS** TLS 1.2/1.3 uniquement, HTTP/2, suites ECDHE-AEAD ; le port HTTP ne fait que rediriger en 301.
- **En-têtes** : HSTS (1 an), CSP, `X-Frame-Options: SAMEORIGIN` (l'iframe `silent-check-sso.html` de
  keycloak-js est de même origine), `nosniff`, `Referrer-Policy`, `Permissions-Policy`, COOP.
- **CSP** : `script-src 'self'` + empreintes SHA-256 des deux scripts en ligne (thème initial, silent SSO),
  recalculées à chaque construction par `infra/nginx/csp-hashes.mjs` ; `connect-src`/`frame-src`/`form-action`
  autorisent l'URL Keycloak (`KEYCLOAK_PUBLIC_URL`) ; `img-src` autorise `tile.openstreetmap.org`, `data:` et
  `blob:` (exports) ; `style-src 'unsafe-inline'` est nécessaire car Radix UI et Sonner injectent des balises
  `<style>` à l'exécution.
- **Cache** : `/assets/*` (noms fingerprintés par Vite) `max-age=1 an, immutable` ; `index.html` et le reste
  `no-cache` ; l'API garde les en-têtes de la passerelle (`no-store`).
- **Compression** : gzip dynamique + fichiers `.gz` précompressés à la construction (`gzip_static`).
  Brotli n'est pas activé : le module `ngx_brotli` n'est pas fourni par les images Nginx officielles ; il
  faudrait une image Nginx compilée sur mesure, gain marginal par rapport au `.gz` statique.
- **SPA** : toute route inconnue renvoie `index.html` ; un fichier `/assets/` absent renvoie une vraie 404.
- **API** : `/api/` relayé vers `ms-gateway:8080` (keep-alive) ; `/api/notifications/stream` (SSE) sans
  tampon (`proxy_buffering off`), délai de lecture `NGINX_SSE_TIMEOUT` (1 h ; ping serveur toutes les 25 s).
- Cartes de sources (`*.map`) retirées de l'image.

Ports : le conteneur écoute 8443/8080 (non privilégiés), publiés sur l'hôte en **8443** (HTTPS) et **8000**
(HTTP → 301) : `WEB_HTTPS_PORT` / `WEB_HTTP_PORT`. Les URI `https://localhost:8443/*` sont déclarées dans le
client Keycloak `cse-web-interne` (`infra/keycloak/realm-cse.json`). Le realm n'étant importé qu'à la première
création, sur un Keycloak existant il faut ajouter l'URI de redirection et l'origine dans la console
d'administration (Clients → cse-web-interne).

Certificats : `make certs` (`scripts/gen-certs.sh`) crée dans `infra/certs/` (ignoré par Git) une CA de
développement, le certificat du frontal (`localhost`, `web-interne`, `127.0.0.1`), un certificat commun aux
services (SAN `ms-*`) et un truststore PKCS12. Importer `infra/certs/ca.crt` dans le navigateur pour éviter
l'alerte. En production : certificats de la PKI de l'entreprise (ou ACME), montés au même chemin
(`/etc/nginx/certs/localhost.{crt,key}`) via un secret, clé lisible du seul UID 101.

## 3. Sauvegardes et restauration (fiabilité)

### 3.1 Sauvegarde quotidienne

Service Compose `backup` (image `postgres:15-alpine`, script `infra/backup/backup.sh`), actif avec le
profil `apps` ou seul : `docker compose --env-file .env -f infra/docker-compose.yml --profile backup up -d backup`.

- Chaque jour à `BACKUP_HEURE` (UTC, défaut 02:00), dans le volume `backups` :
  `AAAAMMJJ-HHMMSS/{globals.sql.gz, cse.dump, keycloak.dump, SHA256SUMS}`, base `cse` complète (les neuf
  schémas), base `keycloak`, rôles globaux (`ms_*`, `keycloak`), au format custom compressé (`pg_dump -Fc`,
  restauration sélective possible par schéma ou table).
- Écriture dans un répertoire temporaire puis renommage atomique : un répertoire daté est toujours complet.
- Rétention `BACKUP_RETENTION_JOURS` (défaut **90 jours**) appliquée après chaque sauvegarde.
- Journal : sortie du conteneur (`docker logs cse-backup-1`) et `backup.log` dans le volume.
- Sauvegarde immédiate : `make backup` (volume) ou `make backup DEST=/chemin` (hôte, pour copie hors site).
- Liste : `scripts/backup-now.sh --liste`.

**Production** : le volume `backups` doit résider sur un stockage distinct du serveur PostgreSQL et être
répliqué hors site (copie quotidienne chiffrée, ex. `restic`/`rclone`). Pour un RPO inférieur à 24 h,
compléter par l'archivage continu des WAL (`archive_mode=on` + `pg_basebackup`, ou pgBackRest) qui permet la
restauration à un instant donné ; ce n'est pas mis en place sur l'environnement Compose.

### 3.2 Restauration

`scripts/restore.sh <sauvegarde> [--base cse|keycloak] [--hote h] [--port p] [--globals] [--sans-proprietaires] [--yes]`
(ou `make restore DUMP=... BASE=... ARGS=...`).

- Source : fichier `.dump` de l'hôte, répertoire de sauvegarde de l'hôte, ou nom `AAAAMMJJ-HHMMSS` du volume.
- Vérifie l'empreinte SHA-256 et la lisibilité de l'archive, demande de **taper le nom de la base** pour
  confirmer (contournable par `--yes`), crée la base si absente, restaure en **une seule transaction**
  (`--clean --if-exists --single-transaction` : en cas d'échec la base cible est inchangée), puis `ANALYZE`.
- `--globals` rejoue d'abord les rôles (serveur neuf) en excluant le superutilisateur de connexion (sinon son
  mot de passe serait remplacé par celui du serveur source).
- Aucun client PostgreSQL requis sur l'hôte (exécution dans un conteneur `postgres:15-alpine`).

### 3.3 Test de restauration (réalisé le 2026-10-31)

Sauvegarde de la base vivante par `scripts/backup-now.sh --dest` (lecture seule), restauration avec
`--globals` dans un PostgreSQL 15 jetable (port 15432), puis comparaison du nombre de lignes de **toutes les
tables** : 74 tables identiques (ex. `suivi.materiels` 42, `besoins.besoins_materiels` 40,
`achats.contrat_achat` 8, `achats.appel_offre` 12, `fournisseurs.fournisseurs` 7, `audit.audit_log` 941,
Keycloak `user_entity` 10) ; schémas restaurés avec leurs propriétaires `ms_*`. Durées : sauvegarde 2 s,
restauration `cse` 4 s, `keycloak` 3 s (volume de recette ≈ 0,8 Mo). Ce test est à rejouer **chaque
trimestre** et après toute évolution majeure du schéma.

### 3.4 Procédure de reprise : objectif RTO < 4 h

| Étape | Action | Durée cible |
|---|---|---|
| 1. Décision | Constater l'incident, déclarer la reprise, choisir la sauvegarde (la plus récente valide) | 30 min |
| 2. Serveur | Nouveau serveur ou nouveau volume : `make infra` sans données (`postgres` seul d'abord : `docker compose ... up -d postgres`) | 30 min |
| 3. Rôles | `scripts/restore.sh <sauvegarde> --base cse --globals --yes` (rôles + base `cse`) | 15 min |
| 4. Keycloak | `scripts/restore.sh <sauvegarde> --base keycloak --yes`, puis démarrage de Keycloak | 15 min |
| 5. Services | `make start` (ou `make up`) : Flyway valide les schémas (`ddl-auto=validate`), les outbox non publiées sont relayées | 30 min |
| 6. Contrôles | `make status`, `GET /api/plateforme/etat`, connexion d'un compte par rôle, comptages de contrôle | 30 min |
| Marge | Transfert de la sauvegarde hors site, imprévus | 1 h 30 |

Total ≤ 4 h. **RPO** : 24 h avec la seule sauvegarde quotidienne (quelques minutes avec l'archivage WAL).
Les événements Kafka postérieurs à la sauvegarde ne sont pas rejoués : les consommateurs étant idempotents
(`processed_event`), un rejeu des topics depuis l'instant de la sauvegarde est sans danger si nécessaire.

## 4. Chiffrement des flux internes (TLS entre services)

La spécification exige TLS en production, y compris entre services. **État actuel : seul le frontal est en
HTTPS.** Derrière lui, les flux restent en clair sur le réseau Docker privé `interne` (passerelle → services,
services → PostgreSQL/Kafka/Redis/Keycloak). Ce n'est pas activé ni validé sur l'environnement Compose : la
mise en œuvre touche chaque service et n'a pas pu être testée de bout en bout sur le poste de développement.
Le profil Compose `tls` n'existe donc pas encore. Procédure prévue pour la production :

1. **PKI** : `make certs` produit déjà `services.crt/key` (SAN = noms DNS `ms-*`) et `truststore.p12` (CA) ;
   en production, un certificat par service émis par la PKI interne.
2. **Services HTTP (Spring Boot 3, sans code)** : variables d'environnement
   `SERVER_SSL_BUNDLE=cse`, `SPRING_SSL_BUNDLE_PEM_CSE_KEYSTORE_CERTIFICATE=file:/certs/services.crt`,
   `SPRING_SSL_BUNDLE_PEM_CSE_KEYSTORE_PRIVATE_KEY=file:/certs/services.key`,
   `SPRING_SSL_BUNDLE_PEM_CSE_TRUSTSTORE_CERTIFICATE=file:/certs/ca.crt` ; les URLs `MS_*_URL` passent en
   `https://ms-xxx:808N`.
3. **Clients** : `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=/certs/truststore.p12 -Djavax.net.ssl.trustStorePassword=...`
   pour les `RestClient` inter-services et le client HTTP de la passerelle (Reactor Netty utilise le
   gestionnaire de confiance par défaut de la JVM).
4. **Actuator** : `MANAGEMENT_SERVER_PORT` dédié resté en HTTP sur le réseau interne (sondes Docker
   `HEALTHCHECK` et sonde de la passerelle), ou sondes adaptées en `https` avec la CA.
5. **PostgreSQL** : `ssl=on` (certificat serveur), JDBC `?sslmode=verify-full&sslrootcert=/certs/ca.crt`.
6. **Kafka** : écouteur `SSL` (ou `SASL_SSL`), `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP`, côté Spring
   `SPRING_KAFKA_SECURITY_PROTOCOL=SSL` + truststore.
7. **Keycloak / Redis** : Keycloak en HTTPS (`KC_HTTPS_CERTIFICATE_FILE`), JWK en `https://` ; Redis
   `tls-port` + `SPRING_DATA_REDIS_SSL_ENABLED=true`.
8. **Validation** : vérifier chaque lien (`openssl s_client -connect ms-xxx:808N -CAfile ca.crt`), tests de
   bout en bout, puis fermer les ports en clair. Alternative : maillage de services (mTLS transparent,
   ex. Linkerd/Istio) si la plateforme migre vers Kubernetes.

## 5. Intégration continue

- `.github/workflows/ci.yml` : job **backend** (JDK 17, cache Maven, `mvn -B verify` : Spotless, Checkstyle,
  tests dont Testcontainers, ArchUnit, JaCoCo), job **frontend** (Node 20, pnpm, `typecheck`, `lint`, `test`,
  `build` de `@cse/web-interne`), job **images** sur `main` uniquement (ms-gateway représentatif + web-interne,
  publiées sur GHCR, cache BuildKit GHA).
- `.github/workflows/dependency-check.yml` : OWASP dependency-check (échec si CVSS ≥ 7) et `pnpm audit`,
  chaque lundi et à la demande ; secret `NVD_API_KEY` recommandé.

## 6. Tests de charge

`make perf` (k6 en conteneur, aucune installation), scénario, seuils (p95 < 2 s, erreurs < 1 %) et réglage
de la limitation de débit pour les campagnes : `tests/perf/README.md`. Mesure de référence sur poste de
développement : 50 utilisateurs, p95 API = 23 ms, 0 % d'erreur.

## 7. Supervision

| Point de contrôle | Accès | Contenu |
|---|---|---|
| `GET /actuator/health` (chaque service, port 808N) | public | état agrégé (base, Kafka, Redis pour la passerelle, SMTP pour ms-notification si `MAIL_HEALTH_ENABLED`) |
| `/actuator/health/liveness`, `/actuator/health/readiness` | public | sondes pour orchestrateur (`management.endpoint.health.probes.enabled=true`) |
| `/actuator/metrics`, `/actuator/prometheus` | `prometheus` public, `metrics` authentifié | métriques Micrometer (JVM, HTTP, Hikari, Kafka) à collecter par Prometheus |
| `/actuator/info` | public | informations de build |
| `GET /api/plateforme/etat` (passerelle) | rôle interne | `{services: [{nom, statut UP/DOWN, latenceMs}]}` sonde concurrente des 9 services (écran **Administration → État de la plateforme**) |
| `make status` | hôte | statut `/actuator/health` des 10 services |
| Keycloak `:9000/health/ready` | conteneur | utilisé par le `healthcheck` Compose |
| Outbox | SQL | `SELECT count(*) FROM <schéma>.outbox_event WHERE published_at IS NULL` doit rester proche de 0 |
| DLT | Kafka | topics `cse.*.events.DLT` vides en régime normal ([events.md](events.md) §5) |

Aucun Prometheus/Grafana n'est déployé par le compose : à brancher en production (et restreindre l'accès à
`/actuator/prometheus` au réseau de supervision).

**Journaux**

- Mode hôte : `logs/<service>.log` (un fichier par service, `nohup`), PID dans `logs/<service>.pid`.
- Conteneurs : `docker compose --env-file .env -f infra/docker-compose.yml logs -f ms-achats` ; infrastructure :
  `make logs`.
- Format texte Spring Boot avec le motif `NIVEAU [service,correlationId]` : rechercher un incident par son
  `traceId` (renvoyé dans chaque ProblemDetail = identifiant de corrélation) :
  `grep 'a1b2c3d4e5' logs/*.log`. Le même identifiant figure dans les événements (`correlationId`) et dans
  le journal d'audit. Les journaux ne sont **pas** au format JSON (écart à la spécification : activer
  `logging.structured.format.console=ecs` de Spring Boot 3.4+ pour une collecte centralisée).

## 8. Variables d'environnement par service

Communes à tous les services Java (défauts orientés développement sur l'hôte) :

| Variable | Défaut | Rôle |
|---|---|---|
| `SERVER_PORT` | 808N | port HTTP |
| `SPRING_PROFILES_ACTIVE` | aucun (`dev` via `scripts/cse.sh` et compose) | `dev` active les `DevDataSeeder` |
| `DB_URL` | `jdbc:postgresql://localhost:5433/cse` | base (schéma fixé par `search_path`) |
| `DB_USERNAME` / `DB_PASSWORD` | `ms_<service>` / `changeit-services` | utilisateur du service (compose : `DB_PASSWORD_<SERVICE>` sinon `CSE_DB_PASSWORD`) |
| `DB_POOL_SIZE` | 10 | pool Hikari (selon service) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | compose : `kafka:29092` |
| `JWK_SET_URI` | `http://localhost:8180/realms/cse/protocol/openid-connect/certs` | clés de signature (compose : `http://keycloak:8080/...`) |
| `JWT_ISSUER_URI` | `http://localhost:8180/realms/cse` | doit valoir l'URL **publique** de Keycloak (`KEYCLOAK_PUBLIC_URL`) |
| `CSE_CRYPTO_KEY` | vide | obligatoire pour ms-achats et ms-fournisseurs ([SECURITY.md](SECURITY.md) §6) |
| `CSE_CLIENT_CONNECT_TIMEOUT` / `CSE_CLIENT_READ_TIMEOUT` | 2 s / 3 s | appels inter-services |

Spécifiques (liste complète et défauts dans chaque `backend/ms-*/README.md` et `application.yml`) :

| Service | Variables principales |
|---|---|
| ms-gateway | `MS_<NOM>_URL` (9 services), `REDIS_HOST`/`REDIS_PORT`, `GATEWAY_RATE_LIMIT_ENABLED`/`_REPLENISH`/`_BURST`, `GATEWAY_CORS_ORIGINS`, `GATEWAY_CIRCUIT_BREAKER_ENABLED`/`_TIMEOUT`, `GATEWAY_SSE_TIMEOUT`, `GATEWAY_HEALTH_PROBE_TIMEOUT`, `GATEWAY_RESPONSE_TIMEOUT`, `GATEWAY_CONNECT_TIMEOUT_MS` |
| ms-auth | `KEYCLOAK_ADMIN_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_ADMIN_CLIENT_SECRET`, `AUTH_OTP_OBLIGATOIRE`, `AUTH_SYNC_AU_DEMARRAGE`/`_RELANCE`/`_PERIODIQUE`/`_INTERVALLE` |
| ms-besoins | `SUIVI_BASE_URL`, `CSE_BESOINS_FENETRE_DOUBLON_JOURS` |
| ms-achats | `MS_FOURNISSEURS_URL`, `CSE_ACHATS_SEUIL_DELEGATION`, `CSE_ACHATS_PENALITE_PCT_JOUR`, `CSE_ACHATS_PLAFOND_PENALITES_PCT`, `CSE_ACHATS_TAUX_TARIF_JOURNALIER`, `CSE_ACHATS_RAPPEL_RETARD_JOURS`, `CSE_ACHATS_CLOTURE_CRON`, `CSE_ACHATS_RETARD_CRON` |
| ms-fournisseurs | `CSE_SCORE_POIDS_*`, `CSE_SCORE_PENALITE_LITIGE_*`, `CSE_SCORE_INITIAL`, `CSE_SCORE_SEUIL_ALERTE`, `CSE_FOURNISSEURS_SURVEILLANCE_ENABLED`/`_CRON`/`_ZONE` |
| ms-suivi | `SUIVI_ALERTES_ACTIVES`, `SUIVI_ALERTE_MAINTENANCE_CRON`, `SUIVI_ALERTE_FIN_AFFECTATION_CRON`, `SUIVI_FIN_AFFECTATION_JOURS_AVANT`, `SUIVI_RAPPEL_MAINTENANCE_JOURS`, `SUIVI_LIVRAISON_DUREE_AFFECTATION_JOURS` |
| ms-reporting | `REPORTING_PERIODE_DEFAUT_JOURS`, `REPORTING_PERIODE_MAX_JOURS`, `REPORTING_MAX_POINTS_SERIE`, `REPORTING_TOP_FOURNISSEURS`, `REPORTING_DUREE_CHANTIER_DEFAUT_JOURS`, `REPORTING_INSTANTANE_CRON` |
| ms-chatbot | `MS_SUIVI_URL`, `MS_BESOINS_URL`, `MS_ACHATS_URL`, `MS_FOURNISSEURS_URL`, `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`, `LLM_COMPLETIONS_PATH`, `LLM_TIMEOUT`, `CHATBOT_RATE_CAPACITE`, `CHATBOT_RATE_PERIODE` |
| ms-notification | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM`, `MAIL_ENABLED`, `MAIL_HEALTH_ENABLED`, `MAIL_TIMEOUT_MS`, `FRONTEND_BASE_URL` (liens des e-mails), `SSE_PING_INTERVAL`, `SSE_TIMEOUT`, `SSE_MAX_CONNEXIONS` |
| ms-audit | `AUDIT_FENETRE_STATISTIQUES_JOURS`, `AUDIT_LONGUEUR_MAX_RECHERCHE` |
| web-interne (build) | `VITE_API_BASE_URL`, `VITE_KEYCLOAK_URL`, `VITE_KEYCLOAK_REALM`, `VITE_KEYCLOAK_CLIENT_ID` (figées dans le bundle) |
| Frontal Nginx | `PUBLIC_HTTPS_PORT`, `GATEWAY_UPSTREAM`, `KEYCLOAK_PUBLIC_URL`, `NGINX_SSE_TIMEOUT` |

## 9. Dépannage

| Symptôme | Cause probable | Correction |
|---|---|---|
| Un service démarré par `make start` échoue avec `NoSuchMethodError`, `ClassNotFoundException` ou un comportement ancien après un passage dans l'IDE | l'IDE (VS Code Java, IntelliJ) recompile dans `target/classes` avec son propre compilateur et désynchronise les classes du jar ou des tests | `cd backend && mvn -o -q -pl ms-xxx clean install` (ou `make clean build`), puis `make stop start` |
| Une modification de `infra/keycloak/realm-cse.json` n'est pas prise en compte | le realm n'est importé **qu'à la première création** de la base Keycloak (`--import-realm` ignore un realm existant) | modifier dans la console (http://localhost:8180, compte `KEYCLOAK_ADMIN`), ou repartir de zéro en supprimant la base Keycloak : `docker compose ... down`, `docker volume rm cse_postgres_data_v2` (**efface aussi les données métier**), `make infra` |
| 401 sur toutes les requêtes alors que la connexion fonctionne | `JWT_ISSUER_URI` ≠ émetteur du jeton (ex. `http://keycloak:8080` vs `http://localhost:8180`) | aligner `JWT_ISSUER_URI` sur `KEYCLOAK_PUBLIC_URL` |
| ms-achats ou ms-fournisseurs refuse de démarrer : `cse.crypto.key est obligatoire` | `CSE_CRYPTO_KEY` vide ou mal encodée | `head -c32 /dev/urandom \| base64` dans `.env` ; **ne jamais changer la clé** sur des données existantes |
| Besoins validés restant `VALIDE` avec « orientation en attente » | ms-suivi indisponible pendant la validation (circuit ouvert) | redémarrer ms-suivi puis « Relancer l'orientation » (`POST /api/besoins/{id}/orienter`) |
| Tableaux de bord vides ou figés | ms-reporting arrêté ou événements en DLT | `make status` ; outbox non publiée ; topics `.DLT` (§7) |
| 429 `TROP_DE_REQUETES` | limiteur de la passerelle (20 req/s par utilisateur), fréquent en test de charge avec un seul compte | voir `tests/perf/README.md` ; ne pas désactiver en production |
| `permission denied for schema` | requête d'un service vers le schéma d'un autre | comportement attendu (ADR-0004) |
| Front : boucle de connexion ou « Invalid redirect uri » | origine non déclarée dans le client `cse-web-interne` | ajouter l'URI dans la console Keycloak (Clients → `cse-web-interne`) |
| Kafka refuse de démarrer après un arrêt brutal | métadonnées KRaft incohérentes | `docker compose ... rm -sf kafka && docker volume rm cse_kafka_data` puis `make infra` (les outbox republieront les événements non publiés ; les consommateurs sont idempotents) |
| Ports occupés (5433, 8080, 8180…) | autre instance | `make down`, `ss -ltnp \| grep 8080` |

**Mémoire (poste de 12 Go)** : la plateforme complète (infrastructure + 10 JVM + Vite + navigateur) tient
dans environ 8 à 9 Go. `scripts/cse.sh` lance chaque JVM avec `-Xms64m -Xmx256m -Xss512k -XX:+UseSerialGC
-XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=192m` ; Kafka est limité à `-Xmx384m`. Conseils : ne pas lancer
`make up` et `make start` en même temps ; construire les images une par une (`make images`) ; pour les
builds Maven, `MAVEN_OPTS=-Xmx768m` et un seul build à la fois (`flock /tmp/cse-maven.lock …`) ; les tests
Testcontainers démarrent un PostgreSQL éphémère par classe d'intégration (1 à 2 par service). Si la
mémoire manque, arrêter les services non utiles (`kill $(cat logs/ms-chatbot.pid)`).

## 10. Réinitialiser les données de démonstration

```bash
scripts/cse.sh reset          # demande confirmation (taper « oui »)
scripts/cse.sh reset --yes    # sans confirmation
```

Développement uniquement. Le script arrête les services, **supprime et recrée les 9 schémas** métier
(`DROP SCHEMA … CASCADE; CREATE SCHEMA … AUTHORIZATION ms_<service>`), supprime les topics `cse.*` et les
groupes de consommateurs Kafka, puis relance les services : Flyway recrée les tables, les `DevDataSeeder`
(profil `dev`) régénèrent le jeu de départ (6 chantiers, 15 matériels dont 6 affectés, 5 fournisseurs actifs, sans besoin,
appel d'offres, contrat ni facture) **via le domaine et l'outbox**, de sorte que reporting, notifications et
audit sont réalimentés par de vrais événements. Les comptes Keycloak (base `keycloak`) sont conservés.

## 11. Mise en production : liste de contrôle

- [ ] `SPRING_PROFILES_ACTIVE=prod` (aucune donnée de démonstration).
- [ ] Mots de passe et secrets régénérés (`.env` ou gestionnaire de secrets), `CSE_CRYPTO_KEY` sauvegardée à part.
- [ ] Realm de production : suppression du client `cse-tests-e2e` et des URI `localhost`, secret
      `cse-admin-api` régénéré, OTP imposé aux rôles sensibles, comptes de démonstration supprimés.
- [ ] Keycloak en mode `start` (pas `start-dev`) derrière HTTPS.
- [ ] Certificats de la PKI d'entreprise montés dans le frontal ; TLS interne (§4).
- [ ] Volume `backups` sur stockage distinct + copie hors site ; test de restauration trimestriel (§3).
- [ ] `/actuator/prometheus` accessible au seul réseau de supervision ; collecte des journaux.
- [ ] Une seule instance des services à jobs planifiés (pas de verrou distribué, voir `docs/QUALITY_REPORT.md` §6).
