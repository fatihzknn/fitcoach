package com.fitcoach.roster.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendTrainerMessageRequest(
        @NotBlank(message = "Message cannot be blank.")
        @Size(max = 2000, message = "Message must be 2000 characters or fewer.")
        String content
) {}
