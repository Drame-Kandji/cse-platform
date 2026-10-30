-- Seed données de développement pour ms-fournisseurs (profil dev uniquement)
-- Chantiers Dakar/Thiès/Saint-Louis/Kaolack/Ziguinchor, engins réalistes

INSERT INTO fournisseurs (id, raison_sociale, ninea, rc, statut, score_performance) VALUES
('f1111111-1111-1111-1111-111111111111', 'SENEGAL TRAVAUX SA', 'SN123456789', 'RC/DKR/2020/B1234', 'ACTIF', 85.5),
('f2222222-2222-2222-2222-222222222222', 'BATIMAT SARL', 'SN987654321', 'RC/DKR/2019/C5678', 'ACTIF', 92.0),
('f3333333-3333-3333-3333-333333333333', 'EQUIPEMENTS AFRICA', 'SN456789123', 'RC/THIES/2021/D9012', 'ACTIF', 78.5)
ON CONFLICT DO NOTHING;
