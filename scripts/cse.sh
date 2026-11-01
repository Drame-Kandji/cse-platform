#!/usr/bin/env bash
# Pilotage local de la plateforme CSE (développement).
#   scripts/cse.sh infra      démarre PostgreSQL, Kafka, Keycloak, Redis, MailHog (Docker)
#   scripts/cse.sh start      démarre les 10 microservices sur l'hôte (profil dev, jars déjà construits)
#   scripts/cse.sh stop       arrête les microservices
#   scripts/cse.sh status     état des microservices (sonde /actuator/health)
#   scripts/cse.sh front      lance l'application web interne (Vite, port 5173)
#   scripts/cse.sh portail    lance le portail fournisseur (Vite, port 5174)
#   scripts/cse.sh down       arrête microservices + infrastructure
#   scripts/cse.sh reset      DÉVELOPPEMENT : efface les données métier (schémas + topics Kafka) puis redémarre ;
#                             les DevDataSeeder recréent un jeu de démonstration cohérent (comptes Keycloak conservés)
set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOGS="$RACINE/logs"
COMPOSE=(docker compose --env-file "$RACINE/.env" -f "$RACINE/infra/docker-compose.yml")
# Ordre de démarrage : la passerelle en dernier.
SERVICES=(ms-auth:8081 ms-suivi:8085 ms-besoins:8082 ms-fournisseurs:8084 ms-achats:8083
          ms-reporting:8086 ms-audit:8089 ms-notification:8088 ms-chatbot:8087 ms-gateway:8080)
# JVM sobres : la plateforme complète doit tenir sur un poste de 12 Go avec l'infrastructure.
JVM_DEV=(-Xms64m -Xmx256m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=192m)

[[ -f "$RACINE/.env" ]] || { echo "Fichier .env absent : cp .env.example .env puis renseigner CSE_CRYPTO_KEY"; exit 1; }
set -a; source "$RACINE/.env"; set +a

infra() {
  "${COMPOSE[@]}" up -d postgres kafka keycloak redis mailhog
  echo "Attente de Keycloak (import du realm)..."
  for _ in $(seq 1 60); do
    [[ "$(docker inspect -f '{{.State.Health.Status}}' cse-keycloak-1 2>/dev/null)" == healthy ]] && { echo "Infrastructure prête."; return; }
    sleep 5
  done
  echo "Keycloak n'est pas prêt : voir docker logs cse-keycloak-1"; exit 1
}

start() {
  mkdir -p "$LOGS"
  for entree in "${SERVICES[@]}"; do
    local nom="${entree%%:*}" port="${entree##*:}" schema="${entree%%:*}"; schema="${schema#ms-}"
    local jar; jar=$(ls "$RACINE"/backend/"$nom"/target/"$nom"-*.jar 2>/dev/null | grep -v plain | head -1 || true)
    [[ -n "$jar" ]] || { echo "  $nom : jar absent (make build)"; continue; }
    if [[ -f "$LOGS/$nom.pid" ]] && kill -0 "$(cat "$LOGS/$nom.pid")" 2>/dev/null; then echo "  $nom : déjà démarré"; continue; fi
    local var="DB_PASSWORD_${schema^^}"
    SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}" SERVER_PORT="$port" \
      DB_PASSWORD="${!var:-$CSE_DB_PASSWORD}" \
      nohup java "${JVM_DEV[@]}" -jar "$jar" >"$LOGS/$nom.log" 2>&1 &
    echo $! >"$LOGS/$nom.pid"
    echo "  $nom : démarrage (port $port, logs/$nom.log)"
    sleep 2
  done
}

stop() {
  for entree in "${SERVICES[@]}"; do
    local nom="${entree%%:*}"
    if [[ -f "$LOGS/$nom.pid" ]]; then kill "$(cat "$LOGS/$nom.pid")" 2>/dev/null || true; rm -f "$LOGS/$nom.pid"; echo "  $nom : arrêté"; fi
  done
}

status() {
  for entree in "${SERVICES[@]}"; do
    local nom="${entree%%:*}" port="${entree##*:}"
    local etat; etat=$(curl -s -m 2 "http://localhost:$port/actuator/health" | grep -o '"status":"[A-Z]*"' | head -1 || true)
    printf '  %-16s %-6s %s\n' "$nom" "$port" "${etat:-ARRÊTÉ}"
  done
}

reset() {
  [[ "${2:-}" == "--yes" ]] || { read -r -p "Effacer TOUTES les données métier de développement ? (oui/non) " r; [[ "$r" == oui ]] || exit 1; }
  stop
  local s
  for entree in "${SERVICES[@]}"; do
    s="${entree%%:*}"; s="${s#ms-}"; [[ "$s" == gateway ]] && continue
    docker exec -e PGPASSWORD="$POSTGRES_PASSWORD" cse-postgres-1 psql -q -h localhost -U "${POSTGRES_USER:-postgres}" -d cse \
      -c "DROP SCHEMA IF EXISTS $s CASCADE; CREATE SCHEMA $s AUTHORIZATION ms_$s; REVOKE ALL ON SCHEMA $s FROM PUBLIC;"
  done
  docker exec cse-kafka-1 bash -c 'for t in $(kafka-topics.sh --bootstrap-server localhost:29092 --list | grep "^cse\."); do kafka-topics.sh --bootstrap-server localhost:29092 --delete --topic "$t"; done'
  docker exec cse-kafka-1 bash -c 'for g in $(kafka-consumer-groups.sh --bootstrap-server localhost:29092 --list); do kafka-consumer-groups.sh --bootstrap-server localhost:29092 --delete --group "$g" >/dev/null 2>&1; done' || true
  echo "Données effacées. Redémarrage (les services recréent schémas, topics et données de démonstration)..."
  sleep 5
  start
}

front() { cd "$RACINE/frontend" && pnpm --filter @cse/web-interne dev; }
portail() { cd "$RACINE/frontend" && pnpm --filter @cse/portail-fournisseur dev; }

case "${1:-}" in
  infra) infra ;;
  start) start ;;
  stop) stop ;;
  status) status ;;
  front) front ;;
  portail) portail ;;
  down) stop; "${COMPOSE[@]}" stop ;;
  reset) reset "$@" ;;
  *) sed -n '2,12p' "$0"; exit 1 ;;
esac
