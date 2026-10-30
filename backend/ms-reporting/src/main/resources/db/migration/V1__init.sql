CREATE SCHEMA IF NOT EXISTS reporting;
CREATE TABLE IF NOT EXISTS reporting.audit_log (id UUID PRIMARY KEY, event_type VARCHAR(100), payload TEXT, created_at TIMESTAMP DEFAULT NOW());
