#!/usr/bin/env bash
echo "Arrêt de tous les microservices CSE..."
pkill -f 'spring-boot:run' 2>/dev/null
pkill -f 'ms-auth' 2>/dev/null
pkill -f 'ms-gateway' 2>/dev/null
pkill -f 'ms-besoins' 2>/dev/null
pkill -f 'ms-achats' 2>/dev/null
pkill -f 'ms-fournisseurs' 2>/dev/null
pkill -f 'ms-suivi' 2>/dev/null
pkill -f 'ms-reporting' 2>/dev/null
pkill -f 'ms-chatbot' 2>/dev/null
pkill -f 'ms-notification' 2>/dev/null
pkill -f 'ms-audit' 2>/dev/null
echo "Services arrêtés"
