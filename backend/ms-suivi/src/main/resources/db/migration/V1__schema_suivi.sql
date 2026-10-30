-- =====================================================================================
-- ms-suivi : chantiers, parc matériel, affectations, maintenances, pannes, mouvements.
-- Schéma « suivi » (Database per Service). Identifiants UUID générés par l'application.
-- =====================================================================================

-- Énumérations natives (alignées sur les enums du domaine et sur docs/API_CONTRACT.md §1)
CREATE TYPE categorie_materiel AS ENUM ('MATERIEL_ROULANT', 'OUTILLAGE', 'EQUIPEMENT_INFORMATIQUE', 'CONSOMMABLE');
CREATE TYPE etat_materiel AS ENUM ('DISPONIBLE', 'AFFECTE', 'EN_MAINTENANCE', 'REFORME');
CREATE TYPE statut_chantier AS ENUM ('EN_COURS', 'TERMINE');
CREATE TYPE statut_affectation AS ENUM ('EN_ATTENTE_RECEPTION', 'EN_COURS', 'TERMINEE');
CREATE TYPE type_maintenance AS ENUM ('PREVENTIVE', 'CORRECTIVE', 'LOURDE');
CREATE TYPE statut_maintenance AS ENUM ('PLANIFIEE', 'EN_COURS', 'CLOTUREE');
CREATE TYPE gravite_panne AS ENUM ('MINEURE', 'MAJEURE', 'BLOQUANTE');
CREATE TYPE statut_panne AS ENUM ('SIGNALEE', 'PRISE_EN_CHARGE', 'RESOLUE');
CREATE TYPE type_mouvement AS ENUM ('AFFECTATION', 'RECEPTION', 'LIBERATION', 'MAINTENANCE', 'REFORME', 'ENREGISTREMENT');

-- Référentiel des types d'engins (seuils de maintenance préventive)
CREATE TABLE types_engins (
    id                       UUID PRIMARY KEY,
    code                     VARCHAR(40)        NOT NULL,
    libelle                  VARCHAR(150)       NOT NULL,
    categorie                categorie_materiel NOT NULL,
    seuil_maintenance_heures INTEGER CHECK (seuil_maintenance_heures > 0),
    seuil_maintenance_jours  INTEGER CHECK (seuil_maintenance_jours > 0),
    actif                    BOOLEAN            NOT NULL DEFAULT TRUE,
    version                  BIGINT             NOT NULL,
    created_at               TIMESTAMPTZ        NOT NULL,
    created_by               VARCHAR(100),
    updated_at               TIMESTAMPTZ,
    updated_by               VARCHAR(100),
    CONSTRAINT uk_types_engins_code UNIQUE (code)
);

CREATE TABLE chantiers (
    id                UUID PRIMARY KEY,
    code              VARCHAR(40)      NOT NULL,
    nom               VARCHAR(200)     NOT NULL,
    localisation      VARCHAR(200)     NOT NULL,
    region            VARCHAR(100)     NOT NULL,
    latitude          DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude         DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    statut            statut_chantier  NOT NULL,
    budget            NUMERIC(18, 0)   NOT NULL CHECK (budget >= 0),
    montant_marche    NUMERIC(18, 0)   NOT NULL CHECK (montant_marche >= 0),
    date_debut        DATE             NOT NULL,
    date_fin_prevue   DATE,
    chef_chantier_id  UUID,
    chef_chantier_nom VARCHAR(150),
    version           BIGINT           NOT NULL,
    created_at        TIMESTAMPTZ      NOT NULL,
    created_by        VARCHAR(100),
    updated_at        TIMESTAMPTZ,
    updated_by        VARCHAR(100),
    CONSTRAINT uk_chantiers_code UNIQUE (code),
    CONSTRAINT ck_chantiers_dates CHECK (date_fin_prevue IS NULL OR date_fin_prevue >= date_debut)
);
CREATE INDEX idx_chantiers_statut ON chantiers (statut);
CREATE INDEX idx_chantiers_chef ON chantiers (chef_chantier_id);

CREATE TABLE materiels (
    id                               UUID PRIMARY KEY,
    code                             VARCHAR(40)        NOT NULL,
    immatriculation                  VARCHAR(40),
    designation                      VARCHAR(200)       NOT NULL,
    categorie                        categorie_materiel NOT NULL,
    type_engin_id                    UUID REFERENCES types_engins (id),
    marque                           VARCHAR(100),
    modele                           VARCHAR(100),
    etat                             etat_materiel      NOT NULL,
    heures_fonctionnement            BIGINT             NOT NULL CHECK (heures_fonctionnement >= 0),
    heures_derniere_maintenance      BIGINT             NOT NULL CHECK (heures_derniere_maintenance >= 0),
    date_reference_maintenance       DATE               NOT NULL,
    seuil_maintenance_heures         INTEGER,
    seuil_maintenance_jours          INTEGER,
    prochaine_maintenance            DATE,
    date_derniere_alerte_maintenance DATE,
    tarif_journalier                 NUMERIC(18, 0)     NOT NULL CHECK (tarif_journalier >= 0),
    valeur_acquisition               NUMERIC(18, 0) CHECK (valeur_acquisition >= 0),
    date_acquisition                 DATE,
    chantier_actuel_id               UUID REFERENCES chantiers (id),
    latitude                         DOUBLE PRECISION,
    longitude                        DOUBLE PRECISION,
    fournisseur_id                   UUID,
    contrat_id                       UUID,
    reforme_demandee                 BOOLEAN            NOT NULL DEFAULT FALSE,
    motif_reforme                    VARCHAR(500),
    version                          BIGINT             NOT NULL,
    created_at                       TIMESTAMPTZ        NOT NULL,
    created_by                       VARCHAR(100),
    updated_at                       TIMESTAMPTZ,
    updated_by                       VARCHAR(100),
    CONSTRAINT uk_materiels_code UNIQUE (code)
);
CREATE INDEX idx_materiels_etat ON materiels (etat);
CREATE INDEX idx_materiels_categorie ON materiels (categorie);
CREATE INDEX idx_materiels_chantier ON materiels (chantier_actuel_id);
CREATE INDEX idx_materiels_type_engin ON materiels (type_engin_id);
CREATE INDEX idx_materiels_prochaine_maintenance ON materiels (prochaine_maintenance);
CREATE INDEX idx_materiels_contrat ON materiels (contrat_id);

