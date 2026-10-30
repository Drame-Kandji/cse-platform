-- ms-besoins : besoins en matériel des chantiers, historique et matériels affectés.
-- Types ENUM natifs (AGENTS.md A.6) ; identifiants UUID générés par l'application.

CREATE TYPE categorie_materiel AS ENUM ('MATERIEL_ROULANT', 'OUTILLAGE', 'EQUIPEMENT_INFORMATIQUE', 'CONSOMMABLE');
CREATE TYPE priorite_besoin AS ENUM ('NORMAL', 'URGENT', 'CRITIQUE');
CREATE TYPE statut_besoin AS ENUM ('EN_ATTENTE', 'VALIDE', 'REJETE', 'AFFECTE_DIRECT', 'EN_ACHAT');
CREATE TYPE action_historique_besoin AS ENUM (
    'SOUMISSION', 'MODIFICATION', 'APPROBATION_ETAPE', 'VALIDATION', 'REJET',
    'ORIENTATION_EN_ATTENTE', 'AFFECTATION_DIRECTE', 'ORIENTATION_ACHAT', 'MATERIEL_AFFECTE');

CREATE TABLE besoins_materiels (
    id                       UUID PRIMARY KEY,
    reference                VARCHAR(30)        NOT NULL,
    chantier_id              UUID               NOT NULL,
    chantier_nom             VARCHAR(200),
    demandeur_id             UUID,
    demandeur_nom            VARCHAR(150),
    designation              VARCHAR(200)       NOT NULL,
    categorie                categorie_materiel NOT NULL,
    type_engin_id            UUID,
    quantite                 INT                NOT NULL CHECK (quantite > 0),
    specifications           VARCHAR(2000),
    priorite                 priorite_besoin    NOT NULL,
    statut                   statut_besoin      NOT NULL,
    duree_utilisation_jours  INT                NOT NULL CHECK (duree_utilisation_jours > 0),
    date_besoin              DATE               NOT NULL,
    justification            VARCHAR(2000),
    motif_rejet              VARCHAR(1000),
    validateur_id            UUID,
    validateur_nom           VARCHAR(150),
    date_validation          TIMESTAMPTZ,
    orientation_en_attente   BOOLEAN            NOT NULL DEFAULT FALSE,
    version                  BIGINT             NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ        NOT NULL,
    created_by               VARCHAR(100),
    updated_at               TIMESTAMPTZ        NOT NULL,
    updated_by               VARCHAR(100),
    CONSTRAINT uk_besoins_materiels_reference UNIQUE (reference),
    -- Règle R08 : justification obligatoire pour URGENT / CRITIQUE (défense en profondeur).
    CONSTRAINT ck_besoins_materiels_justification
        CHECK (priorite = 'NORMAL' OR justification IS NOT NULL),
    CONSTRAINT ck_besoins_materiels_motif_rejet
        CHECK (statut <> 'REJETE' OR motif_rejet IS NOT NULL)
);

CREATE INDEX idx_besoins_materiels_chantier ON besoins_materiels (chantier_id);
CREATE INDEX idx_besoins_materiels_statut ON besoins_materiels (statut);
CREATE INDEX idx_besoins_materiels_created_at ON besoins_materiels (created_at DESC);
CREATE INDEX idx_besoins_materiels_categorie ON besoins_materiels (categorie);
CREATE INDEX idx_besoins_materiels_orientation ON besoins_materiels (id) WHERE orientation_en_attente;

CREATE TABLE historiques_besoins (
    id               UUID PRIMARY KEY,
    besoin_id        UUID                     NOT NULL REFERENCES besoins_materiels (id) ON DELETE CASCADE,
    statut           statut_besoin            NOT NULL,
    action           action_historique_besoin NOT NULL,
    etape_validation VARCHAR(60),
    auteur_id        UUID,
    auteur_nom       VARCHAR(150),
    commentaire      VARCHAR(2000),
    date_action      TIMESTAMPTZ              NOT NULL
);

CREATE INDEX idx_historiques_besoins_besoin ON historiques_besoins (besoin_id, date_action);

CREATE TABLE besoins_materiels_affectes (
    besoin_id   UUID NOT NULL REFERENCES besoins_materiels (id) ON DELETE CASCADE,
    ordre       INT  NOT NULL,
    materiel_id UUID NOT NULL,
    PRIMARY KEY (besoin_id, ordre)
);

CREATE INDEX idx_besoins_materiels_affectes_materiel ON besoins_materiels_affectes (materiel_id);

-- Séquences de références annuelles (BES-<année>-<n°>) ; les années suivantes sont créées à la demande.
DO $$
BEGIN
    FOR annee IN 2024..2035 LOOP
        EXECUTE format('CREATE SEQUENCE IF NOT EXISTS seq_reference_besoin_%s', annee);
    END LOOP;
END
$$;
