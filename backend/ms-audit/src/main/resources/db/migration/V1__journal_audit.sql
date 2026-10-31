-- Journal d'audit immuable de la plateforme CSE (schéma audit).
-- Les colonnes service/action/entite restent des VARCHAR et non des ENUM : leurs valeurs sont ouvertes
-- (tout nouveau service ou type d'événement doit être journalisé sans migration préalable).

CREATE TABLE audit_log (
    id              UUID         PRIMARY KEY,
    event_id        UUID         NOT NULL,
    occurred_at     TIMESTAMPTZ  NOT NULL,
    service         VARCHAR(80)  NOT NULL,
    action          VARCHAR(80)  NOT NULL,
    entite          VARCHAR(120) NOT NULL,
    entite_id       VARCHAR(120) NOT NULL,
    utilisateur_id  VARCHAR(120),
    utilisateur_nom VARCHAR(200),
    roles           TEXT[]       NOT NULL DEFAULT '{}',
    resume          TEXT,
    correlation_id  VARCHAR(120),
    enregistre_le   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_audit_log_event UNIQUE (event_id)
);

COMMENT ON TABLE audit_log IS 'Journal d''audit en ajout seul : UPDATE, DELETE et TRUNCATE sont rejetés par déclencheur.';

-- Index alignés sur les filtres de GET /api/audit (tri par défaut occurred_at DESC).
CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
CREATE INDEX idx_audit_log_service ON audit_log (service, occurred_at DESC);
CREATE INDEX idx_audit_log_action ON audit_log (action, occurred_at DESC);
CREATE INDEX idx_audit_log_entite ON audit_log (entite, entite_id);
CREATE INDEX idx_audit_log_utilisateur ON audit_log (utilisateur_id, occurred_at DESC);
CREATE INDEX idx_audit_log_correlation ON audit_log (correlation_id) WHERE correlation_id IS NOT NULL;

-- Immuabilité garantie au niveau de la base, y compris contre un accès SQL direct avec l'utilisateur
-- du service : toute tentative de modification ou de suppression lève une erreur (SQLSTATE 42501).
CREATE FUNCTION audit_log_interdire_modification() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'Journal d''audit immuable : opération % interdite sur audit_log', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

CREATE TRIGGER trg_audit_log_immuable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_interdire_modification();

CREATE TRIGGER trg_audit_log_sans_truncate
    BEFORE TRUNCATE ON audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION audit_log_interdire_modification();
