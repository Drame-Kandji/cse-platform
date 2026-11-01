-- Portail fournisseur : signature du contrat en deux temps (API_CONTRACT §1 et §13). Le statut
-- EN_ATTENTE_SIGNATURE (signé par le CSE, en attente du fournisseur) est projeté tel quel.
ALTER TYPE statut_contrat ADD VALUE IF NOT EXISTS 'EN_ATTENTE_SIGNATURE' AFTER 'BROUILLON';
