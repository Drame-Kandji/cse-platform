-- ms-chatbot : historique des conversations (chaque utilisateur ne voit que les siennes).
CREATE TYPE role_message AS ENUM ('UTILISATEUR', 'ASSISTANT');
CREATE TYPE intention_chatbot AS ENUM (
    'LOCALISER_ENGIN', 'DISPONIBILITE', 'STATUT_BESOIN', 'STATUT_COMMANDE', 'HISTORIQUE_MAINTENANCE',
    'PLANNING_AFFECTATION', 'FOURNISSEURS_PAR_CATEGORIE', 'PROCEDURE', 'AUTRE');

CREATE TABLE conversations (
    id              UUID PRIMARY KEY,
    utilisateur_id  UUID         NOT NULL,
    titre           VARCHAR(200) NOT NULL,
    dernier_message VARCHAR(500) NOT NULL,
    nombre_messages INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_conversations_utilisateur ON conversations (utilisateur_id, updated_at DESC);

CREATE TABLE conversation_messages (
    id              UUID PRIMARY KEY,
    conversation_id UUID              NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    ordre           INT               NOT NULL,
    role            role_message      NOT NULL,
    contenu         TEXT              NOT NULL,
    sources         TEXT              NOT NULL DEFAULT '[]',
    intention       intention_chatbot,
    mode_degrade    BOOLEAN           NOT NULL DEFAULT FALSE,
    horodatage      TIMESTAMPTZ       NOT NULL,
    CONSTRAINT uq_conversation_messages_ordre UNIQUE (conversation_id, ordre)
);
