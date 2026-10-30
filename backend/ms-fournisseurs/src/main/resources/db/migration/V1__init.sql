CREATE SCHEMA IF NOT EXISTS fournisseurs;
CREATE TABLE IF NOT EXISTS fournisseurs.audit_log (id UUID PRIMARY KEY, event_type VARCHAR(100), payload TEXT, created_at TIMESTAMP DEFAULT NOW());
