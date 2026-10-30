#!/bin/bash
# Initialise PostgreSQL : une base Keycloak, puis dans la base "cse" un schéma et un utilisateur dédiés
# par microservice (pattern Database per Service). Chaque utilisateur n'a de droits que sur son schéma
# et son search_path pointe sur celui-ci : aucun accès cross-schéma possible.
set -euo pipefail

SERVICES="auth besoins achats fournisseurs suivi reporting chatbot notification audit"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE keycloak LOGIN PASSWORD '${KEYCLOAK_DB_PASSWORD}';
CREATE DATABASE keycloak OWNER keycloak;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
SQL

for svc in $SERVICES; do
  var="DB_PASSWORD_$(echo "$svc" | tr '[:lower:]' '[:upper:]')"
  password="${!var:-$CSE_DB_PASSWORD}"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE ms_${svc} LOGIN PASSWORD '${password}';
CREATE SCHEMA ${svc} AUTHORIZATION ms_${svc};
ALTER ROLE ms_${svc} SET search_path = ${svc};
REVOKE ALL ON SCHEMA ${svc} FROM PUBLIC;
SQL
done
echo "Schémas et utilisateurs CSE créés : $SERVICES"
