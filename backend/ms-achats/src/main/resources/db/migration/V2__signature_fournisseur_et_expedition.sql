-- Portail fournisseur : signature du contrat en deux temps (CSE puis fournisseur) et expédition
-- déclarée par le fournisseur (API_CONTRACT §13).
-- La nouvelle valeur n'est pas utilisée dans cette migration (contrainte PostgreSQL sur ADD VALUE
-- dans une transaction).
ALTER TYPE statut_contrat ADD VALUE IF NOT EXISTS 'EN_ATTENTE_SIGNATURE' AFTER 'BROUILLON';

ALTER TABLE contrat_achat
    ADD COLUMN signature_cse_par_id          UUID,
    ADD COLUMN signature_cse_par_nom         VARCHAR(150),
    ADD COLUMN signature_cse_date            TIMESTAMPTZ,
    ADD COLUMN signature_fournisseur_par_id  UUID,
    ADD COLUMN signature_fournisseur_par_nom VARCHAR(150),
    ADD COLUMN signature_fournisseur_date    TIMESTAMPTZ,
    ADD COLUMN motif_refus_signature         TEXT,
    ADD COLUMN date_expedition               DATE,
    ADD COLUMN commentaire_expedition        TEXT;
