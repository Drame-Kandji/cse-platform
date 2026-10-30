DROP TABLE IF EXISTS suivi.signalements_pannes CASCADE;
DROP TABLE IF EXISTS suivi.maintenances CASCADE;
DROP TABLE IF EXISTS suivi.affectations CASCADE;
DROP TABLE IF EXISTS suivi.materiels CASCADE;
DROP TABLE IF EXISTS suivi.chantiers CASCADE;

CREATE TABLE suivi.chantiers (
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

CREATE TABLE suivi.materiels (
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

CREATE TABLE suivi.affectations (
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

CREATE TABLE suivi.maintenances (
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

CREATE TABLE suivi.signalements_pannes (
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

INSERT INTO suivi.chantiers (id, nom, adresse, ville, latitude, longitude, statut, budget) VALUES
('11111111-1111-1111-1111-111111111111', 'Chantier Dakar Plateau', 'Dakar, Sénégal', 'Dakar', 14.6928, -17.4467, 'EN_COURS', 500000000),
('22222222-2222-2222-2222-222222222222', 'Chantier Thiès Autoroute', 'Thiès, Sénégal', 'Thiès', 14.7886, -16.9260, 'EN_COURS', 750000000),
('33333333-3333-3333-3333-333333333333', 'Chantier Saint-Louis Bridge', 'Saint-Louis, Sénégal', 'Saint-Louis', 16.0326, -16.4818, 'EN_COURS', 600000000),
('44444444-4444-4444-4444-444444444444', 'Chantier Kaolack Route', 'Kaolack, Sénégal', 'Kaolack', 14.1825, -16.2533, 'EN_COURS', 450000000),
('55555555-5555-5555-5555-555555555555', 'Chantier Ziguinchor Infrastructures', 'Ziguinchor, Sénégal', 'Ziguinchor', 12.5833, -16.2719, 'EN_COURS', 400000000)
ON CONFLICT (id) DO NOTHING;

INSERT INTO suivi.materiels (id, immatriculation, designation, categorie, etat, heures_fonctionnement, localisation) VALUES
('aaaa1111-1111-1111-1111-111111111111', 'CSE-PEL-001', 'Pelle hydraulique Caterpillar 320D', 'PELLE', 'DISPONIBLE', 1250, 'Parc Central Dakar'),
('aaaa2222-2222-2222-2222-222222222222', 'CSE-BUL-001', 'Bulldozer Komatsu D65EX', 'BULLDOZER', 'DISPONIBLE', 980, 'Parc Central Dakar'),
('aaaa3333-3333-3333-3333-333333333333', 'CSE-NIV-001', 'Niveleuse Caterpillar 140H', 'NIVELEUSE', 'DISPONIBLE', 1500, 'Thiès'),
('aaaa4444-4444-4444-4444-444444444444', 'CSE-CAM-001', 'Camion-benne Mercedes Actros', 'CAMION_BENNE', 'DISPONIBLE', 2100, 'Saint-Louis'),
('aaaa5555-5555-5555-5555-555555555555', 'CSE-GRU-001', 'Grue mobile Liebherr LTM 1055', 'GRUE', 'DISPONIBLE', 450, 'Parc Central Dakar')
ON CONFLICT (id) DO NOTHING;
