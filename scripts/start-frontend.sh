#!/usr/bin/env bash
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$BASE_DIR/logs"
mkdir -p "$LOG_DIR"

cd "$BASE_DIR/frontend"
echo "Installation des dépendances..."
pnpm install

echo "Démarrage web-interne (http://localhost:5173)..."
cd "$BASE_DIR/frontend" && nohup pnpm --filter @cse/web-interne dev > "$LOG_DIR/web-interne.log" 2>&1 &
sleep 5

echo "Démarrage portail-fournisseur (http://localhost:5174)..."
cd "$BASE_DIR/frontend" && nohup pnpm --filter @cse/portail-fournisseur dev > "$LOG_DIR/portail-fournisseur.log" 2>&1 &

echo "Frontend lancé!"
echo "Logs: $LOG_DIR/web-interne.log et $LOG_DIR/portail-fournisseur.log"
