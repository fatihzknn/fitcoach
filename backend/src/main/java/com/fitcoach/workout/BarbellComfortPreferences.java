package com.fitcoach.workout;

import com.fitcoach.profile.domain.BarbellComfort;

import java.util.Map;
import java.util.Optional;

/**
 * Substitutes for the seed library's barbell-implied compounds, applied when a user
 * reports PREFER_ALTERNATIVES at onboarding. Applied in slot() as a second-pass filter
 * over whatever pain-avoidance/trainer-preference already resolved to (not a third
 * parallel alternative) — see slot()'s javadoc for why.
 *
 * Plain static utility (not a Spring bean), same reasoning as PainAvoidancePreferences
 * and TrainerExercisePreferences.
 *
 * Safety invariant: none of these substitute values may collide with a
 * PainAvoidancePreferences key — comfort must never be able to override a
 * pain-motivated substitution. Guarded by a dedicated test.
 */
final class BarbellComfortPreferences {

    private static final Map<String, String> REGISTRY = Map.of(
            "Barbell Bench Press", "Chest Press Machine",
            "Romanian Deadlift", "Hip Thrust",
            "Overhead Press", "Chest Press Machine"
    );

    private BarbellComfortPreferences() {}

    static Optional<String> resolve(BarbellComfort comfort, String canonicalName) {
        if (comfort != BarbellComfort.PREFER_ALTERNATIVES) return Optional.empty();
        return Optional.ofNullable(REGISTRY.get(canonicalName));
    }
}
