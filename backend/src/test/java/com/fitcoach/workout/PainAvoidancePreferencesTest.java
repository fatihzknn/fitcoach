package com.fitcoach.workout;

import com.fitcoach.profile.domain.PainArea;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PainAvoidancePreferencesTest {

    @Test
    void nullPainAreas_returnsEmpty() {
        assertThat(PainAvoidancePreferences.resolve(null, "Leg Extension")).isEmpty();
    }

    @Test
    void emptyPainAreas_returnsEmpty() {
        assertThat(PainAvoidancePreferences.resolve(Set.of(), "Leg Extension")).isEmpty();
    }

    @Test
    void none_hasNoOverrides() {
        assertThat(PainAvoidancePreferences.resolve(Set.of(PainArea.NONE), "Leg Extension")).isEmpty();
    }

    @Test
    void other_hasNoOverrides() {
        assertThat(PainAvoidancePreferences.resolve(Set.of(PainArea.OTHER), "Overhead Press")).isEmpty();
    }

    @Test
    void knee_avoidsLegExtension() {
        Optional<String> result = PainAvoidancePreferences.resolve(Set.of(PainArea.KNEE), "Leg Extension");
        assertThat(result).contains("Leg Curl");
    }

    @Test
    void lowerBack_avoidsRomanianDeadlift() {
        Optional<String> result = PainAvoidancePreferences.resolve(Set.of(PainArea.LOWER_BACK), "Romanian Deadlift");
        assertThat(result).contains("Hip Thrust");
    }

    @Test
    void shoulder_avoidsOverheadPress() {
        Optional<String> result = PainAvoidancePreferences.resolve(Set.of(PainArea.SHOULDER), "Overhead Press");
        assertThat(result).contains("Chest Press Machine");
    }

    @Test
    void unmappedCanonicalName_returnsEmpty() {
        assertThat(PainAvoidancePreferences.resolve(Set.of(PainArea.KNEE), "Biceps Curl")).isEmpty();
    }

    @Test
    void multiplePainAreas_resolvesEachForItsOwnCanonicalName() {
        Set<PainArea> both = Set.of(PainArea.KNEE, PainArea.LOWER_BACK);
        assertThat(PainAvoidancePreferences.resolve(both, "Leg Extension")).contains("Leg Curl");
        assertThat(PainAvoidancePreferences.resolve(both, "Romanian Deadlift")).contains("Hip Thrust");
    }
}
