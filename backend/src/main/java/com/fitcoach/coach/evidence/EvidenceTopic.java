package com.fitcoach.coach.evidence;

import java.util.List;

/**
 * Coaching topics the AI coach can cite real evidence for, each mapped to the
 * underlying claim domains extracted from trainer content. Mirrors the response
 * categories in MockCoachAiProvider so each canned answer can be grounded with a
 * real, sourced quote.
 *
 * PAIN is intentionally absent — injury/pain-related domains are excluded entirely
 * at load time (see EvidenceDataLoader) and the coach must never cite claims near
 * pain guidance; that response stays hardcoded and untouched.
 */
public enum EvidenceTopic {
    RECOVERY(List.of("deload_and_recovery", "recovery", "rest_intervals", "circadian_rhythm")),
    MOTIVATION(List.of("consistency_and_adherence")),
    TECHNIQUE(List.of("exercise_technique", "exercise_selection")),
    VOLUME(List.of("training_volume", "training_frequency", "rep_ranges", "training_programming")),
    STRENGTH(List.of("progressive_overload", "intensity_and_proximity_to_failure")),
    MUSCLE(List.of("muscle_hypertrophy", "hypertrophy", "muscle_development", "progressive_overload")),
    NUTRITION(List.of("nutrition", "nutrition_and_supplementation")),
    PROGRESS(List.of("progressive_overload", "deload_and_recovery", "intensity_and_proximity_to_failure"));

    private final List<String> domains;

    EvidenceTopic(List<String> domains) {
        this.domains = domains;
    }

    public List<String> domains() {
        return domains;
    }
}
