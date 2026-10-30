-- Seed données de développement pour ms-suivi (profil dev uniquement)
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
