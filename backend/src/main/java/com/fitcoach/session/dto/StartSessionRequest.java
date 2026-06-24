package com.fitcoach.session.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartSessionRequest(
        @NotNull(message = "workoutDayId is required")
        UUID workoutDayId
) {}
