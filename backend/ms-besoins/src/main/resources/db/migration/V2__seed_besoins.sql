DROP TABLE IF EXISTS besoins.historique_validation CASCADE;
DROP TABLE IF EXISTS besoins.besoins_materiel CASCADE;

CREATE TABLE besoins.besoins_materiel (
    id UUID PRIMARY KEY,
    reference VARCHAR(50) UNIQUE,
    designation VARCHAR(150) NOT NULL,
    quantite INT NOT NULL,
    specifications TEXT,
    priorite VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_ATTENTE',
    chantier_id UUID NOT NULL,
    demandeur_id UUID NOT NULL,
    duree_utilisation_prevue INT,
    justification TEXT,
    motif_rejet TEXT,
    valide_par UUID,
    date_validation TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE besoins.historique_validation (
    id UUID PRIMARY KEY,
    besoin_id UUID REFERENCES besoins.besoins_materiel(id),
    action VARCHAR(50),
    acteur_id UUID,
    commentaire TEXT,
    date_action TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_besoins_chantier ON besoins.besoins_materiel(chantier_id);
CREATE INDEX IF NOT EXISTS idx_besoins_statut ON besoins.besoins_materiel(statut);
CREATE INDEX IF NOT EXISTS idx_besoins_priorite ON besoins.besoins_materiel(priorite);

INSERT INTO besoins.besoins_materiel (id, reference, designation, quantite, specifications, priorite, statut, chantier_id, demandeur_id, duree_utilisation_prevue, justification)
VALUES
('c1111111-1111-1111-1111-111111111111', 'BES-2026-001', 'Pelle hydraulique Caterpillar 320D', 2, 'Godet 1.5m3, chenilles renforcées', 'URGENT', 'EN_ATTENTE', '11111111-1111-1111-1111-111111111111', 'b1111111-1111-1111-1111-111111111111', 45, 'Démarrage terrassement phase 2 Dakar Plateau'),
('c2222222-2222-2222-2222-222222222222', 'BES-2026-002', 'Niveleuse Caterpillar 140H', 1, 'Lame 3.7m avec ripper arrière', 'NORMAL', 'VALIDE', '22222222-2222-2222-2222-222222222222', 'b1111111-1111-1111-1111-111111111111', 30, 'Nivellement plateforme autoroute Thiès'),
('c3333333-3333-3333-3333-333333333333', 'BES-2026-003', 'Grue mobile Liebherr 50T', 1, 'Flèche télescousique 40m', 'CRITIQUE', 'EN_ATTENTE', '33333333-3333-3333-3333-333333333333', 'b1111111-1111-1111-1111-111111111111', 15, 'Pose urgente des poutres du pont de Saint-Louis')
ON CONFLICT (id) DO NOTHING;
