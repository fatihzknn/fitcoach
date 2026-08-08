package com.fitcoach.roster.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientSummaryDto(
        UUID clientId,
        String displayName,
        String email,
        Instant linkedAt,
        String activePlanName,
        int adherenceRate4Weeks,
        int currentStreakWeeks
) {}
