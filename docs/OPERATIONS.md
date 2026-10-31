# Opérations — plateforme CSE

Ce document décrit l'exploitation : déploiement, frontal HTTPS, sauvegardes et restauration (RTO/RPO),
chiffrement des flux internes, intégration continue, tests de charge et supervision.

## 1. Démarrage

| Mode | Commande | Usage |
|---|---|---|
| Infrastructure seule | `make infra` | développement : PostgreSQL, Kafka, Keycloak, Redis, MailHog en Docker |
| Services sur l'hôte | `make start` / `make status` / `make stop` | développement (JVM sobres, profil dev) |
| Plateforme conteneurisée | `make up` | recette / démonstration : 10 microservices + frontal HTTPS + sauvegardes |
| Images seules | `make images` | construction une image à la fois (`COMPOSE_PARALLEL_LIMIT=1`) |

Variables : `cp .env.example .env`, puis renseigner au minimum `CSE_CRYPTO_KEY` et les mots de passe.

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
(HTTP → 301) — `WEB_HTTPS_PORT` / `WEB_HTTP_PORT`. Les URI `https://localhost:8443/*` sont déclarées dans le
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
  `AAAAMMJJ-HHMMSS/{globals.sql.gz, cse.dump, keycloak.dump, SHA256SUMS}` — base `cse` complète (les neuf
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

### 3.4 Procédure de reprise — objectif RTO < 4 h

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

`make perf` (k6 en conteneur, aucune installation) — scénario, seuils (p95 < 2 s, erreurs < 1 %) et réglage
de la limitation de débit pour les campagnes : `tests/perf/README.md`. Mesure de référence sur poste de
développement : 50 utilisateurs, p95 API = 23 ms, 0 % d'erreur.

## 7. Supervision

- Actuator sur chaque service : `/actuator/health` (sondes `liveness`/`readiness`), `/actuator/metrics`,
  `/actuator/prometheus`.
- État agrégé : `GET /api/plateforme/etat` (authentifié) via la passerelle.
- Journaux : `logs/<service>.log` (mode hôte) ou `docker compose logs -f <service>` ; identifiant de
  corrélation `X-Correlation-Id` propagé de la passerelle aux services et dans les événements.
