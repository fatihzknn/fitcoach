package com.fitcoach.coach.dto;

import com.fitcoach.coach.ChatMessage;
import com.fitcoach.coach.domain.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(
        UUID id,
        MessageRole role,
        String content,
        Instant createdAt
) {
    public static ChatMessageDto from(ChatMessage msg) {
        return new ChatMessageDto(
                msg.getId(),
                msg.getRole(),
                msg.getContent(),
                msg.getCreatedAt()
        );
    }
}
