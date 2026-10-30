-- ms-fournisseurs — schéma « fournisseurs » : référentiel, documents, évaluations, catalogue, litiges.
-- Les identifiants sont générés par l'application (UUID) ; les énumérations sont des types ENUM natifs.

CREATE TYPE statut_fournisseur AS ENUM ('EN_ATTENTE_VALIDATION', 'ACTIF', 'SUSPENDU', 'BLACKLISTE');
CREATE TYPE categorie_materiel AS ENUM ('MATERIEL_ROULANT', 'OUTILLAGE', 'EQUIPEMENT_INFORMATIQUE', 'CONSOMMABLE');
CREATE TYPE type_document AS ENUM ('NINEA', 'RCCM', 'QUITUS_FISCAL', 'ASSURANCE', 'CERTIFICATION', 'AUTRE');
CREATE TYPE gravite_litige AS ENUM ('FAIBLE', 'MOYENNE', 'ELEVEE');
CREATE TYPE statut_litige AS ENUM ('OUVERT', 'EN_COURS', 'RESOLU', 'CLASSE');
CREATE TYPE type_action_litige AS ENUM ('OUVERTURE', 'ACTION', 'RESOLUTION', 'CLASSEMENT');

-- Agrégat Fournisseur ---------------------------------------------------------------------------
CREATE TABLE fournisseurs (
    id                          UUID PRIMARY KEY,
    raison_sociale              VARCHAR(200)       NOT NULL,
    ninea                       VARCHAR(30)        NOT NULL,
    rccm                        VARCHAR(50),
    adresse                     VARCHAR(255),
    ville                       VARCHAR(100),
    pays                        VARCHAR(100)       NOT NULL,
    telephone                   VARCHAR(30),
    email                       VARCHAR(200)       NOT NULL,
    contact_nom                 VARCHAR(150),
    contact_fonction            VARCHAR(150),
    statut                      statut_fournisseur NOT NULL,
    motif_statut                VARCHAR(1000),
    -- Coordonnées bancaires chiffrées AES-256-GCM (Base64) : jamais d'agrégat SQL sur ces colonnes.
    banque                      TEXT,
    iban                        TEXT,
    blacklistage_demandeur_id   UUID,
    blacklistage_demandeur_nom  VARCHAR(150),
    blacklistage_motif          VARCHAR(1000),
    blacklistage_date           TIMESTAMPTZ,
    score_performance           NUMERIC(5, 1)      NOT NULL DEFAULT 0,
    nombre_evaluations          INTEGER            NOT NULL DEFAULT 0,
    livraisons_total            INTEGER            NOT NULL DEFAULT 0,
    livraisons_a_l_heure        INTEGER            NOT NULL DEFAULT 0,
    date_inscription            DATE               NOT NULL,
    version                     BIGINT             NOT NULL,
    created_at                  TIMESTAMPTZ        NOT NULL,
    created_by                  VARCHAR(100),
    updated_at                  TIMESTAMPTZ        NOT NULL,
    updated_by                  VARCHAR(100),
    CONSTRAINT uk_fournisseurs_ninea UNIQUE (ninea),
    CONSTRAINT ck_fournisseurs_score CHECK (score_performance BETWEEN 0 AND 100),
    CONSTRAINT ck_fournisseurs_livraisons CHECK (livraisons_a_l_heure <= livraisons_total)
);
CREATE INDEX idx_fournisseurs_statut ON fournisseurs (statut);
CREATE INDEX idx_fournisseurs_raison_sociale ON fournisseurs (lower(raison_sociale));

CREATE TABLE fournisseur_specialites (
    fournisseur_id UUID               NOT NULL REFERENCES fournisseurs (id) ON DELETE CASCADE,
    specialite     categorie_materiel NOT NULL,
    PRIMARY KEY (fournisseur_id, specialite)
);
CREATE INDEX idx_fournisseur_specialites_specialite ON fournisseur_specialites (specialite);

CREATE TABLE fournisseur_certifications (
    fournisseur_id UUID         NOT NULL REFERENCES fournisseurs (id) ON DELETE CASCADE,
    ordre          INTEGER      NOT NULL,
    certification  VARCHAR(150) NOT NULL,
    PRIMARY KEY (fournisseur_id, ordre)
);

CREATE TABLE documents_administratifs (
    id                  UUID PRIMARY KEY,
    fournisseur_id      UUID          NOT NULL REFERENCES fournisseurs (id) ON DELETE CASCADE,
    type                type_document NOT NULL,
    libelle             VARCHAR(200)  NOT NULL,
    date_expiration     DATE,
    expiration_notifiee BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ   NOT NULL,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ   NOT NULL,
    updated_by          VARCHAR(100)
);
CREATE INDEX idx_documents_fournisseur ON documents_administratifs (fournisseur_id);
CREATE INDEX idx_documents_expiration ON documents_administratifs (date_expiration)
    WHERE date_expiration IS NOT NULL;

