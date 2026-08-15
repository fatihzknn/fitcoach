CREATE TABLE trainer_messages (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_client_id UUID        NOT NULL REFERENCES trainer_clients(id) ON DELETE CASCADE,
    sender_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content           TEXT        NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trainer_messages_trainer_client_id ON trainer_messages(trainer_client_id);

-- trainer_clients only had UNIQUE(trainer_id, client_id) (leading column trainer_id) —
-- no efficient path for "find all links for this client," which listMyTrainers needs.
CREATE INDEX idx_trainer_clients_client_id ON trainer_clients(client_id);
