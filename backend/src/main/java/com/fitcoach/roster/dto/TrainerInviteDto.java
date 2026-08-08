package com.fitcoach.roster.dto;

import com.fitcoach.roster.TrainerInvite;

import java.time.Instant;

public record TrainerInviteDto(String code, Instant expiresAt) {
    public static TrainerInviteDto from(TrainerInvite invite) {
        return new TrainerInviteDto(invite.getCode(), invite.getExpiresAt());
    }
}