CREATE TABLE affectations (
    id                     UUID PRIMARY KEY,
    materiel_id            UUID               NOT NULL REFERENCES materiels (id),
    chantier_id            UUID               NOT NULL REFERENCES chantiers (id),
    besoin_id              UUID,
    date_debut             DATE               NOT NULL,
    date_fin_prevue        DATE               NOT NULL,
    date_fin_reelle        DATE,
    statut                 statut_affectation NOT NULL,
    date_reception         TIMESTAMPTZ,
    observations_reception VARCHAR(1000),
    commentaire_liberation VARCHAR(1000),
    fournisseur_id         UUID,
    contrat_id             UUID,
    alerte_fin_envoyee     BOOLEAN            NOT NULL DEFAULT FALSE,
    version                BIGINT             NOT NULL,
    created_at             TIMESTAMPTZ        NOT NULL,
    created_by             VARCHAR(100),
    updated_at             TIMESTAMPTZ,
    updated_by             VARCHAR(100),
    CONSTRAINT ck_affectations_periode CHECK (date_fin_prevue >= date_debut),
    CONSTRAINT ck_affectations_fin_reelle CHECK (date_fin_reelle IS NULL OR date_fin_reelle >= date_debut)
);
-- Invariant de concurrence (R04) : un matériel n'a jamais plus d'une affectation active.
CREATE UNIQUE INDEX ux_affectations_materiel_active ON affectations (materiel_id) WHERE statut <> 'TERMINEE';
CREATE INDEX idx_affectations_materiel ON affectations (materiel_id, date_debut DESC);
CREATE INDEX idx_affectations_chantier ON affectations (chantier_id, statut);
CREATE INDEX idx_affectations_periode ON affectations (date_debut, date_fin_prevue);

CREATE TABLE pannes (
    id               UUID PRIMARY KEY,
    materiel_id      UUID          NOT NULL REFERENCES materiels (id),
    chantier_id      UUID REFERENCES chantiers (id),
    gravite          gravite_panne NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    statut           statut_panne  NOT NULL,
    signale_par_id   UUID,
    signale_par_nom  VARCHAR(150),
    date_signalement TIMESTAMPTZ   NOT NULL,
    maintenance_id   UUID,
    version          BIGINT        NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    created_by       VARCHAR(100),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(100)
);
CREATE INDEX idx_pannes_materiel ON pannes (materiel_id, date_signalement DESC);
CREATE INDEX idx_pannes_chantier ON pannes (chantier_id, statut);

CREATE TABLE maintenances (
    id              UUID PRIMARY KEY,
    numero_ot       VARCHAR(30)        NOT NULL,
    materiel_id     UUID               NOT NULL REFERENCES materiels (id),
    chantier_id     UUID REFERENCES chantiers (id),
    type            type_maintenance   NOT NULL,
    statut          statut_maintenance NOT NULL,
    description     VARCHAR(1000)      NOT NULL,
    date_planifiee  DATE               NOT NULL,
    date_debut      DATE,
    date_fin        DATE,
    technicien      VARCHAR(150),
    cout            NUMERIC(18, 0) CHECK (cout >= 0),
    heures_compteur BIGINT,
    observations    VARCHAR(1000),
    panne_id        UUID REFERENCES pannes (id),
    version         BIGINT             NOT NULL,
    created_at      TIMESTAMPTZ        NOT NULL,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    CONSTRAINT uk_maintenances_numero_ot UNIQUE (numero_ot)
);
CREATE INDEX idx_maintenances_materiel ON maintenances (materiel_id, statut);
CREATE INDEX idx_maintenances_date ON maintenances (date_planifiee DESC);
CREATE INDEX idx_maintenances_chantier ON maintenances (chantier_id);

ALTER TABLE pannes ADD CONSTRAINT fk_pannes_maintenance FOREIGN KEY (maintenance_id) REFERENCES maintenances (id);

-- Journal des mouvements (ajout seul : pas de version ni de mise à jour)
CREATE TABLE mouvements (
    id                       UUID PRIMARY KEY,
    materiel_id              UUID           NOT NULL REFERENCES materiels (id),
    type                     type_mouvement NOT NULL,
    chantier_origine_id      UUID,
    chantier_origine_nom     VARCHAR(200),
    chantier_destination_id  UUID,
    chantier_destination_nom VARCHAR(200),
    date_mouvement           TIMESTAMPTZ    NOT NULL,
    auteur_id                UUID,
    auteur_nom               VARCHAR(150),
    commentaire              VARCHAR(1000),
    created_at               TIMESTAMPTZ    NOT NULL,
    created_by               VARCHAR(100)
);
CREATE INDEX idx_mouvements_materiel ON mouvements (materiel_id, date_mouvement DESC);
