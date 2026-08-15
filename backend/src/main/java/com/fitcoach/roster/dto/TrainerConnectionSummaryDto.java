package com.fitcoach.roster.dto;

import java.time.Instant;
import java.util.UUID;

public record TrainerConnectionSummaryDto(
        UUID trainerId,
        String displayName,
        String email,
        Instant linkedAt
) {}
