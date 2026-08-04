package com.fitcoach.workout;

import com.fitcoach.profile.domain.PainArea;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Exercise substitutions to avoid movements commonly associated with discomfort for
 * a reported pain area — conservative, well-established accommodations only, not a
 * clinical judgment call. Takes priority over TrainerExercisePreferences in slot()
 * (safety before style).
 *
 * Plain static utility (not a Spring bean) — same reasoning as
 * TrainerExercisePreferences: WorkoutGenerationService is instantiated directly in
 * its test, no Spring context.
 *
 * Curation rule: every pair must stay within the same MovementPattern bucket
 * (compound vs. isolation), same as TrainerExercisePreferences — the numeric
 * prescription must never silently apply to the wrong kind of exercise.
 */
final class PainAvoidancePreferences {

    private static final Map<PainArea, Map<String, String>> REGISTRY = Map.of(
            PainArea.KNEE, Map.of(
                    "Leg Extension", "Leg Curl"
            ),
            PainArea.LOWER_BACK, Map.of(
                    "Romanian Deadlift", "Hip Thrust"
            ),
            PainArea.SHOULDER, Map.of(
                    "Overhead Press", "Chest Press Machine"
            )
            // OTHER / NONE: too unstructured to safely automate — no overrides.
    );

    private PainAvoidancePreferences() {}

    static Optional<String> resolve(Set<PainArea> painAreas, String canonicalName) {
        if (painAreas == null) return Optional.empty();
        for (PainArea area : painAreas) {
            Map<String, String> forArea = REGISTRY.get(area);
            if (forArea == null) continue;
            String candidate = forArea.get(canonicalName);
            if (candidate != null) return Optional.of(candidate);
        }
        return Optional.empty();
    }
}
