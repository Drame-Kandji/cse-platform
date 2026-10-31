-- Schéma du microservice ms-achats : appels d'offres, offres, contrats, engagements, factures,
-- projections locales (besoins à acheter, statut des fournisseurs).
-- Les montants sont chiffrés au repos (AES-256-GCM, colonnes TEXT) : aucun agrégat SQL dessus.

CREATE TYPE type_appel_offre AS ENUM ('APPEL_OFFRES', 'DEMANDE_PRIX');
CREATE TYPE statut_ao AS ENUM ('PUBLIE', 'CLOTURE', 'ATTRIBUE');
CREATE TYPE critere_evaluation AS ENUM ('PRIX', 'DELAI', 'QUALITE_TECHNIQUE', 'GARANTIE', 'SAV', 'REFERENCES');
CREATE TYPE statut_approbation AS ENUM ('EN_ATTENTE', 'APPROUVEE', 'REFUSEE');
CREATE TYPE statut_contrat AS ENUM ('BROUILLON', 'SIGNE', 'ENGAGEMENT_VALIDE', 'EN_LIVRAISON', 'LIVRE', 'CLOTURE');
CREATE TYPE statut_facture AS ENUM ('RECUE', 'MANDATEE', 'PAYEE', 'REJETEE');
CREATE TYPE categorie_materiel AS ENUM ('MATERIEL_ROULANT', 'OUTILLAGE', 'EQUIPEMENT_INFORMATIQUE', 'CONSOMMABLE');
CREATE TYPE priorite_besoin AS ENUM ('NORMAL', 'URGENT', 'CRITIQUE');
CREATE TYPE statut_fournisseur AS ENUM ('EN_ATTENTE_VALIDATION', 'ACTIF', 'SUSPENDU', 'BLACKLISTE');

-- ---------------------------------------------------------------------------------------------
-- Appels d'offres
-- ---------------------------------------------------------------------------------------------
CREATE TABLE appel_offre (
    id                           UUID PRIMARY KEY,
    reference                    VARCHAR(20)        NOT NULL,
    objet                        VARCHAR(255)       NOT NULL,
    description                  TEXT,
    type                         type_appel_offre   NOT NULL,
    besoin_id                    UUID,
    besoin_reference             VARCHAR(30),
    besoin_designation           VARCHAR(255),
    chantier_id                  UUID,
    chantier_nom                 VARCHAR(200),
    categorie                    categorie_materiel NOT NULL,
    quantite                     INT                NOT NULL CHECK (quantite > 0),
    specifications               TEXT,
    budget_estime                TEXT,
    date_publication             TIMESTAMPTZ        NOT NULL,
    date_limite                  TIMESTAMPTZ        NOT NULL,
    date_cloture                 TIMESTAMPTZ,
    statut                       statut_ao          NOT NULL,
    offre_retenue_id             UUID,
    montant_attribue             TEXT,
    -- Proposition d'attribution (R11) : absente tant qu'aucune proposition n'est en cours.
    attribution_offre_id         UUID,
    attribution_montant          TEXT,
    attribution_justification    TEXT,
    attribution_par_id           UUID,
    attribution_par_nom          VARCHAR(150),
    attribution_date             TIMESTAMPTZ,
    covalidation_statut          statut_approbation,
    covalidation_par_id          UUID,
    covalidation_par_nom         VARCHAR(150),
    covalidation_date            TIMESTAMPTZ,
    covalidation_commentaire     TEXT,
    approbation_dg_statut        statut_approbation,
    approbation_dg_par_id        UUID,
    approbation_dg_par_nom       VARCHAR(150),
    approbation_dg_date          TIMESTAMPTZ,
    approbation_dg_commentaire   TEXT,
    cree_par_id                  UUID,
    cree_par_nom                 VARCHAR(150)       NOT NULL,
    version                      BIGINT             NOT NULL,
    created_at                   TIMESTAMPTZ        NOT NULL,
    created_by                   VARCHAR(100),
    updated_at                   TIMESTAMPTZ,
    updated_by                   VARCHAR(100),
    CONSTRAINT uk_appel_offre_reference UNIQUE (reference)
);
CREATE INDEX idx_appel_offre_statut ON appel_offre (statut);
CREATE INDEX idx_appel_offre_date_publication ON appel_offre (date_publication);
CREATE INDEX idx_appel_offre_echeance_publies ON appel_offre (date_limite) WHERE statut = 'PUBLIE';
CREATE INDEX idx_appel_offre_besoin ON appel_offre (besoin_id);

