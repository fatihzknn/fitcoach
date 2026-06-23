package com.fitcoach.profile.dto;

import com.fitcoach.auth.dto.UserDto;

/** Current-user snapshot: identity, onboarding status, and the profile when present. */
public record MeResponse(UserDto user, boolean onboardingCompleted, FitnessProfileDto profile) {}
