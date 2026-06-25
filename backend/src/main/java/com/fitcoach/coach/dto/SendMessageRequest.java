package com.fitcoach.coach.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 2000, message = "Message must be 2000 characters or fewer")
        String message
) {}
