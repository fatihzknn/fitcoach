package com.fitcoach.session.dto;

import java.math.BigDecimal;

public record PreviousSetDto(int setNumber, BigDecimal weightKg, int repsCompleted, Integer rirActual) {}
