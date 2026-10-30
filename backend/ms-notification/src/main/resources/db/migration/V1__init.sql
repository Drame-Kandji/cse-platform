CREATE SCHEMA IF NOT EXISTS notification;
CREATE TABLE IF NOT EXISTS notification.audit_log (id UUID PRIMARY KEY, event_type VARCHAR(100), payload TEXT, created_at TIMESTAMP DEFAULT NOW());
