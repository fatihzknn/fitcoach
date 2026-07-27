package com.fitcoach.trainer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerVisibilityTest {

    @Test
    void nullTargetSex_isVisibleToEveryone() {
        assertThat(TrainerVisibility.isVisible(null, "FEMALE")).isTrue();
        assertThat(TrainerVisibility.isVisible(null, "MALE")).isTrue();
        assertThat(TrainerVisibility.isVisible(null, "OTHER")).isTrue();
        assertThat(TrainerVisibility.isVisible(null, null)).isTrue();
    }

    @Test
    void matchingTargetSex_isVisible() {
        assertThat(TrainerVisibility.isVisible("FEMALE", "FEMALE")).isTrue();
    }

    @Test
    void mismatchedTargetSex_isNotVisible() {
        assertThat(TrainerVisibility.isVisible("FEMALE", "MALE")).isFalse();
        assertThat(TrainerVisibility.isVisible("FEMALE", "OTHER")).isFalse();
        assertThat(TrainerVisibility.isVisible("FEMALE", null)).isFalse();
    }
}
