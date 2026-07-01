-- Trainer coaching philosophies
CREATE TABLE trainer_philosophies (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    slug                    VARCHAR(50) NOT NULL UNIQUE,
    display_name            VARCHAR(100) NOT NULL,
    tagline                 VARCHAR(200) NOT NULL,
    description             TEXT        NOT NULL,
    compound_rep_min        INT         NOT NULL DEFAULT 6,
    compound_rep_max        INT         NOT NULL DEFAULT 12,
    isolation_rep_min       INT         NOT NULL DEFAULT 10,
    isolation_rep_max       INT         NOT NULL DEFAULT 15,
    rest_seconds_compound   INT         NOT NULL DEFAULT 120,
    rest_seconds_isolation  INT         NOT NULL DEFAULT 60,
    rir_target              INT         NOT NULL DEFAULT 2,
    sets_compound           INT         NOT NULL DEFAULT 4,
    sets_isolation          INT         NOT NULL DEFAULT 3,
    deload_frequency_weeks  INT         NOT NULL DEFAULT 6,
    sort_order              INT         NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Link plans to the trainer philosophy used to generate them
ALTER TABLE workout_plans
    ADD COLUMN trainer_philosophy_id   UUID        REFERENCES trainer_philosophies(id),
    ADD COLUMN trainer_philosophy_name VARCHAR(100);

-- ── Seed 4 philosophies ──────────────────────────────────────────────────────

INSERT INTO trainer_philosophies
    (slug, display_name, tagline, description,
     compound_rep_min, compound_rep_max, isolation_rep_min, isolation_rep_max,
     rest_seconds_compound, rest_seconds_isolation,
     rir_target, sets_compound, sets_isolation, deload_frequency_weeks, sort_order)
VALUES
(
    'evidence-based',
    'Evidence-Based',
    'Science-driven volume & frequency',
    'Built around peer-reviewed sports science. Prioritizes progressive overload, high training frequency (each muscle trained twice per week), and leaving 2 reps in reserve to maximise long-term stimulus without excessive fatigue. Ideal if you want a proven, data-backed approach.',
    6, 20, 10, 30, 120, 60, 2, 4, 3, 6, 0
),
(
    'classic-bodybuilding',
    'Classic Bodybuilding',
    'Feel the muscle, chase the pump',
    'Old-school bodybuilding style. Trains primarily in the hypertrophy range with a strong emphasis on mind-muscle connection. Trains close to failure for maximum pump and metabolic stress. Great if you want that classic physique and love feeling every rep.',
    8, 12, 10, 15, 90, 60, 1, 4, 3, 8, 1
),
(
    'strength-focused',
    'Strength Focused',
    'Heavy compounds, build a strong foundation',
    'Powerlifting-influenced approach. Heavy compound lifts dominate every session with long rest periods for full neural recovery. Lower rep ranges and higher intensity build raw strength alongside muscle. Best if your primary goal is to lift heavier over time.',
    3, 6, 8, 12, 180, 90, 1, 5, 3, 4, 2
),
(
    'minimalist',
    'Minimalist',
    'Maximum results in minimum time',
    'Efficient training for busy schedules. Slightly higher rep ranges with shorter rest times maximise time under tension and metabolic stress as the primary growth drivers. No fluff — every set counts. Perfect if you want effective workouts in under an hour.',
    10, 15, 12, 20, 75, 45, 2, 3, 2, 6, 3
);
