#!/usr/bin/env bash
# Jeton d'accès de DÉVELOPPEMENT pour un compte de démonstration (client cse-tests-e2e, absent en production).
#   TOKEN=$(scripts/token.sh dg@cse.sn) ; curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
set -euo pipefail
curl -sf -d client_id=cse-tests-e2e -d grant_type=password -d "username=${1:?e-mail}" -d "password=${2:-Cse@2026}" \
  "${KEYCLOAK_URL:-http://localhost:8180}/realms/cse/protocol/openid-connect/token" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])'
