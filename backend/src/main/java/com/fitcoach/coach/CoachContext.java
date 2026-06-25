package com.fitcoach.coach;

import java.util.List;

public record CoachContext(
        String userName,
        String mainGoal,
        String trainingBackground,
        int trainingDaysPerWeek,
        List<String> painAreas,
        String activePlanName,
        List<String> recentSessionSummaries,
        List<CoachPrinciple> principles,
        List<ChatMessage> recentHistory
) {}
