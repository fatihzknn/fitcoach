package com.fitcoach.measurement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveMeasurementRequest(
        LocalDate measuredAt,    // null → today
        BigDecimal weightKg,
        BigDecimal neckCm,
        BigDecimal waistCm,
        BigDecimal hipCm,        // required for women; ignored/null for men
        BigDecimal chestCm,
        BigDecimal bicepCm,
        BigDecimal thighCm,
        BigDecimal calfCm,
        String notes
) {}
