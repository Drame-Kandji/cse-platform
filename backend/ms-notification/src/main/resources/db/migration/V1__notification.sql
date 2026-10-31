-- ms-notification : notifications in-app, état de lecture par utilisateur, annuaire local.
CREATE TYPE niveau_notification AS ENUM ('INFO', 'SUCCES', 'ALERTE', 'CRITIQUE');

-- Une notification est adressée soit à un utilisateur, soit à un rôle (stockée une seule fois).
CREATE TABLE notifications (
    id                UUID PRIMARY KEY,
    type              VARCHAR(80)         NOT NULL,
    titre             VARCHAR(200)        NOT NULL,
    message           TEXT                NOT NULL,
    niveau            niveau_notification NOT NULL,
    lien              VARCHAR(300),
    entite            VARCHAR(40),
    entite_id         VARCHAR(80),
    destinataire_id   UUID,
    destinataire_role VARCHAR(40),
    event_id          UUID                NOT NULL,
    created_at        TIMESTAMPTZ         NOT NULL,
    CONSTRAINT ck_notifications_destinataire
        CHECK ((destinataire_id IS NULL) <> (destinataire_role IS NULL))
);
CREATE INDEX idx_notifications_destinataire_id
    ON notifications (destinataire_id, created_at DESC) WHERE destinataire_id IS NOT NULL;
CREATE INDEX idx_notifications_destinataire_role
    ON notifications (destinataire_role, created_at DESC) WHERE destinataire_role IS NOT NULL;
CREATE INDEX idx_notifications_event ON notifications (event_id);

-- Lecture par utilisateur : une notification de rôle est lue indépendamment par chaque détenteur.
CREATE TABLE notification_lectures (
    notification_id UUID        NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    utilisateur_id  UUID        NOT NULL,
    lue_le          TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (notification_id, utilisateur_id)
);
CREATE INDEX idx_notification_lectures_utilisateur ON notification_lectures (utilisateur_id);

-- Annuaire alimenté par cse.auth.events (adresses e-mail, chefs de chantier).
CREATE TABLE annuaire_utilisateurs (
    id            UUID PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    prenom        VARCHAR(100),
    nom           VARCHAR(100),
    role          VARCHAR(40)  NOT NULL,
    actif         BOOLEAN      NOT NULL,
    mis_a_jour_le TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_annuaire_utilisateurs_role ON annuaire_utilisateurs (role) WHERE actif;

CREATE TABLE annuaire_utilisateur_chantiers (
    utilisateur_id UUID NOT NULL REFERENCES annuaire_utilisateurs (id) ON DELETE CASCADE,
    chantier_id    UUID NOT NULL,
    PRIMARY KEY (utilisateur_id, chantier_id)
);
CREATE INDEX idx_annuaire_utilisateur_chantiers_chantier ON annuaire_utilisateur_chantiers (chantier_id);
