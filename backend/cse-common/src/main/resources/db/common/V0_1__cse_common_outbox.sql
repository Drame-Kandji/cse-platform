-- Tables techniques communes à chaque microservice (créées dans le schéma du service).
-- Outbox transactionnelle : événements en attente de publication Kafka (ADR-0007).
CREATE TABLE outbox_event (
    id           UUID PRIMARY KEY,
    topic        VARCHAR(120) NOT NULL,
    message_key  VARCHAR(120) NOT NULL,
    event_type   VARCHAR(80)  NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ,
    attempts     INT          NOT NULL DEFAULT 0,
    last_error   VARCHAR(500)
);
CREATE INDEX idx_outbox_event_en_attente ON outbox_event (created_at) WHERE published_at IS NULL;

-- Idempotence des consommateurs Kafka : un événement n'est traité qu'une fois par consommateur.
CREATE TABLE processed_event (
    event_id     UUID         NOT NULL,
    consumer     VARCHAR(120) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (event_id, consumer)
);
