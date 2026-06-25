package com.fitcoach.coach.provider;

import com.fitcoach.coach.CoachContext;

public interface CoachAiProvider {
    String generateResponse(String userMessage, CoachContext context);
}
