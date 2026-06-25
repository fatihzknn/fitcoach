package com.fitcoach.checkin.dto;

import com.fitcoach.checkin.WeeklyCheckIn;
import com.fitcoach.checkin.domain.CheckInPainStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WeeklyCheckInDto(
        UUID id,
        LocalDate weekStart,
        BigDecimal weightKg,
        Integer sleepQualityRating,
        Integer energyRating,
        Integer stressRating,
        CheckInPainStatus painStatus,
        String notes
) {
    public static WeeklyCheckInDto from(WeeklyCheckIn c) {
        return new WeeklyCheckInDto(
                c.getId(),
                c.getWeekStart(),
                c.getWeightKg(),
                c.getSleepQualityRating(),
                c.getEnergyRating(),
                c.getStressRating(),
                c.getPainStatus(),
                c.getNotes()
        );
    }
}
