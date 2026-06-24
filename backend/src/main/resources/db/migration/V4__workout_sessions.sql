-- V4 — workout sessions and set logging.

CREATE TABLE workout_sessions (
    id             UUID        PRIMARY KEY,
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    workout_plan_id UUID       NOT NULL REFERENCES workout_plans (id) ON DELETE CASCADE,
    workout_day_id UUID        NOT NULL REFERENCES workout_days (id) ON DELETE CASCADE,
    status         VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at     TIMESTAMPTZ NOT NULL,
    completed_at   TIMESTAMPTZ,
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE set_logs (
    id                  UUID    PRIMARY KEY,
    workout_session_id  UUID    NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    workout_exercise_id UUID    NOT NULL REFERENCES workout_exercises (id) ON DELETE CASCADE,
    set_number          INT     NOT NULL,
    weight_kg           NUMERIC(6,2),
    reps_completed      INT     NOT NULL,
    rir_actual          INT,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_set_log UNIQUE (workout_session_id, workout_exercise_id, set_number)
);

CREATE INDEX idx_workout_sessions_user_id ON workout_sessions (user_id);
CREATE INDEX idx_workout_sessions_status  ON workout_sessions (status);
CREATE INDEX idx_set_logs_session_id      ON set_logs (workout_session_id);
