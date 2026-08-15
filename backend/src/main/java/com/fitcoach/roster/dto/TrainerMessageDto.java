package com.fitcoach.roster.dto;

import com.fitcoach.roster.TrainerMessage;

import java.time.Instant;
import java.util.UUID;

public record TrainerMessageDto(
        UUID id,
        String content,
        Instant createdAt,
        boolean fromCurrentUser
) {
    public static TrainerMessageDto from(TrainerMessage message, UUID currentUserId) {
        return new TrainerMessageDto(
                message.getId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getSenderId().equals(currentUserId)
        );
    }
}
