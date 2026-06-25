-- V5 — weekly check-ins for progress tracking.

CREATE TABLE weekly_check_ins (
    id                   UUID        PRIMARY KEY,
    user_id              UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    week_start           DATE        NOT NULL,
    weight_kg            NUMERIC(5,2),
    sleep_quality_rating SMALLINT    CHECK (sleep_quality_rating BETWEEN 1 AND 5),
    energy_rating        SMALLINT    CHECK (energy_rating BETWEEN 1 AND 5),
    stress_rating        SMALLINT    CHECK (stress_rating BETWEEN 1 AND 5),
    pain_status          VARCHAR(32) NOT NULL DEFAULT 'NO_PAIN',
    notes                TEXT,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_check_in_user_week UNIQUE (user_id, week_start)
);

CREATE INDEX idx_weekly_check_ins_user_id ON weekly_check_ins (user_id);
