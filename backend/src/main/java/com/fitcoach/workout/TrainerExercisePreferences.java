package com.fitcoach.workout;

import java.util.Map;
import java.util.Optional;

/**
 * Per-trainer exercise substitution preferences: which specific exercise a trainer
 * philosophy prefers within a movement family (e.g. barbell vs. machine bench press).
 *
 * Plain static utility — deliberately NOT a Spring bean. WorkoutGenerationService is
 * instantiated directly (including in tests, via {@code new WorkoutGenerationService()}
 * with no Spring context), and TemplateParams-style logic in that class is already
 * plain in-code data rather than DB- or Spring-managed.
 *
 * Curation rule: every pair here must stay within the same MovementPattern bucket
 * (compound vs. isolation) and target the same muscle intent — WorkoutGenerationService
 * derives sets/reps/RIR/rest from that bucket, so crossing it would silently apply the
 * wrong numeric prescription to a substituted exercise. Actual duplicate-avoidance
 * within a single workout day is handled separately by daySlots()'s per-day guard, not
 * by this registry.
 */
final class TrainerExercisePreferences {

    private static final Map<String, Map<String, String>> REGISTRY = Map.of(
            "classic-bodybuilding", Map.of(
                    "Barbell Bench Press", "Chest Press Machine",
                    "Dumbbell Row", "Cable Row",
                    "Assisted Pull-up", "Lat Pulldown",
                    "Goblet Squat", "Leg Press"
            ),
            "strength-focused", Map.of(
                    "Chest Press Machine", "Barbell Bench Press",
                    "Dumbbell Bench Press", "Barbell Bench Press",
                    "Cable Row", "Dumbbell Row",
                    "Lat Pulldown", "Assisted Pull-up",
                    "Leg Press", "Goblet Squat"
            ),
            "minimalist", Map.of(
                    "Barbell Bench Press", "Chest Press Machine",
                    "Goblet Squat", "Leg Press"
            ),
            "womens-physiology-focused", Map.of(
                    "Chest Press Machine", "Barbell Bench Press",
                    "Dumbbell Bench Press", "Barbell Bench Press",
                    "Cable Row", "Dumbbell Row",
                    "Leg Press", "Goblet Squat",
                    "Romanian Deadlift", "Hip Thrust"
            )
            // evidence-based: no overrides — its differentiator is frequency/volume,
            // not implement choice.
    );

    private TrainerExercisePreferences() {}

    static Optional<String> resolve(String slug, String canonicalName) {
        if (slug == null) return Optional.empty();
        Map<String, String> forSlug = REGISTRY.get(slug);
        if (forSlug == null) return Optional.empty();
        return Optional.ofNullable(forSlug.get(canonicalName));
    }
}
