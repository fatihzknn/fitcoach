-- V13 — gate trainer philosophies to a specific sex where the methodology is
-- physiology-specific. NULL means visible to everyone (the 4 general trainers).
-- The women's-physiology-focused trainer is only shown to/selectable by users
-- whose profile sex is FEMALE — matches the fitness_profile.sex value exactly.

ALTER TABLE trainer_philosophies
    ADD COLUMN target_sex VARCHAR(10);

UPDATE trainer_philosophies
SET target_sex = 'FEMALE'
WHERE slug = 'womens-physiology-focused';
