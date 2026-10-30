# Guide de lancement et de test - CSE Platform

## Prérequis
- Java 17 (JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64)
- Maven 3.9+
- Node.js 20+, pnpm
- Docker & Docker Compose

## 1. Démarrage de l'infrastructure
```bash
cp .env.example .env
cd infra
docker compose up -d postgres kafka redis keycloak mailhog
```

Attendre que tous les services soient healthy (30-60s).

## 2. Build et lancement du backend
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
cd backend
mvn clean install -DskipTests
```

Lancer les microservices dans l'ordre (ou en parallèle) :
- ms-auth (8081)
- ms-gateway (8080)
- ms-besoins (8082)
- ms-achats (8083)
- ms-fournisseurs (8084)
- ms-suivi (8085)
- ms-reporting (8086)
- ms-chatbot (8087)
- ms-notification (8088)
- ms-audit (8089)

```bash
# Exemple
cd backend/ms-auth && mvn spring-boot:run &
cd backend/ms-gateway && mvn spring-boot:run &
# etc.
```

## 3. Swagger UI
- Gateway agrégé: http://localhost:8080/swagger-ui.html
- Par service:
  - ms-auth: http://localhost:8081/swagger-ui.html
  - ms-besoins: http://localhost:8082/swagger-ui.html
  - ms-achats: http://localhost:8083/swagger-ui.html
  - ms-fournisseurs: http://localhost:8084/swagger-ui.html
  - ms-suivi: http://localhost:8085/swagger-ui.html
  - ms-reporting: http://localhost:8086/swagger-ui.html
  - ms-chatbot: http://localhost:8087/swagger-ui.html
  - ms-notification: http://localhost:8088/swagger-ui.html
  - ms-audit: http://localhost:8089/swagger-ui.html

## 4. Keycloak
- Admin Console: http://localhost:8080/admin
- Realm: cse
- Import realm: infra/keycloak/realm-cse.json

## 5. Lancement du frontend
```bash
cd frontend
pnpm install
pnpm --filter @cse/web-interne dev  # http://localhost:5173
pnpm --filter @cse/portail-fournisseur dev  # http://localhost:5174
```

## 6. Données de test (seeders)
Les seeders dev sont disponibles via profil `dev`. Migrations de seed séparées dans `src/main/resources/db/seed/`.

Utilisateurs de test (mot de passe via env/Keycloak) :
| Rôle | Email | Usage |
|---|---|---|
| CHEF_CHANTIER | chef.chantier@cse.sn | Soumettre besoins |
| RESPONSABLE_MATERIEL | resp.materiel@cse.sn | Valider besoins, parc |
| RESPONSABLE_ACHATS | resp.achats@cse.sn | AO, attribution |
| DAF | daf@cse.sn | Engagements, paiements |
| DIRECTION_GENERALE | dg@cse.sn | Approbations, blacklistage |
| ADMIN_SYSTEME | admin@cse.sn | Administration, audit |
| FOURNISSEUR | fournisseur@cse.sn | Portail fournisseur |

## 7. Tests rapides API
```bash
# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
```

## 8. Makefile
```bash
make up      # Démarrer infra
make down    # Arrêter infra
make build   # Build backend
make test    # Tests backend
make verify  # Vérification complète
make logs    # Logs infra
```

## Lancement automatique (recommandé)

### Démarrer TOUS les microservices en une seule commande
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
./scripts/start-all.sh
```

### Vérifier l'état
```bash
# Voir les logs
tail -f logs/ms-gateway.log
tail -f logs/ms-auth.log

# Vérifier les ports
ss -tlnp | grep -E ':808[0-9]|:517[0-9]'
```

### Arrêter tous les services
```bash
./scripts/stop-all.sh
```

### Démarrer le frontend
```bash
./scripts/start-frontend.sh
```

### URLs après démarrage
- **Gateway/API**: http://localhost:8080
- **Swagger agrégé**: http://localhost:8080/swagger-ui.html
- **Web Interne**: http://localhost:5173
- **Portail Fournisseur**: http://localhost:5174
- **Keycloak (si infra up)**: http://localhost:8080/auth ou http://localhost:8080/admin
- **MailHog**: http://localhost:8025
