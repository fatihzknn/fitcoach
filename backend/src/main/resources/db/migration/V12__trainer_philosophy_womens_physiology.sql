-- V12 — 5th trainer philosophy: women's-physiology-informed strength training.
-- Numeric params grounded in real extracted claims from evidence_claims (Dr. Stacy
-- Sims source, domains rep_ranges/intensity_and_proximity_to_failure/training_frequency):
-- ~80% 1RM sits around 3-5 reps, muscular-endurance work cited at 8-10 reps, 2-3x/week
-- frequency per muscle. Sits between evidence-based and strength-focused — heavy
-- compound emphasis, not a "light weights, toning" caricature.
--
-- display_name is synthesized/generic per CLAUDE.md: never imitate an individual real
-- coach's identity or name in product-facing fields.

INSERT INTO trainer_philosophies
    (slug, display_name, tagline, description,
     compound_rep_min, compound_rep_max, isolation_rep_min, isolation_rep_max,
     rest_seconds_compound, rest_seconds_isolation,
     rir_target, sets_compound, sets_isolation, deload_frequency_weeks, sort_order)
VALUES
(
    'womens-physiology-focused',
    'Women''s Physiology Focused',
    'Heavy compounds, tuned to how women''s bodies adapt',
    'Strength training informed by research on training response across the menstrual cycle and female physiology. Prioritizes heavy compound lifts over high-rep ''toning'' work, trains each muscle 2–3 times per week, and leaves close to 2 reps in reserve on working sets. Places strong emphasis on recovery and sleep to support consistent progress — for lifters who want real strength, not a watered-down program.',
    5, 10, 8, 15, 150, 75, 2, 4, 3, 6, 4
);
