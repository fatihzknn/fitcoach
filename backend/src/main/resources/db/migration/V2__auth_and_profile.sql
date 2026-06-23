-- V2 — authentication and onboarding.

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));

CREATE TABLE fitness_profiles (
    id                       UUID PRIMARY KEY,
    user_id                  UUID NOT NULL,
    main_goal                VARCHAR(32) NOT NULL,
    training_background      VARCHAR(32) NOT NULL,
    training_days_per_week   INT  NOT NULL,
    session_duration_minutes INT  NOT NULL,
    age                      INT  NOT NULL,
    height_cm                INT  NOT NULL,
    weight_kg                DOUBLE PRECISION NOT NULL,
    sex                      VARCHAR(16) NOT NULL,
    onboarding_completed_at  TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL,
    updated_at               TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_fitness_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_fitness_profiles_user UNIQUE (user_id),
    CONSTRAINT ck_training_days CHECK (training_days_per_week BETWEEN 3 AND 5),
    CONSTRAINT ck_session_minutes CHECK (session_duration_minutes IN (45, 60, 75))
);

CREATE TABLE fitness_profile_pain_areas (
    fitness_profile_id UUID NOT NULL,
    pain_area          VARCHAR(32) NOT NULL,
    PRIMARY KEY (fitness_profile_id, pain_area),
    CONSTRAINT fk_pain_areas_profile FOREIGN KEY (fitness_profile_id)
        REFERENCES fitness_profiles (id) ON DELETE CASCADE
);
