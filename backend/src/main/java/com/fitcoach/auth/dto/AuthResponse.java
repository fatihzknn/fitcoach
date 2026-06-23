package com.fitcoach.auth.dto;

public record AuthResponse(String token, UserDto user, boolean onboardingCompleted) {}
