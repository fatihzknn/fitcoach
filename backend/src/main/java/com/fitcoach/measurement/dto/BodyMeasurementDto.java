package com.fitcoach.measurement.dto;

import com.fitcoach.measurement.BodyMeasurement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BodyMeasurementDto(
        UUID id,
        LocalDate measuredAt,
        BigDecimal weightKg,
        BigDecimal neckCm,
        BigDecimal waistCm,
        BigDecimal hipCm,
        BigDecimal chestCm,
        BigDecimal bicepCm,
        BigDecimal thighCm,
        BigDecimal calfCm,
        BigDecimal bodyFatPercentage,
        String bodyFatMethod,
        String notes
) {
    public static BodyMeasurementDto from(BodyMeasurement m) {
        return new BodyMeasurementDto(
                m.getId(),
                m.getMeasuredAt(),
                m.getWeightKg(),
                m.getNeckCm(),
                m.getWaistCm(),
                m.getHipCm(),
                m.getChestCm(),
                m.getBicepCm(),
                m.getThighCm(),
                m.getCalfCm(),
                m.getBodyFatPercentage(),
                m.getBodyFatMethod(),
                m.getNotes()
        );
    }
}
