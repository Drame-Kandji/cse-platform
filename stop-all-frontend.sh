#!/bin/bash
pkill -9 -f 'vite' 2>/dev/null
pkill -9 -f 'web-interne' 2>/dev/null
pkill -9 -f 'portail-fournisseur' 2>/dev/null
echo "Frontend arrêté"
