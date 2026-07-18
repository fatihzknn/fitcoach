CREATE TABLE body_measurements (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    measured_at           DATE          NOT NULL,

    -- Weight (optional duplicate here so measurements stand alone from check-ins)
    weight_kg             NUMERIC(5,2),

    -- US Navy BF% inputs
    neck_cm               NUMERIC(5,1),
    waist_cm              NUMERIC(5,1),
    hip_cm                NUMERIC(5,1),   -- required for women; null for men

    -- Optional site measurements (tracking only)
    chest_cm              NUMERIC(5,1),
    bicep_cm              NUMERIC(5,1),
    thigh_cm              NUMERIC(5,1),
    calf_cm               NUMERIC(5,1),

    -- Computed on save by the service
    body_fat_percentage   NUMERIC(5,2),

    notes                 TEXT,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    UNIQUE (user_id, measured_at)
);

CREATE INDEX idx_body_measurements_user_date
    ON body_measurements (user_id, measured_at DESC);
