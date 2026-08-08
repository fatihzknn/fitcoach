-- V14 — Trainer portal: invite codes and trainer<->client links.
--
-- trainer_invites: one standing code per trainer (not one row per invitation
-- event — the requirement is a code any client can enter, not a single-use
-- per-recipient invite). Regenerating updates the row in place.
--
-- trainer_clients: the redeemed link. A client may link to multiple trainers
-- (no uniqueness on client_id alone) — no product requirement forces
-- one-trainer-only, and enforcing it would need its own UX with nothing
-- behind it.

CREATE TABLE trainer_invites (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        VARCHAR(8)   NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_trainer_invites_trainer UNIQUE (trainer_id),
    CONSTRAINT uq_trainer_invites_code UNIQUE (code)
);

CREATE TABLE trainer_clients (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id   UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_trainer_clients_pair UNIQUE (trainer_id, client_id)
);
