#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd $PROJECT_DIR/frontend

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   CSE PLATFORM - Démarrage Frontend${NC}"
echo -e "${BLUE}========================================${NC}"

mkdir -p ../logs

echo -e "${YELLOW}[INFO]${NC} Installation des dépendances..."
pnpm install --silent

echo -e "${YELLOW}[INFO]${NC} Démarrage web-interne (http://localhost:5173)..."
nohup pnpm --filter @cse/web-interne dev > ../logs/web-interne.log 2>&1 &
sleep 5

echo -e "${YELLOW}[INFO]${NC} Démarrage portail-fournisseur (http://localhost:5174)..."
nohup pnpm --filter @cse/portail-fournisseur dev > ../logs/portail-fournisseur.log 2>&1 &
sleep 5

echo ""
echo -e "${GREEN}[OK]${NC} Frontend lancé !"
echo -e "  • Web Interne    : http://localhost:5173"
echo -e "  • Portail Fournisseur: http://localhost:5174"
echo ""
