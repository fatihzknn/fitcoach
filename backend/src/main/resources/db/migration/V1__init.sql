-- V1 — baseline.
-- Phase 1 introduces no domain tables yet (entities arrive from Phase 2 onward).
-- This migration enables the extensions the schema will rely on so later phases can
-- assume UUID generation is available.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- provides gen_random_uuid()

-- Lightweight marker table so the baseline is observable and the DB is verifiably reachable.
CREATE TABLE app_metadata (
    key         VARCHAR(64) PRIMARY KEY,
    value       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_metadata (key, value) VALUES ('schema_phase', '1');
