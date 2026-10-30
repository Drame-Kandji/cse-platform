
# LANCER LE PROJET CSE COMPLET

## Prérequis
- Docker démarré avec l'infra (postgres, kafka, redis, keycloak, mailhog)
- Java 17 installé
- Maven 3.9+
- Node.js 20+ et pnpm

## Démarrage rapide

### 1. Démarrer l'infrastructure Docker
```bash
cd infra
docker compose up -d postgres kafka redis keycloak mailhog
cd ..
```

### 2. Lancer TOUS les microservices backend
```bash
./start-cse.sh
```

Ce script lance automatiquement dans l'ordre :
1. ms-auth (8081)
2. ms-gateway (8080) 
3. ms-besoins (8082)
4. ms-fournisseurs (8084)
5. ms-suivi (8085)
6. ms-achats (8083)
7. ms-notification (8088)
8. ms-audit (8089)
9. ms-reporting (8086)
10. ms-chatbot (8087)

### 3. Lancer le frontend
Dans un autre terminal :
```bash
./start-frontend-cse.sh
```

## URLs d'accès

| Service | URL | Description |
|---------|-----|-------------|
| API Gateway | http://localhost:8080 | Point d'entrée unique |
| Swagger Agrégé | http://localhost:8080/swagger-ui.html | Documentation API complète |
| Web Interne CSE | http://localhost:5173 | Application pour le personnel CSE |
| Portail Fournisseur | http://localhost:5174 | Portail pour les fournisseurs |
| Keycloak Admin | http://localhost:8080 (port docker) | Gestion des utilisateurs/roles |
| MailHog | http://localhost:8025 | Boîte mail de test |

## Commandes utiles

```bash
# Arrêter tous les microservices
./stop-cse.sh

# Voir les logs en temps réel
tail -f logs/ms-gateway.log
tail -f logs/ms-auth.log

# Vérifier l'état des ports
ss -tlnp | grep -E ':808[0-9]|:517[0-9]'

# Vérifier health du gateway
curl http://localhost:8080/actuator/health
```

## Dépannage

Si un service ne démarre pas, vérifie ses logs :
```bash
tail -50 logs/ms-xxx.log
```
