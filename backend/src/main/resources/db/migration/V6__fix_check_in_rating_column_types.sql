-- V6 — fix weekly_check_ins rating columns from SMALLINT (int2) to INTEGER (int4).
-- Hibernate maps Java Integer to Types#INTEGER; SMALLINT causes schema-validation failure.

ALTER TABLE weekly_check_ins
    ALTER COLUMN sleep_quality_rating TYPE INTEGER,
    ALTER COLUMN energy_rating        TYPE INTEGER,
    ALTER COLUMN stress_rating        TYPE INTEGER;
