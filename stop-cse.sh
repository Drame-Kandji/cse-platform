#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}[INFO]${NC} Arrêt de tous les microservices CSE..."

pkill -9 -f 'spring-boot:run' 2>/dev/null
pkill -9 -f 'maven' 2>/dev/null
pkill -9 -f 'ms-auth' 2>/dev/null
pkill -9 -f 'ms-gateway' 2>/dev/null
pkill -9 -f 'ms-besoins' 2>/dev/null
pkill -9 -f 'ms-achats' 2>/dev/null
pkill -9 -f 'ms-fournisseurs' 2>/dev/null
pkill -9 -f 'ms-suivi' 2>/dev/null
pkill -9 -f 'ms-reporting' 2>/dev/null
pkill -9 -f 'ms-chatbot' 2>/dev/null
pkill -9 -f 'ms-notification' 2>/dev/null
pkill -9 -f 'ms-audit' 2>/dev/null

sleep 3

echo -e "${GREEN}[OK]${NC} Tous les microservices ont été arrêtés"
