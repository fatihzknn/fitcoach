-- V3 — exercise library and workout plans.

-- ─────────────────────────────────────────
-- Exercise library
-- ─────────────────────────────────────────

CREATE TABLE exercises (
    id                   UUID PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    primary_muscle_group VARCHAR(32)  NOT NULL,
    movement_pattern     VARCHAR(32)  NOT NULL,
    difficulty_level     VARCHAR(32)  NOT NULL,
    video_url            VARCHAR(500),
    form_cue             TEXT         NOT NULL,
    common_mistake       TEXT         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_exercises_name UNIQUE (name)
);

CREATE TABLE exercise_secondary_muscles (
    exercise_id  UUID        NOT NULL,
    muscle_group VARCHAR(32) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group),
    CONSTRAINT fk_secondary_exercise FOREIGN KEY (exercise_id)
        REFERENCES exercises (id) ON DELETE CASCADE
);

CREATE TABLE exercise_alternatives (
    exercise_id    UUID NOT NULL,
    alternative_id UUID NOT NULL,
    PRIMARY KEY (exercise_id, alternative_id),
    CONSTRAINT fk_alt_exercise    FOREIGN KEY (exercise_id)    REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT fk_alt_alternative FOREIGN KEY (alternative_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT ck_no_self_alternative CHECK (exercise_id <> alternative_id)
);

-- ─────────────────────────────────────────
-- Workout plans
-- ─────────────────────────────────────────

CREATE TABLE workout_plans (
    id                     UUID         PRIMARY KEY,
    user_id                UUID         NOT NULL,
    name                   VARCHAR(100) NOT NULL,
    goal                   VARCHAR(32)  NOT NULL,
    training_days_per_week INT          NOT NULL,
    is_active              BOOLEAN      NOT NULL DEFAULT FALSE,
    sustainability_warning TEXT,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_workout_plan_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE workout_days (
    id              UUID         PRIMARY KEY,
    workout_plan_id UUID         NOT NULL,
    day_number      INT          NOT NULL,
    workout_name    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_workout_day_plan FOREIGN KEY (workout_plan_id) REFERENCES workout_plans (id) ON DELETE CASCADE,
    CONSTRAINT uq_workout_day UNIQUE (workout_plan_id, day_number)
);

CREATE TABLE workout_exercises (
    id             UUID        PRIMARY KEY,
    workout_day_id UUID        NOT NULL,
    exercise_id    UUID        NOT NULL,
    order_index    INT         NOT NULL,
    sets           INT         NOT NULL,
    rep_range_min  INT         NOT NULL,
    rep_range_max  INT         NOT NULL,
    rir_guidance   VARCHAR(20) NOT NULL,
    rest_seconds   INT         NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_workout_exercise_day FOREIGN KEY (workout_day_id) REFERENCES workout_days (id) ON DELETE CASCADE,
    CONSTRAINT fk_workout_exercise_ex  FOREIGN KEY (exercise_id)    REFERENCES exercises (id),
    CONSTRAINT uq_workout_exercise_order UNIQUE (workout_day_id, order_index)
);

-- ─────────────────────────────────────────
-- Seed: exercise library
-- ─────────────────────────────────────────

INSERT INTO exercises (id, name, primary_muscle_group, movement_pattern, difficulty_level, video_url, form_cue, common_mistake, created_at, updated_at) VALUES
    (gen_random_uuid(), 'Barbell Bench Press',  'CHEST',      'PUSH',      'INTERMEDIATE', NULL,
     'Arch upper back, retract shoulder blades, feet flat on floor, bar touches mid-chest.',
     'Bouncing the bar off the chest or letting elbows flare excessively outward.',
     now(), now()),

    (gen_random_uuid(), 'Dumbbell Bench Press', 'CHEST',      'PUSH',      'BEGINNER',     NULL,
     'Control the dumbbells with elbows at 45–75 degrees from torso, lower to chest level.',
     'Dumbbells drifting too wide at the bottom, losing chest tension.',
     now(), now()),

    (gen_random_uuid(), 'Chest Press Machine',  'CHEST',      'PUSH',      'BEGINNER',     NULL,
     'Adjust seat so handles align with mid-chest, press forward smoothly without shrugging.',
     'Shrugging shoulders up during the press or not reaching full extension.',
     now(), now()),

    (gen_random_uuid(), 'Lat Pulldown',         'BACK',       'PULL',      'BEGINNER',     NULL,
     'Lean back slightly, pull bar to upper chest, squeeze shoulder blades at the bottom.',
     'Pulling behind the neck or swinging the torso to generate momentum.',
     now(), now()),

    (gen_random_uuid(), 'Cable Row',            'BACK',       'PULL',      'BEGINNER',     NULL,
     'Sit tall, row handle to lower chest, squeeze back at peak, control the return.',
     'Rounding the lower back or using excessive body swing to move the weight.',
     now(), now()),

    (gen_random_uuid(), 'Leg Press',            'QUADS',      'SQUAT',     'BEGINNER',     NULL,
     'Feet shoulder-width on platform, press until almost straight, lower until 90° or deep without hips lifting.',
     'Locking out knees forcefully or letting hips roll off the pad at the bottom.',
     now(), now()),

    (gen_random_uuid(), 'Romanian Deadlift',    'HAMSTRINGS', 'HINGE',     'INTERMEDIATE', NULL,
     'Hinge at hips with soft knees, keep bar close to legs, maintain a neutral spine throughout.',
     'Rounding the lower back or bending the knees so much it becomes a squat.',
     now(), now()),

    (gen_random_uuid(), 'Leg Curl',             'HAMSTRINGS', 'ISOLATION', 'BEGINNER',     NULL,
     'Curl heel toward glutes through full range, pause at top, control the return slowly.',
     'Lifting hips off the pad or using momentum to swing the weight.',
     now(), now()),

    (gen_random_uuid(), 'Leg Extension',        'QUADS',      'ISOLATION', 'BEGINNER',     NULL,
     'Extend fully and squeeze quad at the top, then lower with a slow controlled descent.',
     'Swinging the weight or not controlling the descent back to the start.',
     now(), now()),

    (gen_random_uuid(), 'Lateral Raise',        'SHOULDERS',  'ISOLATION', 'BEGINNER',     NULL,
     'Lead with elbows, raise to shoulder height with a slight forward lean, thumbs level with pinkies.',
     'Shrugging the traps or swinging the body to generate momentum.',
     now(), now()),

    (gen_random_uuid(), 'Biceps Curl',          'BICEPS',     'ISOLATION', 'BEGINNER',     NULL,
     'Keep elbows pinned to sides, curl through full range, squeeze at the top.',
     'Swinging the back or letting elbows drift forward at the top of the movement.',
     now(), now()),

    (gen_random_uuid(), 'Triceps Pushdown',     'TRICEPS',    'ISOLATION', 'BEGINNER',     NULL,
     'Elbows locked at sides, extend fully at the bottom, control the cable on the way back up.',
     'Flaring elbows out or leaning forward excessively to assist the push.',
     now(), now()),

    (gen_random_uuid(), 'Push-up',              'CHEST',      'PUSH',      'BEGINNER',     NULL,
     'Body forms a straight line head to heels, elbows at 45 degrees, chest touches the floor.',
     'Sagging hips toward the floor or flaring elbows wide like a T.',
     now(), now()),

    (gen_random_uuid(), 'Assisted Pull-up',     'BACK',       'PULL',      'BEGINNER',     NULL,
     'Full hang, pull chin above bar, hold briefly at top, control the descent slowly.',
     'Not using full range of motion or kipping to swing body up.',
     now(), now()),

    (gen_random_uuid(), 'Goblet Squat',         'QUADS',      'SQUAT',     'BEGINNER',     NULL,
     'Hold dumbbell at chest, squat deep with elbows tracking inside knees at the bottom.',
     'Heels lifting off the floor or excessive forward lean of the torso.',
     now(), now()),

    (gen_random_uuid(), 'Overhead Press',       'SHOULDERS',  'PUSH',      'INTERMEDIATE', NULL,
     'Core braced, press bar in a slight arc around the face, lock out fully overhead.',
     'Leaning excessively back to assist the press or not fully locking out at the top.',
     now(), now()),

    (gen_random_uuid(), 'Dumbbell Row',         'BACK',       'PULL',      'BEGINNER',     NULL,
     'Brace on a bench, drive elbow to hip, squeeze lat at the top of each rep.',
     'Rotating the torso to generate momentum instead of isolating the back.',
     now(), now()),

    (gen_random_uuid(), 'Hip Thrust',           'GLUTES',     'HINGE',     'BEGINNER',     NULL,
     'Shoulders on bench, drive hips up until body is parallel, squeeze glutes hard at top.',
     'Hyperextending the lower back instead of achieving a posterior pelvic tilt at lockout.',
     now(), now());

-- ─────────────────────────────────────────
-- Seed: secondary muscle groups
-- ─────────────────────────────────────────

INSERT INTO exercise_secondary_muscles (exercise_id, muscle_group)
SELECT id, 'TRICEPS'   FROM exercises WHERE name = 'Barbell Bench Press'
UNION ALL
SELECT id, 'SHOULDERS' FROM exercises WHERE name = 'Barbell Bench Press'
UNION ALL
SELECT id, 'TRICEPS'   FROM exercises WHERE name = 'Dumbbell Bench Press'
UNION ALL
SELECT id, 'SHOULDERS' FROM exercises WHERE name = 'Dumbbell Bench Press'
UNION ALL
SELECT id, 'TRICEPS'   FROM exercises WHERE name = 'Chest Press Machine'
UNION ALL
SELECT id, 'SHOULDERS' FROM exercises WHERE name = 'Chest Press Machine'
UNION ALL
SELECT id, 'BICEPS'    FROM exercises WHERE name = 'Lat Pulldown'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Lat Pulldown'
UNION ALL
SELECT id, 'BICEPS'    FROM exercises WHERE name = 'Cable Row'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Cable Row'
UNION ALL
SELECT id, 'GLUTES'    FROM exercises WHERE name = 'Leg Press'
UNION ALL
SELECT id, 'HAMSTRINGS' FROM exercises WHERE name = 'Leg Press'
UNION ALL
SELECT id, 'GLUTES'    FROM exercises WHERE name = 'Romanian Deadlift'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Romanian Deadlift'
UNION ALL
SELECT id, 'GLUTES'    FROM exercises WHERE name = 'Leg Curl'
UNION ALL
SELECT id, 'TRICEPS'   FROM exercises WHERE name = 'Push-up'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Push-up'
UNION ALL
SELECT id, 'SHOULDERS' FROM exercises WHERE name = 'Push-up'
UNION ALL
SELECT id, 'BICEPS'    FROM exercises WHERE name = 'Assisted Pull-up'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Assisted Pull-up'
UNION ALL
SELECT id, 'GLUTES'    FROM exercises WHERE name = 'Goblet Squat'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Goblet Squat'
UNION ALL
SELECT id, 'TRICEPS'   FROM exercises WHERE name = 'Overhead Press'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Overhead Press'
UNION ALL
SELECT id, 'BICEPS'    FROM exercises WHERE name = 'Dumbbell Row'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Dumbbell Row'
UNION ALL
SELECT id, 'HAMSTRINGS' FROM exercises WHERE name = 'Hip Thrust'
UNION ALL
SELECT id, 'CORE'      FROM exercises WHERE name = 'Hip Thrust';

-- ─────────────────────────────────────────
-- Seed: alternatives (bidirectional)
-- ─────────────────────────────────────────

-- Bench Press family
INSERT INTO exercise_alternatives (exercise_id, alternative_id)
SELECT e1.id, e2.id FROM exercises e1, exercises e2
WHERE (e1.name = 'Barbell Bench Press' AND e2.name IN ('Dumbbell Bench Press','Chest Press Machine','Push-up'))
   OR (e1.name = 'Dumbbell Bench Press' AND e2.name IN ('Barbell Bench Press','Chest Press Machine','Push-up'))
   OR (e1.name = 'Chest Press Machine' AND e2.name IN ('Barbell Bench Press','Dumbbell Bench Press','Push-up'))
   OR (e1.name = 'Push-up' AND e2.name IN ('Barbell Bench Press','Dumbbell Bench Press','Chest Press Machine'));

-- Pull family
INSERT INTO exercise_alternatives (exercise_id, alternative_id)
SELECT e1.id, e2.id FROM exercises e1, exercises e2
WHERE (e1.name = 'Lat Pulldown' AND e2.name IN ('Assisted Pull-up','Cable Row','Dumbbell Row'))
   OR (e1.name = 'Assisted Pull-up' AND e2.name IN ('Lat Pulldown','Cable Row','Dumbbell Row'))
   OR (e1.name = 'Cable Row' AND e2.name IN ('Lat Pulldown','Assisted Pull-up','Dumbbell Row'))
   OR (e1.name = 'Dumbbell Row' AND e2.name IN ('Lat Pulldown','Assisted Pull-up','Cable Row'));

-- Squat family
INSERT INTO exercise_alternatives (exercise_id, alternative_id)
SELECT e1.id, e2.id FROM exercises e1, exercises e2
WHERE (e1.name = 'Leg Press' AND e2.name = 'Goblet Squat')
   OR (e1.name = 'Goblet Squat' AND e2.name = 'Leg Press');

-- Hinge family
INSERT INTO exercise_alternatives (exercise_id, alternative_id)
SELECT e1.id, e2.id FROM exercises e1, exercises e2
WHERE (e1.name = 'Romanian Deadlift' AND e2.name IN ('Leg Curl','Hip Thrust'))
   OR (e1.name = 'Leg Curl' AND e2.name IN ('Romanian Deadlift','Hip Thrust'))
   OR (e1.name = 'Hip Thrust' AND e2.name IN ('Romanian Deadlift','Leg Curl'));

-- Shoulder press
INSERT INTO exercise_alternatives (exercise_id, alternative_id)
SELECT e1.id, e2.id FROM exercises e1, exercises e2
WHERE (e1.name = 'Overhead Press' AND e2.name = 'Lateral Raise')
   OR (e1.name = 'Lateral Raise' AND e2.name = 'Overhead Press');

-- Arms
INSERT INTO exercise_alternatives (exercise_id, alternative_id)
SELECT e1.id, e2.id FROM exercises e1, exercises e2
WHERE (e1.name = 'Biceps Curl' AND e2.name = 'Triceps Pushdown')
   OR (e1.name = 'Triceps Pushdown' AND e2.name = 'Biceps Curl');
