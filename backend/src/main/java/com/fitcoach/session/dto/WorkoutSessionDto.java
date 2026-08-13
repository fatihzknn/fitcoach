package com.fitcoach.session.dto;

import com.fitcoach.session.WorkoutSession;
import com.fitcoach.session.domain.SessionStatus;
import com.fitcoach.workout.dto.WorkoutDayDto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record WorkoutSessionDto(
        UUID id,
        UUID workoutPlanId,
        WorkoutDayDto workoutDay,
        SessionStatus status,
        Instant startedAt,
        Instant completedAt,
        List<SetLogDto> setLogs,
        Set<UUID> strugglingExerciseIds
) {
    public static WorkoutSessionDto from(WorkoutSession s) {
        return from(s, Set.of());
    }

    /** strugglingExerciseIds is a pre-session hint, not a live indicator — only
     *  worth (re)computing when a session starts or is fetched, not after every
     *  single set log. */
    public static WorkoutSessionDto from(WorkoutSession s, Set<UUID> strugglingExerciseIds) {
        return new WorkoutSessionDto(
                s.getId(),
                s.getWorkoutPlan().getId(),
                WorkoutDayDto.from(s.getWorkoutDay()),
                s.getStatus(),
                s.getStartedAt(),
                s.getCompletedAt(),
                s.getSetLogs().stream().map(SetLogDto::from).toList(),
                strugglingExerciseIds
        );
    }
}
