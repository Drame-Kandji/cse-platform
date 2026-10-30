#!/usr/bin/env bash
# Script pour lancer tous les microservices CSE en une fois
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$BASE_DIR/logs"
mkdir -p "$LOG_DIR"

echo "=== Démarrage de tous les microservices CSE ==="
echo "Logs dans: $LOG_DIR"
echo ""

# Ordre de démarrage: auth, gateway, puis les autres
SERVICES=(
  "ms-auth:8081"
  "ms-gateway:8080"
  "ms-besoins:8082"
  "ms-fournisseurs:8084"
  "ms-suivi:8085"
  "ms-achats:8083"
  "ms-notification:8088"
  "ms-audit:8089"
  "ms-reporting:8086"
  "ms-chatbot:8087"
)

for svc in "${SERVICES[@]}"; do
  IFS=':' read -r name port <<< "$svc"
  echo "[START] $name (port $port)"
  cd "$BASE_DIR/backend/$name" && nohup mvn spring-boot:run > "$LOG_DIR/$name.log" 2>&1 &
  sleep 15
done

echo ""
echo "=== Tous les services lancés ==="
echo "Vérifier les logs: tail -f logs/ms-gateway.log"
echo "Swagger agrégé: http://localhost:8080/swagger-ui.html"
echo "Pour arrêter: pkill -f 'spring-boot:run'"