CREATE TABLE appel_offre_critere (
    appel_offre_id UUID               NOT NULL REFERENCES appel_offre (id) ON DELETE CASCADE,
    ordre          INT                NOT NULL,
    critere        critere_evaluation NOT NULL,
    poids          INT                NOT NULL CHECK (poids BETWEEN 1 AND 100),
    PRIMARY KEY (appel_offre_id, ordre),
    CONSTRAINT uk_appel_offre_critere UNIQUE (appel_offre_id, critere)
);

CREATE TABLE appel_offre_invite (
    appel_offre_id UUID         NOT NULL REFERENCES appel_offre (id) ON DELETE CASCADE,
    ordre          INT          NOT NULL,
    fournisseur_id UUID         NOT NULL,
    raison_sociale VARCHAR(200) NOT NULL,
    PRIMARY KEY (appel_offre_id, ordre),
    CONSTRAINT uk_appel_offre_invite UNIQUE (appel_offre_id, fournisseur_id)
);
CREATE INDEX idx_appel_offre_invite_fournisseur ON appel_offre_invite (fournisseur_id);

CREATE TABLE offre (
    id                    UUID PRIMARY KEY,
    appel_offre_id        UUID         NOT NULL REFERENCES appel_offre (id) ON DELETE CASCADE,
    code_anonyme          VARCHAR(20)  NOT NULL,
    fournisseur_id        UUID         NOT NULL,
    fournisseur_nom       VARCHAR(200) NOT NULL,
    montant_propose       TEXT         NOT NULL,
    delai_livraison_jours INT          NOT NULL CHECK (delai_livraison_jours > 0),
    garantie_mois         INT CHECK (garantie_mois >= 0),
    description           TEXT,
    date_depot            TIMESTAMPTZ  NOT NULL,
    retenue               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL,
    created_by            VARCHAR(100),
    updated_at            TIMESTAMPTZ,
    updated_by            VARCHAR(100),
    CONSTRAINT uk_offre_fournisseur UNIQUE (appel_offre_id, fournisseur_id),
    CONSTRAINT uk_offre_code_anonyme UNIQUE (appel_offre_id, code_anonyme)
);
CREATE INDEX idx_offre_fournisseur ON offre (fournisseur_id);

CREATE TABLE offre_note (
    offre_id UUID               NOT NULL REFERENCES offre (id) ON DELETE CASCADE,
    critere  critere_evaluation NOT NULL,
    note     INT                NOT NULL CHECK (note BETWEEN 0 AND 100),
    PRIMARY KEY (offre_id, critere)
);

-- ---------------------------------------------------------------------------------------------
-- Contrats, engagements et jalons
-- ---------------------------------------------------------------------------------------------
CREATE TABLE contrat_achat (
    id                           UUID PRIMARY KEY,
    numero                       VARCHAR(20)        NOT NULL,
    appel_offre_id               UUID               NOT NULL REFERENCES appel_offre (id),
    appel_offre_reference        VARCHAR(20),
    offre_id                     UUID               NOT NULL,
    fournisseur_id               UUID               NOT NULL,
    fournisseur_nom              VARCHAR(200),
    besoin_id                    UUID,
    chantier_id                  UUID,
    chantier_nom                 VARCHAR(200),
    objet                        VARCHAR(255),
    designation                  VARCHAR(255),
    categorie                    categorie_materiel NOT NULL,
    quantite                     INT                NOT NULL CHECK (quantite > 0),
    montant                      TEXT               NOT NULL,
    statut                       statut_contrat     NOT NULL,
    date_signature               DATE,
    delai_livraison_jours        INT                NOT NULL CHECK (delai_livraison_jours > 0),
    date_livraison_prevue        DATE,
    date_livraison_reelle        DATE,
    numero_bon_livraison         VARCHAR(60),
    observations_livraison       TEXT,
    garantie_mois                INT,
    conditions_maintenance       TEXT,
    conditions_livraison         TEXT,
    penalite_retard_pct_par_jour NUMERIC(7, 4),
    engagement_statut            statut_approbation,
    engagement_montant           TEXT,
    engagement_par_id            UUID,
    engagement_par_nom           VARCHAR(150),
    engagement_date_demande      TIMESTAMPTZ,
    engagement_date_decision     TIMESTAMPTZ,
    engagement_commentaire       TEXT,
    penalites_retard             TEXT,
    date_derniere_alerte_retard  DATE,
    version                      BIGINT             NOT NULL,
    created_at                   TIMESTAMPTZ        NOT NULL,
    created_by                   VARCHAR(100),
    updated_at                   TIMESTAMPTZ,
    updated_by                   VARCHAR(100),
    CONSTRAINT uk_contrat_numero UNIQUE (numero),
    -- Un seul contrat par appel d'offres attribué (R03).
    CONSTRAINT uk_contrat_appel_offre UNIQUE (appel_offre_id)
);
CREATE INDEX idx_contrat_statut ON contrat_achat (statut);
CREATE INDEX idx_contrat_fournisseur ON contrat_achat (fournisseur_id);
CREATE INDEX idx_contrat_engagement ON contrat_achat (engagement_statut) WHERE engagement_statut IS NOT NULL;
CREATE INDEX idx_contrat_echeance ON contrat_achat (date_livraison_prevue);

