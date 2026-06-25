package com.fitcoach.session.dto;

public record ExerciseHistoryEntryDto(
        String sessionDate,
        Double maxWeightKg,
        int bestReps,
        int totalSets
) {}
