#!/usr/bin/env bash
set -e

echo "=== Configuration de la base PostgreSQL CSE ==="

CONTAINER=$(docker ps --format '{{.Names}}' | grep -E 'postgres' | head -n 1)

if [ -z "$CONTAINER" ]; then
    echo "Démarrage du conteneur PostgreSQL via docker compose..."
    docker compose -f infra/docker-compose.yml up -d postgres redis kafka keycloak mailhog
    sleep 5
    CONTAINER=$(docker ps --format '{{.Names}}' | grep -E 'postgres' | head -n 1)
fi

echo "Conteneur PostgreSQL détecté : $CONTAINER"

docker exec -i "$CONTAINER" psql -U postgres <<'EOSQL'
ALTER USER postgres WITH PASSWORD 'changeit';

SELECT 'CREATE DATABASE cse'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse')\gexec

\c cse

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS besoins;
CREATE SCHEMA IF NOT EXISTS achats;
CREATE SCHEMA IF NOT EXISTS fournisseurs;
CREATE SCHEMA IF NOT EXISTS suivi;
CREATE SCHEMA IF NOT EXISTS reporting;
CREATE SCHEMA IF NOT EXISTS chatbot;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS audit;

DO $$
DECLARE
    svc TEXT;
BEGIN
    FOREACH svc IN ARRAY ARRAY['cse_auth','cse_besoins','cse_achats','cse_fournisseurs','cse_suivi','cse_reporting','cse_chatbot','cse_notification','cse_audit']
    LOOP
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = svc) THEN
            EXECUTE format('CREATE USER %I WITH PASSWORD %L SUPERUSER', svc, svc);
        ELSE
            EXECUTE format('ALTER USER %I WITH PASSWORD %L SUPERUSER', svc, svc);
        END IF;
    END LOOP;
END
$$;

SELECT 'CREATE DATABASE cse_auth OWNER cse_auth' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_auth')\gexec
SELECT 'CREATE DATABASE cse_besoins OWNER cse_besoins' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_besoins')\gexec
SELECT 'CREATE DATABASE cse_suivi OWNER cse_suivi' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_suivi')\gexec
SELECT 'CREATE DATABASE cse_achats OWNER cse_achats' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_achats')\gexec
SELECT 'CREATE DATABASE cse_fournisseurs OWNER cse_fournisseurs' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_fournisseurs')\gexec
SELECT 'CREATE DATABASE cse_reporting OWNER cse_reporting' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_reporting')\gexec
SELECT 'CREATE DATABASE cse_chatbot OWNER cse_chatbot' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_chatbot')\gexec
SELECT 'CREATE DATABASE cse_notification OWNER cse_notification' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_notification')\gexec
SELECT 'CREATE DATABASE cse_audit OWNER cse_audit' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'cse_audit')\gexec
EOSQL

echo "=== Base PostgreSQL CSE prête ! ==="

