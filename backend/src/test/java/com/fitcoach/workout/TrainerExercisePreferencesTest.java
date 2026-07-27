package com.fitcoach.workout;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerExercisePreferencesTest {

    @Test
    void nullSlug_returnsEmpty() {
        Optional<String> result = TrainerExercisePreferences.resolve(null, "Barbell Bench Press");
        assertThat(result).isEmpty();
    }

    @Test
    void unknownSlug_returnsEmpty() {
        Optional<String> result = TrainerExercisePreferences.resolve("not-a-real-slug", "Barbell Bench Press");
        assertThat(result).isEmpty();
    }

    @Test
    void knownSlug_unmappedCanonicalName_returnsEmpty() {
        Optional<String> result = TrainerExercisePreferences.resolve("strength-focused", "Biceps Curl");
        assertThat(result).isEmpty();
    }

    @Test
    void knownSlug_mappedCanonicalName_returnsPreferredExercise() {
        Optional<String> result = TrainerExercisePreferences.resolve("strength-focused", "Chest Press Machine");
        assertThat(result).contains("Barbell Bench Press");
    }

    @Test
    void evidenceBased_hasNoOverrides() {
        assertThat(TrainerExercisePreferences.resolve("evidence-based", "Chest Press Machine")).isEmpty();
        assertThat(TrainerExercisePreferences.resolve("evidence-based", "Barbell Bench Press")).isEmpty();
        assertThat(TrainerExercisePreferences.resolve("evidence-based", "Leg Press")).isEmpty();
    }

    @Test
    void womensPhysiologyFocused_prefersFreeWeightCompoundsAndHipHinge() {
        assertThat(TrainerExercisePreferences.resolve("womens-physiology-focused", "Chest Press Machine"))
                .contains("Barbell Bench Press");
        assertThat(TrainerExercisePreferences.resolve("womens-physiology-focused", "Romanian Deadlift"))
                .contains("Hip Thrust");
    }
}
