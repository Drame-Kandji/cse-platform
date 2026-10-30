#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   CSE PLATFORM - Démarrage Complet${NC}"
echo -e "${BLUE}========================================${NC}"

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

mkdir -p logs

if [ ! -f "backend/ms-auth/target/ms-auth-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "${YELLOW}[INFO]${NC} Compilation des modules..."
    mvn -f backend/pom.xml clean package -DskipTests
fi

echo -e "${YELLOW}[INFO]${NC} Arrêt des anciens processus..."
./stop-cse.sh >/dev/null 2>&1 || true

SERVICES=(
    "ms-auth:8081"
    "ms-gateway:8080"
    "ms-besoins:8082"
    "ms-achats:8083"
    "ms-fournisseurs:8084"
    "ms-suivi:8085"
    "ms-reporting:8086"
    "ms-chatbot:8087"
    "ms-notification:8088"
    "ms-audit:8089"
)

echo -e "${BLUE}[DEMARRAGE]${NC} Lancement des 10 microservices en arrière-plan..."
for item in "${SERVICES[@]}"; do
    SVC="${item%%:*}"
    PORT="${item##*:}"
    nohup java -Xmx256m -jar "backend/$SVC/target/$SVC-1.0.0-SNAPSHOT.jar" > "logs/$SVC.log" 2>&1 &
    echo -e "  → ${GREEN}$SVC${NC} lancé (port $PORT, PID $!)"
    sleep 1
done

echo ""
echo -e "${YELLOW}[INFO]${NC} Attente de l'initialisation des services (15s)..."
sleep 15

echo ""
echo -e "${BLUE}[VERIFICATION]${NC} État des 10 microservices :"
echo "--------------------------------------------------------"
for item in "${SERVICES[@]}"; do
    SVC="${item%%:*}"
    PORT="${item##*:}"
    if ss -tln 2>/dev/null | grep -q ":$PORT "; then
        echo -e "  Port $PORT ($SVC) : ${GREEN}● UP${NC}   → http://localhost:$PORT/swagger-ui.html"
    else
        echo -e "  Port $PORT ($SVC) : ${RED}● EN COURS / DOWN${NC} (voir logs/$SVC.log)"
    fi
done
echo "--------------------------------------------------------"
echo ""
echo -e "${GREEN}👉 Swagger Agrégé (Tous les services) : http://localhost:8080/swagger-ui.html${NC}"
echo -e "${YELLOW}Pour tout arrêter : ./stop-cse.sh${NC}"
