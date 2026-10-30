CREATE SCHEMA IF NOT EXISTS besoins;

CREATE TABLE IF NOT EXISTS besoins.besoins_materiel (
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

CREATE TABLE IF NOT EXISTS besoins.historique_validation (
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
