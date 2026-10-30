CREATE SCHEMA IF NOT EXISTS auth;

CREATE TABLE IF NOT EXISTS auth.utilisateurs (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    type_utilisateur VARCHAR(50) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    chantier_attribue_id UUID,
    perimetre_parc VARCHAR(255),
    seuil_delegation DECIMAL(19,2),
    budget_gere DECIMAL(19,2),
    fournisseur_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

DO $$ BEGIN
    CREATE TYPE auth.statut_compte AS ENUM ('ACTIF','INACTIF','SUSPENDU');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;
