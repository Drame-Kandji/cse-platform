-- Profils utilisateurs de ms-auth (hiérarchie UML Utilisateur persistée en SINGLE_TABLE, D2).
-- Aucun mot de passe : l'authentification est entièrement déléguée à Keycloak (ADR-0012).
-- La colonne "role" est le discriminateur JPA : VARCHAR + CHECK plutôt qu'un ENUM natif, car
-- Hibernate lie les discriminateurs comme des chaînes (voir ADR-0013).
CREATE TABLE utilisateurs (
    id               UUID PRIMARY KEY,
    role             VARCHAR(30)  NOT NULL,
    email            VARCHAR(255) NOT NULL,
    prenom           VARCHAR(150) NOT NULL,
    nom              VARCHAR(150) NOT NULL,
    telephone        VARCHAR(30),
    actif            BOOLEAN      NOT NULL DEFAULT TRUE,
    otp_configure    BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Attributs spécifiques (NULL pour les profils non concernés)
    chantier_ids     UUID[],
    perimetre_parc   VARCHAR(100),
    seuil_delegation NUMERIC(19, 0),
    budget_gere      NUMERIC(19, 0),
    fournisseur_id   UUID,
    created_at       TIMESTAMPTZ  NOT NULL,
    created_by       VARCHAR(64),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(64),
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_utilisateurs_role CHECK (role IN ('CHEF_CHANTIER', 'RESPONSABLE_MATERIEL',
        'RESPONSABLE_ACHATS', 'DAF', 'DIRECTION_GENERALE', 'ADMIN_SYSTEME', 'FOURNISSEUR')),
    CONSTRAINT ck_utilisateurs_montants CHECK (
        (seuil_delegation IS NULL OR seuil_delegation >= 0) AND (budget_gere IS NULL OR budget_gere >= 0))
);

CREATE UNIQUE INDEX ux_utilisateurs_email ON utilisateurs (lower(email));
CREATE INDEX idx_utilisateurs_role ON utilisateurs (role);
CREATE INDEX idx_utilisateurs_nom ON utilisateurs (nom, prenom);
CREATE INDEX idx_utilisateurs_fournisseur ON utilisateurs (fournisseur_id) WHERE fournisseur_id IS NOT NULL;
