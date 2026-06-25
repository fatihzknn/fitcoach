package com.fitcoach.checkin.dto;

import com.fitcoach.checkin.domain.CheckInPainStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record SubmitCheckInRequest(
        BigDecimal weightKg,

        @Min(1) @Max(5)
        Integer sleepQualityRating,

        @Min(1) @Max(5)
        Integer energyRating,

        @Min(1) @Max(5)
        Integer stressRating,

        CheckInPainStatus painStatus,

        String notes
) {}
