package com.fitcoach.roster.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemInviteRequest(
        @NotBlank(message = "Enter an invite code.")
        String code
) {}