-- Évaluations ----------------------------------------------------------------------------------
CREATE TABLE evaluations (
    id              UUID PRIMARY KEY,
    fournisseur_id  UUID          NOT NULL REFERENCES fournisseurs (id),
    contrat_id      UUID,
    qualite         INTEGER       NOT NULL CHECK (qualite BETWEEN 0 AND 10),
    delais          INTEGER       NOT NULL CHECK (delais BETWEEN 0 AND 10),
    conformite      INTEGER       NOT NULL CHECK (conformite BETWEEN 0 AND 10),
    sav             INTEGER       NOT NULL CHECK (sav BETWEEN 0 AND 10),
    prix            INTEGER       NOT NULL CHECK (prix BETWEEN 0 AND 10),
    note_globale    NUMERIC(5, 1) NOT NULL CHECK (note_globale BETWEEN 0 AND 100),
    commentaire     VARCHAR(2000),
    evaluateur_id   UUID,
    evaluateur_nom  VARCHAR(150)  NOT NULL,
    date_evaluation TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ   NOT NULL,
    updated_by      VARCHAR(100)
);
CREATE INDEX idx_evaluations_fournisseur_date ON evaluations (fournisseur_id, date_evaluation DESC);

-- Catalogue de prix ----------------------------------------------------------------------------
CREATE TABLE lignes_catalogue (
    id                    UUID PRIMARY KEY,
    fournisseur_id        UUID               NOT NULL REFERENCES fournisseurs (id),
    reference             VARCHAR(80)        NOT NULL,
    designation           VARCHAR(255)       NOT NULL,
    categorie             categorie_materiel NOT NULL,
    prix_unitaire         NUMERIC(15, 0)     NOT NULL CHECK (prix_unitaire > 0),
    unite                 VARCHAR(30)        NOT NULL,
    delai_livraison_jours INTEGER            NOT NULL CHECK (delai_livraison_jours >= 0),
    date_maj              TIMESTAMPTZ        NOT NULL,
    version               BIGINT             NOT NULL,
    created_at            TIMESTAMPTZ        NOT NULL,
    created_by            VARCHAR(100),
    updated_at            TIMESTAMPTZ        NOT NULL,
    updated_by            VARCHAR(100),
    CONSTRAINT uk_lignes_catalogue_reference UNIQUE (fournisseur_id, reference)
);
CREATE INDEX idx_lignes_catalogue_categorie ON lignes_catalogue (categorie);

CREATE TABLE prix_historiques (
    id             UUID PRIMARY KEY,
    fournisseur_id UUID           NOT NULL REFERENCES fournisseurs (id),
    reference      VARCHAR(80)    NOT NULL,
    designation    VARCHAR(255)   NOT NULL,
    prix_unitaire  NUMERIC(15, 0) NOT NULL,
    date_prix      TIMESTAMPTZ    NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL,
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ    NOT NULL,
    updated_by     VARCHAR(100)
);
CREATE INDEX idx_prix_historiques_reference ON prix_historiques (fournisseur_id, reference, date_prix DESC);

-- Litiges --------------------------------------------------------------------------------------
-- Les références LIT-AAAA-NNN proviennent d'une séquence par année (seq_reference_litige_AAAA),
-- créée à la demande par l'application.
CREATE TABLE litiges (
    id              UUID PRIMARY KEY,
    reference       VARCHAR(30)    NOT NULL,
    fournisseur_id  UUID           NOT NULL REFERENCES fournisseurs (id),
    raison_sociale  VARCHAR(200)   NOT NULL,
    contrat_id      UUID,
    objet           VARCHAR(255)   NOT NULL,
    description     VARCHAR(4000)  NOT NULL,
    gravite         gravite_litige NOT NULL,
    statut          statut_litige  NOT NULL,
    date_ouverture  TIMESTAMPTZ    NOT NULL,
    date_resolution TIMESTAMPTZ,
    version         BIGINT         NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ    NOT NULL,
    updated_by      VARCHAR(100),
    CONSTRAINT uk_litiges_reference UNIQUE (reference)
);
CREATE INDEX idx_litiges_fournisseur_statut ON litiges (fournisseur_id, statut);
CREATE INDEX idx_litiges_statut ON litiges (statut);

CREATE TABLE actions_litige (
    litige_id   UUID               NOT NULL REFERENCES litiges (id) ON DELETE CASCADE,
    ordre       INTEGER            NOT NULL,
    date_action TIMESTAMPTZ        NOT NULL,
    auteur_id   UUID,
    auteur_nom  VARCHAR(150)       NOT NULL,
    type        type_action_litige NOT NULL,
    commentaire VARCHAR(2000)      NOT NULL,
    PRIMARY KEY (litige_id, ordre)
);
