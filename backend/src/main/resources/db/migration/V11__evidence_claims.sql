-- V11 — Evidence claims: citable coaching claims extracted from real trainer content
-- (YouTube transcripts + research papers), used to ground AI coach responses in
-- real, sourced material instead of generic LLM output. Populated by
-- EvidenceDataLoader at application startup from bundled JSONL files — not seeded
-- here via SQL because the source format (nested JSON per claim) doesn't fit
-- plain INSERT statements at this volume (~4300 rows).

CREATE TABLE evidence_claims (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id       VARCHAR(120) NOT NULL,
    creator_name    VARCHAR(100) NOT NULL,
    source_type     VARCHAR(30)  NOT NULL,
    source_title    TEXT,
    source_url      VARCHAR(500),
    domain          VARCHAR(60)  NOT NULL,
    claim           TEXT         NOT NULL,
    claim_type      VARCHAR(30)  NOT NULL,
    confidence      VARCHAR(20)  NOT NULL,
    evidence_quote  TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_evidence_claims_domain ON evidence_claims(domain);
