package com.fitcoach.workout;

import com.fitcoach.profile.domain.BarbellComfort;
import com.fitcoach.profile.domain.PainArea;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BarbellComfortPreferencesTest {

    @Test
    void comfortable_hasNoOverrides() {
        assertThat(BarbellComfortPreferences.resolve(BarbellComfort.COMFORTABLE, "Barbell Bench Press")).isEmpty();
    }

    @Test
    void preferAlternatives_avoidsBarbellBenchPress() {
        Optional<String> result = BarbellComfortPreferences.resolve(BarbellComfort.PREFER_ALTERNATIVES, "Barbell Bench Press");
        assertThat(result).contains("Chest Press Machine");
    }

    @Test
    void preferAlternatives_avoidsRomanianDeadlift() {
        Optional<String> result = BarbellComfortPreferences.resolve(BarbellComfort.PREFER_ALTERNATIVES, "Romanian Deadlift");
        assertThat(result).contains("Hip Thrust");
    }

    @Test
    void preferAlternatives_avoidsOverheadPress() {
        Optional<String> result = BarbellComfortPreferences.resolve(BarbellComfort.PREFER_ALTERNATIVES, "Overhead Press");
        assertThat(result).contains("Chest Press Machine");
    }

    @Test
    void preferAlternatives_unmappedCanonicalName_returnsEmpty() {
        assertThat(BarbellComfortPreferences.resolve(BarbellComfort.PREFER_ALTERNATIVES, "Biceps Curl")).isEmpty();
    }

    @Test
    void noBarbellComfortSubstituteCollidesWithAPainAvoidanceKey() {
        // Safety invariant: comfort is applied as a post-filter over whatever
        // pain-avoidance already resolved to (see WorkoutGenerationService.slot()).
        // If a comfort substitute value were itself a pain-avoidance *key*, comfort
        // could silently re-introduce the exact exercise pain-avoidance just removed.
        Set<PainArea> allPainAreas = Set.of(PainArea.KNEE, PainArea.LOWER_BACK, PainArea.SHOULDER);
        for (String comfortSubstitute : Set.of("Chest Press Machine", "Hip Thrust")) {
            for (PainArea area : allPainAreas) {
                assertThat(PainAvoidancePreferences.resolve(Set.of(area), comfortSubstitute))
                        .as("PainAvoidancePreferences must not map away from '%s' (a BarbellComfortPreferences substitute)", comfortSubstitute)
                        .isEmpty();
            }
        }
    }
}
