package com.fitcoach.trainer.dto;

import com.fitcoach.trainer.TrainerPhilosophy;

import java.util.UUID;

public record TrainerPhilosophyDto(
        UUID id,
        String slug,
        String displayName,
        String tagline,
        String description,
        int compoundRepMin,
        int compoundRepMax,
        int isolationRepMin,
        int isolationRepMax,
        int restSecondsCompound,
        int restSecondsIsolation,
        int rirTarget,
        int setsCompound,
        int setsIsolation,
        int deloadFrequencyWeeks
) {
    public static TrainerPhilosophyDto from(TrainerPhilosophy t) {
        return new TrainerPhilosophyDto(
                t.getId(), t.getSlug(), t.getDisplayName(), t.getTagline(), t.getDescription(),
                t.getCompoundRepMin(), t.getCompoundRepMax(),
                t.getIsolationRepMin(), t.getIsolationRepMax(),
                t.getRestSecondsCompound(), t.getRestSecondsIsolation(),
                t.getRirTarget(), t.getSetsCompound(), t.getSetsIsolation(),
                t.getDeloadFrequencyWeeks()
        );
    }
}
