package com.fitcoach.checkin.dto;

import java.util.List;

public record ProgressStatsDto(
        int totalWorkoutsAllTime,
        int workoutsThisWeek,
        int currentStreakWeeks,
        int adherenceRate4Weeks,
        List<WeeklyCheckInDto> checkInHistory
) {}
