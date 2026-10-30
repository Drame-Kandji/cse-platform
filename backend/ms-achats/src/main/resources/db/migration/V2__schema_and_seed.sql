DROP TABLE IF EXISTS achats.contrats_achat CASCADE;
DROP TABLE IF EXISTS achats.offres CASCADE;
DROP TABLE IF EXISTS achats.appels_offres CASCADE;

CREATE TABLE achats.appels_offres (
    id UUID PRIMARY KEY,
    reference VARCHAR(50) UNIQUE NOT NULL,
    objet VARCHAR(200) NOT NULL,
    type_ao VARCHAR(50) NOT NULL DEFAULT 'APPEL_OFFRES',
    besoin_id UUID,
    date_limite DATE NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'PUBLIE',
    offre_retenue_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE TABLE achats.offres (
    id UUID PRIMARY KEY,
    appel_offre_id UUID NOT NULL REFERENCES achats.appels_offres(id),
    fournisseur_id UUID NOT NULL,
    code_anonyme VARCHAR(50) NOT NULL,
    montant_propose DECIMAL(19,2) NOT NULL,
    delai_livraison_jours INT NOT NULL,
    note_technique DOUBLE PRECISION DEFAULT 80.0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE achats.contrats_achat (
    id UUID PRIMARY KEY,
    numero VARCHAR(50) UNIQUE NOT NULL,
    appel_offre_id UUID NOT NULL REFERENCES achats.appels_offres(id),
    offre_id UUID,
    fournisseur_id UUID NOT NULL,
    montant_total DECIMAL(19,2) NOT NULL,
    statut VARCHAR(50) NOT NULL DEFAULT 'BROUILLON',
    engagement_valide_daf BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

INSERT INTO achats.appels_offres (id, reference, objet, type_ao, besoin_id, date_limite, statut, offre_retenue_id) VALUES
('d1111111-1111-1111-1111-111111111111', 'AO-2026-001', 'Acquisition de 2 Pelles hydrauliques Caterpillar 320D', 'APPEL_OFFRES', 'c1111111-1111-1111-1111-111111111111', '2026-12-31', 'PUBLIE', NULL),
('d2222222-2222-2222-2222-222222222222', 'AO-2026-002', 'Acquisition d''une Niveleuse Caterpillar 140H', 'APPEL_OFFRES', 'c2222222-2222-2222-2222-222222222222', '2026-10-15', 'ATTRIBUE', 'e2222222-2222-2222-2222-222222222222')
ON CONFLICT (id) DO NOTHING;

INSERT INTO achats.offres (id, appel_offre_id, fournisseur_id, code_anonyme, montant_propose, delai_livraison_jours, note_technique) VALUES
('e1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'f1111111-1111-1111-1111-111111111111', 'OFF-ANON-781', 185000000, 21, 92.0),
('e2222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'f1111111-1111-1111-1111-111111111111', 'OFF-ANON-402', 110000000, 15, 90.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO achats.contrats_achat (id, numero, appel_offre_id, offre_id, fournisseur_id, montant_total, statut, engagement_valide_daf) VALUES
('a1111111-1111-1111-1111-111111111111', 'CTR-2026-001', 'd2222222-2222-2222-2222-222222222222', 'e2222222-2222-2222-2222-222222222222', 'f1111111-1111-1111-1111-111111111111', 110000000, 'SIGNE', false)
ON CONFLICT (id) DO NOTHING;

