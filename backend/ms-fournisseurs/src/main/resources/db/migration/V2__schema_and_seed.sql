DROP TABLE IF EXISTS fournisseurs.litiges CASCADE;
DROP TABLE IF EXISTS fournisseurs.evaluations CASCADE;
DROP TABLE IF EXISTS fournisseurs.fournisseurs CASCADE;

CREATE TABLE fournisseurs.fournisseurs (
    id UUID PRIMARY KEY,
    raison_sociale VARCHAR(150) NOT NULL,
    ninea VARCHAR(50) UNIQUE NOT NULL,
    rc VARCHAR(50),
    email VARCHAR(150) NOT NULL,
    telephone VARCHAR(50),
    specialite VARCHAR(100) NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIF',
    score_performance DOUBLE PRECISION DEFAULT 80.0,
    demande_blacklistage BOOLEAN DEFAULT FALSE,
    motif_blacklistage TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE fournisseurs.evaluations (
    id UUID PRIMARY KEY,
    fournisseur_id UUID NOT NULL REFERENCES fournisseurs.fournisseurs(id),
    note_qualite DOUBLE PRECISION NOT NULL,
    note_delai DOUBLE PRECISION NOT NULL,
    note_conformite DOUBLE PRECISION NOT NULL,
    note_sav DOUBLE PRECISION NOT NULL,
    score_calcule DOUBLE PRECISION NOT NULL,
    commentaire TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE fournisseurs.litiges (
    id UUID PRIMARY KEY,
    fournisseur_id UUID NOT NULL REFERENCES fournisseurs.fournisseurs(id),
    motif TEXT NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'OUVERT',
    created_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO fournisseurs.fournisseurs (id, raison_sociale, ninea, rc, email, telephone, specialite, statut, score_performance)
VALUES
('f1111111-1111-1111-1111-111111111111', 'Sénégal Équipements BTP SA', 'SN-DKR-2018-00412', 'RC-DKR-1092', 'contact@senegal-btp.sn', '+221 33 821 00 11', 'ENGINS_LOURDS', 'ACTIF', 91.5),
('f2222222-2222-2222-2222-222222222222', 'Dakar Engins & Grues SARL', 'SN-DKR-2020-00891', 'RC-DKR-2341', 'commercial@dakarengins.sn', '+221 33 860 45 20', 'LEVAGE_GRUES', 'ACTIF', 86.0),
('f3333333-3333-3333-3333-333333333333', 'Sahel Matériaux & Transport', 'SN-THS-2019-00125', 'RC-THS-0512', 'info@sahel-materiaux.sn', '+221 33 951 12 34', 'TRANSPORT_BENNES', 'EN_ATTENTE_VALIDATION', 75.0),
('f4444444-4444-4444-4444-444444444444', 'Teranga Pièces & Maintenance', 'SN-DKR-2017-00309', 'RC-DKR-0988', 'sav@terangapieces.sn', '+221 33 832 90 00', 'MAINTENANCE_PIECES', 'SUSPENDU', 54.0)
ON CONFLICT (id) DO NOTHING;

