DROP TABLE IF EXISTS auth.utilisateurs CASCADE;

CREATE TABLE auth.utilisateurs (
    id UUID PRIMARY KEY,
    keycloak_id VARCHAR(255) UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    type_utilisateur VARCHAR(50) NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    chantier_attribue_id UUID,
    perimetre_parc VARCHAR(255),
    seuil_delegation DECIMAL(19,2),
    budget_gere DECIMAL(19,2),
    fournisseur_id UUID,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

INSERT INTO auth.utilisateurs (id, type_utilisateur, keycloak_id, nom, prenom, email, actif) VALUES
('b1111111-1111-1111-1111-111111111111', 'CHEF_CHANTIER', 'chef-chantier-sub', 'Diop', 'Mamadou', 'chef.chantier@cse.sn', true),
('b2222222-2222-2222-2222-222222222222', 'RESPONSABLE_MATERIEL', 'resp-materiel-sub', 'Fall', 'Aminata', 'resp.materiel@cse.sn', true),
('b3333333-3333-3333-3333-333333333333', 'RESPONSABLE_ACHATS', 'resp-achats-sub', 'Ndiaye', 'Ousmane', 'resp.achats@cse.sn', true),
('b4444444-4444-4444-4444-444444444444', 'DAF', 'daf-sub', 'Sarr', 'Fatou', 'daf@cse.sn', true),
('b5555555-5555-5555-5555-555555555555', 'DIRECTION_GENERALE', 'dg-sub', 'Ba', 'Ibrahima', 'dg@cse.sn', true),
('b6666666-6666-6666-6666-666666666666', 'ADMIN_SYSTEME', 'admin-sub', 'Gueye', 'Cheikh', 'admin@cse.sn', true)
ON CONFLICT (id) DO NOTHING;
