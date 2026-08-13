ALTER TABLE workout_exercises
    ADD COLUMN substituted_exercise_id UUID REFERENCES exercises(id);

-- Snapshot of the exercise actually performed for each historical set, so a later
-- swap-back on the workout_exercises slot never retroactively changes which
-- exercise's history/progression a past set counts toward.
ALTER TABLE set_logs
    ADD COLUMN exercise_id UUID REFERENCES exercises(id);

UPDATE set_logs sl
SET exercise_id = we.exercise_id
FROM workout_exercises we
WHERE we.id = sl.workout_exercise_id
  AND sl.exercise_id IS NULL;

ALTER TABLE set_logs
    ALTER COLUMN exercise_id SET NOT NULL;
