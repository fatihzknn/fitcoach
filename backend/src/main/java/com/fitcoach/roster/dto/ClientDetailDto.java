package com.fitcoach.roster.dto;

import com.fitcoach.checkin.dto.ProgressStatsDto;
import com.fitcoach.workout.dto.WorkoutPlanDto;

/** activePlan is null when the client hasn't selected a plan yet — a normal
 *  state for a trainer to see, not an error. */
public record ClientDetailDto(
        ClientSummaryDto summary,
        ProgressStatsDto stats,
        WorkoutPlanDto activePlan
) {}
