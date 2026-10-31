-- Vues de lecture (CQRS) de ms-reporting, alimentées exclusivement par les événements Kafka (D3).
-- Chaque table est un instantané par agrégat : maj_le = instant du dernier événement appliqué
-- (un événement plus ancien, arrivé en retard, ne l'écrase pas).

CREATE TYPE categorie_materiel AS ENUM ('MATERIEL_ROULANT', 'OUTILLAGE', 'EQUIPEMENT_INFORMATIQUE', 'CONSOMMABLE');
CREATE TYPE etat_materiel AS ENUM ('DISPONIBLE', 'AFFECTE', 'EN_MAINTENANCE', 'REFORME');
CREATE TYPE statut_chantier AS ENUM ('EN_COURS', 'TERMINE');
CREATE TYPE statut_besoin AS ENUM ('EN_ATTENTE', 'VALIDE', 'REJETE', 'AFFECTE_DIRECT', 'EN_ACHAT');
CREATE TYPE priorite AS ENUM ('NORMAL', 'URGENT', 'CRITIQUE');
CREATE TYPE statut_affectation AS ENUM ('EN_ATTENTE_RECEPTION', 'EN_COURS', 'TERMINEE');
CREATE TYPE type_maintenance AS ENUM ('PREVENTIVE', 'CORRECTIVE', 'LOURDE');
CREATE TYPE statut_maintenance AS ENUM ('PLANIFIEE', 'EN_COURS', 'CLOTUREE');
CREATE TYPE gravite_panne AS ENUM ('MINEURE', 'MAJEURE', 'BLOQUANTE');
CREATE TYPE statut_fournisseur AS ENUM ('EN_ATTENTE_VALIDATION', 'ACTIF', 'SUSPENDU', 'BLACKLISTE');
CREATE TYPE gravite_litige AS ENUM ('FAIBLE', 'MOYENNE', 'ELEVEE');
CREATE TYPE statut_litige AS ENUM ('OUVERT', 'EN_COURS', 'RESOLU', 'CLASSE');
CREATE TYPE statut_appel_offre AS ENUM ('PUBLIE', 'CLOTURE', 'ATTRIBUE');
CREATE TYPE statut_contrat AS ENUM ('BROUILLON', 'SIGNE', 'ENGAGEMENT_VALIDE', 'EN_LIVRAISON', 'LIVRE', 'CLOTURE');
CREATE TYPE statut_facture AS ENUM ('RECUE', 'MANDATEE', 'PAYEE', 'REJETEE');

CREATE TABLE chantiers (
    id               UUID PRIMARY KEY,
    code             VARCHAR(40),
    nom              VARCHAR(200),
    region           VARCHAR(100),
    statut           statut_chantier,
    budget           NUMERIC(18, 0) NOT NULL DEFAULT 0,
    montant_marche   NUMERIC(18, 0) NOT NULL DEFAULT 0,
    chef_chantier_id UUID,
    date_debut       DATE,
    date_fin_prevue  DATE,
    complet          BOOLEAN        NOT NULL,
    maj_le           TIMESTAMPTZ    NOT NULL
);

CREATE TABLE materiels (
    id                    UUID PRIMARY KEY,
    code                  VARCHAR(60),
    designation           VARCHAR(200),
    categorie             categorie_materiel,
    etat                  etat_materiel,
    chantier_id           UUID,
    tarif_journalier      NUMERIC(18, 0) NOT NULL DEFAULT 0,
    valeur_acquisition    NUMERIC(18, 0),
    alerte_maintenance_le TIMESTAMPTZ,
    etat_maj_le           TIMESTAMPTZ
);
CREATE INDEX idx_materiels_chantier ON materiels (chantier_id);

CREATE TABLE affectations (
    id               UUID PRIMARY KEY,
    materiel_id      UUID           NOT NULL,
    categorie        categorie_materiel,
    chantier_id      UUID,
    tarif_journalier NUMERIC(18, 0) NOT NULL DEFAULT 0,
    besoin_id        UUID,
    date_debut       DATE,
    date_fin_prevue  DATE,
    date_fin_reelle  DATE,
    statut           statut_affectation,
    maj_le           TIMESTAMPTZ    NOT NULL
);
-- Recherche des affectations chevauchant une période, par chantier.
CREATE INDEX idx_affectations_periode ON affectations (date_debut, date_fin_reelle);
CREATE INDEX idx_affectations_chantier ON affectations (chantier_id, date_debut);
CREATE INDEX idx_affectations_materiel ON affectations (materiel_id);

CREATE TABLE maintenances (
    id             UUID PRIMARY KEY,
    numero_ot      VARCHAR(40),
    materiel_id    UUID        NOT NULL,
    chantier_id    UUID,
    type           type_maintenance,
    statut         statut_maintenance,
    cout           NUMERIC(18, 0),
    date_planifiee DATE,
    date_debut     DATE,
    date_fin       DATE,
    cloturee_le    TIMESTAMPTZ,
    maj_le         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_maintenances_materiel ON maintenances (materiel_id, cloturee_le);
CREATE INDEX idx_maintenances_chantier ON maintenances (chantier_id);
CREATE INDEX idx_maintenances_dates ON maintenances (date_fin, date_debut);

CREATE TABLE pannes (
    id          UUID PRIMARY KEY,
    materiel_id UUID        NOT NULL,
    chantier_id UUID,
    gravite     gravite_panne,
    signalee_le TIMESTAMPTZ NOT NULL,
    resolue_le  TIMESTAMPTZ
);
CREATE INDEX idx_pannes_ouvertes ON pannes (materiel_id) WHERE resolue_le IS NULL;
CREATE INDEX idx_pannes_chantier ON pannes (chantier_id);

CREATE TABLE besoins (
    id             UUID PRIMARY KEY,
    reference      VARCHAR(40),
    chantier_id    UUID,
    chantier_nom   VARCHAR(200),
    categorie      categorie_materiel,
    quantite       INT         NOT NULL DEFAULT 0,
    priorite       priorite,
    statut         statut_besoin,
    date_besoin    DATE,
    soumis_le      TIMESTAMPTZ,
    decide_le      TIMESTAMPTZ,
    premier_vu_le  TIMESTAMPTZ NOT NULL,
    -- Jour de soumission retenu pour les séries (calculé par le domaine, indexé pour les filtres).
    date_soumission DATE,
    maj_le         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_besoins_soumission ON besoins (date_soumission);
CREATE INDEX idx_besoins_chantier_statut ON besoins (chantier_id, statut);

CREATE TABLE appels_offres (
    id                    UUID PRIMARY KEY,
    reference             VARCHAR(40),
    chantier_id           UUID,
    besoin_id             UUID,
    categorie             categorie_materiel,
    statut                statut_appel_offre,
    premier_vu_le         TIMESTAMPTZ NOT NULL,
    date_limite           TIMESTAMPTZ,
    date_publication      DATE,
    montant_attribue      NUMERIC(18, 0),
    fournisseur_retenu_id UUID,
    maj_le                TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_appels_offres_publication ON appels_offres (date_publication);

CREATE TABLE contrats (
    id                    UUID PRIMARY KEY,
    numero                VARCHAR(40),
    appel_offre_id        UUID,
    chantier_id           UUID,
    fournisseur_id        UUID,
    fournisseur_nom       VARCHAR(200),
    montant               NUMERIC(18, 0) NOT NULL DEFAULT 0,
    statut                statut_contrat,
    date_signature        DATE,
    date_engagement       DATE,
    date_livraison_prevue DATE,
    date_livraison_reelle DATE,
    maj_le                TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_contrats_chantier ON contrats (chantier_id);
CREATE INDEX idx_contrats_fournisseur ON contrats (fournisseur_id);
CREATE INDEX idx_contrats_engagement ON contrats (date_engagement);

CREATE TABLE factures (
    id               UUID PRIMARY KEY,
    numero           VARCHAR(60),
    contrat_id       UUID,
    fournisseur_id   UUID,
    montant_ttc      NUMERIC(18, 0) NOT NULL DEFAULT 0,
    statut           statut_facture,
    date_echeance    DATE,
    date_reception   DATE,
    date_mandatement DATE,
    date_paiement    DATE,
    maj_le           TIMESTAMPTZ    NOT NULL
);
CREATE INDEX idx_factures_contrat ON factures (contrat_id);
CREATE INDEX idx_factures_dates ON factures (date_mandatement, date_paiement);

CREATE TABLE fournisseurs (
    id                UUID PRIMARY KEY,
    raison_sociale    VARCHAR(200),
    statut            statut_fournisseur,
    score_performance NUMERIC(5, 2) NOT NULL DEFAULT 0,
    complet           BOOLEAN       NOT NULL,
    maj_le            TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_fournisseurs_statut ON fournisseurs (statut, score_performance DESC);

CREATE TABLE litiges (
    id             UUID PRIMARY KEY,
    reference      VARCHAR(40),
    fournisseur_id UUID,
    contrat_id     UUID,
    gravite        gravite_litige,
    statut         statut_litige,
    ouvert_le      TIMESTAMPTZ NOT NULL,
    maj_le         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_litiges_fournisseur ON litiges (fournisseur_id, statut);

-- Instantané quotidien du parc par catégorie et chantier courant (UUID nul = hors chantier, car la
-- colonne appartient à la clé primaire).
CREATE TABLE parc_instantanes (
    jour           DATE               NOT NULL,
    categorie      categorie_materiel NOT NULL,
    chantier_cle   UUID               NOT NULL,
    total          INT                NOT NULL,
    disponibles    INT                NOT NULL,
    affectes       INT                NOT NULL,
    en_maintenance INT                NOT NULL,
    reformes       INT                NOT NULL,
    PRIMARY KEY (jour, categorie, chantier_cle)
);
