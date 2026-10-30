CREATE SCHEMA IF NOT EXISTS suivi;

CREATE TABLE IF NOT EXISTS suivi.chantiers (
    id UUID PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    adresse VARCHAR(255),
    ville VARCHAR(100),
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_COURS',
    budget DECIMAL(19,2),
    maitre_ouvrage VARCHAR(150),
    date_debut DATE,
    date_fin_prevue DATE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS suivi.materiels (
    id UUID PRIMARY KEY,
    immatriculation VARCHAR(50) UNIQUE NOT NULL,
    designation VARCHAR(150) NOT NULL,
    categorie VARCHAR(100) NOT NULL,
    marque VARCHAR(100),
    modele VARCHAR(100),
    annee_fabrication INT,
    etat VARCHAR(50) NOT NULL DEFAULT 'DISPONIBLE',
    heures_fonctionnement BIGINT DEFAULT 0,
    localisation VARCHAR(150),
    chantier_actuel_id UUID REFERENCES suivi.chantiers(id),
    fournisseur_id UUID,
    contrat_id UUID,
    date_acquisition DATE,
    valeur_acquisition DECIMAL(19,2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS suivi.affectations (
    id UUID PRIMARY KEY,
    materiel_id UUID NOT NULL REFERENCES suivi.materiels(id),
    chantier_id UUID NOT NULL REFERENCES suivi.chantiers(id),
    besoin_id UUID,
    demandeur_id UUID,
    date_debut DATE NOT NULL,
    date_fin_prevue DATE,
    date_fin_reelle DATE,
    motif VARCHAR(255),
    affecte_par VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS suivi.maintenances (
    id UUID PRIMARY KEY,
    materiel_id UUID NOT NULL REFERENCES suivi.materiels(id),
    type_maintenance VARCHAR(50) NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'PLANIFIEE',
    date_planifiee DATE,
    date_debut TIMESTAMP,
    date_fin TIMESTAMP,
    cout DECIMAL(19,2),
    technicien VARCHAR(150),
    garage VARCHAR(150),
    motif VARCHAR(500),
    travaux_effectues TEXT,
    pieces_remplacees TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS suivi.signalements_pannes (
    id UUID PRIMARY KEY,
    materiel_id UUID NOT NULL REFERENCES suivi.materiels(id),
    chantier_id UUID,
    signale_par UUID,
    description TEXT NOT NULL,
    urgence VARCHAR(20) DEFAULT 'NORMALE',
    traite BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_materiels_etat ON suivi.materiels(etat);
CREATE INDEX IF NOT EXISTS idx_materiels_categorie ON suivi.materiels(categorie);
CREATE INDEX IF NOT EXISTS idx_affectations_materiel ON suivi.affectations(materiel_id);
CREATE INDEX IF NOT EXISTS idx_affectations_chantier ON suivi.affectations(chantier_id);
CREATE INDEX IF NOT EXISTS idx_maintenances_materiel ON suivi.maintenances(materiel_id);