CREATE TABLE contrat_jalon (
    id          UUID PRIMARY KEY,
    contrat_id  UUID         NOT NULL REFERENCES contrat_achat (id) ON DELETE CASCADE,
    ordre       INT          NOT NULL,
    libelle     VARCHAR(200) NOT NULL,
    date_prevue DATE         NOT NULL,
    date_reelle DATE,
    created_at  TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100)
);
CREATE INDEX idx_contrat_jalon_contrat ON contrat_jalon (contrat_id);

-- ---------------------------------------------------------------------------------------------
-- Factures
-- ---------------------------------------------------------------------------------------------
CREATE TABLE facture (
    id               UUID PRIMARY KEY,
    numero           VARCHAR(60)    NOT NULL,
    contrat_id       UUID           NOT NULL REFERENCES contrat_achat (id),
    contrat_numero   VARCHAR(20),
    fournisseur_id   UUID           NOT NULL,
    fournisseur_nom  VARCHAR(200),
    montant_ht       TEXT           NOT NULL,
    tva              TEXT           NOT NULL,
    date_emission    DATE           NOT NULL,
    date_echeance    DATE           NOT NULL,
    statut           statut_facture NOT NULL,
    reference_mandat VARCHAR(60),
    date_mandatement DATE,
    date_paiement    DATE,
    motif_rejet      TEXT,
    version          BIGINT         NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL,
    created_by       VARCHAR(100),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(100),
    CONSTRAINT uk_facture_fournisseur_numero UNIQUE (fournisseur_id, numero),
    CONSTRAINT ck_facture_echeance CHECK (date_echeance >= date_emission)
);
CREATE INDEX idx_facture_contrat ON facture (contrat_id);
CREATE INDEX idx_facture_statut_echeance ON facture (statut, date_echeance);

-- ---------------------------------------------------------------------------------------------
-- Projections locales alimentées par Kafka
-- ---------------------------------------------------------------------------------------------
CREATE TABLE besoin_a_acheter (
    besoin_id      UUID PRIMARY KEY,
    reference      VARCHAR(30),
    designation    VARCHAR(255),
    categorie      categorie_materiel,
    quantite       INT         NOT NULL,
    specifications TEXT,
    priorite       priorite_besoin,
    chantier_id    UUID,
    chantier_nom   VARCHAR(200),
    date_reception TIMESTAMPTZ NOT NULL,
    appel_offre_id UUID,
    version        BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100)
);
CREATE INDEX idx_besoin_a_acheter_reception ON besoin_a_acheter (date_reception);

CREATE TABLE fournisseur_projection (
    fournisseur_id UUID PRIMARY KEY,
    raison_sociale VARCHAR(200)       NOT NULL,
    statut         statut_fournisseur NOT NULL,
    derniere_maj   TIMESTAMPTZ        NOT NULL,
    version        BIGINT             NOT NULL,
    created_at     TIMESTAMPTZ        NOT NULL,
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100)
);
